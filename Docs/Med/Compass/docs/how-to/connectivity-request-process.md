---
label: Connectivity
order: 30
icon: link
---
# Connectivity Request Process
Use this guide to request network connectivity to internal and external services for your application. Connectivity workflows and open firewall inventories vary by platform. Use this guide for the standard request process and refer to the Platform Inventory for environment-specific details.

!!!
For a video walkthrough of the below process, see the [Networking and Firewall](../education-and-training/index.md#networking-and-firewall) training video!
!!!
---

## NetworkPolicies: Define Your Application's Connectivity

Before requesting firewall access, define your application's **exact** connectivity requirements using a **NetworkPolicy** manifest. This approach provides several benefits:

- **Explicit Documentation**: NetworkPolicy serves as code-based documentation of what your app actually connects to
- **Reduced Surface Area**: Deny-by-default rules prevent accidental connections
- **Easier Troubleshooting**: When connectivity issues occur, the policy clarifies whether the block is intentional
- **Audit Trail**: Your NetworkPolicy is version-controlled and reviewable in Git

### What to Include

Create a `k8s/<environment>/network-policy.yaml` that reflects your application's actual connectivity for that specific environment (e.g., `k8s/dev/network-policy.yaml`, `k8s/production/network-policy.yaml`):

!!!info NetworkPolicy vs CiliumNetworkPolicy
- **Standard NetworkPolicy**: Use for basic ingress/egress rules based on IP addresses, CIDR blocks, or pod labels
- **CiliumNetworkPolicy**: Use when you need **FQDN-based rules** (e.g., `api.example.com`, `db.corp.medtronic.com`). CiliumNetworkPolicy supports DNS-aware rules via `toFQDNs` with `matchName`, allowing you to specify exact hostnames instead of IP addresses.

For most applications connecting to databases or external APIs, use **CiliumNetworkPolicy** to explicitly allow only specific hostnames.
!!!

#### Pattern 1: Apps with No External Connectivity

For static content servers or apps that don't connect to databases/APIs:

```yaml
# k8s/production/network-policy.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: my-app
spec:
  podSelector:
    matchLabels:
      app: my-app
  policyTypes:
  - Ingress
  - Egress

  # Allow ingress on app ports
  ingress:
  - from:
    - namespaceSelector: {}
    ports:
    - protocol: TCP
      port: 8080
    - protocol: TCP
      port: 8888

  # Allow only DNS queries; deny all else
  egress:
  - to:
    - namespaceSelector:
        matchLabels:
          name: kube-system
    ports:
    - protocol: UDP
      port: 53
```

#### Pattern 2: Apps with Database and External API Connectivity

For apps connecting to specific databases and external APIs, use **CiliumNetworkPolicy** which supports FQDN-based rules:

```yaml
# k8s/production/network-policy.yaml
---
# Step 1: Default deny all egress
apiVersion: cilium.io/v2
kind: CiliumNetworkPolicy
metadata:
  name: my-app-default-deny
spec:
  endpointSelector:
    matchLabels:
      app: my-app
  egress:
  - {}

---
# Step 2: Allow DNS and platform services
apiVersion: cilium.io/v2
kind: CiliumNetworkPolicy
metadata:
  name: my-app-default-allow
spec:
  endpointSelector:
    matchLabels:
      app: my-app
  egress:
  # Allow DNS
  - toEndpoints:
    - matchLabels:
        io.kubernetes.pod.namespace: kube-system
        k8s-app: kube-dns
    toPorts:
    - ports:
      - port: "53"
        protocol: UDP
      rules:
        dns:
        - matchPattern: "*"

  # Allow Contrast Security (if using)
  - toFQDNs:
    - matchName: "app.contrastsecurity.com"
    toPorts:
    - ports:
      - port: "443"

---
# Step 3: Allow specific database and APIs your app needs
apiVersion: cilium.io/v2
kind: CiliumNetworkPolicy
metadata:
  name: my-app-custom-allow
spec:
  endpointSelector:
    matchLabels:
      app: my-app
  egress:
  # Allow Oracle database connection
  - toFQDNs:
    - matchName: "mspldb291.corp.medtronic.com"
    toPorts:
    - ports:
      - port: "2484"
        protocol: TCP

  # Allow PostgreSQL database connection
  - toFQDNs:
    - matchName: "prod-db.rds.amazonaws.com"
    toPorts:
    - ports:
      - port: "5432"
        protocol: TCP

  # Allow specific external APIs only
  - toFQDNs:
    - matchName: "api.partner-vendor.com"
    toPorts:
    - ports:
      - port: "443"
        protocol: TCP

  - toFQDNs:
    - matchName: "login.medtronic.com"
    toPorts:
    - ports:
      - port: "443"
        protocol: TCP
```

**Key Points:**
- **Three-policy pattern**: default-deny → default-allow (DNS/platform) → custom-allow (your app's specific dependencies)
- **Explicit hostnames**: Each database and API is explicitly listed by FQDN
- **Deny everything else**: Any connection not listed is blocked by default
- **No wildcards**: Avoid `- namespaceSelector: {}` or `toCIDRSet: 0.0.0.0/0` which would allow all traffic

**Environment-specific example:**

```yaml
# k8s/dev/network-policy.yaml - Development environment
egress:
- toFQDNs:
  - matchName: "dev-db.corp.medtronic.com"  # Dev database
  - matchName: "api-sandbox.partner.com"     # Sandbox API
```

```yaml
# k8s/production/network-policy.yaml - Production environment
egress:
- toFQDNs:
  - matchName: "prod-db.corp.medtronic.com"  # Production database
  - matchName: "api.partner.com"              # Production API
```

### Updates When Requesting New Connectivity

When you request firewall access to a new external service:

1. **Update your NetworkPolicy** for the specific environment to document the new connection
2. **Submit your firewall request** (see process below)
3. **Both should align**: What you document in the policy is what you're requesting in the firewall form
4. **Commit to Git**: The policy becomes the source of truth for your connectivity footprint

**Why environment-specific?** Development may connect to `dev-db.example.com:5432`, while production connects to `prod-db.example.com:5432`. Each environment's NetworkPolicy should reflect its actual dependencies.

This ensures your infrastructure-as-code matches your actual network requirements.

---
## Internal Services (Medtronic systems)

Examples: internal databases, internal APIs, and internal DNS endpoints.

!!!danger On-Prem Connectivity Requirements
Resources in the cloud are generally prohibited from accessing on-premise resources that are on Medtronic’s CORE network.
!!!

1. Use the **Connectivity Tester** for your environment to validate connectivity first.
2. If connectivity fails, submit a **Firewall Policy Change Request** and attach the completed form.
3. For __outbound__ connnections (your application is initiating calls to something), fill out the details as follows:
   - Source: your app's hostname (do not use IPs)
   - Destination: **FQDN** of the internal service (do not use IPs unless absolutely necessary)
   - Port/protocol (typically TLS/443, however Database Ports will vary)
4. If you do have __inbound__ connections (something else will be calling your application), fill out Source IPs with the details of the source that will be calling your application.
   !!!warning
   Do not specify a single IP address for the Destination. The IPs of the application may shift, so FQDN must be used!
   !!!

### Server Vulnerability Remediations
If you request connectivity to an internal server that has a bad/poor Nexpose/Rapid7 scan, the Network team will **not open connectivity** to that server until the vulnerabilities have been addressed. Use the steps below to work through remediations and provide updated scan proof to the Network team so that the request can be completed.

1. Reach out to the GCISO [Attack Surface Reduction Team](https://medtronic.sharepoint.com/sites/GSO/SitePages/Attack-Surface-Reduction.aspx#attack-surface-reduction-(asr)) and request a Vulnerability Report to be generated for the server(s).
    - Provide them a ServiceNow Assignment Group they can assign the scan results to. This should be a group tied to your application so that you can view and update status of the vulnerabilities identified.
2. See the [knowledge article](https://medtronicprod.service-now.com/now/nav/ui/classic/params/target/kb_view.do%3Fsysparm_article%3DKB0051661) on the process for moving items through resolution.
    - The [Attack Surface Reduction Team](https://medtronic.sharepoint.com/sites/GSO/SitePages/Attack-Surface-Reduction.aspx#attack-surface-reduction-(asr)) can assist with questions on the process.
3. Identify other required groups for remediation depending on the server and vulnerabilities identified. This may be a combination of different teams (UNIX, Oracle, etc.) depending on server type and vulnerability proof details that identify where the vulnerabilities exist on the server. Work with those teams to remediate vulnerabilities and ensure the server is on a regular patching schedule going forward.
4. Request an updated scan of the server to confirm vulnerabilities have been remediated. Depending on scan schedules, changes may not reflect immediately.
5. Provide updated scan proof to the Network team to have the firewall request completed.

## External Services (third-party vendors or internet endpoints)

Examples: SaaS APIs, external databases, or vendor-managed endpoints.

1. Use the **Connectivity Tester** for your environment to validate connectivity. Refer to the [Platform Inventory](../platform-inventory/index.md) for environment-specific connectivity tester links and NAT details.
2. If the external service requires allow-listing, provide them the outbound NAT IPs (see [External Vendor Allowlists](#external-vendor-allowlists) below).
3. If the connection fails, download the pre-filled Firewall Request Document from the Connectivity Tester.
4. Complete the form by filling in the:
   - Requestor name/phone number
   - Application Name
   - Summary of Project
   - Destination FQDN and port
   - Remove the sample inbound connection from the document if you have no inbound connections to request. This row is provided as an example.
5. If you do have __inbound__ connections (the external service will be initiating calls to your application), fill out Source IPs with the details of the source that will be calling your application.
   !!!warning
   Do not specify a single IP address for the Destination. The IPs of the application may shift, so FQDN must be used!
   !!!
6. Submit a [Firewall Policy Change Request](https://medtronicprod.service-now.com/it/?id=mdtit_sc_cat_item&sys_id=645fd49f1b3a234482a02f066e4bcb3b)
   - Choose "Add Access Rules"
   - Enter your application's name and other details
   - Attach the completed firewall request form
7. Notify your platform team with the request details so that infrastructure documentation and diagrams can be updated.

### External Vendor Allowlists ###

!!!EKS/AWS Environments Only
These NAT IPs apply to applications hosted in **EKS/AWS clusters only**. If your application is hosted in an on-prem Kubernetes cluster, these IPs do not apply - contact your platform team for the appropriate NAT IPs.
!!!

In addition to submitting the above request form, if your application is initiating connections to an external vendor that requires our IPs to be added to an allowlist/passlist, provide them the following IPs to allow:
* 3.209.5.221
* 35.174.123.55
