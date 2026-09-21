# **Onboarding Support for Argo Platform**

## Onboarding Plan

### Getting Started
- **Infrastructure Overview**:
    - Review the [EKS cluster architecture](https://playbook.argo-dev.eks.mdtcloud.io/application-onboarding/faq/#do-you-have-an-infrastructure-diagram).
    - Understand key AWS services integrated with EKS (e.g., S3, RDS, ALB/ELB, IAM roles for pods).
        * S3 for ALB logging
        * ALB/ELB - created via Ingress resources in EKS
        * IAM roles
        * WAF
        * Certificate Manager & Route 53
- **Tooling Setup**:
    * Gain access to:
        * [GitLab](https://medtronic.gitlab-dedicated.com/) - Submit ticket to Infra-SourceCode-Global to request `Developer` access to `BCP_Web` for your `<userid>`
        * [AWS Accounts](https://medtronicsso.awsapps.com/start/) by submitting a ServiceNow INC assigned to `Cloud-Global`. Request access to the below accounts with the same permissions/roles as Ash Montebello.
            * Send email to the [AWS account owner](mailto:ash.l.montebello@medtronic.com?subject=Argo%20AWS%20Account%20Access%20Approval&body=Please%20approve%20access%20to%20the%20following%20Argo%20AWS%20accounts%3A%0A%0Ait-argo-dev-mdt%20(389242548790)%0Ait-argo-prod-mdt%20(872019488961)%0Ait-argobservability-dev-mdt%20(227143426852)%0Ait-wasobserv-monitor-prod-mdt%20(546946040407)) to request approval. Attach email approval to the INC.
            * it-argo-dev-mdt (389242548790)
                * Argo dev account. Houses customer applications (DEV & TESTING environments).
            * it-argo-prod-mdt (872019488961)
                * Argo prod account. Houses customer applications (STAGING, RELEASE, & PRODUCTION environments).
            * it-argobservability-dev-mdt (227143426852)
                * Dev observability account. Used for POCs related to observability - has cross-account access to it-argo-dev-mdt CloudWatch for logs/metrics.
            * it-wasobserv-monitor-prod-mdt (546946040407)
                * Prod observability account. Contains required Grafana setup and has cross-account access to it-argo-dev-mdt and it-argo-prod-mdt for retrieving CloudWatch logs/metrics.
            <!-- * it-cicdcds-dev-mdt (563893293061)
                * Dedicated dev account for our team. Can be used for any POCs before moving to it-argo-dev-mdt account. -->
        * [WebDev Cloud Solutions](https://teams.microsoft.com/l/channel/19%3Af3b0814ab96c4945a3c3dd04a29823da%40thread.tacv2/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)?groupId=1ee0a48a-d6da-4b9f-b082-bd5cd85ad2a2&tenantId=d73a39db-6eda-495d-8000-7579f56d68b7) Teams channel.
            * Send request to [Dl IT Argo Core Team](mailto:dl.itargocoreteam@medtronic.com) to request access.
        * [Monitoring/alerting tools](https://playbook.argo-dev.eks.mdtcloud.io/application-onboarding/tool-access/#grafana-dashboards-logs-and-metrics).
        * [Port](https://app.getport.io/org_fWJXQcLuuD11DeJs/self-serve)
            * Port access for support team must be configured by an existing support team member or Argo team member using this [workflow](https://app.getport.io/org_fWJXQcLuuD11DeJs/self-serve?workflow=port_team_add_ad_user%2Ftrigger)
            * Select the new user from dropdown and assign to group: `atlas-port-platform-support`
        * [Terraform Cloud (TFC)](https://app.terraform.io/)
            * Add Argo TFC access using this [Port workflow](https://app.getport.io/org_fWJXQcLuuD11DeJs/self-serve?workflow=add_ad_user_to_ad_group%2Ftrigger)
            * Add both groups to the user:
                * `atlas-tfc-global-users`
                * `atlas-tfc-it-argo`
        * [Dynatrace](https://medtronic.dynatrace.com/)
            * Add Dynatrace access using this [Port workflow](https://app.getport.io/org_fWJXQcLuuD11DeJs/self-serve?workflow=add_ad_user_to_ad_group%2Ftrigger)
            * Add both groups to the user:
                * `atlas-dynatrace-global-users`
                * `ENT-ATLAS_dynatrace_users-SECURE`
    * Clone and explore the GitLab projects related to our product:
        * [Semantic Release](https://medtronic.gitlab-dedicated.com/bcp_web/devops/semantic-release): contains all shared pipeline templates
        * [flux-gitops](https://medtronic.gitlab-dedicated.com/bcp_web/devops/fluxconfigs/flux-gitops): stores base k8s files for onboarded apps + k8s infrastructure
        * [einstein](https://medtronic.gitlab-dedicated.com/bcp_web/common/einstein): sample project using our pipeline templates
    * Clone and explore the Terraform projects for IaC:
        * [aws-it-argo-dev-mdt](https://code.medtronic.com/bcp_web/devops/infrastructure/terraform-deployments/aws-it-argo-dev-mdt)
        * [aws-it-argo-prod-mdt](https://code.medtronic.com/bcp_web/devops/infrastructure/terraform-deployments/aws-it-argo-prod-mdt)
        * [aws-it-wasobserv-monitor-prod-mdt](https://code.medtronic.com/bcp_web/devops/infrastructure/terraform-deployments/aws-it-wasobserv-monitor-prod-mdt): Grafana
- **Training Videos**:
    - [Review current videos](https://medtronic.sharepoint.com/:f:/r/sites/WebDevModernization/Shared%20Documents/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)/Compass%20CI/Demos%20and%20Recordings?csf=1&web=1&e=KmCHDx) for overviews of different topics for Compass CI.

### Week 1: Terraform ###
Below are some common tasks requested by application teams that require updates within our Terraform projects.

TODO: Create [ServiceNow Catalog Requests](https://medtronicprod.service-now.com/it?id=sc_cat_item&sys_id=59cf13371b8d6d90f49711f72a4bcbbe&table=sc_cat_item&searchTerm=add%20catalog%20item) for these items, 1-3 being top priority.

1. WAF Rule Updates
   * Required when app team sees 403 errors.
   * [Video walkthrough](https://medtronic.sharepoint.com/:v:/r/sites/WebDevModernization/Shared%20Documents/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)/Training%20Videos/Training%20Videos_CI-CD%20Pipeline/WAF%20Rule%20Updates.mp4?csf=1&web=1&e=kH8Ack)
   * WAF CloudWatch Troubleshooting Steps:
       * Use [Grafana explore](https://g-05c60f9e9b.grafana-workspace.us-east-1.amazonaws.com/explore)
         * Choose `CloudWatch Logs`
         * Select `aws-waf-logs-TF-argo-custom` (for one or both AWS accounts)
         * Enter a query
       * Example WAF CloudWatch query:
        ```text
        fields @timestamp, action, httpRequest.uri, httpRequest.clientIp, httpRequest.httpMethod, httpRequest.country, terminatingRuleId, @message |
        parse @message /(?i)"name":"Host","value":"(?<host>[^"]*)"/ |
        filter action = 'BLOCK' and httpRequest.uri like "/api/contract_transformer/"
        ```
1. IAM Roles for EKS
   * Required when an app running in EKS requires permissions to outside resources within our AWS account or others.
1. [Certificate Imports](https://playbook.argo-dev.eks.mdtcloud.io/build-and-deploy/vanity-dns-setup/#aws-certificate-import) and [Updates](https://playbook.argo-dev.eks.mdtcloud.io/build-and-deploy/vanity-dns-setup/#aws-certificate-re-import--renew) for Custom Hostnames
    * Required when an app wants to use a custom `*.medtronic.com` hostname.
    * [Video walkthrough](https://medtronic.sharepoint.com/:v:/r/sites/WebDevModernization/Shared%20Documents/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)/Training%20Videos/Support/Argo%20Custom%20Hostname%20Certificate%20Upload.mp4?csf=1&web=1&e=wm8Nmz) is located on our [WebDev Cloud Solutions Teams channel](https://teams.microsoft.com/l/channel/19%3Af3b0814ab96c4945a3c3dd04a29823da%40thread.tacv2/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)?groupId=1ee0a48a-d6da-4b9f-b082-bd5cd85ad2a2&tenantId=d73a39db-6eda-495d-8000-7579f56d68b7).
1. Cloud9 Access Requests
    * Required when new CTS or other new support resources are onboarded to the team.
1. S3 Bucket Creation (rare)
    * Required when an app needs S3 for storage.
1. [SNS Topics & Subscriptions](https://playbook.argo-dev.eks.mdtcloud.io/monitoring/application-logging/#alerting) (for Grafana alerts)

### Week 2: EKS / Kubernetes / Cloud9 ###
1. Flux project structure
1. Access and use of Cloud9
1. Things to review when troubleshooting
    * Kustomizations
      * Check whether the commit sha matches the latest in the GitLab project
    * Helm Release status
      * Check whether it has any failures, if so, it may need to be deleted and allow Flux to recreate it.
    * Deployments / Pods
        * Events such as failing health checks
    * Network connectivity
1. Troubleshooting guide

### Week 3: Pipelines / Groovy Scripts ###
1. [Manage Namespace](https://code.medtronic.com/bcp_web/devops/prometheus/manage-namespace)
    * [Diagram and Overview](https://playbook.argo-dev.eks.mdtcloud.io/build-and-deploy/gitlab-ci/manage-namespace/)
    * [Training Video](https://playbook.argo-dev.eks.mdtcloud.io/education-and-training/#application-environment-setup)
1. [Build Image](https://code.medtronic.com/bcp_web/devops/prometheus/build-app-image)
    * [Diagram and Overview](https://playbook.argo-dev.eks.mdtcloud.io/build-and-deploy/gitlab-ci/build-image/)
    * [Training Video](https://playbook.argo-dev.eks.mdtcloud.io/education-and-training/#building-an-application-image)
1. [Deploy Image](https://code.medtronic.com/bcp_web/devops/prometheus/deploy-image)
    * [Diagram and Overview](https://playbook.argo-dev.eks.mdtcloud.io/build-and-deploy/gitlab-ci/deploy-image/)
    * [Training Video](https://playbook.argo-dev.eks.mdtcloud.io/education-and-training/#application-deployment)
1. Artifactory
    * [cacert updates](https://playbook.argo-dev.eks.mdtcloud.io/troubleshooting-and-support/troubleshooting-guide/#web-support-team-instructions)
    * Monthly [base image updates](https://code.medtronic.com/bcp_web/devops/DockerConfigs/-/blob/develop/Builders/README.md?ref_type=heads)
        * [Video walkthrough](https://medtronic.sharepoint.com/:v:/r/sites/WebDevModernization/Shared%20Documents/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)/Training%20Videos/Training%20Videos_CI-CD%20Pipeline/Base%20Image%20Creation.mp4?csf=1&web=1&e=sgfMc6)
1. GitLab [Token Rotations](../TokensAndServiceAccounts.md#gitlab-tokens) (yearly)
   * Rotate each of the tokens mentioned by following the instructions in the `Update / Rotation Instructions`
   * Update the [Token Rotation](../TokensAndServiceAccounts.md#gitlab-tokens) table to document the new expiration date.
   * Create a reminder (e.g Outlook or other) for the next year to rotate again. Set this reminder for one week prior to the new token's expiration to ensure it is rotated prior to expiration.

### Week 4: Grafana - Logs, Alerts ###
1. Logs
1. Traces
1. [New team dashboard setup](https://playbook.argo-dev.eks.mdtcloud.io/application-onboarding/tool-access/#new-dashboard-configuration-for-your-application)
    * See `AD Group Request and Grafana Admin Tasks` for required admin steps
1. [New alert setup](https://playbook.argo-dev.eks.mdtcloud.io/monitoring/application-logging/#alerting)
    * See `Support Team Alert Setup Process` for required admin steps

See additional [observability & monitoring](https://medtronic.sharepoint.com/:f:/r/sites/WebDevModernization/Shared%20Documents/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)/Observability%20%26%20Monitoring?csf=1&web=1&e=jVeSpg) documents on our Teams channel.

## Additional Resources:

### Documentation:

   * [Gitlab-CI documentation](https://playbook.argo-dev.eks.mdtcloud.io/build-and-deploy/gitlab-ci/)
   * [Container Builders](https://playbook.argo-dev.eks.mdtcloud.io/build-and-deploy/gitlab-ci/builder-containers/)
   * [Base Image Versioning](https://playbook.argo-dev.eks.mdtcloud.io/build-and-deploy/base-images/)
   * [Example appInfo.json File](https://playbook.argo-dev.eks.mdtcloud.io/build-and-deploy/settings/example_appinfo_json/)
   * [Example environmentInfo.json File](https://playbook.argo-dev.eks.mdtcloud.io/build-and-deploy/settings/example_environmentinfo_json/)
   * [Custom Hostname](https://playbook.argo-dev.eks.mdtcloud.io/build-and-deploy/vanity-dns-setup/)
   * [SSL Certificates](https://playbook.argo-dev.eks.mdtcloud.io/build-and-deploy/ssl-certificate-creation/)

### Training Videos:

   * [Application Containerization 101](https://medtronic.cloud.panopto.eu/Panopto/Pages/Viewer.aspx?id=58b6548d-3e5a-495d-9da5-b0510134d20a)
   * [Application Deployment](https://medtronic.cloud.panopto.eu/Panopto/Pages/Viewer.aspx?id=e5dd78fd-64c9-4070-80b1-b0b300ff0948)
   * [Application Environment Setup](https://medtronic.cloud.panopto.eu/Panopto/Pages/Viewer.aspx?id=ea89045d-a2ab-4e24-be79-b0660137fd33)
   * [Application Go-Live Preparation](https://medtronic.cloud.panopto.eu/Panopto/Pages/Viewer.aspx?id=8ed5fe0b-f87e-4bf4-924e-b0b3010b8218)
   * [Building an Application Image with the Prometheus Pipeline](https://medtronic.cloud.panopto.eu/Panopto/Pages/Viewer.aspx?id=1d82446a-2fb0-43c6-b9d0-b068011da5fa)
   * [Containerization Best Practices](https://medtronic.cloud.panopto.eu/Panopto/Pages/Viewer.aspx?id=8658ef4f-6183-4618-9164-b05201508069)
   * [Custom Hostnames](https://medtronic.cloud.panopto.eu/Panopto/Pages/Viewer.aspx?id=992bdbf3-0b0a-4233-94af-b083013e8ca4)
   * [Grafana alert rules creation](https://medtronic.cloud.panopto.eu/Panopto/Pages/Viewer.aspx?id=89de9af3-25e4-4af5-901c-b15b00fd5ac9)
   * [Grafana Basic Search Tutorial](https://medtronic.cloud.panopto.eu/Panopto/Pages/Viewer.aspx?id=2bb662b6-182d-405b-81c3-b07a0140219f)
   * [Networking and Firewall](https://medtronic.cloud.panopto.eu/Panopto/Pages/Viewer.aspx?id=65e7a1cb-7ffb-485b-b8aa-b0830162f2cf)
   * [Vulnerability Scanning and Remediation](https://medtronic.cloud.panopto.eu/Panopto/Pages/Viewer.aspx?id=6810700a-1642-4466-baf5-b07b011f4530)
