# **Onboarding SRE Role for Argo Platform**

## **Onboarding Plan (First 30 Days)**

### **Week 1: Getting Started**
- **Infrastructure Overview**:
    - Review the [EKS cluster architecture](https://playbook.argo-dev.eks.mdtcloud.io/application-onboarding/faq/#do-you-have-an-infrastructure-diagram).
    - Understand key AWS services integrated with EKS (e.g., S3, RDS, ALB/ELB, IAM roles for pods).
        * S3 for ALB logging
        * ALB/ELB - created via Ingress resources in EKS
        * IAM roles
        * WAF
        * Certificate Manager & Route 53
- **Laptop Setup / Tools**:
    Download and install the following tools.
    * [Visual Studio Code](https://code.visualstudio.com/download)
        * Once Visual Studio is installed, here are some suggested extensions:
            * SQL Developer
            * GitLab Workflow
            * GitHub Copilot
                * Submit a [request here for access](https://medtronicprod.service-now.com/it?id=sc_cat_item&table=sc_cat_item&sys_id=a9bb3531971a86d0f95e3ffce053afb2&recordUrl=com.glideapp.servicecatalog_cat_item_view.do%3Fv%3D1&sysparm_id=a9bb3531971a86d0f95e3ffce053afb2)
                    * Select the `GitHub Copilot for Business - Non-GitHub User` Role
                * Once you are given access, log in to GitHub from VS Code using your `userid_mdtcop` account
    * [Docker Desktop](https://www.docker.com/products/docker-desktop/)
        * For a business license for Docker Desktop, submit a request [here](https://mspm1bapps0129.ent.core.medtronic.com/esd/Items/Details?PackageId=344). There is a license cost associated with this. For more information on Docker software licensing, please email [RS Software Licensing Admin](mailto:rs.softwarelicensingadmin@medtronic.com).
    * [Draw.io](https://www.drawio.com/)
        * Used for viewing or updating infrastructure diagrams
    * [Git](https://git-scm.com/install/windows)
    * [Postman](https://www.postman.com/downloads/)
        * Useful for API testing
    * [Node](https://nodejs.org/en/download)
    * [Java 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
    * [Retype](https://retype.com/guides/installation/)
        * Used for our documentation / playbook site
- **Tooling Setup**:
    * Gain access to:
      * [GitLab](https://medtronic.gitlab-dedicated.com/) - Submit ticket to Infra-SourceCode-Global to request `Developer` access to `BCP_Web` for your `<userid>`
      * [AWS Accounts](https://medtronicsso.awsapps.com/start/) by submitting a ServiceNow INC assigned to Cloud-Global. Request access to the below accounts with the same permissions/roles as Ash Montebello. Email [AWS account owner](mailto:ash.l.montebello@medtronic.com?subject=Argo%20AWS%20Account%20Access%20Approval&body=Please%20approve%20access%20to%20the%20following%20Argo%20AWS%20accounts%3A%0A%0Ait-argo-dev-mdt%20(389242548790)%0Ait-argo-prod-mdt%20(872019488961)%0Ait-argobservability-dev-mdt%20(227143426852)%0Ait-wasobserv-monitor-prod-mdt%20(546946040407)) for approval.
        * it-argo-dev-mdt (389242548790)
          * Argo dev account. Houses customer applications (DEV & TESTING environments).
        * it-argo-prod-mdt (872019488961)
          * Argo prod account. Houses customer applications (STAGING, RELEASE, & PRODUCTION environments).
        * it-argobservability-dev-mdt (227143426852)
          * Dev observability account. Used for POCs related to observability - has cross-account access to it-argo-dev-mdt CloudWatch for logs/metrics.
        * it-wasobserv-monitor-prod-mdt (546946040407)
          * Prod observability account. Contains required Grafana setup and has cross-account access to it-argo-dev-mdt and it-argo-prod-mdt for retrieving CloudWatch logs/metrics.
        * it-cicdcds-dev-mdt (563893293061)
          * Dedicated dev account for our team. Can be used for any POCs before moving to it-argo-dev-mdt account.
      * [Monitoring/alerting tools](https://playbook.argo-dev.eks.mdtcloud.io/application-onboarding/tool-access/#grafana-dashboards-logs-and-metrics).
      * [Port](https://app.getport.io/org_fWJXQcLuuD11DeJs/self-serve)
        * Port access must be configured by an existing team member using this [workflow](https://app.getport.io/org_fWJXQcLuuD11DeJs/self-serve?workflow=port_team_add_ad_user%2Ftrigger)
        * Add both groups to the user:
          * `atlas-port-it-sharedservices`
          * `atlas-port-platform-engineering`
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
        * [Argo-Dev](https://medtronic.gitlab-dedicated.com/bcp_web/devops/infrastructure/terraform-deployments/aws-it-argo-dev-mdt)
        * [Argo-Prod](https://medtronic.gitlab-dedicated.com/bcp_web/devops/infrastructure/terraform-deployments/aws-it-argo-prod-mdt)
- Watch related videos located on our [Teams Channel](https://medtronic.sharepoint.com/:f:/r/sites/WebDevModernization/Shared%20Documents/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)/Compass%20CI/Demos%20and%20Recordings?csf=1&web=1&e=0WjOe1).
- **Documentation and Standards**:
    - Review documentation on Terraform workflows, [GitLab CI pipelines](https://playbook.argo-dev.eks.mdtcloud.io/build-and-deploy/gitlab-ci/), and [application onboarding](https://playbook.argo-dev.eks.mdtcloud.io/application-onboarding/).
    - Understand the tagging/naming conventions and resource standards used in the infrastructure.
- **Meet the Team**:
  - Understand team structure, responsibilities, and how to get help.
    - Argo Platform team (us)
        - Assists with application onboarding, use of the pipeline, any pipeline / platform issues, and general questions.
    - Web App Support team - dl.webappscognizantteam@medtronic.com
        - Assists with application troubleshooting for applications that they build and support.
    - Shared Services team
        - CI/CD Platform (GitLab, JFrog Artifactory) - Tim Anderson
        - EKS cluster management / upgrades - Niels Van Zwieten
    - Cloud team
        - General AWS infrastruture / support
            - Cloud-Global in ServiceNow
        - Chad Nelson
            - Cloud Networking
        - Dean Schrimpf
            - Cloud architect / Cloud Governance approver
        - Jeon Calhoun
            - AWS contact for MDT
    - Security team
        - Maria Brown
            - Wiz & Contrast Applications
        - Kori Prins
            - Contrast admin
        - Mike Kennedy
            - GCISO approver for Cloud Governance
        - David Michael
            - Wiz & Application Security
---

### **Week 2: Tools and Processes**
- **Terraform Familiarity**:
    - Understand the structure of Terraform [projects](https://code.medtronic.com/bcp_web/devops/infrastructure/terraform-deployments) and [modules](https://code.medtronic.com/bcp_web/devops/infrastructure/terraform-modules) used for Argo AWS resources.
    - Learn the process for creating, reviewing, and deploying infrastructure changes using Terraform.

- **GitLab CI/CD Pipelines**:
    - Dive into GitLab pipeline configurations and deployment workflows.
    - Observe deployments of at least two applications to the EKS cluster.
        * Play with our [Newton](https://code.medtronic.com/bcp_web/common/newton) app and do deployment to the DEV environment.

- **Monitoring and Logging**:
    - Get hands-on with monitoring tools (e.g., CloudWatch, Prometheus, Grafana).
    - Understand [alerting and incident notification setups](https://medtronic.sharepoint.com/:f:/r/sites/WebDevModernization/Shared%20Documents/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)/Observability%20%26%20Monitoring?csf=1&web=1&e=2Xbcs6).

- **Shared Infrastructure Maintenance**:
    - Review policies for managing shared EKS components (e.g., ingress controllers, cluster autoscalers, and resource quotas).

---

### **Week 3–4: Shadowing and Initial Contributions**
- **Shadowing and Observing**:
    - Join in reviews of Terraform changes and GitLab CI pipeline updates.
    - Observe incident response processes for shared infrastructure (e.g., EKS node issues, CI pipeline failures).

- **Hands-on Tasks**:
    - Assist with low-risk Terraform changes (e.g., updating tags or IAM policies).
    - Contribute to CI pipeline optimizations or documentation.

- **Documentation Improvements**:
    - Update or enhance runbooks/playbooks for common EKS or CI/CD scenarios.

---

## **Goals for the First 3 Months**

### **Month 1: Foundations**
- Gain a comprehensive understanding of the EKS infrastructure, Terraform workflows, and GitLab CI pipelines.
- Document one or more gaps in existing processes (e.g., lack of clarity in Terraform module usage or deployment steps).
- Shadow at least three pipeline deployments and participate in post-deployment reviews.

---

### **Month 2: Independence**
- Take ownership of a small Terraform update and push it through the pipeline.
- Resolve one or more low-priority incidents related to the shared EKS cluster or CI pipeline.
- Enhance monitoring by proposing or implementing improvements to Grafana dashboards or alert thresholds.
- Collaborate with the team to propose a new process or tool enhancement (e.g., automating scaling or standardizing CI pipeline variables).

---

### **Month 3: Driving Impact**
- Lead a medium-sized Terraform or GitLab CI pipeline enhancement (e.g., optimizing a shared module or improving pipeline speed/reliability).
- Implement one automation task to reduce toil (e.g., automating rollbacks for failed deployments or simplifying resource provisioning with scripts).
- Present a report or demo covering:
    - Observed pain points in EKS management or CI pipelines.
    - Proposed improvements and initial outcomes.

---

## **Key Success Metrics**
- **Terraform**: Complete at least two pull requests for infrastructure updates with minimal guidance.
- **CI/CD**: Contribute to one pipeline optimization or bug fix.
- **Incident Handling**: Independently resolve medium-priority issues related to EKS or GitLab pipelines.
- **Collaboration**: Actively engage with at least two cross-functional teams and document feedback for process improvements.
