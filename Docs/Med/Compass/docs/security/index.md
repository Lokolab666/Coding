---
label: Security
icon: shield
order: 70
---

# Security

Compass CI integrates automated security scanning and policy enforcement into your CI/CD pipeline to ensure applications meet Medtronic's security standards. All vulnerabilities are automatically tracked as GitLab Work items with remediation timelines based on your application's data classification.

---

## Overview

Security is integrated throughout the CI/CD pipeline with:

### 🔍 Automated Scanning

Every pipeline run includes multiple security scanners:
- **Container Scanning (Trivy)** - Scans Docker images for OS and application vulnerabilities
- **SAST (Static Analysis)** - Analyzes source code for security issues (SQL injection, XSS, hardcoded secrets)
- **Dependency Scanning** - Checks application dependencies for known vulnerabilities
- **Contrast Security** - Runtime security analysis for supported languages

### 📋 Vulnerability Tracking

All discovered vulnerabilities are automatically:
- Created as GitLab Work items in your repository
- Labeled by severity (critical, high, medium, low)
- Assigned remediation due dates
- Linked to CVE databases and remediation guidance

### 🛡️ Policy Enforcement

Based on your application's `DATA_CLASSIFICATION`, the pipeline:
- Blocks deployments with overdue vulnerabilities
- Enforces remediation timelines
- Validates policy exception requests (PERs)
- Ensures compliance with GCISO standards

---

## Remediation Requirements

Remediation requirements are based on your application's data classification per [GCISO Application Security Testing standards](https://medtronic.sharepoint.com/sites/GSO/SitePages/Application-Security-Testing.aspx):

**HIGHLY_SENSITIVE or SENSITIVE applications:**
Must resolve all Critical, High, and Medium findings before production. Low findings require an agreed resolution plan.

| Severity | Pre-Production | Production |
|----------|------|----------|
| **Critical** | ❌ Must resolve | ❌ Must resolve |
| **High** | ❌ Must resolve | ❌ Must resolve |
| **Medium** | ❌ Must resolve | ❌ Must resolve |
| **Low** | Tracked | Plan required |

**INTERNAL_USE_ONLY or PUBLIC applications:**
Must resolve all Critical and High findings before production. Medium and Low findings require an agreed resolution plan.

| Severity | Pre-Production | Production |
|----------|------|----------|
| **Critical** | ❌ Must resolve | ❌ Must resolve |
| **High** | ❌ Must resolve | ❌ Must resolve |
| **Medium** | Tracked | Plan required |
| **Low** | Tracked | Plan required |

**Important:** If your pipeline shows "Policy Evaluation: FAILED," you have findings that don't meet your data classification requirements. You must remediate these issues or request a Policy Exception before deploying to production.

---

## Detailed Guides

- [Vulnerability Remediation](./vulnerability-remediation.md) - Step-by-step guide to fixing security vulnerabilities using base image upgrades, package updates, and VSCode + Copilot analysis
- [Security & GitLab Work items](../cicd-pipeline/security-gitlab-issues.md) - Common developer workflow: reviewing issues, remediating vulnerabilities, updating status
- [Policy Exception Requests (PERs)](./policy-exception-request.md) - Detailed process for requesting exceptions when you cannot remediate by the due date
- [Secrets Management](../how-to/secrets-management.md) - Managing secrets and credentials securely

---

## Best Practices

### Regular Updates

Keep dependencies up-to-date:
- Review security issues weekly
- Update to latest stable versions
- Test updates in dev environment first

### Minimize Base Image Layers

Use minimal base images:
```dockerfile
# Good
FROM node:20-alpine

# Avoid
FROM node:20  # Full image with more potential vulnerabilities
```

### Don't Ignore Warnings

All security findings should be:
- Remediated, OR
- Exception requested, OR
- Documented as false positive

### Secrets Management

- Never commit secrets to Git
- Use External Secrets Operator
- Rotate secrets regularly

[!ref Secrets Management](../how-to/secrets-management.md)

---

## Additional Resources

### Internal Resources
- [Security & GitLab Work items](../cicd-pipeline/security-gitlab-issues.md) - Complete vulnerability remediation workflow
- [Policy Exception Requests](./policy-exception-request.md) - Step-by-step PER submission guide
- [Troubleshooting Guide](../troubleshooting-and-support/troubleshooting-guide.md#security--vulnerability-issues) - Security-specific troubleshooting
- [Secrets Management](../how-to/secrets-management.md) - Secure credential management
- [GitLab Repository Configuration](../getting-started/gitlab-repository-configuration.md) - Required security variables
- [Medtronic GCISO Application Security Testing](https://medtronic.sharepoint.com/sites/GSO/SitePages/Application-Security-Testing.aspx) - Official standards and requirements
- [GCISO Remediation Resources](https://medtronic.sharepoint.com/sites/GSO/SitePages/Remediation-Resources.aspx) - Guidance on vulnerability remediation and handling vulnerable dependencies

### External Resources
- [NIST National Vulnerability Database](https://nvd.nist.gov)
- [CVE Database](https://cve.mitre.org)
- [Contrast Security Documentation](https://docs.contrastsecurity.com/)

---

## Quick Reference

### Pre-Deployment Security Checklist

Before deploying to production, verify:

- ✅ Security and Privacy Intake Request completed (PIA# obtained)
- ✅ `DATA_CLASSIFICATION` set correctly in `.gitlab-ci.yml`
- ✅ Dockerfile runs as non-root user
- ✅ No hardcoded secrets in code or container image
- ✅ All Critical/High vulnerabilities remediated or have approved PERs
- ✅ Medium vulnerabilities remediated or have approved PERs (check requirements for your classification)
- ✅ Low severity vulnerabilities addressed if HIGHLY_SENSITIVE
- ✅ Health endpoints configured and responding
- ✅ Database connections use TLS/SSL
- ✅ Logs don't contain sensitive data
