---
label: Deployments
order: 100
icon: container
---
# Deployments

Use this guide to choose the right Kubernetes workload type and apply best practices for reliable deployments.

## Choose the right workload type

!!! Kustomize image versioning
Define image tags in your `kustomization.yaml` overlays using `images.newTag` (or `images.newName`). Keep base manifests tagless so overlays control versions.
!!!

### Deployment (most web/API apps)
Use a `Deployment` for stateless services that can scale horizontally and tolerate pod replacement.

**Best for:** APIs, web frontends, background workers without durable local state.

<details>
<summary>Example: Deployment with best practices</summary>

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: my-app
  template:
    metadata:
      labels:
        app: my-app
    spec:
      priorityClassName: priority-production  # Set via environment patches
      topologySpreadConstraints:
        - maxSkew: 1
          topologyKey: topology.kubernetes.io/zone
          whenUnsatisfiable: ScheduleAnyway
          labelSelector:
            matchLabels:
              app: my-app
      containers:
        - name: app
          image: my-app # tag set via kustomize overlay
          ports:
            - containerPort: 8080
          resources:
            requests:
              cpu: "200m"
              memory: "256Mi"
            limits:
              cpu: "1"
              memory: "512Mi"
          securityContext:
            runAsNonRoot: true
            runAsUser: 1000
            readOnlyRootFilesystem: true
            allowPrivilegeEscalation: false
            capabilities:
              drop: ["ALL"]
          envFrom:
            - configMapRef:
                name: my-app-config
            - secretRef:
                name: my-app-secrets
          readinessProbe:
            httpGet:
              path: /health
              port: 8080
          livenessProbe:
            httpGet:
              path: /health
              port: 8080
---
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: my-app-pdb
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: my-app
```

</details>

### StatefulSet (stable identity + storage)
Use a `StatefulSet` when pods need stable network IDs and persistent storage.

**Best for:** databases, queues, or apps that require ordered startup or stable pod names.

<details>
<summary>Example: StatefulSet with best practices</summary>

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: my-db
spec:
  serviceName: my-db
  replicas: 3
  selector:
    matchLabels:
      app: my-db
  template:
    metadata:
      labels:
        app: my-db
    spec:
      priorityClassName: priority-production  # Set via environment patches
      containers:
        - name: db
          image: my-db # tag set via kustomize overlay
          ports:
            - containerPort: 5432
          resources:
            requests:
              cpu: "200m"
              memory: "512Mi"
            limits:
              cpu: "1"
              memory: "1Gi"
          securityContext:
            runAsNonRoot: true
            runAsUser: 999
            readOnlyRootFilesystem: true
            allowPrivilegeEscalation: false
            capabilities:
              drop: ["ALL"]
          envFrom:
            - configMapRef:
                name: my-db-config
            - secretRef:
                name: my-db-secrets
          readinessProbe:
            tcpSocket:
              port: 5432
          livenessProbe:
            tcpSocket:
              port: 5432
          volumeMounts:
            - name: data
              mountPath: /var/lib/data
  volumeClaimTemplates:
    - metadata:
        name: data
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 20Gi
---
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: my-db-pdb
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: my-db
```

</details>

### DaemonSet (one per node)
Use a `DaemonSet` to run a pod on every node (or selected nodes).

**Best for:** log shippers, monitoring agents, node-level services.

<details>
<summary>Example: DaemonSet with best practices</summary>

```yaml
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: node-agent
spec:
  selector:
    matchLabels:
      app: node-agent
  template:
    metadata:
      labels:
        app: node-agent
    spec:
      containers:
        - name: agent
          image: node-agent # tag set via kustomize overlay
          resources:
            requests:
              cpu: "100m"
              memory: "128Mi"
            limits:
              cpu: "500m"
              memory: "256Mi"
          securityContext:
            runAsNonRoot: true
            runAsUser: 1000
            readOnlyRootFilesystem: true
            allowPrivilegeEscalation: false
            capabilities:
              drop: ["ALL"]
          envFrom:
            - configMapRef:
                name: node-agent-config
```

</details>

### Job (run-to-completion)
Use a `Job` for one-time tasks that run until they finish successfully.

See [Scheduled Tasks](./scheduled-tasks.md) for step-by-step guidance.

**Best for:** data backfills, one-off scripts, migrations.

