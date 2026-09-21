---
label: Compass CI RACI
icon: people
order: 80
---

# Compass CI RACI

## Introduction

This RACI (Responsible, Accountable, Consulted, Informed) matrix defines the roles and responsibilities for using Compass CI. Understanding these responsibilities ensures smooth operations, clear accountability, and effective collaboration between the Platform Team and Application Teams.

**Before go-live**, a contact from the application team must email the [Platform Team](mailto:dl.itargocoreteam@medtronic.com) stating they've completed testing and are accepting Compass CI as-built as the hosting platform for their application.

## Definitions

- **R = Responsible:** Person who performs an activity or does the work
- **A = Accountable:** Person who is ultimately accountable and has Yes/No/Veto authority
- **C = Consulted:** Person who needs to provide feedback and contribute to the activity
- **I = Informed:** Person who needs to know of the decision or action

## Application Team Responsibilities

Application owners are responsible for:
- Reviewing and completing all steps on the [Go-Live Checklist](go-live-checklist.md)
- Application development and configuration including submitting requests for firewall, DNS, Identity & Access Management, Policy Exception Requests, etc.
- Remediating any vulnerabilities within your application
- Application environment configuration (Kubernetes manifests, kustomize overlays)
- Application deployments and version management
- Coordinating go-live activities, RFCs, and Cloud Governance gates
- Ensuring application meets [security requirements](../security/index.md) and [deployment best practices](../how-to/deployments.md)

---

## RACI Matrix

The following RACI applies to all Compass CI clusters.

| Responsibility | Shared Services Team | Compass Platform Team | GCISO | Application Team | Notes |
|---|---|---|---|---|---|
| **Cluster Infrastructure** | | | | | |
| Deploy Kubernetes Cluster | RA | | C | | Shared Services Team provisions and maintains EKS clusters |
| Cluster platform version updates | RA | I | C | I | Shared Services Team manages Kubernetes version upgrades; Application Teams test compatibility |
| Cluster scaling capability | RA |  | | C | Shared Services Team provides autoscaling; Application Teams define resource requests |
| Cluster security remediation | RA |  | C | | Shared Services Team remediates cluster-level vulnerabilities via node rotation |
| Cluster monitoring and logging | RA | C | | | Platform Team provides centralized Grafana/Loki |
| Provide/maintain ALB and WAFv2 | | RA | C | | Platform Team provides basic ALB and WAF; custom configs are Application Team responsibility |
| Provide persistent storage (EBS/EFS) | RA | | | | Shared Services Team maintains CSI drivers |
| | | | | | |
| **CI/CD Pipeline** | | | | | |
| GitLab CI/CD platform | | RA | | I | Platform Team provides shared pipeline components |
| Pipeline templates and shared jobs | | RA | | C | Platform Team maintains reusable `.gitlab-ci.yml` jobs |
| Application-specific pipeline configuration | | | | RA | Application Team creates and maintains their `.gitlab-ci.yml` |
| Container image builds | | | | RA | Application Team responsible for Dockerfile and build process |
| Container image vulnerability scanning | | RA | C | I | Platform Team provides container scanning |
| SAST/Dependency scanning | | RA | C | I | Platform Team provides scanning tools |
| Prerequisites validation (data classification, etc.) | | RA | | C | Platform Team enforces validation jobs |
| | | | | | |
| **FluxCD and GitOps** | | | | | |
| FluxCD platform deployment | | RA | | | Platform Team maintains FluxCD controllers |
| FluxCD configuration for common components | | RA | | | Platform Team manages platform-level FluxCD resources |
| Application FluxCD onboarding | | RA | | C | Application Team submits GitRepository/Kustomization to flux-gitops repo |
| Application kustomize overlays and manifests | | | | RA | Application Team creates and maintains k8s/ folder structure |
| Application GitOps troubleshooting | I | C | | RA | Application Team troubleshoots deployment issues; Platform Team assists with FluxCD itself |
| | | | | | |
| **Security & Compliance** | | | | | |
| Define security policies | I | I | RA | I | GSO defines security standards |
| Apply security policies to platform | RA | RA | CI | I | Shared Services and Platform Team implement GSO requirements |
| Apply security policies to AWS resources | | R | | A | Application Team owns their AWS account resources (S3, RDS, etc.) |
| Container image security | |  | C | RA | Application Team remediates vulnerabilities |
| Application vulnerability remediation | | | C | RA | Application Team fixes code/dependency vulnerabilities |
| Policy Exception Requests (PERs) | | | C | RA | Application Team submits and owns PERs |
| GCISO Security & Privacy Intake | | | C | RA | Application Team completes intake and determines DATA_CLASSIFICATION |
| | | | | | |
| **Application Deployment & Operations** | | | | | |
| Application development | | | | RA | Application Team owns codebase |
| Application Dockerfile creation | | C | | RA | Application Team creates/maintains; Platform Team provides guidance |
| Application deployment to all environments | | | | RA | Application Team manages via Git commits (FluxCD reconciles) |
| Application configuration (ConfigMaps/Secrets) | | | | RA | Application Team manages application-specific configuration |
| Application monitoring and alerting | | RA | | C | Platform Team configures Grafana dashboards; Application team views and/or edits their dashboards |
| Application incident response | | | | RA | Application Team resolves application-level issues |
| Application health checks (/health, /ready) | | C | | RA | Application Team implements; Platform Team provides guidance |
| Application resource limits and requests | | C | | RA | Application Team defines; Platform Team provides cluster capacity |
| Application scaling (HPA, replicas) | | C | | RA | Application Team configures autoscaling and replica counts |
| Application data backups | | C | | RA | Application Team responsible for resources related to backups (RDS, S3, etc.) |
| | | | | | |
| **Networking & Connectivity** | | | | | |
| Firewall rule requests | | C | C | RA | Application Team requests; Platform Team and GSO approve/implement |
| DNS management | | C | | RA | Application Team requests via standard DNS process |
| SSL/TLS certificates | | R | | A | Platform Team integrates with AWS Certificate Manager and assists with upload of certificates |
| Ingress configuration | | C | | RA | Application Team defines Ingress resources; Platform Team provides ALB controller |
| | | | | | |
| **Kubernetes Access & RBAC** | | | | | |
| Namespace creation | | RA | | C | Platform Team creates namespaces for applications |
| Namespace RBAC configuration | | RA | | C | Platform Team provides namespace-scoped access |
| Application ServiceAccount management | | | | RA | Application Team creates ServiceAccounts for their apps |
| IAM Roles for Service Accounts (IRSA) | | RA | | C | Platform Team enables IRSA; Application Team requests roles |
| | | | | | |
| **Troubleshooting & Support** | | | | | |
| Cluster infrastructure issues | RA | | | I | Shared Services Team troubleshoots cluster-level problems |
| CI/CD Pipeline component issues |  | RA | | I | Platform Team troubleshoots platform components |
| Platform component issues (FluxCD, ingress controller, etc.) | RA | | | I | Shared Services Team troubleshoots platform components |
| Application deployment issues | | C | | RA | Application Team troubleshoots their deployments; Platform Team provides guidance |
| Application runtime issues | | | | RA | Application Team troubleshoots application code and configuration |
| ServiceNow incident creation | | | | RA | Application Team creates incidents for application issues |
| ServiceNow incident response (platform) | | RA | | I | Platform Team responds to platform-level incidents |
| | | | | | |
| **Compliance & Governance** | | | | | |
| Cloud Governance gates | C | C | C | RA | Application Team coordinates; multiple parties consulted |
| Change Request (RFC) coordination | | | | RA | Application Team submits RFCs for production changes |
| Audit compliance for platform | | RA | CI | | Platform Team maintains platform compliance |
| Audit compliance for applications | | | C | RA | Application Team ensures their application complies |

