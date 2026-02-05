#!/usr/bin/env groovy
// datastage.groovy - Simple DataStage utility methods for Jenkins Pipeline

// Status code constants
def STATUS_RUNNING = '0'
def STATUS_RUNNING_WITH_WARNINGS = '2'
def STATUS_FAILED = '3'
def STATUS_RESET = '21'
def STATUS_CRASHED = '96'
def STATUS_STOPPED = '97'

/**
 * Validate required inputs
 * @param project DataStage project name
 * @param jobNames List of job names to check
 */
void validateInputs(String project, List<String> jobNames) {
    if (!project || project.trim().isEmpty()) {
        error('Project name cannot be empty')
    }
    if (!jobNames || jobNames.isEmpty()) {
        error('Job names list cannot be empty')
    }
    if (!env.AUTHFILE) {
        error('AUTHFILE environment variable not set - configure credentials binding in Jenkinsfile')
    }
}

/**
 * Get job status code from DataStage
 * @param project DataStage project name
 * @param job Job name
 * @return Status code as String (e.g., '0', '3', '96')
 */
String getJobStatus(String project, String job) {
    def authFile = env.AUTHFILE
    def output = sh(
        script: "./dsjob -authfile ${authFile} -jobstatus ${project} ${job}",
        returnStdout: true
    ).trim()
    
    def matcher = output =~ /JobStatus:\s*(\d+)/
    if (!matcher || matcher.size() == 0) {
        error("Unable to parse status for job '${job}' in project '${project}'. Output: ${output}")
    }
    return matcher[0][1]
}

/**
 * Map status code to human-readable text
 * @param statusCode Status code string
 * @return Human-readable status description
 */
String getStatusText(String statusCode) {
    switch(statusCode) {
        case '0':  return 'Running'
        case '2':  return 'Running with warnings'
        case '3':  return 'Failed'
        case '21': return 'Reset'
        case '96': return 'Crashed'
        case '97': return 'Stopped'
        default:   return "Unknown status (${statusCode})"
    }
}

/**
 * Check status of specific jobs in a project
 * @param project DataStage project name
 * @param jobNames List of job names to check (e.g., ['JS_EXT_LOAD', 'JS_FILE_IN'])
 * @return Map of job statuses: [jobName: statusCode]
 */
Map<String, String> checkJobsStatus(String project, List<String> jobNames) {
    validateInputs(project, jobNames)
    
    echo "🔍 Checking status for ${jobNames.size()} jobs in project '${project}'"
    def results = [:]
    
    jobNames.each { job ->
        try {
            def status = getJobStatus(project, job)
            def statusText = getStatusText(status)
            echo "✅ ${job}: [${status}] ${statusText}"
            results[job] = status
        } catch (Exception e) {
            echo "❌ Failed to check status for ${job}: ${e.message}"
            results[job] = 'ERROR'
        }
    }
    
    return results
}

/**
 * Reset a single job using dsjob -run -mode RESET
 * @param project DataStage project name
 * @param job Job name to reset
 * @return Boolean true if reset command executed successfully
 */
boolean resetJob(String project, String job) {
    def authFile = env.AUTHFILE
    echo "🔄 Resetting job '${job}' in project '${project}'"
    
    try {
        sh "./dsjob -authfile ${authFile} -run -mode RESET ${project} ${job}"
        echo "✅ Reset command executed for ${job}"
        return true
    } catch (Exception e) {
        echo "❌ Reset failed for ${job}: ${e.message}"
        return false
    }
}

/**
 * Reset jobs that are NOT in running state (status != '0')
 * @param project DataStage project name
 * @param jobNames List of job names to evaluate
 * @param dryRun If true, only simulate reset without executing
 * @return Map of reset results: [jobName: [reset: true/false, statusBefore: '3', statusAfter: '21']]
 */
Map<String, Map> resetJobsOnNonRunning(String project, List<String> jobNames, boolean dryRun = false) {
    validateInputs(project, jobNames)
    
    echo "\n🔄 Resetting non-running jobs in project '${project}'"
    echo "   Mode: ${dryRun ? 'DRY RUN (simulation only)' : 'LIVE RESET'}"
    
    def results = [:]
    
    jobNames.each { job ->
        try {
            def statusBefore = getJobStatus(project, job)
            def statusText = getStatusText(statusBefore)
            def needsReset = (statusBefore != STATUS_RUNNING && statusBefore != STATUS_RESET)
            
            def result = [
                reset: false,
                statusBefore: statusBefore,
                statusTextBefore: statusText,
                needsReset: needsReset
            ]
            
            if (needsReset) {
                if (dryRun) {
                    echo "📝 [DRY RUN] Would reset ${job} (status: ${statusBefore} - ${statusText})"
                    result.reset = true
                    result.reason = 'Dry run simulation'
                } else {
                    echo "⚡ Resetting ${job} (status: ${statusBefore} - ${statusText})"
                    if (resetJob(project, job)) {
                        result.reset = true
                        // Wait briefly for status to update
                        sleep(time: 2, unit: 'SECONDS')
                        def statusAfter = getJobStatus(project, job)
                        result.statusAfter = statusAfter
                        result.statusTextAfter = getStatusText(statusAfter)
                        echo "   ➡️  New status: [${statusAfter}] ${result.statusTextAfter}"
                    } else {
                        result.reason = 'Reset command failed'
                    }
                }
            } else {
                echo "⏭️  Skipping ${job} (status: ${statusBefore} - ${statusText})"
                result.reason = "Already in acceptable state (status ${statusBefore})"
            }
            
            results[job] = result
            
        } catch (Exception e) {
            echo "❌ Error processing ${job}: ${e.message}"
            results[job] = [
                reset: false,
                error: e.message,
                exception: e.toString()
            ]
        }
    }
    
    // Summary
    def total = jobNames.size()
    def toReset = results.findAll { it.value.needsReset }.size()
    def actuallyReset = results.findAll { it.value.reset }.size()
    
    echo "\n📊 Reset Summary: ${actuallyReset}/${toReset} jobs reset out of ${total} evaluated"
    
    return results
}

/**
 * Execute raw dsjob command (authfile automatically injected)
 * @param command Command string without authfile (e.g., "-ljobs Cartolas")
 * @return Command output as String
 */
String executeDsjobCommand(String command) {
    def authFile = env.AUTHFILE
    return sh(
        script: "./dsjob -authfile ${authFile} ${command}",
        returnStdout: true
    ).trim()
}

// Make methods available when loaded via load()
return this
