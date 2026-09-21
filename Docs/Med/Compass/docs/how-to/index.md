---
label: How-To Guides
icon: book
order: 90
---

# How-To Guides

Step-by-step instructions for common configuration tasks. Each guide explains what you're configuring and why, with examples you can copy and customize.

## Quick Links

[!ref icon="container" text="Deployments"](./deployments.md)
[!ref icon="server" text="Service Configuration"](./service-configuration.md)
[!ref icon="archive" text="Volumes & Storage"](./volumes-storage.md)
[!ref icon="key" text="Secrets Management"](./secrets-management.md)
[!ref icon="pulse" text="Health Checks"](../getting-started/kubernetes-manifests.md#2-configure-health-checks)
[!ref icon="shield" text="Authentication"](./authentication.md)
[!ref icon="globe" text="Web Access & Hostnames"](./web-access-hostnames.md)
[!ref icon="broadcast" text="Webhooks"](./webhooks.md)
[!ref icon="link" text="Connectivity"](./connectivity-request-process.md)
[!ref icon="clock" text="Scheduled Tasks"](./scheduled-tasks.md)
[!ref icon="zap" text="High Availability"](./high-availability.md)

---

## Configuration Guides

### Core Configuration
- **[Deployments](./deployments.md)** - Choose the right workload type and rollout strategy
- **[Service Configuration](./service-configuration.md)** - Expose your application within the cluster
- **[Volumes & Storage](./volumes-storage.md)** - Persistent and ephemeral storage options
- **[Secrets Management](./secrets-management.md)** - Securely store and access credentials
- **[Health Checks](../getting-started/kubernetes-manifests.md#2-configure-health-checks)** - Configure application health monitoring

### Exposing Your Application
- **[Web Access & Hostnames](./web-access-hostnames.md)** - Configure custom domains for your application
- **[Webhooks](./webhooks.md)** - Configure app-specific Git webhook endpoints on the shared platform hostname

### Security
- **[Authentication](./authentication.md)** - Configure SSO and user authorization
- **[Connectivity](./connectivity-request-process.md)** - Request connectivity to internal and external services

### Advanced Workloads
- **[Scheduled Tasks](./scheduled-tasks.md)** - Run tasks on a schedule or one-time

### Monitoring & Reliability
- **[Logging](../monitoring/03-logging.md)** - Collect and view application logs
- **[Metrics](../monitoring/04-metrics-and-dashboards.md)** - View application metrics

### Performance & Scaling
- **[Resource Allocation](./resource-allocation.md)** - Set CPU and memory requirements
- **[High Availability](./high-availability.md)** - Auto-scaling and redundancy
- **[Zero-Downtime Deployments](./deployments.md)** - Deploy without service interruption
