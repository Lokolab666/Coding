---
label: Argo to Compass Migration Plan
icon: git-branch
order: 55
---

# Argo to Compass CI Migration Plan

## When to use this runbook

Use this runbook when:
- Migrating an existing Argo CI/CD application on `code.medtronic.com` to Compass CI on `medtronic.gitlab-dedicated.com`.

!!!danger
This page is for migrations only and is not needed for brand-new teams onboarding to the platform. Brand new applications can follow the [GitLab Repository Configuration](../getting-started/gitlab-repository-configuration.md) and Platform team can follow the [Platform and Support Onboarding Runbook](../references/platform-onboarding-runbook.md).
!!!

## Scope and assumptions

This plan defines the first-wave migration path for existing Argo client applications to the semantic-release / Compass CI model.

- Scope: all environments, including production.
- Deployment model: branch-based environments.
- Flux onboarding model: self-service with auto-merge.
- Secrets migration model (wave 1): SOPS-first. Existing base64-only Kubernetes Secret files are normalized and encrypted before commit.

---

## Why migrate

Legacy Argo applications depend on externally generated YAML and pipeline behavior that is hard to evolve per team. Compass CI moves app delivery to a standardized, app-owned model:

- Pipeline and release logic in the application repository.
- Kubernetes manifests in the application repository (`k8s/base` + environment overlays).
- Flux registration driven from `.gitlab-ci.yml` environment blocks.
- Platform guardrails (security scanning, policy checks, release conventions) applied consistently.

---

## Reference mapping (sample)