---

## Notes

1. **Shared Services Team:** The Shared Services Team manages the EKS clusters and related Kubernetes infrastructure within AWS accounts. They do NOT manage other AWS resources (S3, RDS, CloudFront, etc.). For those resources, Application Teams have two options:
   - **Own AWS Account:** Create and manage resources in your own AWS account via [Cloud Services](https://cloudservices.medtronic.com/aws/)
   - **Shared Account:** Work with the Platform Team to provision resources in the Compass CI multi-tenanted AWS accounts

2. **Platform Team:** The Compass CI Platform Team provides infrastructure and platform-level support during normal business hours (8x5) and on-call support is provided for P0/P1 issues.

3. **Application Team Target Audience:** Compass CI is designed for teams with Kubernetes experience. Teams are expected to be able to develop, maintain, and troubleshoot their own applications and Kubernetes resources. The Platform Team does not provide general Kubernetes training or application-level support.

4. **Support Expectations:**
   - Platform issues: Contact Platform Team via ServiceNow: Infra-Argo-Global
   - Application issues: Application Team responsible
   - Security issues: Follow [security incident process](../security/index.md)

5. **Required Application Readiness:**
   - At least 2 replicas for Deployments/StatefulSets
   - Properly configured readinessProbe and livenessProbe
   - PodDisruptionBudget defined
   - topologySpreadConstraints implemented
   - See [Deployments Guide](../how-to/deployments.md) for best practices

---

## Related Resources

- [Go-Live Checklist](go-live-checklist.md)
- [Security Overview](../security/index.md)
- [Deployment Best Practices](../how-to/deployments.md)
- [Getting Started Guide](../getting-started/index.md)
- [Troubleshooting Guide](../troubleshooting-and-support/troubleshooting-guide.md)
