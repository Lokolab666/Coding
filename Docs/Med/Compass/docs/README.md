---
label: Welcome
icon: home
title: Compass CI Playbook
order: 300
---

# ![Compass Logo Image|100x100](/static/medtronic-branding/compass-logo-elec-no-text.png) Compass CI Playbook

**Modern, Automated CI/CD for Medtronic Applications**

Welcome to Compass CI - a GitOps-based continuous integration and deployment platform that automates building, testing, securing, and deploying your applications to Kubernetes.

---

## ⚠️ Before You Start

**Compass CI is right for you if your application:**
- Can be containerized
- Follows stateless architecture patterns
- Has health check endpoints (or can implement them)
- Your team is committed to ongoing security and dependency maintenance
- Your organization has assigned a cost center

**New applications should start here:** [Prerequisites & Requirements](getting-started/prerequisites.md) - Ensures you're ready before investing time in onboarding.

**Already read the prerequisites?** Continue below 👇

---

## 🚀 Quick Start

New to Compass CI? Start here:

[!ref icon="rocket" text="Getting Started Guide"](getting-started/)
[!ref icon="book" text="How-To Guides"](how-to/)
[!ref icon="question" text="FAQ"](faq.md)

---

## 📚 Documentation Sections

### 1. [!badge variant="success" text="Getting Started"](getting-started/)
Everything you need to onboard your application:
- Required files and configuration
- Tool access
- Branch strategy
- Sample templates

### 2. [!badge variant="info" text="How-To Guides"](how-to/)
Step-by-step instructions for common tasks:
- Ingress configuration
- External Secrets (ESO)
- Kubernetes CronJobs
- Health checks
- Authentication

### 3. [!badge variant="danger" text="Security"](security/)
Automated security scanning and policy enforcement:
- Policy evaluation process
- Vulnerability remediation
- Exception requests
- Best practices

### 4. [!badge variant="warning" text="Go-Live"](application-go-live/go-live-checklist.md)
Production readiness activities:
- Go-live checklist
- Cloud governance review
- Architecture diagrams
- RACI matrix

---

## 🎯 Common Tasks

[!ref icon="globe" text="Configure Custom Hostname"](how-to/web-access-hostnames.md)

[!ref icon="broadcast" text="Configure Flux Webhook"](how-to/webhooks.md)

[!ref icon="key" text="Set Up Secrets Management"](how-to/secrets-management.md)

[!ref icon="pulse" text="Configure Health Checks"](getting-started/kubernetes-manifests.md#2-configure-health-checks)

[!ref icon="shield" text="Request Security Exception"](security/policy-exception-request.md)
[!ref icon="cpu" text="AI-Assisted Development Use Cases"](getting-started/ai-assisted-development.md)

[!ref icon="database" text="Request Database"](https://cds-playbook.argo-dev.eks.mdtcloud.io/database-onboarding/)

[!ref icon="flame" text="Submit Firewall Request"](how-to/connectivity-request-process.md)

---

## 🔍 By Technology

**Containers & Kubernetes**
- [Dockerfile best practices](getting-started/containerization.md)
- [Kubernetes manifests](getting-started/kubernetes-manifests.md)
- [Resource limits](how-to/resource-allocation.md)

**CI/CD Pipeline**
- [Pipeline Stages & Jobs](cicd-pipeline/pipeline-stages.md)
- [Semantic Versioning](cicd-pipeline/semantic-versioning.md)
- [GitLab Environments](cicd-pipeline/gitlab-environments.md)
- [Security & GitLab Work items](cicd-pipeline/security-gitlab-issues.md)
- [AI-Assisted Development](getting-started/ai-assisted-development.md)

**Security & Compliance**
- [Secrets management](how-to/secrets-management.md)
- [Authentication](how-to/authentication.md)
- [SSL certificates](how-to/web-access-hostnames.md)

---

## 💬 Support

**Need Help?**

[!ref icon="people" text="Contact Information"](troubleshooting-and-support/contacts.md)
[!ref icon="question" text="Troubleshooting Guide"](troubleshooting-and-support/troubleshooting-guide.md)

---

## 📖 Additional Resources

[!ref icon="database" text="Database Schemas"](how-to/database/index.md)
[!ref icon="graph" text="Monitoring & Logging"](monitoring/)
[!ref icon="tools" text="References"](references/cyberark-process.md)
