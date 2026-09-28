I have the next ticket, and I have access into Cloud9 for k8s and check the validations

Ticket:
can you please check why EDCMDT-TEST.MEDTRONIC.COM is not able to connect to application please check and let us know if there if there was any been change.

Appplication server : mspm7aappd0133,mspm7aappddd0136,mspm7appd0137
Database : MDTOPT
DB server : mspldb666

Regards,
Dinesh V

And this was a las work for another team
Summary for oracleclinicalrdcsso (EDC application):
 
Deployed across 4 namespaces in Argo:
- argo-oracleclinicalrdcsso-dev -> edcmdt-test.medtronic.com (DEV account, healthy 2/2)
- argo-oracleclinicalrdcsso-testing -> edcmdt-dev.medtronic.com (DEV account, degraded - 1 pod unready with repeated restarts)
- argo-oracleclinicalrdcsso-production -> edcmdt.medtronic.com (PROD account, healthy 2/2)
- argo-oracleclinicalrdcsso-staging -> edcmdt-val.medtronic.com (PROD account, healthy 2/2)
 
Note: the naming is swapped from what may be expected - the "-dev" namespace serves edcmdt-test.medtronic.com, and the "-testing" namespace serves edcmdt-dev.medtronic.com.
 
None of these four hostnames match the originally reported edcmdt-dev-servers.medtronic.com or edcmdt-test-servers.medtronic.com. Those two hostnames were not found in Argo Kubernetes Ingress, ACM, Route 53, or EC2 network interfaces in either the DEV or PROD AWS accounts.
 
Please confirm whether the incident is actually referring to edcmdt-dev.medtronic.com / edcmdt-test.medtronic.com, or whether the "-servers" hostnames are managed outside of Argo/AWS.
Summary for oracleclinicalrdcsso (EDC application):
 
Deployed across 4 namespaces in Argo:
- argo-oracleclinicalrdcsso-dev -> edcmdt-test.medtronic.com (DEV account, healthy 2/2)
- argo-oracleclinicalrdcsso-testing -> edcmdt-dev.medtronic.com (DEV account, degraded - 1 pod unready with repeated restarts)
- argo-oracleclinicalrdcsso-production -> edcmdt.medtronic.com (PROD account, healthy 2/2)
- argo-oracleclinicalrdcsso-staging -> edcmdt-val.medtronic.com (PROD account, healthy 2/2)
 
Note: the naming is swapped from what may be expected - the "-dev" namespace serves edcmdt-test.medtronic.com, and the "-testing" namespace serves edcmdt-dev.medtronic.com.
 
None of these four hostnames match the originally reported edcmdt-dev-servers.medtronic.com or edcmdt-test-servers.medtronic.com. Those two hostnames were not found in Argo Kubernetes Ingress, ACM, Route 53, or EC2 network interfaces in either the DEV or PROD AWS accounts.
 
Please confirm whether the incident is actually referring to edcmdt-dev.medtronic.com / edcmdt-test.medtronic.com, or whether the "-servers" hostnames are managed outside of Argo/AWS.


This is my command for k8s ./kubectl --kubeconfig ~/.kube/argo-dev.yaml -n argo-oracleclinicalrdcsso-dev 