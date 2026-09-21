---
label: SOPS Key Advanced Operations
icon: key
order: 10
---

# SOPS Key GitOps Setup (AWS Secrets Manager + Flux)

!!!warning
This page is now an **advanced/manual runbook** for SOPS key operations.

For standard onboarding, use `flux-register-*` jobs from the app pipeline, which will automatically provision the SOPS key.
!!!

## When You Need This Page

Use this reference when you are doing one of the following:

- Disaster recovery and cross-region failover.
- Break-glass/manual recovery when onboarding automation cannot be used.
- SOPS key rotation and validation.
- Troubleshooting ExternalSecret/SecretStore/decryption failures.

## What Automation Already Handles

`flux-register-*` automates the standard onboarding flow:

- Bootstraps `.sops.yaml` if missing.
- Creates and stores `SOPS_AGE_PRIVATE_KEY` in app CI/CD variables.
- Generates Flux registration resources (including SOPS decryption wiring).
- Optionally upserts Terraform Cloud workspace variable when TFC settings are configured.

## Terraform Cloud API token requirements (for SOPS upsert)

To allow `flux-register-*` to write the app SOPS key variable into Terraform Cloud, platform must provide Terraform Cloud API credentials at the onboarding group/team level in GitLab.

Token creation requirements:

- Create [API token](https://app.terraform.io/app/mdt-it-prod/settings/authentication-tokens) in Terraform Cloud organization: `mdt-it-prod`.
- Use organization/team token scope with permissions to read workspaces and create/update workspace variables.
  - Add a Description in the following format containing the GitLab group path to differentiate which group this token belongs to:
    - Example: `bcp_web/common group TFC API token for Compass CI automations`
- This token is used only by CI to upsert the Terraform variable key for the app SOPS private key.

Required GitLab CI variables for app groups/teams:
- `COMPASS_CI_TFC_API_TOKEN` = Terraform Cloud API token (masked/protected)
- `COMPASS_CI_TFC_ORG` = `mdt-it-prod`
- `COMPASS_CI_TFC_WORKSPACE_<CLUSTER_NAME>` = target workspace name per clusters that this group uses for their deployments
  - Example: `COMPASS_CI_TFC_WORKSPACE_TF_ARGO_DEV=aws-389242548790-bcpweb-devops-aws-it-argo-dev-mdt`
  - Example: `COMPASS_CI_TFC_WORKSPACE_TF_ARGO_PROD=aws-872019488961-bcpweb-devops-aws-it-argo-prod-mdt`

Cluster variable naming note:

- `<CLUSTER_NAME>` is normalized to uppercase underscore by automation.
- Example: `TF-argo-dev` becomes `TF_ARGO_DEV`.

If any required TFC variables are missing, `flux-register-*` continues setup but skips Terraform Cloud variable upsert.

If your app is fully using this flow and no break-glass path is needed, you can skip most manual steps below.

## Manual Fallback Checklist (Break-Glass)

Use this only for exceptions.

1. Get the SOPS private key value
   - Preferred source: app CI/CD file variable `SOPS_AGE_PRIVATE_KEY`.
   - Keep team members on the same key file to avoid cross-user encryption mismatch.

2. Ensure AWS Secrets Manager value exists and is plaintext
   - Store as plain text string, not JSON.
   - Recommended path convention: `compass-ci/<team>/<environment>/sops-age-key`.
   - CLI example:

```bash
aws secretsmanager put-secret-value \
  --secret-id compass-ci/einstein/dev/sops-age-key \
  --secret-string "$(cat ~/.sops/key-einstein.txt)"
```

3. Confirm IAM/IRSA access can read the secret
   - Role tied to the SecretStore ServiceAccount needs `secretsmanager:GetSecretValue` (+ related describe/list permissions) and KMS decrypt where required.
   - Scope role permissions to the smallest secret path set possible.

4. Confirm Flux-side resources exist in `flux-gitops`
   - `ServiceAccount` (with IRSA role annotation when AWS access is needed)
   - `SecretStore` (AWS Secrets Manager provider)
   - `ExternalSecret` (writes `sops.agekey` into target Kubernetes Secret)
   - `Kustomization.spec.decryption.secretRef.name` matches that secret name

5. Validate end-to-end

```bash
kubectl -n argo-einstein-dev get externalsecret sops-age-key-einstein-dev
kubectl -n argo-einstein-dev get secret sops-age-key-einstein-dev -o yaml
flux get kustomizations -A
```

---

## Updating SOPS Private Key in AWS Secrets Manager

If the app team needs to rotate their SOPS key (e.g., key compromise, security policy, team member offboarding), follow these steps to update the private key in AWS Secrets Manager:

**1. Get the new private key value**

Preferred: retrieve updated `SOPS_AGE_PRIVATE_KEY` from the app project's CI/CD variables after app team re-runs `flux-register-env` bootstrap/rotation flow.

Fallback: app team provides the new private key securely (legacy/manual flow).

**2. Update AWS Secrets Manager value**

**Option A: AWS Console**
1. Navigate to AWS Secrets Manager console
2. Locate the secret (e.g., `compass-ci/einstein/dev/sops-age-key`)
3. Click **"Retrieve secret value"** → **"Edit"**
4. Select the **"Plaintext"** tab
5. Replace the old private key with the new private key
6. Click **"Save"**

**Option B: AWS CLI**
```bash
# Update the secret value with the new private key
aws secretsmanager put-secret-value \
  --secret-id compass-ci/einstein/dev/sops-age-key \
  --secret-string "$(cat /path/to/new-key-einstein.txt)"
```

**3. Verify ExternalSecret sync**

The ExternalSecret will automatically detect the change and sync the new key to the cluster within the refresh interval (typically 1 hour):

```bash
# Check ExternalSecret status
kubectl -n argo-einstein-dev describe externalsecret sops-age-key-einstein-dev

# Verify the Secret was updated (check the age in metadata)
kubectl -n argo-einstein-dev get secret sops-age-key-einstein-dev -o yaml | grep 'creationTimestamp\|resourceVersion'
```

**4. Trigger immediate sync (optional)**

To force immediate sync instead of waiting for the refresh interval:

```bash
# Delete the Secret - ExternalSecret will recreate it immediately
kubectl -n argo-einstein-dev delete secret sops-age-key-einstein-dev

# Verify recreation
kubectl -n argo-einstein-dev get secret sops-age-key-einstein-dev
```

**5. Confirm Flux can decrypt with the new key**

```bash
# Trigger Flux reconciliation
flux reconcile kustomization einstein-dev -n argo-einstein-dev

# Check for decryption errors
kubectl -n argo-einstein-dev get events --sort-by='.lastTimestamp' | grep einstein-dev
```

**Important notes:**
- The app team must re-encrypt all their secrets with the new public key **before** you update AWS Secrets Manager
- If you update the private key before they re-encrypt, Flux will fail to decrypt existing secrets
- Coordinate the key rotation timing with the app team to avoid disruption

**Note:** For app teams to update their encrypted secrets in their project repositories, refer them to the [Secrets Management documentation](../how-to/secrets-management.md#updating-encrypted-secrets).

---

## DR Guidance

### Secret Replication

Terraform should include automatic replication to a secondary region:

```
  replica {
    region = "us-east-2" # Common DR region for us-east-1
  }
```

AWS Secrets Manager automatically synchronizes the SOPS private key to the replica region, ensuring the key is available for disaster recovery scenarios.

### Cluster Recovery Process

**Same-region cluster rebuild (us-east-1):**
- AWS Secrets Manager acts as the source of truth for the private key in the primary region
- Rebuilding the cluster is safe as long as the ExternalSecret and Flux Kustomization are present in Git
- ESO will automatically re-sync the key from AWS Secrets Manager into the application namespace
- No manual intervention needed - the ExternalSecret will recreate the Secret when the cluster is restored

**Cross-region DR failover (us-east-1 -> us-east-2):**

If you need to fail over to a cluster in the replica region (e.g., us-east-2):

**Update existing SecretStore**
1. Update the SecretStore region in flux-gitops:
   ```yaml
   spec:
     provider:
       aws:
         service: SecretsManager
         region: us-east-2  # Changed from us-east-1
   ```
2. Push changes to Git
3. Flux will reconcile and ESO will fetch from the replica region
4. Verify the Secret was recreated with the replicated key

**Verification after failover:**
```bash
# Confirm ExternalSecret is syncing from the replica region
kubectl -n argo-einstein-dev describe externalsecret sops-age-key-einstein-dev

# Verify the Secret exists
kubectl -n argo-einstein-dev get secret sops-age-key-einstein-dev

# Test Flux decryption
flux reconcile kustomization einstein-dev -n argo-einstein-dev
kubectl -n argo-einstein-dev get events --sort-by='.lastTimestamp' | grep einstein-dev
```

**Note:** If your IAM policy already includes permissions for both `us-east-1` and `us-east-2`, no IAM changes are needed during DR failover.

---

## Troubleshooting

### ExternalSecret not syncing

```bash
# Check ExternalSecret status
kubectl -n argo-einstein-dev describe externalsecret sops-age-key-einstein-dev

# Check SecretStore
kubectl -n argo-einstein-dev describe secretstore einstein-dev-secret-store
```

**Common issues:**
- **AccessDenied on AWS secret:** Verify the IAM role ARN in the ServiceAccount annotation matches the role in your Terraform configuration. Confirm the role has `secretsmanager:GetSecretValue` permission on the secret path.
- **Secret path mismatch:** Ensure the secret name in AWS Secrets Manager matches the `remoteRef.key` without a leading slash (e.g., `compass-ci/einstein/dev/sops-age-key`, not `/compass-ci/...`).
- **Missing ServiceAccount:** Verify the ServiceAccount exists and has the correct IRSA annotation.

### Flux decryption errors

```bash
# Check Kustomization status
kubectl -n argo-einstein-dev get kustomizations
kubectl -n argo-einstein-dev describe kustomization einstein-dev

# Check for recent decryption-related events
kubectl -n argo-einstein-dev get events --sort-by='.lastTimestamp'
```

**Common issues:**
- **Secret not found:** Verify the Secret name in `secretRef.name` matches the `target.name` in the ExternalSecret.
- **Decryption failed:** Confirm the Secret contains the correct `sops.agekey` (check `kubectl -n argo-einstein-dev get secret sops-age-key-einstein-dev -o yaml`).
- **Key mismatch:** Ensure the public key used to encrypt your SOPS files matches the age key in the Secret. The Secret should be decryptable by your private key.

### Terraform apply fails with AWS Secrets Manager name conflict

Symptoms often look like:
- Terraform apply fails creating `aws_secretsmanager_secret` because the name already exists.
- The conflicting secret may be a leftover manual secret, or a secret already scheduled for deletion.

Recommended recovery sequence:
1. Identify the conflicting secret name from Terraform output.
2. Confirm with platform/security owners that force-delete is allowed for that environment.
3. Remove the old conflicting secret (if approved), then rerun Terraform plan/apply from Terraform UI.

AWS CLI example used for force-delete:
```bash
aws secretsmanager delete-secret \
  --secret-id compass-ci/einstein/dev/sops-age-key \
  --force-delete-without-recovery
```

After deletion:
- Re-run Terraform plan/apply.
- Re-verify ExternalSecret sync and Flux decryption as shown above.

Notes:
- `--force-delete-without-recovery` is destructive. Use only when approved.
- If the secret was only scheduled for deletion and should be kept, use `restore-secret` instead of force-delete.

### ESO Controller logs

```bash
# View External Secrets Operator logs
kubectl -n external-secrets-system logs -l app.kubernetes.io/name=external-secrets --tail=100
```
