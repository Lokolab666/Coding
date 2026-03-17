I've a new ticket about Attn: Infra-Argo-Global Upgraded a configuration from CIAM to AUTH0, got an error on namespace job with provider error

Errors:
Processing auth details for: [clientId:M5QWPMlC4Ssyk66ac4FvTfy5rcOspcTP, provider:signin-stage.medtronic.com, temporaryJenkinsSecretsIndex:2]
Downloading cacerts.mdt from Artifactory
Exception in generateSecretsPr

log: https://code.medtronic.com/bcp_web/devops/prometheus/manage-namespace/-/jobs/21260512
The log is:
Preparing to clone dedicated ALB target repository from https://medtronic.gitlab-dedicated.com/bcp_web/devops/fluxconfigs/flux-gitops.git
Cloning into 'flux_clone_alb'...
✅ Successfully cloned primary + secondary mirror repositories
✅ Successfully cloned dedicated ALB target repository
$ echo "running namespace script..." # collapsed multi-line command
running namespace script...
Switched to a new branch 'oracleclinicalrdcsso/dev/env3714193'
HAS_SECRETS? true
Seeded generator tracker from destination: ../flux_clone_alb/clusters/TF-argo-dev/infrastructure/ingress/alb-tracker.json
Using ALB_BASE_DIR for generator: clusters/TF-argo-dev/infrastructure/ingress
groovy ../scripts/generateNamespaceFluxYaml.groovy DEV false argo-dev.eks.mdtcloud.io false true  Yk95Wm9sRG4uTDctSTg0OXZ5ZE5mci1uaH5adG0tOUVjUA== akxHNzJpcEJNTUhya09CN0FzTXVoYkstZV9aMDNZdm55TVVOVl9PZmV4LUlHWU4zS1ZxamhwM0VNdjZRTXY4Qg== dHJhY3Rpb24xMTk4NGNvbnRyb2w=
REMOVE? env prop: null, sys prop: null
env: DEV
useExistingSecrets: false
-----SECRET COUNT: 3----------
file path is: /builds/bcp_web/devops/prometheus/manage-namespace
Found match for DEV - using this
------ NAMESPACE REMOVE?: false, UNINSTALL?: false ------------
Evaluating PROJECT_ID and PROJECT_REPO_URL
Namespace directory for argo-oracleclinicalrdcsso-dev exists
Processing: RDCSSO with details [userName:RDC_SSO, URL:jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCPS)(HOST=mspldb666.corp.medtronic.com)(PORT=2484))(CONNECT_DATA=(SERVICE_NAME=rdcssod.corp.medtronic.com))), temporaryJenkinsSecretsIndex:3]
Processing auth details for: [clientId:58991d60-f3b5-4312-b9bd-b7a832baa2e2, provider:login.microsoftonline.com%2F0a29d274-1367-4a8f-99c5-90c3dc7d4043%2Fv2.0, temporaryJenkinsSecretsIndex:1]
Processing auth details for: [clientId:M5QWPMlC4Ssyk66ac4FvTfy5rcOspcTP, provider:signin-stage.medtronic.com, temporaryJenkinsSecretsIndex:2]
Downloading cacerts.mdt from Artifactory
Exception in generateSecretsProvider: java.lang.Exception: File signin-stage.medtronic.com.provider does not exist within the 'oidc-providers-1.4.tar.gz' file. Please confirm you are using one of the valid provider strings as defined in the Playbook: https://playbook.argo-dev.eks.mdtcloud.io/authentication-and-authorization/supported-authentication-providers/.
Temp directory deleted
Re-throwing exception after deleting directory
Caught: java.lang.Exception: File signin-stage.medtronic.com.provider does not exist within the 'oidc-providers-1.4.tar.gz' file. Please confirm you are using one of the valid provider strings as defined in the Playbook: https://playbook.argo-dev.eks.mdtcloud.io/authentication-and-authorization/supported-authentication-providers/.
java.lang.Exception: File signin-stage.medtronic.com.provider does not exist within the 'oidc-providers-1.4.tar.gz' file. Please confirm you are using one of the valid provider strings as defined in the Playbook: https://playbook.argo-dev.eks.mdtcloud.io/authentication-and-authorization/supported-authentication-providers/.
	at java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
	at java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(Unknown Source)
	at java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(Unknown Source)
	at scripts.generateNamespaceFluxYaml$_generateSecretsProvider_closure32.doCall(generateNamespaceFluxYaml.groovy:1838)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(Unknown Source)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source)
	at scripts.generateNamespaceFluxYaml.generateSecretsProvider(generateNamespaceFluxYaml.groovy:1813)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(Unknown Source)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source)
	at scripts.generateNamespaceFluxYaml.run(generateNamespaceFluxYaml.groovy:819)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(Unknown Source)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source)
Uploading artifacts for failed job
00:01
Uploading artifacts...
WARNING: certArn.env: no matching files. Ensure that the artifact path is relative to the working directory (/builds/bcp_web/devops/prometheus/manage-namespace) 
ERROR: No files to upload                          
Cleaning up project directory and file based variables
00:01
ERROR: Job failed: command terminated with exit code 1

How can solve it?