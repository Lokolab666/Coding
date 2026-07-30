Adding a Flux Tenant Entry Point ("Harness") for a Team in an Existing Namespace

1. Purpose

This procedure describes how to add a Flux tenant entry point, informally called a Flux harness, for a customer team in an existing Kubernetes namespace on an Atlas/Tanzu cluster.

The harness establishes a controlled GitOps delegation boundary. It allows a team-owned Flux "Kustomization" to reconcile resources from a team-owned Git repository while using a dedicated Kubernetes "ServiceAccount" whose permissions are restricted to namespaces owned by that team.

This is an onboarding operation. It is not required every time the team requests another namespace.

2. Scope

Use this procedure only when all the following conditions are met:

- The target cluster already exists and is managed through the Atlas/Tanzu cluster-management Git repository.
- The namespace already exists under "cluster/customer-components/<namespace>/".
- The namespace is intended to be the team’s designated Flux entry-point namespace.
- The team does not already have a Flux reconciler "ServiceAccount" and matching "RBACDefinition" on the same cluster.
- The team requires Flux to reconcile configuration from its own Git repository.

Do not use this procedure when:

- The team is already onboarded to the cluster and only needs an additional namespace. In that case, create or update only the namespace resource as required.
- The requested namespace is not the team’s designated entry-point namespace.
- The team needs to create cluster-scoped Kubernetes resources. Cluster-scoped resources remain under Atlas platform ownership and require a separate platform change.
- The request is only for ingress, egress, or another network-policy change.

3. Core design rule

A team requires one Flux entry point per team per cluster, not one entry point per namespace.

The entry-point namespace contains the team’s Flux reconciliation identity and source configuration. Additional namespaces owned by the same team are selected through the team ownership label and receive permissions through RBAC Manager-generated bindings.

Before making any change, search the target cluster repository for an existing reconciler "ServiceAccount", "RBACDefinition", "GitRepository", or Flux "Kustomization" for the team.

Creating a second harness can cause:

- Duplicate reconciliation.
- Conflicting ownership.
- Multiple Flux objects managing the same resources.
- Unnecessarily broad access.
- Difficult rollback and troubleshooting.

4. Terminology

Entry-point namespace

The namespace selected by the customer team to host its Flux source and reconciliation resources. It may also be called the team’s top-level or system namespace.

It should not be described as a Kubernetes root namespace because Kubernetes does not provide namespace hierarchy in that sense.

Harness

An informal name for the complete set of resources that connects the customer’s Git repository to the cluster through Flux and applies the team’s permission boundary.

Reconciler ServiceAccount

The Kubernetes identity used by the team’s Flux "Kustomization".

The "Kustomization" must explicitly reference this identity rather than a platform-level or controller-level service account.

RBACDefinition

A cluster-scoped custom resource managed by RBAC Manager. It dynamically creates the required "ClusterRoleBinding" and namespace-level "RoleBinding" resources for the reconciler "ServiceAccount".

GitRepository source

A Flux source object that identifies the customer’s Git repository and selected Git revision. The source may reference an encrypted authentication secret.

Flux Kustomization

A Flux reconciliation object that references the "GitRepository", selects a path in that repository, defines reconciliation behavior, and runs under the dedicated reconciler "ServiceAccount".

5. Architecture and repository placement

The Atlas/Tanzu cluster-management repository is the GitOps source of truth. Flux reconciles the "customer-components" tree into the cluster.

A typical layout for an existing entry-point namespace is:

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

File names vary between existing implementations. Preserve the naming and API conventions used by the target cluster repository.

The "RBACDefinition" belongs in:

cluster/customer-components/cluster-scope/

This is because the resource is cluster-scoped.

The following resources belong in the existing entry-point namespace folder:

- "ServiceAccount"
- Git authentication secret
- Flux "GitRepository"
- Flux "Kustomization"

6. Responsibility boundary

6.1 Atlas/platform support responsibilities

The Atlas platform or support team is responsible for:

