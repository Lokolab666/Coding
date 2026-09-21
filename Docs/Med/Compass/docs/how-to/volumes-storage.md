---
label: Volumes & Storage
order: 80
icon: archive
---
# Volumes & Storage

Use this guide to understand storage options, when to use them, and how they compare to using a database.

## When to use volumes vs. a database

**Use a volume when you need:**
- Files that are read/written by your application (uploads, reports, temporary artifacts).
- Local cache or scratch space that doesn’t need strong query capabilities.
- Stable storage for a single pod or a single node.

**Use a database when you need:**
- Structured data with queries, indexing, and transactions.
- Concurrent reads/writes by multiple services.
- High availability, backups, and replication built into the data layer.

!!!warning
Volumes are not a replacement for a database. Use a database for shared, transactional, or query-heavy data.
!!!

---

## Storage options in Kubernetes

### ConfigMap / Secret (non-sensitive vs. sensitive config)
Use ConfigMaps and Secrets for configuration and credentials, not for large files or application data.

**Best for:** small config files, certificates, API keys.

<details>
<summary>Example: ConfigMap + Secret mounted read-only</summary>

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  APP_MODE: "production"
---
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
type: Opaque
stringData:
  API_KEY: "${API_KEY}"
---
apiVersion: v1
kind: Pod
metadata:
  name: config-example
spec:
  containers:
    - name: app
      image: my-app
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
            name: app-config
        - secretRef:
            name: app-secrets
      volumeMounts:
        - name: config-files
          mountPath: /etc/app/config
          readOnly: true
  volumes:
    - name: config-files
      configMap:
        name: app-config
```

</details>

### EmptyDir (ephemeral)
A temporary directory that exists only for the lifetime of a pod.

**Best for:** scratch space, caches that can be rebuilt.

<details>
<summary>Example: EmptyDir scratch space</summary>

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: scratch-example
spec:
  containers:
    - name: app
      image: my-app
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
        readOnlyRootFilesystem: false
        allowPrivilegeEscalation: false
        capabilities:
          drop: ["ALL"]
      volumeMounts:
        - name: scratch
          mountPath: /tmp
  volumes:
    - name: scratch
      emptyDir:
        sizeLimit: 1Gi
```

</details>

### PersistentVolumeClaim (PVC)
A request for durable storage that can outlive a pod restart.

**Best for:** files that must persist across pod restarts (single-writer or shared-writer when using RWX classes like `efs-sc`).

**Storage classes:**
- `efs-sc` (EFS, RWX): shared file storage that can be mounted by multiple pods; best for shared uploads, reports, static assets, and cross-pod access.
- `gp3` (EBS, RWO): block storage attached to a single node; best for single-pod workloads that need lower-latency storage (for example, single-instance databases or queues).

**Default/recommended for most cases:** `efs-sc` (shared file storage). Use `gp3` when you need single-pod block storage and do not need to share data across pods.

**PVC vs. object storage (S3-compatible):**
- Use a PVC when your app expects a "folder on disk" (for example: it writes files to `/data`, reads them back later, or a library requires a local path).
- Use object storage when your app can upload/download files by API (for example: reports, images, backups, or large files shared across multiple services).

<details>
<summary>Example: PVC using efs-sc</summary>

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: app-files
spec:
  accessModes: ["ReadWriteMany"]
  storageClassName: efs-sc
  resources:
    requests:
      storage: 10Gi
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: file-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: file-service
  template:
    metadata:
      labels:
        app: file-service
    spec:
      containers:
        - name: app
          image: file-service
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
            readOnlyRootFilesystem: false
            allowPrivilegeEscalation: false
            capabilities:
              drop: ["ALL"]
          volumeMounts:
            - name: app-files
              mountPath: /data
      volumes:
        - name: app-files
          persistentVolumeClaim:
            claimName: app-files
```

</details>

### StatefulSet + PVC (stable identity + storage)
Combines stable pod identities with dedicated persistent volumes.

**Best for:** stateful workloads that require stable hostnames and dedicated disks.

<details>
<summary>Example: StatefulSet with per-pod PVCs</summary>

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
      containers:
        - name: db
          image: my-db
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
            readOnlyRootFilesystem: false
            allowPrivilegeEscalation: false
            capabilities:
              drop: ["ALL"]
          volumeMounts:
            - name: data
              mountPath: /var/lib/data
  volumeClaimTemplates:
    - metadata:
        name: data
      spec:
        accessModes: ["ReadWriteOnce"]
        storageClassName: gp3
        resources:
          requests:
            storage: 20Gi
```

</details>

### Object storage (external to cluster)
Use object stores (e.g., S3-compatible storage) for large, durable files shared across services.

**Best for:** backups, large media files, reports, artifacts.

Object storage is accessed via API calls (not mounted like a volume). The example below shows how to inject endpoint and credentials; your app then uses an SDK or HTTP client to read/write objects.

<details>
<summary>Example: Object storage configuration</summary>

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: object-store-secrets
type: Opaque
stringData:
  ACCESS_KEY: "${ACCESS_KEY}"
  SECRET_KEY: "${SECRET_KEY}"
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: object-store-config
data:
  BUCKET_NAME: "app-artifacts"
  ENDPOINT_URL: "https://s3.example.com"
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: report-writer
spec:
  replicas: 2
  selector:
    matchLabels:
      app: report-writer
  template:
    metadata:
      labels:
        app: report-writer
    spec:
      containers:
        - name: app
          image: report-writer
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
                name: object-store-config
            - secretRef:
                name: object-store-secrets
```

</details>

**App usage example (SDK-style):**
```python
import os
import boto3

s3 = boto3.client(
  "s3",
  endpoint_url=os.environ["ENDPOINT_URL"],
  aws_access_key_id=os.environ["ACCESS_KEY"],
  aws_secret_access_key=os.environ["SECRET_KEY"],
)
s3.put_object(Bucket=os.environ["BUCKET_NAME"], Key="reports/daily.csv", Body=b"...")
```

---

## Best practices

1. **Prefer external storage for shared data** to avoid coupling data to a specific node.
2. **Use PVCs for durability** when data must survive restarts.
3. **Avoid large data in ConfigMaps/Secrets** (they are not intended for bulk storage).
4. **Limit write access** to the minimum required (single-writer when possible).
5. **Back up important data** using your platform’s standard backup process.

---

## Related guides
- [Database Requests](./database/)
- [Deployments](./deployments.md)
- [High Availability](./high-availability.md)
