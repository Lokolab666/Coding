ey Cristian, hoping you can point me in the right direction on a WAF thing. Fair warning up front: I'm a developer, and my AWS knowledge runs out somewhere shortly after the login page. 🙂
 
Our registration app in TEST is throwing intermittent 403s when people submit the form. The tell is in the response header — Server: awselb/2.0 — so it's the load balancer/WAF turning it away, not our app misbehaving. The request never reaches our pods at all, which is why our own logs are serenely unhelpful.
 
The details:
App: cis-registration (Okta Registration)
Host: myprofile-test.medtronic.com — on the Argo DEV cluster, not prod
Symptom: POST /registration/{locale} → 403, text/html, 520 bytes. GETs to the same URL are perfectly happy.
Intermittent — plenty of registrations do go through
Timestamps: 2026-08-11 15:21:35 UTC, and again around 15:52 UTC
What I'm after: which rule terminated those POSTs? Specifically — is there anything in that web ACL either scoped to HTTP method POST, or inspecting the request body?
 
I did try to self-serve first. The Argo Playbook's §403 section points at the "WAF Logs / Requests" panel in Grafana, but that panel only ever returns prod-cluster hosts (myprofile.medtronic.com, manuals.medtronic.com). No test host, at any time range I try. So either I'm holding it wrong, or it doesn't cover the DEV cluster.
 
If digging out the rule is a pain, here's an alternative that'd work just as well for me: flip the suspect rules to COUNT mode on TEST for 24 hours. That labels the matches without blocking anything, and I could read it myself from there. Fully prepared to be told that's a terrible idea by someone who actually knows what they're doing.
Thanks! Happy to hop on a quick call if that beats typing.



Help me checking if the WAF rule is failing. The next file are the logs from Grafana