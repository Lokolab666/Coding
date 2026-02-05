#!/usr/bin/env groovy
import groovy.json.JsonOutput

// ========================================
// PUBLIC API
// ========================================

/**
 * Check status of specific jobs in a project
 * @param project DataStage project name (e.g., 'Cartolas')
 * @param jobNames List of job names to check (e.g., ['JS_EXT_LOAD', 'JS_FILE_IN'])
 * @param failOnWarning Boolean - treat status=2 (warnings) as UNSTABLE
 * @return Map with job statuses: [jobName: [code: '0', text: 'Running', needsReset: false]]
 */
def checkJobsStatus(String project, List<String> jobNames, Boolean failOnWarning = false) {
    validateInputs(project, jobNames)
    
    echo "\n🔍 DataStage Status Check - Project: ${project}"
    echo "   Jobs to check: ${jobNames.join(', ')}"
    
    def results = [:]
    def failures = []
    def warnings = []
    
    jobNames.each { job ->
        try {
            def status = getJobStatus(project, job)
            def statusText = mapStatusCode(status)
            def needsReset = isResetRequired(status)
            
            results[job] = [
                code: status,
                text: statusText,
                needsReset: needsReset,
                timestamp: new Date().format('yyyy-MM-dd HH:mm:ss')
            ]
            
            logStatus(job, status, statusText, needsReset)
            
            // Determine build outcome
            if (status in ['96', '3']) {
                failures << "${job} (${statusText})"
            } else if (status in ['97', '21'] || (status == '2' && failOnWarning)) {
                warnings << "${job} (${statusText})"
            }
            
        } catch (Exception e) {
            def errorMsg = "Status check failed for ${job}: ${e.message}"
            echo "❌ ${errorMsg}"
            results[job] = [code: 'ERROR', text: errorMsg, needsReset: false, error: e.message]
            warnings << "${job} (check failed)"
        }
    }
    
    // Set Jenkins build result
    if (failures) {
        currentBuild.result = 'FAILURE'
        error("CRITICAL: Jobs failed - ${failures.join(', ')}")
    } else if (warnings) {
        currentBuild.result = 'UNSTABLE'
        echo "⚠️  Jobs requiring attention: ${warnings.join(', ')}"
    } else {
        echo "✅ All jobs in good state"
    }
    
    return results
}

/**
 * Reset jobs that are in a non-running state (failed/crashed/stopped)
 * @param project DataStage project name
 * @param jobNames List of job names to evaluate for reset
 * @param forceReset Boolean - reset even if job is running (use with caution!)
 * @param dryRun Boolean - simulate reset without executing (default: false)
 * @return Map of reset results: [jobName: [reset: true/false, reason: '...', statusBefore: '0']]
 */
def resetJobsOnFailure(String project, List<String> jobNames, Boolean forceReset = false, Boolean dryRun = false) {
    validateInputs(project, jobNames)
    
    echo "\n🔄 DataStage Reset Operation - Project: ${project}"
    echo "   Mode: ${dryRun ? 'DRY RUN (simulation)' : 'LIVE'} | Force: ${forceReset}"
    
    def results = [:]
    def authFile = getAuthFile()
    
    jobNames.each { job ->
        try {
            def beforeStatus = getJobStatus(project, job)
            def statusText = mapStatusCode(beforeStatus)
            def needsReset = isResetRequired(beforeStatus)
            
            def action = [:]
            action.statusBefore = beforeStatus
            action.statusTextBefore = statusText
            
            // Decision logic
            if (beforeStatus == '0' && !forceReset) {
                action.reset = false
                action.reason = "Job is running normally (status 0) - skipping reset"
                echo "⏭️  ${job}: ${action.reason}"
            } else if (beforeStatus == '21' && !forceReset) {
                action.reset = false
                action.reason = "Job already in RESET state (status 21) - skipping"
                echo "⏭️  ${job}: ${action.reason}"
            } else if (needsReset || forceReset) {
                if (dryRun) {
                    action.reset = true
                    action.reason = "[DRY RUN] Would reset job in state '${statusText}' (code ${beforeStatus})"
                    echo "📝 ${job}: ${action.reason}"
                } else {
                    echo "⚡ Resetting job '${job}' (current state: ${statusText})"
                    def resetOutput = resetJob(project, job, authFile)
                    action.reset = true
                    action.reason = "Reset executed successfully"
                    action.resetOutput = resetOutput.trim()
                    
                    // Verify reset took effect
                    sleep(2) // Brief pause for state propagation
                    def afterStatus = getJobStatus(project, job)
                    action.statusAfter = afterStatus
                    action.statusTextAfter = mapStatusCode(afterStatus)
                    
                    if (afterStatus == '21') {
                        echo "✅ ${job}: Successfully reset → status 21 (Reset)"
                    } else {
                        echo "⚠️  ${job}: Reset executed but status is ${afterStatus} (${mapStatusCode(afterStatus)})"
                    }
                }
            } else {
                action.reset = false
                action.reason = "Status ${beforeStatus} (${statusText}) - no reset required by policy"
                echo "⏭️  ${job}: ${action.reason}"
            }
            
            results[job] = action
            
        } catch (Exception e) {
            echo "❌ Reset failed for ${job}: ${e.message}"
            results[job] = [
                reset: false,
                reason: "Reset operation failed",
                error: e.message,
                exception: e.toString()
            ]
        }
    }
    
    // Summary report
    def resetCount = results.findAll { it.value.reset }.size()
    def totalCount = jobNames.size()
    echo "\n📊 Reset Summary: ${resetCount}/${totalCount} jobs reset"
    
    if (resetCount > 0 && !dryRun) {
        currentBuild.result = currentBuild.result ?: 'UNSTABLE' // Mark build as unstable if resets occurred
    }
    
    return results
}

