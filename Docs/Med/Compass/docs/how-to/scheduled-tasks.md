---
label: Scheduled Tasks
icon: clock
order: 20
---

# Scheduled Tasks

Run tasks automatically on a schedule or one-time basis - perfect for batch jobs, data cleanup, reports, or any recurring work.

## What You'll Configure

A **CronJob** is a Kubernetes resource that runs containerized tasks on a schedule (like Unix cron). Use it for:
- Nightly data processing
- Periodic cleanup tasks
- Report generation
- Database backups
- Any scheduled automation

**How it works:** Kubernetes creates a new container on your schedule, runs your script/command, and cleans up when done.

[!ref target="blank" text="Learn more about CronJobs" icon="link-external"](https://kubernetes.io/docs/concepts/workloads/controllers/cron-jobs/)

---

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: data-cleanup
spec:
  # Schedule in cron format: minute hour day month weekday
  schedule: "0 2 * * *"  # Every day at 2 AM

  # Keep last 3 successful and 1 failed job
  successfulJobsHistoryLimit: 3
  failedJobsHistoryLimit: 1

  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: cleanup
            image: my-app:1.0.0
            command: ["/bin/sh"]
            args: ["-c", "python cleanup.py"]
            env:
            - name: DB_HOST
              value: "postgres.database.svc.cluster.local"
          restartPolicy: OnFailure
```

---

## Schedule Formats

### Cron Syntax
```
┌───────────── minute (0 - 59)
│ ┌───────────── hour (0 - 23)
│ │ ┌───────────── day of month (1 - 31)
│ │ │ ┌───────────── month (1 - 12)
│ │ │ │ ┌───────────── day of week (0 - 6) (Sunday=0)
│ │ │ │ │
│ │ │ │ │
* * * * *
```

### Common Examples

**Every hour:**
```yaml
schedule: "0 * * * *"
```

**Every 6 hours:**
```yaml
schedule: "0 */6 * * *"
```

**Every weekday at 9 AM:**
```yaml
schedule: "0 9 * * 1-5"
```

**First day of month:**
```yaml
schedule: "0 0 1 * *"
```

**Every 15 minutes:**
```yaml
schedule: "*/15 * * * *"
```

---

## Advanced Configuration

### Concurrency Policy

Prevent overlapping runs:

```yaml
spec:
  # Options: Allow, Forbid, Replace
  concurrencyPolicy: Forbid
  schedule: "*/5 * * * *"
```

- **Allow** - (default) Multiple jobs can run simultaneously
- **Forbid** - Skip new job if previous still running
- **Replace** - Cancel current job, start new one

### Deadline and Backoff

```yaml
spec:
  # Start job within 100 seconds of scheduled time
  startingDeadlineSeconds: 100

  jobTemplate:
    spec:
      # Retry up to 3 times before marking as failed
      backoffLimit: 3

      # Complete within 10 minutes
      activeDeadlineSeconds: 600
```

### Suspend

Temporarily disable without deleting:

```yaml
spec:
  suspend: true  # Set to false to resume
  schedule: "0 * * * *"
```

---

## Using Secrets and ConfigMaps

```yaml
spec:
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: backup
            image: my-backup-tool:1.0.0
            envFrom:
            - configMapRef:
                name: backup-config
            - secretRef:
                name: s3-credentials
```

---

## Best Practices

### Job History Accumulation

Kubernetes stores completed jobs by default. To prevent accumulation and disk space issues:

```yaml
spec:
  # Keep minimal job history
  successfulJobsHistoryLimit: 1  # Keep last 1 successful job
  failedJobsHistoryLimit: 1      # Keep last 1 failed job
  schedule: "0 2 * * *"
```

**Why this matters:**
- Each completed job creates a Pod and Job object
- Kubernetes doesn't automatically clean up old jobs
- Over time, this consumes significant cluster resources and etcd storage
- Most use cases only need the last run for debugging

**Suggested settings for most applications:**
- `successfulJobsHistoryLimit: 1` - One success is enough to verify last run completed
- `failedJobsHistoryLimit: 1` - One failure is enough for troubleshooting
- For long-running or critical jobs, increase to `3-5` for more history

[!ref target="blank" text="Learn more about CronJob history" icon="link-external"](https://kubernetes.io/docs/concepts/workloads/controllers/cron-jobs/#jobs-history-limits)

### Resource Limits

Always set limits for CronJobs:

```yaml
spec:
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: task
            resources:
              requests:
                memory: "64Mi"
                cpu: "100m"
              limits:
                memory: "128Mi"
                cpu: "200m"
```

For more detailed information, [!ref see the Resource Limits guide](./resource-allocation.md).

### Exit Codes & Idempotency

Return proper exit codes and design jobs to be idempotent (safe to run multiple times).

For implementation details and examples, refer to the [Kubernetes Job documentation](https://kubernetes.io/docs/concepts/workloads/controllers/job/).

---

## Monitoring

### View CronJob Status in Grafana

1. Open [Grafana](https://g-05c60f9e9b.grafana-workspace.us-east-1.amazonaws.com/)
2. Navigate to **Kubernetes / Compute Resources / Namespace (Pods)**
3. Filter by your namespace
4. View CronJob-created pods and their status
5. Check **Kubernetes / Events** dashboard for CronJob events

### View Job History

1. In Grafana, navigate to **Kubernetes / Compute Resources / Namespace (Pods)**
2. Filter by job name label
3. View job execution history and completion status

### View Job Logs

1. Open Grafana **Explore → Loki**
2. Query logs:
   ```logql
   {namespace="<your-namespace>", job_name="data-cleanup"}
   ```
3. View logs from most recent job execution

**For detailed job history**, contact **Infra-Argo-Global** via ServiceNow.

---

## Troubleshooting

### Job Not Starting

Check CronJob events in Grafana:
1. Navigate to **Kubernetes / Events** dashboard
2. Filter by your namespace and CronJob name
3. Look for scheduling or configuration errors

Common issues:
- Invalid cron schedule syntax
- `startingDeadlineSeconds` too short
- `concurrencyPolicy: Forbid` and previous job still running

**For assistance**, contact **Infra-Argo-Global** via ServiceNow.

### Job Failing

View failed job logs in Grafana:
1. Open **Explore → Loki**
2. Query:
   ```logql
   {namespace="<your-namespace>", job_name="<job-name>"} |= "error"
   ```
3. Review error messages and stack traces

Check pod status:
1. Navigate to **Kubernetes / Compute Resources / Namespace (Pods)**
2. Filter by job name
3. View pod status and restart count

**For deeper troubleshooting**, contact **Infra-Argo-Global** via ServiceNow.

### Jobs Accumulating

Set history limits:
```yaml
successfulJobsHistoryLimit: 3  # Keep last 3 successful
failedJobsHistoryLimit: 1      # Keep last 1 failed
```

---

## Complete Example

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: daily-report
spec:
  schedule: "0 8 * * 1-5"  # Weekdays at 8 AM
  concurrencyPolicy: Forbid
  successfulJobsHistoryLimit: 1
  failedJobsHistoryLimit: 1

  jobTemplate:
    spec:
      backoffLimit: 2
      activeDeadlineSeconds: 3600  # 1 hour timeout

      template:
        metadata:
          labels:
            app: daily-report
        spec:
          restartPolicy: OnFailure

          containers:
          - name: report-generator
            image: my-app:1.0.0
            command: ["/app/generate-report.sh"]

            env:
            - name: REPORT_TYPE
              value: "daily-summary"

            envFrom:
            - configMapRef:
                name: report-config
            - secretRef:
                name: smtp-credentials

            resources:
              requests:
                memory: "256Mi"
                cpu: "500m"
              limits:
                memory: "512Mi"
                cpu: "1"
```

---

## Next Steps

[!ref icon="package" text="Deployments"](./deployments.md)

[!ref icon="server" text="Resource Allocation"](./resource-allocation.md)

[!ref icon="graph" text="Monitoring"](../monitoring/03-logging.md)
