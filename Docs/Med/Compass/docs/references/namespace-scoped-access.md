---
label: Kubernetes Access
icon: /static/logos/kubernetes.svg
order: 45
---

# Namespace-Scoped Access for Application Teams

This guide describes how to provide application teams with read-only access to their Kubernetes namespace resources via AWS Cloud9 or kubectl, while maintaining security boundaries.

**This is configured as part of standard application onboarding** using Flux GitOps for Kubernetes resources and Terraform for AWS infrastructure.

---

## Overview

Namespace-scoped access allows application teams to:
- View their pods, deployments, and services
- Read application logs directly via kubectl
- Troubleshoot issues without platform team intervention
- Access only their namespace (no cluster-wide visibility)

**Access is read-only by default** to prevent accidental changes to production workloads.

---

## Architecture

```
Application Team Member
    ↓
AWS SSO / IAM Role (Terraform-managed)
    ↓
EKS aws-auth ConfigMap (Flux GitOps)
    ↓
Kubernetes RBAC (Flux GitOps)
    ↓
Namespace-scoped permissions
```

---

## Prerequisites

- Application onboarded to Compass CI platform
- Application namespace exists in target cluster(s)
- **ServiceNow access** to submit requests to Cloud-Global team

---

## Step 0: Request AWS SSO Role Creation (Cloud-Global)

**Time to Complete: ~1 week** (requires CHG records and coordination between IAM and Cloud-Global teams)

Before creating Terraform resources, the AWS SSO role and AD group must be created by the **Cloud-Global team** via ServiceNow. This is a prerequisite for the Terraform-managed IAM role.

### 0.1 Gather Required Information

Collect the following details:
- **Application name** (e.g., `myapp`)
- **Environment** (dev, production)
- **AWS accounts** where access is needed:
  - `it-argo-dev-mdt` (389242548790) for dev/test environments
  - `it-argo-prod-mdt` (872019488961) for staging/release/production environments
- **AD group name** (e.g., `CN-MyApp-Developers`) or list of users
- **Role name** (e.g., `AWSREF_MyApp-ViewerRole`)

### 0.2 Submit ServiceNow Request

