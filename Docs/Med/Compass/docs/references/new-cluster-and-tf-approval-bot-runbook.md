---
label: New Cluster and TF Approval Bot Runbook
icon: server
order: 140
---

# New Cluster Support and Terraform MR Approval Bot Runbook

Use this runbook when:
- onboarding support for a new shared cluster
- enabling automated Terraform MR approval and auto-merge

## Part 1: Add Support for a New Cluster

Use the onboarding checklist in [../platform-inventory/index.md](../platform-inventory/index.md#adding-a-new-cluster).

Detailed bootstrap mechanics, ordering, and prerequisites are maintained in [flux-gitops/base/sources/Cluster-Onboarding.md](https://medtronic.gitlab-dedicated.com/bcp_web/devops/fluxconfigs/flux-gitops/-/blob/main/base/sources/Cluster-Onboarding.md?ref_type=heads).

This runbook intentionally does not duplicate the onboarding checklist so updates are made in one place.

## Part 2: Enable TF MR Auto-Approval and Auto-Merge Bot (optional)

This section documents the bot setup per Terraform project. This setup is optional but can be useful for automations once they are confirmed to reliably and accurately produce expected changes so that standard, low-risk infrastructure changes can be automatically approved and applied.

### 1. Create a Service Account in the Terraform project

In each Terraform repo project:
- Settings -> Service Accounts
- Create service account with a clear name, for example:
  - svc-tf-merge-approval-bot

Important:
- the human-friendly name can be similar across projects
- the real GitLab identity is project-scoped and unique

### 2. Grant project membership to the service account

Grant the service account direct project access in that Terraform project.
Recommended role:
- Maintainer

Why Maintainer:
- needed to reliably approve and arm merge in repositories with strict branch and MR policies

### 3. Create an access token for the service account

Create a token associated with that service account.
Recommended scope:
- api

Store token securely. Use masked CI variables where appropriate.

### 4. Set project CI variable `TF_MR_APPROVER_TOKEN`

In the same Terraform project, set:
- TF_MR_APPROVER_TOKEN = <service account token>

This token is consumed by the project-local MR approval job.

### 5. Add the service account identity to `CODEOWNERS`

If code owner approval is required:
- add the actual GitLab username handle to `CODEOWNERS`
- for service accounts this is typically the generated handle, for example:
  - @service_account_project_<projectId>_<hash>

Do not assume the display name is the `CODEOWNERS` handle.

### 6. Ensure branch rule requires `CODEOWNERS` approval

In the Terraform project branch rules (main):
- enable Require code owner approval

### 7. Add or update project's MR auto-approve job

In the Terraform project's `.gitlab-ci.yml`, add a job that:
- runs on merge_request_event
- approves the MR with `TF_MR_APPROVER_TOKEN`
- then attempts to arm auto-merge
- retries auto-merge arming a few times to handle mergeability timing delays

Recommended behavior:
- attempt both API styles for compatibility:
  - merge_when_pipeline_succeeds=true
  - auto_merge=true
- retry with delay before failing

## Common Failure Modes

### Approval works but auto-merge not armed

Likely cause:
- auto-merge call executed before MR became mergeable

Fix:
- keep retry loop in the approval job

### `CODEOWNERS` shows inaccessible owner

Likely causes:
- wrong handle used (display name instead of username)
- service account missing project membership
- insufficient role

Fix:
- use generated service account username
- grant direct project access (Maintainer recommended)

### MR job not running

Check:
- pipeline source is merge_request_event
- project CI config is actually loading local `.gitlab-ci.yml`
- no compliance policy is overriding pipeline graph

## Operating Model Recommendation

- use one service account per Terraform project trust boundary
- keep `TF_MR_APPROVER_TOKEN` project-scoped
- keep `CODEOWNERS` entries path-scoped where possible
- rotate service account tokens on a scheduled cadence
