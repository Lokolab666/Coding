---
label: High Availability
icon: zap
order: 10
---

# High Availability & Auto-Scaling

Ensure your application stays online during failures and automatically scales to handle traffic spikes.

## What You'll Configure

**High Availability** means your application can handle failures without downtime. This involves:

### Multiple Replicas
Run multiple copies (replicas) of your application so if one fails, others continue serving traffic.

### Service Configuration
Configure a Service to load balance traffic across healthy replicas and automatically handle failover.

[!ref target="blank" text="Learn more about Services" icon="link-external"](https://kubernetes.io/docs/concepts/services-networking/service/)

### Horizontal Pod Autoscaler (HPA)
Automatically add or remove replicas based on CPU usage, memory, or custom metrics.

**How it works:** HPA monitors your application's resource usage and scales the number of running containers up or down to match demand.

[!ref target="blank" text="Learn more about HPA" icon="link-external"](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/)

### Pod Disruption Budget (PDB)
Ensure a minimum number of replicas stay running during maintenance or cluster upgrades.

[!ref target="blank" text="Learn more about PDB" icon="link-external"](https://kubernetes.io/docs/concepts/workloads/pods/disruptions/)

---

## Step 1: Configure Multiple Replicas

In your `k8s/base/deployment.yaml`, set the default number of replicas:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
spec:
  replicas: 1  # Default for base (dev/testing)
  selector:
    matchLabels:
      app: my-app
  template:
    metadata:
      labels:
        app: my-app
    spec:
      containers:
      - name: my-app
        image: my-app
        resources:
          requests:
            cpu: 100m
            memory: 128Mi
          limits:
            cpu: 500m
            memory: 512Mi
```

### Environment-Specific Replica Counts

Override replica counts for each environment using patches:

**k8s/production/deployment-patch.yaml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
spec:
  replicas: 2  # Production: High availability
```

**k8s/staging/deployment-patch.yaml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
spec:
  replicas: 2  # Staging: Match production setup
```

**Recommended replica counts:**
- **Dev/Testing:** 1 replica (saves resources)
- **Staging:** 2 replicas (matches production behavior)
- **Production:** 2+ replicas (ensures high availability), HPA configuration would allow it to add more replicas as needed

**Add patches to kustomization.yaml:**
```yaml
# k8s/production/kustomization.yaml
resources:
- ../base
patchesStrategicMerge:
- deployment-patch.yaml
```

**Recommended minimum:** 2 replicas for production

---

## Step 2: Configure Service for Load Balancing

Your Service is what actually distributes traffic across healthy pods and provides automatic failover.

Create `k8s/base/service.yaml`:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-app
spec:
  type: ClusterIP
  selector:
    app: my-app  # Routes to pods with this label
  ports:
  - name: http
    port: 80
    targetPort: 8080
```

**How this enables HA:**
- Service automatically load balances across all healthy replicas
- When a pod fails readiness checks, Service stops routing traffic to it
- Remaining healthy pods continue serving requests (no downtime)
- When pod becomes healthy again, Service automatically adds it back

**Critical:** The service selector must match your deployment's pod labels. [!ref For detailed service configuration, see Service Configuration](./service-configuration.md).

---

## Step 3: Enable Auto-Scaling

Horizontal Pod Autoscaler (HPA) automatically adjusts replica count based on resource utilization.

**Recommended environments:**
- ✅ **Production** - Essential for handling traffic spikes and maintaining availability
- ✅ **Staging** - Recommended to match production behavior and test scaling
- ❌ **Dev/Testing** - Not recommended (predictable load, wastes resources)

Create `k8s/production/hpa.yaml`:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: my-app-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: my-app
  minReplicas: 2    # Never go below 2
  maxReplicas: 10   # Never exceed 10
  metrics:
  # Scale based on CPU usage
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70  # Target 70% CPU
  # Scale based on memory usage
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80  # Target 80% memory
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300  # Wait 5 min before scaling down
      policies:
      - type: Percent
        value: 50  # Scale down max 50% at a time
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0  # Scale up immediately
      policies:
      - type: Percent
        value: 100  # Double capacity if needed
        periodSeconds: 15
      - type: Pods
        value: 2  # Or add 2 pods at a time
        periodSeconds: 15
      selectPolicy: Max
```

**What this does:**
- Maintains 2-10 replicas
- Adds replicas when CPU > 70% or memory > 80%
- Removes replicas when usage drops
- Scales up fast, scales down slowly (prevents flapping)

**Note:** When HPA is active, it overrides the `replicas` field in your deployment. The deployment's replica count becomes the baseline when HPA is not installed.

### Add to Kustomization

Update `k8s/production/kustomization.yaml`:

```yaml
resources:
- ../base
- configmap.yaml
- hpa.yaml  # Add this
```

**For staging:** Create `k8s/staging/hpa.yaml` with identical or similar configuration to test scaling behavior before production.

---

### Scaling Strategies

#### CPU-Based (Default)
Best for: CPU-intensive applications
```yaml
metrics:
- type: Resource
  resource:
    name: cpu
    target:
      type: Utilization
      averageUtilization: 70
```

#### Memory-Based
Best for: Memory-intensive applications (databases, caches)
```yaml
metrics:
- type: Resource
  resource:
    name: memory
    target:
      type: Utilization
      averageUtilization: 80
```

#### Request-Based (Custom Metrics)
Best for: Web applications with varying traffic

Requires Prometheus and custom metrics:
```yaml
metrics:
- type: Pods
  pods:
    metric:
      name: http_requests_per_second
    target:
      type: AverageValue
      averageValue: "1000"  # Scale at 1000 req/sec per pod
```

---

### Monitoring Scaling Events

#### Check HPA Status in Grafana

1. Open [Grafana](https://g-05c60f9e9b.grafana-workspace.us-east-1.amazonaws.com/)
2. Navigate to **Kubernetes / Compute Resources / Namespace (Pods)**
3. Filter by your namespace
4. View metrics:
   - Current replica count
   - CPU/Memory usage vs targets
   - Scaling events over time

**Available metrics:**
- Current CPU/Memory utilization percentage
- Target threshold (e.g., 70%)
- Number of replicas (current vs min/max)

#### View Scaling Events

Monitor HPA scaling activity in Grafana:
1. Navigate to **Kubernetes / Events** dashboard
2. Filter by your namespace and HPA name
3. Look for events:
   - `SuccessfulRescale`: Scaled up due to high utilization
   - `SuccessfulRescale`: Scaled down due to low utilization

#### View Pod Distribution

Check pod distribution across nodes in Grafana:
1. Open **Kubernetes / Compute Resources / Namespace (Pods)** dashboard
2. View the **Pod Distribution** panel
3. Verify pods are spread across multiple nodes

**For detailed node placement analysis**, contact **Infra-Argo-Global** via ServiceNow.

---

## Step 4: Protect Against Disruptions

Create `k8s/production/pdb.yaml`:

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: my-app-pdb
spec:
  minAvailable: 1  # At least 1 pod must stay running
  selector:
    matchLabels:
      app: my-app
```

**What this does:**
- During cluster maintenance, Kubernetes won't drain all your pods at once
- Ensures at least 1 replica stays running during voluntary disruptions
- Protects against accidental pod deletions or maintenance

**Alternative:** Use `maxUnavailable` instead:
```yaml
spec:
  maxUnavailable: 1  # Only 1 pod can be down at a time
```

### Add to Kustomization

Update `k8s/production/kustomization.yaml`:

```yaml
resources:
- ../base
- configmap.yaml
- hpa.yaml
- pdb.yaml  # Add this
```

---

## Best Practices

### Always Set Resource Requests

HPA requires resource requests to calculate utilization:

```yaml
resources:
  requests:  # Required for HPA
    cpu: 100m
    memory: 128Mi
  limits:
    cpu: 500m
    memory: 512Mi
```

Without resource requests, HPA cannot make scaling decisions. For detailed guidance on resource allocation, [see the Resource Allocation guide](./resource-allocation.md).

### Use Pod Readiness Probes

Ensure only healthy pods receive traffic:

```yaml
containers:
- name: my-app
  readinessProbe:
    httpGet:
      path: /health
      port: 8080
    initialDelaySeconds: 10
    periodSeconds: 5
```

[!ref For more details, see Health Checks](../getting-started/kubernetes-manifests.md#2-configure-health-checks)

### Spread Pods Across Nodes

Use pod anti-affinity to distribute pods for resilience:

```yaml
affinity:
  podAntiAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
    - weight: 100
      podAffinityTerm:
        labelSelector:
          matchLabels:
            app: my-app
        topologyKey: kubernetes.io/hostname
```

For advanced pod scheduling, [see the Kubernetes Pod Affinity documentation](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/#affinity-and-anti-affinity).

### Conservative Scaling Configuration

Most applications benefit from slow scaling up and down:

- **minReplicas:** 2 (minimum for HA)
- **maxReplicas:** 10 (adjust based on your needs)
- **CPU target:** 70% (conservative threshold)
- **Scale up:** Immediately
- **Scale down:** Wait 5 minutes (prevents flapping)

For advanced HPA configurations, [see the Kubernetes HPA documentation](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/).

---

## Troubleshooting

### HPA Not Scaling

Check metrics in Grafana:
1. Open **Kubernetes / Compute Resources / Pod** dashboard
2. Select your application pods
3. Verify CPU/Memory metrics are being reported

If metrics show `<unknown>` or missing data:
- Ensure resource requests are defined in your deployment
- Verify your application is running and healthy
- Contact **Infra-Argo-Global** if metrics are still unavailable

**Check resource requests:**
Verify your deployment YAML includes:
```yaml
resources:
  requests:
    cpu: "250m"
    memory: "256Mi"
```

### Pods Not Spreading Across Nodes

Anti-affinity is "preferred" not "required".
```yaml
affinity:
  podAntiAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:  # Change to required
    - labelSelector:
        matchLabels:
          app: my-app
      topologyKey: kubernetes.io/hostname
```

⚠️ **Warning:** With `required`, pods won't schedule if no suitable nodes available

### Frequent Scaling (Flapping)

Increase stabilization window:
```yaml
behavior:
  scaleDown:
    stabilizationWindowSeconds: 600  # Wait 10 minutes
  scaleUp:
    stabilizationWindowSeconds: 60   # Wait 1 minute
```

---

## Complete Example

Highly available web application with auto-scaling:

**deployment.yaml:**
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
      priorityClassName: priority-production
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchLabels:
                  app: web-app
              topologyKey: kubernetes.io/hostname
      containers:
      - name: web-app
        image: web-app:1.0.0
        resources:
          requests:
            cpu: 200m
            memory: 256Mi
          limits:
            cpu: 1000m
            memory: 512Mi
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

**hpa.yaml:**
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: web-app-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: web-app
  minReplicas: 3
  maxReplicas: 15
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
    scaleUp:
      stabilizationWindowSeconds: 0
```

**pdb.yaml:**
```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: web-app-pdb
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: web-app
```

---

## Next Steps

[!ref icon="server" text="Service Configuration"](./service-configuration.md)

[!ref icon="pulse" text="Health Checks"](../getting-started/kubernetes-manifests.md#2-configure-health-checks)

[!ref icon="server" text="Resource Allocation"](./resource-allocation.md)

[!ref icon="rocket" text="Zero-Downtime Deployments"](./deployments.md)

[!ref icon="graph" text="Monitoring"](../monitoring/03-logging.md)
