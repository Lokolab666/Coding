---
label: Troubleshooting Guide
icon: hubot
order: 100
---

# Troubleshooting Guide #

## Local Development Issues ##

### Docker Desktop High Memory Usage {#docker-desktop-high-memory-usage}

If you experience high memory usage when Docker Desktop is running, try one of the following solutions:

**Option 1: Disable WSL 2 Engine**
1. Open Docker Desktop
2. Go to Settings
3. Uncheck "Use the WSL 2 based engine"
4. Restart Docker Desktop

**Option 2: Configure WSL Memory Limits**
1. Create a `.wslconfig` file in your user home directory (typically `C:\Users\<userid>\`)
2. Add memory limits to the file (example content):
   ```ini
   [wsl2]
   memory=4GB
   processors=2
   ```
3. Save the file
4. Restart your computer for changes to take effect

!!!warning File Type
Ensure the file is saved as `.wslconfig` with file type `WSLCONFIG File`, not as a text file.
!!!

---

## General GitLab Pipeline Errors ##
These are general errors that could occur on any of the pipeline stages.

#### Protected branch push/merge denied for automation bot ####
If your project enforces protected branch restrictions and pipeline automation fails to push tags, commits, or branch updates, you may see errors like:

```properties
remote: GitLab: You are not allowed to push code to protected branches
```

Common jobs impacted:
- semantic-release (tag/release push)
- deploy/tag automation jobs that commit/push
- webhook/security automation that writes back to GitLab

**Root cause:**
The bot identity behind `GROUP_RW_ACCESS_TOKEN` is not allowed by protected branch rules.

**Fix:**
1. In GitLab, go to **Settings → Repository → Protected Branches**
2. Edit each protected branch used by automation (`dev`, `testing`, `staging`, `release`, `main` as applicable)
3. In **Allowed to push** and/or **Allowed to merge**, search using the token name
4. Select the bot account shown as `group_<...>_bot`
5. Save and re-run the failed pipeline

For full setup details and screenshot guidance, see:
- [GitLab Repository Configuration - Required: Allow the Group RW Token Bot on Protected Branches](../getting-started/gitlab-repository-configuration.md#grant-the-group-rw-token-bot-permissions-on-protected-branches)
- ![](../static/gitlab-rw-token-allow-to-merge-push.png)

#### Failed to get files used from context ####
If receiving the below error, this indicates that the `.war` file generated during the build stage of the Dockerfile does not exist at the location it is later attempting to copy it from.
```properties
INFO[0255] Resolving srcs [/workspace/projects/thisproject/target/*.war /wars/app.war ADD docker-configs/snoop_jsp.out]...
error building image: error building stage: failed to optimize instructions: failed to get files used from context: copy failed: no source files specified
```

To resolve you can:
* Try running the builder stage command that generates the war file from the project on your local machine to determine where in the project structure the application's .war file is placed. It should be towards the top of your Dockerfile. This command should be something like `ant clean war -f build.xml`. If it is placed at the root of the project, then the COPY command should be copying from `/workspace/projects/thisproject/*.war` (`/target` is removed).
* You can also add a line to the Dockerfile during the builder stage to have it print any directories that contain a `.war` file to determine what the folder is for use during the COPY stage. The instruction to add in the `builder` stage after the `.war` file is generated would be something like this:
```Dockerfile
# Run the command to find directories containing .war files and print to console
RUN find . -type f -name "*.war" -exec dirname {} \; | sort -u
```
Copy the outputted directory to the `COPY --from=builder ...` command as the source directory. Destination directory of `/wars/app.war` will remain the same.

#### Packages not found ####
If you attempt to add a package to your `Dockerfile` and it is not found, search for it on the [Alpine Linux Packages site](https://pkgs.alpinelinux.org/packages). Check various repos by changing the dropdowns. If it exists in a different repo, you can reference it accordingly:
```dockerfile
RUN apk add aws-cli@community
```

#### Job timeout ####
The build job timeout is currently set to 2 hours. If the kaniko job times out with a similar message to the below:
```properties
INFO[0154] Taking snapshot of full filesystem...
WARNING: step_script could not run to completion because the timeout was exceeded. For more control over job and script timeouts see: https://docs.gitlab.com/ee/ci/runners/configure_runners.html#set-script-and-after_script-timeouts
ERROR: Job failed: execution took longer than 2h0m0s seconds
```

To resolve this, you can try adding the following `KANIKO_ADDITIONAL_FLAGS` variable with the below value to the `build-app-image` job definition in your `.gitlab-ci.yml`:
```yaml
build-app-image:
  stage: build
  needs: ["preconditions-check"]
  trigger:
    project: bcp_web/devops/prometheus/build-app-image
    branch: test/kaniko-flags-as-args
    strategy: depend
  variables:
    <<: *global_variables
    PROJECT_COMMIT_SHA: $CI_COMMIT_SHA
    KANIKO_ADDITIONAL_FLAGS: "--compressed-caching=false --use-new-run --cleanup"
  rules:
    - if: '$CI_COMMIT_MESSAGE =~ /\[build-image\]/ && $CI_PIPELINE_SOURCE == "push"'
```

#### Terraform apply fails: Secrets Manager secret already scheduled for deletion ####
When Terraform attempts to create an AWS Secrets Manager secret and the same secret name is pending deletion, apply fails with an error like:

```properties
Error: creating Secrets Manager Secret (compass-ci/ciam/cic-profilemanagement/sops-age-key): operation error Secrets Manager: CreateSecret, https response error StatusCode: 400, RequestID: 3dfe6bb9-77d6-4f94-9efb-f7b8ac7fc176, InvalidRequestException: You can't create this secret because a secret with this name is already scheduled for deletion.

with aws_secretsmanager_secret.sops_age_key_ciam_cic_profilemanagement
on app_ciam_cic-profilemanagement.tf line 294, in resource "aws_secretsmanager_secret" "sops_age_key_ciam_cic_profilemanagement":
resource "aws_secretsmanager_secret" "sops_age_key_ciam_cic_profilemanagement" {
```

**Root cause:**
- The prior secret object still exists in AWS as `PendingDeletion`, so the same name cannot be created yet.

**Platform team remediation (approved break-glass):**
1. Stop the current Terraform apply.
1. Confirm current status:

```bash
aws secretsmanager describe-secret \
   --secret-id "compass-ci/ciam/cic-profilemanagement/sops-age-key" \
   --region us-east-1
```

3. If replica exists, remove replica region first:

```bash
aws secretsmanager remove-regions-from-replication \
   --secret-id "compass-ci/ciam/cic-profilemanagement/sops-age-key" \
   --remove-replica-regions us-east-2 \
   --region us-east-1
```

4. Force delete in primary region:

```bash
aws secretsmanager delete-secret \
   --secret-id "compass-ci/ciam/cic-profilemanagement/sops-age-key" \
   --force-delete-without-recovery \
   --region us-east-1
```

5. Re-check until `describe-secret` no longer returns the secret, then rerun Terraform apply.

```bash
aws secretsmanager describe-secret \
   --secret-id "compass-ci/ciam/cic-profilemanagement/sops-age-key" \
   --region us-east-1
```

**Terraform state note (resource rename only):**
- If this was only a Terraform resource address rename, prefer moving Terraform state instead of deleting/recreating the AWS secret:

```bash
terraform state mv <old_resource_address> <new_resource_address>
```

**Caution:**
- `--force-delete-without-recovery` permanently deletes the secret immediately. Use only with platform approval.

#### Altering Build Execution Rules ####
GitHub CoPilot can be a useful too for suggesting GitLab CI rules based on your requirements. As an example, asking something like this:
```text
Write a GitLab CI job rule where the job triggers if the commit message contains [build-image] and does not trigger for merge requests.
```
Output:
```yaml
build_image_job:
  script:
    - echo "Building the image..."
  rules:
    - if: '$CI_COMMIT_MESSAGE =~ /\[build-image\]/ && $CI_PIPELINE_SOURCE != "merge_request_event"'
```
The above `rules` section could then be copied into the `.gitlab-ci.yml` to adjust when the pipeline triggers the build-app-image job for example.

## Containers failing to start ##
Containers can fail to start up for multiple reasons. Below are some of the typical ones to check for.
1. Look at application logs in [Grafana](../monitoring/03-logging.md).
1. Check Event Log as well as the Application Release Status log panels in your [Grafana](../monitoring/03-logging.md) dashboard. This would show events such as pods starting, failing health checks, etc. Release Status logs show the current status of the application's release and whether it is in-sync with the desired state or not.
1. Database connection failures.

    __Example log output:__
   ```properties
    DATABASE JDBC FAILURE [jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=tcps)(HOST=mspldb291.corp.medtronic.com)(PORT=2484))(CONNECT_DATA=(SERVICE_NAME=web12t.test.corp.medtronic.com)))] : 1
    ```
    * Confirm the database username, connection string, and passwords are correct in `environmentInfo.json`. Ensure password was entered correctly when running manage-namespace. Re-run your namespace job and re-enter all secrets in the correct order.
    * Database connectivity is checked at time of startup, so a bad database password, username, or connection string can cause pods to fail to start.
1. Use Docker Compose to [run the application locally](../getting-started/containerization.md) to troubleshoot.

## Timeouts or Connectivity Issues ##
If your application is having trouble connecting or communicating with things like databases, APIs, etc.
- Ensure you have completed the appropriate [Connectivity Request](../how-to/connectivity-request-process.md) for the cluster where your application is deployed. Use the below connectivity tester associated with your app environment to confirm whether a connection is open/available:
    * [Connectivity Tester (Dev)](https://einstein.argo-dev.eks.mdtcloud.io/einstein/network)
        * For applications hosted in the `it-argo-dev-mdt` account (`DEV` and `TESTING` app environments).
    * [Connectivity Tester (Prod)](https://einstein.argo-prd.eks.mdtcloud.io/einstein/network)
        * For applications hosted in the `it-argo-prod-mdt` account (`STAGING`, `RELEASE`, and `PRODUCTION` app environments).
- Check logs for your application.
- If requests are to AWS endpoints such as secretsmanager.us-east-1.amazonaws.com are only occasionally failing from the application, that also indicates a connectivity issue. Submit a [Connectivity Request](../how-to/connectivity-request-process.md). You can download the forms below:
   - DEV / TESTING sample form for Argo-dev Cluster is available from the [Connectivity Request Process](../how-to/connectivity-request-process.md).
        - Complete the first line where the source IP / source FQDN is filled in for you. Add the Destination FQDN with the fully qualified hostname of the endpoint you are needing to connect to. Remove the second line.
   - STAGING / RELEASE / PRODUCTION sample form for Argo-prod Cluster is available from the [Connectivity Request Process](../how-to/connectivity-request-process.md).
        - Complete the first line where the source IP / source FQDN is filled in for you. Add the Destination FQDN with the fully qualified hostname of the endpoint you are needing to connect to. Remove the second line.
- Confirm how connections are authenticated. If the connection is protected by some type of authentication (password, token, etc.), ensure proper authentication details are passed.
- Submit a ticket to Infra-Argo-Global to confirm connectivity from the pod(s) to desired connections. This can be done through `curl` requests from the application pod to a given endpoint.
    - Support team can also check the pod IP to confirm it is appropriately assigned. All pod IPs should begin with 100.64.x.x, if seeing 10.210.x.x that should not happen. Pods erroneously assigned a 10.210.* IP will be removed automatically until a new one comes up with a 100.64.x.x IP.
    - Check VPC Flow logs (LZ-DefaultVPC-VPCFlowLogsLogGroup-xxxxx):
        ```text
        fields @timestamp, dstAddr, srcAddr, azId, subnetId, action
        | sort @timestamp desc
        | limit 10000
        | filter dstAddr like /10\.210\.90\.(114|132)/
        ```
        Change dstAddr to the address of whatever the pods are trying to connect to.
    - Check [Endpoints](https://us-east-1.console.aws.amazon.com/vpcconsole/home?region=us-east-1#Endpoints:). If there is a custom endpoint defined for that service, rules / security groups / subnets may need to be altered.

!!!warning Quarantine Cluster
Ensure your application has `outboundConnections` defined in each environment in your `environmentInfo.json` file or your application will __not__ have connectivity.
!!!

## JDBC Connection Issues ##
If you receive JDBC errors like the below examples, the below can be helpful for troubleshooting:
1. If the errors started after a new build was deployed, revert to a previous deployment and confirm whether the issue is still present.
   * This will help identify whether the problem is with new code and/or image or a hosting environment (Argo) issue.
2. If the issue is intermittent and connections work sporadically, this could be an issue with a connection leak in your application.
   * [This blog post](https://engineering.3ap.ch/post/running-out-of-db-connections/) provides a good step by step that can be taken to troubleshoot for a SpringBoot application, but similar steps can be followed for other application types.
   * Ensure database connections are transactional and that all connections are properly closed after use.
3. Enable or increase JDBC logging. How to do this may vary depending on your application type.

```properties
JDBC driver encountered communication error. Message: Exception encountered for HTTP request: Connection reset.
```

```properties
[Unable to acquire JDBC Connection; nested exception is org.hibernate.exception.GenericJDBCException: Unable to acquire JDBC Connection]
```

```properties
[Tomcat JDBC Pool Cleaner[2085972722:1692180892106]] net.snowflake.client.jdbc.RestRequest.execute HTTP request took longer than 5 min: 300 sec\
```

## PKIX Certificate Errors ##
There are a few different PKIX certificate related errors you may see. Review the below to determine what type of error you are receiving as resolution will vary based on the root cause / error message received.

### Path Validation Failed ###
If receiving an error in your Java-based application like this:
```properties
Caused by: sun.security.validator.ValidatorException: PKIX path validation failed: java.security.cert.CertPathValidatorException: validity check failed
...
Caused by: java.security.cert.CertificateExpiredException: NotAfter: Sat Jan 13 12:28:35 GMT 2024
```

This typically indicates that the certificate on the source system (database, etc.) is not updated. To confirm this, you can do the following.

1. From any machine/system that has connectivity to the source system/port, run a few commands to confirm status of the certificate:
   As an example, if the app receiving an error connecting to msprdb166.corp.medtronic.com on port 2484:
    * `openssl s_client -showcerts -servername msprdb166.corp.medtronic.com -connect msprdb166.corp.medtronic.com:2484 `<br/>
      ![](../static/troubleshooting/openssl-showcerts.png)

    * `curl -v https://msprdb166.corp.medtronic.com:2484`<br/>
      If the certificate is expired on the source system, you may see a response like below. Note the `expire date` and message stating `Peer's Certificate has expired`:
```properties
    curl -v "https://msprdb166.corp.medtronic.com:2484"
    * About to connect() to msprdb166.corp.medtronic.com port 2484 (#0)
    *   Trying 10.51.68.32...
    * Connected to msprdb166.corp.medtronic.com (10.51.68.32) port 2484 (#0)
    * Initializing NSS with certpath: sql:/etc/pki/nssdb
    *   CAfile: /etc/pki/tls/certs/ca-bundle.crt
        CApath: none
    * Server certificate:
    *       subject: CN=msprdb166.corp.medtronic.com,OU=SSIT,O=Medtronic Inc,L=Mounds View,ST=MN,C=US
    *       start date: Jan 13 12:28:35 2022 GMT
    *       expire date: Jan 13 12:28:35 2024 GMT
    *       common name: msprdb166.corp.medtronic.com
    *       issuer: CN=MDT Issuing CA 2-2,O=Medtronic
    * NSS error -8181 (SEC_ERROR_EXPIRED_CERTIFICATE)
    * Peer's Certificate has expired.
    * Closing connection 0
      curl: (60) Peer's Certificate has expired.
      More details here: http://curl.haxx.se/docs/sslcerts.html
```
2. Once you have confirmed the certificate is expired on the source system, reach out to the hosting team or DBAs managing that source and request the certificate to be updated. Ensure they complete both of the below steps:
   a. Reload the listener during cert renewal (reloading your listener will allow listener to take new changes (configuration) without stopping it, and it will un-register\register dynamically registered DBs.)
   b. Bounce (stop and start) the listener (stop will stop listener completely by disallowing new session to connect to the database remotely, start will start listener and this will allow users to continue working on the database.)

3. Re-run commands from Step 1 above to confirm that the error messages are now gone and certificate expiration date is resolved.
4. The application should self-heal and start up without any additional action required.

### Path Building Failed ###
If receiving an error in your Java-based application like this:
```text
sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target; nested exception is javax.net.ssl.SSLHandshakeException: sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
```

This typically means the root and/or issuing CA for the source is not loaded.

!!!warning
Only the CA (Certificate Authority) certs need to be loaded to the cacerts. Individual database or host specific certificates themselves do NOT need to be loaded. As long as the issuing/root CAs are trusted that will allow the secure connectivity.
!!!

1. Determine what your application is attempting to connect to that is throwing the error.
1. Submit a ticket to Infra-Argo-Global and provide:
    - The URL / endpoint your application is attempting to connect to when receiving the error.
    - Mention that you are receiving a certificate error and need the java cacerts updated.
1. Once support team updates the cacerts, you must perform the following actions:
    1. Re-run manage-namespace job
    2. Re-deploy the application. *__Note:__* this can be done using the deploy image process to re-deploy the existing application. You do *__NOT__* need to re-build the application image.

==- Web Support Team Instructions
1. Use a browser such as Chrome or Firefox to download the pem certificate chain for the site that is not accessible by the application.
    - Navigate to the URL provided by the application team.
    - Use the "lock" icon in the URL bar to open details and navigate to viewing the certificate. You can then download it.
1. Convert the pem to a cer file:
   `openssl x509 -inform PEM -outform DER -in input.pem -out output.cer`
1. Download the current `cacerts.mdt` and all other `cacerts.mdt.*` files from this folder in Artifactory. For simplicity, place them in the same folder on your local machine as the cer file from Step 2.
   https://case.artifacts.medtronic.com/artifactory/bcp-frameworks-virtual/mdt-web-tools/certs/
1. Load the cert from Step 2 into __each__ cacerts file downloaded in Step 3 by running:
   `keytool -import -keystore cacerts.mdt -file certificate.cer -alias myalias`
    - Replace "cacerts.mdt" with the cacerts file you are updating
    - Replace certificate.cer with the name of the file saved in Step 2.
    - Replace "myalias" with an appropriate name for this certificate - typically matching the certificate CA (i.e. digicert_root_ca, zscaler_ca, etc.)
    - If prompted for password, use the default java cacerts password.
    - Note: run this command for each file downloaded to ensure they all have the new cert!
1. View contents of the cacerts file to confirm it was loaded properly by running the following command. You can then search the text file for the alias used in the previous step to confirm the cert was loaded.
   `keytool -list -v -keystore cacerts.mdt >> cacerts_content.txt`
1. Upload the new cacerts.mdt and all other cacerts.mdt.* files back into Artifactory under: https://case.artifacts.medtronic.com/artifactory/bcp-frameworks-virtual/mdt-web-tools/certs/
1. Inform application team that they need to:
    1. Re-run manage-namespace job
        - This will pull cacerts.mdt (or cacerts.mdt.*) file from Artifactory and place it in the Flux repo under the namespace's /files folder
    2. Re-deploy the app (using DeployImage).
        - Base 64 encode the cacerts file in the /files folder and put into the secrets-files.yaml. This is what is then used by the container to inject the cacerts.
===

## Azure Login Errors ##

### Missing Redirect URI ###
If after login to the application, you receive an error such as:
```text
AADSTS50011: The redirect URI: `https://<domain>/<contextRoot>/redirect` specified in the request does not match the redirect URLs configured for the application `<clientId>`.
```

Follow the [authentication instructions](../how-to/authentication.md) to request the redirect URI from the error message (shown below outlined in red) be added to the client id. The client id is also shown in the same error message - see the screenshot below where the client id is outlined in blue.

![](../static/troubleshooting/azure-login-redirect-url-missing-error.png)

## Session Behavior ##
- Applications should be written in a stateless manner (i.e each request does not maintain memory of previous transactions or requests).
- Ensure any objects that do need to be stored in memory are Serializable.

## Read-only file system ##
If the application attempts to store files to the system, they must be temp files or written to a directory that has been defined in your `volumes` settings in `appInfo.json`. If they are not, you may see an error such as:
```properties
java.io.FileNotFoundException: TheFile.pdf (Read-only file system)
```

For example, in Java, instances of:
```java
new File(...)
```
Should be updated to use:
```java
File.createTempFile(...)
```

!!!warning
Pods in Kubernetes are temporary and can be replaced at any time. Any files saved inside a pod may be lost if the pod restarts or is rescheduled. For long-term file storage, use external storage solutions.
!!!

## Pods Killed or Evicted ##
Pods being evicted or killed can happen for various reasons. Some common causes are given below.

If you see events in your Grafana dashboard for "Pod Evicted" as shown below:
![](../static/monitoring/pods-evicted-screenshot.PNG)

Scroll down to the "Event Log" where you can view more detailed event information on the time it was seen, the exact pod name, etc.
![](../static/monitoring/event-log.PNG)

### Temporary (/tmp) File Sizes ###
In some cases, the default size of the mounted `/tmp` directory may not be large enough for an application. This may happen if an application creates large temporary files such as large spreadsheets, etc. for users to download. If the file save/creation causes pods to crash, you may need to increase the size of the `/tmp` directory.

Example of pod eviction due to tmp directory reaching its limit:
![](../static/troubleshooting/tmp-dir-eviction.png)

To do so:
1. Determine the necessary size of the volume based on the size of the file(s) that are generated by the application. The below example would allow temporary files up to 50MB.
   !!!warning
   Volumes should always be sized appropriately. Larger volumes may impact pod scheduling. If the Kubernetes nodes don't have enough resources to satisfy the increased sizeLimit, pod scheduling may be delayed or fail.
   !!!
2. Use the following `volumes` setting within your `appInfo.json`:
```json
  {
    ...,
    "volumes": {
        "tmp": {
            "mountPath": "/tmp",
            "size": "50Mi"
        }
    }
}
```

### Memory Constraints ###
Below are some details on how memory is managed within the application containers. You may see the "Container OOM Killed" count in Grafana show a number greater than zero when this has occurred.

If a pod uses too much memory, it will get shut down by Kubernetes for exceeding the memory limit that was set for it.

__Java-based Applications__
1. The memory usage is split in the following manner:
    1. JVM
      - The JVM memory is pegged to x% of the container memory. This is a configurable parameter. See `MaxRAMPercentage` in your `appInfo.json` `javaOpts` for an example.
       - Java manages memory so that if you use too much, you get a Java OOM message. That should __not__ happen with single applications running in a JVM, but could be seen if memory can't be released (such as a large file getting uploaded and stored in memory for processing, etc.)
            - See garbage collection logs under `/tmp/gc.log` for details on JVM memory usage. This can help identify if there is a memory constraint.
    1. Non-JVM
        1. Apache
        1. OS
        1. Non-JVM processes can be what puts the container memory over the limit.
!!!
Thus __REDUCING__ the MaxRAMPercentage of the JVM can help to stabilize the application.
!!!

In cases where the application is running into Out of Memory errors, the following should be looked at. For well-designed applications, the default settings should be sufficient.

1. Ensure your application is not holding large objects in memory. Investigate any problem areas of the application that tend to trigger the issue. Typical areas where this is encountered are things like processing files, large database queries, etc.
    - Add additional logging or debug messages around problem areas to further narrow down potential issues.
    - Make adjustments to your application to more efficiently handle the requests.
1. Use Docker Compose to [run the application locally](../getting-started/containerization.md) to troubleshoot.
1. ***Only if all other attempts have been made to fix without simply increasing the memory***:
   - Update `appInfo.json` to adjust memory requests and/or limits (`containerResources`)
   - Re-deploy the application to utilize the updated settings

## Site Can't be Reached ##
These types of errors are seen if DNS configuration is incorrect OR user is not connected to VPN or on-prem network in cases where a site is internally facing.
#### Server IP address could not be found ####
![](../static/troubleshooting/site-unreachable-ip.PNG)

#### Connection Timed Out ####
![](../static/troubleshooting/site-unreachable.PNG)

1. Ensure the user is connected to VPN or an on-prem Medtronic network if your application is internally facing.
1. Check FluxConfigs to confirm if application is on a correct ALB by checking the `ingress.yaml` `albName`. If site should be reachable outside VPN or on-prem networks, this value should start with `ext`.
   - If it does not match the desired setup, make sure your `environmentInfo.json` file contains the `externalAccess` setting and re-run the manage-namespace job.
1. Use `nslookup` from a terminal window on your machine to check that domain is found: `nslookup clm.dev.argo-dev.eks.mdtcloud.io`
    - If `nslookup` returns a response like this: `*** dns-wby.medtronic.com can't find clm.dev.argo-dev.eks.mdtcloud.io: Non-existent domain`, submit a ticket with Infra-Argo-Global to check AWS Route53 records for the application. Confirm CNAME, TXT, and A records exist and contain the proper hostname details.

!!!warning Warning
A change to this configuration will result in the application being moved to a different load balancer. If using `alternateHostname` this may break DNS routing to your application. Submit a [request to modify](../how-to/web-access-hostnames.md) the existing DNS record.
!!!

## UnsatisfiedLinkError ##
If seeing an issue such as this:
```properties
java.lang.UnsatisfiedLinkError: /opt/java/openjdk/lib/amd64/libfontmanager.so: Error loading shared library libgcc_s.so.1: No such file or directory (needed by /opt/java/openjdk/lib/amd64/libfontmanager.so)
```

You can add additional OS packages to your Dockerfile. Depending on the OS and its package manager, for example:
```Dockerfile
RUN apk add font-dejavu libgcc
```

## 503 Service Unavailable ##
A 503 error typically means the Application Load Balander can't get to the application pods. Some things to check if you are receiving a '503 Service Unavailable' message for your application. Submit a ticket with Infra-Argo-Global to confirm:
1. Ensure you are using the correct hostname to access the application.
    - If you have *not* configured an alternate hostname, the default format for hostname is as follows:
        DEV: <appName>.dev.argo-dev.eks.mdtcloud.io  (i.e. clm.dev.argo-dev.eks.mdtcloud.io if "appName" in appInfo.json is "clm")
        TESTING: <appName>.testing.argo-dev.eks.mdtcloud.io
        STAGING (quarantine): <appName>.staging.argo-quarantine-prd.eks.mdtcloud.io
        STAGING: <appName>.staging.argo-prd.eks.mdtcloud.io
        RELEASE (quarantine): <appName>.staging.argo-quarantine-prd.eks.mdtcloud.io
        RELEASE: <appName>.release.argo-prd.eks.mdtcloud.io
        PRODUCTION (quarantine): <appName>.production.argo-quarantine-prd.eks.mdtcloud.io
        PRODUCTION: <appName>.production.argo-prd.eks.mdtcloud.io
    - If using alternate hostname, the host should be listed in the application's `environmentInfo.json` file.
1. Check application logs to see if the application is starting up correctly. This may identify whether there were issues with configuration such as DB credentials. Ensure manage-namespace is run with the proper credentials for all DB connections.
1. Check Event Log, App Release Status, and Ingress Release Status log panels in your [Grafana](../monitoring/03-logging.md) dashboard. This would show events such as pods starting, failing health checks, etc. Release Status logs show the current status of the application and load balancer rule releases and whether it is in-sync with the desired state or not.
1. The FluxConfig repo's ingress.yaml for this application has:
   - A valid `certificateArn` defined and matches the hostname being used.
      - If not, check GitLab Jobs to see if something went wrong with SSL Certificate or Route 53 entries (if not a custom hostname).
   - An `albName` assigned
      - This can be used to then look up the ALB in the AWS Console through EC2 > Load Balancers
1. Confirm ALB the app is assigned to has application specific rules defined on it.
   - If not, use K9s (if available in your environment) or the [Grafana Dashboard](https://g-05c60f9e9b.grafana-workspace.us-east-1.amazonaws.com/d/flux-cluster/flux-cluster-stats?orgId=1&refresh=30s&var-namespace=All) to check:
      - __Kustomizations__
         - Confirm kustomization status for the associated flux repo (argononprod, argoquarantine, or argoprod) shows a Ready status of 'True'. Drill in if any errors are shown to determine root cause.
           ![](../static/troubleshooting/kustomization-list.PNG)
         - The Status will show the latest applied revision. This ID should correspond to the latest commit SHA in the GitLab FluxConfig project repo as a way to confirm which revision it has applied.
         - If error is tied to a specific configuration file in the flux repo, you can use the kustomization.yaml to comment out the problem file and prevent it from being persisted to the cluster. This is a temporary solution to allow other changes to persist, the root cause of the error should be investigated and fixed.
      - __HelmRelease__
         - Confirm a webdev-ingress helm release exists for the application. Ready status should show 'True'. Drill in to the record using `Enter`. If any errors are shown to determine root cause.
           ![](../static/troubleshooting/helm-failure.PNG)
         - If it is in an error status due to an immutable field, you can use `Ctrl+D` to delete the helm release and allow flux to re-create it.
            !!!danger Danger
            Deletion of a HelmRelease will cause a temporary outage to the application. This outage should last no more than 10 minutes, but plan and notify your application users accordingly.
            !!!
      - __Pods__
        - Check that pod(s) are up and running for an application. If not, drill into logs to determine cause for startup failures.
      - __Ingresses__
         - App is listed and has an assigned ALB under `Address`. If it does not, drill into the record to see if there are listed errors and correct them through re-running manage-namespace or updates in the FluxConfig repository.
           ![](../static/troubleshooting/ingress-list.PNG)
      - __Services__
         - Find service for the application and hit `Enter`.
           ![](../static/troubleshooting/service-list.PNG)
         - Confirm the service is able to see the pod(s) for the application by hitting `Enter` after selecting the app/namespace. This checks that it is able to find pod(s) matching the required criteria to route traffic to from the ingress.
           ![](../static/troubleshooting/service-pods.PNG)
1. From a running application pod, test a curl to the build.json returns a response: `curl http://localhost/<contextRoot>/build.json`
1. Check Apache access.log (`/var/log/apache2/access.log`) to see if requests are reaching it
    - Checks to build.json with `?ALB` or `?ELB` come from the load balancer, meaning requests are getting through.

## Ingress Issues ##

#### FailedBuildModel: deletion_protection is enabled ####
If the following error is present on an ingress:
```
FailedBuildModel: Failed build model due to deletion_protection is enabled, cannot delete the ingress: <ingress-name>
```

This means the underlying ALB has deletion protection enabled, which is preventing the AWS Load Balancer Controller from reconciling the ingress.

**For application teams:** Submit a ticket to the **Infra-Argo-Global** team for assistance resolving this error.

==- Support Team Instructions
Resolve this by temporarily disabling deletion protection on the associated ALB ingress configuration in the `flux-gitops` repo:

1. Locate the affected ingress in the [flux-gitops repo](https://medtronic.gitlab-dedicated.com/bcp_web/devops/fluxconfigs/flux-gitops/) under the path:
   ```
   /clusters/<cluster-name>/infrastructure/ingress/...
   ```
2. Find the `alb.ingress.kubernetes.io/load-balancer-attributes` annotation on the ingress resource.
3. Temporarily set `deletion_protection.enabled=false` within that annotation value, for example:
   ```yaml
   alb.ingress.kubernetes.io/load-balancer-attributes: deletion_protection.enabled=false,...
   ```
4. Commit and push the change. Confirm the ingresses are reconciling successfully (Ready status returns to `True`).
5. Once reconciliation is confirmed successful, re-enable the flag by setting `deletion_protection.enabled=true` in the same annotation and commit the change.
===

## 403 Forbidden ##
If receiving a 403 forbidden error, this typically means a Web Application Firewall (WAF) rule within AWS may be blocking the request. There are a [standard set of rules](https://cloudservices.medtronic.com/reference_implementations/ri_aws_waf/#aws-managed-rule-requirements) that are implemented and evaluated on every request, however in some cases exceptions may need to be granted.

1. Check WAF logs in Grafana by going to your application's dashboard. Scroll towards the bottom of the page to find the "WAF Logs / Requests" panel.
2. Use the filters on the columns to filter by:
    * __Action__: BLOCK
    * __Host__: the hostname for the application / environment you're looking for
    * __URI__: the specific URI that the error was seen on
3. See the WAF Rule column to identify what rule blocked the request.

Submit a ticket to Infra-Argo-Global for assistance adding an exception to the WAF rules. Include the following details:
    1. Name of the application, hostname, environment where the issue was found
    1. Date(s) and Time(s) when the 403 was seen.
    1. Any specific URL/URIs for the application that cause the 403.
    1. The WAF Rule blocking the request - based on the above Grafana log search.
    1. Steps to reproduce (if possible).

==- Web Support Team Instructions
1. Locate the Web ACL used by the application/ALB. This can be found by looking at the `ingress.yaml` for the application in the FluxConfigs repo.
   ![](../static/troubleshooting/waf-acl-ingress-annotation.PNG)
1. From the AWS Console, locate the same WAF and click on the name to view details.
   ![](../static/troubleshooting/waf-acl-match-ingress.PNG)
1. From the "Overview" tab, scroll down to the "Sampled requests" area. Here you can search for a specific URI or Action. For example, type "BLOCK" in the search field to see blocked requests. These may not appear in time linear order, but should provide a sample of blocked requests. Similarly, searching for a specific URI can help identify behavior of a specific endpoint.
1. If a request is being blocked and requires an exception, identify the "Rule inside rule group" column for it to determine what rule is blocking. In the below case, this is "SizeRestrictions_BODY" rule in "AWS-AWSManagedRulesCommonRuleSet".
   ![](../static/troubleshooting/waf-acl-block-sample.PNG)
1. Open associated Terraform Deployment project for the AWS account:
    * [it-argo-dev-mdt](https://code.medtronic.com/bcp_web/devops/infrastructure/terraform-deployments/aws-it-argo-dev-mdt)
    * [it-argo-prod-mdt](https://code.medtronic.com/bcp_web/devops/infrastructure/terraform-deployments/aws-it-argo-prod-mdt)
1. Create a new branch of the above project to make your changes in.
1. Open the `terraform.tfvars` file in the project in the created branch.
1. Locate the specific rule and rule set identified in Step 4 that caused the blocking. Find that rule block in the `terraform.tfvars` file. In this example, search for "AWSManagedRulesCommonRuleSet".
   ![](../static/troubleshooting/waf-tf-locate-rule.PNG)
1. Look for an existing block as shown above that is tied to the name of the rule that was blocking (in this case "SizeRestrictions_BODY"). If one does not exist, copy an existing name/action block and add one with the associated name:
   ![](../static/troubleshooting/waf-tf-rule-override.PNG)
1. In most cases, we then want to then put in a specific rule to only allow an exception for the specific application or URI(s) that require an override. In general this should always be done so that we do not introduce additional attack vectors to other applications or URIs that do not need an exception.
1. Check whether an existing override already exists for that rule or if a new one is required.
   - In the `terraform.tfvars` file, locate the `custom_waf_rules` section and search for the override rule name, in this case: "AWSManagedRulesCommonRuleSet-SizeRestrictions_BODY"
     (i.e. `AWSManagedRulesCommonRuleSet-SizeRestrictions_BODY` would be an override rule for the SizeRestrictions_BODY rule in the `AWSManagedRulesCommonRuleSet`.)
1. If an existing override rule exists, copy/paste a new match_pattern/match_string block under the associated rule and adjust the "match_string" as needed to match the override you require.
   ![](../static/troubleshooting/waf-tf-rule-definition.PNG)
1. If a new override rule is required because one does not yet exist, copy the last `{}` block under `custom_waf_rules` to define a new one. Then alter the following fields:
   ![](../static/troubleshooting/waf-tf-new-rule.PNG)
   1. Name the new rule following the naming convention of the others (AWSxxxxRuleSet-RuleNameToOverride).
   1. Update the `label_match_key` to the key for the rule being overridden (i.e. `awswaf:managed:aws:core-rule-set:SizeRestrictions_Body` for SizeRestrictions_BODY). You can use the AWS console view to find the correct awswaf key to use.
   1. Alter the `exceptions` list to contain just the list of required exceptions for this rule.
1. Run `terraform fmt` to format the files before committing.
1. Commit your changes to the `terraform.tfvars` file to the branch.
1. Upon check-in of the changes, this should automatically kick off a pipeline in the associated GitLab project which will validate the format of the terraform script. Ensure this pipeline succeeds before continuing with the merge.
1. Create a merge request to the `main` branch of the project. Select `Merge when pipeline succeeds`. This should create a few new pipelines. The last pipeline created should then contain a "deploy:apply" job that you can push the "play" button to apply the WAF rule changes.
   ![](../static/troubleshooting/tf-pipeline.PNG)
1. After apply succeeds, you can then navigate to the AWS console to confirm the rule changes are applied.
   !!!warning
   All WAF changes must now be done through the Terraform projects. Any changes not made through Terraform will be reverted when the project does its daily apply step to ensure the configurations remain in sync with the Terraform configurations.
   !!!
1. Work with the application team to re-test the problem area(s) to confirm if the issue has been resolved by the added exception rules.
===

## Security & Vulnerability Issues ##

### Pipeline Blocked - "Policy Evaluation: FAILED" ###

If your pipeline fails with a "Policy Evaluation: FAILED" message in staging, release, or production environments, this means your application has security vulnerabilities that are past their remediation due dates.

**Check:**
1. Your `DATA_CLASSIFICATION` setting in `.gitlab-ci.yml` - this determines which vulnerability severities block your pipeline
2. Which vulnerabilities are overdue in your GitLab Work items:
   - Go to your project's **Issues** page
   - Filter by labels: `security` and `overdue`
3. Whether Low severity issues are blocking (only occurs if your app is `HIGHLY_SENSITIVE`)
   - HIGHLY_SENSITIVE apps must remediate ALL vulnerabilities including Low severity
   - SENSITIVE, INTERNAL_USE_ONLY, and PUBLIC apps only need to remediate Critical, High, and Medium

**Fix Options:**
1. **Remediate the vulnerability** - Update dependencies or fix code issues:
   ```bash
   # For container vulnerabilities
   # Update package version in Dockerfile

   # For application dependencies
   # Update version in pom.xml, build.gradle, package.json, etc.

   # Commit with conventional format
   git commit -m "fix: upgrade vulnerable package to secure version"
   ```
   The pipeline will automatically re-scan and close resolved issues.

2. **Request a Policy Exception (PER)** if you cannot remediate by the due date:
   - Document why the vulnerability can't be fixed immediately
   - Identify compensating controls in place
   - Submit PER in LogicGate with business justification
   - Once approved, add `PER::<PER-ID>` label to GitLab Work items

[!ref Complete PER Guide](../security/policy-exception-request.md)

**Related Documentation:**
- [Security & GitLab Work items](../cicd-pipeline/security-gitlab-issues.md) - Detailed vulnerability remediation workflow
- [Data Classification](../getting-started/gitlab-repository-configuration.md#determining-your-data-classification) - Understanding your security requirements

### Security Issues Not Being Created ###

If vulnerabilities are detected by scans but GitLab Work items are not being created automatically:

**Check:**
1. Did the `security-issue-management` job succeed in your pipeline?
   - Go to your pipeline view and check the security stage
   - Click on the job to view logs for any errors

2. Does `GITLAB_TOKEN` have the correct scope?
   - The token needs `api` scope to create and update issues
   - Contact your GitLab admin or platform team to verify

3. Does the `svc-ws-git1` service account have proper permissions?
   - Service account must have **Maintainer** role on your project
   - Check in `Settings → Members` for your project

**Fix:**
1. If service account is missing, add it:
   - Go to `Settings → Members`
   - Click **Invite members**
   - Search for `svc-ws-git1`
   - Set role to **Maintainer**
   - Click **Invite**

2. If token scope is incorrect, submit a ticket to Infra-Argo-Global for assistance

3. Re-run your pipeline after fixing permissions

### False Positive Vulnerabilities ###

If a vulnerability is flagged by scanners but doesn't actually apply to your application (false positive):

**Steps to Request Review:**
1. **Document the false positive** in the GitLab Work item:
   - Comment with evidence why it's a false positive
   - Examples:
     - "This package is only used in test/dev dependencies, not in production"
     - "The vulnerable code path is not actually used by our application"
     - "This CVE only affects Windows environments; we run on Linux"

2. **Submit a Policy Exception Request (PER)**:
   - In LogicGate, mark it as a false positive
   - Provide technical justification from step 1
   - Attach supporting evidence (code snippets, dependency trees, etc.)

3. **GCISO Review**:
   - GCISO team will review your justification
   - If valid, they'll close the issue and may add it to exclusion lists
   - If not valid, you'll need to remediate or provide additional justification

**Common False Positives:**
- Transitive dependencies that aren't actually used
- Test-only dependencies included in production scans
- Vulnerabilities in code paths your application never executes
- OS packages that don't affect containerized applications

[!ref Policy Exception Request Process](../security/policy-exception-request.md)

### Contrast Security Issues ###

Contrast Security provides runtime vulnerability detection for your application. Issues detected by Contrast are automatically synced to GitLab Work items.

**Remediation Process:**
1. **Review in Contrast Console**:
   - Log into [Contrast Security Console](https://app.contrastsecurity.com)
   - Find your application
   - Review vulnerability details with full execution traces

2. **Fix the vulnerability** in your code:
   - Contrast provides detailed remediation guidance
   - Shows exact code paths where vulnerability occurs
   - Includes recommendations for secure alternatives

3. **Mark as Remediated in Contrast**:
   - After applying the fix, mark the issue as **Remediated** in the Contrast dashboard
   - Contrast will communicate the status back to the pipeline
   - The corresponding GitLab Work item will be updated automatically

4. **Deploy and verify**:
   - Deploy your fixed code to the environment
   - Contrast will re-assess during runtime
   - If the vulnerability is gone, the issue closes automatically

**Common Contrast Findings:**
- SQL Injection vulnerabilities
- Cross-Site Scripting (XSS)
- Insecure deserialization
- Command injection
- Path traversal

For complete details on the Contrast Security integration, see [Security & GitLab Work items](../cicd-pipeline/security-gitlab-issues.md).

---

## java.net.BindException ##
If seeing the below error messages in the application log:
```properties
java.net.BindException: Address is in use (Bind failed)
```

![](../static/troubleshooting/java-bind-exception.png)

To fix:
1. Submit a ticket to `Infra-Argo-Global` and provide:
    - The error message
    - The application name and application environment where this error was seen
    - Request that the application's HelmRelease be deleted.
2. Once the HelmRelease has been deleted by the support team, re-deploy the application to resolve the error.
