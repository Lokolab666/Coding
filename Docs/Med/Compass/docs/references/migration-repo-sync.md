---
label: Migration Repo Sync via Webhook
icon: git-merge
order: 50
---

# Migration Repo Sync: Keeping Old and New GitLab Repos in Sync

During the Argo-to-Compass migration, teams often continue committing to the old `code.medtronic.com` repository while the new path (GitLab Dedicated + Compass CI) is being built out. This guide shows how to automatically sync code changes from the old GitLab instance to the new instance so that no manual reconciliation is needed at cutover time.

## Overview

Setting up continuous sync ensures:
- The Dedicated repo stays current with all **application code** changes from the old repo
- **Compass CI files** (`.gitlab-ci.yml`, `.releaserc.json`, `.sops.yaml`, `k8s/`) remain under your control in the new repo and are never overwritten
- No commits are lost during migration
- Cutover is a simple on/off switch (disable sync and you're done)
- Teams don't have to manually cherry-pick or merge changes at go-live

The sync workflow (with selective file preservation):

```
Old GitLab (code.medtronic.com)
    ↓ [push event → triggers CI job]
    ↓
Git merge: bring application code into new repo
    ↓
Restore Compass files: checkout .gitlab-ci.yml, .releaserc.json, .sops.yaml, k8s/ from new repo
    ↓
New GitLab Dedicated (with merged app code + preserved Compass files)
```

### Important: Compass Files Are Protected

The recommended CI job configuration (Option A) automatically:
- ✅ Merges `src/`, `Dockerfile`, and other application code from old repo
- ✅ Preserves `.gitlab-ci.yml`, `.releaserc.json`, `.sops.yaml`, and `k8s/` from new repo
- ✅ Handles conflicts gracefully (alerts you if app code conflicts)
- ✅ Ensures no migration work is lost

You can safely make changes to Compass files in the new repo without worrying about the sync job overwriting them.

---

## Prerequisites

### Access and Credentials

1. **Deploy token in Dedicated repo** (with push access)
   - Create in GitLab Dedicated project (`Settings → Repository → Deploy tokens`)
   - Name: `migration-sync-token`
   - Scopes: `read_repository`, `write_repository`
   - Username: `migration-sync-bot`
   - Store the password securely for later use

2. **Runner access from old repo** (already configured by the platform team)
   - Old `code.medtronic.com` project must have instance runners enabled
   - Or a tied-down project-level runner with network access to code.medtronic.com AND GitLab Dedicated
   - If using a restricted runner, ensure it can reach both GitLab instances via HTTPS

### Timeline

- Set up sync **immediately after Phase 0 completes** (after initial repo import to Dedicated)
- Leave sync **enabled through all migration phases** (1–4)
- **Disable sync at cutover** when Dedicated becomes the single source of truth
- Estimated setup time: 30–45 minutes

---

## Option A: CI Pipeline Sync (Recommended)

Use a GitLab CI pipeline in the old repo to push changes to Dedicated on every commit.

### Step 1: Create CI Pipeline in Old Repo

Add the following content to the existing `.gitlab-ci.yml`:

```yaml
# Syncs operational branches to GitLab Dedicated during migration
# Merges application code changes but preserves Compass CI files in new repo

stages:
  - sync

# Sync operational branches only (not experimental branches)
sync-to-dedicated:
  stage: sync
  image: case.artifacts.medtronic.com/ext-docker-hub-remote/alpine/git:latest
  only:
    - dev
    - testing
    - staging
    - release
    - main
  script:
    # Set up git config
    - git config --global user.email "migration-sync-bot@medtronic.com"
    - git config --global user.name "Migration Sync Bot"

    # Add remotes
    - git remote add old origin  # origin already points to old repo
    - git remote add new "https://migration-sync-bot:${DEDICATED_REPO_DEPLOY_TOKEN}@${DEDICATED_GITLAB_HOST}/${DEDICATED_PROJECT_PATH}.git"

    # Fetch both remotes
    - git fetch old ${CI_COMMIT_REF_NAME}
    - git fetch new ${CI_COMMIT_REF_NAME} || true  # May not exist on first run

    # Check out new repo's branch as the base
    - git checkout -B sync-branch new/${CI_COMMIT_REF_NAME}

    # Merge application code from old repo
    # This brings in changes to src/, Dockerfile, etc.
    - git merge --no-ff -m "Sync application code from old repo (${CI_COMMIT_SHORT_SHA})" old/${CI_COMMIT_REF_NAME} || {
        echo "⚠️  Merge conflict - inspect manually";
        exit 1;
      }

    # Restore Compass CI files from new repo (these are the source of truth)
    # This ensures we never lose migration work
    - git checkout new/${CI_COMMIT_REF_NAME} -- .gitlab-ci.yml .releaserc.json .sops.yaml k8s/

    # If Compass files don't exist yet in new repo, that's OK - skip restore
    - |
      if git ls-tree new/${CI_COMMIT_REF_NAME} -- .gitlab-ci.yml .releaserc.json .sops.yaml k8s/ > /dev/null 2>&1; then
        git checkout new/${CI_COMMIT_REF_NAME} -- .gitlab-ci.yml .releaserc.json .sops.yaml k8s/
        git add .gitlab-ci.yml .releaserc.json .sops.yaml k8s/
        git commit --amend --no-edit || true  # Amend merge commit with file restoration
      fi

    # Push merged result back to new repo
    - git push new sync-branch:${CI_COMMIT_REF_NAME}

    # Also sync all tags
    - git fetch new --tags || true
    - git push --force new --tags

  allow_failure: true  # Don't block merges on sync failure
  retry:
    max: 2
    when: runner_system_failure
```

### Step 2: Add CI/CD Variables to Old Project

In the old `code.medtronic.com` project, add these as **Protected** variables (or as CI file vars):

| Variable | Value | Notes |
|----------|-------|-------|
| `DEDICATED_GITLAB_HOST` | `medtronic.gitlab-dedicated.com` | GitLab Dedicated hostname |
| `DEDICATED_PROJECT_PATH` | `<group>/<project>` | Path to your Dedicated project (e.g. `bcp_web/common/einstein`) |
| `DEDICATED_REPO_DEPLOY_TOKEN` | `gldt_xxxxxxxxxxxx...` | Token password from Step 1 above; mark as **Masked** and **Protected** |

### Step 3: Test the Sync

1. Make a test commit on a non-protected branch:
   ```bash
   git checkout -b test-sync
   echo "# Test sync" >> README.md
   git add README.md
   git commit -m "test: verify migration sync"
   git push origin test-sync
   ```

2. Push to a protected operational branch:
   ```bash
   git checkout dev
   git pull origin dev
   echo "# Sync test on dev" >> MIGRATION_NOTES.md
   git add MIGRATION_NOTES.md
   git commit -m "docs: test sync to dedicated"
   git push origin dev
   ```

3. Check the old repo's CI pipeline output for the `sync-to-dedicated` job.

4. Verify the commit appears in the Dedicated repo:
   ```bash
   git clone https://medtronic.gitlab-dedicated.com/bcp_web/common/einstein.git --depth=1
   cd einstein
   git log --oneline dev | head -5  # Should show your test commit
   ```

### Step 4: Operational Monitoring

Create a scheduled job to validate sync health (runs daily):

```yaml
# Validate sync is working
validate-sync-health:
  stage: sync
  image: case.artifacts.medtronic.com/ext-docker-hub-remote/alpine/git:latest
  script:
    - git remote add old origin
    - git remote add new "https://migration-sync-bot:${DEDICATED_REPO_DEPLOY_TOKEN}@${DEDICATED_GITLAB_HOST}/${DEDICATED_PROJECT_PATH}.git"
    - git fetch old
    - git fetch new

    # Compare branch heads
    - |
      for branch in dev testing staging release main; do
        old_head=$(git rev-parse old/$branch 2>/dev/null || echo "MISSING")
        new_head=$(git rev-parse new/$branch 2>/dev/null || echo "MISSING")
        echo "Branch: $branch | Old: ${old_head:0:8} | New: ${new_head:0:8}"
        if [ "$old_head" != "$new_head" ]; then
          echo "❌ DIVERGED: $branch"
          exit 1
        fi
      done
    - echo "✅ All branches synchronized"

  only:
    - schedules
  tags:
    - docker
  allow_failure: false
```

Execute this as a pipeline schedule: `Settings → Pipelines → Schedules` → daily at 01:00 UTC.

---

## Option B: Manual Sync Script (On-Demand Fallback)

If the CI pipeline approach is not feasible, use this script to run sync manually on demand:

```bash
#!/bin/bash
# manual-sync.sh — one-time sync for operational branches

set -e

OLD_REPO="https://user:token@code.medtronic.com/group/project.git"
NEW_REPO="https://migration-sync-bot:${DEDICATED_TOKEN}@medtronic.gitlab-dedicated.com/bcp_web/common/einstein.git"
BRANCHES=("dev" "testing" "staging" "release" "main")

# Clone old repo and add new remote
git clone --bare "$OLD_REPO" sync-temp.git
cd sync-temp.git

# Add new repo as remote
git remote add new "$NEW_REPO"

# Sync each branch
for branch in "${BRANCHES[@]}"; do
  echo "Syncing branch: $branch"
  git push new "$branch:$branch" --force || echo "Branch $branch may not exist in old repo"
done

# Sync all tags
echo "Syncing tags..."
git push new --tags --force

cd ..
rm -rf sync-temp.git

echo "✅ Sync complete"
```

Run this script:
```bash
export DEDICATED_TOKEN="gldt_xxxxxxxxxxxx..."
chmod +x manual-sync.sh
./manual-sync.sh
```

---

## Disabling Sync at Cutover

Once you've completed Phase 4 and are ready for final cutover:

### Option A (CI Pipeline)

1. Update the sync job to only run on a manual trigger (not on every push):
   ```yaml
   sync-to-dedicated:
     only:
       - web  # Manual pipeline trigger only
   ```

2. Or disable the job entirely:
   ```yaml
   sync-to-dedicated:
     when: manual  # Requires explicit trigger
   ```

3. Make these changes **only after** final reconciliation is complete.

### Option B (Manual Script)

- After cutover, do not run the script again.
- Archive or delete the script from your workspace.

---

## Monitoring and Troubleshooting

### Health Check: Compare Branch Heads

Run periodically to confirm sync is working:

```bash
git clone --depth=1 https://code.medtronic.com/group/project.git old-repo
git clone --depth=1 https://medtronic.gitlab-dedicated.com/bcp_web/common/einstein.git new-repo

cd old-repo
for branch in dev testing staging release main; do
  old_sha=$(git rev-parse origin/$branch 2>/dev/null | cut -c1-8)
  new_sha=$(cd ../new-repo && git rev-parse origin/$branch 2>/dev/null | cut -c1-8)

  status="✅"
  [ "$old_sha" != "$new_sha" ] && status="❌"

  echo "$status Branch $branch: old=$old_sha new=$new_sha"
done
```

Expected output:
```
✅ Branch dev: old=a1b2c3d4 new=a1b2c3d4
✅ Branch testing: old=e5f6g7h8 new=e5f6g7h8
✅ Branch staging: old=i9j0k1l2 new=i9j0k1l2
✅ Branch release: old=m3n4o5p6 new=m3n4o5p6
✅ Branch main: old=q7r8s9t0 new=q7r8s9t0
```

### Common Issues

#### Sync job fails with "permission denied"

- Verify the deploy token hasn't expired
- Check the token has `write_repository` scope
- Confirm the token password is correctly masked in CI/CD variables

#### `DIVERGED: branch` in health check

- The branch in the old repo has a commit not in the new repo
- This usually means the sync job didn't run or failed
- Manually trigger a sync (run CI pipeline or use manual script)
- Re-run health check

#### "Repository not found" or 404

- Verify `DEDICATED_PROJECT_PATH` is correct (format: `group/project`, not `group/subgroup/project`)
- Confirm the deploy token is from the **destination** Dedicated project, not the old repo
- Check network connectivity to `medtronic.gitlab-dedicated.com`

#### Tags not syncing

- Tags are synced by `git push new --tags --force`
- Verify the tag exists locally: `git tag -l | grep <tag-name>`
- Force-push the tag: `git push new <tag>:refs/tags/<tag> --force`

### Changes made in new repo are getting overwritten by sync

With the recommended CI job configuration (Option A above), this should **not** happen. The job explicitly preserves `.gitlab-ci.yml`, `.releaserc.json`, `.sops.yaml`, and `k8s/` by checking them out from the new repo after merging application code.

**If you're seeing Compass files get overwritten:**

1. Verify you're using the latest job script from Step 1 (includes the `git checkout new/... --` restore lines).
2. Check that the file restore logic ran: look for log output like `"Restore Compass CI files from new repo"`.
3. If the restore is failing, manually verify those files exist in the new repo at `new/${CI_COMMIT_REF_NAME}`.

**If you choose to use a separate migration branch:**

Keep the default force-push sync on operational branches, but do all Compass CI migration work on a separate branch:

```bash
# Create migration branch based on new repo's current main
git checkout -b migration/compass-ization
# Now add all Compass files: .gitlab-ci.yml, .releaserc.json, k8s/, etc.
git push origin migration/compass-ization
```

**During migration phases 1–4:**
- Old repo continues pushing → syncs to new repo's `main`, `dev`, etc. (merges app code, preserves Compass files)
- You can still maintain a separate migration branch if you prefer extra isolation

**At final cutover:**
- Compass files are already in operational branches (protected by the job), so no merge step needed
- Just disable the sync

**Recommendation:** Use the CI pipeline approach (Option A) with selective file restoration. It's automatic, requires no branch management, and keeps your workflow clean.

---

## Audit and Compliance

### Logging

- **Old repo:** CI pipeline logs are visible in the project (`CI/CD → Pipelines`)
  - Logs include remote URLs (with tokens masked) and push results
  - Logs are retained per GitLab retention policy
- **Dedicated repo:** Access logs in project activity (`Project → Activity → Audit events`)
  - Shows that `migration-sync-bot` pushed changes
  - Timestamp and commit SHAs recorded

### Access Control

- Deploy token is **project-scoped** and can be revoked anytime
- Create the token **only during migration** and delete it after cutover
- Use a **dedicated bot user** (`migration-sync-bot`) separate from personal accounts
- Token has minimum required scopes: `read_repository` + `write_repository` only

### Audit Fields

```json
{
  "actor": "migration-sync-bot (deploy token)",
  "action": "push",
  "target": "GitLab Dedicated project",
  "timestamp": "2024-03-24T14:23:15Z",
  "scope": "Refs: dev, testing, staging, release, main",
  "status": "success",
  "log_location": "Old repo → CI/CD → Pipelines → <job-id>"
}
```

---

## Cutover Checklist

Before disabling sync:

- [ ] Sync validation job has been running daily with `✅ All branches synchronized`
- [ ] No pending commits in old repo that haven't synced
- [ ] Health check confirms all operational branches match between old and new
- [ ] **Compass files protected:** `.gitlab-ci.yml`, `.releaserc.json`, `.sops.yaml`, `k8s/` are preserved in new repo and not overwritten by sync
- [ ] Baseline tag (for semantic-release) has been synced and verified on Dedicated
- [ ] Phase 4 repo modernization (`.gitlab-ci.yml`, `.releaserc.json`, etc.) is complete in Dedicated
- [ ] First Dedicated pipeline has been triggered and is passing
- [ ] New pipeline is deploying successfully to environments
- [ ] Rollback owner has signed off on cutover plan
- [ ] Change window is scheduled

When ready:
- [ ] Disable sync (update CI config to manual-only, or remove mirror)
- [ ] Verify no auto-syncs occur in next 5 minutes
- [ ] Confirm Dedicated is now the single writer
- [ ] Delete the deploy token after hold window (typically 48–72 hours post-cutover)

---

## References

- [GitLab Dedicated Documentation](https://docs.gitlab.com/ee/subscriptions/gitlab_dedicated/)
- [GitLab Deploy Tokens](https://docs.gitlab.com/ee/user/project/deploy_tokens/)
- [GitLab CI Pipeline Syntax](https://docs.gitlab.com/ee/ci/yaml/)
- [Argo to Compass Migration Plan - Phase 5 Reconciliation](../references/argo-to-compass-migration-plan.md#2-reconcile-repo-divergence-from-old-gitlab-to-dedicated)
