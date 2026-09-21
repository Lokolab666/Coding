---
label: "GitLab Repository Configuration"
icon: /static/logos/gitlab.svg
order: 120
---

# GitLab Repository Configuration

Set up your application repository so Compass CI can build, release, and deploy your workloads. This phase is completed by your team.

---
## Branch Strategy & Protected Branches

Now that your repository has the required files, set up the branch structure for environment-based deployments.

Compass CI recommends **branch-based environments** - each environment has its own protected branch for deployment.

### Required Protected Branches

Create these branches in GitLab and protect them:

| Branch Name | Environment | Purpose |
|--------|-------------|---------|
| **dev** | Development | For developer testing (optional, but recommended) |
| **testing** | Testing | For QA and integration testing (optional) |
| **staging** | Staging | Pre-production validation |
| **release** | Release | For release candidates (optional) |
| **main** (or **production**) | Production | Live environment |

**Recommended setup:** `dev` + `staging` + `main`

### Set Up Protected Branches

1. In GitLab, go to **Settings → Repository → Protected Branches**
2. Create each branch (e.g., type "dev" and click "Create wildcard")
3. Configure protection rules (customize to your preferences):
   - **Require code review** - Mandate merge request approvals
   - **Allow force pushes** - Uncheck (prevent accidental overwrites)
   - **Require status checks** - CI pipeline must pass

### Branch-Specific Deployments

Your Flux GitOps configuration watches specific branches:

```yaml
# For staging environment
spec:
  ref:
    branch: staging    # Watch staging branch

---

# For production environment
spec:
  ref:
    branch: main       # Watch main branch
```

When you merge to `staging`, Flux automatically:
- Detects the updated manifests in the `staging` branch
- Applies the changes to the staging environment
- Deploys your application

### GitLab Environments (Required Before First Pipeline)

Create your GitLab project environments manually so environment-scoped variables resolve correctly on the first run.

If environments do not exist yet, jobs that rely on environment-scoped variables (for example `ROLE_ARN` and `EXT_ID`) may run without the expected values.

**Where:** `Operate → Environments`

1. In GitLab, go to **Operate → Environments**.
2. Click **Create environment**.
3. Create one environment for each deployment branch you use:
  - `dev`
  - `testing`
  - `staging`
  - `release`
  - `main` (or `production` if your project uses that naming)
4. Save each environment.
5. Go to **Settings → CI/CD → Variables** and scope project secrets to the matching environment names.

Recommended: create these environments immediately after creating protected branches and before triggering the first pipeline.

---

## Group Settings in Gitlab
!!!
If this is the first project in your GitLab group to use Compass CI, the following Group Settings must be completed. Subsequent projects in the group do not need to repeat this setup as they will inherit these settings.
!!!

### Give `SVC-compass-ci` group permissions

To support automated rotation of all required GitLab and Terraform Cloud tokens, add `SVC-compass-ci` as an `Owner` or `Maintainer++` on the top-level GitLab group associated with your application. The scheduled rotation discovers eligible groups through that service account and refreshes tokens before they expire, ensuring no disruption to future pipeline executions due to expired tokens.

### Create GROUP_RW_ACCESS_TOKEN

Create this once per GitLab group before running Compass CI pipelines. This token is used for app repo write/API operations in shared templates (semantic-release tagging, deploy automation commits, security issue automation, and other GitLab API updates).

**Where:** `Group → Settings → Access tokens`

| Field | Value |
|---|---|
| **Name** | `compass-ci-rw-<group-name>` (example: `compass-ci-rw-bcp-web-common`) |
| **Role** | `Maintainer` minimum |
| **Scopes** | `api`, `write_repository`, `self_rotate` |
| **Expiration date** | per your policy (recommended to align with scheduled rotation cadence) |

After creating it:
1. Copy the token value immediately (GitLab only shows it once).
2. Go to `Group → Settings → CI/CD → Variables`.
3. Add variable `GROUP_RW_ACCESS_TOKEN` with the token value.
4. Set `Masked=true` and `Protected=true`.

If this token is missing, template jobs that push commits/tags or call GitLab APIs can fail.

