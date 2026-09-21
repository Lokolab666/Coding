---
label: Authentication
icon: people
order: 50
---

# Authentication

Configure ALB (Application Load Balancer) authentication to protect your application with SSO using OpenID Connect.

## What You'll Configure

ALB authentication using Ingress annotations to:
- Protect entire applications or specific paths
- Support internal Medtronic users (Azure AD)
- Support external users (external identity providers)
- Configure both internal and external authentication for the same app

---

## How It Works

AWS ALB intercepts requests and validates user authentication before traffic reaches your application. User claims are passed to your app via HTTP headers (`X-AMZN-OIDC-*`).

**Flow:**
1. User requests protected path
2. ALB redirects to identity provider (Azure AD, etc.)
3. User authenticates
4. ALB validates and creates session cookie
5. Request forwarded to your app with user claims in headers

---

## Prerequisites

- Work with the IAM team to request dedicated Client ID/Secret for your application depending on the user-base for your application:
  - [Internal Users](https://medtronicprod.service-now.com/it?id=sc_cat_item&table=sc_cat_item&sys_id=a582e82bc3028e10841e5dbeb001316b&recordUrl=com.glideapp.servicecatalog_cat_item_view.do%3Fv%3D1&sysparm_id=a582e82bc3028e10841e5dbeb001316b)
  - [External Users](https://medtronicprod.service-now.com/it?id=sc_cat_item&sys_id=7f8319b3c3864650c60b98577d01313b&sysparm_category=e1c4c06e4fb7d2006254cf5d0210c790)
- Provide redirect URLs in this format: `https://<your-hostname>/oauth2/idpresponse`

!!!warning App Type for ALB OIDC
If the IAM request form asks for application type or accessibility, choose **Web** for ALB OIDC flows. The ALB uses a confidential client with a client secret, so select **Web** even if your application is a Single Page Application (SPA).
!!!

**Store the Client Secret:**
- Add to External Secrets as described in [Secrets Management](./secrets-management.md)

!!!warning Preferred secret store for Entra ID / Azure AD client secrets
For Entra ID / Azure AD client secrets, use **CyberArk/Conjur** as the preferred secret store. `Identity and Access Management-Global` can configure automatic rotation to keep client secrets compliant.
!!!

---

## Single Provider Setup

### Protect All Paths (Single Provider)

Protect your entire application with a single identity provider.

#### Step 1: Add Ingress Annotations

Add these annotations to your `k8s/base/ingress.yaml`:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: myapp
  annotations:
    # Enable OIDC authentication
    alb.ingress.kubernetes.io/auth-type: oidc

    # Azure AD endpoints (internal Medtronic users)
    alb.ingress.kubernetes.io/auth-idp-oidc: |
      {
        "issuer": "https://login.microsoftonline.com/0a29d274-1367-4a8f-99c5-90c3dc7d4043/v2.0",
        "authorizationEndpoint": "https://login.microsoftonline.com/0a29d274-1367-4a8f-99c5-90c3dc7d4043/oauth2/v2.0/authorize",
        "tokenEndpoint": "https://login.microsoftonline.com/0a29d274-1367-4a8f-99c5-90c3dc7d4043/oauth2/v2.0/token",
        "userInfoEndpoint": "https://graph.microsoft.com/oidc/userinfo",
        "secretName": "oidc-client-secret"
      }

    # Session configuration
    alb.ingress.kubernetes.io/auth-session-cookie: "myapp-auth-session"
    alb.ingress.kubernetes.io/auth-session-timeout: "43200"  # 12 hours
    alb.ingress.kubernetes.io/auth-scope: "openid profile email"

    # On unauthenticated request, redirect to auth flow
    alb.ingress.kubernetes.io/auth-on-unauthenticated-request: "authenticate"
spec:
  ingressClassName: alb
  rules:
  - host: myapp.example.com
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

**Key configurations:**
- `auth-type: oidc` - Enables OpenID Connect authentication
- `auth-idp-oidc` - Identity provider endpoints and secret reference
- `auth-session-cookie` - Unique name for your app's session cookie
- `auth-session-timeout` - Session duration in seconds
- `secretName` - Reference to your External Secret containing `clientId` and `clientSecret`

#### Step 2: Create the Client Secret

For Entra ID / Azure AD client secrets, prefer CyberArk/Conjur (with IAM-managed rotation). The example below uses AWS Secrets Manager as a generic ESO pattern.

Create `k8s/base/external-secret-oidc.yaml`:

```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: oidc-client-secret
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secretsmanager
    kind: SecretStore
  target:
    name: oidc-client-secret
  dataFrom:
  - extract:
      key: myapp/oidc  # Path in AWS Secrets Manager
```

**Secret format in AWS Secrets Manager:**
```json
{
  "clientId": "xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxx",
  "clientSecret": "your-client-secret-here"
}
```

#### Advanced: Format Secret Values with ESO Templates (Apache OIDC / custom app formats)

If your application expects a specific file format (for example Apache `mod_auth_openidc` `.client` and `.conf` files), use `spec.target.template` to transform raw secret values into exactly the keys/content your app expects.

Create `k8s/base/external-secret-oidc-template.yaml`:

```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: oidc-client-secret
  labels:
    com.medtronic.web/monitor-secrets: "true"
spec:
  refreshInterval: 24h
  secretStoreRef:
    name: aws-secretsmanager
    kind: SecretStore
  target:
    name: oidc-client-secret
    creationPolicy: Owner
    template:
      engineVersion: v2
      data:
        login.microsoftonline.com-2F0a29d274-1367-4a8f-99c5-90c3dc7d4043-2Fv2.0.client: '{ "client_id": "{{ '{{' }} .client_id {{ '}}' }}", "client_secret": "{{ '{{' }} .client_secret {{ '}}' }}" }'
        login.microsoftonline.com-2F0a29d274-1367-4a8f-99c5-90c3dc7d4043-2Fv2.0.conf: '{ "scope": "{{ '{{' }} .scope {{ '}}' }}" }'
  data:
  - secretKey: client_id
    remoteRef:
      key: myapp/oidc
      property: client_id
  - secretKey: client_secret
    remoteRef:
      key: myapp/oidc
      property: client_secret
  - secretKey: scope
    remoteRef:
      key: myapp/oidc
      property: scope
```

**Example input secret in AWS Secrets Manager:**
```json
{
  "client_id": "xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxx",
  "client_secret": "your-client-secret-here",
  "scope": "openid profile email"
}
```

**Resulting Kubernetes Secret keys:**
- `login.microsoftonline.com-2F0a29d274-1367-4a8f-99c5-90c3dc7d4043-2Fv2.0.client` → JSON string with `client_id` and `client_secret`
- `login.microsoftonline.com-2F0a29d274-1367-4a8f-99c5-90c3dc7d4043-2Fv2.0.conf` → JSON string with `scope`

You can use the same pattern for non-auth use cases such as building a DB connection string from separate host/port/db/user/password fields:

```yaml
target:
  name: app-db-secret
  template:
    engineVersion: v2
    data:
      DB_CONNECTION_STRING: 'jdbc:postgresql://{{ '{{' }} .host {{ '}}' }}:{{ '{{' }} .port {{ '}}' }}/{{ '{{' }} .dbname {{ '}}' }}?user={{ '{{' }} .username {{ '}}' }}&password={{ '{{' }} .password {{ '}}' }}'
```

This lets teams keep backend secrets normalized while delivering app-specific formats at runtime.

#### Step 3: Mount Secret Files in Deployment

Mount the templated secret as files in your application container:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp
spec:
  replicas: 2
  selector:
    matchLabels:
      app: myapp
  template:
    metadata:
      labels:
        app: myapp
    spec:
      containers:
      - name: myapp
        image: myapp:latest
        ports:
        - containerPort: 8080
        volumeMounts:
        - name: oidc-config
          mountPath: /etc/apache2/oidc-config
          readOnly: true
      volumes:
      - name: oidc-config
        secret:
          secretName: oidc-client-secret
          defaultMode: 0644
          items:
          - key: login.microsoftonline.com-2F0a29d274-1367-4a8f-99c5-90c3dc7d4043-2Fv2.0.client
            path: azure-ad.client
          - key: login.microsoftonline.com-2F0a29d274-1367-4a8f-99c5-90c3dc7d4043-2Fv2.0.conf
            path: azure-ad.conf
```

**How it works:**
1. The `volumes.secret` references the Secret created by ESO (`oidc-client-secret`)
2. The `items[]` array maps each template output key to a file path
3. The `volumeMount` makes those files available at `/etc/apache2/oidc-config/` inside the container
4. Apache's `mod_auth_openidc` module reads `login.microsoftonline.com-2F0a29d274-1367-4a8f-99c5-90c3dc7d4043-2Fv2.0.client` and `login.microsoftonline.com-2F0a29d274-1367-4a8f-99c5-90c3dc7d4043-2Fv2.0.conf` as JSON files.

**In your Apache configuration, reference the mounted files:**

```apache
LoadModule auth_openidc_module modules/mod_auth_openidc.so

# Read configuration from the mounted secret files
OIDCMetadataDir /etc/apache2/oidc-config

# Extract client_id and client_secret from login.microsoftonline.com-2F0a29d274-1367-4a8f-99c5-90c3dc7d4043-2Fv2.0.client JSON
OIDCClientID  $(cat /etc/apache2/oidc-config/login.microsoftonline.com-2F0a29d274-1367-4a8f-99c5-90c3dc7d4043-2Fv2.0.client | jq -r '.client_id')
OIDCClientSecret $(cat /etc/apache2/oidc-config/login.microsoftonline.com-2F0a29d274-1367-4a8f-99c5-90c3dc7d4043-2Fv2.0.client | jq -r '.client_secret')

# Extract scope from login.microsoftonline.com-2F0a29d274-1367-4a8f-99c5-90c3dc7d4043-2Fv2.0.conf JSON
OIDCScope $(cat /etc/apache2/oidc-config/login.microsoftonline.com-2F0a29d274-1367-4a8f-99c5-90c3dc7d4043-2Fv2.0.conf | jq -r '.scope')
```

**Secret updates trigger deployment restarts automatically:**
When ESO detects a credential update and the Deployment references a Secret labeled `com.medtronic.web/monitor-secrets: "true"`, Kyverno's cluster policy automatically restarts the Deployment, reloading the new credentials without manual intervention.

#### Step 4: Update kustomization.yaml

Add the external secret to `k8s/base/kustomization.yaml`:

```yaml
resources:
- deployment.yaml
- service.yaml
- ingress.yaml
- external-secret-oidc.yaml
```

---

### Protect Some Paths (Public + Protected)

Protect specific paths while leaving others public (e.g., health checks, public landing pages).

#### Create Two Ingress Resources

**Protected paths** - `k8s/base/ingress-protected.yaml`:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: myapp-protected
  annotations:
    alb.ingress.kubernetes.io/auth-type: oidc
    alb.ingress.kubernetes.io/auth-idp-oidc: |
      {
        "issuer": "https://login.microsoftonline.com/0a29d274-1367-4a8f-99c5-90c3dc7d4043/v2.0",
        "authorizationEndpoint": "https://login.microsoftonline.com/0a29d274-1367-4a8f-99c5-90c3dc7d4043/oauth2/v2.0/authorize",
        "tokenEndpoint": "https://login.microsoftonline.com/0a29d274-1367-4a8f-99c5-90c3dc7d4043/oauth2/v2.0/token",
        "userInfoEndpoint": "https://graph.microsoft.com/oidc/userinfo",
        "secretName": "oidc-client-secret"
      }
    alb.ingress.kubernetes.io/auth-session-cookie: "myapp-auth-session"
    alb.ingress.kubernetes.io/auth-session-timeout: "43200"
    alb.ingress.kubernetes.io/auth-scope: "openid profile email"
    alb.ingress.kubernetes.io/auth-on-unauthenticated-request: "authenticate"
spec:
  ingressClassName: alb
  rules:
  - host: myapp.example.com
    http:
      paths:
      - path: /admin
        pathType: Prefix
        backend:
          service:
            name: myapp
            port:
              number: 8080
      - path: /dashboard
        pathType: Prefix
        backend:
          service:
            name: myapp
            port:
              number: 8080
```

**Unprotected paths** - `k8s/base/ingress-public.yaml`:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: myapp-public
  # No auth annotations - public access
spec:
  ingressClassName: alb
  rules:
  - host: myapp.example.com
    http:
      paths:
      - path: /health
        pathType: Exact
        backend:
          service:
            name: myapp
            port:
              number: 8080
      - path: /
        pathType: Exact
        backend:
          service:
            name: myapp
            port:
              number: 8080
```

**Update kustomization.yaml:**

```yaml
resources:
- deployment.yaml
- service.yaml
- ingress-protected.yaml
- ingress-public.yaml
- external-secret-oidc.yaml
```

---

## Dual Provider Setup (Internal + External)

Support both Medtronic employees (internal) and external users on the same application using different hostnames.

### Architecture

- **Internal hostname:** `myapp-internal.medtronic.com` → Azure AD (Medtronic users)
- **External hostname:** `myapp.example.com` → External IdP (partners, customers)

### Step 1: Create Internal Ingress

`k8s/base/ingress-internal.yaml`:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: myapp-internal
  annotations:
    alb.ingress.kubernetes.io/auth-type: oidc
    alb.ingress.kubernetes.io/auth-idp-oidc: |
      {
        "issuer": "https://login.microsoftonline.com/0a29d274-1367-4a8f-99c5-90c3dc7d4043/v2.0",
        "authorizationEndpoint": "https://login.microsoftonline.com/0a29d274-1367-4a8f-99c5-90c3dc7d4043/oauth2/v2.0/authorize",
        "tokenEndpoint": "https://login.microsoftonline.com/0a29d274-1367-4a8f-99c5-90c3dc7d4043/oauth2/v2.0/token",
        "userInfoEndpoint": "https://graph.microsoft.com/oidc/userinfo",
        "secretName": "oidc-internal-secret"
      }
    alb.ingress.kubernetes.io/auth-session-cookie: "myapp-internal-session"
    alb.ingress.kubernetes.io/auth-session-timeout: "43200"
    alb.ingress.kubernetes.io/auth-scope: "openid profile email"
    alb.ingress.kubernetes.io/auth-on-unauthenticated-request: "authenticate"
spec:
  ingressClassName: alb
  rules:
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

### Step 2: Create External Ingress

`k8s/base/ingress-external.yaml`:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: myapp-external
  annotations:
    alb.ingress.kubernetes.io/auth-type: oidc
    alb.ingress.kubernetes.io/auth-idp-oidc: |
      {
        "issuer": "https://your-external-idp.com",
        "authorizationEndpoint": "https://your-external-idp.com/oauth2/authorize",
        "tokenEndpoint": "https://your-external-idp.com/oauth2/token",
        "userInfoEndpoint": "https://your-external-idp.com/oauth2/userinfo",
        "secretName": "oidc-external-secret"
      }
    alb.ingress.kubernetes.io/auth-session-cookie: "myapp-external-session"
    alb.ingress.kubernetes.io/auth-session-timeout: "43200"
    alb.ingress.kubernetes.io/auth-scope: "openid profile email"
    alb.ingress.kubernetes.io/auth-on-unauthenticated-request: "authenticate"
spec:
  ingressClassName: alb
  rules:
  - host: myapp.example.com
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

### Step 3: Create Separate Secrets

**Internal secret** - `k8s/base/external-secret-oidc-internal.yaml`:

```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: oidc-internal-secret
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secretsmanager
    kind: SecretStore
  target:
    name: oidc-internal-secret
  dataFrom:
  - extract:
      key: myapp/oidc-internal
```

**External secret** - `k8s/base/external-secret-oidc-external.yaml`:

```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: oidc-external-secret
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secretsmanager
    kind: SecretStore
  target:
    name: oidc-external-secret
  dataFrom:
  - extract:
      key: myapp/oidc-external
```

### Step 4: Update kustomization.yaml

```yaml
resources:
- deployment.yaml
- service.yaml
- ingress-internal.yaml
- ingress-external.yaml
- external-secret-oidc-internal.yaml
- external-secret-oidc-external.yaml
```

**Key points:**
- Two separate Ingress resources with different hostnames
- Each uses its own Client ID/Secret from different External Secrets
- Different session cookie names prevent conflicts
- Your application receives the same headers from both providers

---

## Accessing User Information

ALB passes user claims to your application via headers.

### Available Headers

| Header | Description |
|--------|-------------|
| `x-amzn-oidc-accesstoken` | Access token (JWT) |
| `x-amzn-oidc-identity` | User's unique identifier |
| `x-amzn-oidc-data` | Base64-encoded user claims (JWT) |

### Decode User Claims

The `x-amzn-oidc-data` header contains a JWT with user information. Decode it in your application:

**Python example:**
```python
import base64
import json

def get_user_claims(oidc_data_header):
    # Split JWT and decode payload
    payload = oidc_data_header.split('.')[1]
    # Add padding if needed
    payload += '=' * (4 - len(payload) % 4)
    decoded = base64.b64decode(payload)
    return json.loads(decoded)

# In your request handler:
claims = get_user_claims(request.headers['x-amzn-oidc-data'])
user_email = claims.get('email')
user_name = claims.get('name')
```

**Node.js example:**
```javascript
function getUserClaims(oidcDataHeader) {
  const payload = oidcDataHeader.split('.')[1];
  const decoded = Buffer.from(payload, 'base64').toString('utf8');
  return JSON.parse(decoded);
}

// In your request handler:
const claims = getUserClaims(req.headers['x-amzn-oidc-data']);
const userEmail = claims.email;
const userName = claims.name;
```

### Common User Attributes

**Internal (Medtronic) users:**
- `email` - User's email
- `name` - Display name
- `sub` - Unique identifier
- `preferred_username` - Username
- Custom attributes may be available based on your Client ID configuration

**External users:**
- Attributes depend on your external identity provider
- Common: `email`, `name`, `sub`

---

## Implementing Logout

Remove the session and redirect users to sign out.

### Backend Logout Handler

Your application must:
1. Expire the ALB session cookie
2. Redirect to the identity provider's logout endpoint

**Python (Flask) example:**
```python
from flask import redirect, make_response

@app.route('/logout')
def logout():
    response = make_response(redirect(
        'https://login.microsoftonline.com/0a29d274-1367-4a8f-99c5-90c3dc7d4043/oauth2/v2.0/logout'
    ))

    # Expire all session cookie shards (0-4)
    for i in range(5):
        response.set_cookie(
            f'myapp-auth-session-{i}',
            '',
            expires=0,
            domain='.medtronic.com'
        )

    return response
```

**Node.js (Express) example:**
```javascript
app.get('/logout', (req, res) => {
  // Expire all session cookie shards (0-4)
  for (let i = 0; i < 5; i++) {
    res.clearCookie(`myapp-auth-session-${i}`, {
      domain: '.medtronic.com'
    });
  }

  // Redirect to IdP logout
  res.redirect(
    'https://login.microsoftonline.com/0a29d274-1367-4a8f-99c5-90c3dc7d4043/oauth2/v2.0/logout'
  );
});
```

**Session cookie shards:**
- ALB creates up to 5 cookie shards (suffixed with `-0` through `-4`)
- Your logout must expire all of them

---

## Supported Providers

The list below reflects the supported internal and external identity providers. Use the well-known configuration URL for your provider to populate the `issuer`, `authorizationEndpoint`, `tokenEndpoint`, `userInfoEndpoint`, and `logout` values in your Ingress annotations. The JSON response includes these fields directly.

### Internal (Medtronic) Providers

| Environment | Provider | Well-known Config URL |
| --- | --- | --- |
| Non-prod | Azure AD (Entra ID) | https://login.microsoftonline.com/0a29d274-1367-4a8f-99c5-90c3dc7d4043/v2.0/.well-known/openid-configuration |
| Production | Azure AD (Entra ID) | https://login.microsoftonline.com/d73a39db-6eda-495d-8000-7579f56d68b7/v2.0/.well-known/openid-configuration |

### External (CIAM) Providers

| Environment | Provider | Well-known Config URL |
| --- | --- | --- |
| Non-prod (Dev) | Okta (CIAM) | https://dev.login.medtronic.com/oauth2/ausqxu4lutnO83mOb1d6/.well-known/openid-configuration |
| Non-prod (Test) | Okta (CIAM) | https://test.login.medtronic.com/oauth2/aus1n3gftlEI6n8B70x7/.well-known/openid-configuration |
| Non-prod (Stage) | Okta (CIAM) | https://stage.login.medtronic.com/oauth2/aus12k2r9jvwY2zRl417/.well-known/openid-configuration |
| Production | Okta (CIAM) | https://login.medtronic.com/oauth2/aus16gc5wjVvJqXUg417/.well-known/openid-configuration |

---

## Best Practices

1. **Use unique session cookie names** - Prevents conflicts between applications or authentication providers
2. **Set appropriate timeout** - Balance security and user experience (recommended: 8-12 hours)
3. **Test logout flow** - Ensure all cookie shards are cleared
4. **Validate JWT claims** - Don't trust header data without verification in sensitive operations
5. **Use environment-specific secrets** - Different Client IDs/Secrets per environment
6. **Monitor authentication errors** - Check ALB logs for failed auth attempts

---

## Troubleshooting

### Redirect Loop

**Symptom:** Browser keeps redirecting between your app and identity provider

**Fix:**
- Verify redirect URI in IAM configuration matches: `https://<your-hostname>/oauth2/idpresponse`
- Check session cookie name doesn't conflict with other apps

### "Invalid redirect URI" Error

**Symptom:** Error message from identity provider about invalid redirect

**Fix:**
- Request IAM team to add your redirect URI to the Client ID
- Format: `https://<hostname>/oauth2/idpresponse`

### Headers Not Present

**Symptom:** `x-amzn-oidc-*` headers missing in application

**Fix:**
- Verify `auth-type: oidc` annotation is set
- Check if path is protected by the Ingress with auth annotations
- Unprotected paths won't have headers

---

## Next Steps

[!ref icon="lock" text="Connectivity & Firewall Requests"](./connectivity-request-process.md)
[!ref icon="key" text="Secrets Management"](./secrets-management.md)
[!ref icon="globe" text="Web Access & Hostnames"](./web-access-hostnames.md)
