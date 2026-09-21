---
label: Go-Live Checklist
icon: tasklist
order: 100
---

# Go-Live Checklist

Ensure all items are completed before deploying to production. Most items should be completed well before the deployment date.

!!!warning
This is a generalized checklist. Review and adjust for your specific application requirements.
!!!

---

## Two or More Weeks Before Go-Live

- [ ] **Cloud Governance** (if first-time deployment with Highly Sensitive data):
  - See [Cloud Governance Gates](cloud-governance.md) for Gate 2 and Gate 3 requirements
  - Create [Application Logical Diagram](cloud-governance.md#required-artifacts--data) using provided templates for your cluster (Production or Quarantine)
  - Note: Gate 2 and Gate 3 cannot occur on the same date; plan two weeks for reviews

- [ ] **RACI Review**: Reconfirm responsibilities in the [Argo RACI](argo-raci.md) for your application and the platform.

- [ ] **Security**: Resolve all critical and high severity vulnerabilities identified in your [GitLab Work items](../cicd-pipeline/security-gitlab-issues.md) from security scanning (SAST, dependency, container scans). For medium/low and Policy Exception Requests, see [Security Vulnerability Remediation](../cicd-pipeline/security-gitlab-issues.md) for guidance.

- [ ] **Complete Security Assessment**: Finish [GSO Security & Privacy Assessment](https://medtronicprod.service-now.com/it/?id=sc_cat_item_order_guide&sys_id=10bd221adb726c1014c354f94896192d) (must be within the past year)

- [ ] **Network Connectivity**: Request any [external connectivity](../how-to/connectivity-request-process.md) required. Test using the [Connectivity Tester](https://newton-prod.argo-prd.eks.mdtcloud.io/connectivity-test).
  - Contact Infra-Argo-Global if you have questions or need assistance.

- [ ] **Authentication**: If your app requires user login:
  - [ ] For internal Medtronic users: Request production [Azure AD Configuration](../how-to/authentication.md#authentication)
  - [ ] For external users: Request production [CIAM Configuration](../how-to/authentication.md#external-ciam-providers)

- [ ] **Custom Domain** (if applicable):
  - [ ] Create a *.medtronic.com TLS certificate. See [Web Access & Hostnames](../how-to/web-access-hostnames.md) for details.
  - [ ] Provide certificate to Infra-Argo-Global for AWS import.

- [ ] **Release Candidate**: Build and deploy a release version to your staging environment.

- [ ] **UAT Testing**: Complete functional and user acceptance testing against the staging environment.

- [ ] **DNS Registration** (if applicable):
  - [ ] Submit [DNS request](https://medtronicprod.service-now.com/it/?id=mdtit_sc_cat_item&sys_id=32ff13841b9984504455631e6e4bcb32) for your custom domain (CNAME to ALB).
  - [ ] Request the ALB URL from Infra-Argo-Global if you don't have it.
  - [ ] Verify with `nslookup <domain>` once complete.

- [ ] **Monitoring**: Request access to [Grafana dashboards](../monitoring/03-logging.md) for your application.

- [ ] **Communities**:
  - [ ] Subscribe to the [Global IT Kubernetes Platform Community](https://engage.cloud.microsoft/main/org/medtronic.com/groups/eyJfdHlwZSI6Ikdyb3VwIiwiaWQiOiIxNzAzMTgyMjU0MDgifQ) for cluster updates and upgrade notifications.
  - [ ] Subscribe to the [Global IT Compass Platform Community](https://engage.cloud.microsoft/main/org/medtronic.com/groups/eyJfdHlwZSI6Ikdyb3VwIiwiaWQiOiIyMzg5MjY4ODA3NjgifQ) for pipeline and platform updates and notifications.
---

## One Week Before Go-Live

- [ ] **Cloud Governance Gate 3** (applies to all new apps):
  - Prepare and review [Gate 3 requirements](cloud-governance.md#gate-3---application-go-live)
  - Provide Gate 3 artifacts to Cloud Governance team for weekly agenda

- [ ] **Change Control**: Create an RFC (Request for Change) and attach testing summary.

- [ ] **CAB Approval**: Attend Change Advisory Board meeting to present and get approval for the change.

---

## On Go-Live Date

- [ ] **Database** (if applicable): Migrate production database schema and data.

- [ ] **Deploy**: Deploy your release candidate to production by merging changes from staging to the main or production branch.

- [ ] **Smoke Tests**: Run basic tests to confirm the application is up and functioning correctly.

- [ ] **DNS Verification** (if applicable): Verify `nslookup <domain>` returns the correct ALB.

- [ ] **CMDB Update**: [Register your application](../supporting-docs/request-cmdb-configuration-item.md) as a Configuration Item in ServiceNow CMDB with all relationships documented.

- [ ] **Decommission** (if applicable): Shut down or uninstall the application from previous hosting environments.
