---
label: Prerequisites & Requirements
icon: checklist
order: 130
---

# Compass CI Platform Prerequisites

Before onboarding your application to Compass CI, ensure your organization, team, and application meet the following requirements. This page covers both technical and organizational prerequisites.

### How to Read This Page

- **✅ REQUIRED** - Hard blockers. Your application/team must meet these to be eligible for Compass CI
- **🎯 RECOMMENDED** - Best practices with clear reasoning. Not hard blockers, but strongly suggested. We explain the tradeoffs and benefits

**Example:**
- ✅ REQUIRED: Application must be containerized
- 🎯 RECOMMENDED: Use Alpine Linux or distroless base images (smaller, more secure, but adds build complexity)

### Quick Overview: ✅ Must-Have vs 🎯 Best Practice

| Category | Required | Recommended |
|----------|----------|-------------|
| **Organizational** | App is in ServiceNow CMDB, <br/>Cost center assignment, Team commitment | — |
| **Architecture** | Containerized, Health check endpoints, Non-root user | Stateless design, Minimal base images |
| **Repository** | GitLab Dedicated, Deploy tokens, Protected branches |  |
| **Team** | Basic Git/Docker knowledge | Kubernetes basics, Security training |

**Key insight:** You need to meet all REQUIRED items to be eligible. RECOMMENDED items make your deployment smoother and your application more reliable, but aren't blockers.

---

## 🏢 Organizational Requirements

### ✅ SericeNow CMDB Configuration

All applications deployed to Compass CI must be registered in Medtronic's Configuration Management Database (CMDB) within ServiceNow. This is **critical for production deployments** and must be requested before going live.

**Important:** CMDB setup typically takes 1-2 weeks due to approval workflows. Start this process early—ideally while the platform team prepares your Flux configuration.

**What you need to do:**
1. Contact ServiceNow and request a **Business Application Configuration Item (CI)**
2. Provide application information and select environments (Dev, Testing, Staging, Production)
3. Define relationships to supporting infrastructure (Argo clusters, databases, authentication providers, etc.)
4. Work with IT and your business sponsor to complete compliance questionnaires

**For detailed instructions:** [!ref Request a CMDB Configuration Item](../supporting-docs/request-cmdb-configuration-item.md)

**Timeline:**
- Initial request submission: 30 minutes
- ServiceNow approval workflow: 1-2 weeks
- CI relationship setup: 1-2 hours
- **Total: Plan 2-3 weeks before targeting production deployment**

!!! warning "Production Blocker"
CMDB registration is required before production deployment. Do not wait until the last moment to submit your request—start as soon as you commit to Compass CI.
!!!

### ✅ Cost Center Assignment

Your application must be assigned to a cost center for operational and support cost allocation.

- **Monthly cost**: ~$530/month per team
- **Covers**: Support team, observability infrastructure (Grafana, logging), ongoing platform maintenance
- **Billing**: Charged monthly to your assigned cost center
- **You'll provide:** Valid cost center code during Phase 1 onboarding

**Who to contact:** Compass CI Platform Team - [dl.itargocoreteam@medtronic.com](mailto:dl.itargocoreteam@medtronic.com)

### ✅ Team Support Commitment

Your team must commit to:

- ✅ **Security Scanning**: Run vulnerability scans during CI pipeline and address issues timely
- ✅ **Dependency Updates**: Keep application dependencies current (security patches at minimum)
- ✅ **Platform Upgrades**: Accommodate scheduled Kubernetes and cluster upgrades (typically quarterly, planned in advance)
- ✅ **Health Check Maintenance**: Keep `/health` and `/ready` endpoints functional and responsive

This shared responsibility model ensures platform stability and security for all applications.

By onboarding, your team acknowledges the [Argo RACI](../application-go-live/argo-raci.md) and the shared responsibilities between application teams and platform teams.

---

## 🏗️ Application Architecture Requirements

### ✅ Containerized Application

Your application must be containerizable. This is a hard requirement for Compass CI.

**Requirements:**
- ✅ **Dockerfile**: Builds your application into a container image
- ✅ **Container Registry Access**: Push images to Medtronic's Artifactory container registry
- ✅ **Non-Root User**: Container runs as non-root for security
  - **Why?** If container is compromised, attacker has limited access
  - **Impact**: Significantly improves security posture

**Recommendations (Strong Best Practices):**
- 🎯 **Minimal Base Image**: Use Alpine Linux, distroless, or similar minimal images (not full OS images)
  - **Why?** Reduces attack surface, faster deployment, lower resource usage
  - **Impact**: 50-90% smaller images vs. full OS images

**Best Practice Example:**
```dockerfile
FROM alpine:3.18

# Run as non-root user
RUN addgroup -g 1001 -S appgroup && adduser -u 1001 -S appuser -G appgroup

WORKDIR /app
COPY . .

RUN apk add --no-cache python3

EXPOSE 8080

USER appuser

HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

CMD ["python3", "-m", "http.server", "8080"]
```

### 🎯 Stateless Architecture

**TL;DR:** Stateless is the preferred architecture for Compass CI. Stateful apps are supported but with limitations.

**What is stateless?**

