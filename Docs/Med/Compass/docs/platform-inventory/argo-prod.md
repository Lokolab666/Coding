---
label: Argo - Production
order: 90
---
# Argo - Production & Quarantine Clusters

Live instances of the Argo production clusters hosting production and staging environments.

## Cluster Information

**Names:** TF-argo-prd, TF-argo-quarantine-prd
**Environments:** STAGING, RELEASE, PRODUCTION
**Region:** us-east-1
**VPC:** 10.210.0.0/16

---

## Application URL Patterns

### TF-argo-prd Cluster

Applications deployed to the TF-argo-prd cluster follow this URL pattern:

```
https://{app-name}.{environment}.argo-prd.eks.mdtcloud.io
```

**Examples:**

| Environment | URL Pattern | Example |
|-------------|-------------|----------|
| Staging | `https://{app-name}.staging.argo-prd.eks.mdtcloud.io` | `https://einstein.staging.argo-prd.eks.mdtcloud.io` |
| Release | `https://{app-name}.release.argo-prd.eks.mdtcloud.io` | `https://einstein.release.argo-prd.eks.mdtcloud.io` |
| Production | `https://{app-name}.argo-prd.eks.mdtcloud.io` | `https://einstein.argo-prd.eks.mdtcloud.io` |

**Note:** It is recommended to omit the environment subdomain for the Production environment as shown above.

### TF-argo-quarantine-prd Cluster

Applications deployed to the TF-argo-quarantine-prd cluster follow this URL pattern:

```
https://{app-name}.{environment}.argo-quarantine-prd.eks.mdtcloud.io
```

**Examples:**

| Environment | URL Pattern | Example |
|-------------|-------------|----------|
| Staging | `https://{app-name}.staging.argo-quarantine-prd.eks.mdtcloud.io` | `https://einstein.staging.argo-quarantine-prd.eks.mdtcloud.io` |
| Release | `https://{app-name}.release.argo-quarantine-prd.eks.mdtcloud.io` | `https://einstein.release.argo-quarantine-prd.eks.mdtcloud.io` |
| Production | `https://{app-name}.argo-quarantine-prd.eks.mdtcloud.io` | `https://einstein.argo-quarantine-prd.eks.mdtcloud.io` |

**Note:** It is recommended to omit the environment subdomain for the Production environment as shown above.

---

## Priority Classes

The clusters have PriorityClasses pre-configured to ensure production workloads take precedence over lower environments during resource constraints.

**Available Priority Classes:**

| Priority Class | Value | Environment | Description |
|----------------|-------|-------------|-------------|
| `priority-staging` | 750 | Staging | High priority - only preempted by production |
| `priority-production` | 1000 | Production | Highest priority - never preempted |

**How Priority Works:**
- When the cluster runs low on resources, Kubernetes will preempt (evict) lower-priority pods to make room for higher-priority pods
- Pods without a priorityClassName have priority 0 (lowest)
- Higher values = higher priority
- Production workloads should **always** use `priority-production`

**Setting Priority Class:**

Add `priorityClassName` to your deployment spec:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
spec:
  template:
    spec:
      priorityClassName: priority-production  # Set based on environment
      containers:
      - name: my-app
        image: my-app:1.0.0
```

**Environment-Specific Configuration:**

Use Kustomize patches to set different priorities per environment:

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

**k8s/release/deployment-patch.yaml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
spec:
  template:
    spec:
      priorityClassName: priority-production  # Release uses production priority
```

