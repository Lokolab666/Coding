# Adding a Flux Tenant Entry Point ("Harness") for a Team in an Existing Namespace

## 1. Purpose

This procedure describes how to add a Flux tenant entry point, informally called a **Flux harness**, for a customer team in an **existing Kubernetes namespace** on an Atlas/Tanzu cluster.

The harness establishes a controlled GitOps delegation boundary. It allows a team-owned Flux `Kustomization` to reconcile resources from a team-owned Git repository while using a dedicated Kubernetes `ServiceAccount` whose permissions are restricted to namespaces owned by that team.

This is an onboarding operation. It is not required every time the team requests another namespace.

## 2. Scope

Use this procedure only when all of the following are true:

- The target cluster already exists and is managed through the Atlas/Tanzu cluster-management Git repository.
- The namespace already exists under `cluster/customer-components/<namespace>/`.
- The namespace is intended to be the team's designated Flux entry-point namespace.
- The team does not already have a Flux reconciler `ServiceAccount` and matching `RBACDefinition` on the same cluster.
- The team requires Flux to reconcile configuration from its own Git repository.

Do not use this procedure when:

- The team is already onboarded to the cluster and only needs an additional namespace. In that case, create or update only the namespace resource as required.
- The requested namespace is not the team's designated entry-point namespace.
- The team needs to create cluster-scoped Kubernetes resources. Cluster-scoped resources remain under Atlas platform ownership and require a separate platform change.
- The request is only for ingress, egress, or another network-policy change.

## 3. Core design rule

A team requires **one Flux entry point per team per cluster**, not one entry point per namespace.

The entry-point namespace contains the team's Flux reconciliation identity and source configuration. Additional namespaces owned by the same team are selected through the team ownership label and receive permissions through RBAC Manager-generated bindings.

Before making any change, search the target cluster repository for an existing reconciler `ServiceAccount`, `RBACDefinition`, `GitRepository`, or Flux `Kustomization` for the team. Creating a second harness can create duplicate reconciliation, ambiguous ownership, and unnecessarily broad access.

## 4. Terminology

### Entry-point namespace

The namespace selected by the customer team to host its Flux source and reconciliation resources. It may also be called the team's top-level or system namespace. It should not be described as a Kubernetes root namespace because Kubernetes does not provide namespace hierarchy in that sense.

### Harness

An informal name for the complete set of resources that connects the customer's Git repository to the cluster through Flux and applies the team's permission boundary.

### Reconciler ServiceAccount

The Kubernetes identity used by the team's Flux `Kustomization`. The `Kustomization` must explicitly reference this identity rather than a platform-level or controller-level service account.

### RBACDefinition

A cluster-scoped custom resource managed by RBAC Manager. It dynamically creates the required `ClusterRoleBinding` and namespace-level `RoleBinding` resources for the reconciler `ServiceAccount`.

### GitRepository source

A Flux source object that identifies the customer's repository and selected Git revision. The source may reference an encrypted authentication secret.

### Flux Kustomization

A Flux reconciliation object that references the `GitRepository`, selects a path in that repository, defines reconciliation behavior, and runs under the dedicated reconciler `ServiceAccount`.

## 5. Architecture and repository placement

The Atlas/Tanzu cluster-management repository is the GitOps source of truth. Flux reconciles the `customer-components` tree into the cluster.

A typical layout for an existing entry-point namespace is:

```text
cluster/
└── customer-components/
    ├── cluster-scope/
    │   └── <team>-reconciler-rbacdefinition.yaml
    └── <entry-point-namespace>/
        ├── namespace.yaml                 # Already exists
        ├── serviceaccount.yaml            # Atlas/platform-managed
        ├── gitrepository.yaml             # Customer/team-owned values
        ├── kustomization.yaml              # Customer/team-owned values
        └── <git-auth-secret>.yaml          # Encrypted; customer/team-owned
```

File names vary between existing implementations. Preserve the naming and API conventions used by the target cluster repository.

The `RBACDefinition` belongs in `cluster/customer-components/cluster-scope/` because it is cluster-scoped. The `ServiceAccount`, `GitRepository`, authentication secret, and Flux `Kustomization` belong in the existing entry-point namespace folder.

## 6. Responsibility boundary

### Atlas/platform support responsibilities

