---
label: Kubernetes Validation
icon: /static/logos/kubernetes.svg
order: 40
---

# Kubernetes Validation

Your Kubernetes manifests are automatically scanned for misconfigurations and policy violations during the CI/CD pipeline. This guide explains how validation works, how to interpret results, and when/how to suppress findings.

## What is Validated?

The CI/CD pipeline uses [Trivy](https://trivy.dev/) to scan your Kubernetes YAML files against OPA (Open Policy Agent) policies. Trivy checks for:

- **Security Context Issues** - Container privilege escalation, root user requirements, capability dropping
- **Resource Configuration** - Missing resource limits/requests, liveness/readiness probes
- **Image Configuration** - Missing image tags, pull policy issues
- **Network Security** - Service exposure, network policies
- **RBAC & Permissions** - Service account token mounting, role configurations
- **Storage & Secrets** - Sensitive data in ConfigMaps instead of Secrets

## Pipeline Validation

### When Validation Runs

The `.validate-k8s-manifests` job runs during the **lint** stage of your CI/CD pipeline:

```
Code Push → GitLab CI
           ↓
        [.pre]
           ↓
        [lint] ← validate-k8s-manifests runs here
           ↓
        [test]
           ↓
        ...
```

### What Gets Scanned

All YAML files in your `k8s/` directory are scanned:

```
k8s/
├── base/
│   ├── deployment.yaml        ✓ Scanned
│   ├── service.yaml           ✓ Scanned
│   └── ingress.yaml           ✓ Scanned
├── production/
│   ├── deployment-patch.yaml  ✓ Scanned
│   └── configmap.yaml         ✓ Scanned
└── ...
```

## Understanding Test Results

### JUnit Test Output in GitLab

When validation completes, results appear in your pipeline's **Tests** tab as individual test cases:

```
Tests (42 total)
├── ✅ PASSED: Allow privileged escalation disabled (base/deployment.yaml)
├── ✅ PASSED: Running as non-root user (base/deployment.yaml)
├── ✅ PASSED: Image tag specified (base/deployment.yaml)
├── ❌ FAILED: Container security context defined (production/deployment-patch.yaml)
└── ❌ FAILED: Sensitive data in ConfigMap (production/configmap.yaml)
```

Each test case represents a single security check (e.g., KSV001, AVD-KSV-01010).

### Interpreting Failures

Each failed test shows:
- **Check ID** - Unique identifier (e.g., `KSV013`, `AVD-KSV-01010`)
- **Severity** - MEDIUM, HIGH, or CRITICAL
- **File** - Which YAML file has the issue
- **Description** - What the security issue is
- **Link** - Reference documentation

Example:

```
KSV013 (MEDIUM): Container should specify an image tag
File: base/deployment.yaml:29-64
Issue: Image tag not specified in deployment
Link: https://avd.aquasec.com/misconfig/ksv013
```

### Console Output

The pipeline job also displays a table summary:

```
Report Summary
┌──────────────────────┬────────────┬───────────────────┐
│ Target               │ Type       │ Misconfigurations │
├──────────────────────┼────────────┼───────────────────┤
│ base/deployment.yaml │ kubernetes │ 1                 │
├──────────────────────┼────────────┼───────────────────┤
│ production/configmap │ kubernetes │ 1                 │
└──────────────────────┴────────────┴───────────────────┘
```

## Policy Enforcement

### Cluster-Level Enforcement

**Important:** Many of these security policies are also enforced at the cluster level using Kyverno and other admission controllers. The recommendation is to:

1. **Resolve all findings** by implementing proper security configurations
2. **Only suppress findings** when you have a documented reason

This ensures your manifests comply with both:
- CI/CD validation checks
- Cluster admission policies

### Why Both Layers?

- **CI/CD validation** - Catches issues before they reach production
- **Cluster enforcement** - Prevents non-compliant deployments even if validation is bypassed
- **Defense in depth** - Multiple verification points ensure security

## Suppressing False Positives

Sometimes Trivy flags findings that are either:
- **Legitimate exceptions** - Your architecture requires this
- **False positives** - The check doesn't apply to your situation

Use a `.trivyignore.yaml` file to document these exceptions.

### Creating .trivyignore.yaml

Create a file at your project root (same level as `.gitlab-ci.yml`). The `.trivyignore.yaml` format allows you to:
- Specify checks to ignore by ID
- Limit ignores to specific file paths
- Document why each finding is suppressed

**Format:**

```yaml
misconfigurations:
  - id: CHECK_ID              # Trivy check identifier (e.g., KSV001, AVD-KSV-01010)
    paths:                    # File paths where check should be ignored (relative to scan root)
      - k8s/path/to/file.yaml
      - k8s/another/file.yaml
    statement: |
      Reason for suppression. Explain why this finding is safe to ignore
      in your architecture or configuration.
```

**Note:** Paths are relative to the scan root. If you run `trivy fs k8s/`, paths should be like `production/deployment-patch.yaml`, not `k8s/production/deployment-patch.yaml`.

For complete documentation on `.trivyignore.yaml` format, see [Trivy Filtering Documentation](https://trivy.dev/docs/latest/configuration/filtering/#trivyignoreyaml).

**Example 1: Kustomize-Managed Image Tags**

If Kustomize patches your image tag at deployment time, suppress KSV013:

```yaml
misconfigurations:
  # KSV013: Image tag not specified
  # Rationale: Image tag is managed by Kustomize for dynamic versioning.
  #            Kustomize patches the image tag at deployment time based on environment.
  - id: KSV013
    paths:
      - "k8s/base/deployment.yaml"
    statement: |
      Image tag is managed dynamically by Kustomize patches.
      Tag is injected at deployment time from CI/CD pipeline parameters.
```

This matches the [compass-ci-playbook/.trivyignore.yaml](https://github.com/your-org/compass-ci-playbook/blob/main/.trivyignore.yaml) pattern.

**Example 2: Non-Sensitive ConfigMap Data**

If ConfigMap contains non-sensitive configuration (not credentials):

```yaml
  # AVD-KSV-01010: ConfigMap contains sensitive data
  # Rationale: MEDTRONIC_ENVIRONMENT_DEPLOYMENT is a non-sensitive deployment
  #            environment identifier, not credentials or sensitive data.
  - id: AVD-KSV-01010
    paths:
      - "k8s/production/configmap.yaml"
    statement: |
      MEDTRONIC_ENVIRONMENT_DEPLOYMENT value is a non-sensitive deployment
      environment identifier (PRODUCTION/STAGING/DEV), not credentials.
      Appropriate for ConfigMap rather than Secret.
```

**Example 3: Patch File Limitations**

Kustomize patch files intentionally only override specific fields. Security contexts from the base are merged at deploy time:

```yaml
  # Security checks in patch files
  # Rationale: Patch files are incomplete fragments. Full security contexts are
  #            defined in base/deployment.yaml and merged by Kustomize.
  - id: KSV001
    paths:
      - "k8s/production/deployment-patch.yaml"
    statement: |
      Security context defined in base/deployment.yaml.
      Patch file contains only environment and label overrides.

  - id: KSV012
    paths:
      - "k8s/production/deployment-patch.yaml"
    statement: |
      Security context defined in base/deployment.yaml.
      Patch file contains only environment and label overrides.
```

### Format Requirements

Your `.trivyignore.yaml` file must:

1. **Be in project root** - At the same level as `.gitlab-ci.yml`
2. **Use correct YAML syntax** - Proper indentation and structure
3. **Include statement** - Explain why this finding is suppressed
4. **Specify paths** - List which files are affected

### Multiple Ignore Files Supported

You can use any of these file names:

- `.trivyignore.yaml` (recommended - structured format)
- `.trivyignore.yml` (alternative YAML extension)
- `.trivyignore` (plain text format)

The CI/CD pipeline checks in order and uses the first one found.

## Decision Tree: Fix vs. Suppress

Use this to decide whether to fix or suppress each finding:

```
Finding Detected
    ↓
Is it a security best practice?
    ├─ YES → Fix it!
    │        └─ Implement proper security configuration
    └─ NO → Is it a false positive or legitimate exception?
           ├─ FALSE POSITIVE / LEGITIMATE → Document in .trivyignore.yaml
           │                                 └─ Add statement explaining why
           └─ NEITHER → This shouldn't happen, investigate
```

## Common Findings to Fix

These findings should **always be fixed** unless there's a documented exception:

| Finding | Severity | Fix |
|---------|----------|-----|
| `KSV001` | MEDIUM | Set `securityContext.allowPrivilegeEscalation: false` |
| `KSV012` | MEDIUM | Set `securityContext.runAsNonRoot: true` |
| `KSV014` | HIGH | Set `securityContext.readOnlyRootFilesystem: true` |
| `KSV104` | MEDIUM | Add `securityContext.seccompProfile.type: RuntimeDefault` |
| `KSV118` | HIGH | Define full security context (don't use defaults) |

Example fix:

=== base/deployment.yaml

```yaml
spec:
  securityContext:
    runAsNonRoot: true
    runAsUser: 1000
    seccompProfile:
      type: RuntimeDefault
  containers:
  - name: myapp
    securityContext:
      allowPrivilegeEscalation: false
      readOnlyRootFilesystem: true
      capabilities:
        drop:
          - ALL
```

## Common Suppressions

These findings are commonly suppressed with proper justification:

### 1. Kustomize-Managed Image Tags

**Issue:** Trivy complains about missing image tags
**Reason:** Kustomize patches the tag dynamically
**Suppression:** KSV013
**Example:** See the `.trivyignore.yaml` file in your application repository root.

### 2. Non-Sensitive ConfigMap Data

**Issue:** Trivy flags ConfigMap containing non-sensitive values
**Reason:** Environment identifiers and URLs aren't credentials
**Suppression:** AVD-KSV-01010
**Example:** See the `.trivyignore.yaml` file in your application repository root.

### 3. Incomplete Patch Files

**Issue:** Kustomize patches show missing security contexts
**Reason:** Base file defines them; patch only overrides specific fields
**Suppression:** KSV001, KSV012, KSV104, KSV118
**Example:** See the `.trivyignore.yaml` file in your application repository root.

## Trivy Documentation

For detailed information about specific checks:

- [Trivy Kubernetes Checks](https://aquasecurity.github.io/trivy-docs/latest/docs/scanners/misconfiguration/kubernetes/) - All available Kubernetes checks
- [Trivy Filtering Documentation](https://trivy.dev/docs/latest/configuration/filtering/) - How to use ignore files
- [Individual Check Details](https://avd.aquasec.com/) - Search for specific check IDs (e.g., KSV013)

## Troubleshooting

### Validation Job Fails

**Issue:** `❌ k8s validation failed`

**Debug Steps:**

1. Check the pipeline output for specific failures
2. Click on failed tests in **Tests** tab to see details
3. Search for the check ID on [https://avd.aquasec.com/](https://avd.aquasec.com/)
4. Implement the recommended fix or add to `.trivyignore.yaml`

### Ignore File Not Working

**Issue:** Finding is still reported even with `.trivyignore.yaml`

**Troubleshooting:**

1. Verify `.trivyignore.yaml` is in project root
2. Check that file path matches exactly (case-sensitive)
3. Look at pipeline log for: `📋 Using ignore file: .trivyignore.yaml`
4. If not shown, verify file syntax is valid YAML

### What If Cluster Won't Accept My Deployment?

**Issue:** Deployment passes validation but cluster rejects it

**Cause:** Cluster admission controllers are stricter than CI validation
**Solution:** Fix the finding rather than suppress it - the cluster will enforce it anyway

---

## Next Steps

[!ref icon="server" text="Kubernetes Manifests"](../getting-started/kubernetes-manifests.md) - Learn manifest structure

[!ref icon="key" text="Security Context"](../how-to/service-configuration.md) - Implement security best practices

[!ref icon="book" text="Trivy Documentation"](https://trivy.dev/docs/latest/) - Deep dive into Trivy scanning

---
