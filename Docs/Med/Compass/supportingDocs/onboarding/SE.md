# **Onboarding Software Engineer Role for Argo Platform**

## **Onboarding Plan (First 30 Days)**

### **Week 1: Getting Started**
- **Platform Overview**:
    - Review the [EKS cluster architecture](https://playbook.argo-dev.eks.mdtcloud.io/application-onboarding/faq/#do-you-have-an-infrastructure-diagram).
    - Review how applications are deployed, monitored, and managed on the platform.
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

- **Documentation Review**:
    - Study the existing CI/CD pipeline configurations, deployment workflows, and automation scripts.

---

### **Week 2: Tools and Processes**
- **CI/CD Pipeline Familiarity**:
    - Dive into GitLab CI pipeline configurations to understand existing stages (e.g., build, manage namespace, deploy).
    - Review existing integrations with Terraform, testing frameworks, and deployment scripts.

- **Infrastructure Automation Exploration**:
    - Study existing automation scripts for common infrastructure tasks (e.g., managing IAM roles, SSL certificates, etc.).
    - Understand any pain points or manual processes that could benefit from automation.

- **Deployment and Monitoring**:
    - Observe at least two application deployments to understand pipeline execution, logging, and error handling.
    - Learn how monitoring tools (e.g., Prometheus, Grafana, CloudWatch) are used to validate deployments.

---

### **Week 3–4: Shadowing and Initial Contributions**
- **Shadowing and Observing**:
    - Join pipeline reviews and deployment planning meetings.
    - Shadow the DevOps or SRE team during an incident or deployment to observe troubleshooting workflows.

- **Hands-on Tasks**:
    - Start with small pipeline enhancements (e.g., adding new environment variables, optimizing existing scripts).
    - Assist with automating a repetitive task, such as log analysis or resource cleanup.

- **Documentation Improvements**:
    - Update documentation for pipeline configurations or newly automated workflows.

---

## **Goals for the First 3 Months**

### **Month 1: Foundations**
- Gain a comprehensive understanding of the existing CI/CD pipelines and Terraform workflows.
- Document areas of improvement in pipeline efficiency, logging, or error handling.
- Make at least one minor contribution to the CI/CD pipeline (e.g., enhancing a job or fixing a script).

---

### **Month 2: Independence**
- Propose and implement enhancements to the pipeline, such as:
    - Reducing build times by optimizing Docker caching.
    - Automating cleanup of unused resources.
    - Improving accuracy of pipeline messaging or outputs.
- Develop and deploy an automation script to simplify infrastructure tasks (e.g., backup processes, IAM role management).
- Collaborate with the QA team to integrate additional automated testing into the pipeline.

---

### **Month 3: Driving Impact**
- Lead the development and deployment of a medium-sized pipeline improvement:
    - Example: Improving CI/CD workflows or improving rollback mechanisms.
- Build and implement a robust automation tool for infrastructure management (e.g., self-healing scripts for common issues).
- Deliver a presentation or report to the team on:
    - Pipeline enhancements and their impact (e.g., reduced build time, fewer manual steps).
    - Infrastructure automation benefits (e.g., reduced toil, faster incident resolution).

---

## **Key Success Metrics**
- **Pipeline Enhancements**: Deliver at least two meaningful CI/CD pipeline improvements with measurable impact (e.g., faster execution, better error handling).
- **Automation Development**: Implement at least one automation tool or script that improves infrastructure reliability or reduces manual effort.
- **Collaboration**: Actively engage with QA, DevOps, and SRE teams to align pipeline and automation improvements with platform needs.
- **Documentation**: Create or update documentation for all new pipeline configurations and automation scripts.
- **Incident Support**: Assist in debugging at least one deployment issue or infrastructure incident using automation or pipeline adjustments.

---
