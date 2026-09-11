I have the next issue
We need support completing OpenTofu provider delivery for the Cisco ACI GitOps project.
 
Current CI design:
- GitLab runner #155 uses only the internal Artifactory OpenTofu provider mirror.
- No direct internet download is required for the standard pipeline.
- The NaC ACI module is already vendored locally, so module archive delivery is no longer needed.
 
Required providers:
- registry.opentofu.org/ciscodevnet/aci v2.20.0
- registry.opentofu.org/netascode/utils v1.0.2
 
Issue:
The GitLab CI job previously timed out when retrieving these providers through the configured Artifactory provider mirror.
 
What we have done:
I successfully generated signed linux_amd64 provider mirror packages on my laptop for both required providers.
 
What is needed:
Please confirm the correct Artifactory repository and path for uploading the generated providers directory. Also confirm whether I have permission to upload it, or whether the Artifactory team must upload it.
 
Once the provider mirror is available in Artifactory, runner #155 will retrieve the providers through the existing CI network_mirror configuration.
Please upload the attached provider folder into the Artifactory repository that serves this URL:
https://case.artifacts.medtronic.com/artifactory/api/terraform/ext-terraform-registry-remote/providers/
After extracting the ZIP, upload the registry.opentofu.org folder directly under the providers path, preserving all subfolders and files.
If ext-terraform-registry-remote is read-only, please provide the writable local repository/path that feeds this provider mirror.

And my checks were, because this is a remote artifactor:
The ext-terraform-registry-remote is read-only, so look the next info:
Registry URL
https://registry.terraform.io
Providers URL
https://releases.hashicorp.com

The reply form the customer was:
Thank you for confirming the current Artifactory repository is read-only.
Please configure it to retrieve and cache these required OpenTofu providers from registry.opentofu.org:
ciscodevnet/aci v2.20.0
netascode/utils v1.0.2
Once the providers are cached in Artifactory, GitLab runner #155 will download them through the existing provider mirror configuration.
If this is not possible, please advise the approved alternative for making these provider packages available to the GitLab runner.

So, check  alternative for making these provider packages available to the GitLab runner in Dedicated