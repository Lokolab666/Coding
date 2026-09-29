Worked for 1m 12s

This changes the diagnosis. The application is reachable; the failure is an incorrect post-authentication redirect to an internal address:

http://localhost:8080/rdcadfsrnd/...

A user’s browser interprets localhost as the user’s own computer, so the request fails after authentication.

Likely cause

The request path is:

ALB → Apache on pod port 8081 → Tomcat/application on localhost:8080

Tomcat is probably generating an absolute redirect using its internal address, and Apache is not rewriting it to the external hostname. Likely causes are:

Missing/incorrect ProxyPassReverse for /rdcadfsrnd.

Tomcat not honoring X-Forwarded-Host, X-Forwarded-Proto, and X-Forwarded-Port.

Missing Tomcat proxyName, proxyPort, scheme, or RemoteIpValve configuration.

The initial URL uses http:// instead of https://, and this Ingress does not appear to enforce HTTPS for every /rdcadfsrnd request.


This is an application/reverse-proxy configuration problem, not a DNS, ALB, Kubernetes Secret, WAF, or database connectivity failure.

Two problems with the supplied information

They still did not provide the actual source:

internal-k8s-intnon003-...elb.amazonaws.com [10.210.90.218]

That is the destination ALB, not the source server or user workstation.

There is also a datasource discrepancy:

Observed redirect: db=jdbc/rdcmtopat
Requested URL:     db=jdbc/rdcmdtopt

Given that the database is MDTOPT, jdbc/rdcmdtopt appears more plausible. This must be verified from the browser rather than dismissed as a typing error.

Immediate test

Test the exact path with both schemes:

curl -sS -o /dev/null -D - \
"http://edcmdt-test.medtronic.com/rdcadfsrnd/faces/Login?setUpDone=Y&mode=P&display_descpId=Y&display_docnum=Y&db=jdbc/rdcmdtopt"

curl -sS -o /dev/null -D - \
"https://edcmdt-test.medtronic.com/rdcadfsrnd/faces/Login?setUpDone=Y&mode=P&display_descpId=Y&display_docnum=Y&db=jdbc/rdcmdtopt"

As an immediate workaround, the requester should use the HTTPS URL. However, that is not a permanent fix if the post-login response still generates localhost:8080.

Inspect Apache and Tomcat configuration

POD=$($K -n "$NS" get pods \
  -l app.kubernetes.io/instance=oracleclinicalrdcsso-dev \
  -o jsonpath='{.items[0].metadata.name}')

$K -n "$NS" exec "$POD" -- sh -c '
grep -RniE "ProxyPass|ProxyPassReverse|rdcadfsrnd|localhost:8080" \
/etc/apache2 /usr/local/apache2/conf 2>/dev/null
'

Check the Tomcat connector configuration:

$K -n "$NS" exec "$POD" -- sh -c '
echo "CATALINA_HOME=$CATALINA_HOME"
grep -RniE "Connector|proxyName|proxyPort|scheme=|secure=|RemoteIpValve" \
"${CATALINA_HOME:-/opt/tomcat}/conf" 2>/dev/null
'

We are looking for either:

ProxyPassReverse /rdcadfsrnd http://localhost:8080/rdcadfsrnd

or an equivalent Tomcat/proxy configuration that ensures redirects use:

https://edcmdt-test.medtronic.com/rdcadfsrnd/...

Do not change it yet. Compare it with the working staging or production environment first.

Evidence needed from the browser

Ask the user to:

1. Open Developer Tools → Network.


2. Enable Preserve log.


3. Reproduce the authentication.


4. Find the first response containing:



Location: http://localhost:8080/...

5. Export the HAR or provide:

Request URL

Response status

Response Location header

Exact timestamp and date

User workstation/server IP




7:18 PM IST corresponds to 13:48 UTC and 08:48 Colombia time, but the date is still missing.

Updated ticket conclusion

> The application endpoint is operational and its health endpoint returns HTTP 200. The failure occurs after authentication because the browser is redirected to http://localhost:8080/rdcadfsrnd/....

Port 8080 is an internal application-container endpoint and must not be exposed in an external browser redirect. The expected redirect must retain https://edcmdt-test.medtronic.com.

This points to application reverse-proxy/Tomcat forwarded-header configuration, such as ProxyPassReverse, Tomcat proxy settings, or handling of X-Forwarded-* headers. A discrepancy was also observed between jdbc/rdcmtopat and jdbc/rdcmdtopt, which must be confirmed.

No DNS, certificate, ALB, Ingress, Service, pod, Flux, WAF, Kubernetes Secret, or Secrets Provider failure has been identified. The application team should review the redirect-generation and Apache/Tomcat proxy configuration for /rdcadfsrnd.
