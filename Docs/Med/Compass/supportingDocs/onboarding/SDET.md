# Onboarding SDET Role for Argo Platform

## Onboarding Plan

### **Week 1: Getting Started**

- **Platform Overview**:
  - Review the [EKS cluster architecture](https://playbook.argo-dev.eks.mdtcloud.io/application-onboarding/faq/#do-you-have-an-infrastructure-diagram).
  - Understand how testing is integrated into the CI/CD pipeline for:
    - Our CI/CD pipeline projects:
      - [Manage Namespace](https://code.medtronic.com/bcp_web/devops/prometheus/manage-namespace)
      - [Build Image](https://code.medtronic.com/bcp_web/devops/prometheus/build-app-image)
      - [Deploy Image](https://code.medtronic.com/bcp_web/devops/prometheus/deploy-image)
    - Applications using the platform (TBD - no standards created yet)

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
  - Gain access to:
    - [GitLab](https://medtronic.gitlab-dedicated.com/) - Submit ticket to Infra-SourceCode-Global to request `Developer` access to `BCP_Web` for your `<userid>`
    - [AWS Accounts](https://medtronicsso.awsapps.com/start/) by submitting a ServiceNow INC assigned to Cloud-Global. Request access to the below accounts with the same permissions/roles as Ash Montebello. Email [AWS account owner](mailto:ash.l.montebello@medtronic.com?subject=Argo%20AWS%20Account%20Access%20Approval&body=Please%20approve%20access%20to%20the%20following%20Argo%20AWS%20accounts%3A%0A%0Ait-argo-dev-mdt%20(389242548790)%0Ait-argo-prod-mdt%20(872019488961)%0Ait-argobservability-dev-mdt%20(227143426852)%0Ait-wasobserv-monitor-prod-mdt%20(546946040407)) for approval.
      - it-argo-dev-mdt (389242548790)
        - Argo dev account. Houses customer applications (DEV & TESTING environments).
      - it-argo-prod-mdt (872019488961)
        - Argo prod account. Houses customer applications (STAGING, RELEASE, & PRODUCTION environments).
      - it-argobservability-dev-mdt (227143426852)
        - Dev observability account. Used for POCs related to observability - has cross-account access to it-argo-dev-mdt CloudWatch for logs/metrics.
      - it-wasobserv-monitor-prod-mdt (546946040407)
        - Prod observability account. Contains required Grafana setup and has cross-account access to it-argo-dev-mdt and it-argo-prod-mdt for retrieving CloudWatch logs/metrics.
      - it-cicdcds-dev-mdt (563893293061)
        - Dedicated dev account for our team. Can be used for any POCs before moving to it-argo-dev-mdt account.
    - [Monitoring/alerting tools](https://playbook.argo-dev.eks.mdtcloud.io/application-onboarding/tool-access/#grafana-dashboards-logs-and-metrics).
    - [Port](https://app.getport.io/org_fWJXQcLuuD11DeJs/self-serve)
      - Port access must be configured by an existing team member using this [workflow](https://app.getport.io/org_fWJXQcLuuD11DeJs/self-serve?workflow=port_team_add_ad_user%2Ftrigger)
      - Add both groups to the user:
        - `atlas-port-it-sharedservices`
        - `atlas-port-platform-engineering`
    - [Terraform Cloud (TFC)](https://app.terraform.io/)
      - Add Argo TFC access using this [Port workflow](https://app.getport.io/org_fWJXQcLuuD11DeJs/self-serve?workflow=add_ad_user_to_ad_group%2Ftrigger)
      - Add both groups to the user:
        - `atlas-tfc-global-users`
        - `atlas-tfc-it-argo`
    - [Dynatrace](https://medtronic.dynatrace.com/)
      - Add Dynatrace access using this [Port workflow](https://app.getport.io/org_fWJXQcLuuD11DeJs/self-serve?workflow=add_ad_user_to_ad_group%2Ftrigger)
      - Add both groups to the user:
        - `atlas-dynatrace-global-users`
        - `ENT-ATLAS_dynatrace_users-SECURE`
    - Clone and explore the GitLab projects related to our product:
        * [Semantic Release](https://medtronic.gitlab-dedicated.com/bcp_web/devops/semantic-release): contains all shared pipeline templates
        * [flux-gitops](https://medtronic.gitlab-dedicated.com/bcp_web/devops/fluxconfigs/flux-gitops): stores base k8s files for onboarded apps + k8s infrastructure
        * [einstein](https://medtronic.gitlab-dedicated.com/bcp_web/common/einstein): sample project using our pipeline templates
    - Clone and explore the Terraform projects for IaC:
        * [Argo-Dev](https://medtronic.gitlab-dedicated.com/bcp_web/devops/infrastructure/terraform-deployments/aws-it-argo-dev-mdt)
        * [Argo-Prod](https://medtronic.gitlab-dedicated.com/bcp_web/devops/infrastructure/terraform-deployments/aws-it-argo-prod-mdt)
  - Watch related videos located on our [Teams Channel](https://medtronic.sharepoint.com/:f:/r/sites/WebDevModernization/Shared%20Documents/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)/Compass%20CI/Demos%20and%20Recordings?csf=1&web=1&e=0WjOe1).

- **Tooling Setup**:
  - Gain access to GitLab, testing frameworks, Terraform code repositories, and test result dashboards.
  - Set up testing tools and frameworks locally (e.g., Selenium, JUnit, Pytest, or others in use).

- **Meet the Team**:
  - Understand team expectations for testing shared infrastructure and applications.

- **Documentation Review**:
  - Study existing documentation for test cases, automation frameworks, and pipeline integration.

---

### **Week 2: Tools and Processes**

- **Testing Framework Familiarity**:
  - Explore the existing test frameworks and review the suite of integration, end-to-end (E2E), and infrastructure tests.
  - Run a few sample tests locally and through the CI pipeline.

- **CI/CD Pipeline Exploration**:
  - Review how tests are integrated into the GitLab pipelines for our own Argo pipeline updates.
  - Review how tests are integrated into the GitLab pipelines for application deployments (Not applicable - yet).
  - Observe pipeline execution and the handling of test failures.

- **Automation Standards**:
  - Understand the team's approach to test case design, version control, and test data management (what tools should we support? what should be used for test case management?).
  - Familiarize yourself with Terraform testing tools (e.g., Terratest, InSpec) and any custom validation scripts (Not applicable - yet).

---

### **Week 3–4: Shadowing and Initial Contributions**

- **Shadowing and Observing**:
  - Shadow the team to understand the current testing strategy for Terraform and CI/CD pipeline changes.
  - Observe testing in real-world scenarios (e.g., new feature deployments or bug fixes).

- **Hands-on Tasks**:
  - Execute and debug existing test cases.
  - Assist in triaging test failures and investigating root causes.

- **Documentation Improvements**:
  - Update or enhance documentation for existing test cases or automation frameworks.

---

## **Goals for the First 3 Months**

### **Month 1: Foundations**

- Gain a thorough understanding of the infrastructure and its dependencies.
- Be able to run all existing test suites locally and in the CI/CD pipeline.
- Document any gaps or inconsistencies in current test automation or validation processes.
- Shadow at least three deployments to observe testing workflows.

---

### **Month 2: Independence**

- Write new test cases for a small feature or infrastructure change (e.g., Terraform module or CI/CD pipeline).
- Propose and implement improvements to one aspect of test automation, such as:
  - Reducing test execution time.
  - Improving test coverage for critical infrastructure components.
  - Improving accuracy of test outcomes.
- Contribute to debugging and resolving at least one test-related issue in the CI/CD pipeline.
- Develop a POC with our [Newton](https://code.medtronic.com/bcp_web/common/newton) application to demonstrate test case integrations within the CI/CD pipeline such as:
  - Unit tests
  - E2E tests
- Begin collaborating with application developers to integrate tests earlier in the pipeline (shift-left testing).

---

### **Month 3: Driving Impact**

- Design and implement a medium-sized test automation enhancement, such as:
  - Adding end-to-end tests for an EKS component (e.g., ingress controllers, autoscalers).
  - Improving infrastructure validation for Terraform changes.
  - Developing a roadmap for integrating a set of standard test frameworks and test case management for application teams to use in their pipelines.
- Create or enhance a testing framework for a specific need (e.g., mocking AWS services, load testing for EKS pods).
- Present findings and recommendations to improve the testing strategy, covering:
  - Gaps in current test coverage.
  - Opportunities to automate repetitive validation tasks.
  - Suggestions for pipeline optimization to handle test failures.

---

## **Key Success Metrics**

- **Test Case Development**: Write and implement at least three new test cases for the infrastructure or applications.
- **Automation**: Contribute to at least one significant improvement in the automation framework or CI pipeline.
- **Incident Handling**: Debug and resolve at least two test failures during pipeline execution.
- **Collaboration**: Work closely with developers and DevOps teams to integrate and maintain robust testing practices.
- **Documentation**: Update or create comprehensive documentation for all new and modified tests.

---
