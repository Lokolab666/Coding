---
label: Resource Allocation
icon: cpu
order: 70
---

# Resource Allocation

Configure CPU and memory resource requests and limits to ensure your application runs reliably and efficiently in the Kubernetes cluster.

## What You'll Configure

**Resource Requests** - Minimum guaranteed resources for your container
**Resource Limits** - Maximum resources your container can consume

**Why this matters:**
- **Scheduling:** Kubernetes uses requests to decide which node can run your pod
- **Auto-scaling:** HPA uses request values to calculate CPU/memory utilization percentages
- **Performance:** Prevents resource starvation and "noisy neighbor" problems
- **Stability:** Protects cluster from runaway processes consuming all resources

[!ref target="blank" text="Learn more about Resource Management" icon="link-external"](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/)

---

## Understanding Requests vs Limits

### Resource Requests (Minimum Guaranteed)

```yaml
resources:
  requests:
    cpu: 100m      # Guaranteed 100 millicores
    memory: 128Mi  # Guaranteed 128 MiB
```

**What requests do:**
- Reserve this amount on the node
- Kubernetes won't schedule pod if node doesn't have enough
- Container **guaranteed** to get at least this much
- Can use more if available (up to limit)

### Resource Limits (Maximum Allowed)

```yaml
resources:
  limits:
    cpu: 500m      # Can't exceed 500 millicores
    memory: 512Mi  # Can't exceed 512 MiB
```

**What limits do:**
- **CPU:** Container throttled if it tries to use more
- **Memory:** Container killed (OOMKilled) if it exceeds limit
- Prevents one container from consuming all node resources

---

## CPU Units

CPU is measured in **millicores** (1/1000th of a CPU core):

| Value | Meaning | Equivalent |
|-------|---------|------------|
| `1000m` or `1` | 1 full CPU core | 1 vCPU |
| `500m` | Half a CPU core | 0.5 vCPU |
| `100m` | One-tenth of a CPU core | 0.1 vCPU |
| `2000m` or `2` | Two full CPU cores | 2 vCPUs |

**Example:**
```yaml
resources:
  requests:
    cpu: 250m  # 25% of one CPU core
  limits:
    cpu: 1     # Max 1 full CPU core (1000m)
```

**CPU behavior:**
- Request below limit: Can burst up to limit when CPU available
- Exceeds limit: Container throttled (slowed down), not killed
- No limit set: Can use all available CPU on node

---

## Memory Units

Memory is measured in bytes with standard suffixes:

| Suffix | Decimal (Base 10) | Binary (Base 2) | Example |
|--------|-------------------|-----------------|---------|
| `Mi` | - | Mebibyte (1024²) | `256Mi` = 268,435,456 bytes |
| `Gi` | - | Gibibyte (1024³) | `1Gi` = 1,073,741,824 bytes |
| `M` | Megabyte (1000²) | - | `256M` = 256,000,000 bytes |
| `G` | Gigabyte (1000³) | - | `1G` = 1,000,000,000 bytes |

**Recommended:** Use binary units (`Mi`, `Gi`) for precision and consistency with Kubernetes metrics.

**Example:**
```yaml
resources:
  requests:
    memory: 256Mi  # 256 MiB minimum
  limits:
    memory: 512Mi  # 512 MiB maximum
```

**Memory behavior:**
- Request below limit: Can use more if available (up to limit)
- Exceeds limit: Container **killed** with OOMKilled error
- No limit set: Can use all available memory on node (dangerous!)

---

## Setting Appropriate Values

### Step 1: Start with Baseline

If you don't know your application's resource needs, start with these baselines:

**Lightweight web applications:**
```yaml
resources:
  requests:
    cpu: 100m
    memory: 128Mi
  limits:
    cpu: 500m
    memory: 512Mi
```

**Standard web applications:**
```yaml
resources:
  requests:
    cpu: 250m
    memory: 256Mi
  limits:
    cpu: 1000m
    memory: 1Gi
```

**Heavy backend services:**
```yaml
resources:
  requests:
    cpu: 500m
    memory: 512Mi
  limits:
    cpu: 2000m
    memory: 2Gi
```

### Step 2: Monitor Actual Usage

Deploy with baseline values and monitor for 1-2 weeks:

