---
label: Secrets Management
icon: key
order: 70
---

# Secrets Management

Securely store and access sensitive credentials like database passwords, API keys, and certificates in your application.

## What You'll Configure

This guide covers multiple approaches to secrets management, each with different tradeoffs:

- **External Secrets Operator (ESO)** - Syncs secrets from external services (AWS Secrets Manager, GitLab, CyberArk) into Kubernetes Secrets at runtime
  - Best for: Centralized secret management, automatic rotation, audit logging
  - Your secrets stay in the secret store, not in your repository

- **Repository-Based (SOPS)** - Encrypt secrets directly in your Git repository
  - Best for: Keeping all configuration in version control, smaller teams, GitOps workflows
  - Your secrets are encrypted in Git, decrypted by Flux in the cluster at deployment time

Choose the approach that best fits your team's security requirements and operational needs.

---

## Prerequisites

### External Secrets Operator (ESO)

1. **Secret Backend** - Create secrets in your chosen service:
   - AWS Secrets Manager (default and recommended)
   - GitLab CI/CD Variables
   - CyberArk/Conjur
2. **IAM Role** - Platform team configures service account with IAM role
3. **SecretStore** - Platform team creates in your namespace

### SOPS (Repository-Based)

1. **Local Tools** - Install on your development machine:
   ```bash
   # macOS
   brew install sops age

   # Windows
   winget install SecretsOPerationS.SOPS
   winget install FiloSottile.age
   ```
2. **Automation-first key bootstrap** - Run the `flux-register-*` job for your environment branch. If `.sops.yaml` is missing, the job generates an age keypair, writes `.sops.yaml`, and stores the private key in your project CI/CD file variable `SOPS_AGE_PRIVATE_KEY`.
3. **Optional Terraform Cloud upsert** - Add `COMPASS_CI_TFC_API_TOKEN`, `COMPASS_CI_TFC_ORG`, and `COMPASS_CI_TFC_WORKSPACE_<CLUSTER_NAME>` to automatically upsert the app SOPS key variable into the target TFC workspace to ensure the hosted application has the same SOPS key-pair.

---

## Choosing a Secret Store

