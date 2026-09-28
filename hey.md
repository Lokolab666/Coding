Worked for 22s

This confirms the application is accessible from your workstation, but it does not validate the three servers listed in the ticket.

Tested source:

Hostname: CPC-mesac-4BM7S
Source IP: 10.213.87.36

Results:

DNS resolves correctly to the Argo internal ALB.

TCP port 443 succeeds.

TLS connection succeeds.

ALB reaches the Kubernetes application.

Apache returns the expected 302 authentication redirect.

The response identifies backend port 8081, confirming traffic reached the application pod.


Therefore:

This is not a general Compass/Argo outage.

WAF is not blocking this specific GET request from 10.213.87.36.

DNS, ALB, Ingress, Service and application backend are working.

A Secrets Provider failure is not supported by the evidence.


However, CPC-mesac-4BM7S is not one of the reported servers:

mspm7aappd0133

mspm7aappddd0136

mspm7appd0137


The same commands must be run from each actual affected server. Also, two names may contain spelling mistakes.

One more important point: the current Argo backend consists of Kubernetes pods, not those mspm... servers. The requester must explain whether those servers are:

Clients calling the Argo application;

Legacy application servers;

Downstream systems needed by the application; or

Simply outdated information copied into the ticket.


Recommended ticket update

> Platform validation was completed for edcmdt-test.medtronic.com. DNS resolves correctly to the Argo DEV internal ALB, TCP connectivity on port 443 succeeds, the TLS certificate is valid, and the Kubernetes deployment has two healthy pods with active Service endpoints.

The application health endpoint returns HTTP 200. A request to /oracleclinicalrdcsso/ returns the expected HTTP 302 authentication redirect from Apache, confirming that the ALB successfully reaches the application backend.

The test from workstation CPC-mesac-4BM7S, source IP 10.213.87.36, was successful. No deployment, Kubernetes Secret mount, OIDC provider-file, DNS, certificate or platform connectivity failure was identified.

The servers listed in the incident have not yet been tested. Please confirm their exact hostnames and their role in the current architecture, and execute DNS, TCP 443 and HTTPS tests from each affected server. Also provide the exact error, timestamp, requested URL and whether the failure occurs before or after authentication.



At this point, continuing to inspect Kubernetes Secrets without an authentication error would be chasing the wrong hypothesis. The next evidence must come from the actual affected server or the failing user workflow.