- Confirm that the selected namespace is the correct team entry point.
- Confirm that no harness already exists for the team on the target cluster.
- Validate the namespace ownership label.
- Create or review the dedicated reconciler `ServiceAccount`.
- Create or review the matching cluster-scoped `RBACDefinition`.
- Reject any configuration that uses an excessively privileged platform or Flux controller identity.
- Review the customer-authored Flux resources before merge.
- Protect cluster-scoped resources and the cluster-level control plane.
- Support failures caused by Flux, RBAC Manager, shared platform components, or the Kubernetes cluster.

### Customer team responsibilities

- Select the entry-point namespace.
- Provide and own the Git repository URL.
- Provide and own the Git branch, tag, semver constraint, or other revision selector.
- Provide and own the repository path reconciled by Flux.
- Provide the required Git authentication material using the approved encrypted-secret pattern.
- Define the `GitRepository` and Flux `Kustomization` settings.
- Maintain the manifests stored in the customer repository.
- Resolve application-level deployment or runtime problems that are not caused by the Atlas platform.

The platform team may assist with the initial merge request, but repository content, source revision, path, and application health remain the customer's responsibility.

## 7. Required inputs

Collect and validate the following before implementation:

| Input | Description |
|---|---|
| Target cluster | Cluster-management repository to be changed. |
| Entry-point namespace | Existing namespace selected to host the harness. |
| Team identifier | Exact value used by the namespace ownership label, normally `k8s.mdtcloud.io/team-name`. |
| Reconciler ServiceAccount name | Prefer the established repository naming convention, commonly `<namespace>-reconciler`. |
| Existing harness status | Confirmation that no reconciler and `RBACDefinition` already exist for this team on the cluster. |
| Customer Git URL | Repository to be polled by the Flux `GitRepository`. |
| Git revision | Branch, tag, semver constraint, or commit policy approved by the customer. |
| Repository path | Directory containing the manifests Flux must reconcile. |
| Authentication secret | Name and approved encrypted-secret implementation used to access the repository. |
| Reconciliation settings | Interval, prune behavior, timeout, health checks, and dependencies, following the existing platform pattern. |
| Reviewer | Independent Atlas approver. During the KT transition, Wesley was the designated reviewer. |

Do not infer customer repository values. Obtain them from the customer or from an existing, approved deployment for the same team.

## 8. Pre-change validation

### 8.1 Confirm the namespace exists

Verify that the namespace folder and namespace manifest are already present:

```text
cluster/customer-components/<entry-point-namespace>/namespace.yaml
```

Do not recreate or replace the namespace manifest unless the request explicitly includes a namespace correction.

### 8.2 Confirm team ownership

Verify that the namespace has the correct team label:

```yaml
metadata:
  labels:
    k8s.mdtcloud.io/team-name: <team-identifier>
```

The exact label key must be copied from a working namespace in the same repository. A wrong team label prevents the expected namespace-level RBAC bindings from being generated or applies them to the wrong tenant boundary.

### 8.3 Confirm the harness does not already exist

Search the repository for the team identifier, reconciler name, and namespace name. Check at least:

```text
cluster/customer-components/cluster-scope/
cluster/customer-components/*/
```

Look for:

- A reconciler `ServiceAccount`.
- An `RBACDefinition` that references the team or service account.
- A Flux `GitRepository` for the team.
- A Flux `Kustomization` using the team's reconciler service account.

If a valid harness already exists on that cluster, stop. Do not create another one. Additional team namespaces should use the existing delegation model.

### 8.4 Select a known-good reference

Use references in this order:

1. The same team on another Atlas/Tanzu cluster.
2. Another team on the same cluster using the current naming and CRD versions.
3. Another current Atlas/Tanzu cluster with the same platform release pattern.

Do not build the `RBACDefinition` schema from memory. Copy a working current resource and modify only the fields that are tenant-specific.

## 9. Implementation procedure

### Step 1: Synchronize the repository and create a branch

Start from the latest `main` branch:

```bash
git checkout main
git pull --ff-only
git checkout -b onboard/<team>-flux-entry-point
```

Use Linux line endings. Avoid committing Windows CRLF changes to unrelated files.

### Step 2: Add the reconciler ServiceAccount

Create the service account in the existing entry-point namespace folder.

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: <entry-point-namespace>-reconciler
  namespace: <entry-point-namespace>
