Worked for 44s

These results confirm that the application is reachable and serving traffic. The hypothesis of a Secrets Provider failure is now very weak.

What is proven

/oracleclinicalrdcsso/ reaches the application pods and returns HTTP 302.

The response comes from Apache, not only the ALB.

The redirect to login.html is expected for an unauthenticated request.

The health endpoint returns HTTP 200.

Application version 3.4.298t1-SNAPSHOT, environment DEV, is running.

The signin-stage.medtronic.com provider has all three required files:

.client

.conf

.provider


Those files are projected into /var/cache/mod_auth_openidc/metadata.

No OIDC, provider, authentication, Oracle, JDBC, or SQL errors were found in the last 24 hours.


The secrets-providers resource is a normal Kubernetes Secret containing the mod_auth_openidc provider files. It is not a Secrets Store CSI provider.

Important qualification

The Windows test originated from:

10.213.87.36

If that IP belongs to one of the reported application servers, connectivity from that server is working. The same validation still needs to be executed from the other affected servers because the original ticket names three sources.

The 302 is not a failure:

Location: /oracleclinicalrdcsso/login.html

It is the expected authentication entry point.

Remaining possibilities

The issue is now more likely to be:

1. A specific server—not the one tested—cannot reach the endpoint.


2. A browser/user-specific SSO problem after displaying login.html.


3. An OIDC callback or token-exchange failure after selecting the login provider.


4. A specific application transaction failing against MDTOPT.


5. Incorrect server names in the ticket.


6. The issue was temporary and is no longer reproducible.



Next validation

From each listed application server, run:

hostname
ipconfig | findstr /i "IPv4"

Resolve-DnsName edcmdt-test.medtronic.com

Test-NetConnection edcmdt-test.medtronic.com -Port 443

curl.exe -v `
  --connect-timeout 10 `
  "https://edcmdt-test.medtronic.com/oracleclinicalrdcsso/"

The requester must provide the actual symptom. “Not able to connect” is not enough. Ask for:

Exact error message or screenshot.

Timestamp and timezone.

Which of the three servers fails.

Whether the problem occurs before or after authentication.

Full URL/path being accessed.

Whether the health endpoint works from the affected server.

Source IP of every affected server.


Suggested ticket update

> We validated edcmdt-test.medtronic.com in the Argo DEV environment. The Kubernetes deployment is healthy with two ready pods and no restarts. The Service has both expected endpoints, Flux reconciliation is healthy, DNS resolves to the configured internal ALB, and the TLS certificate is valid.

The application health endpoint /oracleclinicalrdcsso/build.json?ALB returns HTTP 200 and identifies version 3.4.298t1-SNAPSHOT in DEV. The application context returns the expected HTTP 302 redirect to the authentication page.

The OIDC provider files for signin-stage.medtronic.com are present and mounted correctly. No provider, authentication, Oracle, JDBC, or SQL errors were found in the reviewed application logs. No recent application deployment was identified.

A connectivity test from source IP 10.213.87.36 successfully reached the application. Please confirm which listed server owns this IP and provide the exact error, timestamp, source server/IP, requested URL, and whether the failure occurs before or after authentication. The same connectivity test must be executed from each reportedly affected server.



Do not state that Compass caused the incident. The available evidence currently shows a healthy platform and application endpoint.