[!ref For complete deployment configuration examples, see Deployments guide](../how-to/deployments.md#priority-classes)

---

## Flux Webhook Receiver

Use this endpoint to configure GitLab webhooks for push-based Flux syncs.

- **Endpoint (TF-argo-prd):** `https://flux-webhook-ext.argo-prd.eks.mdtcloud.io/hook/381ef3306c918f1d1f2c32e2997bd84accf1903953bcf00a1def5e6412b6a7ca`
- **Endpoint (TF-argo-quarantine-prd):** `https://flux-webhook-ext.argo-quarantine-prd.eks.mdtcloud.io/hook/65a657e4736eaf700c81b4fc155904b754f454d7205a2987efae8981e2893c04`
- **Type:** GitLab
- **Scope:** TF-argo-prd (STAGING, RELEASE, PRODUCTION)

See the webhook setup steps in [GitLab Repository Configuration](../getting-started/gitlab-repository-configuration.md#webhooks-optional).

---

## Network Configuration

### App Subnet CIDRs

- 10.210.190.192/26
- 10.210.191.0/26
- 10.210.191.64/26

### NAT Gateway IPs

- 10.210.191.11
- 10.210.190.214

---

## Connectivity Inventory

Test connectivity using the connectivity tester tool inside this cluster:
- [Argo Quarantine/Prod Connectivity Tester](https://einstein.argo-prd.eks.mdtcloud.io/einstein/network)

### Outbound Connections

| Destination IP | Destination FQDN | Port | TLS? | Description | Completed? | Request Date | Notes |
|---|---|---|---|---|---|---|---|
| 10.21.183.152 | mspldb291.corp.medtronic.com | 2484 | yes | Access to WEB12T database with SSL | Yes | | Completed 01-12-2023 |
| 10.21.183.170 | mspldb515.corp.medtronic.com | 2484 | yes | Access to WEB12S database with SSL | Yes | | Completed 01-12-2023 |
| 10.51.68.5 | mspldb305.corp.medtronic.com | 2484 | yes | Access to WEB12 database with SSL | No | | Pending patching on 305 and inc for db connection |
| 10.225.68.31 | msprdb167.corp.medtronic.com | 2484 | yes | Access to IPUBD4 database with SSL | Yes | | |
| 10.225.68.32 | msprdb169.corp.medtronic.com | 2484 | yes | Access to IPUBT4 database with SSL | Yes | | |
| external | pxlcloud-medtronic-dev2.perceptive.com | 443 | yes | Access to PXL Cloud for InsightProductAssignment | Yes | | |
| external | pxlcloud-medtronic-uat2.perceptive.com | 443 | yes | Access to PXL Cloud for InsightProductAssignment | Yes | | |
| 10.21.184.78 | mspr47.corp.medtronic.com | 1521 | no | Access to DIHT database | Yes | | |
| 10.21.184.28 | mspr49.corp.medtronic.com | 1521 | no | Access to DIHS database | Yes | | |
| 10.21.184.xx | msplex01-scan.corp.medtronic.com | 1521 | no | Access to DIHR/DIHT2/DIHI2 database | Yes | | |
| 10.21.184.xx | msplex01-scan.corp.medtronic.com | 2484 | yes | Access to DIHR/DIHT2/DIHI2 database with SSL | Yes | | |
| external | webapi-test-eu.medtronic.com | 443 | yes | Access to Mulesoft | Yes | | |
| external | medtronic.okta.com, login.medtronic.com, medtronic-stg.okta.com, stage.login.medtronic.com, medtronic-test.oktapreview.com, test.login.medtronic.com, medtronic-dev.oktapreview.com, dev.login.medtronic.com | 443 | yes | Access to Okta | Yes | | |
| 10.51.76.33 | mspmxdb335.wpn.medtronic.com | 41430 | yes | MS SQL Server DB (CLM / MeDocs) | Yes | | |
| 144.15.208.143 | dctmmedocsserv.medtronic.com | 443 | yes | Documentum VIP connection | Yes | | INC9160512 |
| 144.15.172.128 | edc-val2.servers.medtronic.com | 443 | yes | RDCSSO VIP (10.51.232.96 & 10.51.232.98) | No | TBD | |
| 144.15.172.126 | edc-dev2-servers.medtronic.com | 443 | yes | RDCSSO VIP (10.51.232.78 & 10.51.232.80) | No | TBD | |
| 10.51.232.79 | mspmxopd13.wpn.medtronic.com | 443 | yes | Direct server access | No | TBD | |
| external | na3.docusign.net | 443 | yes | Digital FCA | Yes | | |
| external | account.docusign.com | 443 | yes | Digital FCA | Yes | | |
| external | api.docusign.net | 443 | yes | Digital FCA | Yes | | |
| 10.51.112.23 | msplwb280.corp.medtronic.com | 443 | yes | Access to AEM | Yes | | |
| 144.15.216.32 | cqauthor.medtronic.com | 443 | yes | Access to AEM | Yes | | |
| 144.15.232.28 | mft.medtronic.com | 443 | no | Access to WebMethods | Yes | | |
| 144.15.232.20 | tn.medtronic.com | 443 | no | Access to WebMethods | Yes | | |
| 10.208.135.107 / 10.208.146.90 | webapi-int.medtronic.com | 442 | yes | Access to MuleSoft API | Yes | | |

---

## Infrastructure Diagram

Download the architecture diagram for the Argo Prod/Quarantine clusters:

[!file Prod/Quarantine Cluster Infrastructure Diagram](../static/architecture/IT-Argo-Prod-Infrastructure-Diagram.drawio)

This diagram shows the complete network topology, load balancers, ingress configuration, and application flow for the production and quarantine clusters.

---

## Related Resources

- [Connectivity Request Process](../how-to/connectivity-request-process.md)
- [Deployments](../how-to/deployments.md)
