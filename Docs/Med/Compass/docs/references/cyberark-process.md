---
label: CyberArk Credential Management
icon: shield-lock
---

## Request access to CyberArk

See [Tool Access](../getting-started/tooling-and-access.md) for instructions.

## Adding Credentials to CyberArk

When adding credentials to CyberArk in the SSIT-WebSolutions safe, the following steps show a consistent approach to use.  Please use the following attributes for the various types of credentials:

|Type|CyberArk Platform ID|CyberArk Address|
|---|---|---|
|Database user|Oracle Database|WEB12D, WEB12T, WEB12S, WEB12R or WEB12<br>(or similar database names)|
|OpenIDC secrets|Microsoft Azure Management|stage or production<br>(development and test if needed)|
|Service accounts or other application secrets (API keys, etc.)|MDT-StoredOnly|stage or production|

### Database accounts

1. Log into CyberArk: [https://passwordvault.medtronic.com](https://passwordvault.medtronic.com)

1. In the Accounts View select "Add Account"
![Add Account step](../static/cyberark/cyberark_step0.jpg)

1. Select "Database"

1. Select "Oracle Database"

1. Select "WebSolutions-Database"

1. Enter Username and Password

1. In the Address field, enter the database name in all lowercase (e.g. web)

1. Turn off “Allow automatic password management” and enter "Managed by another team" for the Reason


### Authentication / OpenIDC
!!!warning
For OpenIDC client secrets, it is recommended to work with `Identity and Access Management-Global` so they can configure automatic secret rotation and keep this credential in compliance.
!!!

1. Log into CyberArk: [https://passwordvault.medtronic.com](https://passwordvault.medtronic.com)

1. In the Accounts View select "Add Account"
![Add Account step](../static/cyberark/cyberark_step0.jpg)

1. Select the "Azure web services" for the OpenIDC example.
![Select system type](../static/cyberark/cyberark_step1.jpg)

1. Select "Microsoft Azure Management" - for this OpenIDC example, it is the only option.
![Select platform step](../static/cyberark/cyberark_step2.jpg)

1. Select the "SSIT-WebSolutions" safe to use for this example.
![Select safe step](../static/cyberark/cyberark_step3.jpg)

1. Enter the information shown below and click "Add"
![Define account properties step](../static/cyberark/cyberark_step4.jpg)

### Service accounts / API keys (MDT-StoredOnly)

Use this pattern for application secrets such as API keys, tokens, and non-database service credentials.

1. Log into CyberArk: [https://passwordvault.medtronic.com](https://passwordvault.medtronic.com)

1. In the Accounts View select "Add Account"

1. Select Platform ID: **MDT-StoredOnly**

1. Select Safe (e.g., **SSIT-WebSolutions**)

1. Enter the account/secret name using a clear app + purpose pattern (example: `my-app-external-api-token`)

1. Enter the secret value in the password/secret field (API key, token, or client secret)

1. Set **Address** to environment scope (`staging` or `production`, and `dev`/`testing` when needed)

1. Turn off automatic password management and use reason: "Managed by another team"

**Example values:**
- Platform ID: `MDT-StoredOnly`
- Safe: `SSIT-WebSolutions`
- Account/Secret Name: `Application-App-Secret-MyApp-ExternalAPI`
- Address: `staging`

These values map cleanly to Conjur variable paths used by External Secrets (for example: `UAT/GITUAT/GIT-GAS-P-WEBSOL/Application-App-Secret-MyApp-ExternalAPI/password`).

## Conjur Policy Reference for ESO JWT

Use this section as a **Conjur-side policy reference** for External Secrets Operator (ESO) with CyberArk Conjur.

!!!warning
These policies are created and loaded in Conjur by `Identity and Access Management-Global`. Application teams should coordinate exact values (service ID, namespace, service account identity, and variable paths) with IAM.
!!!

### Required Conjur policy pieces

For ESO `auth.jwt`, Conjur needs two policy pieces:

1. **JWT identity mapping** for the Kubernetes service account identity
2. **Secret access permits** for each Conjur variable path used by `remoteRef.key`

### Sample file: `conjur-jwt-host-policy.yml`

```yaml
- !policy
	id: conjur/authn-jwt/it-argo-dev-mdt-jwks/apps
	body:
	- !group eso-consumers
	- !host system:serviceaccount:newton-dev:default
	- !grant
		role: !group eso-consumers
		member: !host system:serviceaccount:newton-dev:default
```

### Sample file: `conjur-permit-policy.yml`

```yaml
- !policy
	id: UAT/GITUAT/GIT-GAS-P-WEBSOL
	body:
	- !variable Application-App-Secret-CLM-OktaTest-0oa4kshizhed4ikVz0x7/password
	- !variable Application-App-Secret-AzureADStage-test-eff21357-eb1b-45ae-9d68-5a00c11c9086/password
	- !variable Database-Oracle-web12t-WEB_UPDATE/password

	- !group eso-consumers

	- !permit
		role: !group eso-consumers
		privileges: [ read, execute ]
		resources: !variable *
```

### Value mapping checklist (ESO ↔ Conjur)

- `spec.provider.conjur.auth.jwt.serviceID` in `SecretStore` must match the Conjur `authn-jwt` policy path.
- `serviceAccountRef.name` + namespace must match the host identity format: `system:serviceaccount:<namespace>:<service-account>`.
- Every `ExternalSecret.spec.data[].remoteRef.key` must exist as a Conjur variable path and be permitted for the mapped identity/group.