/**
 * Execute raw dsjob command (for advanced use cases)
 * @param command Full dsjob command string (authfile will be injected)
 * @return Command output
 */
def executeCommand(String command) {
    def authFile = getAuthFile()
    def fullCmd = command.replaceFirst(/dsjob/, "./dsjob -authfile ${authFile}")
    return sh(script: fullCmd, returnStdout: true).trim()
}

// ========================================
// PRIVATE HELPERS
// ========================================

private def validateInputs(String project, List<String> jobNames) {
    if (!project || project.trim().isEmpty()) {
        error('Project name cannot be empty')
    }
    if (!jobNames || jobNames.isEmpty()) {
        error('Job names list cannot be empty')
    }
    if (!getAuthFile()) {
        error('AUTHFILE environment variable not set - configure Jenkins credentials binding')
    }
}

private def getAuthFile() {
    return env.AUTHFILE ?: error('AUTHFILE not bound - ensure credentials(\'datastage-authfile\') is set in environment block')
}

private def getJobStatus(String project, String job) {
    def authFile = getAuthFile()
    def output = sh(
        script: "./dsjob -authfile ${authFile} -jobstatus ${project} ${job}",
        returnStdout: true,
        returnStatus: false
    ).trim()
    
    def matcher = output =~ /JobStatus:\s*(\d+)/
    if (!matcher || matcher.size() == 0) {
        error("Unable to parse status for job '${job}' in project '${project}'. Output: ${output}")
    }
    return matcher[0][1]
}

private def resetJob(String project, String job, String authFile) {
    return sh(
        script: "./dsjob -authfile ${authFile} -run -mode RESET ${project} ${job}",
        returnStdout: true
    )
}

private def isResetRequired(String statusCode) {
    // Reset required for failed/crashed/stopped states
    return statusCode in ['3', '96', '97'] // Failed, Crashed, Stopped
}

private def mapStatusCode(String code) {
    def map = [
        '0': 'Running',
        '2': 'Running with warnings',
        '3': 'Failed',
        '21': 'Reset',
        '96': 'Crashed',
        '97': 'Stopped',
        'ERROR': 'Status check error'
    ]
    return map[code] ?: "Unknown status (${code})"
}

private def logStatus(String job, String code, String text, Boolean needsReset) {
    def icon = code == '0' ? '✅' : (code in ['96','3'] ? '❌' : '⚠️')
    def resetMsg = needsReset ? ' | [RESET REQUIRED]' : ''
    echo "${icon} ${job}: [${code}] ${text}${resetMsg}"
}

// ========================================
// EXPORT API
// ========================================
return this







@Library('your-shared-lib') _

pipeline {
    agent { label 'datastage-agent' }
    
    environment {
        // Bind authfile securely (Jenkins Credentials > Secret File > ID: datastage-authfile)
        AUTHFILE = credentials('datastage-authfile')
        DSHOME = '/opt/IBM/InformationServer/Server/DSEngine'
        PATH = "${DSHOME}/bin:${env.PATH}"
    }
    
    parameters {
        choice(name: 'ACTION', choices: ['CHECK_ONLY', 'RESET_ON_FAILURE', 'DRY_RUN_RESET'], description: 'Operation mode')
        booleanParam(name: 'FAIL_ON_WARNING', defaultValue: false, description: 'Treat warnings (status 2) as unstable')
    }
    
    stages {
        stage('DataStage Operations') {
            steps {
                script {
                    def project = 'Cartolas'
                    def jobsToMonitor = ['JS_EXT_LOAD', 'JS_FILE_IN', 'JS_DW_LOAD'] // Your curated list
                    
                    switch (params.ACTION) {
                        case 'CHECK_ONLY':
                            datastage.checkJobsStatus(project, jobsToMonitor, params.FAIL_ON_WARNING)
                            break
                            
                        case 'DRY_RUN_RESET':
                            // Simulate resets without execution
                            datastage.resetJobsOnFailure(project, jobsToMonitor, false, true)
                            break
                            
                        case 'RESET_ON_FAILURE':
                            // 1. Check status first
                            def statusResults = datastage.checkJobsStatus(project, jobsToMonitor)
                            
                            // 2. Reset only jobs that need it
                            def resetResults = datastage.resetJobsOnFailure(project, jobsToMonitor)
                            
                            // 3. Optional: Generate report
                            echo "\n📄 FINAL REPORT"
                            echo "Status Check: ${JsonOutput.toJson(statusResults)}"
                            echo "Reset Actions: ${JsonOutput.toJson(resetResults)}"
                            break
                    }
                }
            }
        }
    }
    
    post {
        always {
            script {
                // Optional: Publish results to Slack/Teams
                if (currentBuild.result != 'SUCCESS') {
                    slackSend channel: '#data-alerts', 
                              color: currentBuild.result == 'FAILURE' ? 'danger' : 'warning',
                              message: "DataStage job issue in ${env.JOB_NAME} #${env.BUILD_NUMBER} - ${currentBuild.result}"
                }
            }
        }
    }
}