**View resource usage in Grafana:**
1. Open [Grafana](https://g-05c60f9e9b.grafana-workspace.us-east-1.amazonaws.com/)
2. Navigate to **Kubernetes / Compute Resources / Pod**
3. Select your namespace and pod
4. View CPU and memory usage over time

**What to look for:**
- **CPU usage** consistently near limit → Increase CPU limit
- **Memory usage** approaching limit → Increase memory limit
- **CPU/Memory usage** well below request → Decrease request values
- **OOMKilled errors** in logs → Increase memory limit

### Step 3: Calculate Appropriate Values

**Request calculation:**
- Set requests to **average usage** during normal operation
- Ensures scheduling works and provides baseline performance

**Limit calculation:**
- Set limits to **peak usage** + 20-30% buffer
- Prevents resource exhaustion during traffic spikes

**Example calculation:**
```
Observed average CPU: 150m
Observed peak CPU: 450m

Request: 150m (average)
Limit: 600m (peak + 30% buffer)
```

---

## Best Practices

### Always Set Request Values

**Required for:**
- Pod scheduling
- HPA (Horizontal Pod Autoscaler)
- Resource quota enforcement
- Quality of Service (QoS) classification

```yaml
# ✅ Good
resources:
  requests:
    cpu: 100m
    memory: 128Mi

# ❌ Bad - No requests
resources:
  limits:
    cpu: 500m
    memory: 512Mi
```

### Set Limits to Prevent Runaway Processes

Always set memory limits to prevent OOM conditions affecting the node:

```yaml
# ✅ Good - Limits set
resources:
  requests:
    cpu: 100m
    memory: 128Mi
  limits:
    cpu: 500m
    memory: 512Mi

# ❌ Dangerous - No limits (can consume entire node)
resources:
  requests:
    cpu: 100m
    memory: 128Mi
```

### Request = Limit for Guaranteed QoS

For critical applications, set requests equal to limits for guaranteed resources:

```yaml
resources:
  requests:
    cpu: 1000m
    memory: 1Gi
  limits:
    cpu: 1000m   # Same as request
    memory: 1Gi  # Same as request
```

**Benefits:**
- Highest QoS class ("Guaranteed")
- Resources never throttled or evicted
- Predictable performance

**Downside:**
- Less efficient resource utilization
- More expensive (reserves resources even when idle)

### Different Values for Different Environments

Use Kustomize patches to adjust resources per environment:

**k8s/base/deployment.yaml:**
```yaml
resources:
  requests:
    cpu: 100m
    memory: 128Mi
  limits:
    cpu: 500m
    memory: 512Mi
```

**k8s/production/deployment-patch.yaml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
spec:
  template:
    spec:
      containers:
      - name: my-app
        resources:
          requests:
            cpu: 250m
            memory: 256Mi
          limits:
            cpu: 2000m
            memory: 2Gi
```

---

## Common Patterns

### Web Applications (Node.js, Python, Ruby)

```yaml
resources:
  requests:
    cpu: 100m
    memory: 256Mi
  limits:
    cpu: 1000m
    memory: 512Mi
```

**Characteristics:**
- Low CPU, moderate memory
- Memory increases with connections
- Can handle bursts with CPU headroom

### Java Applications (Spring Boot, Tomcat)

```yaml
resources:
  requests:
    cpu: 500m
    memory: 768Mi
  limits:
    cpu: 2000m
    memory: 2Gi
```

**Characteristics:**
- Higher memory for JVM heap
- Moderate CPU usage
- Needs memory headroom for GC

### Background Workers / Batch Jobs

```yaml
resources:
  requests:
    cpu: 250m
    memory: 512Mi
  limits:
    cpu: 4000m
    memory: 4Gi
```

**Characteristics:**
- Variable CPU (can use all available)
- Memory depends on job size
- Higher limits for burst processing

### Databases / Caches (Redis, PostgreSQL)

```yaml
resources:
  requests:
    cpu: 500m
    memory: 1Gi
  limits:
    cpu: 2000m
    memory: 2Gi
```

**Characteristics:**
- Memory-intensive (data in RAM)
- Steady CPU usage
- Request close to limit for stability

---

## Resource Quotas

Resource quotas limit total resources a namespace can use:

```yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: namespace-quota
spec:
  hard:
    requests.cpu: "10"        # Max 10 CPU cores requested
    requests.memory: 20Gi     # Max 20 GiB memory requested
    limits.cpu: "20"          # Max 20 CPU cores limit
    limits.memory: 40Gi       # Max 40 GiB memory limit
    pods: "50"                # Max 50 pods
```

**Impact on your deployments:**
- Must set requests/limits when quota exists
- Deployment fails if quota exceeded
- Contact platform team to increase quota if needed

---

## Quality of Service (QoS) Classes

Kubernetes assigns QoS classes based on resource configuration:

### Guaranteed (Highest Priority)

Requests = Limits for all containers:
```yaml
resources:
  requests:
    cpu: 500m
    memory: 512Mi
  limits:
    cpu: 500m     # Same as request
    memory: 512Mi # Same as request
```

**Behavior:**
- Last to be evicted under pressure
- Resources never throttled
- Best for critical workloads

### Burstable (Medium Priority)

Requests < Limits:
```yaml
resources:
  requests:
    cpu: 100m
    memory: 128Mi
  limits:
    cpu: 500m     # Higher than request
    memory: 512Mi # Higher than request
```

**Behavior:**
- Can use extra resources when available
- Evicted before Guaranteed pods
- Good for most applications

### BestEffort (Lowest Priority)

No requests or limits set:
```yaml
resources: {}  # Nothing specified
```

**Behavior:**
- First to be evicted under pressure
- Can be throttled/killed anytime
- Only use for non-critical testing

---

## Impact on Horizontal Pod Autoscaler (HPA)

HPA calculates utilization as percentage of **requests**, not limits:

```yaml
resources:
  requests:
    cpu: 500m  # HPA uses this value
  limits:
    cpu: 2000m # HPA ignores this
```

**Example HPA calculation:**

Current CPU usage: 350m
CPU request: 500m
**Utilization: 70%** (350m / 500m)

If HPA target is 70%, it would maintain current replica count.

**Best practices for HPA:**
- Set requests to expected average usage
- Set limits higher (2-4x request) for burst capacity
- Monitor actual utilization in Grafana dashboards

[!ref For detailed HPA configuration, see High Availability](./high-availability.md).

---

## Troubleshooting

### Pod Stuck in Pending State

**Check pod status in Grafana:**
1. Open [Grafana](https://g-05c60f9e9b.grafana-workspace.us-east-1.amazonaws.com/)
2. Navigate to **Kubernetes / Compute Resources / Namespace (Pods)**
3. Filter by your namespace
4. Look for pods in **Pending** state
5. Check **Kubernetes / Events** dashboard for scheduling errors

**Common causes:**
- `Insufficient cpu` - Not enough CPU available on cluster nodes
- `Insufficient memory` - Not enough memory available

**Solutions:**
- Reduce resource requests in your deployment
- Contact **Infra-Argo-Global** to scale cluster or adjust quotas

### Container Keeps Getting OOMKilled

**Check pod status in Grafana:**
1. View **Kubernetes / Compute Resources / Pod** dashboard
2. Look for memory usage approaching limits
3. Check **Explore → Loki** for OOMKilled events:
   ```logql
   {namespace="<your-namespace>"} |= "OOMKilled"
   ```

**Solutions:**
- Increase memory limit in your deployment
- Investigate memory leak in application code
- Check for memory-intensive operations in logs

### CPU Throttling

**Check metrics in Grafana:**
1. Open **Kubernetes / Compute Resources / Pod** dashboard
2. View CPU usage vs limits
3. If CPU usage consistently at limit, you're being throttled

**Solutions:**
- Increase CPU limit in your deployment
- Optimize application code
- Add more replicas (horizontal scaling)

### HPA Not Scaling

**Check HPA status in Grafana:**
1. Open **Kubernetes / Compute Resources / Namespace (Pods)** dashboard
2. View replica count and metrics
3. If metrics show `<unknown>` or are missing:

**Causes:**
- Resource requests not set in deployment
- Metrics not being collected

**Solution:**
```yaml
# Add requests to deployment
resources:
  requests:
    cpu: 100m      # Required for HPA
    memory: 128Mi  # Required for HPA
```

**If issues persist**, contact **Infra-Argo-Global** via ServiceNow.

---

## Monitoring Resource Usage

### Grafana Dashboards

Navigate to [Grafana](https://g-05c60f9e9b.grafana-workspace.us-east-1.amazonaws.com/) and use these dashboards:

- CPU usage over time
- Memory usage over time
- Request vs limit comparison
- Pod restart history

### Set Up Alerts

Configure alerts in Grafana for:
- CPU usage > 80% of limit
- Memory usage > 80% of limit
- Pod restarts due to OOMKilled
- Pods pending due to insufficient resources

---

## Complete Example

Highly available web application with optimized resources:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: web-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: web-app
  template:
    metadata:
      labels:
        app: web-app
    spec:
      containers:
      - name: web-app
        image: web-app:1.0.0
        resources:
          requests:
            cpu: 250m      # Average usage
            memory: 256Mi  # Average usage
          limits:
            cpu: 1000m     # Peak usage + buffer
            memory: 512Mi  # Peak usage + buffer
        ports:
        - containerPort: 8080
        readinessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
```

**This configuration:**
- Reserves 250m CPU and 256Mi memory (requests)
- Can burst up to 1000m CPU and 512Mi memory (limits)
- Works with HPA for auto-scaling
- Provides 2x headroom for traffic spikes
- Gets "Burstable" QoS class

---

## Next Steps

[!ref icon="zap" text="High Availability & Auto-Scaling"](./high-availability.md)

[!ref icon="pulse" text="Health Checks"](../getting-started/kubernetes-manifests.md#2-configure-health-checks)

[!ref icon="server" text="Service Configuration"](./service-configuration.md)

[!ref icon="graph" text="Monitoring & Logging"](../monitoring/03-logging.md)
