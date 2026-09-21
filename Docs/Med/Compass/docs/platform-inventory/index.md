---
label: Platform Inventory
icon: server
order: 10
---
# Platform Inventory

Infrastructure inventory for each platform cluster, including network configurations, connectivity requirements, and architecture diagrams.

## Clusters

- **[Argo - Development](./argo-dev.md)** - Shared multi-tenant cluster for development and testing (DEV, TESTING) - AWS Account #389242548790
- **[Argo - Production](./argo-prod.md)** - Shared multi-tenant cluster for staging and production (STAGING, RELEASE, PRODUCTION) - AWS Account #872019488961

---

## What's Included

Each cluster page contains:

- **Cluster Information** - Basic details about the cluster (name, environments, region, VPC)
- **Application URL Patterns** - URL format for deployed applications in each environment
- **Webhook URLs** - Webhook URL for setting up push-based GitOps
- **Priority Classes** - Pre-configured priority classes to ensure production workloads are scheduled first
- **Network Configuration** - App subnet CIDRs and NAT gateway IPs
- **Connectivity Inventory** - Complete list of outbound and inbound network connections
- **Infrastructure Diagram** - Visual representation of the cluster architecture

---

## Adding a New Cluster

This section is the source of truth for cluster onboarding steps in compass-ci-playbook. Update onboarding instructions here and avoid duplicating them in other playbook pages.

When onboarding a new cluster:

1. Create a new markdown file in this directory (e.g., `my-cluster.md`)
2. Document cluster information, URL patterns, priority classes, network config, and connectivity inventory
3. Add a link to the new cluster page in this index file
4. Add Flux GitOps onboarding assets in the [`flux-gitops`](https://medtronic.gitlab-dedicated.com/bcp_web/devops/fluxconfigs/flux-gitops) repo:
	- Create a new folder under `flux-gitops/base/sources` using the cluster name
	- Add the four bootstrap resources documented in `flux-gitops/base/sources/Cluster-Onboarding.md`. These will be manually loaded to each cluster via `kubectl` commands:
	  - GitLab token Secret (read access to flux-gitops)
	  - GitRepository (points to flux-gitops)
	  - Kustomization (syncs the cluster's infrastructure folder)
	  - Kustomization (syncs the cluster's apps folder)
5. Create per-cluster folders in flux-gitops/clusters:
	- `clusters/<cluster-name>/infrastructure`
	- `clusters/<cluster-name>/apps`
	- Ensure bootstrap Kustomization paths match these folder names exactly
6. Apply bootstrap resources in order, using `kubectl` commands to apply them to the cluster:
	1. `gitlab-dedicated-token.yaml`
	2. `git-repository.yaml`
	3. `infrastructure-kustomization.yaml`
	4. Wait for `infrastructure` Kustomization to be Ready
	5. `apps-kustomization.yaml`
7. Configure the external Flux webhook endpoint for the cluster (one per cluster):
	- Create a `Receiver` in `flux-system` to accept push events (type `gitlab`)
	- Create a Secret containing the webhook token used by GitLab (referenced by the Receiver)
	- Ensure the receiver service is exposed via Ingress (HTTPS) so GitLab can reach it
	- Recommended: place these in `clusters/<cluster-name>/infrastructure/flux-webhook/` and include them from `clusters/<cluster-name>/infrastructure/kustomization.yaml`
	- Validate with `flux get receivers -n flux-system` and confirm events in the Receiver status
8. Ensure app-layer decryption prerequisites are in place before app sync:
	- Include `base/external-secrets-operator/` in `clusters/<cluster-name>/infrastructure/kustomization.yaml`
	- Add cluster-specific ESO resources to create `flux-system/sops-age`
	- Ensure `apps-kustomization.yaml` includes SOPS decryption with `secretRef.name: sops-age`
9. Ensure baseline platform components are installed for the cluster:
	- Priority classes for each environment so production preempts lower environments
		- `priority-production` (1000) for production and release
		- `priority-staging` (750) for staging
		- `priority-testing` (500) for testing
		- `priority-dev` (250) for dev
	- Store these as cluster-scoped resources in the Flux repo under `clusters/<cluster-name>/infrastructure/`
	- Add `priority-classes.yaml` to `clusters/<cluster-name>/infrastructure/` and reference it from `clusters/<cluster-name>/infrastructure/kustomization.yaml`
	- Keep application-level Kustomizations under `clusters/<cluster-name>/apps/` (do not place priority classes there)
	- Do not place priority classes in `base/sources/` (that folder is reserved for Flux bootstrap resources)
	- Include `base/kyverno-policies/` and verify `restart-deployment-on-secret-change` is active
10. Configure `clusters/<cluster-name>/cluster.properties` with onboarding metadata:
	- `TF_REPO_PATH`
	- `TF_PROJECT_ID`
	- `DEFAULT_HOSTNAME_SUFFIX` (if applicable)
	- `ROUTE53_ZONE_DATASOURCE_NAME` (if applicable, for AWS-backed clusters)
	- `TF_TOKEN_VAR` (recommended)
11. Ensure app project CI variables are ready for the new cluster:
	- `CLUSTER_NAME`
	- `FLUX_GITOPS_REPO_RW_TOKEN`
	- `TERRAFORM_REPO_RW_TOKEN`
	- `EXT_ID` and `ROLE_ARN` (environment scoped)

---

## Related Resources

- [Connectivity Request Process](../how-to/connectivity-request-process.md)
- [Deployments](../how-to/deployments.md)