<details>
<summary>Example: Job with best practices</summary>

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: data-backfill
spec:
  backoffLimit: 3
  ttlSecondsAfterFinished: 86400
  template:
    spec:
      restartPolicy: Never
      containers:
        - name: backfill
          image: backfill # tag set via kustomize overlay
          resources:
            requests:
              cpu: "200m"
              memory: "256Mi"
            limits:
              cpu: "1"
              memory: "512Mi"
          securityContext:
            runAsNonRoot: true
            runAsUser: 1000
            readOnlyRootFilesystem: true
            allowPrivilegeEscalation: false
            capabilities:
              drop: ["ALL"]
          envFrom:
            - configMapRef:
                name: backfill-config
            - secretRef:
                name: backfill-secrets
          livenessProbe:
            exec:
              command: ["/bin/sh", "-c", "test -f /tmp/healthy"]
```

</details>

### CronJob (scheduled Job)
Use a `CronJob` for scheduled tasks.

See [Scheduled Tasks](./scheduled-tasks.md) for configuration examples.

**Best for:** daily/weekly batch runs, periodic maintenance.

<details>
<summary>Example: CronJob with best practices</summary>

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: nightly-report
spec:
  schedule: "0 2 * * *"
  concurrencyPolicy: Forbid
  successfulJobsHistoryLimit: 1
  failedJobsHistoryLimit: 1
  jobTemplate:
    spec:
      backoffLimit: 2
      template:
        spec:
          restartPolicy: Never
          containers:
            - name: report
              image: report # tag set via kustomize overlay
              resources:
                requests:
                  cpu: "100m"
                  memory: "128Mi"
                limits:
                  cpu: "500m"
                  memory: "256Mi"
              securityContext:
                runAsNonRoot: true
                runAsUser: 1000
                readOnlyRootFilesystem: true
                allowPrivilegeEscalation: false
                capabilities:
                  drop: ["ALL"]
              envFrom:
                - configMapRef:
                    name: report-config
                - secretRef:
                    name: report-secrets
              livenessProbe:
                exec:
                  command: ["/bin/sh", "-c", "test -f /tmp/healthy"]
```

</details>

---

## Deployment Strategies

Choose the right deployment strategy based on your risk tolerance, rollback requirements, and infrastructure capabilities.

### Rolling Update (Default)
Gradually replaces old pods with new ones, maintaining availability throughout the update.

**How it works:**
1. Creates new pods with the updated version
2. Waits for new pods to become ready
3. Terminates old pods
4. Repeats until all pods are updated

**Pros:**
- Zero downtime
- No additional infrastructure required
- Default Kubernetes behavior

**Cons:**
- Both versions run simultaneously during rollout
- Slower rollback (requires another rolling update)
- Cannot control traffic distribution between versions

<details>
<summary>Example: Rolling Update Configuration</summary>

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
spec:
  replicas: 5
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 2        # Max 2 extra pods during update
      maxUnavailable: 0  # Never reduce below desired count
  selector:
    matchLabels:
      app: my-app
  template:
    metadata:
      labels:
        app: my-app
        version: v2  # Update this with each deployment
    spec:
      containers:
        - name: app
          image: my-app:v2
          readinessProbe:
            httpGet:
              path: /health
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
```

**Configuration Tips:**
- `maxSurge: 1` and `maxUnavailable: 0` for conservative, zero-downtime updates
- `maxSurge: 100%` and `maxUnavailable: 0` for faster updates with double capacity temporarily
- Always set readiness probes to prevent traffic to unhealthy pods

</details>

### Blue/Green Deployment
Runs two identical production environments (blue and green). Deploy to the inactive environment, then switch all traffic at once.

**How it works:**
1. Deploy new version to the inactive environment (e.g., green)
2. Test the new version in the green environment
3. Switch router/load balancer to point to green
4. Keep blue running as a quick rollback option
5. Decommission blue after validation period

**Pros:**
- Instant rollback (just switch traffic back)
- Full testing of new version before user traffic
- Clear separation between versions

**Cons:**
- Requires double infrastructure during deployment
- Database migrations can be complex (must be compatible with both versions)
- More complex orchestration

<details>
<summary>Example: Blue/Green with Kubernetes Services</summary>

```yaml
# Blue Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app-blue
spec:
  replicas: 3
  selector:
    matchLabels:
      app: my-app
      version: blue
  template:
    metadata:
      labels:
        app: my-app
        version: blue
    spec:
      containers:
        - name: app
          image: my-app:v1.0.0
          ports:
            - containerPort: 8080
---
# Green Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app-green
spec:
  replicas: 3
  selector:
    matchLabels:
      app: my-app
      version: green
  template:
    metadata:
      labels:
        app: my-app
        version: green
    spec:
      containers:
        - name: app
          image: my-app:v2.0.0
          ports:
            - containerPort: 8080
---
# Service (switch selector to change active version)
apiVersion: v1
kind: Service
metadata:
  name: my-app
