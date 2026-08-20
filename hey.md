kubectl get pod envoy-backend-gateway-2gw44 -n cis-genai-poc-system -o jsonpath='{.spec.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[*].matchFields[*].values}'
["awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-5pltm"]mesac2@CPC-mesac-4BM7S:~$ kubectl config current-context
awby1-atlas-prd
mesac2@CPC-mesac-4BM7S:~$  kubectl describe node awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-5pltm
Name:               awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-5pltm
Roles:              <none>
Labels:             beta.kubernetes.io/arch=amd64
                    beta.kubernetes.io/os=linux
                    failure-domain.beta.kubernetes.io/zone=domain-c8
                    kubernetes.io/arch=amd64
                    kubernetes.io/hostname=awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-5pltm
                    kubernetes.io/os=linux
                    node.cluster.x-k8s.io/esxi-host=awby1-w2c1-esxi05.corp.medtronic.com
                    run.tanzu.vmware.com/kubernetesDistributionVersion=v1.32.0---vmware.6-fips-vkr.2
                    run.tanzu.vmware.com/tkr=v1.32.0---vmware.6-fips-vkr.2
                    topology.kubernetes.io/zone=domain-c8
Annotations:        alpha.kubernetes.io/provided-node-ip: 172.16.160.51
                    cluster.x-k8s.io/annotations-from-machine:
                    cluster.x-k8s.io/cluster-name: awby1-atlas-prd
                    cluster.x-k8s.io/cluster-namespace: awby1-prod-ns
                    cluster.x-k8s.io/labels-from-machine: node.cluster.x-k8s.io/esxi-host
                    cluster.x-k8s.io/machine: awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-5pltm
                    cluster.x-k8s.io/owner-kind: MachineSet
                    cluster.x-k8s.io/owner-name: awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b
                    csi.volume.kubernetes.io/nodeid:
                      {"csi.oneagent.dynatrace.com":"awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-5pltm","csi.vsphere.vmware.com":"awby1-atlas-prd-worker-node-p...
                    kubeadm.alpha.kubernetes.io/cri-socket: unix:///var/run/containerd/containerd.sock
                    node.alpha.kubernetes.io/ttl: 0
                    volumes.kubernetes.io/controller-managed-attach-detach: true
CreationTimestamp:  Tue, 21 Oct 2025 20:03:54 +0000
Taints:             <none>
Unschedulable:      false
Lease:              Failed to get lease: leases.coordination.k8s.io "awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-5pltm" is forbidden: User "sso:mesac2@ent.core.medtronic.com" cannot get resource "leases" in API group "coordination.k8s.io" in the namespace "kube-node-lease"
Conditions:
  Type             Status  LastHeartbeatTime                 LastTransitionTime                Reason                       Message
  ----             ------  -----------------                 ------------------                ------                       -------
  MemoryPressure   False   Thu, 20 Aug 2026 22:03:43 +0000   Thu, 23 Jul 2026 20:10:51 +0000   KubeletHasSufficientMemory   kubelet has sufficient memory available
  DiskPressure     False   Thu, 20 Aug 2026 22:03:43 +0000   Thu, 23 Jul 2026 20:10:51 +0000   KubeletHasNoDiskPressure     kubelet has no disk pressure
  PIDPressure      False   Thu, 20 Aug 2026 22:03:43 +0000   Thu, 23 Jul 2026 20:10:51 +0000   KubeletHasSufficientPID      kubelet has sufficient PID available
  Ready            True    Thu, 20 Aug 2026 22:03:43 +0000   Thu, 23 Jul 2026 20:10:51 +0000   KubeletReady                 kubelet is posting ready status
Addresses:
  InternalIP:  172.16.160.51
  Hostname:
Capacity:
  cpu:                8
  ephemeral-storage:  25625808Ki
  hugepages-1Gi:      0
  hugepages-2Mi:      0
  memory:             32862124Ki
  pods:               110
Allocatable:
  cpu:                7915m
  ephemeral-storage:  23616744614
  hugepages-1Gi:      0
  hugepages-2Mi:      0
  memory:             29069228Ki
  pods:               110
System Info:
  Machine ID:                 60e7c70472eb489cbd632537ec9ee10e
  System UUID:                651d1542-f569-468f-a8d6-624b8df66c7a
  Boot ID:                    0d126d36-78ae-47d7-925b-0671d3572522
  Kernel Version:             5.15.0-131-generic
  OS Image:                   Ubuntu 22.04.5 LTS
  Operating System:           linux
  Architecture:               amd64
  Container Runtime Version:  containerd://1.7.25+vmware.2-fips
  Kubelet Version:            v1.32.0+vmware.6-fips
  Kube-Proxy Version:         v1.32.0+vmware.6-fips
PodCIDR:                      100.64.7.0/24
PodCIDRs:                     100.64.7.0/24
ProviderID:                   vsphere://42151d65-69f5-8f46-a8d6-624b8df66c7a
Non-terminated Pods:          (25 in total)
  Namespace                   Name                                                                  CPU Requests  CPU Limits  Memory Requests  Memory Limits  Age
  ---------                   ----                                                                  ------------  ----------  ---------------  -------------  ---
  atlas-system                kubescape-node-agent-nsq6j                                            100m (1%)     500m (6%)   180Mi (0%)       700Mi (2%)     134d
  cert-manager                cert-manager-7c4866444b-z5gn5                                         100m (1%)     100m (1%)   200Mi (0%)       512Mi (1%)     149d
  cert-manager                cert-manager-webhook-7786f5c8dd-d799v                                 10m (0%)      0 (0%)      32Mi (0%)        32Mi (0%)      302d
  cis-genai-poc-db            cis-genai-poc-db-cis-genai-poc-cluster-4                              100m (1%)     100m (1%)   4Gi (14%)        4Gi (14%)      8d
  cis-genai-poc-incai-db      cis-genai-poc-incai-db-cis-genai-poc-incai-cluster-2                  100m (1%)     100m (1%)   4Gi (14%)        4Gi (14%)      8d
  cnpg-system                 cloudnative-pg-59b4568767-wnqr7                                       100m (1%)     100m (1%)   256Mi (0%)       256Mi (0%)     8d
  contour-system              contour-envoy-dwhcl                                                   200m (2%)     300m (3%)   256Mi (0%)       384Mi (1%)     302d
  dynatrace-operator          awby1-atlas-prd-otel-collector-0                                      0 (0%)        0 (0%)      0 (0%)           0 (0%)         65d
  dynatrace-operator          dynatrace-fluent-bit-nwdz2                                            100m (1%)     0 (0%)      128Mi (0%)       128Mi (0%)     302d
  dynatrace-operator          dynatrace-oneagent-csi-driver-mm54r                                   190m (2%)     90m (1%)    260Mi (0%)       360Mi (1%)     70d
  external-secrets            external-secrets-webhook-8687f4f5fc-224hm                             30m (0%)      100m (1%)   32Mi (0%)        64Mi (0%)      36d
  flux-system                 helm-controller-757b969798-8lpwf                                      100m (1%)     1 (12%)     64Mi (0%)        1Gi (3%)       302d
  flux-system                 source-controller-855bcc59bb-tfgq9                                    50m (0%)      1 (12%)     64Mi (0%)        1Gi (3%)       302d
  kube-system                 antrea-agent-wvt94                                                    400m (5%)     0 (0%)      0 (0%)           0 (0%)         303d
  kube-system                 docker-registry-awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-5pltm    0 (0%)        0 (0%)      0 (0%)           0 (0%)         303d
  kube-system                 kube-proxy-g6lmc                                                      0 (0%)        0 (0%)      0 (0%)           0 (0%)         303d
  kyverno-system              kyverno-admission-controller-54bc7f8f-qbdvc                           100m (1%)     100m (1%)   128Mi (0%)       384Mi (1%)     135d
  kyverno-system              kyverno-background-controller-f5c7b88c4-g4dnh                         100m (1%)     100m (1%)   64Mi (0%)        200Mi (0%)     135d
  kyverno-system              kyverno-reports-controller-784d5f479b-xtwqw                           100m (1%)     100m (1%)   64Mi (0%)        256Mi (0%)     135d
  livelink-db                 livelink-db-01-cluster-5                                              100m (1%)     100m (1%)   4Gi (14%)        4Gi (14%)      8d
  livelink-db                 livelink-db-platform-cluster-2                                        100m (1%)     100m (1%)   4Gi (14%)        4Gi (14%)      8d
  livelink-db                 livelink-db-site-custom-cluster-3                                     100m (1%)     100m (1%)   2Gi (7%)         2Gi (7%)       3d2h
  livelink-db                 livelink-db-timescaledb-cluster-2                                     100m (1%)     100m (1%)   4Gi (14%)        4Gi (14%)      3d2h
  livelink-db                 livelink-db-timescaledb-cluster-3                                     100m (1%)     100m (1%)   4Gi (14%)        4Gi (14%)      3d2h
  vmware-system-csi           vsphere-csi-node-mbv5r                                                0 (0%)        0 (0%)      0 (0%)           0 (0%)         303d
Allocated resources:
  (Total limits may be over 100 percent, i.e., overcommitted.)
  Resource           Requests       Limits
  --------           --------       ------
  cpu                2380m (30%)    4190m (52%)
  memory             28352Mi (99%)  31948Mi (112%)
  ephemeral-storage  100Mi (0%)     4Gi (18%)
  hugepages-1Gi      0 (0%)         0 (0%)
  hugepages-2Mi      0 (0%)         0 (0%)
Events:              <none>
mesac2@CPC-mesac-4BM7S:~$ kubectl get daemonset -n cis-genai-poc-system
NAME                          DESIRED   CURRENT   READY   UP-TO-DATE   AVAILABLE   NODE SELECTOR   AGE
envoy-backend-gateway         5         5         4       5            4           <none>          146d
envoy-frontend-gateway        5         5         4       5            4           <none>          149d
envoy-grafana-gateway         5         5         4       5            4           <none>          126d
envoy-itops-backend-gateway   5         5         4       5            4           <none>          79d
envoy-keycloak-gateway        5         5         4       5            4           <none>          157d
envoy-ticket-audit-gateway    5         5         4       5            4           <none>          104d
mesac2@CPC-mesac-4BM7S:~$ kubectl get pods -n cis-genai-poc-system -l app=envoy-backend-gateway -o wide
NAME                          READY   STATUS    RESTARTS   AGE     IP             NODE
  NOMINATED NODE   READINESS GATES
envoy-backend-gateway-2gw44   0/2     Pending   0          2d14h   <none>         <none>
  <none>           <none>
envoy-backend-gateway-7zp72   2/2     Running   0          20d     100.64.9.140   awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-5vbwl   <none>           <none>
envoy-backend-gateway-bjhww   2/2     Running   0          5h19m   100.64.2.12    awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-xwdnw   <none>           <none>
envoy-backend-gateway-hftgt   2/2     Running   0          20d     100.64.3.193   awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-jqnvr   <none>           <none>
envoy-backend-gateway-l4gj2   2/2     Running   0          5h19m   100.64.4.5     awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-bknvv   <none>           <none>
mesac2@CPC-mesac-4BM7S:~$ kubectl describe pod itops-nginx-86c87cf945-btgzn -n cis-genai-poc-system
Name:             itops-nginx-86c87cf945-btgzn
Namespace:        cis-genai-poc-system
Priority:         0
Service Account:  default
Node:             <none>
Labels:           app=itops-nginx
                  pod-template-hash=86c87cf945
Annotations:      dynakube.dynatrace.com/injected: true
                  kubectl.kubernetes.io/restartedAt: 2026-07-27T11:07:57Z
                  metadata-enrichment.dynatrace.com/injected: true
                  metadata.dynatrace.com: {}
                  metadata.dynatrace.com/k8s.workload.kind: deployment
                  metadata.dynatrace.com/k8s.workload.name: itops-nginx
                  oneagent.dynatrace.com/injected: true
Status:           Pending
IP:
IPs:              <none>
Controlled By:    ReplicaSet/itops-nginx-86c87cf945
Init Containers:
  dynatrace-operator:
    Image:           case.artifacts.medtronic.com/ext-docker-aws-remote/dynatrace/dynatrace-operator:v1.9.0
    Port:            <none>
    Host Port:       <none>
    SeccompProfile:  RuntimeDefault
    Args:
      bootstrap
      --config-directory=/mnt/config
      --input-directory=/mnt/input
      --suppress-error
      --source=/opt/dynatrace/oneagent
      --target=/mnt/bin
      --install-path=/opt/dynatrace/oneagent-paas
      --attribute=k8s.workload.kind=deployment
      --attribute=k8s.workload.name=itops-nginx
      --attribute=dt.kubernetes.workload.kind=deployment
      --attribute=dt.kubernetes.workload.name=itops-nginx
      --metadata-enrichment
      --attribute-container={"container_image.registry":"case.artifacts.medtronic.com","container_image.repository":"genai-project-team-docker-dev-local/itops-nginx-medtronics","container_image.tags":"Version1","k8s.container.name":"itops-nginx"}
      --attribute=k8s.pod.name=$(K8S_PODNAME)
      --attribute=k8s.pod.uid=$(K8S_PODUID)
      --attribute=k8s.node.name=$(K8S_NODE_NAME)
      --attribute=k8s.namespace.name=cis-genai-poc-system
      --attribute=k8s.cluster.uid=2d798f2e-6740-4435-8747-8fc15de6aaa6
      --attribute=k8s.cluster.name=awby1-atlas-prd
      --attribute=dt.entity.kubernetes_cluster=KUBERNETES_CLUSTER-9FC59BE3DB6FBE81
      --attribute=dt.kubernetes.cluster.id=2d798f2e-6740-4435-8747-8fc15de6aaa6
    Limits:
      cpu:     100m
      memory:  60Mi
    Requests:
      cpu:     30m
      memory:  30Mi
    Environment:
      K8S_PODNAME:    itops-nginx-86c87cf945-btgzn (v1:metadata.name)
      K8S_PODUID:      (v1:metadata.uid)
      K8S_NODE_NAME:   (v1:spec.nodeName)
    Mounts:
      /mnt/bin from oneagent-bin (ro)
      /mnt/config from dynatrace-config (rw)
      /mnt/input from dynatrace-input (ro)
Containers:
  itops-nginx:
    Image:           case.artifacts.medtronic.com/genai-project-team-docker-dev-local/itops-nginx-medtronics:Version1
    Ports:           8080/TCP, 8443/TCP
    Host Ports:      0/TCP, 0/TCP
    SeccompProfile:  RuntimeDefault
    Limits:
      cpu:     1
      memory:  2Gi
    Requests:
      cpu:     500m
      memory:  1Gi
    Environment:
      DT_DEPLOYMENT_METADATA:  orchestration_tech=Operator-application_monitoring;script_version=v1.9.0;orchestrator_id=2d798f2e-6740-4435-8747-8fc15de6aaa6
      LD_PRELOAD:              /opt/dynatrace/oneagent-paas/agent/lib64/liboneagentproc.so
      DT_STORAGE:              /var/lib/dynatrace/oneagent
    Mounts:
      /etc/ld.so.preload from dynatrace-config (rw,path="oneagent/ld.so.preload")
      /etc/nginx/nginx.conf from itops-nginx-config (rw,path="nginx.conf")
      /etc/ssl/certs from tls (ro)
      /opt/dynatrace/oneagent-paas from oneagent-bin (ro)
      /run from run-dir (rw)
      /var/cache/nginx from nginx-cache (rw)
      /var/lib/dynatrace from dynatrace-config (rw,path="itops-nginx")
Conditions:
  Type           Status
  PodScheduled   False
Volumes:
  itops-nginx-config:
    Type:      ConfigMap (a volume populated by a ConfigMap)
    Name:      itops-nginx-config
    Optional:  false
  tls:
    Type:        Secret (a volume populated by a Secret)
    SecretName:  itops-tls
    Optional:    false
  nginx-cache:
    Type:       EmptyDir (a temporary directory that shares a pod's lifetime)
    Medium:
    SizeLimit:  <unset>
  run-dir:
    Type:       EmptyDir (a temporary directory that shares a pod's lifetime)
    Medium:
    SizeLimit:  <unset>
  oneagent-bin:
    Type:              CSI (a Container Storage Interface (CSI) volume source)
    Driver:            csi.oneagent.dynatrace.com
    FSType:
    ReadOnly:          true
    VolumeAttributes:      dynakube=awby1-atlas-prd
                           mode=app
                           retryTimeout=10m0s
  dynatrace-input:
    Type:                Projected (a volume that contains injected data from multiple sources)
    SecretName:          dynatrace-bootstrapper-config
    SecretOptionalName:  0xc00099a015
    SecretName:          dynatrace-bootstrapper-certs
    SecretOptionalName:  0xc00099a016
  dynatrace-config:
    Type:        EmptyDir (a temporary directory that shares a pod's lifetime)
    Medium:
    SizeLimit:   <unset>
QoS Class:       Burstable
Node-Selectors:  kubernetes.io/hostname=awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-5pltm
Tolerations:     node.kubernetes.io/not-ready:NoExecute op=Exists for 300s
                 node.kubernetes.io/unreachable:NoExecute op=Exists for 300s
Events:
  Type     Reason            Age                     From               Message
  ----     ------            ----                    ----               -------
  Warning  FailedScheduling  3m59s (x69 over 5h18m)  default-scheduler  0/8 nodes are available: 1 Insufficient memory, 3 node(s) had untolerated taint {node-role.kubernetes.io/control-plane: }, 4 node(s) didn't match Pod's node affinity/selector. preemption: 0/8 nodes are available: 1 No preemption victims found for incoming pod, 7 Preemption is not helpful for scheduling.
mesac2@CPC-mesac-4BM7S:~$ for node in $(kubectl get nodes -o name | grep worker | cut -d/ -f2); do
  echo "===== $node ====="
  kubectl describe node "$node" | sed -n '/Allocated resources:/,/Events:/p'
done
===== awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-5pltm =====
Allocated resources:
  (Total limits may be over 100 percent, i.e., overcommitted.)
  Resource           Requests       Limits
  --------           --------       ------
  cpu                2380m (30%)    4190m (52%)
  memory             28352Mi (99%)  31948Mi (112%)
  ephemeral-storage  100Mi (0%)     4Gi (18%)
  hugepages-1Gi      0 (0%)         0 (0%)
  hugepages-2Mi      0 (0%)         0 (0%)
Events:              <none>
===== awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-5vbwl =====
Allocated resources:
  (Total limits may be over 100 percent, i.e., overcommitted.)
  Resource           Requests       Limits
  --------           --------       ------
  cpu                4645m (58%)    10240m (129%)
  memory             28114Mi (99%)  37776Mi (133%)
  ephemeral-storage  150Mi (0%)     6Gi (27%)
  hugepages-1Gi      0 (0%)         0 (0%)
  hugepages-2Mi      0 (0%)         0 (0%)
Events:              <none>
===== awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-bknvv =====
Allocated resources:
  (Total limits may be over 100 percent, i.e., overcommitted.)
  Resource           Requests         Limits
  --------           --------         ------
  cpu                6540m (82%)      9590m (121%)
  memory             9327744Ki (32%)  17695872Ki (60%)
  ephemeral-storage  100Mi (0%)       4Gi (18%)
  hugepages-1Gi      0 (0%)           0 (0%)
  hugepages-2Mi      0 (0%)           0 (0%)
Events:              <none>
===== awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-jqnvr =====
Allocated resources:
  (Total limits may be over 100 percent, i.e., overcommitted.)
  Resource           Requests       Limits
  --------           --------       ------
  cpu                4400m (55%)    6390m (80%)
  memory             23574Mi (83%)  29584Mi (104%)
  ephemeral-storage  100Mi (0%)     4Gi (18%)
  hugepages-1Gi      0 (0%)         0 (0%)
  hugepages-2Mi      0 (0%)         0 (0%)
Events:              <none>
===== awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-xwdnw =====
Allocated resources:
  (Total limits may be over 100 percent, i.e., overcommitted.)
  Resource           Requests         Limits
  --------           --------         ------
  cpu                6750m (85%)      10290m (130%)
  memory             9692288Ki (33%)  19268736Ki (66%)
  ephemeral-storage  100Mi (0%)       4Gi (18%)
  hugepages-1Gi      0 (0%)           0 (0%)
  hugepages-2Mi      0 (0%)           0 (0%)
Events:              <none>