- Confirming that the selected namespace is the correct team entry point.
- Confirming that no harness already exists for the team on the target cluster.
- Validating the namespace ownership label.
- Creating or reviewing the dedicated reconciler "ServiceAccount".
- Creating or reviewing the matching cluster-scoped "RBACDefinition".
- Rejecting any configuration that uses an excessively privileged platform or Flux controller identity.
- Reviewing the customer-authored Flux resources before merge.
- Protecting cluster-scoped resources and the cluster-level control plane.
- Supporting failures caused by Flux, RBAC Manager, shared platform components, or the Kubernetes cluster.

6.2 Customer team responsibilities

The customer team is responsible for:

- Selecting the entry-point namespace.
- Providing and owning the Git repository URL.
- Providing and owning the Git branch, tag, semantic-version constraint, or other revision selector.
- Providing and owning the repository path reconciled by Flux.
- Providing the required Git authentication material through the approved encrypted-secret pattern.
- Defining the "GitRepository" and Flux "Kustomization" settings.
- Maintaining the manifests stored in the customer repository.
- Resolving application-level deployment or runtime problems that are not caused by the Atlas platform.

The platform team may assist with the initial merge request, but repository content, source revision, path, and application health remain the customer’s responsibility.

7. Required inputs

Collect and validate the following before implementation:

Input| Description
Target cluster| Cluster-management repository to be changed.
Entry-point namespace| Existing namespace selected to host the harness.
Team identifier| Exact value used by the namespace ownership label, normally "k8s.mdtcloud.io/team-name".
Reconciler ServiceAccount name| Prefer the established repository naming convention, commonly "<namespace>-reconciler".
Existing harness status| Confirmation that no reconciler and "RBACDefinition" already exist for this team on the cluster.
Customer Git URL| Repository to be polled by the Flux "GitRepository".
Git revision| Branch, tag, semantic-version constraint, or commit policy approved by the customer.
Repository path| Directory containing the manifests Flux must reconcile.
Authentication secret| Name and approved encrypted-secret implementation used to access the repository.
Reconciliation settings| Interval, prune behavior, timeout, health checks, and dependencies, following the existing platform pattern.
Reviewer| Independent Atlas approver. During the KT transition, Wesley was the designated reviewer.

Do not infer customer repository values. Obtain them from the customer or from an existing approved deployment for the same team.

8. Pre-change validation

8.1 Confirm the namespace exists

Verify that the namespace folder and namespace manifest are already present:

cluster/customer-components/<entry-point-namespace>/namespace.yaml

Do not recreate or replace the namespace manifest unless the request explicitly includes a namespace correction.

8.2 Confirm that this is the designated entry-point namespace

An existing namespace is not automatically an appropriate harness location.

Confirm that:

- The customer selected this namespace as its entry point.
- The namespace belongs to the correct team.
- The namespace is not a workload-only namespace such as a database, Kafka, or application-specific namespace unless the team explicitly selected it.
- Moving or duplicating an existing harness is not part of the request.

8.3 Confirm team ownership

Verify that the namespace has the correct team label:

metadata:
  labels:
    k8s.mdtcloud.io/team-name: <team-identifier>

The exact label key must be copied from a working namespace in the same repository.

A wrong team label can:

- Prevent namespace-level RBAC bindings from being generated.
- Apply bindings to the wrong tenant.
- Cause reconciliation authorization failures.
- Break the multi-tenant security boundary.

8.4 Confirm that the harness does not already exist

Search the repository for the team identifier, reconciler name, and namespace name.

Check at least:

