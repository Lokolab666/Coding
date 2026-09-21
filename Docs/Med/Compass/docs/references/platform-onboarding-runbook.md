---
label: Platform and Support Onboarding Runbook
icon: book
order: 130
---

# Platform Team Runbook: Flux-GitOps Onboarding and Shared Access Setup

!!!warning
This page is for the platform & platform support team only. It consolidates required setup for `flux-gitops` onboarding support, platform-managed tokens, and cross-project CI access controls.
!!!

## When to use this runbook

Use this checklist when:
- Onboarding a new application team/group to Compass CI
- Enabling `flux-register-*` and `flux-verify-*` jobs for a new repo
- Troubleshooting cross-project permission failures

!!!
Watch a video-based walkthrough explaining the below steps [here](https://medtronic.sharepoint.com/:v:/r/sites/WebDevModernization/Shared%20Documents/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)/Compass%20CI/Demos%20and%20Recordings/Compass%20CI%20App%20Onboarding%20Walkthrough.mp4?csf=1&web=1&e=9JITJF).
!!!

## Platform-managed responsibilities

### 1. Shared semantic-release project access for template consumption
Completing this step ensures all pipeline executions from the client's GitLab group are able to access and use the semantic-release project's templates.

For each onboarding group/team:
- Share `bcp_web/devops/semantic-release` with the onboarding group at `Reporter` access level.
    - Navigate to: https://medtronic.gitlab-dedicated.com/bcp_web/devops/semantic-release/-/project_members
    - Click `Invite a group`
        - **Select a group to invite**: Enter the name of the GitLab group to invite (**note: if the group is not visible, ask the group owner to add you to their group with `Reporter` level permissions, that will enable you to then select the group**).
        - **Select maximum role**: `Reporter`
- Once complete, any pipeline includes of the semantic-release project should resolve for runs from that group's projects.

### 2. Trivy policies CI_JOB_TOKEN allowlist

In `trivy-policies` project, configure Job Token Permissions:
- URL: https://medtronic.gitlab-dedicated.com/bcp_web/devops/security/trivy-policies/-/settings/ci_cd#js-token-access
- Click `Add` on the **CI/CD job token allowlist**, then select `Group or project`.
- Type in the onboarding group path (for example `bcp_web/<group>`)
- Select **Fine-grained permissions**
- Grant **Read** on Repositories. Leave all others as **None**

Why this is required:
- App pipelines read policy content from `trivy-policies` using `CI_JOB_TOKEN`. The `CI_JOB_TOKEN` is included by default on all pipelines.
- Without allowlisting, security policy evaluation jobs fail with access errors.

### 3. Configure group-level CI/CD variables

For teams deploying to shared clusters, group-level CI/CD variables are now provisioned via onboarding automation rather than manual entry.

!!!
Watch a video-based walkthrough explaining the below steps [here](https://medtronic.sharepoint.com/:v:/r/sites/WebDevModernization/Shared%20Documents/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)/Compass%20CI/Demos%20and%20Recordings/Group%20Variable%20and%20Token%20Automation.mp4?csf=1&web=1&e=0cQYBc).
!!!

==- :icon-terminal: Local machine prerequisites for running `shared_vars.py` and `token_vars.py`
Before running the Python onboarding scripts locally, make sure the following are installed and available on your `PATH`:

- **Python 3**: required for both `shared_vars.py` and `token_vars.py`
    - Windows install option: `winget install Python.Python.3`
    - Alternate install: https://www.python.org/downloads/
- **AWS CLI v2**: required for `shared_vars.py` because it reads variable defaults from AWS Secrets Manager using the local `aws` CLI
    - Windows install option: `winget install Amazon.AWSCLI`
    - Alternate install: https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html

Validation checks:
- `python --version`
- `aws --version`

Notes:
- No additional `pip install` step is required for these scripts. The current implementation uses only Python standard library modules plus the locally installed AWS CLI.
- `token_vars.py` requires Python only. `shared_vars.py` requires both Python and AWS CLI.
===

!!!
**Note:** These should be set at the highest group-level to avoid duplication and take advantage of inheritance of variables for sub-groups. Teams can override these variables at a sub-group or project level as needed.
!!!

#### Automation workflow  (`shared_vars.py`)

1. Locally clone and open a terminal in the [`semantic-release`](https://medtronic.gitlab-dedicated.com/bcp_web/devops/semantic-release) repository.
2. Pull temporary AWS credentials for `it-argo-dev-mdt` from the [AWS SSO start page](https://medtronicsso.awsapps.com/start/#/)
3. Set AWS credentials in the terminal:
    ```bash
    export AWS_ACCESS_KEY_ID="<from-aws-sso>"
    export AWS_SECRET_ACCESS_KEY="<from-aws-sso>"
    export AWS_SESSION_TOKEN="<from-aws-sso>"
    ```
4. Have an owner of the onboarding group create a temporary group access token for their group:
    - Name: `compass-ci-onboarding-token`
    - Description: `Temporary token used to automate group-level variable setup`
    - Role: `Owner`
    - Scope: `api`
    - Expiration: short-lived (for example 1 week)

5. Set the group token and target path in the terminal:
    - Rules:
        - Use only the group path segment from the URL.
        - Do not include leading or trailing slash.
        - Preserve case exactly as shown in GitLab.

    ```bash
    export GITLAB_TARGET_GROUP_TOKEN="<group-owner-provided-token>"
    export TARGET_GROUP_PATH="<gitlab-group-path>" (e.g `bcp_web/common`, `PC_AI_Lab`, `CIAM`)
    ```

6. From the `semantic-release` directory on your local, run dry-run first to preview the actions:
    ```bash
    python scripts/group-vars/shared_vars.py \
        --target-group-path "$TARGET_GROUP_PATH" \
        --gitlab-token "$GITLAB_TARGET_GROUP_TOKEN" \
        --aws-auth-mode direct \
        --aws-region us-east-1 \
        --aws-secret-id compass-ci/onboarding/group-vars \
        --check-inherited-vars true \
        --strict-parent-visibility true \
        --dry-run true
    ```

7. Run again in apply mode to create the variables:
    ```bash
    python scripts/group-vars/shared_vars.py \
        --target-group-path "$TARGET_GROUP_PATH" \
        --gitlab-token "$GITLAB_TARGET_GROUP_TOKEN" \
        --aws-auth-mode direct \
        --aws-region us-east-1 \
        --aws-secret-id compass-ci/onboarding/group-vars \
        --check-inherited-vars true \
        --strict-parent-visibility true \
        --dry-run false
    ```
    ==- :icon-alert-fill: Overwrite Existing Variables
    !!!warning
    If existing CI/CD variables **must be** updated intentionally, enable overwrite on the run.
    When overwrite mode is enabled, the script will output what will be changed and prompt for confirmation before proceeding with the updates. Type `OVERWRITE` to proceed.
    !!!
    ```bash
    python scripts/group-vars/shared_vars.py \
        --target-group-path "$TARGET_GROUP_PATH" \
        --gitlab-token "$GITLAB_TARGET_GROUP_TOKEN" \
        --aws-auth-mode direct \
        --aws-region us-east-1 \
        --aws-secret-id compass-ci/onboarding/group-vars \
        --check-inherited-vars true \
        --strict-parent-visibility true \
        --allow-overwrite true \
        --dry-run false
    ```
    ===

9. Have the onboarding team confirm the variables exist under Group -> Settings -> CI/CD -> Variables.

#### Source of truth for variable defaults

- Shared variable values and metadata are stored in AWS Secrets Manager JSON payload of the [`compass-ci/onboarding/group-vars`](https://us-east-1.console.aws.amazon.com/secretsmanager/secret?name=compass-ci%2Fonboarding%2Fgroup-vars&region=us-east-1#) secret.
- To add or update defaults for future runs, update that secret JSON and rerun onboarding for target groups.

### 4. Create and provide platform-owned tokens

After group-level CI/CD variables have been set, the onboarding group requires some group-specific tokens to be provisioned in order for various job templates to successfully run in a fully-automated fashion.

!!!
Watch a video-based walkthrough explaining the below steps [here](https://medtronic.sharepoint.com/:v:/r/sites/WebDevModernization/Shared%20Documents/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)/Compass%20CI/Demos%20and%20Recordings/Group%20Variable%20and%20Token%20Automation.mp4?csf=1&web=1&e=0cQYBc).
!!!

**Behavior summary:**
- Default mode is no-overwrite for existing token variables.
- To plan or execute rotation/replacement of existing group token variables, include `--allow-overwrite true`.
- Generated GitLab token scopes are `api`, `write_repository`, `self_rotate` to support future self-rotation of the GitLab tokens.
- If an existing same-name Flux/Terraform token is missing required scopes, the automation revokes and recreates it.
- TFC token creation is duplicate-aware by org + team + description.

!!!warning
**Note:** Depending on the size and structure of the onboarding group, the tokens should be provisioned at sub-group levels. For example `BCP_Web` is a very large group with many sub-groups owned by different teams. Therefore, in this case, it would make sense to provision tokens for each sub-group (e.g. `bcp_web/common`, `bcp_web/finance`, `bcp_web/commercial`, etc.)
!!!

#### Token Script Prerequisites
The following variables/inputs are needed in order to run the `token_vars.py` automation script from the [`semantic-release`](https://medtronic.gitlab-dedicated.com/bcp_web/devops/semantic-release) project.
- `--gitlab-flux-admin-token` should be a GitLab [personal access token](https://medtronic.gitlab-dedicated.com/-/user_settings/personal_access_tokens) with `api` scope and sufficient access to the `--flux-source-project` project.
- `--gitlab-terraform-admin-token` should be a GitLab [personal access token](https://medtronic.gitlab-dedicated.com/-/user_settings/personal_access_tokens) with `api` scope and Owner access to the `--terraform-source-group` group. It can be the same as the `--gitlab-flux-admin-token`.
- `--tfc-org` must be the Terraform Cloud organization name that owns the target team token metadata (e.g. `mdt-it-prod`).
- `--tfc-team-id` must be the Terraform Cloud team ID value (for example `team-xxxxxxxx`), not the team name.
    - Navigate in Terraform Cloud to the [Teams](https://app.terraform.io/app/mdt-it-prod/settings/teams) page, click on `it-argo-admins`. The team ID will be displayed in the URL of the resulting page: https://app.terraform.io/app/mdt-it-prod/settings/teams/<team-xxxxxxxxx>
- `--tfc-admin-token` is a Terraform Cloud [Team API token](https://app.terraform.io/app/mdt-it-prod/settings/authentication-tokens) for `it-argo-admins`. This token can be created with a short expiry (e.g. 1 week or less) as it will just be used for this `token_var.py` execution and then will not be needed.

#### Automation workflow (`token_vars.py`)

Once you have the above [Prerequisite values](#token-script-prerequisites), this script will automate creation of the following required tokens and set them as CI/CD variables on the onboarding group:
| Token Name | Purpose |
| --- | --- |
|`FLUX_GITOPS_REPO_RW_TOKEN`| The `FLUX_GITOPS_REPO_RW_TOKEN` allows `flux-register-*` jobs to clone the `flux-gitops` repo, push onboarding branch updates, and open/update MRs via GitLab API.|
|`TERRAFORM_REPO_RW_TOKEN`| `TERRAFORM_REPO_RW_TOKEN` is used by Terraform/IaC automations to read/write Terraform project content for automations such as certificate provisioning, S3 buckets, etc.|
|`COMPASS_CI_TFC_API_TOKEN`| `COMPASS_CI_TFC_API_TOKEN` is used for variable upserts into Terraform Cloud. This is used by various `semantic-release` job templates such as `flux-register-*` and `provision-cert-*`. |

1. Use the same group token from Step 4 of the [previous shared_vars.py automation flow](#automation-workflow--shared_varspy) and set the target group variables:
    ```bash
    export TARGET_GROUP_PATH="<gitlab-group-path>" (e.g `bcp_web/common`, `PC_AI_Lab`, `CIAM`)
    export GITLAB_TARGET_GROUP_TOKEN="<group-owner-provided-token>"
    ```

2. Use `scripts/group-vars/token_vars.py` from the [`semantic-release`](https://medtronic.gitlab-dedicated.com/bcp_web/devops/semantic-release) repo to manage required token variables instead of manual token creation. Use the [Prerequisite values](#token-script-prerequisites) to set variables.
    ```bash
    export GITLAB_FLUX_ADMIN_TOKEN="<bcp-web-owner-personal-access-token>"
    export GITLAB_TERRAFORM_ADMIN_TOKEN="<bcp-web-owner-personal-access-token>"
    export TFC_ADMIN_TOKEN="<temporary-it-argo-admins-tfc-api-token>"
    export TFC_TEAM_ID="<tfc-it-argo-admins-team-id>" (e.g. `team-xxxxxxxxx`)
    ```

3. Execute a dry-run first to identify which tokens and CI/CD variables will be created:
    ```bash
    python scripts/group-vars/token_vars.py \
        --target-group-path "$TARGET_GROUP_PATH" \
        --gitlab-target-group-token "$GITLAB_TARGET_GROUP_TOKEN" \
        --gitlab-flux-admin-token "$GITLAB_FLUX_ADMIN_TOKEN" \
        --gitlab-terraform-admin-token "$GITLAB_TERRAFORM_ADMIN_TOKEN" \
        --flux-source-project bcp_web/devops/fluxconfigs/flux-gitops \
        --terraform-source-group bcp_web/devops/infrastructure/terraform-deployments \
        --token-valid-months 12 \
        --tfc-admin-token "$TFC_ADMIN_TOKEN" \
        --tfc-org mdt-it-prod \
        --tfc-team-id "$TFC_TEAM_ID" \
        --check-inherited-vars false \
        --strict-parent-visibility true \
        --dry-run true
    ```

4. Run again in apply mode to create the tokens and set the group's CI/CD token variables:
    ```bash
    python scripts/group-vars/token_vars.py \
        --target-group-path "$TARGET_GROUP_PATH" \
        --gitlab-target-group-token "$GITLAB_TARGET_GROUP_TOKEN" \
        --gitlab-flux-admin-token "$GITLAB_FLUX_ADMIN_TOKEN" \
        --gitlab-terraform-admin-token "$GITLAB_TERRAFORM_ADMIN_TOKEN" \
        --flux-source-project bcp_web/devops/fluxconfigs/flux-gitops \
        --terraform-source-group bcp_web/devops/infrastructure/terraform-deployments \
        --token-valid-months 12 \
        --tfc-admin-token "$TFC_ADMIN_TOKEN" \
        --tfc-org mdt-it-prod \
        --tfc-team-id "$TFC_TEAM_ID" \
        --check-inherited-vars false \
        --strict-parent-visibility true \
        --dry-run false
    ```

    ==- :icon-alert-fill: Overwrite / Rotate tokens
    !!!warning
    Overwrite mode should only be used wwhen existing token variables must be intentionally updated or rotated.
    When overwrite mode is enabled, the script will output what will be changed and prompt for confirmation before proceeding with the updates. Type `OVERWRITE` to proceed.
    !!!
    ```bash
    python scripts/group-vars/token_vars.py \
        --target-group-path "$TARGET_GROUP_PATH" \
        --gitlab-target-group-token "$GITLAB_TARGET_GROUP_TOKEN" \
        --gitlab-flux-admin-token "$GITLAB_FLUX_ADMIN_TOKEN" \
        --gitlab-terraform-admin-token "$GITLAB_TERRAFORM_ADMIN_TOKEN" \
        --flux-source-project bcp_web/devops/fluxconfigs/flux-gitops \
        --terraform-source-group bcp_web/devops/infrastructure/terraform-deployments \
        --token-valid-months 12 \
        --allow-overwrite true \
        --tfc-admin-token "$TFC_ADMIN_TOKEN" \
        --tfc-org mdt-it-prod \
        --tfc-team-id team-xxxxxxxx \
        --check-inherited-vars false \
        --strict-parent-visibility true \
        --dry-run false
    ```
    ===

## Automated onboarding flow
The app pipeline must include `semantic-release/flux-registration.yml`. The `.gitlab-ci.yml` template is availabe [here](../getting-started/gitlab-repository-configuration.md#1-gitlab-ciyml). Confirm these variables/patterns before first run.

When an app team runs a `flux-register-<env>` job for onboarding, the automation now opens **two MRs** and assigns the platform team reviewers on both:

1. MR in `flux-gitops` with the base onboarding manifests/files.
2. MR in the associated Terraform project with IAM + Secrets Manager resources.

Use the following review and execution sequence:

1. Review the **Terraform project MR first**.
2. Approve and merge the Terraform MR to `main`.
3. Open the Terraform run in the [TFC workspace](https://app.terraform.io/app/mdt-it-prod/workspaces) and confirm/apply the changes.
    - Confirm the workspace's Variables contains a new Sensitive variable for the application's SOPs key
        - Examples: `acm_it_acm_strat_alliance_snop_sops_key_value`, `bcp_web_common_newton_sops_key_value`
4. Spot-check AWS account resources:
    - Confirm the app's `sops-age-key` secret exists and has a value set.
    - Confirm the generated team EKS role exists and has access to the app `sops-age-key` secret.
5. After Terraform run completion, review/approve/merge the `flux-gitops` MR.
6. After Flux changes are applied, confirm a new app kustomization appears in the cluster (Cloud9/k9s).

Current operating model:
- Terraform apply remains a reviewed/manual confirmation step for onboarding runs.
- Automated apply can be enabled later after platform sign-off.

### Flux-gitops repository structure expectations

For each app path under a cluster:
- Folder path: `clusters/<cluster>/apps/<group-or-team>/<app>/`
- App folder must be listed in cluster apps index:
  - `clusters/<cluster>/apps/kustomization.yaml`

Generated app folder content includes (but not limited to):
- `app.properties`
    - Flat properties file with app metadata: `COST_CENTER`, `APP_NAME`, `TEAM_NAME`, etc. Used to identify ownership and enable future automated chargeback reporting.
- `source-token.yaml`
    - Provisions the GitLab deploy token into the cluster so Flux can authenticate when cloning/polling the app's GitLab repo. The deploy token itself is stored in AWS Secrets Manager (not hard-coded in this file); the app team saves the token value as a CI/CD variable (`FLUX_DEPLOY_TOKEN`) in their GitLab project, which the `flux-register-*` job then writes to AWS Secrets Manager. This file (an `ExternalSecret`) syncs it into a Kubernetes Secret at runtime.
- `webhook.yaml`
    - Flux `Receiver` resource (and its associated HMAC secret) that registers a GitLab push webhook. **One per cluster** (not per env) - a single webhook endpoint covers all environments for the app on that cluster. This triggers an immediate Flux reconciliation on push rather than waiting for the polling interval.

#### File naming standard

Environment-specific files use **env-prefix** naming. These should all be auto-generated by the `flux-register-*` job, but listed here for examples.
- `dev-namespace.yaml`
- `staging-secretstore.yaml`
- `production-source.yaml`

### Next Steps
After `flux-gitops` and Terraform project MRs have been merged to main and TFC apply has been done:
1. Re-run the matching `flux-verify-<env>` job to confirm expected onboarding resources are present for that environment. The app team can run it directly, or platform/support can run it when we have access to the app project.
    - This job validates manifest wiring in `flux-gitops` and checks the key cluster resources for that env (Namespace, GitRepository, Kustomization, SecretStore, ExternalSecret, and Receiver status).
    - `ExternalSecret` and `Receiver` not-ready states are currently surfaced as warnings in the job output (not hard failures), so review job logs for those warnings before closing onboarding.
    - If additional troubleshooting is needed, use AWS Cloud9/k9s to spot-check:
        - GitRepository exists and is reconciling against the app team's repo
        - Kustomization exists (may or may not reconcile successfully depending on app repo `k8s/*` content)
        - SecretStore exists and is `Valid`
            - If not, this usually indicates that the generated `ServiceAccount` is missing the role annotation. Use `describe` on the SecretStore for detailed reason.
        - ExternalSecret for the SOPS key exists and is `SecretSynced`
            - If `SecretSyncedError`, confirm SecretStore validity and IAM/KMS permissions for secret read + decrypt. Use `describe` on the ExternalSecret for detailed reason.
        - Receiver exists for [GitLab Webhook configuration](../getting-started/gitlab-repository-configuration.md#webhooks-optional)
2. Assist the app team as needed for configuring the basic `k8s` directory and file structure with the base + env overlays and yaml files.

3. Assist the app team with setting up a [webhook (until that is fully automated)](https://compass-ci.medtronic.com/getting-started/gitlab-repository-configuration/#webhooks-optional) so that changes to the `k8s` files immediately trigger a reconciliation in the cluster:

### Webhook token generation and update (clear rule)

Use this rule any time you create or rotate a webhook token:

- The token can be any random string; it does not need to follow a platform-specific format.
- Generate a random token with a tool such as: https://it-tools.tech/token-generator
- In the Flux/Kubernetes Secret referenced by the application's `-webhook.yaml`, store the Base64-encoded token value.
- In the GitLab project webhook configuration (`Settings -> Webhooks -> Secret token`), use the same token as plain text (not Base64).
- Effective requirement: the plain token configured in GitLab must match the decoded value represented by the Flux secret, or webhook requests will fail (typically HTTP 401/403).

## Common misses checklist

Before handing onboarding back to app team, confirm:
- `semantic-release` project share is in place for their group
- `trivy-policies` Job Token allowlist entry exists for their group
- `FLUX_GITOPS_REPO_RW_TOKEN` exists and has correct scopes/access
- `COMPASS_CI_TFC_*` variables are set to allow for automated `.tf` object generation during onboarding
- Group or app project has env blocks for all intended environments (`dev`, `staging`, `production`, etc.)
- `flux-register-<env>` rules match real branch strategy
- app change globs in deploy/verify jobs match actual repo layout (avoid frontend-only patterns in backend repos)

## Ongoing operations

### Token rotation

Maintain a rotation schedule for platform-owned token variables:
- `FLUX_GITOPS_REPO_RW_TOKEN`
- `TERRAFORM_REPO_RW_TOKEN`
- `COMPASS_CI_TFC_API_TOKEN`

There is an automated scheduled rotation pipeline in the [`semantic-release` project](https://medtronic.gitlab-dedicated.com/bcp_web/devops/semantic-release/-/pipeline_schedules), which runs `rotate_tokens.py` from `semantic-release/scripts/group-vars/` on a weekly basis.

How it works:
- The scheduled pipeline authenticates as the `svc-compass-ci` service account via a Personal Access Token (stored as a CI/CD variable on the semantic-release project, `SVC_COMPASS_CI_PAT`).
- It discovers eligible groups where that service account has sufficient permission (`Maintainer++` or `Owner`) and evaluates expiration dates of the three platform-owned token variables listed above.
- For groups where `svc-compass-ci` has been added as `Maintainer++` or higher, the automation can rotate the backing tokens and update the associated group CI/CD variables. This includes the GitLab-backed source tokens and the Terraform Cloud token variable used for onboarding automation.
- If the `svc-compass-ci` Personal Access Token is within the configured rotation window / nearing expiry, the automation also self-rotates that PAT and writes the fresh value back to the `SVC_COMPASS_CI_PAT` CI/CD variable in the `semantic-release` project, ensuring a fully-automated process.

Supported scheduled-pipeline variables:
- `RUN_TOKEN_ROTATION_ONLY=true`: runs only the token-rotation pipeline path and skips the normal semantic-release pipeline.
- `TOKEN_ROTATION_MODE=dry-run|apply`: selects whether the schedule only reports planned actions (`dry-run`) or actually rotates tokens and updates variables (`apply`).
- `TOKEN_ROTATION_WINDOW_DAYS=14`: rotation threshold in days before expiry. Tokens expiring within this window are considered due for rotation.
- `TOKEN_ROTATION_VALID_MONTHS=12`: validity period to apply when creating or rotating GitLab-backed tokens.
- `TOKEN_ROTATION_ROTATE_TFC=true|false`: enables or disables Terraform Cloud token rotation as part of the scheduled run.

**Prerequisite:**
- Each onboarding group must grant `SVC-compass-ci` the required permissions at the top-level group. See [Give `SVC-compass-ci` group permissions](../getting-started/gitlab-repository-configuration.md#give-svc-compass-ci-group-permissions).
- If teams do not grant the service account that access, their tokens will *not* be automatically rotated and they will run the risk of pipeline failures for expired tokens until they are manually updated.

Operationally, as long as the service account has the required permissions on the groups using our service, the scheduled rotation job can maintain these token values automatically and keep the corresponding CI/CD variables updated.

### Access audits

On a regular cadence (monthly/quarterly):
- review `trivy-policies` Job Token allowlist entries for stale groups/projects
- review `flux-gitops` project access tokens for expiry and least-privilege alignment
- verify onboarding groups still require `semantic-release` share

## Related references

- [`docs/getting-started/gitlab-repository-configuration.md`](../getting-started/gitlab-repository-configuration.md)
- [`docs/getting-started/tooling-and-access.md`](../getting-started/tooling-and-access.md)
- [`docs/cicd-pipeline/pipeline-stages.md`](../cicd-pipeline/pipeline-stages.md)
- [`docs/references/argo-to-compass-migration-plan.md`](../references/argo-to-compass-migration-plan.md)
- [`semantic-release/flux-registration.yml`](https://medtronic.gitlab-dedicated.com/bcp_web/devops/semantic-release/-/blob/main/flux-registration.yml?ref_type=heads)
