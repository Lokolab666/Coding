Title: Request JFrog Docker repository and GitLab CI publishing details for Training GPT Proxy

Description:

I am preparing the Training GPT Proxy application for deployment to Amazon ECS Express Mode. The application is packaged as a Docker image, and our intended deployment flow is:


The application repository and GitLab pipeline already exist. I can access the JFrog instance at
https://case.artifacts.medtronic.com
, but I need the approved repository, authentication, scanning, and promotion details before configuring the pipeline.

Please provide or help configure the following:

The JFrog Docker repository this project should publish to.
The exact Docker registry/login URL and image naming convention.
The approved authentication method for GitLab CI:
CI service account
JFrog access token
OIDC or another supported method
The process for requesting write/deploy permissions to the repository.
Whether JFrog Xray scans Docker images automatically.
Any required Xray policy, build-info publication, scan, or approval gate before deployment.
The approved process for copying or promoting an approved image from JFrog into Amazon ECR.
Whether an existing GitLab CI template should be used for JFrog publishing and scanning.
Any required image tags, labels, retention rules, or release naming conventions.
The appropriate team or documentation for troubleshooting this integration.
Project details:

Application: Training GPT Proxy
Artifact type: Docker/OCI container image
Source CI/CD system: GitLab CI
JFrog instance:
case.artifacts.medtronic.com

AWS account: corp-pdna-aitools-dev-mdt (293968602262)
AWS region: us-east-1
Deployment target: Amazon ECS Express Mode
Proposed ECR repository: training-gpt-proxy