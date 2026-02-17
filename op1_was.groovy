/**
 * Reset job ONLY if in failed/crashed/stopped state (3,96,97)
 * @param project DataStage project name
 * @param job Job name
 * @param dryRun Simulate reset without execution
 * @return true if reset performed (or would be performed in dryRun)
 */
boolean resetJobIfFailed(String project, String job, boolean dryRun = false) {
    def status = getJobStatus(project, job)
    def statusText = getStatusText(status)
    
    if (status in ['3', '96', '97']) {
        echo "⚠️  [${project}/${job}] Status ${status} (${statusText}) - requires reset"
        if (dryRun) {
            echo "📝 [DRY RUN] Would reset job '${job}'"
            return true
        } else {
            echo "🔄 Resetting job '${job}'..."
            return resetJob(project, job)
        }
    } else {
        echo "✅ [${project}/${job}] Status ${status} (${statusText}) - no reset needed"
        return false
    }
}


Your Schedule Description
Jenkins Cron Trigger (Runs 3-5 min AFTER job start)
Parameters
Lunes a Domingo 02:00
5 2 * * *
PROJECT=beneficios;JOB=SEQ_PROCESA
Lunes a Domingo 21:00
5 21 * * *
PROJECT=beneficios;JOB=SEQ_HDV
dias 09,10,11 de cada mes 05:00
5 5 9,10,11 * *
PROJECT=BNPP;JOB=CURRENCY_DSS
Martes y Jueves 19:00
5 19 * * 2,4
PROJECT=...;JOB=...
Lunes a Viernes 22:50
55 22 * * 1-5
PROJECT=...;JOB=...
día 09 de cada mes a las 09:00
5 9 9 * *
PROJECT=...;JOB=...
Cada Miercoles y Domingo 10:00
5 10 * * 3,0
PROJECT=...;JOB=...
Los días 15 y 30, a las 08:00
5 8 15,30 * *
PROJECT=...;JOB=...


    # Check 3 minutes after each interval
3,13,23,33,43,53 * * * * % PROJECT=...;JOB=...
