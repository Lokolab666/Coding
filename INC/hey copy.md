I have a new incident

Detailed description of issue:
Script (manifests repo, main branch):
scripts/hivemq-expand-pvc/expand-hivemq-pvc.sh
https://medtronic.gitlab-dedicated.com/Operations_Innovation/digital-transformation/manifests/-/blob/main/scripts/hivemq-expand-pvc/expand-hivemq-pvc.sh

Commands (after cloning/pulling manifests main):

git clone/pull manifests repo main
TARGET_SIZE=100Gi ./scripts/hivemq-expand-pvc/expand-hivemq-pvc.sh
SITE_LABEL=pnl ./scripts/livelink-sanity-check.sh

Verify: All PVCs and STS volumeClaimTemplates show 100Gi.


I did some commands at the cluster
kubectl config use-context aply3-atlas-prd
Switched to context "aply3-atlas-prd".
mesac2@CPC-mesac-4BM7S:~$ kubectl get ns
NAME                                 STATUS   AGE
ami-mde                              Active   70d
ami-mde-db                           Active   55d
ami-svp                              Active   70d
ami-system                           Active   70d
atlas-system                         Active   232d
atlas-tenants                        Active   232d
cert-manager                         Active   232d
cnpg-system                          Active   232d
confluent-system                     Active   12d
contour-system                       Active   232d
default                              Active   239d
dynatrace-operator                   Active   232d
external-secrets                     Active   232d
flow-aggregator                      Active   239d
flux-system                          Active   232d
gpu-operator                         Active   230d
hivemq-operator                      Active   232d
kube-node-lease                      Active   239d
kube-public                          Active   239d
kube-system                          Active   239d
kyverno-system                       Active   232d
livelink-db                          Active   34d
livelink-device                      Active   34d
livelink-hivemq                      Active   34d
livelink-ignition                    Active   34d
livelink-kafka                       Active   34d
livelink-system                      Active   35d
secretgen-controller                 Active   239d
tkg-system                           Active   239d
traefik-gateway                      Active   232d
v2containers-hivemq                  Active   125d
v2containers-ignition                Active   125d
v2containers-kafka-connect           Active   125d
v2containers-postgres-db             Active   70d
v2containers-system                  Active   70d
velero-vsphere-plugin-backupdriver   Active   239d
vmware-system-antrea                 Active   148d
vmware-system-auth                   Active   239d
vmware-system-cloud-provider         Active   239d
vmware-system-csi                    Active   239d
vmware-system-tkg                    Active   239d

So, I request permissions for to see the .sh file in the URL. Idk if I need do more question