#### Group-Level CI/CD Variables
If this is the first project in your GitLab group to onboard to Compass CI, have a group owner create a temporary group access token (under Group -> Settings -> Access tokens) with the following details:
  - Name: `compass-ci-onboarding-token`
  - Role: `Owner`
  - Scope: `api`
  - Expiration: 1 week (or less)

Provide the generated token value to the `Infra-Argo-Global` team so they can assist with initial group-level variable setup. Instructions for the platform team can be found [in the reference guide](../references/platform-onboarding-runbook.md#3-configure-group-level-cicd-variables).

## Project Settings in GitLab

Configure these settings first to establish access and credentials for Compass CI automation.

### Enable Instance Runners

By default, GitLab CI/CD pipeline jobs will not execute without runners enabled. You must explicitly enable Instance Runners for your group to use the shared runners managed by the platform team.

**Where:** `Settings → CI/CD → Runners`

**Steps:**
1. Go to `Settings → CI/CD → Runners`
2. Click on the **Instance** tab
3. Toggle **ON** the instance runners to enable them for your group
4. Your pipeline jobs can now use the shared runners with the tags: `linux-small-amd64-k8s-aws`, `linux-medium-amd64-k8s-aws`, etc.

Without enabling instance runners, all pipeline jobs will remain in a pending state and never execute.

### Deploy Token

Flux runs inside the cluster with no user session — it authenticates to your GitLab repository using a Kubernetes Secret that is built from a deploy token. You create the deploy token, store its value as a CI/CD variable, and the job does the rest.

1. **Create the deploy token**

    **Where:** `Settings → Repository → Deploy tokens`

    | Field | Value |
    |---|---|
    | **Name** | `gitlab-deploy-token` |
    | **Expiration date** | leave blank |
    | **Username** | `gitlab-flux-deploy-token` |
    | **Scopes** | `read_repository` |

    Copy the generated token password immediately — GitLab only shows it once.

2. **Store it as a CI/CD variable**

    **Where:** `Settings → CI/CD → Variables` (project level, masked)

    | Variable | Value | Masked |
    |---|---|---|
    | `FLUX_RO_DEPLOY_TOKEN` | The deploy token value from Step 1 | Yes |
    | `FLUX_RO_DEPLOY_TOKEN_USERNAME` | The deploy token Username from Step 1 (for example `gitlab-flux-deploy-token`) | No |

**What happens next — the `flux-register-env` job:**
1. Reads `FLUX_RO_DEPLOY_TOKEN` and `FLUX_RO_DEPLOY_TOKEN_USERNAME` and encodes both for the Kubernetes Secret
2. Generates a Kubernetes Secret named `<app>-gitlab-flux-token`
3. If `.sops.yaml` is missing, the job bootstraps SOPS automatically (generates keypair + `.sops.yaml` + stores private key as a GitLab project CI/CD file variable `SOPS_AGE_PRIVATE_KEY`)
4. Commits generated/updated infrastructure files to `flux-gitops` via a merge request and assigns to the platform team for approval.

!!!info SOPS bootstrap automation
App teams should download `SOPS_AGE_PRIVATE_KEY` for local decrypt/edit/re-encrypt workflows and store it in their local SOPS key file location.
!!!

Every `GitRepository` source created for your app references this Secret:

```yaml
spec:
  url: https://medtronic.gitlab-dedicated.com/bcp_web/common/einstein.git
  secretRef:
    name: einstein-gitlab-flux-token   # ← built from FLUX_RO_DEPLOY_TOKEN + FLUX_RO_DEPLOY_TOKEN_USERNAME
```

Flux uses the Secret to authenticate to GitLab when polling your repository for changes and applying deployments.

!!!info Rotation
Create the deploy token with no expiry date. To rotate: update `FLUX_RO_DEPLOY_TOKEN` with the new token password and re-run the `flux-register-env` job — it will regenerate and re-commit the Kubernetes Secret. If the deploy token Username changes, also update `FLUX_RO_DEPLOY_TOKEN_USERNAME` to match exactly.
!!!

### CI/CD Variables

Add CI/CD variables required by Compass CI. For teams using Compass CI across multiple projects in the same GitLab group, set the shared variables at the **Group** level. Project-specific variables should remain at the **Project** level.

**Where:** `Settings → CI/CD → Variables`

#### CI/CD Variables Required

**Note:** Teams can override any group-level variables at sub-group or project levels as needed unless a platform standard is explicitly required.

#### Project-level variables (required for all projects):

These variables can be defined in your `.gitlab-ci.yml` file under the `variables:` section or in GitLab's Settings → CI/CD → Variables:

| Variable | Description | Example | Provided by | Visibility + Flags |
| --- | --- | --- | --- | --- |
| `COST_CENTER` | Billing/operational cost center | `"2030102010"` | App team | Visible |
| `DATA_CLASSIFICATION` | Security policy enforcement level | `HIGHLY_SENSITIVE`, `SENSITIVE`, `INTERNAL_USE_ONLY`, or `PUBLIC` | App team | Visible |
| `TEAM_NAME` | Team/group namespace key | `argo` | App team | Visible |
| `PROJECT` | Project/app identifier | `my-app` | App team | Visible |
| `CI_REGISTRY_IMAGE` | Full container image path | `${CI_REGISTRY_BASE}/bcp-web-docker-releases-virtual/com/medtronic/web/app/${CI_PROJECT_PATH}` | App team | Visible |
| `K8S_DIR` | Kubernetes manifest directory | `k8s` | App team | Visible (containerized apps) |
| `FLUX_RO_DEPLOY_TOKEN` | Deploy token password used by `flux-register-env` to create Flux GitRepository secret. See [Deploy Token](#deploy-token) for creation instructions. | - | App team | Masked |
| `FLUX_RO_DEPLOY_TOKEN_USERNAME` | Deploy token username paired with `FLUX_RO_DEPLOY_TOKEN` | - | App team | Visible |
| `SOPS_AGE_PRIVATE_KEY` | File variable created by `flux-register-env` during SOPS bootstrap | - | Generated by pipeline | Masked (file) |

**Example in `.gitlab-ci.yml`:**
```yaml
variables:
  PROJECT: my-app
  TEAM_NAME: argo
  COST_CENTER: "2030102010"
  DATA_CLASSIFICATION: SENSITIVE
  CI_REGISTRY_IMAGE: "${CI_REGISTRY_BASE}/bcp-web-docker-releases-virtual/com/medtronic/web/app/${CI_PROJECT_PATH}"
  K8S_DIR: "k8s"  # Only if building containers
```

!!!warning Important: Variables vs Secrets
**Define in `.gitlab-ci.yml`:**
- Project configuration variables (listed above)
- Non-sensitive values that are version-controlled

**Define in GitLab UI (Settings → CI/CD → Variables):**
- Credentials and secrets (passwords, tokens, API keys)
- Environment-specific values that differ per deployment
- Sensitive data that should never be committed to Git
!!!

#### Project-level secrets (scoped per Environment in GitLab UI):

These **secrets** are set in GitLab's UI at **Settings → CI/CD → Variables** and can be scoped to specific environments using the **Environments** dropdown. This allows different values per environment (dev, staging, production, etc.).

| Variable | Purpose | Provided by | Visibility + Flags |
| --- | --- | --- | --- |
| `EXT_ID` | External ID used for deployment verification role assumption | Platform team | Masked + Env scoped |
| `ROLE_ARN` | AWS role ARN used for deployment verification | Platform team | Visible + Env scoped |

**To scope a variable to an environment:**
1. When adding/editing a variable in `Settings → CI/CD → Variables`
2. Use the **Environments** dropdown
3. Select the specific environment (matches your branch name: `dev`, `staging`, `production`, etc.)
4. The variable will only be available when deploying to that environment

Contact the platform team for any variable values marked as Platform team provided.

### Grant the Group RW Token Bot Permissions on Protected Branches

If your protected branch rules restrict who can push or merge, you must allow the bot identity behind [`GROUP_RW_ACCESS_TOKEN`](#create-group_rw_access_token) to perform those actions.

Without this, automation jobs that commit/tag/push (for example `semantic-release`, deploy/tag jobs, and webhook/security automation) can fail with errors like:
- `remote: GitLab: You are not allowed to push code to protected branches on this project.`

**How to configure it:**
1. Go to **Settings → Repository → Protected Branches**
2. Edit each protected branch used by automation (`dev`, `testing`, `staging`, `release`, `main` as applicable)
3. In **Allowed to push** and/or **Allowed to merge** (based on your policy), search for the token name
4. Select the bot account shown as `group_<...>_bot`
5. Save the protection rule

Example (search by token name and select the `group_..._bot` identity):

![](../static/gitlab-rw-token-allow-to-merge-push.png)

---

## Determining Your Data Classification


The `DATA_CLASSIFICATION` variable is a required CI/CD variable that determines your application's security policy enforcement levels. Before setting this value, you should complete a formal security and privacy assessment through Medtronic's GCISO (Global Chief Information Security Office).

### Submit a Security and Privacy Intake Request

**All applications must complete a Security and Privacy Intake Request before going live.** This assessment determines your application's official data classification level.

**Steps:**
1. Review the [Personal Data](https://medtronic.sharepoint.com/sites/dataprivacy/SitePages/Personal-Data.aspx) definition
2. Review the [Information / Data Classification](https://mrcsd2.medtronic.com/d2anonserv/getcontentbynamestatus?docbase_name=mrcs&auth=crts_wmuser&format=c2pdf&name=91249-InfoSec-IC&status=Effective) levels document
3. Complete the [Security and Privacy Intake Request Form](https://medtronicprod.service-now.com/it/?id=sc_cat_item_order_guide&sys_id=10bd221adb726c1014c354f94896192d) for your application
4. Retain your Privacy Impact Assessment Number (PIA#) for application go-live
5. Use the classification determined by the assessment in your `DATA_CLASSIFICATION` variable

**If your application's data handling changes:**
- Complete a new assessment to determine if your classification level should change
- Update the `DATA_CLASSIFICATION` variable in `.gitlab-ci.yml` to reflect the new level
- The pipeline will automatically adjust vulnerability remediation requirements and due dates

### Data Classification Levels

| Level | Description | Security Requirements |
| --- | --- | --- |
| `HIGHLY_SENSITIVE` | Personal health information, financial data, credentials | **All vulnerabilities** (Critical, High, Medium, Low) block deployment |
| `SENSITIVE` | Proprietary business data, employee information | Critical, High, and Medium vulnerabilities block deployment |
| `INTERNAL_USE_ONLY` | Internal tools, non-sensitive business data | Critical, High, and Medium vulnerabilities block deployment |
| `PUBLIC` | Public-facing marketing content, documentation | Critical, High, and Medium vulnerabilities block deployment |

[!ref Learn more about security policy enforcement](../cicd-pipeline/security-gitlab-issues.md)

---

## Add Required Files

Your repository needs these files in order to use Compass CI.

!!!info Prerequisites Validation
The Compass CI pipeline includes an automatic validation job that checks for all required files and CI/CD variables at the start of every pipeline run. This helps identify configuration issues early before any builds or deployments occur.

The validation job checks for:
- **Required variables in .gitlab-ci.yml**: `COST_CENTER`, `DATA_CLASSIFICATION`, `TEAM_NAME`, `PROJECT`, `CI_REGISTRY_IMAGE`
- **Required files** (all apps): `.gitlab-ci.yml`, `commitlint.config.js`, `package.json`
- **Additional files** (containerized apps): `Dockerfile`, `k8s/` directory

If validation fails, you'll see clear error messages indicating what's missing with code examples and links to this documentation.

**Note:** Secret credentials (passwords, tokens) should still be added via GitLab's UI at **Settings → CI/CD → Variables**.
!!!

### 1. `.gitlab-ci.yml`
Defines your CI/CD pipeline stages and jobs.

!!!warning
The filename must be exactly `.gitlab-ci.yml` (including the leading dot). GitLab will not detect the pipeline file if it is named incorrectly.
!!!

[!file Download Template](./../static/templates/.gitlab-ci.yml)

**Key sections:**
- `stages` - Pipeline execution order (lint, build, test, security, release, deploy)
- `variables` - Project-specific configuration
- `include` - Import reusable templates from semantic-release project
- `jobs` - Individual pipeline tasks

[!ref Learn more](../cicd-pipeline/index.md)

---

### 2. `.releaserc.json`
Configures semantic-release for automated versioning based on conventional commits.

!!!warning
The filename must be exactly `.releaserc.json` (including the leading dot).
!!!

[!file Download Template](./../static/templates/.releaserc.json)

**Features:**
- Analyzes commits to determine version bump (major.minor.patch)
- Generates CHANGELOG.md automatically
- Creates Git tags
- Posts release notes to GitLab

[!ref Learn more](../cicd-pipeline/semantic-versioning.md)

---

### 3. `commitlint.config.js`
Enforces conventional commit message format to help automate versioning of the application based on commit messages.

[!file Download Template](./../static/templates/commitlint.config.js)

**Commit format:**
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Valid types:** `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`

**Examples:**
```
feat(auth): add SSO integration
fix(api): resolve timeout issue on large datasets
docs: update deployment guide
```

==- Optional: Enable Guided Commits with Commitizen

If you want a guided commit workflow, add a `.cz.toml` file. This does not replace commitlint; it helps authors create valid messages.


1. Add `.cz.toml` to your repository root with this content:

```
[tool.commitizen]
name = "cz_conventional_commits"
version = "0.0.0"
tag_format = "$version"
```

2. Add `commitizen` to your `package.json` devDependencies:

```
{
  "devDependencies": {
    "commitizen": "^4.3.0"
  }
}
```

3. Run `npm install` to update dependencies.

4. Use `git cz` instead of `git commit` for an interactive prompt, or commit normally and commitlint will validate.

**Note:** Commitizen is optional. Commitlint will still enforce conventional format even without Commitizen.
===

### 4. Pre-commit Hook Setup (Recommended)

1. Download the example config to your repository root as `.pre-commit-config.yaml` and adjust as needed.
[!ref .pre-commit-config.yaml](../static/templates/.pre-commit-config.yaml)

2. Install the hooks:
```bash
pre-commit install
pre-commit install --hook-type commit-msg
```

For installation and command troubleshooting (including when `pre-commit` is not recognized), see [Tooling & Access → Pre-Commit (Recommended)](./tooling-and-access.md#pre-commit-recommended).

!!!
Browse the full list of available hooks on the [official index](https://pre-commit.com/hooks.html).
!!!

`pre-commit install --hook-type commit-msg` is required for commit message validation (`commitlint`) to run locally. Commitizen is optional and provides a guided commit workflow.

---

### 5. `Dockerfile`
Multi-stage Dockerfile for building your application container.

[!file Download Template](./../static/templates/Dockerfile)

**Best practices:**
- Use multi-stage builds to minimize image size
- Leverage build cache with proper layer ordering
- Use specific base image tags (not `latest`)
- Run as non-root user
- Include health check endpoints
- Use Artifactory-backed registries for images and packages

[!ref Learn more](./containerization.md)

---

### 6. `package.json` and `package-lock.json`
Dependency management and tooling configuration.

[!file Download Template](./../static/templates/package.json)

**Required devDependencies:**
```json
{
  "devDependencies": {
    "@commitlint/cli": "^20.1.0",
    "@commitlint/config-conventional": "^20.0.0",
    "@semantic-release/changelog": "^6.0.3",
    "@semantic-release/commit-analyzer": "^13.0.1",
    "@semantic-release/exec": "^7.1.0",
    "@semantic-release/git": "^10.0.1",
    "@semantic-release/gitlab": "^13.2.9",
    "@semantic-release/npm": "^13.1.2",
    "@semantic-release/release-notes-generator": "^14.1.0",
    "conventional-changelog-conventionalcommits": "^9.1.0",
    "semantic-release": "^25.0.2"
  }
}
```

!!!info Version guidance
The versions shown above are a tested baseline, not a strict lock. Teams may upgrade dependency versions and CI include versions over time.

Any newer versions are acceptable as long as your pipeline passes end-to-end validation after the change.
!!!

**To generate package-lock.json:**
```bash
npm install
```

**To upgrade all listed packages to latest in one command (optional):**
```bash
npm install -D @commitlint/cli@latest @commitlint/config-conventional@latest @semantic-release/changelog@latest @semantic-release/commit-analyzer@latest @semantic-release/exec@latest @semantic-release/git@latest @semantic-release/gitlab@latest @semantic-release/npm@latest @semantic-release/release-notes-generator@latest conventional-changelog-conventionalcommits@latest semantic-release@latest
```

**Adding to existing package.json:**

If you already have a package.json, merge the devDependencies above with your existing ones, then run `npm install`.

---

### 7. `k8s/` Directory Structure
Kubernetes manifests organized with Kustomize.

!!!
For existing teams migrating from the legacy Argo pipeline, use the [`kube-migrate-*` job](../references/argo-to-compass-migration-plan/#phase-3-k8s-parity-mapping-and-migration) to automatically configure the `k8s` directory structure in your project.
!!!

```
k8s/
+-- base/
-   +-- deployment.yaml
-   +-- service.yaml
-   +-- ingress.yaml
-   +-- kustomization.yaml
+-- production/
    +-- configmap.yaml
    +-- deployment-patch.yaml
    +-- kustomization.yaml
```

[!ref Learn more](./kubernetes-manifests.md)

After adding these files to your repository, commit them to your default branch (typically `main`).

## Platform Team Steps

1. Complete the [platform-managed steps](../references/platform-onboarding-runbook.md#platform-managed-responsibilities). Including:
   - Setting up Group-level CI/CD variables & tokens
   - Share `semantic-release` project for template consumption
   - Update `trivy-policies` allowlist
2. Approve onboarding MRs as teams complete the [pipeline trigger](#trigger-a-pipeline).

---

## Trigger A Pipeline
1. Create feature branches from your lowest-level environment (e.g. `dev`)
2. Make changes using conventional commits
3. Open a merge request and pass all pipeline checks
4. Merge into the environment branch (e.g. merge `feat/compass-ci-onboarding` --> `dev`)
    - Run the `flux-register-env` job to onboard the new application environment. If TFC variables are present, the job also upserts the app SOPS key variable in the mapped Terraform Cloud workspace. The Platform team will review and approve the onboarding for each application.
5. Flux deploys automatically to the corresponding environment.

---
## Webhooks (Optional)

Configure webhooks to enable push-based deployment updates. With webhooks, Flux immediately detects and applies your changes when you push to the repository. Without webhooks, Flux polls for changes every 10 minutes (pull-based), so any changes made may be delayed by up to 10 minutes.

**Where:** `Settings → Webhooks`

**Important:** You need a **separate webhook for each cluster** your application deploys to. For example, if your app deploys to dev, staging, and production clusters, create three webhooks.

**Naming convention for webhooks:**
```
compass-flux-{cluster-name}
```

Examples:
- `compass-flux-tf-argo-dev`
- `compass-flux-tf-argo-prd`

**Recommended events:**
- Push events
- Tag push events

**For Push events, filter by branch using a regular expression pattern:**

In the Push events section, enable **Wildcard/Regex Push events** and set a pattern to match the branches that deploy to that cluster:

```
^(dev|testing)$
```

This example triggers the webhook when pushing to either the `dev` or `testing` branch. Adjust the pattern for each cluster:
- **Dev cluster:** `^(dev|testing)$` - dev and testing environments both exist inside the Dev cluster in this example
- **Production cluster:** `^(staging|release|main)$` - staging, release, and production environments all exist inside the Production cluster in this example

**SSL verification:** Leave **Enable SSL verification** checked.

Webhook URLs are listed on the [platform inventory pages](../platform-inventory/index.md) for each cluster.

For the webhook secret token:
- Use any sufficiently random string.
- Generate one with a tool such as [IT Tools Token Generator](https://it-tools.tech/token-generator).
- Store that token in Flux as the webhook secret value (Kubernetes Secret `data.token` uses Base64 encoding).
- In GitLab webhook configuration, paste the same token as plain text in **Secret token**.
- The value configured in GitLab must match the value represented by the Flux secret, or webhook delivery fails with 401/403.

**Test the webhook:** After saving, use the **Test** dropdown on the Webhooks page and select **Push events**. You should see a confirmation banner at the top of the page:

```
Hook executed successfully: HTTP 200
```

---

## Next Steps

[!ref icon="container" text="Add Kubernetes Manifests"](../getting-started/kubernetes-manifests.md)
[!ref icon="check" text="Configure Health Checks"](../getting-started/kubernetes-manifests.md#2-configure-health-checks)
[!ref icon="globe" text="Web Access & Hostnames"](../how-to/web-access-hostnames.md)
[!ref icon="key" text="Configure Secrets"](../how-to/secrets-management.md)