[Submit a ServiceNow ticket](https://medtronicprod.service-now.com/now/nav/ui/classic/params/target/incident.do) assigned to `Cloud-Global` with the following details:

```text
Assignment Group: Cloud-Global
Configuration Item: AWS
Category: Account / Security
Subcategory: license_access

Short description: Create AWS SSO Role for MyApp View-Only Access

Description:
Please setup a new SSO role and associated AD group for MyApp team viewer access in it-argo-prod-mdt (872019488961) account.

New Role Name: AWSREF_MyApp-ViewerRole

This role will be used for read-only access to app-specific resources and Kubernetes resources in the myapp-production namespace. The team will access the namespace via a Cloud9 environment.

Inline policy:
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "EKSClusterAccess",
      "Effect": "Allow",
      "Action": [
        "eks:DescribeCluster",
        "eks:ListClusters"
      ],
      "Resource": "arn:aws:eks:us-east-1:872019488961:cluster/TF-argo-prd"
    },
    {
      "Sid": "Cloud9EnvironmentAccess",
      "Effect": "Allow",
      "Action": [
        "cloud9:DescribeEnvironments",
        "cloud9:GetUserSettings",
        "cloud9:ListEnvironments",
        "cloud9:DescribeEnvironmentMemberships"
      ],
      "Resource": "*"
    },
    {
      "Sid": "SSMSessionManagerForCloud9",
      "Effect": "Allow",
      "Action": [
        "ssm:StartSession"
      ],
      "Resource": [
        "arn:aws:ec2:us-east-1:872019488961:instance/*",
        "arn:aws:ssm:us-east-1:872019488961:document/AWS-StartSSHSession"
      ]
    },
    {
      "Sid": "SSMSessionStatus",
      "Effect": "Allow",
      "Action": [
        "ssm:DescribeSessions",
        "ssm:GetConnectionStatus",
        "ssm:DescribeInstanceInformation"
      ],
      "Resource": "*"
    },
    {
      "Sid": "EC2InstanceInfo",
      "Effect": "Allow",
      "Action": [
        "ec2:DescribeInstances",
        "ec2:DescribeInstanceStatus"
      ],
      "Resource": "*"
    },
    {
      "Sid": "CloudWatchLogsAccess",
      "Effect": "Allow",
      "Action": [
        "logs:GetLogEvents",
        "logs:FilterLogEvents",
        "logs:DescribeLogStreams",
        "logs:DescribeLogGroups"
      ],
      "Resource": "arn:aws:logs:us-east-1:872019488961:log-group:/aws/containerinsights/TF-argo-prd/*:*",
      "Condition": {
        "StringLike": {
          "logs:LogStreamName": "*myapp-production*"
        }
      }
    }
  ]
}

Add the following users/AD group to the role:
Doe, John (doej123)
Smith, Jane (smitj456)
Montebello, Ash (montea4)
```

!!! Initial Permissions
The initial inline policy above includes:
- **EKS access**: Describe cluster for kubectl authentication
- **Cloud9 access**: List and open shared Cloud9 environments
- **SSM Session Manager**: Connect to Cloud9's underlying EC2 instance (no tag filtering - Cloud9 instances can be shared across all teams/namespaces)
- **EC2 describe**: View instance status in AWS console
- **CloudWatch Logs**: Read container logs from the namespace

**Note**: Cloud9 environments can be **shared across multiple teams and namespaces**. Once connected, each user's AWS credentials (from their SSO role) determine what kubectl commands they can run. There's no need for namespace-specific Cloud9 instances since user permissions are enforced by their own IAM role + Kubernetes RBAC.

Additional permissions (S3, RDS, SQS, Secrets Manager, etc.) can be added later via ServiceNow request. See [Updating IAM Role Permissions](#updating-iam-role-permissions-aws-sso-role) in the Maintenance section.
!!!

### 0.3 Verify SSO Role Creation

Once Cloud-Global fulfills the request (~1 week):

1. Navigate to [AWS SSO Start Screen](https://medtronicsso.awsapps.com/start/#/?tab=accounts)
2. Verify you see the new role listed under the appropriate AWS account
3. Click the role to access the AWS console and confirm permissions work
4. **Note the IAM role ARN** - you'll need this for Terraform (format: `arn:aws:iam::872019488961:role/AWSReservedSSO_AWSREF_MyApp-ViewerRole_<random-id>`)

!!! Role ARN Format
AWS SSO roles have a specific naming format: `AWSReservedSSO_<YourRoleName>_<RandomID>`. The Cloud-Global team will provide the full ARN once created.
!!!

### 0.4 Proceed to Terraform

Once the SSO role exists, proceed to Step 1 to configure the IAM policies and EKS permissions via Terraform.

---

## Step 1: Configure IAM Role Permissions (Terraform)

Now that the SSO role exists (created by Cloud-Global in Step 0), use Terraform to attach the necessary IAM policies for EKS and CloudWatch access.

### 1.1 Define Terraform Configuration

Create or update the Terraform configuration for the application's IAM role policies:

**File: `terraform/eks-viewer-roles/myapp-viewer.tf`**

```
# Data source to reference the SSO-created IAM role
data "aws_iam_role" "myapp_eks_viewer_sso_role" {
  name = "AWSReservedSSO_AWSREF_MyApp-ViewerRole_abc123xyz"  # Replace with actual role name from Step 0
}

# Attach EKS cluster access policy
resource "aws_iam_role_policy" "myapp_eks_access" {
  name = "myapp-eks-cluster-access"
  role = data.aws_iam_role.myapp_eks_viewer_sso_role.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "eks:DescribeCluster",
          "eks:ListClusters"
        ]
        Resource = "arn:aws:eks:us-east-1:872019488961:cluster/TF-argo-prd"
      }
    ]
  })
}

# Attach Cloud9 access policy (if providing Cloud9 environments)
resource "aws_iam_role_policy" "myapp_cloud9_access" {
  name = "myapp-cloud9-access"
  role = data.aws_iam_role.myapp_eks_viewer_sso_role.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "cloud9:DescribeEnvironments",
          "cloud9:GetUserSettings",
          "cloud9:ListEnvironments",
          "cloud9:DescribeEnvironmentMemberships"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "ssm:StartSession"
        ]
        Resource = [
          "arn:aws:ec2:us-east-1:872019488961:instance/*",
          "arn:aws:ssm:us-east-1:872019488961:document/AWS-StartSSHSession"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "ssm:DescribeSessions",
          "ssm:GetConnectionStatus",
          "ssm:DescribeInstanceInformation"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "ec2:DescribeInstances",
          "ec2:DescribeInstanceStatus"
        ]
        Resource = "*"
      }
    ]
  })
}

# Attach CloudWatch Logs access policy (namespace-filtered)
resource "aws_iam_role_policy" "myapp_cloudwatch_logs" {
  name = "myapp-cloudwatch-logs-access"
  role = data.aws_iam_role.myapp_eks_viewer_sso_role.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:GetLogEvents",
          "logs:FilterLogEvents",
          "logs:DescribeLogStreams"
        ]
        Resource = "arn:aws:logs:us-east-1:872019488961:log-group:/aws/containerinsights/TF-argo-prd/*:*"
        Condition = {
          StringLike = {
            "logs:LogStreamName" = "*myapp-production*"
          }
        }
      },
      {
        Effect = "Allow"
        Action = [
          "logs:DescribeLogGroups"
        ]
        Resource = "*"
      }
    ]
  })
}

output "myapp_viewer_role_arn" {
  value       = data.aws_iam_role.myapp_eks_viewer_sso_role.arn
  description = "ARN of SSO IAM role for MyApp EKS viewer access"
}

output "myapp_viewer_role_name" {
  value       = data.aws_iam_role.myapp_eks_viewer_sso_role.name
  description = "Name to use in EKS aws-auth ConfigMap"
}
```

!!!info Cloud9 Access Requirements & Sharing
If providing Cloud9 environments (Step 3), the Cloud9 access policy above is required. It grants:
- **Cloud9 environment access**: View and open Cloud9 IDEs
- **SSM Session Manager**: Connect to Cloud9's underlying EC2 instance
- **EC2 describe**: View instance status in AWS console

**Cloud9 instances can be shared** across multiple users, teams, and namespaces. Each user's permissions inside Cloud9 are determined by **their own AWS SSO role**, not the Cloud9 instance. When a user runs kubectl commands in Cloud9, they authenticate using their own IAM role, which limits them to their authorized namespaces/resources via Kubernetes RBAC.

No namespace-specific Cloud9 instances are needed - a single Cloud9 environment can serve multiple teams securely.

Without these permissions, users cannot access the Cloud9 environment even if it's created.
!!!

!!!warning SSO Role Management
- IAM roles created via AWS SSO **cannot be created or deleted via Terraform**
- Terraform can only attach/detach **policies** to SSO-created roles
- Use `data.aws_iam_role` to reference the SSO role, not `resource.aws_iam_role`
- Role name changes require new ServiceNow request to Cloud-Global
!!!

### 1.2 Alternative: Reusable Terraform Module (Optional)

If you manage multiple applications, create a reusable module to attach policies to SSO roles:

**File: `terraform/modules/eks-namespace-viewer-policy/main.tf`**

```
variable "sso_role_name" {
  description = "Name of the SSO-created IAM role"
  type        = string
}

variable "cluster_name" {
  description = "EKS cluster name"
  type        = string
}

variable "cluster_arn" {
  description = "EKS cluster ARN"
  type        = string
}

variable "namespace" {
  description = "Kubernetes namespace"
  type        = string
}

variable "application_name" {
  description = "Application name (used for Cloud9 filtering)"
  type        = string
}

variable "enable_cloudwatch_logs" {
  description = "Enable CloudWatch Logs access"
  type        = bool
  default     = true
}

variable "enable_cloud9_access" {
  description = "Enable Cloud9 environment access"
  type        = bool
  default     = false
}

variable "log_group_prefix" {
  description = "CloudWatch log group prefix"
  type        = string
  default     = "/aws/containerinsights"
}

variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "aws_account_id" {
  description = "AWS account ID"
  type        = string
}

# EKS cluster access policy
resource "aws_iam_role_policy" "eks_access" {
  name = "${var.namespace}-eks-access"
  role = var.sso_role_name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "eks:DescribeCluster",
          "eks:ListClusters"
        ]
        Resource = var.cluster_arn
      }
    ]
  })
}

# CloudWatch Logs access policy (namespace-filtered)
resource "aws_iam_role_policy" "cloudwatch_logs" {
  count = var.enable_cloudwatch_logs ? 1 : 0
  name  = "${var.namespace}-cloudwatch-logs"
  role  = var.sso_role_name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:GetLogEvents",
          "logs:FilterLogEvents",
          "logs:DescribeLogStreams"
        ]
        Resource = "arn:aws:logs:${var.aws_region}:${var.aws_account_id}:log-group:${var.log_group_prefix}/${var.cluster_name}/*:*"
        Condition = {
          StringLike = {
            "logs:LogStreamName" = "*${var.namespace}*"
          }
        }
      }
    ]
  })
}

# Cloud9 access policy (optional)
resource "aws_iam_role_policy" "cloud9_access" {
  count = var.enable_cloud9_access ? 1 : 0
  name  = "${var.namespace}-cloud9-access"
  role  = var.sso_role_name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "cloud9:DescribeEnvironments",
          "cloud9:GetUserSettings",
          "cloud9:ListEnvironments",
          "cloud9:DescribeEnvironmentMemberships"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "ssm:StartSession"
        ]
        Resource = [
          "arn:aws:ec2:${var.aws_region}:${var.aws_account_id}:instance/*",
          "arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:document/AWS-StartSSHSession"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "ssm:DescribeSessions",
          "ssm:GetConnectionStatus",
          "ssm:DescribeInstanceInformation"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "ec2:DescribeInstances",
          "ec2:DescribeInstanceStatus"
        ]
        Resource = "*"
      }
    ]
  })
}

output "role_name" {
  value       = var.sso_role_name
  description = "SSO IAM role name"
}
```

**Usage:**

```
# terraform/eks-viewer-roles/myapp-viewer.tf
data "aws_iam_role" "myapp_sso_role" {
  name = "AWSReservedSSO_AWSREF_MyApp-ViewerRole_abc123xyz"
}

module "myapp_viewer_policies" {
  source = "../../modules/eks-namespace-viewer-policy"

  sso_role_name    = data.aws_iam_role.myapp_sso_role.name
  cluster_name     = "TF-argo-prd"
  cluster_arn      = "arn:aws:eks:us-east-1:872019488961:cluster/TF-argo-prd"
  namespace        = "myapp-production"
  application_name = "myapp"
  aws_region       = "us-east-1"
  aws_account_id   = "872019488961"

  enable_cloudwatch_logs = true
  enable_cloud9_access   = true  # Enable Cloud9 environment access
  log_group_prefix       = "/aws/containerinsights"
}
```

### 1.3 Apply Terraform

Run via CI/CD pipeline in the associated Terraform project in GitLab.

---

## Step 2: Configure Kubernetes RBAC (Flux GitOps)

### 2.1 Create RBAC Resources in flux-gitops Repository

**File: `flux-gitops/clusters/TF-argo-prd/apps/myapp-production/rbac.yaml`**

```yaml
---
apiVersion: v1
kind: Namespace
metadata:
  name: myapp-production
  labels:
    app: myapp
    team: myapp-team
---
# Namespace-scoped viewer role
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: namespace-viewer
  namespace: myapp-production
  labels:
    app: myapp
    rbac.authorization.k8s.io/aggregate-to-view: "true"
rules:
# Pods and logs
- apiGroups: [""]
  resources: ["pods", "pods/log"]
  verbs: ["get", "list", "watch"]
# Deployments and ReplicaSets
- apiGroups: ["apps"]
  resources: ["deployments", "replicasets", "statefulsets", "daemonsets"]
  verbs: ["get", "list", "watch"]
# Services and Endpoints
- apiGroups: [""]
  resources: ["services", "endpoints"]
  verbs: ["get", "list", "watch"]
# ConfigMaps (no secrets)
- apiGroups: [""]
  resources: ["configmaps"]
  verbs: ["get", "list", "watch"]
# Events
- apiGroups: [""]
  resources: ["events"]
  verbs: ["get", "list", "watch"]
# Ingress
- apiGroups: ["networking.k8s.io"]
  resources: ["ingresses"]
  verbs: ["get", "list", "watch"]
# HPA
- apiGroups: ["autoscaling"]
  resources: ["horizontalpodautoscalers"]
  verbs: ["get", "list", "watch"]
# Jobs and CronJobs
- apiGroups: ["batch"]
  resources: ["jobs", "cronjobs"]
  verbs: ["get", "list", "watch"]
# PVCs
- apiGroups: [""]
  resources: ["persistentvolumeclaims"]
  verbs: ["get", "list", "watch"]
# Helm releases and charts (Flux)
- apiGroups: ["helm.toolkit.fluxcd.io"]
  resources: ["helmreleases"]
  verbs: ["get", "list", "watch"]
- apiGroups: ["source.toolkit.fluxcd.io"]
  resources: ["helmcharts"]
  verbs: ["get", "list", "watch"]
# Kustomizations (Flux)
- apiGroups: ["kustomize.toolkit.fluxcd.io"]
  resources: ["kustomizations"]
  verbs: ["get", "list", "watch"]
---
# RoleBinding for application team
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: myapp-team-viewer-binding
  namespace: myapp-production
  labels:
    app: myapp
subjects:
- kind: Group
  name: myapp-viewers  # Matches group in aws-auth ConfigMap
  apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: Role
  name: namespace-viewer
  apiGroup: rbac.authorization.k8s.io
```

### 2.2 Update Kustomization

**File: `flux-gitops/clusters/TF-argo-prd/apps/myapp-production/kustomization.yaml`**

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
namespace: myapp-production

resources:
  - rbac.yaml
  - deployment.yaml
  - service.yaml
  - ingress.yaml
  # ... other app resources
```

### 2.3 Update aws-auth ConfigMap

**Action Required:** Submit a request to the Shared Services team to add your application's IAM role mapping.

**Example request:**
```text
Team: Shared Services
Request: Add IAM role mapping to aws-auth ConfigMap for MyApp

IAM Role ARN: arn:aws:iam::872019488961:role/AWSReservedSSO_AWSREF_MyApp-ViewerRole_abc123
Kubernetes Username: myapp-team-viewer
Kubernetes Groups: myapp-viewers
Cluster: TF-argo-prd
Namespace: myapp-production

This role is used for namespace-scoped viewing access as documented in the Compass CI Playbook.
```

The Shared Services team will add this entry to their aws-auth ConfigMap source:

**File: `shared-services-flux/base/aws-auth/aws-auth-configmap.yaml`** (managed by Shared Services)

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: aws-auth
  namespace: kube-system
data:
  mapRoles: |
    # Platform team cluster admin access
    - rolearn: arn:aws:iam::123456789012:role/PlatformTeamEKSAdmin
      username: platform-admin
      groups:
        - system:masters

    # Node IAM role
    - rolearn: arn:aws:iam::123456789012:role/TF-argo-prd-node-role
      username: system:node:{{EC2PrivateDNSName}}
      groups:
        - system:bootstrappers
        - system:nodes

    # Application team viewer roles (added by Shared Services per app team request)
    - rolearn: arn:aws:iam::872019488961:role/AWSReservedSSO_AWSREF_MyApp-ViewerRole_abc123
      username: myapp-team-viewer
      groups:
        - myapp-viewers

    # Add additional application teams below
    # - rolearn: arn:aws:iam::123456789012:role/otherapp-production-eks-viewer
    #   username: otherapp-team-viewer
    #   groups:
    #     - otherapp-viewers
```

### 2.5 Verify aws-auth ConfigMap

After Shared Services updates the aws-auth ConfigMap, verify your IAM role mapping exists:

```bash
# View the aws-auth ConfigMap
kubectl get configmap aws-auth -n kube-system -o yaml

# Search for your role ARN
kubectl get configmap aws-auth -n kube-system -o yaml | grep -A3 "myapp-team-viewer"
```

Expected output:
```yaml
- rolearn: arn:aws:iam::872019488961:role/AWSReservedSSO_AWSREF_MyApp-ViewerRole_abc123
  username: myapp-team-viewer
  groups:
    - myapp-viewers
```

### 2.6 Test Access

Have a team member test access:

```bash
# Update kubeconfig with the role
aws eks update-kubeconfig \
  --region us-east-1 \
  --name TF-argo-prd \
  --role-arn arn:aws:iam::872019488961:role/AWSReservedSSO_AWSREF_MyApp-ViewerRole_abc123

# Test namespace access
kubectl get pods -n myapp-production

# Verify no cross-namespace access
kubectl get pods -n other-namespace  # Should fail with Forbidden error
```

---

## Step 3: Create Cloud9 Environment (Terraform) - Optional

### 3.1 Cloud9 Terraform Module

**Important**: A single Cloud9 environment can be **shared across multiple teams, applications, and namespaces**. Each user's permissions are based on their own AWS SSO role credentials, not the Cloud9 instance. You may only need **one Cloud9 instance per cluster** rather than one per namespace.

**File: `terraform/cloud9/cluster-cloud9.tf`** (example for shared cluster-wide Cloud9)

```
module "cluster_cloud9" {
  source = "../../modules/cloud9-namespace-viewer"

  application_name = "shared"
  environment      = "production"

  # Use private subnet in EKS VPC
  subnet_id = data.aws_subnet.eks_private_subnet.id

  # Attach the viewer IAM role
  iam_role_arn = module.myapp_eks_viewer_role.role_arn

  # EC2 instance settings
  instance_type = "t3.small"

  # Auto-configure kubectl on startup
  eks_cluster_name = "TF-argo-prd"
  namespace        = "myapp-production"

  # Team members who can access Cloud9
  owner_arn = "arn:aws:iam::123456789012:user/platform-team"

  tags = {
    Application = "shared"
    Team        = "Platform"
    ManagedBy   = "Terraform"
    Purpose     = "Multi-team kubectl access"
  }
}

output "cluster_cloud9_url" {
  value       = module.cluster_cloud9.cloud9_url
  description = "URL to access shared Cloud9 environment"
}
```

### 3.2 Cloud9 Module Definition

**File: `terraform/modules/cloud9-namespace-viewer/main.tf`**

```
variable        = "${var.application_name}-${var.environment}-viewer"
    Application = var.application_name  # Used for SSM access filtering
  description = "Name of the application"
  type        = string
}

variable "environment" {
  description = "Environment (dev, staging, production)"
  type        = string
}

variable "subnet_id" {
  description = "Private subnet ID for Cloud9 instance"
  type        = string
}

variable "iam_role_arn" {
  description = "IAM role ARN to attach to Cloud9 instance"
  type        = string
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t3.small"
}

variable "eks_cluster_name" {
  description = "EKS cluster name to configure"
  type        = string
}

variable "namespace" {
  description = "Kubernetes namespace to configure access for"
  type        = string
}

variable "owner_arn" {
  description = "ARN of user/role who owns the environment"
  type        = string
}

variable "tags" {
  description = "Tags to apply to resources"
  type        = map(string)
  default     = {}
}

# Cloud9 environment
resource "aws_cloud9_environment_ec2" "viewer" {
  name                        = "${var.application_name}-${var.environment}-viewer"
  description                 = "Namespace-scoped kubectl access for ${var.application_name} team"
  instance_type               = var.instance_type
  subnet_id                   = var.subnet_id
  automatic_stop_time_minutes = 30
  image_id                    = "amazonlinux-2023-x86_64"

  tags = merge(var.tags, {
    Name = "${var.application_name}-${var.environment}-viewer"
  })
}

# Instance profile for Cloud9
resource "aws_iam_instance_profile" "cloud9" {
  name = "${var.application_name}-${var.environment}-cloud9-profile"
  role = aws_iam_role.cloud9.name
}

resource "aws_iam_role" "cloud9" {
  name = "${var.application_name}-${var.environment}-cloud9-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })

  tags = var.tags
}

# Allow Cloud9 to assume the viewer role
resource "aws_iam_role_policy" "assume_viewer_role" {
  name = "assume-viewer-role"
  role = aws_iam_role.cloud9.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = "sts:AssumeRole"
        Resource = var.iam_role_arn
      }
    ]
  })
}

# SSM policy for Cloud9 connectivity
resource "aws_iam_role_policy_attachment" "ssm" {
  role       = aws_iam_role.cloud9.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# User data to configure kubectl
locals {
  user_data = <<-EOT
    #!/bin/bash

    # Install kubectl
    curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
    sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
    rm kubectl

    # Install AWS CLI v2
    curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
    unzip awscliv2.zip
    sudo ./aws/install
    rm -rf aws awscliv2.zip

    # Configure kubectl for EKS
    sudo -u ec2-user bash << 'EOF'
    aws eks update-kubeconfig \
      --region ${data.aws_region.current.name} \
      --name ${var.eks_cluster_name} \
      --role-arn ${var.iam_role_arn}

    # Create helpful aliases
    echo "alias k='kubectl'" >> ~/.bashrc
    echo "alias kgp='kubectl get pods -n ${var.namespace}'" >> ~/.bashrc
    echo "alias kgs='kubectl get svc -n ${var.namespace}'" >> ~/.bashrc
    echo "alias kgd='kubectl get deployment -n ${var.namespace}'" >> ~/.bashrc
    echo "alias kl='kubectl logs -n ${var.namespace}'" >> ~/.bashrc

    # Set default namespace
    kubectl config set-context --current --namespace=${var.namespace}

    # Create welcome message
    cat > ~/README.md << 'WELCOME'
    # ${var.application_name} Cloud9 Environment

    This environment has kubectl access to namespace: ${var.namespace}

    Common commands:
    - kgp          # List pods
    - kgs          # List services
    - kgd          # List deployments
    - kl <pod>     # View logs

    Full kubectl access available (read-only)
    WELCOME

    EOF
  EOT
}

data "aws_region" "current" {}

output "cloud9_url" {
  value       = "https://${data.aws_region.current.name}.console.aws.amazon.com/cloud9/ide/${aws_cloud9_environment_ec2.viewer.id}"
  description = "URL to access Cloud9 environment"
}

output "environment_id" {
  value       = aws_cloud9_environment_ec2.viewer.id
  description = "Cloud9 environment ID"
}
```

### 3.3 Apply Terraform

```bash
cd terraform/cloud9
terraform init
terraform plan
terraform apply

# Get Cloud9 URL
terraform output myapp_cloud9_url
```

---

## Step 4: Onboarding Checklist

When onboarding a new application, complete these steps:

### Platform Team Checklist

- [ ] **Step 0: Request AWS SSO role creation** (~1 week lead time)
  - [ ] Gather application name, environment, AD group/users
  - [ ] Submit ServiceNow ticket to Cloud-Global team
  - [ ] Wait for fulfillment (~1 week for CHG approval and AD/SSO setup)
  - [ ] Verify role appears in [AWS SSO Start Screen](https://medtronicsso.awsapps.com/start/)
  - [ ] Test role access and note the full IAM role ARN

- [ ] **Step 1: Configure IAM role policies via Terraform**
  - [ ] Add Terraform configuration in `terraform/eks-viewer-roles/<app>-viewer.tf`
  - [ ] Reference the SSO-created role using `data.aws_iam_role`
  - [ ] Attach EKS and CloudWatch policies
  - [ ] Run `terraform apply`
  - [ ] Note the IAM role ARN for aws-auth ConfigMap

- [ ] **Step 2: Configure Kubernetes RBAC via Flux**
  - [ ] Create `flux-gitops/clusters/<cluster>/apps/<namespace>/rbac.yaml`
  - [ ] Update kustomization to include rbac.yaml
  - [ ] Commit and push to GitLab
  - [ ] **Request Shared Services team** to add IAM role mapping to aws-auth ConfigMap
  - [ ] Wait for Shared Services to apply changes (~5-10 minutes)
  - [ ] Verify aws-auth mapping: `kubectl get configmap aws-auth -n kube-system -o yaml`
  - [ ] Verify Flux sync: `flux reconcile kustomization apps`
  - [ ] Test access with application team member

- [ ] **Step 3 (Optional): Create Cloud9 environment via Terraform**
  - [ ] Add Terraform configuration in `terraform/cloud9/<app>-cloud9.tf`
  - [ ] Run `terraform apply`
  - [ ] Share Cloud9 URL with application team

- [ ] **Step 4: Notify application team**
  - [ ] Send [access instructions](#user-access-instructions)
  - [ ] Share AWS SSO role name (e.g., `AWSREF_MyApp-EKS-ViewerRole`)
  - [ ] Share Cloud9 URL (if applicable)
  - [ ] Link to [kubectl commands documentation](#common-kubectl-commands)
  - [ ] Provide [AWS SSO start screen](https://medtronicsso.awsapps.com/start/) link

### Application Team Checklist

- [ ] **AWS SSO access configured**
  - [ ] Can access [AWS SSO Start Screen](https://medtronicsso.awsapps.com/start/)
  - [ ] See the EKS viewer role listed under appropriate account
  - [ ] Can log in to AWS console using the role

- [ ] **Verify kubectl access** (if using local machine)
  - [ ] AWS CLI v2 installed
  - [ ] kubectl 1.28+ installed
  - [ ] Configured AWS SSO: `aws configure sso`
  - [ ] Updated kubeconfig: `aws eks update-kubeconfig --name <cluster> --role-arn <role-arn>`
  - [ ] Can list pods: `kubectl get pods -n <namespace>`

- [ ] **Verify Cloud9 access** (if applicable)
  - [ ] Can open Cloud9 environment
  - [ ] kubectl pre-configured and working
  - [ ] Can view namespace resources
  - [ ] Understand auto-stop timeout (30 minutes)

---

## User Access Instructions

### AWS Cloud9

Platform team will provide Cloud9 environment URL.

#### Access the environment
1. Navigate to provided Cloud9 URL
2. Open terminal

#### Configure kubectl

Configure kubectl with **your own AWS SSO role**:
   ```bash
   aws eks update-kubeconfig \
     --region us-east-1 \
     --name TF-argo-prd \
     --role-arn <your-sso-role-arn-from-step-0>
   ```

Run `kubectl get pods` to verify access

!!!info Shared Cloud9 Environment
**Cloud9 environments are shared** among team members. Your permissions are based on **your own AWS SSO role**, not the Cloud9 instance. Each user authenticates with their own credentials, so different users in the same Cloud9 environment may have different access levels.
!!!

---

## GitOps Workflow

All namespace-scoped access configuration is managed via GitOps:

**Infrastructure as Code Flow:**
1. Platform team creates PR in Terraform repository (IAM roles, Cloud9)
2. PR reviewed and merged
3. Terraform pipeline runs, creates AWS resources
4. Platform team creates PR in flux-gitops repository (RBAC, aws-auth)
5. PR reviewed and merged to main
6. Flux controller detects changes within ~1 minute
7. Flux applies Kubernetes manifests to cluster
8. Access is active

**Key Benefits:**
- **No manual kubectl apply**: All Kubernetes resources deployed via Flux
- **No manual IAM changes**: All AWS resources created via Terraform
- **Auditable**: All changes tracked in Git with PR history
- **Repeatable**: Same process for all applications
- **Automated**: Flux reconciles every ~1 minute
- **Rollback capable**: Revert Git commits to undo changes

---

## Common kubectl Commands

### View Resources

```bash
# List all pods
kubectl get pods -n my-app-production

# Watch pods in real-time
kubectl get pods -n my-app-production -w

# View deployments
kubectl get deployments -n my-app-production

# View services
kubectl get svc -n my-app-production

# View ingress
kubectl get ingress -n my-app-production

# View all resources
kubectl get all -n my-app-production
```

### View Logs

```bash
# Tail logs from a pod
kubectl logs -f <pod-name> -n my-app-production

# Last 100 lines
kubectl logs <pod-name> -n my-app-production --tail=100

# Logs from previous container (after crash)
kubectl logs <pod-name> -n my-app-production --previous

# Logs from specific container in multi-container pod
kubectl logs <pod-name> -c <container-name> -n my-app-production

# Logs from all pods with label
kubectl logs -l app=myapp -n my-app-production
```

### Describe Resources

```bash
# Detailed pod information (events, status, etc.)
kubectl describe pod <pod-name> -n my-app-production

# Deployment details
kubectl describe deployment <deployment-name> -n my-app-production

# Service details
kubectl describe svc <service-name> -n my-app-production

# HPA status
kubectl describe hpa <hpa-name> -n my-app-production
```

### View Events

```bash
# Recent events in namespace
kubectl get events -n my-app-production --sort-by='.lastTimestamp'

# Watch events in real-time
kubectl get events -n my-app-production -w

# Filter events by type
kubectl get events -n my-app-production --field-selector type=Warning
```

---

## Troubleshooting

### Error: "User cannot list pods in namespace"

**Cause**: RoleBinding not correctly configured OR IAM role not mapped in aws-auth ConfigMap (managed by Shared Services).

**Fix**:
```bash
# 1. Verify Flux sync status (for your RBAC resources)
flux get kustomizations

# 2. Check if RoleBinding exists
kubectl get rolebinding -n myapp-production

# 3. Check if your IAM role is in aws-auth ConfigMap
kubectl get configmap aws-auth -n kube-system -o yaml | grep myapp

# If your role is NOT in aws-auth:
#   - Contact Shared Services team to add the mapping
#   - Provide IAM role ARN, desired username, and Kubernetes group

# 4. Force Flux reconciliation (for your RBAC resources)
flux reconcile kustomization apps --with-source

# 5. Test access again
kubectl get pods -n myapp-production
```

### Error: "You must be logged in to the server (Unauthorized)"

**Cause**: AWS credentials expired or incorrect role.

**Fix**:
```bash
# Re-authenticate with AWS SSO
aws sso login --profile <profile-name>

# Update kubeconfig
aws eks update-kubeconfig \
  --region us-east-1 \
  --name TF-argo-prd \
  --role-arn arn:aws:iam::123456789012:role/myapp-production-eks-viewer \
  --profile <profile-name>

# Verify credentials
aws sts get-caller-identity
```

### Error: "Forbidden: User cannot get resource"

**Cause**: Role doesn't include the requested resource type.

**Fix**: Update the Role in flux-gitops to include the missing resource:

```yaml
# Add to flux-gitops/clusters/TF-argo-prd/apps/myapp-production/rbac.yaml
- apiGroups: ["<api-group>"]
  resources: ["<resource-type>"]
  verbs: ["get", "list", "watch"]
```

Commit, push, and wait for Flux to sync (~1 minute).

### Terraform Plan Shows Changes to Existing Resources

**Cause**: Terraform state drift or manual changes made outside Terraform.

**Fix**:
```bash
# Review the plan carefully
terraform plan

# If changes are expected, apply
terraform apply

# If changes are drift, import existing resources
terraform import aws_iam_role.eks_viewer <role-name>

# Or refresh state
terraform refresh
```

### Flux Not Syncing RBAC Changes

**Check Flux status:**
```bash
flux get kustomizations
flux get sources git

# Check for errors
flux logs --all-namespaces --since=10m

# Force reconciliation
flux reconcile source git flux-system
flux reconcile kustomization apps
```

### Cloud9 Cannot Access EKS Cluster

**Cause**: Instance profile not attached or IAM role missing permissions.

**Fix**:
```bash
# Verify instance profile
aws ec2 describe-instances --instance-ids <instance-id> | grep IamInstanceProfile

# Test role assumption from Cloud9
aws sts assume-role \
  --role-arn arn:aws:iam::123456789012:role/myapp-production-eks-viewer \
  --role-session-name test

# Reconfigure kubectl
aws eks update-kubeconfig \
  --region us-east-1 \
  --name TF-argo-prd \
  --role-arn arn:aws:iam::123456789012:role/myapp-production-eks-viewer
```

---

## Maintenance

### Adding New Team Members

**Via AD Group**

If the SSO role is configured to use an AD group (e.g., `CN-MyApp-Developers`):
1. Submit a ServiceNow ticket to **IAM team** to add user to the AD group
2. No AWS or GitOps changes needed—access is automatic once AD sync completes
3. User can immediately access [AWS SSO start screen](https://medtronicsso.awsapps.com/start/) and see the role

!!! Best Practice
Configure SSO roles to use AD groups rather than individual users for easier user management and faster onboarding.
!!!

### Updating IAM Role Permissions (AWS SSO Role)

**Time to Complete: ~2-3 days** (requires CHG records)

When application teams need additional AWS permissions beyond EKS/CloudWatch access (e.g., S3, RDS, SQS), the IAM policy attached to the SSO role must be updated by the **Cloud-Global team** via ServiceNow.

#### When to Update IAM Permissions

Common scenarios requiring IAM policy updates:
- **S3 bucket access**: Read/write to application-specific S3 buckets
- **RDS database access**: Connect to RDS instances
- **Secrets Manager**: View/edit/manage application secrets (including SOPS private keys)
- **SQS/SNS**: Access to message queues or topics
- **Additional CloudWatch permissions**: Metrics, dashboards

#### Option 1: Update via ServiceNow (SSO Roles)

For SSO-created roles, updates must be requested via Cloud-Global:

**Step 1: Create IAM policy with desired permissions**

In AWS console, create a **Customer Managed Policy** with the required permissions.

**Example 1: S3 bucket access with KMS encryption**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::myapp-prod-bucket",
        "arn:aws:s3:::myapp-prod-bucket/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "kms:Decrypt",
        "kms:Encrypt",
        "kms:GenerateDataKey"
      ],
      "Resource": "arn:aws:kms:us-east-1:872019488961:key/abc-123-xyz"
    }
  ]
}
```

**Example 2: AWS Secrets Manager access for team secrets**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "SecretsManagerTeamAccess",
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue",
        "secretsmanager:DescribeSecret",
        "secretsmanager:PutSecretValue",
        "secretsmanager:UpdateSecret",
        "secretsmanager:ListSecretVersionIds"
      ],
      "Resource": [
        "arn:aws:secretsmanager:us-east-1:872019488961:secret:compass-ci/myapp/*",
        "arn:aws:secretsmanager:us-east-2:872019488961:secret:compass-ci/myapp/*"
      ]
    },
    {
      "Sid": "SecretsManagerListSecrets",
      "Effect": "Allow",
      "Action": [
        "secretsmanager:ListSecrets"
      ],
      "Resource": "*"
    },
    {
      "Sid": "KMSDecryptForSecrets",
      "Effect": "Allow",
      "Action": [
        "kms:Decrypt"
      ],
      "Resource": [
        "arn:aws:kms:us-east-1:872019488961:key/*",
        "arn:aws:kms:us-east-2:872019488961:key/*"
      ]
    }
  ]
}
```

!!! Secrets Manager Naming Convention
All team secrets follow the pattern `compass-ci/<app-or-team-name>/<environment>/<secret-name>`. This scopes permissions per team and environment:
- `compass-ci/myapp/dev/*` - Development secrets
- `compass-ci/myapp/staging/*` - Staging secrets
- `compass-ci/myapp/production/*` - Production secrets

This includes SOPS age private keys (e.g., `compass-ci/myapp/dev/sops-age-key`) and application-specific secrets synced via External Secrets Operator. Team members can view, edit, encrypt, and decrypt any secrets within their team's prefix.
!!!

**Step 2: Submit ServiceNow request**

[Submit a ServiceNow ticket](https://medtronicprod.service-now.com/now/nav/ui/classic/params/target/incident.do) assigned to `Cloud-Global`:

```text
Assignment Group: Cloud-Global
Configuration Item: AWS
Category: Account / Security
Subcategory: license_access

Short description: Update IAM permissions for AWS SSO role AWSREF_MyApp-ViewerRole

Description:
Please update the inline policy for the SSO role "AWSReservedSSO_AWSREF_MyApp-ViewerRole_<id>" in it-argo-prod-mdt (872019488961) account.

The role currently has EKS and CloudWatch permissions. We need to add S3 and KMS permissions for the application to access its S3 bucket.

**Preferred Option**: Please attach the existing IAM policy "myapp-s3-access-policy" to this SSO role.

**Alternative Option (if attaching policies is not supported)**: Please copy the permissions from the "myapp-s3-access-policy" policy into the inline policy for this role.

The policy ARN is: arn:aws:iam::872019488961:policy/myapp-s3-access-policy

Or, the full policy JSON is below:
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::myapp-prod-bucket",
        "arn:aws:s3:::myapp-prod-bucket/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "kms:Decrypt",
        "kms:Encrypt",
        "kms:GenerateDataKey"
      ],
      "Resource": "arn:aws:kms:us-east-1:872019488961:key/abc-123-xyz"
    }
  ]
}

Please keep the existing EKS and CloudWatch permissions intact and add these new S3/KMS permissions.
```

**Step 3: Wait for fulfillment**

- Typical turnaround: 2-3 days
- Cloud-Global will either:
  - **Attach the policy** to the SSO role (if supported), OR
  - **Copy the permissions** into the inline policy on the SSO role
- They usually update the inline policy rather than attaching managed policies

**Step 4: Verify permissions**

Once fulfilled, test the new permissions:

```bash
# Assume the role via AWS SSO
aws sso login --profile <profile-name>

# Test S3 access
aws s3 ls s3://myapp-prod-bucket/ --profile <profile-name>

# Verify using AWS CLI
aws iam list-attached-role-policies --role-name AWSReservedSSO_AWSREF_MyApp-ViewerRole_<id>
aws iam list-role-policies --role-name AWSReservedSSO_AWSREF_MyApp-ViewerRole_<id>
```

!!! Policy Attachment Limitation
Cloud-Global typically **cannot attach managed policies** to SSO roles. They will copy the policy permissions into the inline policy attached to the SSO role instead. Always provide both options in your ServiceNow request.
!!!

#### Option 2: Update via Terraform (For Terraform-Managed Policies)

If your Terraform configuration is managing policies attached to the SSO role (as shown in Step 1), simply update the Terraform code:

```
# Add new policy attachment
resource "aws_iam_role_policy" "myapp_s3_access" {
  name = "myapp-s3-access"
  role = data.aws_iam_role.myapp_eks_viewer_sso_role.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:ListBucket"
        ]
        Resource = [
          "arn:aws:s3:::myapp-prod-bucket",
          "arn:aws:s3:::myapp-prod-bucket/*"
        ]
      }
    ]
  })
}
```

Then apply:
```bash
terraform plan
terraform apply
```

!!!warning Terraform Limitations with SSO Roles
- Terraform **can** attach/detach inline policies to SSO roles
- Terraform **cannot** create or delete the SSO role itself
- If Cloud-Global manages the inline policy directly, Terraform may conflict with their changes
- Use either Terraform OR ServiceNow requests, not both, for the same policy
!!!

#### Best Practice: Policy Management Strategy

**For simple, static permissions:**
- Use Terraform to manage inline policies (Step 1 approach)
- Faster updates, no ServiceNow dependency

**For complex or frequently changing permissions:**
- Create customer-managed policies in AWS
- Request Cloud-Global to attach or copy to SSO role
- Easier to audit and version control

**For application-specific resources (S3, RDS):**
- Manage via Terraform in your application's infrastructure repository
- Update policies whenever resources change

### Updating Kubernetes RBAC Permissions

**To add new Kubernetes resource types** (e.g., allow viewing ExternalSecrets):

1. Edit `flux-gitops/clusters/TF-argo-prd/apps/myapp-production/rbac.yaml`:
   ```yaml
   - apiGroups: ["external-secrets.io"]
     resources: ["externalsecrets"]
     verbs: ["get", "list", "watch"]
   ```

2. Commit and push:
   ```bash
   git add flux-gitops/clusters/TF-argo-prd/apps/myapp-production/rbac.yaml
   git commit -m "feat: allow viewing ExternalSecrets for myapp"
   git push origin main
   ```

3. Flux automatically applies within ~1 minute

### Revoking Access

**Remove all access:**

1. Remove IAM role mapping from aws-auth:
   ```bash
   # Edit flux-gitops/base/aws-auth/aws-auth-configmap.yaml
   # Remove the mapRoles entry for the application
   git commit -m "revoke: remove myapp EKS access"
   git push origin main
   ```

2. Delete RoleBinding:
   ```bash
   # Remove from flux-gitops/clusters/TF-argo-prd/apps/myapp-production/rbac.yaml
   git commit -m "revoke: remove myapp RoleBinding"
   git push origin main
   ```

3. Destroy Terraform resources:
   ```bash
   cd terraform/eks-viewer-roles
   terraform destroy -target=module.myapp_eks_viewer_role

   # If Cloud9 exists
   cd terraform/cloud9
   terraform destroy -target=module.myapp_cloud9
   ```

---

## Alternative: Grafana-Only Access (Recommended Default)

For most teams, **Grafana provides sufficient observability** without granting AWS/kubectl access:

**Grafana provides:**
- Pod status and metrics
- Application logs via Loki
- Resource usage dashboards
- Event logs
- No AWS account needed

**When to use kubectl access:**
- Team has strong Kubernetes expertise
- Frequent need for detailed troubleshooting
- Low-latency debugging requirements
- Team requires access to CronJob history or specific resource details

**Recommendation:** Start with Grafana-only, grant kubectl access on request after evaluating team maturity.

---

## Next Steps

- [Monitoring & Observability](../monitoring/index.md) - Primary method for troubleshooting
- [Troubleshooting Guide](../troubleshooting-and-support/troubleshooting-guide.md) - Common issues and platform support
- [Security Best Practices](../security/index.md) - Additional security considerations
