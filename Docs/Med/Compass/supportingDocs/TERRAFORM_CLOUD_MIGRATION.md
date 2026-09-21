# Terraform Cloud Migration Guide

**Project:** aws-it-cicdcds-dev-mdt

**Old GitLab:** https://code.medtronic.com/bcp_web/devops/infrastructure/terraform-deployments/aws-it-cicdcds-dev-mdt

**New GitLab:** https://medtronic.gitlab-dedicated.com/bcp_web/devops/infrastructure/terraform-deployments/aws-it-cicdcds-dev-mdt

---

## Recommended Workflow Order

This guide is organized to follow a safe, tested migration workflow:

1. **Phase 1** — Pre-Migration: Verify current setup and create AWS OIDC role
2. **Phase 2** — Sync GitLab repo (git mirroring only; code updates come later)
3. **Phase 3** — Create HCP Terraform workspace and configure variables/credentials
4. **Phase 4** — Update code references to new GitLab hostname and commit/push
5. **Phase 5** — Run `terraform init -migrate-state` to transfer state
6. **Phase 6-9** — Verify, test, and finalize

**Key point:** Do NOT run `terraform init -migrate-state` until after Phase 4 code changes are committed. This ensures your local terraform configuration matches the committed code in the new repo.

---

## Table of Contents

