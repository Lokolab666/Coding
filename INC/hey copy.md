Project:
it-icons/connectivity-services/cisco-aci-epg-gitops

Runner:
#155 -  (zMpBDLU8)
 msplap1900.corp.medtronic.com

Purpose:
Enable the Cisco ACI GitOps pipeline to run OpenTofu validation and plan jobs using only approved internal Artifactory repositories. No direct Internet access is requested.

Current status:
- Runner #155 successfully pulls the approved OpenTofu image from Artifactory:
  case.artifacts.medtronic.com/ext-docker-ghcr-remote/opentofu/opentofu:1.12.5
- GitLab CI validation, plan-only execution, and GitLab remote state/locking have passed.
- The ACI module download is blocked.

Required Artifactory packages:
1. OpenTofu provider: CiscoDevNet/aci
2. OpenTofu provider: netascode/utils
3. OpenTofu module: netascode/nac-aci/aci
   Required version: 0.7.0

Issue:
During `tofu init`, the existing Artifactory Terraform registry can resolve the available version of `netascode/nac-aci/aci`, but it returns HTTP 404 when OpenTofu requests the actual module archive download.

Request:
Please make the listed providers and module available through the approved internal Terraform/OpenTofu registry. For `netascode/nac-aci/aci` version 0.7.0, please ensure both version discovery and module archive download work from GitLab runner #155.
 