Stateless means your application doesn't store persistent data locally—all state is stored externally (databases, caches, object storage). Each instance is independently replaceable.

```
❌ Storing user sessions in local files
❌ Saving uploaded files to container filesystem
❌ Writing application state to /tmp or /var
❌ Relying on sticky sessions or node affinity

✅ Using external storage (S3, shared databases) for persistence
✅ Storing sessions in Redis or DynamoDB
✅ Using the container filesystem only for application code
✅ Each instance is independently substitutable
```

**Why Stateless is Recommended:**

| Benefit | Stateless | Stateful |
|---------|-----------|----------|
| **Auto-scaling** | ✅ Multiple instances work seamlessly | ⚠️ Requires data synchronization |
| **Pod Restarts** | ✅ No data loss, instant recovery | ⚠️ Data may be lost or orphaned |
| **Rolling Updates** | ✅ Zero downtime, simple process | ⚠️ Complex coordination required |
| **Disaster Recovery** | ✅ Spin up new infrastructure instantly | ⚠️ Must restore data from backups |
| **Multi-Zone Deployment** | ✅ Works across any nodes | ⚠️ Requires replication setup |

**Can I run stateful applications?**

Yes, but with caveats:
- ✅ You can use **PersistentVolumes** for databases, message queues, etc.
- ✅ You can use **StatefulSets** for clustered systems (databases, Elasticsearch, etc.)
- ⚠️ Requires more careful architecture and operational expertise
- ⚠️ Limits scalability and flexibility
- ⚠️ Requires dedicated storage infrastructure

!!! Stateful Apps on Compass CI
If your application needs to be stateful, contact the platform team early to discuss architecture and storage options. We support it, but it requires additional configuration and has different deployment patterns.

Examples that typically require platform team involvement:
- PersistentVolumes or StorageClasses not already available to your namespace
- StatefulSets with ordered startup/shutdown or stable network identities
- Databases or message queues you want to run inside the cluster (instead of managed services)
- Cross-zone storage replication, backups, or snapshot/restore requirements
- Node affinity, anti-affinity, or dedicated node pools for stateful workloads

If you only need external persistence (for example Oracle, S3, or a managed cache) and your app itself stays stateless, you usually do not need a special review.
!!!

**Recommended Implementation:**

- **Session Management**: Use external session store (Redis, DynamoDB, database)
- **File Storage**: Use S3 or shared storage for uploaded files
- **Caching**: Use Redis or memcached for distributed caching
- **Database**: Store all persistent state in external databases

**Benefits of Stateless Architecture:**

- Pods can be stopped, restarted, or replaced instantly without data loss
- Auto-scaling works transparently—new instances join without data sync or rebalancing
- Disaster recovery is simpler (spin up new infrastructure, reconnect to external state stores)
- Easier to manage and debug—no hidden state on nodes

### ✅ Health Check Endpoints

Kubernetes automatically monitors applications using two HTTP health check endpoints. **Your application must implement these** for Compass CI to manage it safely.

**`GET /health` - Liveness Probe**
- Called every 10 seconds to verify the application is still running
- After 3 consecutive failures (30 seconds), Kubernetes restarts the pod
- Should return HTTP 200 if alive, any non-200 status triggers restart
- Keep this lightweight—don't perform heavy validation (just verify the process is running)

```python
# Python Flask Example
@app.route('/health')
def health():
    return {'status': 'ok'}, 200
```

**`GET /ready` - Readiness Probe**
- Called every 5 seconds to verify the application is ready to accept traffic
- After 3 consecutive failures (15 seconds), Kubernetes removes pod from load balancer
- Can perform more comprehensive checks here—verify database connectivity, cache availability, etc.
- Should return HTTP 200 when ready to serve requests

```python
# Python Flask Example
@app.route('/ready')
def ready():
    try:
        # Check database connectivity
        db.connection.ping()
        # Check cache connectivity
        cache.ping()
        return {'ready': True}, 200
    except Exception as e:
        return {'ready': False, 'reason': str(e)}, 503
```

**Configuration in Kubernetes Deployment:**
```yaml
livenessProbe:
  httpGet:
    path: /health
    port: 8080
  initialDelaySeconds: 30  # Wait 30s before first check
  periodSeconds: 10       # Check every 10s
  failureThreshold: 3     # Restart after 3 failures

readinessProbe:
  httpGet:
    path: /ready
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 5       # Check every 5s for faster failover
  failureThreshold: 3
```

!!! info "Can't implement health endpoints?"
While health checks are technically optional in Kubernetes, **Compass CI requires them** for safe production deployments. If your application can't implement HTTP endpoints, contact the platform team to discuss alternatives (exec probes, TCP probes).
!!!

---

## 🛠️ Technical Requirements

### ✅ Repository Requirements

- ✅ **GitLab Dedicated Repository**: Project hosted on medtronic.gitlab-dedicated.com
- ✅ **Protected Branches**: Enforce CI/CD pipeline and code reviews (dev, testing, staging, main)
- ✅ **Deploy Token**: For Flux to pull your manifests and application code
- ✅ **Semantic-Release Bot Token**: For automatic versioning and release creation