1. [Pre-Migration](#phase-1-pre-migration)
2. [Sync GitLab Repository](#phase-2-sync-gitlab-repository)
3. [Terraform Cloud Setup](#phase-3-terraform-cloud-setup)
4. [Update GitLab References in Terraform Code](#phase-4-update-gitlab-references-in-terraform-code)
5. [Migrate Terraform State](#phase-5-migrate-terraform-state)
6. [Verify GitLab Provider Connectivity](#phase-6-verify-gitlab-provider-connectivity)
7. [Connect VCS to Terraform Cloud Workspace](#phase-7-connect-vcs-to-terraform-cloud-workspace)
8. [Test Migration](#phase-8-test-migration)
9. [Cleanup](#phase-9-cleanup--documentation)
10. [Verification](#phase-10-verification-checklist)
11. [Rollback Plan](#phase-11-rollback-plan-if-something-breaks)

---

## Phase 1: Pre-Migration

### 1.1 Verify current setup

Pull down the `code.medtronic.com` repo to your local machine.

```bash
cd /path/to/aws-it-cicdcds-dev-mdt

# Check what backend you're currently using
cat providers.tf | grep -A 10 "backend"
# Should show: backend "http"

# Confirm git remote points to old code.medtronic.com GitLab
git remote -v
```

### 1.2 Document current state location

Your old backend is in the old code.medtronic.com GitLab instance.

Example: `https://code.medtronic.com/bcp_web/devops/infrastructure/terraform-deployments/aws-it-cicdcds-dev-mdt/-/terraform`

Download the state json as a backup (Actions > Download JSON).

### 1.3 Confirm you have Terraform Cloud credentials

- Have your Terraform Cloud organization name ready (`mdt-it-prod`)
- [Log in](https://app.terraform.io/sso/sign-in)
  - Use the "Sign in with SSO" option
  - Enter `mdt-it-prod` as the Organization Name
- On first login, Terraform Cloud may prompt you to set an account password. You can choose any password that meets policy requirements.
- Generate an API token in Terraform Cloud (Settings → API Tokens → Team Tokens): https://app.terraform.io/app/mdt-it-prod/settings/authentication-tokens
- Store it safely (you'll use it in Phase 4)

### 1.4 Create OIDC IAM role for Terraform Cloud

This role must exist in AWS **before** migrating state so Terraform Cloud can authenticate immediately on cutover. Create and apply it using your current local/CI pipeline before proceeding to Phase 3.

Create the file `iam_hcp_terraform_deployment_role.tf`:

**Note: Replace `<aws-account-id>` placeholders in this file with the AWS account ID for the account that this project is for. `<suffix>` can be any value, but should describe what or who this role is used for (e.g. `eks`, `argo`, `cds`, etc.)

```hcl
resource "aws_iam_role" "custom_terraform_deployment_role_hcp" {
  name = "Custom-TerraformDeploymentRole-HCPTerraform-mdt-it-prod-<suffix>"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Federated = "arn:aws:iam::<aws-account-id>:oidc-provider/app.terraform.io"
        }
        Action = "sts:AssumeRoleWithWebIdentity"
        Condition = {
          StringEquals = {
            "app.terraform.io:aud" = "aws.workload.identity"
          }
          StringLike = {
            "app.terraform.io:sub" = "organization:mdt-it-prod:project:it-argo-default:workspace:aws-<aws-account-id>-*:run_phase:*"
          }
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "custom_terraform_deployment_role_admin" {
  role       = aws_iam_role.custom_terraform_deployment_role_hcp.name
  policy_arn = "arn:aws:iam::aws:policy/AdministratorAccess"
}
```

Notes:
- This OIDC trust policy does not use `sts:ExternalId`.
- If your Terraform Cloud project/workspace naming differs, update only the `app.terraform.io:sub` pattern.
- The resulting role ARN (`arn:aws:iam::<aws-account-id>:role/Custom-TerraformDeploymentRole-HCPTerraform-mdt-it-prod-<suffix>`) is set as `TFC_AWS_RUN_ROLE_ARN` in Phase 3.2.

---

## Phase 2: Sync GitLab Repository (Old → New Instance)

### 2.1 Add both remote references

```bash
cd /path/to/aws-it-cicdcds-dev-mdt

# Rename current remote to track old instance
git remote rename origin old-gitlab

# Verify old remote is set
git remote -v
# Should show:
# old-gitlab    https://code.medtronic.com/bcp_web/devops/infrastructure/terraform-deployments/aws-it-cicdcds-dev-mdt.git (fetch)
# old-gitlab    https://code.medtronic.com/bcp_web/devops/infrastructure/terraform-deployments/aws-it-cicdcds-dev-mdt.git (push)

# Add new GitLab Dedicated instance
git remote add new-gitlab https://medtronic.gitlab-dedicated.com/bcp_web/devops/infrastructure/terraform-deployments/aws-it-cicdcds-dev-mdt.git
```

### 2.2 Push all branches and tags to new GitLab

**Option A: Mirror push (simplest — use this if the local repo is a clean clone)**

```bash
git push new-gitlab --mirror
```

`--mirror` pushes all refs (branches, tags, and notes) in one command. It also sets the destination to exactly match the source, meaning it will **delete** any refs on the target that don't exist locally.

> **Warning:** Only use `--mirror` on a freshly cloned repo. If your local repo has extra remote-tracking refs (e.g. `refs/remotes/old-gitlab/...`), those will be pushed to the new remote as remote-tracking refs instead of regular branches, which can confuse GitLab. Use Option B in that case.

**Option B: Branch-by-branch push (safer for existing local repos)**

```bash
# Push all branches explicitly, mapping remote-tracking refs to proper branch heads
for branch in $(git branch -r | grep 'old-gitlab/' | grep -v 'HEAD' | sed 's|old-gitlab/||' | tr -d ' '); do
  git push new-gitlab "refs/remotes/old-gitlab/$branch:refs/heads/$branch"
done

# Push all tags
git push new-gitlab --tags

# Verify all branches exist on new instance
git branch -r | grep new-gitlab
# Should list same branches as old-gitlab
```

> **Note:** If `--mirror` was already run and some branches are missing, the Option B loop will push only what's missing. It's safe to re-run.

### 2.3 Verify sync completed

```bash
# List branches on new remote
git ls-remote --heads new-gitlab
git ls-remote --tags new-gitlab

# Should match what's on old GitLab
```

### 2.4 Switch your working remote to new GitLab

```bash
# Note: 'origin' no longer exists because we renamed it to 'old-gitlab' in step 2.1
# Use 'add' instead of 'set-url'
git remote add origin https://medtronic.gitlab-dedicated.com/bcp_web/devops/infrastructure/terraform-deployments/aws-it-cicdcds-dev-mdt.git

# Keep old-gitlab as reference (don't delete yet)
git remote -v
# Should now show three remotes:
# old-gitlab    https://code.medtronic.com/... (fetch/push)
# new-gitlab    https://medtronic.gitlab-dedicated.com/... (fetch/push)
# origin        https://medtronic.gitlab-dedicated.com/... (fetch/push)
```

---

## Phase 3: Terraform Cloud Setup

### 3.1 Create workspace in Terraform Cloud UI

Go to `https://app.terraform.io/sso/sign-in`:

1. Workspaces → New workspace
2. Project → it-argo-default
3. Choose **CLI-Driven Workflow** (not Version Control)
3. **Workspace name:** `aws-<account-id>-bcpweb-devops-<gitlab-project-name>`
4. Save

### 3.1.1 Set Execution Mode to Agent

In the workspace → **Settings** → **General**:

1. Execution Mode → **Agent**
2. Agent Pool → **shared-aws-eks**
3. Save

> This ensures runs execute on the internal EKS-based agent that can reach MDT AWS resources.

### 3.1.2 Create a Terraform Cloud Team API Token

1. In TFC: Settings → [**API tokens**](https://app.terraform.io/app/mdt-it-prod/settings/authentication-tokens)
2. Click **Create an team token**
3. Select the `it-argo-admins` team
3. Give it a description such as: `Token to trigger TF Cloud pipelines from GitLab`
4. Set the expiration to the desired time frame & click `Create`.
4. Copy and save the token value securely

> **Token Reusability Note:** This token can authenticate to any workspace in the `mdt-it-prod` organization that the `it-argo-admins` team has access to. While you *can* reuse a single token across multiple projects, **best practice is to create a separate team token for each project/workspace**. This follows the principle of least privilege: if one GitLab project's CI environment is compromised, the attacker only gains access to that specific TFC workspace, not all of them. Tokens are free in Terraform Cloud, so the overhead is minimal.

### 3.1.3 Set TF_TOKEN_app_terraform_io in GitLab CI/CD

In new GitLab project → **Settings** → **CI/CD** → **Variables**:

| Variable | Value | Masked | Protected |
|---|---|---|---|
| `TF_TOKEN_app_terraform_io` | (TFC API token from step 3.1.2) | Yes | Yes |

> This allows GitLab CI Terraform jobs to authenticate to Terraform Cloud when running `terraform init`, `plan`, and `apply`.

### 3.1.4 Configure Auto-apply Settings

In the workspace → **Settings** → **Execution Mode**, you will see two Auto-apply options. Configure them as follows:

| Setting | Initial Value | Later |
|---|---|---|
| **Auto-apply API, UI, & VCS runs** | **OFF** | Turn ON after migration is verified (1-2 weeks) |
| **Auto-apply run triggers** | **OFF** | Leave OFF (not applicable — you have no run triggers) |

**Why OFF initially:**
- Lets you manually review and approve every plan during migration
- Prevents accidental applies if state or variables are misconfigured
- Gives you a safety net while validating the new setup

**When to turn on "Auto-apply API, UI, & VCS runs":**
- After `terraform plan` has run successfully at least 2-3 times with no unexpected changes
- After you've confirmed state drift is zero
- Appropriate for this dev/non-critical account once stable

> **Note:** You can change these settings at any time in Workspace → Settings → Version Control.

### 3.2 Configure workspace variables

In the TFC workspace, go to **Variables** tab and add these:

| Key | Value | Category | Sensitive | Description |
|---|---|---|---|---|
| `TFC_AWS_PROVIDER_AUTH` | `true` | Environment | No | Enables Terraform Cloud dynamic AWS credentials via OIDC federation. |
| `TFC_AWS_RUN_ROLE_ARN` | `arn:aws:iam::563893293061:role/Custom-TerraformDeploymentRole-HCPTerraform-mdt-it-prod-cicdcds` | Environment | No | Role ARN Terraform Cloud should assume for plan/apply runs. This should match the value created in Step 1.4 |
| `gitlab_api_url` | `https://medtronic.gitlab-dedicated.com` | Terraform | No | GitLab API base URL for the new dedicated instance (updated in Phase 2.5). |
| `gitlab_project_id` | (new project ID from medtronic.gitlab-dedicated.com) | Terraform | No | Project ID on the new GitLab instance — found in project Settings → General → Project ID. |
| `gitlab_api_token` | (your token) | Terraform | Yes | Used by the GitLab Terraform provider to update GitLab resources. |
| `argo_account_id` | (your ID) | Terraform | No | The Argo AWS Account ID. Used for cross-account resource references. |

> **Tip:** `owner_short_name` and `owner_long_name` are already known from `terraform.tfvars`.

**DO NOT ADD:**
- `TF_VAR_BACKEND_username`
- `TF_VAR_BACKEND_password`
- `TF_VAR_role_arn`
- `TF_VAR_external_id`

### 3.3 Remove old CI/CD variables from GitLab

Go to new GitLab project → Settings → CI/CD → Variables

Delete these (no longer needed):
- `TF_VAR_BACKEND_username`
- `TF_VAR_BACKEND_password`
- `TF_VAR_role_arn`
- `TF_VAR_external_id`

### 3.3.1 Configure GitLab credentials for private internal modules

This repository uses a private internal module over HTTPS, for example in `cross_account_s3.tf`:

```hcl
source = "git::https://medtronic.gitlab-dedicated.com/bcp_web/cloud-database-solutions/infrastructure/terraform-modules/mdt-cds-s3-bucket-module.git?ref=v1.1.4"
```

Terraform Cloud agents must have Git credentials available at `terraform init` time or module download will fail.

Recommended approach: create a **GitLab Deploy Token** with **read-only** access on the module repository or module parent group.

Example GitLab location for this module group:

- `https://medtronic.gitlab-dedicated.com/groups/bcp_web/cloud-database-solutions/infrastructure/terraform-modules/-/settings/repository#js-deploy-tokens`

Minimum permission needed:

- `read_repository`

After creating the deploy token, add these **Workspace Environment Variables** in HCP Terraform:

| Key | Value | Category | Sensitive | Description |
|---|---|---|---|---|
| `GIT_CONFIG_COUNT` | `1` | Environment | No | Enables a single runtime Git config override. |
| `GIT_CONFIG_KEY_0` | `url.https://<DEPLOY_USERNAME>:<DEPLOY_TOKEN>@medtronic.gitlab-dedicated.com/.insteadof` | Environment | Yes | Rewrites unauthenticated GitLab HTTPS URLs to include deploy token credentials. |
| `GIT_CONFIG_VALUE_0` | `https://medtronic.gitlab-dedicated.com/` | Environment | No | Matches all GitLab HTTPS clone URLs for rewrite. |
| `GIT_TERMINAL_PROMPT` | `0` | Environment | No | Prevents interactive Git username/password prompts during runs. |

Notes:
- Use the **actual deploy token username** generated by GitLab, not `oauth2`.
- Mark `GIT_CONFIG_KEY_0` as **Sensitive** because it contains the deploy token secret.
- If the username or token contains special characters, URL-encode them before saving the variable.
- This configuration is only needed for private Git-based module sources. It is separate from AWS provider authentication.
- For guidance on publishing and consuming internal Terraform modules, see the [internal module publishing documentation](https://medtronic.gitlab-dedicated.com/it-sharedservices/documentation/-/blob/change-name/docs/internal/hcp/module-publishing.md?ref_type=heads).

Troubleshooting:
- If `terraform init` fails with `fatal: could not read Username for 'https://medtronic.gitlab-dedicated.com': No such device or address`, the Terraform Cloud agent can reach GitLab but Git has no non-interactive credentials for the private module clone.
- Re-check that the deploy token has `read_repository`, the HCP Terraform workspace variables are set exactly as above, and `GIT_CONFIG_KEY_0` is marked sensitive.

> The OIDC deployment role was created as a pre-migration step in **Phase 1.5**. Confirm it is deployed before continuing.

---

## Phase 4: Update GitLab References in Terraform Code

Now that the repository lives on the new instance and TFC workspace is configured with credentials, update any Terraform source files that still reference the old GitLab hostname or the old project ID.

**Find all references to the old hostname:**

```bash
cd /path/to/aws-it-cicdcds-dev-mdt

# Search for old hostname in all .tf files
grep -rn "code.medtronic.com" --include="*.tf" .
```

Common places these appear:
- GitLab provider `base_url` or `gitlab_api_url` variable defaults
- Module `source` URLs pointing to the old GitLab instance
- Any hardcoded `gitlab_project_id` values in `.tf` or `.tfvars` files
- `gitlab_pipeline_schedule`, `gitlab_project`, or other GitLab provider resources

**Replace the hostname:**

```bash
# Replace all occurrences in .tf files (review the diff before committing)
grep -rln "code.medtronic.com" --include="*.tf" . | xargs sed -i 's|code.medtronic.com|medtronic.gitlab-dedicated.com|g'

# Verify no old references remain
grep -rn "code.medtronic.com" --include="*.tf" .
# Should return no results
```

**Update the GitLab project ID:**

If `gitlab_project_id` is hardcoded anywhere in `.tf` or `.tfvars` files, update it to the new project ID from `medtronic.gitlab-dedicated.com`.

To find the new project ID:

```bash
curl -H "PRIVATE-TOKEN: <new-gitlab-token>" \
  "https://medtronic.gitlab-dedicated.com/api/v4/projects?search=aws-it-cicdcds-dev-mdt"
# Look for the "id" field in the response
```

**Commit the changes:**

```bash
git add -A
git commit -m "chore: update gitlab hostname refs to medtronic.gitlab-dedicated.com"
git push origin main
```

---

## Phase 5: Migrate Terraform State

> The confirmed migration command is `terraform init -migrate-state`. This automatically copies state from the old GitLab HTTP backend into Terraform Cloud in one step. Manual `state push` is a fallback only if `migrate-state` cannot reach old GitLab.

### 4.1 Authenticate to Terraform Cloud locally


**terraform login (persistent on this machine)**

```bash
terraform login
```

Enter `yes` when prompted if you want to proceed. Follow the browser flow to generate and paste the token back into the terminal.

> Note: if this is your first time using Terraform Cloud, you may be prompted to set a password during the login flow. You may also be asked for that password occasionally in future `terraform login` flows.
> This persists across terminal sessions.

### 4.2 Update `providers.tf` for Terraform Cloud

**Backup first:**
```bash
cd /path/to/aws-it-cicdcds-dev-mdt
cp providers.tf providers.tf.backup
```

**Edit `providers.tf`** and replace the backend block:

**REMOVE THIS:**
```hcl
terraform {
  backend "http" {
    lock_method    = "POST"
    unlock_method  = "DELETE"
    retry_wait_min = 5
  }
  required_providers {
    # ...keep this part...
  }
}
```

**ADD THIS:**
```hcl
terraform {
  cloud {
    organization = "mdt-it-prod"

    workspaces {
      name = "aws-563893293061-bcpweb-devops-aws-it-cicdcds-dev-mdt" # update this to match the workspace created in 3.1
    }
  }

  required_providers {
    # ...keep the rest exactly as is...
  }
}
```

### 4.2.1 Update AWS provider auth block for OIDC

When using Terraform Cloud dynamic AWS credentials (`TFC_AWS_PROVIDER_AUTH=true`), remove the legacy AWS provider `assume_role` block that depends on `role_arn` and `external_id`.

In `providers.tf`, keep this:

```hcl
provider "aws" {
  region = var.region

  default_tags {
    tags = merge(
      {
        Terraform_Managed = "true"
        Owner             = var.owner_long_name
      },
      var.ENTITY_TAGS
    )
  }
}
```

Remove this legacy section:

```hcl
dynamic "assume_role" {
  for_each = var.sandbox == true ? [] : [1]
  content {
    role_arn     = var.role_arn
    external_id  = var.external_id
    session_name = "Terraform"
  }
}
```

> Important: other resources/modules in this repository may still reference `var.role_arn` or `var.external_id`. Remove those variables only after all downstream references are migrated.

### 4.2.2 Update code that derives values from role_arn

Any code that extracts the AWS account ID or other values from `var.role_arn` should be updated to use `data.aws_caller_identity` instead. This is especially important for locals and data sources.

**Example:** If you have code like this:

```hcl
locals {
  aws_account_id = split(":", var.role_arn)[4]
}
```

Update it to:

```hcl
data "aws_caller_identity" "current" {}

locals {
  aws_account_id = data.aws_caller_identity.current.account_id
}
```

Benefits:
- Works with any authentication method (OIDC, assume_role, or direct credentials)
- Self-documenting and maintainable
- No manual variable management required
- `data.aws_caller_identity` provides `account_id`, `caller_arn`, `user_id`, and `arn`

Search your codebase for these patterns and update them before removal:

```bash
# Find uses of role_arn in terraform code
grep -rn "var.role_arn" --include="*.tf" .

# Update any that extract values (e.g., split, regex_replace)
# Replace with data.aws_caller_identity references
```

After all references are removed, delete the obsolete variable declarations from `variables.tf`:

- `variable "role_arn" { ... }`
- `variable "external_id" { ... }`

This prevents Terraform from prompting for unused required inputs during `plan`.

### 4.3 Run terraform init --migrate-state (primary migration path)

With `providers.tf` updated to the cloud block, run:

```bash
cd /path/to/aws-it-cicdcds-dev-mdt

terraform init -migrate-state
```

Terraform will:
1. Detect the old HTTP backend state
2. Prompt: **"Do you want to copy existing state to the new backend?"**
3. Answer: **yes**
4. State is automatically copied from old GitLab into the TFC workspace

#### 4.3.1 Fallback: manual state push (if migrate-state cannot reach old GitLab)

Only use this if `terraform init -migrate-state` fails to connect to the old backend:

```bash
# Step 1: Download state from old GitLab
curl --header "PRIVATE-TOKEN: <OLD_GITLAB_TOKEN>" \
  "https://code.medtronic.com/api/v4/projects/<PROJECT_ID>/terraform/state/main" \
  -o downloaded-state.tfstate

# Validate file
ls -lh downloaded-state.tfstate
head -n 5 downloaded-state.tfstate

# Step 2: Push into TFC workspace
terraform state push downloaded-state.tfstate

# If lineage/serial protection blocks it:
# terraform state push -force downloaded-state.tfstate
```

### 4.5 Move GitLab CI/CD variables to Terraform Cloud workspace variables

Do this **after** `terraform init` succeeds. In TFC workspace → **Variables** tab:

See the full variable mapping table in Phase 3.2.

### 4.6 Verify state migrated successfully

```bash
# List all resources (proves state is accessible)
terraform state list
# Should show all your existing AWS resources

# Spot-check one resource
terraform state show 'aws_s3_bucket.example'
# Should show your resource details

# Check in TFC UI
# Go to: app.terraform.io → Your Org → Workspace → States
# Should see your state there with a recent timestamp
```

---

## Phase 6: Verify GitLab Provider Connectivity

Terraform code hostname references were updated in **Phase 4** and workspace variables (`gitlab_api_url`, `gitlab_project_id`, `gitlab_api_token`) were set in **Phase 3.2**. At this point, verify the GitLab provider can authenticate successfully.

### 6.1 Run a plan to confirm GitLab provider auth

```bash
terraform plan
# Should succeed without GitLab authentication errors
```

If the plan fails with a GitLab authentication error, check:
- `gitlab_api_url` is set to `https://medtronic.gitlab-dedicated.com` (not the old instance)
- `gitlab_api_token` is a valid token for the new dedicated instance
- `gitlab_project_id` matches the project ID on `medtronic.gitlab-dedicated.com`

---

## Phase 7: Connect VCS to Terraform Cloud Workspace

After state is migrated and verified, connect the workspace to GitLab so TFC automatically plans on every push.

### 7.1 Grant HCP Terraform service account access in GitLab

In new GitLab project → **Settings** → **Members**:

1. Add member: `svc-hcp-terraform-mdt-it-prod`
2. Role: **Maintainer**
3. Save

> This account exists in GitLab Dedicated (`medtronic.gitlab-dedicated.com`) but **not** in the old `code.medtronic.com` instance. This is why the workspace must be connected to the new dedicated instance.
>
> Maintainer access is required for TFC to create the webhook that triggers automatic plans on push.

### 7.2 Connect workspace to GitLab VCS

In TFC workspace → **Settings** → **Version Control**:

1. Click **Connect to a version control provider**
2. Select your GitLab Dedicated OAuth connection
3. Repository: `bcp_web/devops/infrastructure/terraform-deployments/aws-it-cicdcds-dev-mdt`
4. VCS branch: (default branch)
5. Save

> After this step, every push to `main` will trigger a TFC plan automatically.

### 7.3 Remove `.gitlab-ci.yml` from the project

Once TFC is running plans via VCS webhook, the GitLab CI pipeline is no longer needed for Terraform.

```bash
cd /path/to/aws-it-cicdcds-dev-mdt

git rm .gitlab-ci.yml
git commit -m "chore: remove gitlab-ci.yml - terraform now managed by TFC"
git push origin main
```

> Keep any compliance/security jobs if they still run separately. If the only jobs were Terraform-related, remove the file entirely.

### 7.4 Remove scheduled pipelines in GitLab

In new GitLab project → **CI/CD** → **Schedules**:

1. Delete any Terraform-related scheduled pipelines
2. Remove any Terraform code that creates or manages GitLab drift-detection schedules (for example `gitlab_pipeline_schedule` resources, related schedule variables, and schedule-only CI jobs).

> Drift detection is now handled inside Terraform Cloud (workspace → **Settings** → **Health**), not GitLab scheduled jobs.

### 7.4.1 Remove legacy GitLab schedule resources from Terraform state

If drift schedule resources were previously created and are still present in state, remove them after deleting/commenting the Terraform code. Otherwise Terraform may still try to initialize the GitLab provider during plans.

Run from the repository root (with your HCP Terraform workspace already configured in `providers.tf`):

```bash
terraform state rm gitlab_pipeline_schedule.drift-protection \
  gitlab_pipeline_schedule_variable.drift-protect-auto-apply-variable
```

Then verify they are gone:

```bash
terraform state list | grep gitlab
# Should return no results
```

### 7.5 Enable Workspace Health Assessments

In the workspace → **Settings** → **Health**:

1. Click the **Enable health checks** button (if not already enabled)
2. This activates HCP Terraform's Health Assessments feature, which continuously monitors your infrastructure state
3. Health Assessments will run alongside planned runs and report:
   - Drift between infrastructure and Terraform state
   - Potential issues with resources
   - Coverage metrics for your configuration

> Health Assessments provide automated compliance and state quality insights without requiring custom integrations.

---

## Phase 8: Test Migration

### 8.1 Plan in Terraform Cloud (via UI)

Go to TFC workspace → Click **Queue Plan**:

```bash
# OR push a commit to trigger pipeline
git add -A
git commit -m "chore: migrate to Terraform Cloud"
git push origin main
```

### 8.2 Verify no state drift

```bash
cd /path/to/aws-it-cicdcds-dev-mdt

terraform refresh
terraform state list
# Should match what was in old backend

terraform state show 'aws_s3_bucket.example' | head -20
# Spot-check a few resources
```

### 8.3 Test GitLab CI runs against TFC

```bash
# Commit a small change and push to trigger pipeline
git add -A
git commit -m "chore: test TFC integration"
git push origin main

# Watch pipeline in new GitLab instance
# terraform plan should run and succeed in TFC
```

### 8.4 Module refresh troubleshooting (when needed)

Use a module refresh when any of the following happens:

- `terraform plan` fails with many errors inside `.terraform/modules/...` (undeclared resource/module errors from downloaded module code)
- A module source ref (tag/branch) was changed upstream and local/TFC runs now disagree
- You changed module `source` refs and Terraform is still using stale downloaded module content

Local module refresh commands:

```bash
cd /path/to/aws-it-cicdcds-dev-mdt

# Re-download modules/providers to match current source refs
terraform init -upgrade

# Optional: validate after refresh
terraform validate
terraform plan
```

If local refresh works but Terraform Cloud still fails:

1. Queue a **new** run in TFC after pushing the latest commit.
2. Confirm workspace Terraform version matches what works locally.
3. Prefer pinning Git module sources to immutable commit SHAs (instead of movable tags) for deterministic behavior across environments.

Example deterministic module source pattern:

```hcl
source = "git::https://medtronic.gitlab-dedicated.com/.../module.git?ref=<commit-sha>"
```

Notes:

- Avoid editing files under `.terraform/modules/` directly; those are generated cache artifacts.
- `terraform init -upgrade` is typically enough for refresh. Use cache deletion only as a last resort.

### 8.4.1 Module mirroring options for restricted runners

Provider mirrors in `.terraformrc` do not mirror Git-based module sources. For modules that still resolve from GitHub, use one of these patterns:

1. Mirror upstream module repositories into GitLab Dedicated and update `source` URLs to internal GitLab.
2. Configure Git URL rewrite in CI so `https://github.com/` is rewritten to your internal mirror base URL.
3. Fork the dependent module and change its nested module `source` URLs to internal locations.

In this project, `.gitlab-ci.yml` supports option 2 via `GITHUB_MODULE_MIRROR_BASE`. Set it to your internal mirror base URL to rewrite module downloads during `terraform init`.

### 8.4.2 Temporary native resource bridge for private modules

If a shared internal module still depends on legacy `role_arn` / `external_id` provider behavior, a temporary workaround is to replace that module call with first-party `aws_*` resources in the root module.

In this repository, that temporary bridge currently applies to `cross_account_s3.tf` for the Argo S3 bucket and KMS key.

Guidelines:

- Preserve the existing resource names, bucket name, and KMS alias so Terraform manages the same underlying AWS objects.
- Use `moved` blocks so state is transferred from the old module addresses to the new root resource addresses without recreation.
- Keep the same security defaults that the shared module enforced, including SSE-KMS, deny-insecure-transport bucket policy, public access block, and intelligent tiering.
- Treat this as a temporary compatibility layer, not a new long-term pattern.


---

## Phase 8: Cleanup & Documentation

### 8.1 Remove old remote (after confirming new works for ~24hrs)

```bash
git remote remove old-gitlab
```

### 8.2 Update `README.md` with new setup instructions

Add this section to `README.md`:

```markdown
## Terraform Cloud Setup

This project uses Terraform Cloud as the remote backend.

### CI/CD

- GitLab CI automatically uses variables from TFC workspace
- No backend credential variables needed in GitLab
- Push to main → TFC auto-runs plan (manual approval required)

### State Location

- **Before:** GitLab HTTP backend (old instance)
- **After:** Terraform Cloud (https://app.terraform.io/)
```

### 8.3 Delete old state from old GitLab (optional, do this later)

Wait at least 1-2 weeks, then:

```bash
# Get project ID
OLD_PROJECT_ID=$(git ls-remote old-gitlab HEAD | awk '{print $2}' | cut -d'/' -f5)

# Delete old state
curl -X DELETE \
  -H "PRIVATE-TOKEN: $OLD_TOKEN" \
  "https://code.medtronic.com/api/v4/projects/$OLD_PROJECT_ID/terraform/state/main"

# Then delete or archive the old GitLab project
```

---

## Phase 10: Verification Checklist

Work through this checklist in order, following the phases above:

### Phase 1: Pre-Migration
- [ ] Current setup verified (backend "http", old GitLab remote)
- [ ] State location documented and backed up from old instance
- [ ] TFC credentials confirmed (logged in via SSO)
- [ ] AWS IAM OIDC role created and deployed (`iam_hcp_terraform_deployment_role.tf`)

### Phase 2: Sync GitLab Repository
- [ ] Git remotes configured (old-gitlab, new-gitlab, origin)
- [ ] All branches and tags pushed to new instance
- [ ] Sync verified (branches/tags match between old and new)

### Phase 3: Terraform Cloud Setup
- [ ] TFC workspace created (CLI-driven, Agent: shared-aws-eks)
- [ ] Execution mode set to Agent (shared-aws-eks pool)
- [ ] TFC API token created
- [ ] TF_TOKEN_app_terraform_io set in GitLab CI/CD Variables (masked & protected)
- [ ] Auto-apply configured OFF (for both API/UI and run triggers)
- [ ] Workspace variables configured:
  - [ ] TFC_AWS_PROVIDER_AUTH = true
  - [ ] TFC_AWS_RUN_ROLE_ARN = (correct role ARN from Phase 1.4)
  - [ ] gitlab_api_url = https://medtronic.gitlab-dedicated.com
  - [ ] gitlab_project_id = (new project ID)
  - [ ] gitlab_api_token = (valid token for new instance)
  - [ ] argo_account_id = (correct account ID)
- [ ] Old CI/CD backend variables removed (TF_VAR_BACKEND_username, etc.)
- [ ] GitLab deploy token created for private module access
- [ ] Auth environment variables configured (GIT_CONFIG_*)

### Phase 4: Update GitLab References in Terraform Code
- [ ] Old hostname `code.medtronic.com` found and replaced with `medtronic.gitlab-dedicated.com` in all .tf files
- [ ] GitLab project ID updated to new dedicated instance ID
- [ ] Code changes committed and pushed to new-gitlab

### Phase 5: Migrate Terraform State
- [ ] Local TFC authentication completed (terraform login)
- [ ] providers.tf backed up
- [ ] providers.tf updated to cloud block (removed backend "http")
- [ ] AWS provider assume_role block removed from providers.tf
- [ ] Obsolete `role_arn` and `external_id` declarations removed from variables.tf after all references are migrated
- [ ] terraform init -migrate-state completed successfully
- [ ] State migrated to TFC workspace
- [ ] terraform state list shows all expected resources
- [ ] Old CI/CD variables migrated to TFC workspace variables

### Phase 6: Verify GitLab Provider Connectivity
- [ ] terraform plan succeeds without GitLab authentication errors

### Phase 7: Connect VCS to Terraform Cloud
- [ ] svc-hcp-terraform-mdt-it-prod added as Maintainer in GitLab project
- [ ] TFC workspace connected to GitLab VCS
- [ ] VCS webhook created (verified in GitLab Integrations)
- [ ] .gitlab-ci.yml removed from project
- [ ] Scheduled pipelines removed from GitLab
- [ ] Terraform drift-detection code removed (gitlab_pipeline_schedule resources, etc.)
- [ ] Legacy GitLab drift schedule resources removed from Terraform state (`terraform state rm ...`) if present
- [ ] Health Assessments enabled in workspace settings (Settings → Health → Enable)

### Phase 8: Test Migration
- [ ] terraform plan triggered by git push succeeds in TFC
- [ ] terraform apply approved and succeeds without errors
- [ ] No unintended state drift detected
- [ ] Module refresh working (if applicable)

### Phase 9: Cleanup & Documentation
- [ ] README.md updated with new TFC setup instructions
- [ ] Old git remote removed after 24hrs confirmation (git remote remove old-gitlab)
- [ ] Plan to delete old state from old GitLab after 1-2 weeks

---

## Phase 11: Rollback Plan (if something breaks)

If you need to rollback during this process:

### 11.1 Switch back to old backend

```bash
# Restore old providers.tf
cp providers.tf.backup providers.tf

# OR manually edit providers.tf to restore backend "http" block
```

### 11.2 Point git back to old GitLab

```bash
git remote set-url origin https://code.medtronic.com/bcp_web/devops/infrastructure/terraform-deployments/aws-it-cicdcds-dev-mdt.git
```

### 11.3 Push to old instance

```bash
git push origin main
```

### 11.4 Re-init with old backend

```bash
terraform init

# If prompted, say "no" to copy state
# Old state will remain in old GitLab
```

### 11.5 Verify

```bash
terraform state list
# Should work

terraform plan
# Should work without errors
```

### 11.6 Roll back temporary native S3/KMS resources to shared module

If the internal S3 bucket module is later refactored to stop depending on `role_arn` / `external_id`, revert the temporary native-resource bridge in `cross_account_s3.tf` by moving management back to the shared module.

Recommended process:

1. Update the shared internal module so it uses the default AWS provider context and no longer requires legacy assume-role inputs.
2. Restore the module block in `cross_account_s3.tf` with the same bucket name / alias behavior expected by the current resources.
3. Remove the temporary root-level `aws_s3_bucket`, `aws_kms_key`, related bucket policy/public access/intelligent tiering resources, and the old `moved` blocks that pointed from module addresses to root resources.
4. Add reverse `moved` blocks so Terraform transfers state from the temporary root resources back to the module resource addresses.
5. Run `terraform plan` and confirm the transition is state-only with no destroy/create for the bucket, KMS key, alias, policy, encryption config, or tiering configuration.

Important:

- Do not simply delete the temporary resources and re-add the module without state moves.
- The goal is to preserve the same AWS objects and only change which Terraform addresses manage them.
- If `moved` blocks are not practical for the final module shape, use explicit `terraform state mv` commands during a controlled maintenance window instead.

---

## Additional Resources

- [Terraform Cloud Documentation](https://www.terraform.io/cloud-docs)
- [Terraform Cloud VCS Integration](https://www.terraform.io/cloud-docs/vcs)
- [Migrating State to Terraform Cloud](https://www.terraform.io/cloud-docs/migrate)

---

## Questions and Support

- **Question: Can I change Workspace type later?** Yes, you can switch between VCS Workflow and CLI-Driven at any time in Workspace Settings.
- **Question: What if plan/apply fails?** See Phase 11 Rollback Plan for recovery steps.
- **Question: Should I delete old state?** Wait at least 1-2 weeks to ensure TFC is stable, then delete.