```

Use the naming convention already established in the repository. The service account must be dedicated to the customer team and must not be replaced with a Flux controller service account or another platform-level identity.

### Step 3: Add the matching RBACDefinition

Create the `RBACDefinition` in:

```text
cluster/customer-components/cluster-scope/
```

Use a working `RBACDefinition` as the template. The required semantics are:

- The subject is the reconciler `ServiceAccount` created in the entry-point namespace.
- The resource selects namespaces owned by the team using the team ownership label.
- Namespace-level administrative permissions are granted only inside matching team namespaces.
- Read or view access required to inspect Flux resources is provided according to the current platform pattern.
- The service account is not granted unrestricted cluster-scoped write access.

Illustrative structure only:

```yaml
apiVersion: <copy-from-current-repository>
kind: RBACDefinition
metadata:
  name: <team>-reconciler
spec:
  # Copy the current Atlas RBAC Manager schema.
  # Subject: ServiceAccount/<entry-point-namespace>-reconciler
  # Namespace selector: k8s.mdtcloud.io/team-name=<team-identifier>
  # Bindings: current approved Flux visibility and namespace-local permissions
```

Do not convert a namespace-scoped `RoleBinding` that references the `cluster-admin` `ClusterRole` into a global `ClusterRoleBinding`. A `RoleBinding` can reference a `ClusterRole` while still limiting the permissions to one namespace. A global `ClusterRoleBinding` would cross the intended tenant boundary.

The service account and `RBACDefinition` are a required pair. A service account without the matching RBAC definition has no useful reconciliation permissions.

### Step 4: Add or review the encrypted Git credential resource

The customer should supply the approved Git authentication resource. Store credentials only through the existing encrypted pattern, such as the SOPS-based secret workflow used by the platform.

Requirements:

- No plaintext token, password, SSH private key, or deploy key is committed.
- The secret is created in the entry-point namespace.
- The secret name matches the `GitRepository.spec.secretRef` value.
- The encrypted file can be decrypted by the cluster's established secret-management process.

The transcripts do not expose the exact customer-secret schema. Copy the current implementation used by another onboarded team.

### Step 5: Add or review the Flux GitRepository source

The `GitRepository` defines the external source of truth for the customer team.

Illustrative structure:

```yaml
apiVersion: source.toolkit.fluxcd.io/<copy-current-version>
kind: GitRepository
metadata:
  name: <team-source-name>
  namespace: <entry-point-namespace>
spec:
  interval: <approved-interval>
  url: <customer-git-url>
  ref:
    branch: <customer-branch>
  secretRef:
    name: <git-auth-secret>
```

The customer controls the repository URL, revision selector, and related source settings. The Atlas reviewer validates that the values are syntactically valid, use the approved authentication pattern, and do not create an obvious security or operational risk.

Do not assume `main` is the correct customer branch. Existing Atlas platform components may use environment-specific branches, and customer repositories may use a branch, tag, or constraint selected by the team.

### Step 6: Add or review the Flux Kustomization

The Flux `Kustomization` connects the `GitRepository` to the target manifests and enforces the service-account boundary.

Illustrative structure:

```yaml
apiVersion: kustomize.toolkit.fluxcd.io/<copy-current-version>
kind: Kustomization
metadata:
  name: <team-reconciliation-name>
  namespace: <entry-point-namespace>
spec:
  interval: <approved-interval>
  sourceRef:
    kind: GitRepository
    name: <team-source-name>
  path: <customer-repository-path>
  serviceAccountName: <entry-point-namespace>-reconciler
  prune: <approved-value>