| Store | Best For | Pros | Cons |
|-------|----------|------|------|
| **[AWS Secrets Manager](#aws-secrets-manager-default)** | Application secrets, database credentials | Automatic rotation, tight AWS integration, audit logging | AWS-specific, API call costs |
| **[GitLab](#gitlab)** | CI/CD variables, build-time secrets | Integrated with GitLab workflow, per-environment scoping | Limited rotation, less secure for production |
| **[CyberArk/Conjur](#cyberark--conjur)** | High-security environments, privileged access, Entra ID/Azure AD client secrets | Enterprise features, advanced policies, centralized audit, IAM-supported automatic rotation for client secrets | Complex setup, requires security team involvement |
| **[SOPS (Repository-Based)](#sops-repository-based-approach)** | Configuration alongside secrets, smaller teams | Secrets in Git with full audit trail, no external service, easy code review | Secrets distributed in Git (encrypted), key rotation still requires re-encrypting secrets |

---

## External Secrets Operator (ESO)

The **External Secrets Operator** automatically syncs secrets from external services into your application. This means:
- Secrets stay in the secret store, never in Git or code
- Centralized management with audit logging
- Automatic rotation support (depending on backend)
- Your secrets are fetched at runtime from AWS, GitLab, CyberArk, etc.

**How it works:** ESO watches your configured secret backend and creates Kubernetes Secrets that your application can use as environment variables or files.

[!ref target="blank" text="Learn more about External Secrets" icon="link-external"](https://external-secrets.io/latest/)

Create `external-secret.yaml` in your `k8s/production/` directory:

```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: my-app-database-secret
spec:
  # Refresh interval
  refreshInterval: 1h

  # Reference to SecretStore (created by platform team)
  secretStoreRef:
    name: aws-secretsmanager
    kind: SecretStore

  # Target Kubernetes Secret
  target:
    name: my-app-database
    creationPolicy: Owner

  # Data to fetch
  data:
  - secretKey: username
    remoteRef:
      key: compass-ci/my-app/production/database
      property: DB_USERNAME

  - secretKey: password
    remoteRef:
      key: compass-ci/my-app/production/database
      property: DB_PASSWORD
```

**This creates a Kubernetes Secret:**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: my-app-database
data:
  username: YWRtaW4=  # base64
  password: c3VwZXItc2VjcmV0LXBhc3N3b3Jk
```

---

### Secret Store Examples

For the **External Secrets Operator (ESO)** approach, here are examples for the most commonly used external secret stores:

#### AWS Secrets Manager (Default)

AWS Secrets Manager is the default and recommended secret store for applications on the Compass CI platform.

<details>
<summary>Creating Secrets in AWS</summary>

**Using AWS Console:**
1. Navigate to AWS Secrets Manager
2. Click "Store a new secret"
3. Choose "Other type of secret"
4. Add key-value pairs:
   ```json
   {
     "DB_USERNAME": "admin",
     "DB_PASSWORD": "super-secret-password",
     "API_KEY": "abc123def456"
   }
   ```
5. Name: `compass-ci/my-app/production/database`
6. Click "Store"

**Using AWS CLI:**
```bash
aws secretsmanager create-secret \
  --name compass-ci/my-app/production/database \
  --secret-string '{"DB_USERNAME":"admin","DB_PASSWORD":"super-secret-password"}'
```

</details>

<details>
<summary>Example: AWS Secrets Manager ExternalSecret</summary>

```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: my-app-aws-secret
spec:
  refreshInterval: 1h

  # Reference to AWS SecretStore
  secretStoreRef:
    name: aws-secretsmanager
    kind: SecretStore

  target:
    name: my-app-credentials
    creationPolicy: Owner

  data:
  # Fetch specific properties from JSON secret
  - secretKey: db-username
    remoteRef:
      key: compass-ci/my-app/production/database
      property: DB_USERNAME

  - secretKey: db-password
    remoteRef:
      key: compass-ci/my-app/production/database
      property: DB_PASSWORD

  - secretKey: db-port
    remoteRef:
      key: compass-ci/my-app/production/database
      property: port

  - secretKey: db-connection-string
    remoteRef:
      key: compass-ci/my-app/production/database
      property: DB_CONNECTION_STRING

  # Fetch entire secret as single key
  - secretKey: api-config
    remoteRef:
      key: compass-ci/my-app/production/api-keys

  # dataFrom: Fetch all properties as separate keys
  dataFrom:
  - extract:
      key: compass-ci/my-app/production/oauth
```

**SecretStore Configuration:**
```yaml
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: aws-secretsmanager
  namespace: my-app-production
spec:
  provider:
    aws:
      service: SecretsManager
      region: us-east-1
      auth:
        jwt:
          serviceAccountRef:
            name: default  # Service account with IAM role
```

</details>

<details>
<summary>Example: RDS Secret Rotation (Alternating Users)</summary>

Use this pattern when AWS Secrets Manager rotation is configured for an RDS secret. See [Secret Rotation](#secret-rotation) section below for details on automatic application restart behavior.

**How rotation values map:**
- `AWSCURRENT`: current active credential
- `AWSPREVIOUS`: previous credential (useful during rotation overlap)

**Why this matters for refresh interval:**
- RDS alternating-user rotation keeps `AWSPREVIOUS` valid for a period of time during cutover.
- This overlap gives applications time to refresh credentials, so polling does not always need to be very aggressive.
- You can usually use moderate refresh intervals (for example `6h-24h`) unless your team has stricter rotation/recovery requirements.

```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: rds-db-secret-dual
  labels:
    com.medtronic.web/monitor-secrets: "true"
spec:
  refreshInterval: 1m
  secretStoreRef:
    name: aws-secretsmanager
    kind: SecretStore
  target:
    name: my-app-rds-db-secrets
    creationPolicy: Owner
  data:
  - secretKey: DB_PASSWORD_PREVIOUS
    remoteRef:
      key: "sbx_test_rotation_alternatinguser"
      property: password
      version: "AWSPREVIOUS"
  - secretKey: DB_PASSWORD
    remoteRef:
      key: "sbx_test_rotation_alternatinguser"
      property: password
      version: "AWSCURRENT"
  - secretKey: DB_USERNAME
    remoteRef:
      key: "sbx_test_rotation_alternatinguser"
      property: username
      version: "AWSCURRENT"
  - secretKey: DB_USERNAME_PREVIOUS
    remoteRef:
      key: "sbx_test_rotation_alternatinguser"
      property: username
      version: "AWSPREVIOUS"
  - secretKey: DB_HOST
    remoteRef:
      key: "sbx_test_rotation_alternatinguser"
      property: host
  - secretKey: DB_NAME
    remoteRef:
      key: "sbx_test_rotation_alternatinguser"
      property: dbname
  - secretKey: DB_PORT
    remoteRef:
      key: "sbx_test_rotation_alternatinguser"
      property: port
```

</details>

#### GitLab

Store secrets as CI/CD variables in GitLab projects or groups. Useful for pipeline secrets and environment-specific configurations.

<details>
<summary>Example: GitLab SecretStore + ExternalSecret</summary>

Use a namespace-scoped `SecretStore` for GitLab in this guide.

**Setup order (recommended):**
1. Create GitLab CI/CD variables (project-level or group-level)
2. Capture the target ID (project ID or group ID)
3. Create the matching GitLab access token (type, role, and scope)
4. Create and encrypt the Kubernetes token Secret (`external-secrets-gitlab-token`) with [SOPs](#sops-repository-based-approach)
5. Create `SecretStore`
6. Create `ExternalSecret`

**Where to place these YAML files in your repo (`k8s/`):**

Use your environment folder (for example `dev`, `qa`, `staging`, or `production`) and keep all GitLab manifests together:

```text
k8s/
  production/                         # or dev/qa/staging
    secret-gitlab-access-token.yaml   # Kubernetes Secret (SOPS-encrypted)
    secretstore-gitlab.yaml           # SecretStore (choose projectID OR groupIDs)
    externalsecret-gitlab.yaml        # ExternalSecret
    kustomization.yaml                # references the three files above
```

**Token type, scope, and role (project vs group):**

| Variable Location | Token Type | SecretStore Field | Token Scope | Minimum Role Guidance |
|-------------------|------------|-------------------|-------------|-----------------------|
| Project CI/CD variables | Project access token | `projectID` | `read_api` (preferred) or `api` | Project `Maintainer` (or higher) |
| Group CI/CD variables | Group access token | `groupIDs` | `read_api` (required) | Group `Owner` (required) |

Use `read_api` whenever possible for least privilege.

**SecretStore (project-level variables):**
File: `k8s/production/secretstore-gitlab.yaml`
```yaml
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: gitlab-secret-store
spec:
  provider:
    gitlab:
      url: https://medtronic.gitlab-dedicated.com
      projectID: "12345"
      auth:
        secretRef:
          accessToken:
            name: external-secrets-gitlab-token
            key: token
```

**SecretStore (group-level variables):**
File: `k8s/production/secretstore-gitlab.yaml` (use this version instead of `projectID`)
```yaml
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: gitlab-secret-store
spec:
  provider:
    gitlab:
      url: https://medtronic.gitlab-dedicated.com
      groupIDs: ["22444"]
      auth:
        secretRef:
          accessToken:
            name: external-secrets-gitlab-token
            key: token
```

**Token Secret manifest:**
!!!danger
Encrypt with [SOPS](#sops-repository-based-approach) prior to committing this to your repository!
!!!
File: `k8s/production/secret-gitlab-access-token.yaml`
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: external-secrets-gitlab-token
  namespace: my-app-production
type: Opaque
stringData:
  token: <gitlab-access-token>
```

For SOPS encryption, key generation, and edit/rotate workflow, use the instructions in [SOPS (Repository-Based) Approach](#sops-repository-based-approach).

**ExternalSecret (reads GitLab variables):**
File: `k8s/production/externalsecret-gitlab.yaml`
```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: my-app-gitlab-secrets
spec:
  refreshInterval: 10m
  secretStoreRef:
    name: gitlab-secret-store
    kind: SecretStore
  target:
    name: my-app-gitlab-secrets
    creationPolicy: Owner
  data:
  - secretKey: DOCKER_REGISTRY_TOKEN
    remoteRef:
      key: DOCKER_REGISTRY_TOKEN
  - secretKey: EXTERNAL_API_KEY
    remoteRef:
      key: EXTERNAL_API_KEY
  - secretKey: DATABASE_URL
    remoteRef:
      key: DATABASE_URL
      property: production #example for env specific variables
```

**Add the GitLab secret to your Deployment (`envFrom`)**

If this secret should be available in all environments, add it in your base Deployment (for example `k8s/base/deployment.yaml`) so overlays inherit it.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
spec:
  template:
    spec:
      containers:
      - name: my-app
        envFrom:
        - secretRef:
            name: my-app-gitlab-secrets
            optional: true
```

`optional: true` is recommended while onboarding new environments so pods can still start before the `ExternalSecret` has synced.

</details>

#### CyberArk / Conjur

CyberArk Conjur provides enterprise-grade secrets management with advanced access controls and audit capabilities.

For **Entra ID / Azure AD client secrets**, CyberArk/Conjur is the preferred store because `Identity and Access Management-Global` can configure automatic rotation for compliance.

!!!warning
This setup requires coordination with `Identity and Access Management-Global` to create and approve Conjur policies and `authn-jwt` service mappings before your `ExternalSecret` can access secrets.
!!!

<details>
<summary>Example: CyberArk Conjur ExternalSecret</summary>

```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: my-app-cyberark-secret
spec:
  refreshInterval: 15m

  # Reference to CyberArk SecretStore
  secretStoreRef:
    name: conjur-secret-store
    kind: SecretStore

  target:
    name: my-app-vault-credentials
    creationPolicy: Owner

  data:
  # Fetch secrets from CyberArk vault using policy variable paths
  - secretKey: CLM_OKTA_CLIENT_SECRET
    remoteRef:
      # Full Conjur variable path (includes Safe/policy path)
      # Format: <Safe>/<Item-or-Secret-Name>/password
      key: UAT/GITUAT/GIT-GAS-P-WEBSOL/Application-App-Secret-CLM-OktaTest-0oa4kshizhed4ikVz0x7/password

  - secretKey: AZURE_AD_CLIENT_SECRET
    remoteRef:
      # Full Conjur variable path (includes Safe/policy path)
      key: UAT/GITUAT/GIT-GAS-P-WEBSOL/Application-App-Secret-AzureADStage-test-eff21357-eb1b-45ae-9d68-5a00c11c9086/password

  - secretKey: WEB_UPDATE_DB_PASSWORD
    remoteRef:
      # Full Conjur variable path (includes Safe/policy path)
      key: UAT/GITUAT/GIT-GAS-P-WEBSOL/Database-Oracle-web12t-WEB_UPDATE/password
```

**SecretStore Configuration:**
```yaml
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: conjur-secret-store
spec:
  provider:
    conjur:
      url: https://dap.fl.medtronic.com
      caProvider:
        type: ConfigMap
        name: conjur-configmap
        key: CONJUR_SSL_CERTIFICATE
      auth:
        jwt:
          account: mdt
          serviceID: it-argo-dev-mdt-jwks
          serviceAccountRef:
            name: default
            audiences:
              - https://kubernetes.default.svc
```

**CyberArk Policy Example:**
```yaml
# 1) authn-jwt host mapping (required for ESO JWT auth)
- !policy
  id: conjur/authn-jwt/it-argo-dev-mdt-jwks/apps
  body:
  - !group eso-consumers
  - !host system:serviceaccount:einstein-dev:default
  - !grant
    role: !group eso-consumers
    member: !host system:serviceaccount:einstein-dev:default

# 2) Secret path policy (required for remoteRef key access)
- !policy
  # Safe / policy path where these Conjur variables are organized
  id: UAT/GITUAT/GIT-GAS-P-WEBSOL
  body:
  # Variables typically map to CyberArk Item/Secret names plus /password
  # Format: <Item-or-Secret-Name>/password
  - !variable Application-App-Secret-CLM-OktaTest-0oa4kshizhed4ikVz0x7/password
  - !variable Application-App-Secret-AzureADStage-test-eff21357-eb1b-45ae-9d68-5a00c11c9086/password
  - !variable Database-Oracle-web12t-WEB_UPDATE/password

  - !group eso-consumers

  - !permit
    role: !group eso-consumers
    privileges: [ read, execute ]
    resources: !variable *
```

These Conjur policies are created/loaded by `Identity and Access Management-Global` and must match your `serviceID`, service account identity, namespace, and `remoteRef.key` paths.

[!ref icon="book" text="Conjur-Side Policy Reference (ESO JWT + legacy examples)"](../references/cyberark-process.md#conjur-policy-reference-for-eso-jwt)


</details>

### Multi-Source Secrets

You can use multiple secret stores in the same application by creating multiple ExternalSecret resources:

```yaml
# Database credentials from AWS
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: database-credentials
spec:
  secretStoreRef:
    name: aws-secretsmanager
  target:
    name: db-creds
  data:
  - secretKey: password
    remoteRef:
      key: compass-ci/my-app/production/database
---
# CI/CD tokens from GitLab
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: ci-tokens
spec:
  secretStoreRef:
    name: gitlab-secret-store
  target:
    name: ci-creds
  data:
  - secretKey: deploy-token
    remoteRef:
      key: DEPLOY_TOKEN
```

### Combining Variables into Custom Formats

When your application requires secrets in a specific format—combining multiple values into a single connection string, config file, or other composite value—use ESO's templating feature with [Jinja2](https://jinja.palletsprojects.com/) to transform raw secret properties into exactly the format your app expects.

**Common use cases:**
- Database connection strings combining host, port, database name, username, password
- Apache [OIDC configuration files](./authentication.md#advanced-format-secret-values-with-eso-templates-apache-oidc--custom-app-formats) (`.client` and `.conf` formats)
- Custom JSON or YAML configuration
- Environment variables derived from multiple backend secrets

#### Example: Oracle JDBC Connection String

Transform separate host, port, SID (database), username, and password properties into a single Oracle JDBC connection string:

```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: my-app-database-secret
  labels:
    com.medtronic.web/monitor-secrets: "true"
spec:
  refreshInterval: 6h
  secretStoreRef:
    name: aws-secretsmanager
    kind: SecretStore
  target:
    name: my-app-database
    creationPolicy: Owner
    template:
      engineVersion: v2
      data:
        DB_CONNECTION_STRING: 'jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=tcps)(HOST={{ '{{' }} .host {{ '}}' }})(PORT={{ '{{' }} .port {{ '}}' }}))(CONNECT_DATA=(SID={{ '{{' }} .dbname {{ '}}' }})))'
        DB_USERNAME: '{{ '{{' }} .username {{ '}}' }}'
        DB_PASSWORD: '{{ '{{' }} .password {{ '}}' }}'
  data:
  - secretKey: host
    remoteRef:
      key: compass-ci/my-app/production/database
      property: host
  - secretKey: port
    remoteRef:
      key: compass-ci/my-app/production/database
      property: port
  - secretKey: dbname
    remoteRef:
      key: compass-ci/my-app/production/database
      property: dbname
  - secretKey: username
    remoteRef:
      key: compass-ci/my-app/production/database
      property: username
  - secretKey: password
    remoteRef:
      key: compass-ci/my-app/production/database
      property: password
```

**Input secret in AWS Secrets Manager:**
```json
{
  "host": "cds-tools-dev.cewjunbgzjfq.us-east-1.rds.amazonaws.com",
  "port": "2484",
  "dbname": "TOOLS",
  "username": "admin",
  "password": "super-secret-password-here"
}
```

**Output Kubernetes Secret:**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: my-app-database
data:
  DB_CONNECTION_STRING: amRiYzpvcmFjbGU6dGhpbjpAKERFU0NSSVBUSU9OPSBQQURKU1M9KFBSTlRPQ09MPSR0Y3BzKShIT1NUPWNkcy10b29scy1kZXYuY2V3anVuYmd6amZxLnVzLWVhc3QtMS5yZHMuYW1hem9uYXdzLmNvbSkgKFBPUlQ9MjQ4NCkpKENPTk5FQ1RfREFUQT0oU0lEPVRPT0xTKSkp
  DB_USERNAME: YWRtaW4=
  DB_PASSWORD: c3VwZXItc2VjcmV0LXBhc3N3b3JkLWhlcmU=
```

**How it works:**
1. ESO fetches the separate properties (host, port, dbname, username, password) from AWS Secrets Manager
2. The `spec.target.template` with `engineVersion: v2` enables Jinja2 templating
3. ESO combines the values using the Jinja2 expressions into an Oracle connection string with TCPS encryption
4. The result is stored as the `DB_CONNECTION_STRING` key in the final Secret
5. Your application reads `DB_CONNECTION_STRING` environment variable with all details combined

**Using the connection string in your Deployment:**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
spec:
  template:
    spec:
      containers:
      - name: my-app
        envFrom:
        - secretRef:
            name: my-app-database
```

This injects all keys from the secret as environment variables: `DB_CONNECTION_STRING`, `DB_USERNAME`, and `DB_PASSWORD`.

#### How Templating Works

- **`engineVersion: v2`** - Enables [Jinja2 v2](https://jinja.palletsprojects.com/) template engine
- **`{{ '{{' }} variable_name {{ '}}' }}`** - References a secret property
- **Template expressions** - Standard [Jinja2](https://jinja.palletsprojects.com/) filters and logic available (e.g., `{{ '{{' }} value | upper {{ '}}' }}`)

!!!warning
When using `spec.target.template`, **all output keys must be explicitly defined in `template.data`**, even if you're fetching those values in the `data` section below. The `template.data` section controls what keys appear in the final Kubernetes Secret. Keys only listed in `data` but not in `template.data` will not appear in the output. This allows you to fetch many properties but only output the ones your application needs.
!!!

See [Authentication: Format Secret Values with ESO Templates](./authentication.md#advanced-format-secret-values-with-eso-templates-apache-oidc--custom-app-formats) for additional examples like Apache OIDC configuration files.

---

## Secret Rotation

When your secret backend (AWS Secrets Manager, CyberArk, GitLab, etc.) rotates credentials, you need your applications to pick up the new values. This section explains how to trigger automatic application restarts when secrets change.

### How Secret Updates Reach Kubernetes

#### ESO flow (external backend rotation)

1. **Secret rotates** in your backend (AWS Secrets Manager, CyberArk, GitLab, etc.)
2. **External Secrets Operator syncs** the new value into a Kubernetes Secret
3. **Kyverno detects the Secret update** (when labeled `com.medtronic.web/monitor-secrets: "true"`)
4. **Deployment rollout is triggered**, restarting the application with new values

#### SOPS/Flux flow (Git-based secret updates)

1. **Encrypted Secret is updated in Git** (after re-encrypting with SOPS)
2. **Flux applies the updated Secret** to the cluster
3. **Kyverno detects the Secret update** (when labeled `com.medtronic.web/monitor-secrets: "true"`)
4. **Deployment rollout is triggered**, restarting the application with new values

In both patterns, the restart behavior is the same: Kyverno reacts to updates on labeled Secret/ConfigMap resources.

### Cluster-Level Policy

The cluster runs a Kyverno policy that automatically patches Deployments when monitored Secrets/ConfigMaps update:

```yaml
apiVersion: kyverno.io/v1
kind: ClusterPolicy
metadata:
  name: restart-deployment-on-secret-change
  annotations:
    policies.kyverno.io/title: Restart Deployments Upon Secret or ConfigMap Change
spec:
  mutateExistingOnPolicyUpdate: false
  rules:
  - name: update-secret
    match:
      any:
      - resources:
          kinds: ["Secret", "ConfigMap"]
          operations: ["UPDATE"]
          selector:
            matchLabels:
              com.medtronic.web/monitor-secrets: "true"
    mutate:
      targets:
      - apiVersion: apps/v1
        kind: Deployment
        namespace: "{{ '{{' }}request.namespace{{ '}}' }}"
      patchStrategicMerge:
        spec:
          template:
            metadata:
              annotations:
                ops.corp.com/triggerrestart: "{{ '{{' }}request.object.metadata.resourceVersion{{ '}}' }}"
```

**Key behaviors:**
- Monitors **all** Secrets/ConfigMaps with label `com.medtronic.web/monitor-secrets: "true"`
- Triggers **all** Deployments in that namespace by default
- Patches only on `UPDATE` operations (not on initial creation)

### Enabling Automatic Application Restart on Secret Changes (When Needed)

Whether an application restart is required depends on how your application consumes secrets:

- **Restart usually required:** Secrets consumed as environment variables (`env` / `envFrom`) because values are only read at process start
- **Restart may not be required:** Secrets mounted as files, if your application can re-read files or hot-reload configuration
- **Application-dependent:** Some frameworks cache secrets at startup even when mounted as files

To enable automatic application restart when your secret rotates:

1. **If you use ESO, label your ExternalSecret** with the monitor label:
   ```yaml
   apiVersion: external-secrets.io/v1beta1
   kind: ExternalSecret
   metadata:
     name: my-app-db-secret
     labels:
       com.medtronic.web/monitor-secrets: "true"  # Add this label
   spec:
     # ... rest of ExternalSecret config
   ```

  2. **If you use SOPS/Flux, label the Secret manifest directly**:
     ```yaml
     apiVersion: v1
     kind: Secret
     metadata:
       name: my-app-db-secret
       labels:
         com.medtronic.web/monitor-secrets: "true"  # Add this label
     type: Opaque
     # ... data or stringData ...
     ```

  3. **Ensure the label exists on the actual Kubernetes Secret object** that gets updated; Kyverno evaluates the updated Secret/ConfigMap resource

### Selective Deployment Targeting

If you have multiple Deployments in the same namespace but **only some** should restart on secret updates, create a custom namespace-scoped policy:

<details>
<summary>Example: Namespace-Scoped Custom Policy</summary>

Instead of using the cluster-wide policy, create a custom policy in your namespace with deployment selectors:

```yaml
apiVersion: kyverno.io/v1
kind: ClusterPolicy
metadata:
  name: restart-specific-deployments-on-secret
  namespace: my-app-production  # Target a specific namespace (or omit for all)
spec:
  validationFailureAction: audit
  rules:
  - name: update-secret-selective
    match:
      any:
      - resources:
          kinds: ["Secret"]
          namespaces: ["my-app-production"]
          operations: ["UPDATE"]
          selector:
            matchLabels:
              com.medtronic.web/monitor-secrets: "true"
    mutate:
      targets:
      - apiVersion: apps/v1
        kind: Deployment
        namespace: "{{ '{{' }}request.namespace{{ '}}' }}"
        selector:
          matchLabels:
            app: my-database-consumer  # Only target Deployments with this label
      patchStrategicMerge:
        spec:
          template:
            metadata:
              annotations:
                ops.corp.com/triggerrestart: "{{ '{{' }}request.object.metadata.resourceVersion{{ '}}' }}"
```

**Then label only the Deployments that need restarts:**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: database-app
  namespace: my-app-production
  labels:
    app: my-database-consumer  # Matches the policy selector above
spec:
  # ... rest of deployment
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cache-app
  namespace: my-app-production
  # No label = no restart on secret updates
spec:
  # ... rest of deployment
```

**Advantages:**
- Only specified Deployments restart on monitored secret changes
- Multiple policies can coexist (each targeting different Deployments)
- Fine-grained control per application

</details>

### Troubleshooting Rotation

**Secrets updated but app not restarting?**

1. Verify the Secret has label `com.medtronic.web/monitor-secrets: "true"`:
   ```bash
   kubectl get secret <secret-name> -o yaml | grep monitor-secrets
   ```

2. Confirm the Deployment exists in the same namespace:
   ```bash
   kubectl get deployments -n <namespace>
   ```

3. Check Kyverno policy is enabled:
   ```bash
   kubectl get clusterpolicies restart-deployment-on-secret-change
   ```

4. Review Kyverno controller logs:
   ```bash
   kubectl logs -n kyverno deployment/kyverno
   ```

5. Verify the Secret actually changed (Deployment won't roll out new pods if Secret data is identical)

---

## SOPS (Repository-Based) Approach

### Repository-Based Secrets with SOPS Encryption

For teams that prefer to store secrets in their Git repository, you can use **SOPS** (Secrets OPerationS) to encrypt sensitive values before committing them. This approach is useful for:
- Keeping all configuration in version control
- Easy secret rotation with code reviews
- Decryption handled in-cluster by Flux using a decryption key managed by the platform/GitOps team
- Team members can view encrypted secrets in Git history

<details>
<summary>Initial Setup: Encrypting Your First Secret with SOPS</summary>

**Encrypted Secret Example (safe to commit):**
```yaml
apiVersion: v1
kind: Secret
metadata:
    name: my-app-database-secret
    namespace: my-app-production
stringData:
    username: ENC[AES256_GCM,data:jcw6llqn...,iv:pxD8e/2N9...,tag:gWt6QUOy...,type:str]
    password: ENC[AES256_GCM,data:k9x2mnop...,iv:xyZ1a/3Q4...,tag:hXu7RPVz...,type:str]
sops:
    age:
        - recipient: age19xfc2z2w30facfzu3e5pr72qgd7gsu80ze4m6stzf8ffse0eq45qd44zqx
          enc: |
            -----BEGIN AGE ENCRYPTED FILE-----
            YWdlLWVuY3J5cHRpb24ub3JnL3YxCi0+IFgyNTUxOSBpVjJUWWUwdldxdGlEVyt1
            -----END AGE ENCRYPTED FILE-----
```

#### Prerequisites

Install SOPS and age (encryption tool):

```bash
# macOS
brew install sops age

# Windows
winget install SecretsOPerationS.SOPS
winget install FiloSottile.age

# Or download from https://github.com/mozilla/sops/releases
```

#### Setup Steps (Automation-first)

**1. Bootstrap with `flux-register-*` (recommended)**

When you run the environment-specific `flux-register-*` job (for example `flux-register-dev`), Compass CI now bootstraps SOPS automatically if `.sops.yaml` does not exist:

- Generates an age keypair
- Creates `.sops.yaml` at repo root using the standard rules
- Stores the private key as a project file variable: `SOPS_AGE_PRIVATE_KEY`
- Uses that key for Terraform Cloud variable upsert when TFC variables are configured

The generated `.sops.yaml` format is:

```yaml
creation_rules:
  - path_regex: k8s[/\\].*[/\\]secret.*\.yaml$
    encrypted_regex: ^(data|stringData)$
    age: age1...
```

**2. Retrieve the private key locally (for decrypt/edit/re-encrypt)**

After bootstrap, download `SOPS_AGE_PRIVATE_KEY` from your project CI/CD variables and store it locally at `~/.sops/key-${projectName}.txt`.

If your computer does not yet have a `.sops` directory:
```bash
# macOS
mkdir -p ~/.sops

# Windows
New-Item -ItemType Directory -Force -Path "$HOME\.sops"
```

Save the downloaded value exactly as the file contents.

!!!warning Windows: VS Code and SOPS default key lookup path
On Windows, SOPS and the VS Code SOPS extension look for age keys in a **different location** than where `age-keygen` saves the file by default:

```
%APPDATA%\sops\age\keys.txt
```
(typically `C:\Users\<your-username>\AppData\Roaming\sops\age\keys.txt`)

If you skip this step, opening an encrypted file in VS Code will fail with a decryption error, and `sops --decrypt` will also fail unless you set the env var manually.

**To fix — append your key to the SOPS default location:**
```powershell
# Create the directory if it doesn't exist
New-Item -ItemType Directory -Force -Path "$env:APPDATA\sops\age"

# Append your key to the SOPS keys file (multiple keys are supported in this file)
Get-Content "$HOME\.sops\key-${projectName}.txt" | Add-Content "$env:APPDATA\sops\age\keys.txt"
```

**Alternative — set the env var to point at your project key file:**
```powershell
$env:SOPS_AGE_KEY_FILE = "$HOME\.sops\key-${projectName}.txt"
```
Add it to your PowerShell profile (`$PROFILE`) to make it persistent.
!!!

**Important: Share key with your team members**

All team members must use the **same** SOPS key file to ensure consistent encryption/decryption:

1. **One person triggers `flux-register-*`** to bootstrap the key
2. **Share the downloaded file** (`~/.sops/key-${projectName}.txt`) securely with all team members.
3. **Everyone saves it** to the same location: `~/.sops/key-${projectName}.txt`

**Why this matters:** If different team members use different keys:
- ❌ Team member A encrypts `secrets.yaml` with their key
- ❌ Team member B can't decrypt it (different key)
- ❌ Cluster can't decrypt secrets encrypted by team member A (only has the team key)
- ❌ Deployments fail with decryption errors

Using a shared team key ensures everyone can encrypt/decrypt the same secrets, and the cluster (which has the private key) can decrypt all team-encrypted secrets.

**3. Ensure `.sops.yaml` exists in repository root**

This file tells SOPS which files to encrypt, which YAML fields to protect, and which public key to use. It keeps only `data` and `stringData` encrypted while leaving metadata readable, and ensures Flux can decrypt in-cluster using the matching private key.

The regex pattern below works on **both macOS and Windows** by matching forward slashes or backslashes in the path.

```yaml
# .sops.yaml
creation_rules:
  - path_regex: k8s[/\\].*[/\\]secret.*\.yaml$
    encrypted_regex: ^(data|stringData)$
    age: age1pmd0a3hj3vhjtvv58nc2jxdq5dxmv4lqxljdh00y9afgzl53uewsml9zfs  # Your PUBLIC key (from age-keygen output)
```

**Example from Einstein project:**
```yaml
# einstein/.sops.yaml
creation_rules:
  - path_regex: k8s[/\\].*[/\\]secret.*\.yaml$
    encrypted_regex: ^(data|stringData)$
    age: age1pmd0a3hj3vhjtvv58nc2jxdq5dxmv4lqxljdh00y9afgzl53uewsml9zfs
```

**4. No manual private-key handoff needed**

For Compass CI onboarding, `flux-register-*` now handles key bootstrap and cluster wiring automatically. You do not need to send your private key to the GitOps team for standard onboarding.

Manual handoff is only for exceptional cases outside the standard automation flow.

**5. Create unencrypted Secret locally (before encryption)**
```yaml
# k8s/dev/secrets.yaml (before encryption)
apiVersion: v1
kind: Secret
metadata:
    name: secrets
stringData:
    SAMPLE_SECRET: test123
    WEB_session_PW: mysecretpw
type: opaque
```

**6. Encrypt the Secret & Push to Protected Branch**

Choose the command for your platform:

**macOS:**
```bash
sops --encrypt --in-place k8s/dev/secrets.yaml
```

**Windows (PowerShell):**
```powershell
sops --encrypt --in-place k8s\dev\secrets.yaml
```

**Windows (Git Bash):**
```bash
sops --encrypt --in-place k8s/dev/secrets.yaml
```

After encryption, the file is safe to commit to Git.

**Example encrypted output:**
```yaml
# k8s/dev/secrets.yaml (after encryption)
apiVersion: v1
kind: Secret
metadata:
    name: secrets
stringData:
    SAMPLE_SECRET: ENC[AES256_GCM,data:1MmC1t0GPBDNeQ==,iv:D+t9yQ/K6tphd12NcHLj6c+TaMLM1lKsuDkSpu7o+wo=,tag:OOQu8hAmlrpmyCEQEFe8kA==,type:str]
    WEB_session_PW: ENC[AES256_GCM,data:HoffAsRN3G19OHaU,iv:y+uc8cWPlUdDnNGTxAI9TITmSHLzCF7Wzu6HXMuH20w=,tag:Dqb/HigBbBORchVxv3uy1A==,type:str]
type: opaque
sops:
    age:
        - recipient: age1pmd0a3hj3vhjtvv58nc2jxdq5dxmv4lqxljdh00y9afgzl53uewsml9zfs
          enc: |
            -----BEGIN AGE ENCRYPTED FILE-----
            YWdlLWVuY3J5cHRpb24ub3JnL3YxCi0+IFgyNTUxOSBKWmttLy9kWHp6RlpEV0NQ
            NXJmQWZ5NldmN0U4M0h0UFl4bHFZRlZIVkFZCms5SzFDMzRNTnp6ZnVJd0V6dTdn
            MUw2TTZGYnlNdWpDZ21Lc3ZQWkpPZXMKLS0tIEIxb3d1T0dXMWxtc0tMYUhBQm5D
            dXNDMjYyM3FXdTZ3NmhFNnVoMnlPYlEK3o/JGgZo2P4wZ/7+SNuIO1bMZjt4J8ZW
            jrPiQ2nulsg4ckieffZ3k9V6iNki+raufCzj6OOXS6fxh9yJvQP/5Q==
            -----END AGE ENCRYPTED FILE-----
    lastmodified: "2026-02-18T15:29:27Z"
    mac: ENC[AES256_GCM,data:Cygg4lCzBoLtW3Z8iGyFNJdA4V2UUSS6XI9bP3YxUxFPv3DgeBsklgpivg+KFLE70cFr+QyHOG1Z/+WnHpVndw64/Q7bVGReoEb3tfDFGA388HH44BsHgL+8YOjd77qce1kt7B06qD8XYPNQOB7rc+/GarmGuWgwFXt1wtn7RyM=,iv:Opl5GTbHUZ/pLsLnkJHJjKHLb3bDPueAYcoxTl5k03w=,tag:al27ZIlpi7j8YAg/WenbKw==,type:str]
    encrypted_regex: ^(data|stringData)$
    version: 3.11.0
```



**What happens at runtime:**
1. Flux detects encrypted secrets (ENC[...]) in your committed YAML
2. Uses the private key from the cluster Secret to decrypt them
3. Creates the actual Kubernetes Secret in your application namespace
4. Your app accesses the decrypted Secret normally

#### Advantages of This Approach

- ✅ **One key per team** - Encrypt all your team's secrets with the same public key
- ✅ **One-time GitOps setup** - GitOps team loads key to cluster once, configures Kustomization once
- ✅ **Works across all app secretes** - Private key in the app namespace decrypts any SOPS-encrypted manifests in your repo
- ✅ **No Secret object references needed** - Encrypted YAML files are just normal YAML; Kustomization handles decryption transparently
- ✅ **Minimal dev overhead** - Just encrypt locally with public key and commit
- ✅ **Full GitOps workflow** - Everything is in Git; only cluster has decryption capability
- ✅ **Audit trail** - Git shows encrypted secrets; cluster logs show decryption events

#### Disadvantages

- ⚠️ **Private key exists in cluster** - Anyone with cluster access can decrypt secrets
- ⚠️ **Manual rotation requires key replacement** - If SOPs private key is compromised, all secrets must be re-encrypted with a new key

**For higher security**, consider ESO-based approaches (AWS, GitLab, CyberArk) where secrets never live in the cluster.

</details>

### Updating Encrypted Secrets

To modify an already-encrypted secret:

**Option 1: Edit in place (recommended)**

**macOS/Linux:**
```bash
# SOPS will decrypt, open in editor, and re-encrypt on save
sops k8s/dev/secrets.yaml
```

**Windows (PowerShell):**
```powershell
# SOPS will decrypt, open in editor, and re-encrypt on save
sops k8s\dev\secrets.yaml
```

SOPS will:
1. Decrypt the file in memory
2. Open it in your default editor (set via `$EDITOR` or `$VISUAL` environment variable)
3. Automatically re-encrypt when you save and close the editor
4. The encrypted file is updated in place

**Setting your editor:**

**macOS/Linux:**
```bash
# In ~/.bashrc or ~/.zshrc
export EDITOR=nano  # or vim, code, etc.
```

**Windows (PowerShell):**
```powershell
# In your PowerShell profile
$env:EDITOR = "notepad"  # or "code", "notepad++", etc.
```

**Option 2: Decrypt, edit, re-encrypt manually**

```bash
# 1. Decrypt to plaintext
sops --decrypt k8s/dev/secrets.yaml > secrets-temp.yaml

# 2. Edit with any text editor
notepad secrets-temp.yaml  # Windows
nano secrets-temp.yaml     # macOS/Linux

# 3. Re-encrypt
sops --encrypt secrets-temp.yaml > k8s/dev/secrets.yaml

# 4. Clean up plaintext file
rm secrets-temp.yaml
```

**After updating:**
1. Verify encryption: `cat k8s/dev/secrets.yaml` should show `ENC[...]` values
2. Commit and push changes to Git
3. Flux will automatically detect and apply the updated secrets to the cluster

#### Additional SOPS Commands

```bash
# View decrypted values without editing (stdout only, doesn't modify file)
sops --decrypt k8s/dev/secrets.yaml

# Rotate encryption key (re-encrypt with new key from .sops.yaml)
sops rotate k8s/dev/secrets.yaml

# Encrypt a new plaintext file
sops --encrypt k8s/dev/new-secret.yaml > k8s/dev/new-secret.yaml.enc
```

**For help with SOPS setup**, contact `Infra-Argo-Global`.

## Use Secret in Deployment

### As Environment Variables

```yaml
spec:
  containers:
  - name: my-app
    env:
    - name: DB_USERNAME
      valueFrom:
        secretKeyRef:
          name: my-app-database
          key: username
    - name: DB_PASSWORD
      valueFrom:
        secretKeyRef:
          name: my-app-database
          key: password
```

### As Environment Block

```yaml
spec:
  containers:
  - name: my-app
    envFrom:
    - secretRef:
        name: my-app-database
```

### As Volume Mount

```yaml
spec:
  containers:
  - name: my-app
    volumeMounts:
    - name: secrets
      mountPath: /etc/secrets
      readOnly: true
  volumes:
  - name: secrets
    secret:
      secretName: my-app-database
```

Access at `/etc/secrets/username` and `/etc/secrets/password`

---

## Best Practices

### Secret Naming Convention

Use a consistent naming pattern in AWS Secrets Manager:

```
compass-ci/{app-name}/{environment}/{secret-type}
```

**Examples:**
- `compass-ci/my-app/production/database`
- `compass-ci/my-app/production/api-keys`
- `compass-ci/my-app/staging/oauth`

This makes secrets discoverable and prevents conflicts across teams.

### Organize by Type

Create separate ExternalSecret resources for different secret types:

```yaml
# database-secret.yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: database-secret
spec:
  refreshInterval: 1h
  # ... rest of config

---
# api-keys-secret.yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: api-keys-secret
spec:
  refreshInterval: 6h
  # ... rest of config
```

This separation makes it easier to manage different rotation schedules and permissions.

### Set Appropriate Refresh Intervals

Balance between security and API call costs, and align to your current rotation cadence (currently most secrets change ~1-4 times/year):

- **API keys:** `1h-6h` (often the most likely to change)
- **Database credentials:** `6h-24h` (typically infrequent rotation today; often acceptable with RDS dual-user overlap)
- **Static secrets:** `24h` (rarely updated)

If your team rotates credentials more aggressively in the future, reduce refresh intervals accordingly.

If you use RDS alternating-user rotation (`AWSCURRENT`/`AWSPREVIOUS`), that overlap period can provide additional buffer for credential refresh.

```yaml
spec:
  refreshInterval: 1h  # Auto-sync frequency
```

### Avoid Secrets in Environment Variables (When Possible)

For sensitive credentials, prefer volume mounts:

```yaml
# Better: Mounted as files
volumeMounts:
- name: secrets
  mountPath: /etc/secrets
  readOnly: true

volumes:
- name: secrets
  secret:
    secretName: my-app-database
```

Files have built-in access controls and are less likely to be logged accidentally.

### Secure Secret Management

- **For ESO-based approaches (AWS, GitLab, CyberArk):** Never store raw secret values in Git or code - only store configuration that references external secret stores
- **For SOPS-based approach (Flux decryption):**
  - Encrypt secrets locally before committing (your `.sops.yaml` uses public key only)
  - Retrieve `SOPS_AGE_PRIVATE_KEY` from your project CI/CD variables for local decrypt/edit workflows
  - Flux bootstrap automation provisions the cluster-side secret wiring for standard onboarding
  - All communication via Git is encrypted; only cluster has decryption capability
- **Audit access** - Enable CloudTrail logging for AWS Secrets Manager or equivalent for your chosen backend; for SOPS, check Flux logs and cluster audit logs for decryption events
- **Rotate regularly** - Set up automatic rotation whenever possible (ESO supports this; SOPS requires generating new key and re-encrypting all secrets)
- **Principle of least privilege** - Give only required IAM/access permissions per application; for SOPS, limit who has access to the private key file itself

For detailed External Secrets configuration, [!ref See the External Secrets documentation](https://external-secrets.io/latest/)

---

## Troubleshooting

### For External Secrets Operator (ESO)

#### Check ExternalSecret Status

Review your ExternalSecret YAML and check status in Grafana:
1. Navigate to **Kubernetes / Events** dashboard
2. Filter by your namespace and ExternalSecret name
3. Look for sync status events

**Expected status:**
- Ready: Secret successfully synced from AWS Secrets Manager
- Error messages will appear in events if sync fails

### Secret Not Syncing
1. Verify IAM permissions - contact **Infra-Argo-Global** via ServiceNow
2. Verify secret exists in AWS Secrets Manager
3. Check naming convention matches requirements
4. Contact **Infra-Argo-Global** for ESO controller logs if issues persist

### Access Denied Errors
- Contact platform team to verify IAM role permissions
- Ensure secret path matches naming convention

### For SOPS (Repository-Based)

With SOPS, you encrypt secrets locally and commit them. Flux automatically decrypts them at deployment time using the private key loaded by the GitOps team. Troubleshooting steps:

1. **Verify encrypted secrets exist in your repository**
   ```bash
   cat k8s/production/secret-database.yaml
   # Should show ENC[AES256_GCM,...] values, not plaintext
   ```

2. **Verify you're using your team's public key in `.sops.yaml`**
   ```bash
   cat .sops.yaml
   # The 'age' value should match your public key from age-keygen
   ```

3. **Check bootstrap outputs from `flux-register-*`**
  - Confirm `SOPS_AGE_PRIVATE_KEY` exists in project CI/CD variables
  - Confirm the flux-register MR includes generated SecretStore/ExternalSecret/Kustomization wiring

4. **Check Flux logs for decryption errors**
   - Contact **Infra-Argo-Global** via ServiceNow to review Flux controller logs
   - They can check source-controller and kustomize-controller for decryption errors

5. **Verify decrypted Secret was created**
   - Check Grafana **Kubernetes / Events** dashboard for secret creation events
   - Contact **Infra-Argo-Global** if secret not appearing

6. **Verify your app is using the Secret**
   - Review your Deployment YAML to ensure secret is mounted correctly
   - Check application logs in Grafana for secret-related errors

**Common Issues:**
- Encrypted values not showing `ENC[...]`: You may not have `.sops.yaml` configured correctly or used the wrong public key to encrypt
- Secret not created: GitOps team hasn't configured the Kustomization yet; contact them
- Secret not created: flux-register bootstrap has not been run/merged yet
- Decryption failed: Contact **Infra-Argo-Global** via ServiceNow to verify the correct private key was loaded

If issues persist, contact the [Platform Team](mailto:dl.itargocoreteam@medtronic.com) for assistance.

---

## Next Steps

[!ref icon="package" text="Configure Deployment"](../getting-started/kubernetes-manifests.md)

[!ref icon="pulse" text="Health Checks"](../getting-started/kubernetes-manifests.md#2-configure-health-checks)

[!ref icon="shield" text="Authentication"](./authentication.md)
