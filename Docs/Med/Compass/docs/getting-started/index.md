---
label: Getting Started
icon: rocket
order: 100
---

# Getting Started with Compass CI

Welcome to Compass CI - Medtronic's modern, GitOps-based continuous integration and deployment platform. This guide will help you onboard your application to our automated pipeline.

## Overview

Compass CI uses:
- **GitLab CI/CD** for pipeline execution
- **Semantic Release** for automated versioning
- **Flux CD** for GitOps-based deployment
- **Kubernetes** on AWS EKS for container orchestration

## Key Benefits

By joining the Compass CI platform, your team gains:

### 🛡️ Best Practices Enforced
- **Standardized Deployment Patterns** - Security and reliability best practices are built into every deployment
- **Automated Security Scanning** - Container image scanning and vulnerability detection integrated into your pipeline
- **Consistent Configuration** - RBAC, network policies, and environment isolation applied uniformly across all applications
- **Policy-Driven Governance** - Compliance requirements are enforced through the platform, not manual reviews

### 📦 Built-In Versioning & Release Management
- **Semantic Versioning** - Automated version numbering based on commit messages (e.g., v1.2.3)
- **Git-Integrated Releases** - Version tags and release notes automatically created in your GitLab repository
- **Standardized Release Process** - Same release workflow for all teams, reducing inconsistencies and errors
- **Automatic Changelog Generation** - Release notes generated automatically from your commit history

### 📈 Scalability & Reliability
- **Auto-Scaling Infrastructure** - Applications automatically scale based on demand
- **High Availability** - Multi-zone deployment with automatic failover
- **Zero-Downtime Deployments** - Rolling updates ensure continuous availability during releases
- **Load Balancing** - Automatic distribution of traffic across application instances
- **Disaster Recovery (DR)** - Infrastructure as Code (IaC) enables rapid recovery from failures and consistent environment recreation across regions

### 🔧 Platform Maintenance Included
- **Regular Cluster Upgrades** - Kubernetes and infrastructure updates handled by the platform team
- **Security Patches** - Vulnerabilities patched proactively without disrupting your applications
- **Infrastructure Optimization** - Ongoing performance tuning and cost optimization
- **99.5% Platform SLA** - Committed availability with dedicated support team

---

## Platform Support & Costs

### What's Included
When you onboard to the Compass CI platform, your team receives comprehensive support and observability services:

- **24/5 Dedicated Support** - Business hours coverage with weekend on-call for critical P0/P1 issues (ServiceNow Assignment Group: Infra-Argo-Global)
- **Full Observability Suite** - Monitoring, logging, and alerting for your applications via Grafana dashboards
- **Proactive Incident Management** - Minimize downtime and impact to your users
- **Platform Maintenance** - Regular cluster upgrades, security patches, and infrastructure improvements

### Cost Structure
The platform operates on a transparent cost allocation model to ensure sustainability and continued enhancement of services:

- **Estimated Cost**: ~$530/month per team
- **What It Covers**: Full support team, observability infrastructure, and platform operations
- **Economies of Scale**: Cost per team decreases as more teams join the platform
- **Billing**: Charged to the cost center you provide during onboarding

!!! Cost Center Required
You'll need to provide a valid cost center during Phase 1 onboarding for operational and support cost allocation.
!!!

For questions about platform costs or billing, contact the [Platform Team](mailto:dl.itargocoreteam@medtronic.com).

## Before You Start: Verify Prerequisites

**Don't skip this!** Before onboarding, ensure your application and organization meet Compass CI requirements. This prevents delays and ensures successful deployment.

[!ref icon="checklist" text="Prerequisites & Requirements"](./prerequisites.md)

Key things to verify:
- ✅ Application is containerized and stateless
- ✅ Health check endpoints implemented
- ✅ CMDB request submitted to ServiceNow (takes 2-3 weeks)
- ✅ Cost center assigned
- ✅ Team committed to ongoing maintenance and support

---

## Onboarding Process

### Phase 1: Initial Setup (Platform Team)
Once prerequisites are verified, contact the Compass CI platform team with the following information to set up initial infrastructure:
- Application Name
- Team Name
- Cost Center (for chargeback of operational and support costs)
- Which application environments do you need? Recommendation is 3 - (dev, staging, production)
  - [ ] Dev
  - [ ] Testing
  - [ ] Staging
  - [ ] Release
  - [ ] Production
- Which cluster(s) will your application be deployed to?
  - If you have your own, we will onboard those cluster(s) to the platform
  - If not provided, they will be onboarded to the multi-tenant TF-argo-* clusters
- GitLab dedicated project repository link (e.g. https://medtronic.gitlab-dedicated.com/bcp_web/common/einstein.git)
  - [ ] Create a deploy token for your project and provide to the platform team (found under project `Settings > Repository > Deploy tokens`)

The platform team will set up required files to sync your project to the cluster(s):
- Kubernetes namespace configuration
- Flux CD GitRepository and Kustomization resources
- RBAC and network policies
- Webhook URL / secret to provide to the project team
- Grafana dashboard setup

**Contact:** [Platform Team](mailto:dl.itargocoreteam@medtronic.com)

### Tooling & Access Setup
Before configuring your repository, ensure you have the required tools and access.

[!ref icon="tools" text="Tooling & Access Setup"](./tooling-and-access.md)

### Phase 2: Repository Configuration (Your Team)
Configure your application repository with required files, branch protections, and project settings.

[!ref icon="repo" text="Phase 2: Repository Configuration"](./gitlab-repository-configuration.md)

---

## Next Steps

Once you have your files configured:

[!ref icon="cpu" text="AI-Assisted Development"](./ai-assisted-development.md)
[!ref icon="check" text="Configure Health Checks"](../getting-started/kubernetes-manifests.md#2-configure-health-checks)
[!ref icon="globe" text="Set Up Ingress"](../how-to/web-access-hostnames.md)
[!ref icon="key" text="Configure Secrets"](../how-to/secrets-management.md)
[!ref icon="shield" text="Review Security Policies"](../cicd-pipeline/security-gitlab-issues.md)
[!ref icon="rocket" text="Go Live"](../application-go-live/go-live-checklist.md)

---

## Questions?

[!ref icon="question" text="FAQ"](../faq.md)
[!ref icon="people" text="Contact Support"](../troubleshooting-and-support/contacts.md)