```

Mandatory review points:

- `sourceRef.name` matches the `GitRepository` name.
- `path` exists in the selected Git revision.
- `serviceAccountName` references the dedicated team reconciler.
- The object does not use a platform controller service account.
- Reconciliation settings follow a working platform example.
- The manifests under the selected path are intended for namespaces owned by the team.
- Any cluster-scoped resources are removed or routed through a separate Atlas-owned change.

One recording showed a customer configuration reconciling every five minutes. Treat that as an example, not a mandatory platform default.

### Step 7: Perform static validation

Review the complete change before committing:

```bash
git status
git diff --check
git diff
```

Confirm:

- Only the expected files changed.
- The service account namespace and name are correct.
- The `RBACDefinition` subject matches the service account exactly.
- The team selector matches the namespace label exactly.
- The `GitRepository` and secret references match.
- The `Kustomization` source, path, and service account references match.
- No secret material is visible in plaintext.
- No broad or root-level service account is used.
- No duplicate harness is being added.
- YAML indentation and repository naming conventions are preserved.

Use any repository-provided lint, schema-validation, or CI commands before opening the merge request.

### Step 8: Commit and open the merge request

Use a descriptive commit and branch:

```bash
git add cluster/customer-components
git commit -m "Add <team> Flux entry point"
git push --set-upstream origin onboard/<team>-flux-entry-point
```

The merge request should include:

- Customer/team name.
- Target cluster.
- Entry-point namespace.
- Customer request or ticket reference.
- Repository URL, revision type, and path without exposing credentials.
- Confirmation that no existing harness was found.
- Validation performed.
- Expected files and resources added.

Assign the author as the assignee and an independent Atlas maintainer as reviewer. The KT sessions recommend peer review by someone other than the committer. During the initial transition, changes were assigned to Wesley for review.

Squashing commits and deleting the source branch after merge are recommended repository housekeeping practices when allowed by the project settings.

### Step 9: Merge and allow Flux to reconcile

After approval and merge to `main`, Flux automatically reconciles the cluster-management repository. Do not manually apply the same manifests with `kubectl` as the normal deployment path because that creates drift from Git.

## 10. Post-merge verification

### 10.1 Verify the Kubernetes and Flux objects

Example commands:

```bash
kubectl -n <entry-point-namespace> get serviceaccount <entry-point-namespace>-reconciler
kubectl -n <entry-point-namespace> get gitrepository
kubectl -n <entry-point-namespace> get kustomization
kubectl -n <entry-point-namespace> describe gitrepository <team-source-name>
kubectl -n <entry-point-namespace> describe kustomization <team-reconciliation-name>
```

Flux CLI checks:

```bash
flux get sources git -A
flux get kustomizations -A
flux tree kustomization flux-system
```

The exact root `Kustomization` name can differ by cluster. Use the cluster's current Flux tree.

### 10.2 Verify generated RBAC bindings

Confirm that RBAC Manager created the expected bindings for the service account:

```bash
kubectl get rolebinding -A -o wide | grep '<entry-point-namespace>-reconciler'
kubectl get clusterrolebinding -o wide | grep '<entry-point-namespace>-reconciler'
```

Expected behavior:

- The reconciler can inspect the Flux resources required by the platform pattern.
- The reconciler can manage resources in namespaces carrying the team's ownership label.
- The reconciler cannot create namespaces, modify CRDs, or perform other unauthorized cluster-scoped changes.

### 10.3 Verify readiness

The `GitRepository` and Flux `Kustomization` should report `Ready=True`. Review status conditions and events rather than relying only on the object existing.

The customer should confirm that the intended repository path is being reconciled and that the resulting application resources are correct.

## 11. Acceptance criteria

The change is complete when all of the following are true:

- The team has exactly one Flux harness on the target cluster.
- The harness is located in the approved entry-point namespace.
- The namespace has the correct team ownership label.
- The dedicated reconciler `ServiceAccount` exists.
- A matching cluster-scoped `RBACDefinition` exists.
- RBAC Manager generated the expected restricted bindings.
- The Flux `Kustomization` explicitly uses the dedicated service account.
- The `GitRepository` references the customer-approved repository and revision.
- The repository authentication secret is encrypted and resolves successfully.
- The selected path exists and reconciles successfully.
- `GitRepository` and `Kustomization` report `Ready=True`.
- The reconciler cannot perform unauthorized cluster-scoped writes.
- The merge request was reviewed by someone other than the committer.
- The customer confirms that the expected workload configuration was deployed.

## 12. Troubleshooting

| Symptom | Likely cause | Action |
|---|---|---|
| Service account exists but reconciliation is forbidden | Missing `RBACDefinition`, subject mismatch, wrong team selector, or RBAC Manager has not reconciled | Compare the `RBACDefinition` with a working team; verify service-account name, namespace, and team label. |
| `GitRepository` is not ready | Invalid repository URL, revision, secret reference, or credentials | Describe the source, inspect events, validate the encrypted secret and Git revision with the customer. |
| `Kustomization` reports source not found | `sourceRef` name or namespace mismatch | Align the `Kustomization.sourceRef` with the `GitRepository`. |
| `Kustomization` reports path not found | Customer path is wrong for the selected revision | Customer must correct the path or revision. |
| Reconciliation fails with permission errors in one namespace | Namespace is missing or has the wrong team ownership label | Correct the label through the cluster-management repository and verify generated bindings. |
| Reconciliation attempts cluster-scoped resources | Customer manifests contain namespaces, CRDs, cluster roles, or other cluster-scoped objects | Remove those objects from the customer path and route them through the Atlas platform change process. |
| Two Flux objects deploy the same resources | Duplicate harness or overlapping repository paths | Stop one reconciliation path and revert the duplicate configuration through Git. |
| Flux objects are ready but the application is unhealthy | Customer manifest or application problem | Return ownership to the customer unless a shared platform component is failing. |
| Change exists in the branch but not in the cluster | Merge request is not merged, wrong cluster repository was changed, or parent Flux reconciliation is unhealthy | Confirm merge to `main`, verify the target repository, and inspect the Flux tree. |
| Wrong branch, tag, or path was selected | Customer-provided source configuration is incorrect | Customer supplies the correction; update it through a reviewed merge request. |

## 13. Security and governance controls

- Never use a Flux controller's own service account for customer reconciliation.
- Never grant the customer reconciler global `cluster-admin` through a `ClusterRoleBinding`.
- Namespace-local access must be selected through the approved team label.
- Do not commit plaintext credentials.
- Do not accept customer-authored cluster-scoped resources through the tenant Git path.
- Keep Git as the source of truth. Correct configuration through merge requests rather than manual persistent changes in the cluster.
- Require independent review because merging to `main` causes Flux to apply the change automatically.
- Treat repository URL, revision, path, and application manifests as customer-owned configuration.
- Treat Flux controllers, RBAC Manager, shared components, cluster-scoped policy, and cluster health as Atlas-owned platform responsibilities.

## 14. Rollback

Use Git to roll back the change:

1. Revert the merge commit or submit a corrective merge request.
2. Review the rollback independently.
3. Merge the rollback to `main`.
4. Allow Flux to reconcile the desired state.
5. Verify that only the intended resources were removed or restored.

Do not delete resources manually as the primary rollback method. Manual deletion without a Git change is temporary because Flux can recreate the resources and it leaves the repository inconsistent with the cluster.

## 15. Source references

This knowledge document was derived from the following KT transcriptions led by Wesley Merrick:

1. **CapGemini ATLAS Tanzu KT-20251103_193223 – Meeting Recording** (`20251103_193223.docx`), especially approximately 43:00–55:43. This session introduces the cluster-management repository, `customer-components`, the entry-point namespace, reconciler `ServiceAccount`, cluster-scoped `RBACDefinition`, one-harness-per-team-per-cluster rule, Git branch and merge-request workflow, and automatic Flux reconciliation after merge.

2. **Capgemini Atlas KT Session-20251119_193528 – Meeting Recording** (`20251119_193528.docx`), especially approximately 41:21–46:50. This session clarifies the onboarding responsibility split: Atlas manages or validates the namespace, service account, and RBAC boundary; the customer provides the encrypted secret, Flux `GitRepository`, repository revision, path, and reconciliation configuration. It also states that an overprivileged controller or root-level service account must not be accepted.

3. **Capgemini Atlas KT Session-20251120_193200 – Meeting Recording** (`20251120_193200.docx`), especially approximately 5:25–10:42, 17:03–33:42, and 35:26–40:22. This session confirms that a team needs only one Flux entry point per cluster, explains how Flux reconciles the `customer-components` tree, and defines the tenant boundary: full permissions only in namespaces owned by the team, no unauthorized cluster-scoped actions, and customer ownership of the repository content and application health.

## 16. Known limitations of this document

The recordings describe the architecture and procedure while Wesley is showing working YAML files on screen, but the transcriptions do not contain the complete YAML schemas or exact API versions. Therefore:

- Copy the `RBACDefinition`, Flux API versions, secret pattern, and optional reconciliation fields from a current working example in the target repository.
- Treat the YAML in this document as an implementation skeleton, not as a drop-in manifest.
- The target cluster repository and its CI validation remain the authoritative implementation reference.
