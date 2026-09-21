---
label: Argo - Development
order: 100
---
# Argo - Development Cluster

Live instance of the Argo development cluster hosting non-production environments (DEV, TESTING).

## Cluster Information

**Name:** TF-argo-dev
**Environments:** DEV, TESTING
**Region:** us-east-1
**VPC:** 10.210.0.0/16

---

## Application URL Patterns

Applications deployed to the argo-dev cluster follow this URL pattern:

```
https://{app-name}.{environment}.argo-dev.eks.mdtcloud.io
```

**Examples:**

| Environment | URL Pattern | Example |
|-------------|-------------|----------|
| Dev | `https://{app-name}.dev.argo-dev.eks.mdtcloud.io` | `https://einstein.dev.argo-dev.eks.mdtcloud.io` |
| Testing | `https://{app-name}.testing.argo-dev.eks.mdtcloud.io` | `https://einstein.testing.argo-dev.eks.mdtcloud.io` |

---

## Priority Classes

The cluster has PriorityClasses pre-configured to ensure production workloads take precedence over lower environments during resource constraints.

**Available Priority Classes:**

| Priority Class | Value | Environment | Description |
|----------------|-------|-------------|-------------|
| `priority-dev` | 250 | Dev | Lowest priority - can be preempted by all other environments |
| `priority-testing` | 500 | Testing | Low priority - can be preempted by staging and production |

**How Priority Works:**
- When the cluster runs low on resources, Kubernetes will preempt (evict) lower-priority pods to make room for higher-priority pods
- Pods without a priorityClassName have priority 0 (lowest)
- Higher values = higher priority

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
      priorityClassName: priority-dev  # Set based on environment
      containers:
      - name: my-app
        image: my-app:1.0.0
```

**Environment-Specific Configuration:**

Use Kustomize patches to set different priorities per environment:

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

[!ref For complete deployment configuration examples, see Deployments guide](../how-to/deployments.md#priority-classes)

---

## Flux Webhook Receiver

Use this endpoint to configure GitLab webhooks for push-based Flux syncs.

- **Endpoint:** `https://flux-webhook-ext.argo-dev.eks.mdtcloud.io/hook/759845e9ae27b5c84bb620cbd6a473bbe36ce355008cdc8f724130472d010f7a`
- **Type:** GitLab
- **Scope:** TF-argo-dev (DEV, TESTING)

See the webhook setup steps in [GitLab Repository Configuration](../getting-started/gitlab-repository-configuration.md#webhooks-optional).

---

## Network Configuration

### App Subnet CIDRs

- 10.210.91.0/26
- 10.210.90.192/26
- 10.210.91.64/26

### NAT Gateway IPs

- 10.210.90.254
- 10.210.91.31

---

## Connectivity Inventory

Test connectivity using the connectivity tester tool inside this cluster:
- [Argo Dev Cluster Connectivity Tester](https://einstein.argo-dev.eks.mdtcloud.io/einstein/network)

### Outbound Connections

| Destination IP | Destination FQDN | Port | TLS? | Description | Completed? | Request Date | Notes |
|---|---|---|---|---|---|---|---|
| 10.21.183.152 | mspldb291.corp.medtronic.com | 2484 | yes | Access to WEB12T database with SSL | Yes | | |
| 10.21.183.170 | mspldb515.corp.medtronic.com | 2484 | yes | Access to WEB12S database with SSL | Yes | | |
| 10.225.68.31 | msprdb167.corp.medtronic.com | 2484 | yes | Access to IPUBD4 database with SSL | Yes | | |
| 10.225.68.32 | msprdb169.corp.medtronic.com | 2484 | yes | Access to IPUBT4 database with SSL | Yes | | |
| external | pxlcloud-medtronic-dev2.perceptive.com | 443 | yes | Access to PXL Cloud for InsightProductAssignment | Yes | | |
| external | pxlcloud-medtronic-uat2.perceptive.com | 443 | yes | Access to PXL Cloud for InsightProductAssignment | Yes | | |
| 10.21.184.78 | mspr47.corp.medtronic.com | 1521 | no | Access to DIHT database | Yes | | |
| 10.21.184.28 | mspr49.corp.medtronic.com | 1521 | no | Access to DIHS database | Yes | | |
| 10.21.184.xx | msplex01-scan.corp.medtronic.com | 1521 | no | Access to DIHR/DIHT2/DIHI2 database | Yes | | |
| 10.21.184.xx | msplex01-scan.corp.medtronic.com | 2484 | yes | Access to DIHR/DIHT2/DIHI2 database with SSL | Yes | | |
| 144.15.16.42 | msaws-test.medtronic.com | 443 | yes | Access to IDM web service | Yes | 11/8/2022 | |
| external | webapi-test-eu.medtronic.com | 443 | yes | Access to Mulesoft | Yes | | |
| external | medtronic.okta.com, login.medtronic.com, medtronic-stg.okta.com, stage.login.medtronic.com, medtronic-test.oktapreview.com, test.login.medtronic.com, medtronic-dev.oktapreview.com, dev.login.medtronic.com | 443 | yes | Access to Okta | Yes | | |
| 10.51.76.33 | mspmxdb335.wpn.medtronic.com | 41430 | yes | MS SQL Server DB (CLM / MeDocs) | Yes | | |
| 144.15.208.143 | dctmmedocsserv.medtronic.com | 443 | yes | Documentum VIP connection | Yes | | |
| 144.15.172.128 | edc-val2.servers.medtronic.com | 443 | yes | RDCSSO VIP (10.51.232.96 & 10.51.232.98) | No | 02/10/2023 | REQ2435817/RITM3070566 |
| 144.15.172.126 | edc-dev2-servers.medtronic.com | 443 | yes | RDCSSO VIP (10.51.232.78 & 10.51.232.80) | No | 02/10/2023 | REQ2435817/RITM3070566 |
| 10.51.232.79 | mspmxopd13.wpn.medtronic.com | 443 | yes | Direct server access | No | 02/10/2023 | REQ2435817/RITM3070566 |
| external | demo.docusign.net | 443 | yes | Digital FCA | Yes | 01/26/2023 | REQ2414137 - completed 01/31/2023 |

---

## Infrastructure Diagram

Download the architecture diagram for the Argo Dev cluster:

[!file Dev Cluster Infrastructure Diagram](../static/architecture/IT-Argo-Dev-Infrastructure-Diagram.drawio)

This diagram shows the complete network topology, load balancers, ingress configuration, and application flow for the development cluster.

---

## Related Resources

- [Connectivity Request Process](../how-to/connectivity-request-process.md)
- [Deployments](../how-to/deployments.md)