cluster/customer-components/cluster-scope/
cluster/customer-components/*/

Look for:

- A reconciler "ServiceAccount".
- An "RBACDefinition" that references the team or service account.
- A Flux "GitRepository" for the team.
- A Flux "Kustomization" using the team’s reconciler service account.

If a valid harness already exists on that cluster, stop.

Do not create another one. Additional team namespaces should use the existing delegation model.

8.5 Select a known-good reference

Use references in this order:

1. The same team on another Atlas/Tanzu cluster.
2. Another team on the same cluster using the current naming and CRD versions.
3. Another current Atlas/Tanzu cluster using the same platform release pattern.

Do not build the "RBACDefinition" schema from memory. Copy a working current resource and modify only the tenant-specific fields.

9. Implementation procedure

Step 1: Synchronize the repository and create a branch

Start from the latest "main" branch:

git checkout main
git pull --ff-only
git checkout -b onboard/<team>-flux-entry-point

Use Linux line endings.

Avoid committing Windows CRLF changes to unrelated files.

Step 2: Add the reconciler ServiceAccount

Create the service account in the existing entry-point namespace folder.

apiVersion: v1
kind: ServiceAccount
metadata:
  name: <entry-point-namespace>-reconciler
  namespace: <entry-point-namespace>

Use the naming convention already established in the repository.

The service account must be dedicated to the customer team. It must not be replaced with:

- A Flux controller service account.
- A Kustomize controller service account.
- A platform-wide service account.
- A root-level or cluster administrator identity.

Step 3: Add the matching RBACDefinition

Create the "RBACDefinition" in:

cluster/customer-components/cluster-scope/

Use a working "RBACDefinition" as the template.

The required behavior is:

- The subject is the reconciler "ServiceAccount" created in the entry-point namespace.
- The resource selects namespaces owned by the team through the team ownership label.
- Namespace-level administrative permissions are granted only inside matching team namespaces.
- Read or view access required to inspect Flux resources is provided according to the current platform pattern.
- The service account is not granted unrestricted cluster-scoped write access.

Illustrative structure only:

apiVersion: <copy-from-current-repository>
kind: RBACDefinition
metadata:
  name: <team>-reconciler
spec:
  # Copy the current Atlas RBAC Manager schema.
  #
  # Subject:
  #   ServiceAccount/<entry-point-namespace>-reconciler
  #
  # Namespace selector:
  #   k8s.mdtcloud.io/team-name=<team-identifier>
  #
  # Bindings:
  #   Current approved Flux visibility and namespace-local permissions.

Do not convert a namespace-scoped "RoleBinding" that references the "cluster-admin" "ClusterRole" into a global "ClusterRoleBinding".

This distinction is critical:

- A "RoleBinding" may reference a "ClusterRole" while limiting the granted permissions to one namespace.
- A "ClusterRoleBinding" grants the referenced permissions at cluster scope.
- Binding "cluster-admin" through a "ClusterRoleBinding" would violate the tenant boundary.

The service account and "RBACDefinition" are a required pair.

A service account without the matching RBAC definition has no useful reconciliation permissions.

Step 4: Add or review the encrypted Git credential resource

The customer should supply the approved Git authentication resource.

Credentials must be stored only through the existing encrypted pattern, such as the SOPS-based secret workflow used by the platform.

Requirements:

- No plaintext token, password, SSH private key, or deploy key is committed.
- The secret is created in the entry-point namespace.
- The secret name matches the "GitRepository.spec.secretRef" value.
- The encrypted file can be processed by the cluster’s established secret-management workflow.

The transcriptions do not expose the exact customer-secret schema. Copy the current implementation used by another onboarded team.

Step 5: Add or review the Flux GitRepository source

The "GitRepository" defines the external source of truth for the customer team.

Illustrative structure:

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

The customer controls:

- Repository URL.
- Branch, tag, commit, or version constraint.
- Authentication configuration.
- Other customer-specific source settings.

The Atlas reviewer validates that the values:

- Are syntactically valid.
- Use the approved authentication pattern.
- Reference an approved customer repository.
- Do not expose credentials.
- Do not create an obvious security or operational risk.

Do not assume "main" is the correct customer branch.

Existing platform and customer repositories may use:

- Environment-specific branches.
- Tags.
- Semantic-version constraints.
- Fixed revisions.

Step 6: Add or review the Flux Kustomization

The Flux "Kustomization" connects the "GitRepository" to the target manifests and enforces the service-account boundary.

Illustrative structure:

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

Mandatory review points:

- "sourceRef.name" matches the "GitRepository" name.
- "path" exists in the selected Git revision.
- "serviceAccountName" references the dedicated team reconciler.
- The object does not use a platform controller service account.
- Reconciliation settings follow a working platform example.
- The manifests under the selected path are intended for namespaces owned by the team.
- Cluster-scoped resources are removed or routed through a separate Atlas-owned change.

One recording showed a customer configuration reconciling every five minutes. Treat this as an example, not a mandatory platform default.

Step 7: Perform static validation

Review the complete change before committing:

git status
git diff --check
git diff

Confirm:

- Only the expected files changed.
- The service account namespace and name are correct.
- The "RBACDefinition" subject matches the service account exactly.
- The team selector matches the namespace label exactly.
- The "GitRepository" and secret references match.
- The "Kustomization" source, path, and service-account references match.
- No secret material is visible in plaintext.
- No broad or root-level service account is used.
- No duplicate harness is being added.
- YAML indentation is valid.
- Repository naming conventions are preserved.
- No unrelated line-ending changes are included.

Run any repository-provided lint, schema-validation, policy, or CI commands before opening the merge request.

Step 8: Commit and open the merge request

Use a descriptive commit and branch:

git add cluster/customer-components
git commit -m "Add <team> Flux entry point"
git push --set-upstream origin onboard/<team>-flux-entry-point

The merge request should include:

- Customer or team name.
- Target cluster.
- Entry-point namespace.
- Customer request or ticket reference.
- Repository URL without credentials.
- Git revision type and value.
- Repository path.
- Confirmation that no existing harness was found.
- Validation performed.
- Expected files and resources added.
- Any identified operational risk.

Assign the author as the assignee and an independent Atlas maintainer as reviewer.

The KT sessions recommend peer review by someone other than the committer. During the initial transition, changes were assigned to Wesley for review.

Squashing commits and deleting the source branch after merge are recommended housekeeping practices when allowed by the repository settings.

Step 9: Merge and allow Flux to reconcile

After approval and merge to "main", Flux automatically reconciles the cluster-management repository.

Do not manually apply the same manifests with "kubectl" as the normal deployment path.

Manual application would:

- Create drift between Git and the cluster.
- Make rollback less reliable.
- Hide the actual source of the change.
- Allow Flux to overwrite the manually applied state later.

10. Post-merge verification

10.1 Verify the Kubernetes and Flux objects

Example commands:

kubectl -n <entry-point-namespace> \
  get serviceaccount <entry-point-namespace>-reconciler

kubectl -n <entry-point-namespace> get gitrepository

kubectl -n <entry-point-namespace> get kustomization

kubectl -n <entry-point-namespace> \
  describe gitrepository <team-source-name>

kubectl -n <entry-point-namespace> \
  describe kustomization <team-reconciliation-name>

Flux CLI checks:

flux get sources git -A
flux get kustomizations -A
flux tree kustomization flux-system

The exact root "Kustomization" name may differ by cluster. Use the current Flux tree for the target cluster.

10.2 Verify generated RBAC bindings

Confirm that RBAC Manager created the expected bindings for the service account:

kubectl get rolebinding -A -o wide \
  | grep '<entry-point-namespace>-reconciler'

kubectl get clusterrolebinding -o wide \
  | grep '<entry-point-namespace>-reconciler'

Expected behavior:

- The reconciler can inspect the Flux resources required by the current platform pattern.
- The reconciler can manage resources in namespaces carrying the team’s ownership label.
- The reconciler cannot create namespaces.
- The reconciler cannot modify CRDs.
- The reconciler cannot modify unauthorized cluster-scoped resources.
- The reconciler cannot manage namespaces belonging to another team.

10.3 Verify readiness

The "GitRepository" and Flux "Kustomization" should report:

Ready=True

Review status conditions and events rather than relying only on the objects existing.

The customer should confirm that:

- The intended repository revision is being used.
- The intended path is being reconciled.
- The expected application resources were created.
- No unintended namespace or resource is being managed.

11. Acceptance criteria

The change is complete when all the following conditions are true:

- The team has exactly one Flux harness on the target cluster.
- The harness is located in the approved