Sample applications that can be used for reference of the repository structure and configurations:
- [einstein](https://medtronic.gitlab-dedicated.com/bcp_web/common/einstein)
- [newton](https://medtronic.gitlab-dedicated.com/bcp_web/common/newton)

---

## Migration phases

### High-level execution summary (operational order)

Use this as the practical sequence teams should follow during migration execution:

1. **Migrate repository to GitLab Dedicated first**
    - Complete import, tag/history validation, and variable recreation.
2. **Complete repository modernization and CI automation prerequisites**
    - Configure `.gitlab-ci.yml`, required project/group variables, required repo files, protected branches, and baseline tagging setup before running migration automation jobs.
3. **Configure Flux-GitOps wiring**
    - Register the app so Flux watches the app repository `k8s/` paths for each environment by running the `flux-register-*` jobs.
4. **Map and migrate Kubernetes manifests into the app repo**
    - Translate existing Argo-managed manifests into `k8s/base` + `k8s/<env>` overlays and validate parity by running the `kube-migrate-*` jobs.
5. **Bring up the app in the newly configured namespace**
    - Reconcile through Flux and confirm healthy runtime behavior in target environments.
6. **Bring down the app in the legacy namespace**
    - Remove folder for legacy `argo-*` prefixed namespace from the associated FluxConfigs/argo* project.

This summary is an execution guide and complements the detailed phase controls below.

## Phase 0: Dedicated Instance Readiness Gate

**Goal:** verify the GitLab Dedicated instance is fully ready to receive this application **before any migration work begins**. Because teams are moving from on-prem GitLab 16.x to GitLab Dedicated 18.x at the same time as adopting Compass CI, the two concerns must be de-risked separately. A broken import looks identical to a broken pipeline until you separate them.

This gate is completed once per application repo. Do not proceed to Phase 1 until all exit criteria below are met.

### Repo import verification

!!!warning **Action required**
 To initiate a repository migration from `code.medtronic.com` to the GitLab Dedicated instance, teams must contact [Jose Chaves](mailto:jose.chaves@medtronic.com) to submit a migration request and receive a scheduled timeline for when the work will be completed. When submitting the request, explicitly ask that the source repository on `code.medtronic.com` **remain active and read-write** until cutover has been signed off.
!!!

Once the migration has been performed by the platform team, verify the following after the project appears on the Dedicated instance:
- Full commit history is present and matches on-prem depth.
- All existing Git tags are present and point to the correct commits (`git rev-parse <tag>` matches on-prem).
- Default branch is correctly set to `main`.

### Platform Team Responsibilities
Platform team should execute the required [platform-managed onboarding steps](../references/platform-onboarding-runbook.md#platform-managed-responsibilities).

### GitLab Group/Project Configuration Complete

App team should follow all steps in the [Gitlab Repository Configuration Guide](../getting-started/gitlab-repository-configuration.md) to get their project configured for use with Compass CI.

### Runner and pipeline smoke test

- Instance runners are visible and enabled on the project (`Settings → CI/CD → Runners`).
- Confirm the `include: project: bcp_web/devops/semantic-release` template resolution succeeds — this verifies cross-project CI access from Dedicated.

### On-prem fallback window

- The on-prem project remains **read-write and deployable** until Phase 5 production cutover is complete and a stabilization window has passed.
- Document the on-prem fallback trigger: if a Dedicated pipeline failure blocks a production deploy, the on-prem path is the rollback. Assign a named owner responsible for authorizing that rollback.
- Do not disable or archive the on-prem project until Phase 5 exit criteria are signed off.

### Exit criteria

- Full git history and all tags verified on Dedicated instance.
- Protected branches recreated.
- GitLab environments created in `Operate → Environments` for all in-scope deployment branches.
- All CI/CD variables present (group and project level).
- Smoke-test pipeline ran successfully on Dedicated.
- On-prem project retained as fallback with named rollback owner.

---

## Phase 1: Repository Modernization and Automation Prerequisites

**Goal:** Make application Compass CI ready. Complete all project/repo setup required for `flux-register-*` and `kube-migrate-*` automation before starting Phase 1 and Phase 2 execution.

This phase must be completed before running migration job automation.

### Required completion checklist

- Follow [GitLab Repository Configuration](../getting-started/gitlab-repository-configuration.md) and complete all required group/project setup.
- Ensure `.gitlab-ci.yml` includes required shared templates and defines environment blocks (`.dev`, `.testing`, `.staging`, `.prod`) with required env variables.
- Ensure migration template jobs are present when needed (`flux-register-*` and `kube-migrate-*`).

!!!
Watch a video-based walkthrough explaining the below steps [here](https://medtronic.sharepoint.com/:v:/r/sites/WebDevModernization/Shared%20Documents/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)/Compass%20CI/Demos%20and%20Recordings/End-to-End%20Project%20Migration.mp4?csf=1&web=1&e=sleTjJ).

Relevant sections:
- `0:20:33` - `1:18:00` (app repo setup and prerequisites)
- `1:24:00` - `1:29:00` (webhook setup)
!!!

#### Net-new files (not present in legacy Argo repos)

After following the [GitLab Repository Configuration](../getting-started/gitlab-repository-configuration.md), these files should be present in the repository. The [`einstein`](https://medtronic.gitlab-dedicated.com/bcp_web/common/einstein) project can be used as a reference.

| File | Purpose | Reference
|------|---------|----------
| `.releaserc.json` | Semantic-release plugin and branch config | [Versioning migration](#versioning-migration-from-build-versionproperties)
| `package.json` + `package-lock.json` | Required by platform prerequisite checks, commitlint, and semantic-release | [GitLab Repository Configuration](../getting-started/gitlab-repository-configuration.md)
| `commitlint.config.js` | Conventional commit validation (required by CI lint job) | [GitLab Repository Configuration](../getting-started/gitlab-repository-configuration.md)
| `.pre-commit-config.yaml` | Local git hook enforcement (YAML, Dockerfile lint, commit-msg) | [Tooling & Access](../getting-started/tooling-and-access.md)
| `.sops.yaml` | SOPS encryption rule config; maps secret file paths to the age public key. Auto-generated by `.flux-register-*` job as part of [Phase 2](#phase-2-flux-registration-cluster-wiring-sops-key-and-terraformiam-foundation). | [Secrets Management](../how-to/secrets-management.md)
| `k8s/base/` + `k8s/<env>/` overlays | Kustomize manifest structure replacing legacy `edata/`. Auto-generated by `.kube-migrate-*` job. | [K8s File Migration](#phase-3-k8s-parity-mapping-and-migration)<br/>[Kubernetes Manifests](../getting-started/kubernetes-manifests.md)

#### Files to rewrite or significantly change
The below files should be replaced or updated as part of migration. The [`einstein`](https://medtronic.gitlab-dedicated.com/bcp_web/common/einstein) project can be used as a reference.

| File | What changes
|------|-------------
| `.gitlab-ci.yml` | Full rewrite: include semantic-release shared templates, define environment blocks (`.dev`, `.testing`, `.staging`, `.prod`) with `CLUSTER_NAME`, `NAMESPACE_NAME`, `BRANCH`, `PROMOTION_LEVEL`, and `EKS_REGION`. Teams should [download the template](../static/templates/.gitlab-ci.yml) for initial setup.
| `Dockerfile` | Review against containerization standards; remove legacy `edata/` copy steps if present; remove any SonarQube scan stages or tooling (code quality analysis is now handled by built-in GitLab code quality scans via the shared pipeline templates).

### Versioning migration (from build-version.properties)

Legacy apps tracked version in `build-version.properties`. Semantic-release takes over this responsibility; the file is no longer the release source of truth.

1. Check for existing tags after repo import
   - After the Phase 0 import, run `git tag -l | sort -V` on the Dedicated clone.
   - **If tags exist and transferred correctly**: verify the most recent tag is on the correct production-equivalent commit (`git log --oneline <tag>`). If it is, no baseline tag creation is needed — semantic-release will use the most recent existing tag as its starting point.
   - **If no tags exist** (repo had no prior releases, or tags were lost during import): proceed to step 2 to seed a baseline.

2. Seed a baseline Git tag (only if no tags exist)
   - Read `BUILD_RELEASE` from `build-version.properties` and normalize to valid SemVer (e.g. `0.0.52`), stripping legacy suffixes.
   - Tag name must match `tagFormat` in `.releaserc.json` (default: `${version}`, so tag is bare semver).
   - This is required — without any tag the pipeline has no version reference and will fail.

   ```bash
   # Replace 0.0.52 with your actual baseline version from build-version.properties
   git checkout <release-enabled-branch>   # e.g. main or staging
   git pull
   git tag -a 0.0.52 -m "baseline for semantic-release"
   git push origin 0.0.52
   ```

3. Verify tag is on the right commit before proceeding
   - Regardless of whether the tag came from import or was newly created:
   ```bash
   git log --oneline -1 <your-baseline-tag>
   # Must match the production-equivalent HEAD commit
   ```
   - If the tag is on the wrong commit, delete and retag before running any pipeline.

4. Do not manually create a GitLab Release for the baseline
   - Semantic-release creates GitLab Releases automatically from the next real conventional commit onward.
   - Historical releases can be backfilled manually if needed for UI visibility.

5. Trigger first automated release
   - Merge a conventional commit (`fix:`, `feat:`, etc.) to a release-enabled branch (`staging` or `main`).
   - Semantic-release computes next version from commits after the baseline tag.
   - Verify outputs: new Git tag, GitLab Release, `CHANGELOG.md` update, `package.json` version bump.

6. Validate and decommission legacy version source
   - Run the `semantic-release-conditions` dry-run job before the first real release to verify auth, branch rules, and plugin config.
   - After the first successful automated release, remove version-driving logic tied to `build-version.properties` from build/release jobs.
   - Keep the file only if any non-release runtime logic still reads from it.

**Notes applicable to all onboarded projects:**
- `package.json` is required by platform prerequisite checks even for non-Node repos.
- Version progression is commit-driven after baseline; stop all manual version bumps.
- Baseline tag is the hard dependency — no tag means no version and pipeline failure.
- Tags are not transferred by all import methods — always verify post-import before first pipeline run.

### Exit criteria
- Shared template includes and project-level Compass CI configuration are in place and valid.
- `semantic-release-conditions` dry-run passes on Dedicated.
- Baseline version/tag strategy is confirmed (existing tags validated or baseline tag created when needed).
- All required GitLab project settings applied.
- Pre-commit hooks operational in repo.

---

## Phase 2: Flux Registration, Cluster Wiring, SOPS Key and Terraform/IAM Foundation

**Goal:** create the cryptographic and IAM/Terraform foundation required so existing secrets can be migrated and encrypted, and Flux can decrypt SOPS-encrypted manifests safely in every target environment.

**Ownership:**
  - **Application team:** runs `flux-register-*`, validates generated `.sops.yaml`, and downloads `SOPS_AGE_PRIVATE_KEY` for local SOPS usage.
  - **Platform/Cloud IAM team (or owning infra team):** reviews and approves auto-generated Terraform changes for Secrets Manager storage and IAM/role policy updates.

!!!
Watch a video-based walkthrough explaining the below steps [here](https://medtronic.sharepoint.com/:v:/r/sites/WebDevModernization/Shared%20Documents/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)/Compass%20CI/Demos%20and%20Recordings/End-to-End%20Project%20Migration.mp4?csf=1&web=1&e=sleTjJ).

Relevant section:
- `1:05:00` - `1:18:00` (flux environment registration and SOPS bootstrap)
!!!

### Phase 2 prerequisites (must be complete before automated onboarding)

- Phase 1 repository modernization and automation prerequisites are complete.
- Application team must grant any platform team member from [DL IT Argo Core Team](dl.itargocoreteam@medtronic.com) `Reporter`-level access to your overall GitLab group so that it will appear when the group is granted permission to the platform's `semantic-release` project.
- Platform team has completed the [platform-managed responsibilities](../references/platform-onboarding-runbook.md#platform-managed-responsibilities).
- Application team has completed the [Gitlab Repository Configuration Guide](../getting-started/gitlab-repository-configuration.md).

### Required implementation steps

1. Bootstrap SOPS for the project **(Application Team)**
    - Run the environment-specific `flux-register-*` job to bootstrap SOPS (will only generate if `.sops.yaml` does not already exist in the project's branch).
    - The job generates a dedicated age keypair, writes `.sops.yaml`, and stores the private key as `SOPS_AGE_PRIVATE_KEY` (project file variable).
    - Download the key for local decrypt/edit/re-encrypt workflows; never commit the private key.

2. Platform/Support team reviews and approves generated onboarding changes:
    - **Flux-gitops onboarding MR** (expected): namespace/service account/secret-store/externalsecret/kustomization wiring for the app environment.
    - **Terraform changes** (expected for SOPS key path + IAM EKS role): ensure the owning infrastructure repo has the required Secrets Manager and role/policy updates, whether produced by automation or added manually.
    - If Terraform changes were not auto-generated, open/track the infra MR as a required Phase 1 dependency before proceeding.

3. Add or update Terraform-managed IAM role/policies for decryption access
    - Update the role used by Flux decryption path (typically `kustomize-controller` via IRSA, or the environment-specific app/ops role used by your secret retrieval flow).
    - Grant least-privilege read access to the exact AWS Secrets Manager paths created for the app SOPS key.
    - If a role already exists, update in place rather than creating duplicate roles; document the change and ownership.

4. Apply Terraform and verify infrastructure state
    - Run plan/apply through the approved Terraform pipeline.
    - Confirm secret presence in AWS Secrets Manager and confirm IAM policy attachments are effective in each target environment.

5. In the AWS console, open the created secret (`compass-ci/<gitlab-path>/<app-name>/sops-age-key`) in Secrets Manager.
    - Select `Retrieve secret value` to confirm SOPs key is set.

### Phase 2 file inventory example
Below is a sample of the inventory of files that should appear in the `flux-gitops` repository after the `flux-register-*` job has been run for an application environment.

| File | Required now | What it is used for | Why it is required
|------|--------------|---------------------|--------------------
| `kustomization.yaml` | Yes | Root aggregator for all app Flux registration resources in this app folder. | Without this, Flux will not apply the app registration bundle from this path.
| `source-token.yaml` | Yes | Flux `Secret` in `flux-system` containing GitLab deploy token credentials (`username`/`password`) used by app `GitRepository` sources. | Flux cannot clone the private app repo without credentials.
| `<env>-source.yaml` | Yes (per environment) | Flux `GitRepository` source for one branch/environment (example: `dev`) with repo URL, branch ref, and labels. | This defines what Git repo and branch Flux actually watches.
| `<env>-namespace.yaml` | Yes (per environment) | Creates the target namespace where this environment will reconcile app workloads. | Target namespace must exist before namespaced resources can reconcile cleanly.
| `<env>-serviceaccount.yaml` | Usually yes (per environment) | ServiceAccount (default in this pattern) annotated with IRSA role ARN for AWS access. | Required when app/ESO needs AWS IAM-backed access (Secrets Manager, KMS, SQS, etc.).
| `<env>-secretstore.yaml` | Yes when using ESO/SOPS key bootstrap | `SecretStore` that tells External Secrets Operator how to access AWS Secrets Manager in that namespace. | ExternalSecret cannot retrieve remote secrets without a backing SecretStore provider config.
| `<env>-externalsecret-sops-key.yaml` | Yes when using SOPS decryption | `ExternalSecret` that materializes the `sops.agekey` secret used by Flux Kustomization decryption. `remoteRef.key` must match the AWS Secret name set via Terraform in [Phase 2](#phase-2-flux-registration-cluster-wiring-sops-key-and-terraformiam-foundation). | Flux decryption fails for encrypted manifests until this secret is created in the app namespace.
| `<env>-kustomization.yaml` | Yes (per environment) | Flux `Kustomization` that points to app repo `./k8s/dev`, sets target namespace, and declares SOPS decryption secretRef. | This is the actual sync object that applies the app manifests into cluster.
| `webhook-token.yaml` | Optional (recommended for fast sync) | Flux `Secret` containing webhook token used by Receiver authentication. | Needed only if enabling push-triggered reconcile via webhook instead of polling-only behavior.
| `webhook.yaml` | Optional (recommended for fast sync) | Flux `Receiver` for GitLab push/tag events targeting this app's labeled `GitRepository` resources. | Enables immediate reconcile on commit/tag; otherwise Flux waits for poll interval.

==- Manual `flux-gitops` onboarding procedure (exception-only)
!!!
Use this only when automated onboarding cannot be used for a specific migration or as a recovery path.
!!!

1. Create the app folder in the cluster apps tree
    - In the `flux-gitops` project, add the application under `clusters/<cluster>/apps/<app-path>/` using the naming convention for the owning GitLab group/app.
    - Start from a similar existing app in the same cluster when possible, then rename the resources to the new application.
    - Keep the app-specific Flux resources together in that folder so the cluster apps tree remains the source of truth for registration.

2. Add the per-environment Flux source and sync objects
    - Create one `GitRepository` per branch/environment that Flux should watch.
    - Point each source to the correct GitLab project URL and the correct branch (`dev`, `testing`, `staging`, `release`, and `main`, depending on the environment).
    - Create the matching `Kustomization` for each environment and point `spec.path` to the app repo path that Flux should read, typically `./k8s/<env>`.
    - Set the correct target namespace and ensure the source and kustomization names line up with the app/environment naming used in the cluster.

3. Add the namespace and app support resources needed for reconciliation
    - Define the target namespace that the new app deployment will reconcile into (remove `argo-*` prefix from the namespace is the recommmended renaming).
    - Add any app-specific support resources that must exist before the app manifests are reconciled, such as service accounts and secret-store references.
    - If SOPS-encrypted manifests will be used, include the Flux-side decryption wiring so the `Kustomization` can reference the expected decryption secret when that secret is later provisioned.

4. Configure GitLab read access for Flux
    - Create a project deploy token with the following configuration:
        - **Name**: `gitlab-deploy-token`
        - **Expiration date**: leave empty
        - **Username**: `gitlab-flux-deploy-token`
        - **Scopes**: `read_repository`
    - Store the deploy token credentials in the Flux source secret expected by the `GitRepository` objects.
    - If teams choose to set an expiration date, document the rotation plan because an expired token stops Flux from reading the repository.

5. Configure webhook receiver resources when immediate sync is required
    - Add an app-specific Flux `Receiver` and webhook token secret using the shared cluster webhook ingress pattern documented in the webhooks guide.
    - Use one receiver per app per cluster, not one receiver per environment.
    - Webhook token generation rule: token can be any random string; store Base64 value in Flux secret (`data.token`) and use the same raw token in GitLab webhook **Secret token**.
    - After the receiver is created, retrieve the generated webhook path from cluster status and use it to configure the GitLab webhook endpoint.
    - For token generation, you may use: https://it-tools.tech/token-generator
    - If the webhook setup is not ready yet, the app can still reconcile on normal Flux polling; add the receiver once the base source and kustomization wiring is stable.

6. Update any cloud-side access required by the new namespace wiring
    - If the app uses IRSA, AWS Secrets Manager, KMS, SQS, or other IAM-backed dependencies, update the existing role/policy attachments so the new namespace and service account can access the required resources.
    - If a shared SOPS key or shared webhook token is intentionally reused across related apps, record that decision and confirm both apps reference the same backing secret.

7. Add the new app folder to the parent `apps/` kustomization
    - Open `clusters/<cluster>/apps/kustomization.yaml` in `flux-gitops`.
    - Add a reference to the new app folder under `resources:` so Flux applies everything under it to the cluster:
      ```yaml
      resources:
        - ./<app-path>/   # e.g. - ./contract-gpt-reactjs/
      ```
    - Without this entry, Flux will not pick up any of the resources created in the preceding steps, even if the files exist in the repo.

8. Validate the Flux registration before moving to manifest migration
    - Confirm the `GitRepository` objects reconcile successfully and point to the intended repo/branch.
    - Confirm the `Kustomization` objects reconcile against the intended app repo `k8s/` path and target namespace.
    - Confirm the namespace and prerequisite support objects exist in cluster.
    - If webhook wiring was added, confirm the receiver is Ready and the GitLab webhook test returns HTTP 200.
===

### Exit criteria

- Platform [prerequisites](../references/platform-onboarding-runbook.md) complete (`semantic-release` share, `trivy-policies` allowlist, platform tokens available).
- App folder and Flux registration resources created in `flux-gitops` for all in-scope environments.
- `GitRepository` and `Kustomization` objects point to the correct repo, branch, `k8s/` path, and target namespace.
- Deploy token access is configured for Flux source reads.
- Required namespace-scoped support resources are present for the new app namespace.
- If webhook mode is enabled, webhook token generation/matching is validated (Flux `data.token` Base64 value decodes to the same raw token configured in GitLab webhook Secret token).
- Reconcile status is healthy with no drift loops.
- If webhook mode is enabled, Receiver is Ready and GitLab webhook test succeeds (HTTP 200).

## Phase 3: K8s Parity Mapping and Migration

**Goal:** create a complete current-state inventory, satisfy platform prerequisites, and build a target parity matrix.

**Ownership:**
- **Application teams execute Phase 2 migration using the provided `kube-migrate-*` job templates** (including inventory, translation output review, and environment-by-environment rollout). `Infra-Argo-Global` support team can be engaged as needed for assistance, issue triage, and troubleshooting.

!!!
Watch a video-based walkthrough explaining the below steps [here](https://medtronic.sharepoint.com/:v:/r/sites/WebDevModernization/Shared%20Documents/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)/Compass%20CI/Demos%20and%20Recordings/End-to-End%20Project%20Migration.mp4?csf=1&web=1&e=sleTjJ).

Relevant section:
- `1:18:00` - `1:24:00` (k8s file migration)
!!!

### Automated manifest migration with kube-migrate

Use the `kube-migrate-*` jobs as the default Phase 2 migration path.

Before running migration, ensure your app's `.gitlab-ci.yml` includes the shared kube template from `semantic-release` and defines at least one `kube-migrate-*` job.

Example:

```yaml
include:
  - file: flux-registration.yml
    project: bcp_web/devops/semantic-release
    ref: main
  - file: kube.yml
    project: bcp_web/devops/semantic-release
    ref: main

stages:
  - infrastructure

kube-migrate-dev:
  extends: [.dev, .kube-migrate]
  variables:
    SOURCE_NAMESPACE_NAME: argo-myapp-dev
    TARGET_NAMESPACE_NAME: myapp-dev
    MIGRATE_INCLUDE_SECRETS: "true"
  rules:
    - if: $MIGRATION_MODE == "true"
      when: on_success
    - if: $CI_COMMIT_BRANCH == "dev"
      when: manual
```

Run the migration in this exact order:

1. **Prepare `.gitlab-ci.yml` env and migration job config**
    - Ensure each env block has `CLUSTER_NAME`, `NAMESPACE_NAME`, `PROMOTION_LEVEL`, and `EKS_REGION`.
    - In the `kube-migrate-*` job, set `SOURCE_NAMESPACE_NAME` to the legacy namespace to export from (e.g. `argo-newton-dev`).
    - Set `TARGET_NAMESPACE_NAME` to the desired target namespace (e.g. `newton-dev`).

2. **Run `flux-register-*` first on the target branch** as outlined in [Phase 2](#phase-2-flux-registration-cluster-wiring-sops-key-and-terraformiam-foundation).
    - This creates/commits `.sops.yaml` (if missing) and wires the age key flow.
    - This step is required before secret-inclusive migrations.

3. **After `flux-register-*` has committed the `.sops.yaml` file, start a new pipeline on that branch head and run `kube-migrate-*`**
    - This explicit UI-triggered pipeline is only required for the first branch where `.sops.yaml` was just added, subsequent higher-level environments will already have the `.sops.yaml` as it is merged up the env chain.
    - In that pipeline, set `MIGRATION_MODE=true` when required by project rules.
    - Set `MIGRATE_INCLUDE_SECRETS=true` so that existing secrets are migrated and SOPS-encrypted.
    - The job exports resources from `SOURCE_NAMESPACE_NAME`, translates into `k8s/base` + `k8s/<env>` overlays, and commits generated output back to the triggering branch.

4. **Validate generated parity and structure**
    - Confirm expected base and env overlay files are present.
    - Review secret outputs to ensure `kind: Secret` files are SOPS-encrypted and no plaintext values were committed.

5. **Repeat per environment**
    - Execute the same flow for upper level environments `testing`, `staging`, and `production` as applicable.
    - If those branches were merged forward from `dev` and already contain `.sops.yaml`, you can run `kube-migrate-*` directly for that environment (no extra "create a new pipeline to pick up `.sops.yaml`" step is needed).
      - Keep per-env runs isolated; each migration run should stage only `k8s/base` plus the current env overlay.

### Exit criteria
- `kube-migrate-*` flow completed per environment in the correct order (`flux-register-*` first, then a new pipeline run for migration after `.sops.yaml` exists).
- Generated `k8s/base` + `k8s/<env>` overlays are committed to respective environment branches.
- Application deployments are successfully running in the new namespace.

## Phase 4: Cutover and decommission

**Goal:** switch production traffic and retire old Argo flow safely.

- This phase starts only after the migrated application is already running successfully in the new GitLab Dedicated repo, Compass CI pipeline, and Flux-managed namespaces.
- If the team continued making changes in the legacy `code.medtronic.com` repo while the new path was being stood up, treat final cutover as a controlled reconciliation event, not just a traffic flip.
- Progressive cutover sequence: dev → testing → staging → release → production.
- At each stage: health checks, policy checks, and runtime verification.
- Freeze old Argo deployment path after production stabilization window.

!!!
Watch a video-based walkthrough explaining the below steps [here](https://medtronic.sharepoint.com/:v:/r/sites/WebDevModernization/Shared%20Documents/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)/Compass%20CI/Demos%20and%20Recordings/End-to-End%20Project%20Migration.mp4?csf=1&web=1&e=sleTjJ).

Relevant sections:
 - `1:31:00` - `1:36:00` (cutover discussion)
!!!

### Final GitLab Dedicated cutover after migration build-out

Use this runbook once the new repo, pipeline, and target namespaces are already working and the team is ready to make GitLab Dedicated the single source of truth.

#### 1. Announce the cutover window and change control

- Name a cutover owner and a rollback owner.
- Define the final reconciliation window for all in-scope branches (`dev`, `testing`, `staging`, `release`, `main`).
- Require the application team to stop merging feature work to the legacy `code.medtronic.com` repo at the start of this window.
- Keep the legacy repo readable during the window so history can still be compared and rollback remains possible.

#### 2. Reconcile repo divergence from old GitLab to Dedicated

**Recommended approach: continuous sync via webhook during migration window (not manual at cutover)**

To avoid reconciliation work at cutover time, set up a one-way sync webhook **immediately after** Phase 0 completes and teams begin parallel work. See [Migration Repo Sync: Keeping Old and New GitLab Repos in Sync](./migration-repo-sync.md) for detailed configuration steps.

**Recommended approach:** Use the CI pipeline sync job (Option A in the how-to guide) which:
- Merges application code changes from the old repo into the new repo
- Automatically preserves `.gitlab-ci.yml`, `.releaserc.json`, `.sops.yaml`, and `k8s/` as the source of truth in the new repo
- Ensures no Compass CI work is lost during sync
- Eliminates the need for a separate migration branch

This hybrid approach means:
- Application code stays synchronized automatically (no manual reconciliation needed)
- You freely edit Compass CI files in the new repo without fear of losing work
- Final cutover simply disables the sync job (no complex merge steps)

The webhook approach ensures the Dedicated repo is never stale and eliminates manual reconciliation at cutover. Setup brings 30–45 minutes of one-time effort that saves significant time and risk during the cutover window.

**Fallback: manual reconciliation if webhook sync was not set up**

If the team did not set up continuous sync and kept committing to the old repo after import, manually reconcile that drift before final cutover.

Setup for reconciliation (run once in your working clone):

```bash
# Add the old repo as a remote for comparison
git remote add old-gitlab https://code.medtronic.com/<group>/<project>.git
git fetch old-gitlab
git fetch origin

# Verify current state
git log --left-right --graph --oneline old-gitlab/main...origin/main | head -20
git diff --stat old-gitlab/main..origin/main
```

For each branch with drift (`dev`, `testing`, `staging`, `release`, `main`):

```bash
# Example: reconcile the main branch

# Option 1: Fast-forward (if Dedicated is a parent of old repo)
git checkout main
git merge --ff-only old-gitlab/main
git push origin main

# Option 2: Merge if branches diverged
git checkout main
git merge --no-ff old-gitlab/main -m "Merge post-import changes from code.medtronic.com/$(git remote get-url old-gitlab | xargs basename)"
git push origin main

# Option 3: Cherry-pick specific commits (if only some post-import work should land)
git checkout main
git log --oneline old-gitlab/main..origin/main | tail -20  # see commits in Dedicated not yet in old
git log --oneline origin/main..old-gitlab/main | head -20  # see commits in old not yet in Dedicated
# Pick commit SHAs from the old-gitlab list that you want
git cherry-pick <commit-sha>  # repeat for each commit
git push origin main
```

Reconcile tags:

```bash
# List all tags from both sides
git tag -l | sort -V
git ls-remote --tags old-gitlab | grep -v '\^{}' | awk '{print $2}' | sed 's/refs\/tags\///' | sort -V

# Identify tags that exist only in old-gitlab and should be in Dedicated
# Pull those tags from old-gitlab and push them to Dedicated
git fetch old-gitlab refs/tags/<tag-name>:refs/tags/<tag-name>
git push origin refs/tags/<tag-name>:refs/tags/<tag-name>
```

Verification after reconciliation:

```bash
# For each in-scope branch, confirm they now match
for branch in dev testing staging release main; do
  echo "=== Branch: $branch ==="
  git log --left-right --oneline old-gitlab/$branch...origin/$branch | wc -l
done

# Should show 0 commits on each branch (fully synchronized)

# Confirm the release baseline tag is still on the intended commit
git log --oneline -1 <baseline-tag>  # Must match the production-equivalent commit
```

Do not mark migration complete until the Dedicated repo contains every intended post-import change from the legacy repo and branch/tag parity is verified.

#### 3. Rebuild and redeploy from Dedicated after reconciliation

- Run the Dedicated pipeline on the reconciled commit for each in-scope environment.
- Rebuild container images from the Dedicated repo commit that will become the source of truth.
- Reconcile Flux from the Dedicated-backed repository/branch references and confirm the intended image tags/manifests are deployed.
- Repeat smoke tests and critical-path validation because the reconciled code may differ from the code that was first used to stand up the new namespaces.

#### 4. Cut external integrations over to the new GitLab project

Move all automation and integration points that still reference the legacy GitLab project.

- Update GitLab webhooks, deploy tokens, bot tokens, and API clients to point to the Dedicated project.
- Update any Flux `GitRepository` URLs, credentials, or webhook receivers still referencing `code.medtronic.com`.
- Confirm CI template includes, package registry references, and release automation resolve from the Dedicated instance.
- Move project access requests, branch protection administration, and day-2 operational ownership to the Dedicated project.
- Disable any scheduled jobs or external automations that can still write to the old repo.

#### 5. Transition traffic, DNS, hostnames, and certificates

Traffic cutover is separate from repo cutover; both must complete before the migration is truly done.

- Confirm the new environment exposes the final intended hostname, ingress, certificate, and authentication behavior.
- Decide whether the old hostname will:
  - stay the same and be repointed to the new ingress/load balancer, or
  - redirect to a new hostname introduced by the migration
- Lower DNS TTL in advance when a DNS move is required.
- Update DNS records, ingress rules, ALB listeners, API gateway mappings, or service-mesh routing so production traffic lands on the new environment.
- Validate TLS certificates, redirect behavior, session handling, callback URLs, and any allowlisted source/target hostnames.
- Confirm downstream consumers, SSO integrations, external API clients, and firewall allowlists recognize the final hostname.

#### 6. Enforce single-writer state on the new instance

Once code and traffic are cut over, prevent new divergence immediately.

- Change the legacy repo to read-only or archive it after the rollback hold window begins.
- Disable merges, tags, scheduled pipelines, and release jobs on the old repo.
- Add a banner or README notice in the old repo that points users to the Dedicated project.
- Confirm all new commits, releases, and deployment promotions now originate only from GitLab Dedicated.

#### 7. Complete rollback and retirement controls

- Keep the legacy repo and legacy runtime path available only for the agreed rollback window.
- Define the exact rollback target commit/tag and the operator authorized to invoke it.
- When the hold window passes, remove legacy webhook endpoints, deploy credentials, and branch protections that are no longer needed.
- Archive or retire the old project and decommission legacy Argo resources only after production sign-off.

### Zero-downtime cutover strategy (recommended)

Use blue/green namespace cutover for each environment when moving from the previous Flux repository to the new onboarding model.

Approach summary:

- Keep existing workload running in legacy namespace (for example `argo-<app>-<env>`).
- Deploy migrated workload in parallel namespace (for example `<app>-<env>` without `argo-` prefix).
- Validate new workload under real traffic patterns before shifting production traffic.
- Decommission legacy namespace only after a stabilization window and rollback hold period.

#### Guardrails and decision points

- If namespace-bound external dependencies cannot be safely duplicated, use same-namespace in-place cutover with stricter maintenance controls.
- For stateful components, include data/schema compatibility plan before traffic split.
- Do not transfer Flux ownership of the same live resources between repos without overlap controls; use parallel namespaces to avoid controller contention.

---

## Team execution checklist

For each migrating application:

**Phase 0 — Dedicated Instance Readiness**
- [ ] Project imported to GitLab Dedicated instance.
- [ ] Full git history depth verified against on-prem.
- [ ] All git tags verified present and pointing to correct commits.
- [ ] Protected branches recreated on Dedicated with correct protection rules.
- [ ] All CI/CD variables (group and project level) recreated on Dedicated.

**Phase 1 — Repository Modernization **
- [ ] Net-new Compass CI files added to repo (`.releaserc.json`, `package.json`, `commitlint.config.js`, `.pre-commit-config.yaml`, `.sops.yaml`, `k8s/`).
- [ ] `.gitlab-ci.yml` rewritten with semantic-release template includes and environment blocks matching the branch→env mapping table.
- [ ] All required CI/CD variables set (group-level and project-level).
- [ ] Protected branches created (`dev`, `testing`, `staging`, `release`, `main`).
- [ ] Pre-commit hooks installed and verified locally.
- [ ] Git tags verified post-import; baseline tag created from `build-version.properties` only if no tags exist.
- [ ] Baseline tag confirmed on correct production-equivalent commit.

**Phase 2 — Flux Registration, SOPS Key and Terraform/IAM Foundation**
- [ ] Platform team: `semantic-release` share, `trivy-policies` CI_JOB_TOKEN allowlist, and `FLUX_GITOPS_REPO_RW_TOKEN` are configured for onboarding group.
- [ ] `.gitlab-ci.yml` environment blocks validated for Flux onboarding.
- [ ] Application team: app deploy token is created and mapped to `FLUX_RO_DEPLOY_TOKEN` + `FLUX_RO_DEPLOY_TOKEN_USERNAME` (with `FLUX_DEPLOY_TOKEN` only if required by legacy templates).
- [ ] Application team: `flux-register-*` SOPS bootstrap completed, `.sops.yaml` present, and `SOPS_AGE_PRIVATE_KEY` retrieved for local workflows.
- [ ] Platform/Cloud IAM team: Terraform changes merged/applied to store SOPS private key in AWS Secrets Manager for each environment.
- [ ] Platform/Cloud IAM team: Terraform role/policy updates merged/applied so Flux decryption path can read the SOPS key secret path.
- [ ] Flux resources created for all environments; reconcile status healthy.
- [ ] If webhook mode is enabled, Flux receiver/token secret and GitLab webhook are configured with matching token values.

**Phase 3 — K8s Parity Mapping and Migration**
- [ ] Cost center confirmed and provided.
- [ ] `flux-register-*` has run and committed `.sops.yaml` on the target branch.
- [ ] A subsequent pipeline (new commit SHA) was started and `kube-migrate-*` executed with `MIGRATION_MODE=true` when required by project rules.
- [ ] `kube-migrate-*` generated `k8s/base` + `k8s/<env>` overlays from the source namespace.
- [ ] Helm-managed resources (Deployment, Ingress, Service, etc.) converted to plain Kubernetes manifests; Helm labels/annotations removed.
- [ ] Legacy Argo inventory completed and signed off.
- [ ] Namespace/IAM dependency mapping complete.

**Phase 4 — Cutover and Decommission**
- [ ] Final reconciliation window scheduled; cutover and rollback owners named.
- [ ] All post-import commits/tags from the old `code.medtronic.com` repo reconciled into GitLab Dedicated.
- [ ] Dedicated pipelines rerun on reconciled commits and target environments revalidated.
- [ ] Flux/webhooks/tokens/integrations switched to the Dedicated project.
- [ ] Production hostname/DNS/ingress/certificate transition completed and validated.
- [ ] Legacy repo changed to read-only or archived after single-writer cutover.
- [ ] Parallel namespace deployed and functionally validated.
- [ ] Traffic shifted and stabilization window observed.
- [ ] Production cutover approved and executed.
- [ ] Legacy Argo path retired and old namespace decommissioned.

---

## Risks and mitigations

- GitLab Dedicated import missing tags or history
  - Mitigation: Phase 0 readiness gate requires explicit tag and history verification before any pipeline work begins; on-prem remains live as fallback.
- CI/CD variables not carried over from on-prem to Dedicated
  - Mitigation: Phase 0 checklist includes full variable recreation and smoke-test pipeline confirmation; dry-run job (`semantic-release-conditions`) validates auth before any real release.
- Teams continue committing to the old repo after the initial import, creating drift before cutover
  - Mitigation: Set up a continuous one-way sync webhook from the old repo to Dedicated immediately after Phase 0 (or use GitLab push mirroring). This keeps the Dedicated repo in sync with operational branches in real-time and eliminates manual reconciliation at cutover. If webhook sync was not configured, Phase 5 includes detailed git commands for manual remediation (merge, cherry-pick, tag sync).
- SOPS-encrypted manifests reconciled before cluster decryption key is provisioned
  - Mitigation: cluster-side age key provisioning is a hard gate in Phase 2 with end-to-end validation required before any real secrets are committed.
- Blue/green namespace infeasible for some apps (IRSA trust, singleton consumers, shared storage)
  - Mitigation: app classification gate in Phase 0/3 excludes ineligible apps from wave 1 self-service; hard singleton and stateful apps require platform team involvement and/or maintenance windows.
- Namespace/branch mismatch across files
  - Mitigation: add automated consistency validation before merge.
- Secret format and encryption drift
  - Mitigation: enforce SOPS checks in CI + pre-commit.
- Incomplete parity from legacy Argo YAML
  - Mitigation: `argo*` Argo manifests are the authoritative source (GitOps ensures no live drift); translate as a Phase 2 hard prerequisite; optional live cluster diff available for spot-checking; explicit parity matrix and staged cutover gates.
- Namespace-scoped IAM/IRSA trust broken during namespace rename
  - Mitigation: pre-map all trust policies before deploying parallel namespace; validate before traffic shift.
- Hostname, DNS, or external allowlist still points to the old environment after repo/runtime cutover
  - Mitigation: Phase 5 includes explicit DNS/hostname/certificate transition steps plus validation of SSO callbacks, downstream clients, and firewall allowlists.
- Stateful workloads (DB schema, persistent volumes) requiring coordinated data cutover
  - Mitigation: classified as out-of-wave-1 during Phase 0; treat as a separate migration work item with backward-compatible schema migrations and a controlled freeze window.

---