### ✅ Tooling & Access

Before configuring your repository, verify you have the required tools and access:

[!ref icon="tools" text="Tooling & Access Setup"](./tooling-and-access.md)

**Required:**
- ✅ Git client and GitLab account with repository access
- ✅ Docker Desktop or container runtime for building/testing locally
- ✅ Node.js 18+ (required for CI pipeline semantic-release jobs)
- ✅ Access to Medtronic's Artifactory for pushing container images

**Recommended:**
- 🎯 Helm knowledge (useful for advanced Kubernetes manifests, not required for basic onboarding)

## 📈 Support Model & Platform SLA

### What You Get

When onboarded to Compass CI, your team receives:

| Feature | Details |
|---------|---------|
| **Dedicated Platform Support** | 24/5 Business hours (Mon-Fri) + on-call weekends for P0/P1 incidents |
| **ServiceNow Assignment** | Tickets routed to: Infra-Argo-Global |
| **Full Observability** | Grafana dashboards, logs, metrics, alerts |
| **Incident Response** | Proactive monitoring and incident management |
| **Platform SLA** | 99.5% availability with maintenance windows communicated in advance |
| **Regular Upgrades** | Quarterly Kubernetes and infrastructure updates (scheduled with advance notice) |
| **Security Patching** | Proactive vulnerability patching for cluster and core services |

### Platform Maintenance & Downtime

The Compass CI platform is actively maintained and upgraded regularly:

- **Planned Maintenance**: Quarterly cluster patches
- **Your Action Required**: Ensure your application tolerates pod restarts during maintenance windows

**Your team's role:** Monitor platform notifications and be available during windows where applications may be restarted.

### Support Channels

- **Emergency (P0/P1)**: On-call through ServiceNow
- **Standard Issues & Support**: Create ServiceNow ticket with assignment group `Infra-Argo-Global`
- **Questions**: Email [dl.itargocoreteam@medtronic.com](mailto:dl.itargocoreteam@medtronic.com)

---

## ✅ Pre-Onboarding Checklist

### ✅ Your Organization

Before contacting the platform team, your organization must have:

- [ ] **CMDB Request Submitted** - Business Application CI request sent to ServiceNow (or plan to within 1 week)
- [ ] **Cost Center Identified** - Valid cost center for monthly chargeback (~$530/month)
- [ ] **Budget Approved** - Finance/Manager approval for platform costs
- [ ] **Business Sponsor Identified** - For ServiceNow compliance questions

### ✅ Your Application

- [ ] **Containerized** - Application builds and runs in Docker
- [ ] **Health Endpoints Implemented** - `/health` and `/ready` endpoints return HTTP 200 when healthy
- [ ] **GitLab Repository Ready** - Repository created on medtronic.gitlab-dedicated.com
- [ ] **Dockerfile Valid** - Builds successfully with no errors

### ✅ Your Team

- [ ] **Team Committed** - Understanding of ongoing support requirements (security scans, dependency updates, on-call)
- [ ] **On-Call Rotation** - Team has established on-call schedule for production incidents
- [ ] **GitLab Access** - All team members have GitLab accounts with repository access
- [ ] **Docker Knowledge** - Team familiar with Dockerfile and container basics

### 🎯 Application Best Practices

While not hard blockers, these significantly improve your experience on Compass CI:

- [ ] **Stateless Architecture** - No local state stored; all persistence is external
  - *Why?* Enables auto-scaling, simple pod restarts, and resilience
- [ ] **Minimal Base Image** - Dockerfile uses Alpine Linux, distroless, or similar minimal image
  - *Why?* Smaller images, faster deploys, reduced attack surface
- [ ] **Non-Root User** - Container runs as non-root user
  - *Why?* Improves security posture if image is compromised

### 🎯 Team Knowledge

- [ ] **Kubernetes Basics** - Team has basic understanding of Kubernetes concepts (pods, deployments, services)
- [ ] **Security Scanning** - Team aware of security scanning during CI pipeline and how to address findings
- [ ] **Grafana** - Team familiar with Grafana for monitoring application health and logs

---

## 📋 Next Steps

Once you've verified all REQUIRED items:

1. **[Submit CMDB Request Now](../supporting-docs/request-cmdb-configuration-item.md)** - Don't wait—takes 2-3 weeks before prod
2. **[Review the Getting Started Guide](./index.md)** - Detailed 2-phase setup guide
3. **[Get Tooling & Access Set Up](./tooling-and-access.md)** - Verify required tools and permissions
4. **[Create Your First Application](./gitlab-repository-configuration.md)** - 5-minute quick start

---

## ❓ Questions?

- **"What if my app isn't stateless?"** → While not required, stateless is strongly recommended. Contact platform team to discuss architecture
- **"How do I implement health checks?"** → See [Health Check Configuration](../getting-started/kubernetes-manifests.md#2-configure-health-checks)
- **"When must we do CMDB?"** → Before production (but start early—takes 2-3 weeks)
- **"Can we use a different base image?"** → Yes, but we recommend Alpine/distroless for security and efficiency
- **"More questions?"** → [!ref FAQ](../faq.md)