spec:
  selector:
    app: my-app
    version: green  # Change to 'blue' to rollback
  ports:
    - port: 80
      targetPort: 8080
```

**Switching Process:**
1. Deploy green deployment with new version
2. Test via direct pod access or temporary service
3. Update service selector from `version: blue` to `version: green`
4. Monitor for issues
5. Delete blue deployment after validation period

</details>

### Canary Deployment
Gradually shifts traffic from old version to new version, starting with a small percentage.

**How it works:**
1. Deploy new version alongside old version with fewer replicas
2. Route small percentage of traffic to new version
3. Monitor metrics (error rates, latency, etc.)
4. Gradually increase traffic to new version
5. If issues detected, route all traffic back to old version
6. Once validated, complete the rollout

**Pros:**
- Limits blast radius of bugs to small user percentage
- Real production traffic testing with low risk
- Data-driven rollout based on metrics

**Cons:**
- Requires traffic splitting capability (service mesh or ingress controller)
- More complex monitoring and decision logic
- Both versions run longer during gradual rollout

<details>
<summary>Example: Canary with Ingress Traffic Splitting</summary>

```yaml
# Stable Deployment (v1)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app-stable
spec:
  replicas: 9
  selector:
    matchLabels:
      app: my-app
      track: stable
  template:
    metadata:
      labels:
        app: my-app
        track: stable
        version: v1
    spec:
      containers:
        - name: app
          image: my-app:v1.0.0
---
# Canary Deployment (v2)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app-canary
spec:
  replicas: 1  # Start with 10% capacity (1 of 10 total pods)
  selector:
    matchLabels:
      app: my-app
      track: canary
  template:
    metadata:
      labels:
        app: my-app
        track: canary
        version: v2
    spec:
      containers:
        - name: app
          image: my-app:v2.0.0
---
# Stable Service
apiVersion: v1
kind: Service
metadata:
  name: my-app-stable
spec:
  selector:
    app: my-app
    track: stable
  ports:
    - port: 80
      targetPort: 8080
---
# Canary Service
apiVersion: v1
kind: Service
metadata:
  name: my-app-canary
spec:
  selector:
    app: my-app
    track: canary
  ports:
    - port: 80
      targetPort: 8080
---
# Ingress with traffic splitting (using NGINX Ingress)
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: my-app
  annotations:
    nginx.ingress.kubernetes.io/canary: "true"
    nginx.ingress.kubernetes.io/canary-weight: "10"  # 10% to canary
spec:
  rules:
    - host: myapp.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: my-app-canary
                port:
                  number: 80
---
# Main Ingress (90% traffic)
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: my-app-main
spec:
  rules:
    - host: myapp.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: my-app-stable
                port:
                  number: 80
```

**Canary Rollout Process:**
1. Deploy canary with `canary-weight: 10` (10% traffic)
2. Monitor error rates, latency in Grafana
3. If metrics look good after 30 min, increase to `canary-weight: 25`
4. Continue increasing: 50% → 75% → 100%
5. Once at 100%, delete stable deployment and promote canary to stable
6. If issues at any stage, set `canary-weight: 0` to rollback

</details>

### Recreate Deployment
Terminates all old pods before creating new ones. Results in downtime but ensures clean state.

**How it works:**
1. Terminate all pods of the old version
2. Wait for termination to complete
3. Create pods with new version

**Pros:**
- Simple and predictable
- No mixed-version state
- Guaranteed clean start

**Cons:**
- Causes downtime during deployment
- Not suitable for production services requiring high availability

**Best for:** Development/testing environments, batch jobs, or services with scheduled maintenance windows.

<details>
<summary>Example: Recreate Strategy</summary>

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
spec:
  replicas: 3
  strategy:
    type: Recreate  # All pods terminated before new ones created
  selector:
    matchLabels:
      app: my-app
  template:
    metadata:
      labels:
        app: my-app
    spec:
      containers:
        - name: app
          image: my-app:v2.0.0
```

</details>

### Comparison Table

| Strategy | Downtime | Infrastructure | Rollback Speed | Complexity | Best For |
|----------|----------|----------------|----------------|------------|----------|
| **Rolling Update** | None | 1x (+ surge) | Medium (new rollout) | Low | Most production services |
| **Blue/Green** | None | 2x | Instant (switch back) | Medium | Critical services, complex testing needs |
| **Canary** | None | 1x + canary | Fast (reduce traffic) | High | Risk-averse deployments, large user bases |
| **Recreate** | Yes | 1x | Medium (redeploy old) | Low | Dev/test, scheduled maintenance |

