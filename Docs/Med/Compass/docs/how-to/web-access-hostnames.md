---
label: Web Access & Hostnames
icon: globe
order: 40
---

# Web Access & Hostnames

Configure how your application is reached in a web browser. This guide covers the “front door” for web access—routing, HTTPS, and DNS—for applications exposed through an AWS Application Load Balancer with a certificate-backed hostname.

## What You'll Configure

An **Ingress** is a Kubernetes resource that routes HTTP/HTTPS traffic from the internet to your application. Think of it as a smart reverse proxy that handles:
- Domain routing
- SSL/TLS termination
- Load balancing
- Path-based routing

This guide walks you through the complete process from SSL certificate creation to DNS configuration.

[!ref target="blank" text="Learn more about Ingress" icon="link-external"](https://kubernetes.io/docs/concepts/services-networking/ingress/)

---
## Common Ingress Patterns

The patterns below apply after you choose either certificate path.

### Recommended: Base + Environment Patches Pattern

For applications deployed across multiple environments (dev, testing, staging, production), use Kustomize's strategic merge patches to avoid duplicating common configuration.

**Structure:**
```
k8s/
├── base/
│   └── ingress.yaml          # Common annotations and path rules
├── dev/
│   ├── ingress-patch.yaml    # Dev-specific hostname and certificates
│   └── kustomization.yaml
├── testing/
│   ├── ingress-patch.yaml
│   └── kustomization.yaml
├── staging/
│   ├── ingress-patch.yaml
│   └── kustomization.yaml
└── production/
    ├── ingress-patch.yaml
    └── kustomization.yaml
```

**Base Ingress** (`k8s/base/ingress.yaml`) - Common configuration shared across all environments:

```yaml
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: einstein
  annotations:
    # Root redirect
    alb.ingress.kubernetes.io/actions.redirect-to-context-root: '{"type":"redirect","RedirectConfig":
      { "Protocol": "HTTPS", "host": "#{host}", "path": "/einstein/", "StatusCode":
      "HTTP_301"}}'
    # Shared ALB group (internal apps)
    alb.ingress.kubernetes.io/group.name: intnon001
    # Health check
    alb.ingress.kubernetes.io/healthcheck-path: /einstein/build.json?ALB
    # Network access (Medtronic internal only)
    alb.ingress.kubernetes.io/inbound-cidrs: 10.0.0.0/8
    # IP address type
    alb.ingress.kubernetes.io/ip-address-type: ipv4
    # Listener ports
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTPS": 443}, {"HTTP": 80}]'
    # Load balancer attributes
    alb.ingress.kubernetes.io/load-balancer-attributes: routing.http2.enabled=true,idle_timeout.timeout_seconds=600
    # Internal ALB (Medtronic network only)
    alb.ingress.kubernetes.io/scheme: internal
    # SSL policy
    alb.ingress.kubernetes.io/ssl-policy: ELBSecurityPolicy-FS-1-2-Res-2020-10
    # Success codes
    alb.ingress.kubernetes.io/success-codes: 200,302
    # Target type
    alb.ingress.kubernetes.io/target-type: ip
spec:
  ingressClassName: alb-webdev
  rules:
  - http:
      paths:
      # Redirect root to context root
      - backend:
          service:
            name: redirect-to-context-root
            port:
              name: use-annotation
        path: /
        pathType: ImplementationSpecific
      # App context root
      - backend:
          service:
            name: einstein
            port:
              number: 80
        path: /einstein
        pathType: Prefix
      # Static files
      - backend:
          service:
            name: einstein
            port:
              number: 80
        path: /einstein-static
        pathType: Prefix
```

**Environment Patch** (`k8s/dev/ingress-patch.yaml`) - Only environment-specific overrides:

```yaml
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: einstein
  annotations:
    # Dev environment certificate
    alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:us-east-1:389242548790:certificate/fa071e39-6ef1-4c9d-b044-ea593d0f21ec
    # Dev environment WAF
    alb.ingress.kubernetes.io/wafv2-acl-arn: arn:aws:wafv2:us-east-1:389242548790:regional/webacl/TF-argo-custom/da31944e-a61d-4602-8df6-43458992be69
spec:
  rules:
  - host: einstein.dev.argo-dev.eks.mdtcloud.io
```

**Update kustomization.yaml** to include the patch:

```yaml
# k8s/dev/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
namespace: argo-einstein-dev
resources:
- ../base

patchesStrategicMerge:
- deployment-patch.yaml
- ingress-patch.yaml  # Add this

images:
- name: einstein
  newName: case.artifacts.medtronic.com/einstein
  newTag: v1.0.0
```

**Benefits of this approach:**
- ✅ **DRY principle** - Common config defined once in base
- ✅ **Easy updates** - Change ALB settings in one place
- ✅ **Clear separation** - Environment-specific values isolated in patches
- ✅ **Reduced errors** - Less duplication = fewer mistakes

### Making an Environment Externally Available

To make a specific environment (e.g., production) accessible from the public internet, override the `scheme`, `inbound-cidrs`, and `group.name` annotations in that environment's patch:

```yaml
# k8s/production/ingress-patch.yaml
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: einstein
  annotations:
    # Production certificate
    alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:us-east-1:872019488961:certificate/PROD_CERT_ARN
    # Production WAF
    alb.ingress.kubernetes.io/wafv2-acl-arn: arn:aws:wafv2:us-east-1:872019488961:regional/webacl/TF-argo-custom/PROD_WAF_ARN
    # Override: Make internet-facing (external access)
    alb.ingress.kubernetes.io/scheme: internet-facing
    # Override: Allow public internet traffic
    alb.ingress.kubernetes.io/inbound-cidrs: 0.0.0.0/0
    # Override: Use external ALB group
    alb.ingress.kubernetes.io/group.name: extprd001
spec:
  rules:
  - host: einstein.medtronic.com
```

!!!warning ALB Change Requires DNS Update
**Critical:** Changing the `scheme` (internal ↔ internet-facing) or `group.name` will create a **new Application Load Balancer** with a different DNS hostname.

**Impact:**
- The ALB hostname will change from `k8s-intnon001-...` to `k8s-extprd001-...`
- Any DNS CNAME records pointing to the old ALB hostname **must be updated**
- Users will experience downtime until DNS is updated

**Before changing scheme or group:**
1. Submit [ServiceNow DNS request to modify CNAME](#modify-existing-hostname-migration)
2. Coordinate timing with your team
3. Plan for brief outage during DNS cutover
4. Update DNS immediately after deploying the Ingress change
!!!

**Other annotations that merge (safe to override per environment):**
- `certificate-arn` - Different certs per environment
- `wafv2-acl-arn` - Different WAF rules per environment
- `healthcheck-path` - Different health checks (rare)
- `load-balancer-attributes` - Environment-specific timeouts/settings

**Annotations inherited from base (unless overridden):**
- `target-type: ip`
- `ssl-policy`
- `listen-ports`
- `success-codes`
- All other annotations not specified in patch

### Multiple Hostnames

Support multiple domains on the same application:

```yaml
spec:
  rules:
  - host: myapp.medtronic.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: myapp
            port:
              number: 8080
  - host: myapp-internal.medtronic.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: myapp
            port:
              number: 8080
```

### Shared ALB (Multi-Tenant Clusters)

To reduce the number of load balancers in multi-tenant clusters, multiple applications can share a single ALB by using **Ingress Groups**. All Ingress resources that should share the same ALB must use the **same group name**.

**Key requirement:** Use a shared group name provided by the Platform Team for your cluster/environment.

```yaml
metadata:
  annotations:
    # Share an ALB across multiple apps
    alb.ingress.kubernetes.io/group.name: shared-alb-dev
    # Optional: order rules when multiple ingresses share the ALB
    alb.ingress.kubernetes.io/group.order: "10"
```

**Notes:**
- All shared ingresses must use compatible settings (scheme, IP target type, and listener ports).
- If multiple applications must be served under the **same hostname**, they must be placed on the **same ALB** (same `alb.ingress.kubernetes.io/group.name`), because a DNS CNAME for that hostname can only point to one ALB DNS target.
- Use unique hostnames per app (host-based routing) to avoid conflicts.
- Coordinate certificate usage with the Platform Team (single wildcard cert or multiple certs on the same ALB).

Contact the [Platform Team](mailto:dl.itargocoreteam@medtronic.com) to confirm the correct shared group name for your cluster.

### Path-Based Routing

Route different URL paths to different services:

```yaml
spec:
  rules:
  - host: myapp.medtronic.com
    http:
      paths:
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: myapp-api
            port:
              number: 8080
      - path: /ui
        pathType: Prefix
        backend:
          service:
            name: myapp-frontend
            port:
              number: 3000
      - path: /
        pathType: Prefix
        backend:
          service:
            name: myapp-frontend
            port:
              number: 3000
```

---
### ALB Annotations

### Required ALB Ingress Annotations

The annotations below are **required** for ALB-based ingresses in Compass CI. Use the sample values as a template and replace with cluster-specific values provided by the Platform Team.

**Required:**

```yaml
metadata:
  annotations:
    # Internet-facing or internal (required)
    alb.ingress.kubernetes.io/scheme: "internet-facing"

    # Allowed source CIDRs (required)
    alb.ingress.kubernetes.io/inbound-cidrs: "0.0.0.0/0"

    # Listener ports (required)
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTPS": 443}, {"HTTP": 80}]'

    # Enforce HTTPS redirect (required)
    alb.ingress.kubernetes.io/actions.ssl-redirect: >
      {"Type":"redirect", "RedirectConfig":{"Protocol":"HTTPS", "Port":"443", "StatusCode":"HTTP_301"}}

    # Target type (required)
    alb.ingress.kubernetes.io/target-type: "ip"

    # WAF (required) - automatically assigned by the assign-alb-* job (extends .assign-alb-group)
    alb.ingress.kubernetes.io/wafv2-acl-arn: "arn:aws:wafv2:us-east-1:123456789012:regional/webacl/your-waf/abc-123"

    # Load balancer attributes (required)
    # Must include deletion protection and access logs
    alb.ingress.kubernetes.io/load-balancer-attributes: >
      deletion_protection.enabled=true,access_logs.s3.enabled=true,access_logs.s3.bucket=tf-argo-it-argo-prod-mdt-alb-logs,access_logs.s3.prefix=ALBLogs
```

**Required best practices in `load-balancer-attributes`:**
- `deletion_protection.enabled=true`
- `access_logs.s3.enabled=true`
- `access_logs.s3.bucket=<cluster-log-bucket>`
- `access_logs.s3.prefix=ALBLogs`

---

### Recommended ALB Ingress Annotations

Use these annotations when applicable or when required by the Platform Team for your cluster.

```yaml
metadata:
  annotations:
    # Shared ALB group (required for multi-tenant shared ALBs) - automatically assigned by the assign-alb-* job (extends .assign-alb-group)
    alb.ingress.kubernetes.io/group.name: "extprd001"

    # IP address type
    alb.ingress.kubernetes.io/ip-address-type: "ipv4"

    # SSL policy
    alb.ingress.kubernetes.io/ssl-policy: "ELBSecurityPolicy-FS-1-2-Res-2020-10"

    # Success codes for health checks
    alb.ingress.kubernetes.io/success-codes: "200,302"

    # Custom redirects (example favicon redirect)
    alb.ingress.kubernetes.io/actions.favicon: >
      {"Type":"redirect", "RedirectConfig": {"host":"www.medtronic.com", "path":"/content/dam/medtronic-com/favicon.ico", "Protocol":"HTTPS", "Port":"443", "StatusCode":"HTTP_301"}}
```

Confirm required values (WAF ARN, log bucket, shared ALB group name, and allowed CIDRs) with the [Platform Team](mailto:dl.itargocoreteam@medtronic.com).

---

## Decide Which Certificate Path to Use

Choose one certificate management path first, then follow the implementation steps for that path.

| Path | Use this when | Maintenance | Cost |
|---|---|---|---|
| **Automated ACM via CI** | The application is internal-facing or otherwise low-visibility and it is acceptable to have a `*.eks.mdtcloud.io` suffixed hostname | Low maintenance: certificate provisioning and renewals are automatic | Free |
| **Imported Medtronic-Signed Certificate** | Your application must have a `*.medtronic.com` hostname and your user base is *only* internal Medtronic users | Manual maintenance: certificate renewal and update are required at least every 2 years | Free |
| **Imported DigiCert Certificate** | Your application must have a `*.medtronic.com` hostname and your user base includes external users | Manual maintenance: certificate renewal and update are required at least every 2 years | ~$200/year ([KB details](https://medtronicprod.service-now.com/it?id=mdtit_kb_article&sys_id=88f43e51dbbea3c0c19522e648961986)) |

## Automated ACM Certificate Provisioning

Use this path when you want Compass CI to provision and manage the ACM certificate for your hostname.

This automation can:
- Discover hostnames from `k8s/<env>` Ingress files
- Create or update certificate Terraform in the Terraform deployments repo
- Create or update a cert MR
- Add or update `alb.ingress.kubernetes.io/certificate-arn` in your Ingress patch

### When to choose this path

- The application is internal-facing or otherwise low-visibility, and an ACM-managed certificate satisfies the requirement
- You want the certificate and ARN wiring handled by CI instead of by a manual certificate request and import process
- You want automated renewal of the cert for less ongoing maintenance
- You want a no-cost certificate option

### Prerequisites

- Your project includes `aws.yml` from `bcp_web/devops/semantic-release`
- CI variables are set for:
  - `CLUSTER_NAME` (environment specific)
  - `FLUX_GITOPS_REPO_RW_TOKEN` (group-level token provided by Platform Team during onboarding)
  - `TERRAFORM_REPO_RW_TOKEN` (group-level token provided by Platform Team during onboarding)

### Step 1: Provide the hostname

Choose one of these input methods.

#### Option A: Let the job discover the hostname from Ingress YAML

Use this when your app stores hostnames in `k8s/<env>/*.yaml` or `k8s/base/*.yaml`.

Example Ingress with hostname in `spec.rules[].host`:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: myapp
spec:
  rules:
  - host: myapp-dev.argo-dev.eks.mdtcloud.io
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: myapp
            port:
              number: 8080
```

#### Option B: Set `TARGET_HOSTNAME` variable explicitly in the .provision-cert-env job

Use this when you are not using `k8s/` manifests for discovery, or when you want to force one specific hostname.

```yaml
include:
  - file: aws.yml
    project: bcp_web/devops/semantic-release
    ref: main

provision-cert-dev:
  extends: [.dev, .provision-cert-env]
  variables:
    TARGET_HOSTNAME: "myapp-dev.argo-dev.eks.mdtcloud.io"
  rules:
    - if: $CI_COMMIT_BRANCH == "dev"
      changes:
        ...
```

### Step 2: Add the `.provision-cert-env` job

If you want CI to discover the hostname from Ingress YAML, use a job like this:

```yaml
include:
  - file: aws.yml
    project: bcp_web/devops/semantic-release
    ref: main

provision-cert-dev:
  extends: [.dev, .provision-cert-env]
  rules:
    - if: $CI_COMMIT_BRANCH == "dev"
      changes:
        - "k8s/dev/*.yml"
        - "k8s/dev/*.yaml"
        - "k8s/base/*.yml"
        - "k8s/base/*.yaml"
      when: on_success
    - if: $CI_COMMIT_BRANCH == "dev"
      when: manual
```

Adjust `rules.changes` to match the files that actually define the hostname for that environment. For example, if the hostname lives in `k8s/dev/ingress-patch.yaml`, you can scope the rule more narrowly:

```yaml
provision-cert-dev:
  extends: [.dev, .provision-cert-env]
  rules:
    - if: $CI_COMMIT_BRANCH == "dev"
      changes:
        - "k8s/dev/ingress-patch.yaml"
      when: on_success
    - if: $CI_COMMIT_BRANCH == "dev"
      when: manual
```

If you want to pass the hostname explicitly, use a job like this:

```yaml
provision-cert-dev-explicit-host:
  extends: [.dev, .provision-cert-env]
  variables:
    TARGET_HOSTNAME: "myapp-dev.argo-dev.eks.mdtcloud.io"
  rules:
    - if: $CI_COMMIT_BRANCH == "dev"
      when: manual
```

### Step 3: Run the pipeline and review the automated changes

When the job runs, it will automatically:
- Discover the hostname from Ingress or use `TARGET_HOSTNAME`
- Create or update the certificate infrastructure code in the Terraform repo so the certificate change is tracked and versioned
- Add or update `alb.ingress.kubernetes.io/certificate-arn` in your Ingress patch


## Imported Certificate (Medtronic Signed or DigiCert)

Use this path when your hostname requires imported certificate material (for example `*.medtronic.com`).

### When to choose this path

- Your application must have a `*.medtronic.com` hostname

### Prerequisites

- Your project includes `aws.yml` from `bcp_web/devops/semantic-release`
- Your cert job uses `.provision-cert-env` with import variables set
- Terraform Cloud variables are set so Compass CI can upsert workspace variables:
  - `COMPASS_CI_TFC_API_TOKEN`
  - `COMPASS_CI_TFC_ORG`
  - `COMPASS_CI_TFC_WORKSPACE_<CLUSTER_NAME>`
- The Ingress for the target environment uses the same hostname in `spec.rules[].host` that your certificate covers (CN/SAN)

### Step 1: Obtain the certificate

Choose the appropriate certificate type based on your application's access requirements.

| Type | Use Case | Provider | Cost |
|------|----------|----------|----------|
| **Medtronic Signed** | Internal Medtronic users only | [Medtronic Certificate Generator](https://certrequest.medtronic.com/cmsadminenroll) | Free |
| **Certificate Authority (CA) Signed** | External users or public internet | DigiCert via ServiceNow | ~$200 / year ([see KB article for details](https://medtronicprod.service-now.com/it?id=mdtit_kb_article&sys_id=88f43e51dbbea3c0c19522e648961986)) |

#### Medtronic-Signed Certificate

**Prerequisites:**
- Access to `openssl` (Linux, macOS, or Windows with Git Bash)
- Access to https://certrequest.medtronic.com/cmsadminenroll
  - If you don't have access, [request it via ServiceNow](https://medtronicprod.service-now.com/it?id=sc_cat_item&sys_id=0ee1285d1b414d506ed99603b24bcb3c)
  - Select: **Web Server Template**

##### Installing OpenSSL

**macOS:**
OpenSSL is typically pre-installed. Verify by running:
```bash
openssl version
```

If not installed, use Homebrew:
```bash
brew install openssl
```

**Windows:**

Option 1: **Git Bash (Recommended)** - OpenSSL is included with [Git for Windows](https://git-scm.com/download/win)

Option 2: **WSL (Windows Subsystem for Linux)** - Provides a full Linux environment:
```bash
wsl --install
sudo apt-get update && sudo apt-get install openssl
```

Option 3: **Direct Install** - Download from [slproweb.com/products/Win32OpenSSL.html](https://slproweb.com/products/Win32OpenSSL.html) and use the "Win64 OpenSSL" version.

##### Generate Private Key and CSR

1. Generate the private key locally:
   ```bash
   openssl genrsa -out myapp.key 2048
   ```

   This creates `myapp.key` containing text starting with `-----BEGIN RSA PRIVATE KEY-----`.

   !!!warning Keep this file secure
   You'll need to set this `.key` file into a CI/CD variable later for AWS import.
   !!!

2. Generate a certificate signing request:
   ```bash
   openssl req -new -sha256 -key myapp.key -out myapp.csr
   ```

3. Answer the prompts:
   ```
   Country Name: US
   State or Province: Minnesota
   Locality Name: Mounds View
   Organization Name: Medtronic, PLC
   Organizational Unit: (leave blank)
   Common Name: myapp.medtronic.com
   Email Address: your-team-dl@medtronic.com
   Challenge password: (leave blank)
   Optional company name: (leave blank)
   ```

   The CSR file will contain text starting with `-----BEGIN CERTIFICATE REQUEST-----`.

##### Submit to Medtronic Certificate Generator

1. Log into https://certrequest.medtronic.com/cmsadminenroll
2. Click **CSR Enrollment** in left navigation
3. Select **MDT-ICA2-7-WebServer** for Template
4. Select **MSPM1AESA219.core.medtronic.com\MDT Issuing CA 2-7** for Certificate Authority
5. Paste your CSR content in **CSR Content** field
6. Add **Subject Alternative Names:**
   - Click **+ Add**
   - Enter DNS name: `myapp.medtronic.com`
   - For additional environments, click **+ Add** again: `myapp-dev.medtronic.com`, `myapp-test.medtronic.com`
7. Enter **Email-Contact:** Your team DL
8. Enter **Server-name:** `MyAppName-ClusterName-EnvironmentName` (for example `Einstein-TF-argo-prd-production`)
9. Enter **Support-Group:** Your *application's* support group name
10. Enter **Certificate-Installation-Location:** AWS
11. Click **Add** under **Subject Alternative Names** and add at least one SAN (Subject Alternative Name) where the entry exactly matches the CN name.  Additional SANs can be added as needed.
12. Keep **Certificate Format** as **Base-64 encoded**
13. Click **Enroll**
14. Download the generated certificate file containing text that starts with `-----BEGIN CERTIFICATE-----`

##### Prepare files for CI import

Keep the following files ready for GitLab CI/CD file variables:
- `myapp.key` -> `COMPASS_CI_CERT_PRIVATE_KEY`
- `myapp.cer` or `myapp.pem` -> `COMPASS_CI_CERT`
- `MDT-Issuing-CA-2-7.crt` -> `COMPASS_CI_CERT_CHAIN`

#### DigiCert Certificate

Use this for externally accessible applications and user-bases.

!!!warning Annual Cost
There is cost associated with externally signed DigiCert SSL certificates. See [KB0045207](https://medtronicprod.service-now.com/it?id=mdtit_kb_article&sys_id=88f43e51dbbea3c0c19522e648961986) for details and cost explanation.
!!!

1. Navigate to ServiceNow: https://medtronicprod.service-now.com/it
2. Search for **Certificate**
3. Select **DigiCert SSL Certificate Request**
4. Fill out the form with your application details
5. Submit the request
6. Once fulfilled, download the certificate files

### Step 2: Add environment-scoped CI/CD variables

1. Add/update these GitLab CI/CD file variables with your certificate file content:
    - `COMPASS_CI_CERT` (certificate body `-----BEGIN CERTIFICATE----- ... -----END CERTIFICATE-----`)
    - `COMPASS_CI_CERT_CHAIN` (issuer/intermediate chain)
    - `COMPASS_CI_CERT_PRIVATE_KEY` (private key `-----BEGIN PRIVATE KEY----- ... -----END PRIVATE KEY-----`)

    **Example** showing setting the above variables scoped to the `dev` environment:
    ![](../static/certs/cert-import-gitlab-variables.png)

### Step 3: Add the `.provision-cert-env` job

Ensure your `.gitlab-ci.yml` contains a cert provisioning job for that environment that extends the provided template.

```yaml
include:
  - file: aws.yml
    project: bcp_web/devops/semantic-release
    ref: main

provision-cert-dev:
  extends: [.dev, .provision-cert-env]
  rules:
    - if: $CI_COMMIT_BRANCH == "dev"
      changes:
        - "k8s/dev/*.yml"
        - "k8s/dev/*.yaml"
        - "k8s/base/*.yml"
        - "k8s/base/*.yaml"
      when: on_success
    - if: $CI_COMMIT_BRANCH == "dev"
      when: manual
```

3. The job will:
    - Validate import inputs
    - Resolve hostnames from Ingress (or `TARGET_HOSTNAME` override)
    - Upsert certificate values into Terraform Cloud workspace variables
    - Ensure Terraform code for imported ACM cert resource exists
    - Assign / set the correct certificate-arn annotation on the Ingress

### Step 3: Ensure Ingress hostname alignment

In your target env Ingress patch, confirm `spec.rules[].host` matches the cert hostname you are importing. If this hostname is `*.medtronic.com`, import variables are required.

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: myapp
spec:
  rules:
  - host: myapp.medtronic.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: myapp
            port:
              number: 8080
```

### Step 4: Run the pipeline and review the automated changes
When the job runs, it will automatically:
- Ensure hostname and `COMPASS_CI_CERT*` variables are present
- Create or update the certificate infrastructure code in the Terraform repo so the certificate change is tracked and versioned
- Add or update alb.ingress.kubernetes.io/certificate-arn in your Ingress patch

### Step 5: DNS Configuration / Mapping
Continue with the DNS steps in [Common Deployment and DNS Steps](#common-deployment-and-dns-steps).

## Common Deployment and DNS Steps

After you complete either certificate path, use the steps below to expose the application and wire DNS.

### Required ALB Pipeline Jobs

Before deployment, ensure your pipeline includes:
- `validate-alb-prerequisites` (extends `.validate-alb-prerequisites`)
- An environment ALB assignment job such as `assign-alb-dev`/`assign-alb-testing`/`assign-alb-staging`/`assign-alb-production` (each extends `.assign-alb-group`)

Example `.gitlab-ci.yml`:

```yaml
include:
  - file: containers.yml
    project: bcp_web/devops/semantic-release
    ref: main

validate-alb-prerequisites:
  extends: .validate-alb-prerequisites

assign-alb-dev:
  extends: [.assign-alb-group, .dev]
  needs:
    - job: validate-alb-prerequisites

assign-alb-testing:
  extends: [.assign-alb-group, .testing]
  needs:
    - job: validate-alb-prerequisites

assign-alb-staging:
  extends: [.assign-alb-group, .staging]
  needs:
    - job: validate-alb-prerequisites

assign-alb-production:
  extends: [.assign-alb-group, .prod]
  needs:
    - job: validate-alb-prerequisites
```

### Step 1: Deploy Application

Deploy your application with the Ingress configuration through your GitLab CI/CD pipeline.

!!!warning ALB Group/Scheme Change Detected?
If your pipeline output indicates the app moved from one ALB group to another (for example `intnon001 -> extnon001` or `intnon001 -> intnon002`):

- **Imported Medtronic-signed or DigiCert certificate paths:** Treat this as a required DNS follow-up item. Any existing CNAME that pointed to the old ALB DNS hostname must be updated to the new ALB DNS hostname. Proceed to [Step 2: Request DNS CNAME](#step-2-request-dns-cname) immediately after deployment verification.
- **Automated ACM via CI path:** A manual DNS CNAME update is not required for this ALB switch workflow.
!!!

**Get the Load Balancer DNS:**

The ALB hostname is automatically generated and output by the `assign-alb-*` CI/CD job in your pipeline:

1. **Check pipeline job output:**
   - In GitLab, go to **CI/CD → Pipelines**
   - Find the most recent pipeline for your branch
   - Check the **infrastructure** stage
   - Click on the `assign-alb-*` (environment-specific) job
   - Look for the **"Resolved ALB addresses"** section in the job log

**Example job output:**
```
✅ Ingress einstein reconciled with address internal-k8s-intnon005-61cab6debf-329763559.us-east-1.elb.amazonaws.com

📌 Resolved ALB addresses:
  - einstein: internal-k8s-intnon005-61cab6debf-329763559.us-east-1.elb.amazonaws.com
```

This address (`internal-k8s-intnon005-61cab6debf-329763559.us-east-1.elb.amazonaws.com`) is your **Canonical Name** for the DNS request form.

**Fallback options:**
- Check in [Grafana](https://g-05c60f9e9b.grafana-workspace.us-east-1.amazonaws.com/) - Navigate to your application's dashboard and look for the Load Balancer hostname
- Contact **Infra-Argo-Global** via ServiceNow to retrieve your Load Balancer DNS hostname

Save this Load Balancer DNS - you'll use it as the **Canonical Name** in the DNS request.

---

### Step 2: Request DNS CNAME

This step is required for imported Medtronic-signed or DigiCert certificate paths when the ALB changed as part of deployment.

If you are using the automated ACM via CI path, you can skip this step and continue to [Step 3: Verify DNS](#step-3-verify-dns).

For imported certificate paths, if the ALB changed as part of deployment, use **Modify Existing Hostname (Migration)** below, even if the application name did not change.

### New Hostname (First Time Setup)

1. Navigate to ServiceNow: https://medtronicprod.service-now.com/it
2. Search for **DNS**
3. Select **Infoblox DNS and IP Address Service Request**
4. Fill out the form:
   - **Request Type:** Add a CNAME record
   - **Alias:** `myapp.medtronic.com`
   - **DNS Zone:** Internal and External (or just Internal for internal-only apps)
   - **Canonical Name:** The ALB hostname from your `assign-alb-*` job output (e.g., `internal-k8s-intnon005-61cab6debf-329763559.us-east-1.elb.amazonaws.com`)
   - **When would you like this worked performed?:** Enter the date/time you would like this done
5. Submit the request

### Modify Existing Hostname (Migration)

If your hostname is already in use elsewhere and you're migrating to Compass CI:

1. Follow the same steps as above but select **Modify a CNAME record**
2. Update the **Canonical Name** to the new ALB DNS hostname from your `assign-alb-*` job output (e.g., `internal-k8s-intnon005-61cab6debf-329763559.us-east-1.elb.amazonaws.com`)
3. Validate any additional hostnames (aliases) that should move with the same cutover
4. **Important:** Set **When would you like this worked performed?** to your go-live date/time
   - This timing is critical for cutover
   - Coordinate with your team to ensure application is ready

---

### Step 3: Verify DNS

Once the DNS request is fulfilled (usually within 1-2 business days):

```bash
nslookup myapp.medtronic.com
```

**Expected output:**
```
Server:  dns-cha.medtronic.com
Address:  10.20.246.170

Name:    internal-k8s-intnon005-61cab6debf-329763559.us-east-1.elb.amazonaws.com
Addresses:  52.23.45.67
            54.12.34.56
            3.89.123.45
Aliases:  myapp.medtronic.com
```

The `Name` should match the **Canonical Name** from your `assign-alb-*` job output, confirming traffic routes to your AWS Load Balancer.

---

## Testing Before DNS Cutover

If you're migrating an existing hostname, test your application before the DNS cutover.

### Edit Local Hosts File

**Windows:** `C:\Windows\System32\drivers\etc\hosts`
**Mac/Linux:** `/etc/hosts`

1. **Open the hosts file with administrator/root privileges**

2. **Get an IP address from your Load Balancer:**
   ```bash
  nslookup internal-k8s-intnon005-61cab6debf-329763559.us-east-1.elb.amazonaws.com
   ```

   Copy one of the returned IP addresses (e.g., `52.23.45.67`)

3. **Add entry to hosts file:**
   ```
   52.23.45.67   myapp.medtronic.com
   ```

4. **Save the file**

5. **Flush DNS cache:**
   - **Windows:** `ipconfig /flushdns`
   - **Mac:** `sudo dscacheutil -flushcache`
   - **Linux:** `sudo systemd-resolve --flush-caches`

6. **Test in browser:**
   - Navigate to `https://myapp.medtronic.com`
   - Check `/build.json` endpoint to confirm version

7. **Remove the hosts entry after testing:**
   - Comment out or delete the line you added
   - Flush DNS cache again

!!!warning Important
Remove the hosts file entry after testing to avoid conflicts when DNS cutover happens.
!!!

---

## Renew Imported Certificates

These renewal steps apply to the imported certificate path only.

SSL certificates expire annually. You'll receive email notifications before expiration.

### Renew Medtronic-Signed Certificate

1. Follow the same steps as [Medtronic-Signed Certificate](#medtronic-signed-certificate)
2. Generate new private key and CSR
3. Submit to Medtronic Certificate Generator
4. Send new certificate and private key to [Platform Team](mailto:dl.itargocoreteam@medtronic.com)
5. Platform Team will update the certificate in AWS (ARN remains the same)
6. No changes needed to your Ingress configuration

### Renew DigiCert Certificate

1. Follow renewal instructions in the email notification
2. Or submit a new DigiCert request via ServiceNow
3. Send renewed certificate to Platform Team
4. Platform Team updates in AWS (ARN remains the same)

---

## Common Ingress Annotations

### Timeouts

```yaml
alb.ingress.kubernetes.io/load-balancer-attributes: idle_timeout.timeout_seconds=60
```

### Access Logs

```yaml
alb.ingress.kubernetes.io/load-balancer-attributes: |
  access_logs.s3.enabled=true,access_logs.s3.bucket=my-logs-bucket,access_logs.s3.prefix=myapp
```

### WAF Integration

```yaml
alb.ingress.kubernetes.io/wafv2-acl-arn: arn:aws:wafv2:us-east-1:123456789:regional/webacl/my-waf/abc-123
```

### IP Allowlist

```yaml
alb.ingress.kubernetes.io/inbound-cidrs: 10.0.0.0/8,172.16.0.0/12
```

### Custom Health Check

```yaml
alb.ingress.kubernetes.io/healthcheck-path: /api/health
alb.ingress.kubernetes.io/healthcheck-protocol: HTTP
alb.ingress.kubernetes.io/healthcheck-port: traffic-port
alb.ingress.kubernetes.io/success-codes: '200,201'
```

---

## Best Practices

### 1. Health Check Configuration

Always configure a health check that returns HTTP 200:

```yaml
alb.ingress.kubernetes.io/healthcheck-path: /health
alb.ingress.kubernetes.io/healthcheck-interval-seconds: '15'
alb.ingress.kubernetes.io/healthcheck-timeout-seconds: '5'
alb.ingress.kubernetes.io/healthy-threshold-count: '2'
alb.ingress.kubernetes.io/unhealthy-threshold-count: '2'
```

Ensure your application has a lightweight `/health` endpoint. [!ref See Health Checks guide](../getting-started/kubernetes-manifests.md#2-configure-health-checks)

### 2. SSL/TLS Configuration

Always enable SSL redirect and use HTTPS:

```yaml
alb.ingress.kubernetes.io/ssl-redirect: '443'
alb.ingress.kubernetes.io/listen-ports: '[{"HTTP": 80}, {"HTTPS": 443}]'
```

### 3. Certificate Management

- Request certificates early (allow 3-5 business days)
- Use descriptive server names in certificate requests
- Set up calendar reminders for renewal (certificates expire annually)
- Use team distribution lists for certificate contacts

### 4. DNS Planning

- **New hostnames:** Can be created anytime
- **Existing hostnames:** Coordinate cutover timing with your team
- Test thoroughly using hosts file before cutover
- Plan for rollback if issues occur

### 5. Network Security

Choose appropriate scheme based on access requirements:
- **internet-facing:** Public internet access
- **internal:** Medtronic network only

Use IP allowlists when restricting to specific networks.

---

## Troubleshooting

### Ingress Not Creating ALB

**Check for issues:**
1. Review your Ingress YAML for correct annotations
2. Verify certificate ARN is valid
3. Check Grafana **Kubernetes / Events** dashboard for errors
4. Contact **Infra-Argo-Global** via ServiceNow for ingress controller logs

**Common issues:**
- Invalid certificate ARN
- Missing required annotations
- Ingress class not set correctly

### 502 Bad Gateway

**Checks:**
1. Verify your Service YAML exists and is configured correctly
2. Check health check path returns HTTP 200 from your application
3. View pod status in Grafana **Kubernetes / Compute Resources / Namespace (Pods)**
4. Review application logs in Grafana **Explore → Loki**

**For deeper investigation**, contact **Infra-Argo-Global** via ServiceNow.

### Certificate Errors

**Browser shows certificate error:**
- Verify certificate ARN is correct in Ingress
- Check certificate covers the hostname (including wildcards)
- Ensure certificate is in the same AWS region as cluster

**Certificate validation failed:**
- Confirm private key matches certificate
- Verify certificate chain is complete
- Check certificate hasn't expired

### DNS Not Resolving

**nslookup shows old address:**
- DNS changes can take time to propagate
- Flush local DNS cache
- Contact ServiceNow if issue persists after 24 hours

**nslookup returns no results:**
- Verify DNS request was fulfilled in ServiceNow
- Check spelling of hostname
- Confirm DNS zone was set correctly (internal vs external)

### Load Balancer Not Created

**Check ingress status:**
1. Review your Ingress YAML configuration
2. Check Grafana **Kubernetes / Events** dashboard for errors
3. Contact **Infra-Argo-Global** via ServiceNow for assistance

**Look for events indicating:**
- Missing IAM permissions
- Invalid subnet configuration
- Certificate not found

Contact **Infra-Argo-Global** via ServiceNow if you cannot resolve the issue.

---

## Next Steps

[!ref icon="shield" text="Authentication"](./authentication.md)
[!ref icon="pulse" text="Health Checks"](../getting-started/kubernetes-manifests.md#2-configure-health-checks)
[!ref icon="server" text="Service Configuration"](./service-configuration.md)
