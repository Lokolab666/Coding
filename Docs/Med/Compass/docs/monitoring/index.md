---
label: Monitoring & Observability
icon: rocket
order: 50
---

# Monitoring & Observability

Compass CI provides multiple tools for monitoring application health, performance, and troubleshooting issues in production.

## 🏥 Health Checks

Your application is monitored continuously through health check endpoints:

### `/health` Endpoint (Required)
- **Purpose:** Liveness check - is your application alive?
- **Response:** HTTP 200 if healthy, anything else if not
- **Checked:** Every 10 seconds by Kubernetes
- **Max Attempts:** 3 consecutive failures trigger pod restart

**Example Implementation:**
```python
@app.route('/health')
def health():
    return {'status': 'ok'}, 200
```

### `/ready` Endpoint (Recommended)
- **Purpose:** Readiness check - can you handle traffic?
- **Response:** HTTP 200 if ready, 503 if warming up
- **Checked:** Before routing traffic to pods
- **Used:** During deployments to roll out gradually

**Example Implementation:**
```python
@app.route('/ready')
def ready():
    if database_connected and cache_loaded:
        return {'status': 'ready'}, 200
    return {'status': 'not ready'}, 503
```

## 📊 Pod Status Monitoring

### View Pod Status in Grafana

Monitor your pod status through Grafana dashboards:

1. Open [Grafana](https://g-05c60f9e9b.grafana-workspace.us-east-1.amazonaws.com/)
2. Navigate to **Kubernetes / Compute Resources / Namespace (Pods)**
3. Filter by your application namespace
4. View real-time pod status, restarts, and resource usage

### Pod Status Meanings
| Status | Meaning | Action |
|---|---|---|
| **Pending** | Pod waiting for resources | Check node availability |
| **Running** | Pod active and healthy | No action needed |
| **CrashLoopBackOff** | Pod keeps crashing | Check logs for errors |
| **ImagePullBackOff** | Cannot pull container image | Check Artifactory access |
| **Terminating** | Pod shutting down | Normal during deployments |

**For deeper pod troubleshooting**, contact the platform support team (**Infra-Argo-Global**) via ServiceNow.

## 📝 Application Logs

### View Logs in Grafana

1. Open [Grafana](https://g-05c60f9e9b.grafana-workspace.us-east-1.amazonaws.com/)
2. Navigate to **Explore**
3. Select **Loki** data source
4. Filter by namespace and pod:
   ```
   {namespace="<your-namespace>", pod="<pod-name>"}
   ```
5. View real-time logs and search for errors

### CloudWatch Log Search
For centralized logging across cluster:

1. Open CloudWatch (AWS Console)
2. Navigate to **Log Groups**
3. Select log group for your cluster environment
4. Use queries to search logs:

**Example: Find all errors**
```
fields @timestamp, log, kubernetes.pod_name
| filter log like /ERROR|WARN|Exception/
| sort @timestamp desc
```

**Example: Search by namespace**
```
fields @timestamp, log, kubernetes.namespace_name
| filter kubernetes.namespace_name = "my-app-prod"
| sort @timestamp desc
```

## 📈 Metrics & Performance

### Container Resources

Monitor CPU and memory usage through Grafana:

1. Open [Grafana](https://g-05c60f9e9b.grafana-workspace.us-east-1.amazonaws.com/)
2. Navigate to **Kubernetes / Compute Resources / Pod**
3. Select your namespace and pod
4. View real-time CPU and memory metrics

**Available metrics:**
- CPU usage vs requests/limits
- Memory usage vs requests/limits
- Network I/O
- Disk I/O

### CPU/Memory Limits
Ensure your Kubernetes manifests define resource requests:

```yaml
containers:
  - name: app
    resources:
      requests:
        cpu: "250m"        # Minimum CPU needed
        memory: "256Mi"    # Minimum memory needed
      limits:
        cpu: "1000m"       # Maximum CPU allowed
        memory: "1Gi"      # Maximum memory allowed
```

## 🔍 Debugging Pod Issues

### Pod Won't Start

Use Grafana dashboards to investigate:

1. Check **Kubernetes / Compute Resources / Namespace (Pods)** for pod status
2. View **Explore → Loki** for recent logs with `{namespace="<your-namespace>"} |= "error"`
3. Review deployment events in the **Kubernetes / Events** dashboard

**If issues persist**, contact **Infra-Argo-Global** platform support via ServiceNow with:
- Application name and namespace
- Pod name and timestamp
- Error messages from Grafana logs

### Pod Crashing

Investigate crashloops through Grafana:

1. Open **Kubernetes / Compute Resources / Pod** dashboard
2. Check restart count and resource usage
3. View logs in **Explore → Loki** filtering for the crashed pod
4. Look for OOMKilled status or exit codes in pod metrics

**For detailed crash analysis**, contact **Infra-Argo-Global** with crash logs and timestamps.

### High CPU/Memory Usage

Monitor resource consumption in Grafana:

1. Open **Kubernetes / Compute Resources / Pod** dashboard
2. View CPU/Memory usage vs limits
3. Check historical trends to identify spikes
4. Review application logs for memory leaks or high load events

**To scale replicas**, update your deployment manifest in Git and merge to trigger redeployment. Contact **Infra-Argo-Global** for urgent scaling needs.

## 🚨 Alerting

### Set Up Alerts
To receive notifications about critical issues:

1. Contact the platform team with:
   - Application name and environment
   - Alert type (high CPU, pod crashes, health check fails, etc.)
   - Notification email or distribution list

2. Platform team configures alerts in monitoring system

3. You receive email notifications when thresholds are exceeded

### Common Alert Types
- **High CPU Usage:** Pod using >80% of limit
- **High Memory Usage:** Pod using >80% of limit
- **Pod Restart Loop:** Pod restarting every few minutes
- **Health Check Failing:** Pod failing `/health` endpoint
- **Zero Running Pods:** No pods running (complete outage)

## 📊 Distributed Tracing

Compass CI supports OpenTelemetry for distributed tracing:

- **JVM Applications:** Automatic tracing without code changes
- **Other Languages:** Add OpenTelemetry SDK to your application

Benefits:
- Track requests across services
- Identify performance bottlenecks
- Visualize service dependencies
- Debug complex multi-service issues

See [OpenTelemetry Documentation](https://opentelemetry.io/docs/instrumentation/) for implementation details.

## 🆘 Common Monitoring Scenarios

### Scenario: Pod crashes immediately after deploy
1. Check logs in Grafana **Explore → Loki** for the previous pod instance
2. Look for startup errors in the log stream
3. Verify environment variables in your Kubernetes manifest
4. Check database connectivity from logs
5. Verify secrets are configured in ExternalSecret resources

**For deeper investigation**, contact **Infra-Argo-Global** via ServiceNow.

### Scenario: Application slow/hanging
1. Check resource usage in Grafana **Kubernetes / Compute Resources / Pod** dashboard
2. Review logs in Grafana for errors or timeout messages
3. Check database query performance in logs
4. Verify external service connectivity from application logs
5. Update replica count in your deployment manifest to scale up

### Scenario: Intermittent failures
1. Check `/health` endpoint directly
2. Look for connection pool exhaustion
3. Check memory leaks in logs
4. Review recent code changes
5. Check for resource contention on node

## 📚 Next Steps

- [Application Logging](03-logging.md) - Deep dive into log analysis
- [Troubleshooting Guide](../troubleshooting-and-support/troubleshooting-guide.md) - Common issues and solutions
- [Security Monitoring](../security/index.md) - Vulnerability and security alerts