---

## GitOps Webhook Triggering (Flux)

Flux continuously reconciles from Git, and webhooks provide faster pull-triggering after changes are pushed.

- Use the **shared platform webhook hostname/certificate** for your cluster
- Create a **separate Receiver per app/repo** to get isolated webhook endpoints
- Point your app repo webhook to that app-specific endpoint

See [Webhooks](./webhooks.md) for the standard structure and an example.

---

## Shutting Down an Environment

When you need to temporarily shut down an environment (e.g., to reduce costs during extended periods of non-use), follow these steps:

### Recommended Approach: Scale to Zero

The cleanest way to shut down an environment is to scale your workloads to zero replicas:

**For Deployments and StatefulSets:**
```yaml
# In your k8s/<environment>/deployment.yaml or k8s/<environment>/statefulset.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
spec:
  replicas: 0  # Scale to zero
  # ... rest of configuration
```

**Steps:**
1. Edit your environment's Kubernetes manifests (e.g., `k8s/staging/deployment.yaml`)
2. Set `spec.replicas: 0` for all Deployments, StatefulSets, or DaemonSets
3. Commit and push changes:
   ```bash
   git add k8s/staging/
   git commit -m "chore: scale down staging environment"
   git push
   ```
4. FluxCD will reconcile and scale down your workloads
5. To restart, set `replicas` back to your desired count and push

**What stays running:**
- Services (no cost, just definitions)
- ConfigMaps and Secrets (no cost)
- PersistentVolumeClaims (storage costs continue - see below)

**What stops:**
- All pods and containers
- Compute costs for those resources

### Handling Persistent Storage

If your environment uses PersistentVolumeClaims (PVCs), scaling to zero does **not** delete the volumes:

- **Keep PVCs:** Data persists; storage costs continue (~$0.10/GB-month for gp3)
- **Delete PVCs:** To eliminate storage costs, contact **Infra-Argo-Global** via ServiceNow to request PVC deletion

**Warning:** This permanently deletes data. Ensure backups exist before requesting deletion.

### Alternative: Delete the Namespace

For long-term shutdown of an entire environment, contact **Infra-Argo-Global** via ServiceNow to delete the namespace.

**This removes:**
- All Deployments, StatefulSets, DaemonSets, Jobs, CronJobs
- All Services, ConfigMaps, Secrets
- All PersistentVolumeClaims (and associated volumes)

**To restore:**
1. FluxCD will automatically recreate the namespace and resources from Git
2. PVCs will be recreated as empty volumes (data loss if original PVCs were deleted)

### Best Practices

- **Document the shutdown:** Add a comment in your README or create a GitLab work item
- **Check external dependencies:** Ensure shutdown won't impact other teams/services
- **Back up data:** If deleting PVCs, back up databases or critical data first
- **Cost estimation:** Use Grafana dashboards and AWS Cost Explorer to estimate savings
- **Testing:** Test the restart procedure to ensure smooth startup

---

## Best practices (applies to most workloads)

1. **Set resource requests/limits** to ensure fair scheduling and avoid noisy-neighbor issues.
2. **Use readiness and liveness probes** so traffic only hits healthy pods.
3. **Use rolling updates** with `maxSurge`/`maxUnavailable` tuned to your SLOs.
4. **Keep pods stateless** where possible; use external storage/services for state.
5. **Pin images by version** and avoid `latest` tags.
    - Pin versions in kustomize overlays using `images.newTag`.
6. **Use PodDisruptionBudgets** to keep enough replicas available during maintenance.
7. **Spread replicas** across nodes/availability zones with topology spread constraints or anti-affinity.
8. **Separate config and secrets** from code using ConfigMaps and Secrets.
9. **Run containers as non-root** and use read-only filesystems (`securityContext` with `runAsNonRoot: true` and `readOnlyRootFilesystem: true`).
10. **Disable privilege escalation** and drop unnecessary capabilities (`allowPrivilegeEscalation: false`, `capabilities.drop: ["ALL"]`).
11. **Set priority classes** to ensure production workloads are scheduled before lower environments.

---

## Priority Classes

Priority classes ensure your production workloads take precedence over lower-environment workloads when cluster resources are constrained.

### Why Use Priority Classes?

**Problem:** During resource shortages, Kubernetes may delay scheduling high-priority production pods while lower-priority dev/testing pods consume resources.

**Solution:** Priority classes allow Kubernetes to:
- Schedule higher-priority pods first
- Preempt (evict) lower-priority pods to make room for critical workloads
- Ensure production availability during cluster congestion

### Available Priority Classes

Each cluster has pre-configured priority classes:

| Cluster | Priority Class | Value | Environment |
|---------|----------------|-------|-------------|
| TF-argo-dev | `priority-dev` | 250 | Dev |
| TF-argo-dev | `priority-testing` | 500 | Testing |
| TF-argo-prd | `priority-staging` | 750 | Staging |
| TF-argo-prd | `priority-production` | 1000 | Production |
| TF-argo-quarantine-prd | `priority-staging` | 750 | Staging |
| TF-argo-quarantine-prd | `priority-production` | 1000 | Production/Release |

**Priority Values:**
- Higher value = higher priority
- Pods without `priorityClassName` have priority 0 (lowest)
- Production (1000) > Staging (750) > Testing (500) > Dev (250)

### Configuration

Add `priorityClassName` to your pod template spec:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: my-app
  template:
    metadata:
      labels:
        app: my-app
    spec:
      priorityClassName: priority-production  # Add this line
      containers:
      - name: my-app
        image: my-app:1.0.0
```

### Environment-Specific Priorities with Kustomize

Use patches to set different priority classes per environment:

**k8s/base/deployment.yaml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
spec:
  replicas: 1
  selector:
    matchLabels:
      app: my-app
  template:
    metadata:
      labels:
        app: my-app
    spec:
      # No priorityClassName in base
      containers:
      - name: my-app
        image: my-app
```

**k8s/dev/deployment-patch.yaml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
spec:
  template:
    spec:
      priorityClassName: priority-dev
```

**k8s/testing/deployment-patch.yaml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
spec:
  template:
    spec:
      priorityClassName: priority-testing
```

**k8s/staging/deployment-patch.yaml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
spec:
  template:
    spec:
      priorityClassName: priority-staging
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
      priorityClassName: priority-production
```

**Add patches to kustomization.yaml:**
```yaml
# k8s/production/kustomization.yaml
resources:
- ../base
patchesStrategicMerge:
- deployment-patch.yaml
```

### How Preemption Works

**Scenario:** Cluster is at capacity, production pod needs to be scheduled.

1. **Kubernetes identifies lower-priority pods** that can be preempted
2. **Sends SIGTERM** to selected pods (graceful shutdown)
3. **Waits for `terminationGracePeriodSeconds`** (default: 30s)
4. **Forces termination** if pod doesn't exit gracefully
5. **Schedules high-priority pod** in freed resources

**Example:**
```
Cluster at capacity:
- 10 dev pods (priority-dev: 250)
- 5 testing pods (priority-testing: 500)
- 3 staging pods (priority-staging: 750)

Production pod arrives (priority-production: 1000)
→ Kubernetes preempts 2 dev pods to make room
→ Production pod schedules immediately
```

### Best Practices

1. **Always set priority classes** - Don't leave production workloads at default priority 0
2. **Use environment-specific patches** - Automate priority assignment via Kustomize
3. **Production = highest priority** - Use `priority-production` for all production workloads
4. **Match priorities to criticality** - Production > Staging > Testing > Dev
5. **Be consistent** - Use the same priority class for all pods in an environment

### Verification

**Check priority class assignment in Grafana:**
1. Open [Grafana](https://g-05c60f9e9b.grafana-workspace.us-east-1.amazonaws.com/)
2. Navigate to **Kubernetes / Compute Resources / Namespace (Pods)**
3. Filter by your namespace
4. View pod details to see priority class assignment

**Or view in your deployment YAML:**
Check that `priorityClassName` is set in your deployment manifest.

**View priority class details** by contacting **Infra-Argo-Global** via ServiceNow if needed.

### Troubleshooting

**Pod stuck in Pending with "Preemption" events:**

This is normal - Kubernetes is preempting lower-priority pods. Check in Grafana:
1. Navigate to **Kubernetes / Events** dashboard
2. Filter by your namespace
3. Look for preemption events

**Lower-priority pods being preempted frequently:**

Indicates cluster capacity issues - contact **Infra-Argo-Global** for assistance.
- Add more nodes to the cluster
- Reduce resource requests for lower environments
- Scale down non-production replicas

**For more details, see:**
- [TF-argo-dev Priority Classes](../platform-inventory/argo-dev.md#priority-classes)
- [TF-argo-prd Priority Classes](../platform-inventory/argo-prod.md#priority-classes)
- [Kubernetes Priority and Preemption](https://kubernetes.io/docs/concepts/scheduling-eviction/pod-priority-preemption/)

---

## Related guides
- [Zero-Downtime Deployments](./deployments.md)
- [High Availability](./high-availability.md)
- [Scheduled Tasks](./scheduled-tasks.md)
