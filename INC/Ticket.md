Look the next issue and my commands, the suggestion of my leader

1. Context
Hi Merrick, Wesley/Eragam Reddy, Mounika,
In adan1-atlas-prd cluster the envoy-oee-gateway-67t2p in the livelink-system namespace has been in Pending state
sunkas6@CPC-sunka-A5WCX:~$ kubectl get pods -n livelink-system
NAME                                   READY   STATUS    RESTARTS   AGE
envoy-oee-gateway-67t2p                0/2     Pending   0          3d14h
Pod is not being scheduled to any node
 
Events:
  Type     Reason            Age                      From               Message
  ----     ------            ----                     ----               -------
  Warning  FailedScheduling  101s (x1059 over 3d14h)  default-scheduler  0/9 nodes are available: 1 Insufficient memory, 8 node(s) didn't satisfy plugin(s) [NodeAffinity]. no new claims to deallocate, preemption: 0/9 nodes are available: 1 No preemption victims found for incoming pod, 8 Preemption is not helpful for scheduling
The Envoy DaemonSet is scheduled only on a limited set of eligible nodes. Two Envoy pods are running successfully:
adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-9s8g7
adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-bx9ct

The third eligible node, adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6, is unable to accommodate the pod due to memory constraints.

Could you please review the node affinity configuration and memory allocation strategy for the gateway nodes, and advise on the recommended remediation (capacity increase, workload redistribution, or affinity adjustment) to allow the pending Envoy pod to be scheduled?
sunkas6@CPC-sunka-A5WCX:~$ kubectl describe node adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6 | egrep "Allocatable:|memory"
  MemoryPressure           False   Wed, 14 Oct 2026 07:00:30 +0000   Sat, 10 Oct 2026 16:11:19 +0000   KubeletHasSufficientMemory   kubelet has sufficient memory available
  memory:             32862028Ki
Allocatable:
  memory:             29069132Ki
  memory             28376Mi (99%)  39168Mi (137%)
 

2. My leader reply:
We need to adjust the priority class for the gateway, or at the very least evict something claiming a lot of memory on the note that pod has its affinity set to
 
DaemonSet deploys one pod to each node, so that particular pod can only schedule to one node exactly, which has all its memory claimed (memory requests from all running pods totals too high for this one to be added)
 
I won't be able to work on that myself with my current OpenShift tasking for the foreseeable future, so I'll leave this up to this team on how you want to address this
 
PriorityClass can probably be added to our ContourDeployment spec here (this is the dev branch so feel free to test changes there) https://code.medtronic.com/io-shared-services/applications/helm-common-resources/-/blob/atlas-tkg-dev-resources/common-resources/templates/contour/gatewayclass.yaml?ref_type=heads
 
The YAML file is (in /atlas-tkg-dev-resources branch):
{{- if and (eq .Values.cluster.provider "tkg") .Values.contour.gatewayProvisioner.enabled }}
kind: GatewayClass
apiVersion: gateway.networking.k8s.io/v1beta1
metadata:
  name: contour
spec:
  controllerName: projectcontour.io/gateway-controller
  parametersRef:
    kind: ContourDeployment
    group: projectcontour.io
    name: contour-and-envoy-params
    namespace: contour-system

---
kind: ContourDeployment
apiVersion: projectcontour.io/v1alpha1
metadata:
  namespace: contour-system
  name: contour-and-envoy-params
spec:
  contour:
    resources:
      limits:
        memory: 256Mi
      requests:
        cpu: 25m
        memory: 50Mi
  envoy:
    resources:
      limits:
        memory: 256Mi
      requests:
        cpu: 25m
        memory: 50Mi
{{- end }}

3. My kubectl commands and the output:
mesac2@CPC-mesac-4BM7S:~$ kubectl describe node adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6
Name:               adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6
Roles:              <none>
Labels:             beta.kubernetes.io/arch=amd64
                    beta.kubernetes.io/os=linux
                    failure-domain.beta.kubernetes.io/zone=vmware-system-legacy
                    feature.node.kubernetes.io/cpu-cpuid.ADX=true
                    feature.node.kubernetes.io/cpu-cpuid.AESNI=true
                    feature.node.kubernetes.io/cpu-cpuid.AMXFP8=true
                    feature.node.kubernetes.io/cpu-cpuid.AVX=true
                    feature.node.kubernetes.io/cpu-cpuid.AVX2=true
                    feature.node.kubernetes.io/cpu-cpuid.AVX512BITALG=true
                    feature.node.kubernetes.io/cpu-cpuid.AVX512BW=true
                    feature.node.kubernetes.io/cpu-cpuid.AVX512CD=true
                    feature.node.kubernetes.io/cpu-cpuid.AVX512DQ=true
                    feature.node.kubernetes.io/cpu-cpuid.AVX512F=true
                    feature.node.kubernetes.io/cpu-cpuid.AVX512IFMA=true
                    feature.node.kubernetes.io/cpu-cpuid.AVX512VBMI=true
                    feature.node.kubernetes.io/cpu-cpuid.AVX512VBMI2=true
                    feature.node.kubernetes.io/cpu-cpuid.AVX512VL=true
                    feature.node.kubernetes.io/cpu-cpuid.AVX512VNNI=true
                    feature.node.kubernetes.io/cpu-cpuid.AVX512VPOPCNTDQ=true
                    feature.node.kubernetes.io/cpu-cpuid.CMPXCHG8=true
                    feature.node.kubernetes.io/cpu-cpuid.FLUSH_L1D=true
                    feature.node.kubernetes.io/cpu-cpuid.FMA3=true
                    feature.node.kubernetes.io/cpu-cpuid.FSRM=true
                    feature.node.kubernetes.io/cpu-cpuid.FXSR=true
                    feature.node.kubernetes.io/cpu-cpuid.FXSROPT=true
                    feature.node.kubernetes.io/cpu-cpuid.GFNI=true
                    feature.node.kubernetes.io/cpu-cpuid.HYPERVISOR=true
                    feature.node.kubernetes.io/cpu-cpuid.IA32_ARCH_CAP=true
                    feature.node.kubernetes.io/cpu-cpuid.IBPB=true
                    feature.node.kubernetes.io/cpu-cpuid.LAHF=true
                    feature.node.kubernetes.io/cpu-cpuid.MD_CLEAR=true
                    feature.node.kubernetes.io/cpu-cpuid.MOVBE=true
                    feature.node.kubernetes.io/cpu-cpuid.OSXSAVE=true
                    feature.node.kubernetes.io/cpu-cpuid.PSFD=true
                    feature.node.kubernetes.io/cpu-cpuid.SHA=true
                    feature.node.kubernetes.io/cpu-cpuid.SPEC_CTRL_SSBD=true
                    feature.node.kubernetes.io/cpu-cpuid.STIBP=true
                    feature.node.kubernetes.io/cpu-cpuid.SYSCALL=true
                    feature.node.kubernetes.io/cpu-cpuid.SYSEE=true
                    feature.node.kubernetes.io/cpu-cpuid.VAES=true
                    feature.node.kubernetes.io/cpu-cpuid.VPCLMULQDQ=true
                    feature.node.kubernetes.io/cpu-cpuid.WBNOINVD=true
                    feature.node.kubernetes.io/cpu-cpuid.X87=true
                    feature.node.kubernetes.io/cpu-cpuid.XGETBV1=true
                    feature.node.kubernetes.io/cpu-cpuid.XSAVE=true
                    feature.node.kubernetes.io/cpu-cpuid.XSAVEC=true
                    feature.node.kubernetes.io/cpu-cpuid.XSAVEOPT=true
                    feature.node.kubernetes.io/cpu-cpuid.XSAVES=true
                    feature.node.kubernetes.io/cpu-hardware_multithreading=false
                    feature.node.kubernetes.io/cpu-model.family=6
                    feature.node.kubernetes.io/cpu-model.id=106
                    feature.node.kubernetes.io/cpu-model.vendor_id=Intel
                    feature.node.kubernetes.io/kernel-config.NO_HZ=true
                    feature.node.kubernetes.io/kernel-config.NO_HZ_IDLE=true
                    feature.node.kubernetes.io/kernel-version.full=5.15.0-160-generic
                    feature.node.kubernetes.io/kernel-version.major=5
                    feature.node.kubernetes.io/kernel-version.minor=15
                    feature.node.kubernetes.io/kernel-version.revision=0
                    feature.node.kubernetes.io/pci-15ad.present=true
                    feature.node.kubernetes.io/system-os_release.ID=ubuntu
                    feature.node.kubernetes.io/system-os_release.VERSION_ID=22.04
                    feature.node.kubernetes.io/system-os_release.VERSION_ID.major=22
                    feature.node.kubernetes.io/system-os_release.VERSION_ID.minor=04
                    kubernetes.io/arch=amd64
                    kubernetes.io/hostname=adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6
                    kubernetes.io/os=linux
                    node.cluster.x-k8s.io/esxi-host=adan1-esxi06.corp.medtronic.com
                    run.tanzu.vmware.com/kubernetesDistributionVersion=v1.34.1---vmware.1-vkr.4
                    run.tanzu.vmware.com/tkr=v1.34.1---vmware.1-vkr.4
                    topology.kubernetes.io/zone=vmware-system-legacy
                    vks.vmware.com/nodepool=worker-node-pool
Annotations:        alpha.kubernetes.io/provided-node-ip: 172.19.0.62
                    cluster.x-k8s.io/annotations-from-machine:
                    cluster.x-k8s.io/cluster-name: adan1-atlas-prd
                    cluster.x-k8s.io/cluster-namespace: adan1-prod-ns
                    cluster.x-k8s.io/labels-from-machine: node.cluster.x-k8s.io/esxi-host
                    cluster.x-k8s.io/machine: adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6
                    cluster.x-k8s.io/owner-kind: MachineSet
                    cluster.x-k8s.io/owner-name: adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk
                    csi.volume.kubernetes.io/nodeid:
                      {"csi.oneagent.dynatrace.com":"adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6","csi.vsphere.vmware.com":"adan1-atlas-prd-worker-node-p...
                    machine-agent.vks.vmware.com/last-applied-config: 2026-10-10T16:11:22Z
                    nfd.node.kubernetes.io/feature-labels:
                      cpu-cpuid.ADX,cpu-cpuid.AESNI,cpu-cpuid.AMXFP8,cpu-cpuid.AVX,cpu-cpuid.AVX2,cpu-cpuid.AVX512BITALG,cpu-cpuid.AVX512BW,cpu-cpuid.AVX512CD,c...
                    node.alpha.kubernetes.io/ttl: 0
                    volumes.kubernetes.io/controller-managed-attach-detach: true
CreationTimestamp:  Sat, 10 Oct 2026 16:11:19 +0000
Taints:             <none>
Unschedulable:      false
Lease:              Failed to get lease: leases.coordination.k8s.io "adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6" is forbidden: User "sso:mesac2@ent.core.medtronic.com" cannot get resource "leases" in API group "coordination.k8s.io" in the namespace "kube-node-lease"
Conditions:
  Type                     Status  LastHeartbeatTime                 LastTransitionTime                Reason                       Message
  ----                     ------  -----------------                 ------------------                ------                       -------
  MemoryPressure           False   Wed, 14 Oct 2026 16:06:25 +0000   Sat, 10 Oct 2026 16:11:19 +0000   KubeletHasSufficientMemory   kubelet has sufficient memory available
  DiskPressure             False   Wed, 14 Oct 2026 16:06:25 +0000   Sat, 10 Oct 2026 16:11:19 +0000   KubeletHasNoDiskPressure     kubelet has no disk pressure
  PIDPressure              False   Wed, 14 Oct 2026 16:06:25 +0000   Sat, 10 Oct 2026 16:11:19 +0000   KubeletHasSufficientPID      kubelet has sufficient PID available
  Ready                    True    Wed, 14 Oct 2026 16:06:25 +0000   Sat, 10 Oct 2026 16:11:25 +0000   KubeletReady                 kubelet is posting ready status
  MachineConfigOutOfDate   False   Mon, 01 Jan 0001 00:00:00 +0000   Sat, 10 Oct 2026 16:11:22 +0000   Success                      Machineadm configuration applied successfully
Addresses:
  InternalIP:  172.19.0.62
  Hostname:
Capacity:
  cpu:                8
  ephemeral-storage:  25625808Ki
  hugepages-1Gi:      0
  hugepages-2Mi:      0
  memory:             32862028Ki
  pods:               110
Allocatable:
  cpu:                7915m
  ephemeral-storage:  23616744614
  hugepages-1Gi:      0
  hugepages-2Mi:      0
  memory:             29069132Ki
  pods:               110
System Info:
  Machine ID:                 f47cc80a4b7f49db8b3d4dcc49db322b
  System UUID:                fd491342-9c28-ea8e-aeb7-8a85d1cecaf8
  Boot ID:                    e7987eae-f1b7-4881-a1d8-b4f2bbd60380
  Kernel Version:             5.15.0-160-generic
  OS Image:                   Ubuntu 22.04.5 LTS
  Operating System:           linux
  Architecture:               amd64
  Container Runtime Version:  containerd://2.1.4+vmware.3-fips
  Kubelet Version:            v1.34.1+vmware.1
  Kube-Proxy Version:
PodCIDR:                      100.64.18.0/24
PodCIDRs:                     100.64.18.0/24
ProviderID:                   vsphere://421349fd-289c-8eea-aeb7-8a85d1cecaf8
Non-terminated Pods:          (49 in total)
  Namespace                   Name                                                      CPU Requests  CPU Limits   Memory Requests  Memory Limits  Age
  ---------                   ----                                                      ------------  ----------   ---------------  -------------  ---
  ami-mde-db                  ami-mde-db-mde-prd-cluster-1                              100m (1%)     100m (1%)    2Gi (7%)         2Gi (7%)       3d23h
  ami-mde-db                  ami-mde-db-mde-prd-cluster-2                              100m (1%)     100m (1%)    2Gi (7%)         2Gi (7%)       3d23h
  atlas-system                kubescape-node-agent-wvcjj                                100m (1%)     500m (6%)    180Mi (0%)       700Mi (2%)     3d23h
  atlas-system                kubescape-storage-87b86b58f-q2tgg                         100m (1%)     1500m (18%)  400Mi (1%)       1500Mi (5%)    5d12h
  atlas-system                rbac-manager-86976c7b6f-9l9jz                             100m (1%)     100m (1%)    128Mi (0%)       128Mi (0%)     5d12h
  cert-manager                cert-manager-65bb4b7446-4hk68                             100m (1%)     0 (0%)       200Mi (0%)       512Mi (1%)     5d12h
  cert-manager                cert-manager-65bb4b7446-vfqzg                             100m (1%)     0 (0%)       200Mi (0%)       512Mi (1%)     5d12h
  cert-manager                cert-manager-cainjector-5877b687dc-4rsms                  10m (0%)      0 (0%)       64Mi (0%)        128Mi (0%)     5d12h
  cert-manager                cert-manager-cainjector-5877b687dc-g8bgx                  10m (0%)      0 (0%)       64Mi (0%)        128Mi (0%)     5d12h
  cert-manager                cert-manager-webhook-5c564bbfcd-2vwnh                     10m (0%)      0 (0%)       32Mi (0%)        32Mi (0%)      5d12h
  cert-manager                cert-manager-webhook-5c564bbfcd-6tkjx                     10m (0%)      0 (0%)       32Mi (0%)        32Mi (0%)      5d12h
  cert-manager                cert-manager-webhook-5c564bbfcd-sv9hh                     10m (0%)      0 (0%)       32Mi (0%)        32Mi (0%)      5d12h
  cnpg-system                 cloudnative-pg-75d7696cb5-rvpr2                           100m (1%)     0 (0%)       256Mi (0%)       256Mi (0%)     5d12h
  cnpg-system                 cloudnative-pg-75d7696cb5-wlz8k                           100m (1%)     0 (0%)       256Mi (0%)       256Mi (0%)     5d12h
  confluent-system            confluent-operator-77f55d5978-drtbv                       100m (1%)     500m (6%)    256Mi (0%)       512Mi (1%)     5d12h
  confluent-system            confluent-operator-77f55d5978-rqxrj                       100m (1%)     500m (6%)    256Mi (0%)       512Mi (1%)     5d12h
  confluent-system            confluent-operator-77f55d5978-tlvhk                       100m (1%)     500m (6%)    256Mi (0%)       512Mi (1%)     5d12h
  contour-system              contour-contour-88c7c6c9b-qrcnl                           100m (1%)     150m (1%)    128Mi (0%)       192Mi (0%)     5d12h
  contour-system              contour-envoy-l5xbs                                       200m (2%)     300m (3%)    256Mi (0%)       384Mi (1%)     3d23h
  contour-system              contour-gateway-provisioner-68d5bd54dc-4flnv              100m (1%)     0 (0%)       128Mi (0%)       256Mi (0%)     5d12h
  dynatrace-operator          adan1-atlas-prd-activegate-0                              250m (3%)     0 (0%)       768Mi (2%)       2Gi (7%)       5d12h
  dynatrace-operator          adan1-atlas-prd-otel-collector-0                          0 (0%)        0 (0%)       0 (0%)           0 (0%)         5d12h
  dynatrace-operator          dynatrace-fluent-bit-h4bnb                                100m (1%)     0 (0%)       128Mi (0%)       128Mi (0%)     3d23h
  dynatrace-operator          dynatrace-oneagent-csi-driver-477zn                       190m (2%)     90m (1%)     260Mi (0%)       360Mi (1%)     3d23h
  dynatrace-operator          dynatrace-webhook-86457b7fc7-qq575                        300m (3%)     300m (3%)    128Mi (0%)       128Mi (0%)     5d12h
  dynatrace-operator          dynatrace-webhook-86457b7fc7-tzb7c                        300m (3%)     300m (3%)    128Mi (0%)       128Mi (0%)     5d12h
  external-secrets            external-secrets-55777f75bd-8pmvd                         100m (1%)     0 (0%)       128Mi (0%)       128Mi (0%)     5d12h
  external-secrets            external-secrets-webhook-c566b5766-gfkht                  10m (0%)      0 (0%)       32Mi (0%)        64Mi (0%)      5d12h
  flux-system                 helm-controller-6d98cf5d6-4rnrh                           100m (1%)     1 (12%)      64Mi (0%)        1Gi (3%)       5d12h
  flux-system                 kustomize-controller-5f7c645849-wrdfr                     100m (1%)     1 (12%)      64Mi (0%)        1Gi (3%)       5d12h
  flux-system                 source-controller-7c74d5c6d8-cq4zm                        50m (0%)      1 (12%)      64Mi (0%)        1Gi (3%)       5d12h
  gpu-operator                gpu-operator-node-feature-discovery-gc-bdbc64d47-45l6t    10m (0%)      0 (0%)       128Mi (0%)       1Gi (3%)       5d12h
  gpu-operator                gpu-operator-node-feature-discovery-worker-prrfl          5m (0%)       0 (0%)       64Mi (0%)        512Mi (1%)     3d23h
  kube-system                 antrea-agent-9zpqt                                        200m (2%)     0 (0%)       0 (0%)           0 (0%)         3d23h
  kube-system                 kube-proxy-zh44f                                          0 (0%)        0 (0%)       0 (0%)           0 (0%)         3d23h
  kyverno-system              kyverno-admission-controller-5655b8f7fc-hqwpb             100m (1%)     100m (1%)    128Mi (0%)       384Mi (1%)     5d12h
  kyverno-system              kyverno-admission-controller-5655b8f7fc-vwwbf             100m (1%)     100m (1%)    128Mi (0%)       384Mi (1%)     5d12h
  kyverno-system              kyverno-admission-controller-5655b8f7fc-w8csb             100m (1%)     100m (1%)    128Mi (0%)       384Mi (1%)     5d12h
  kyverno-system              kyverno-background-controller-65cd987565-knbt8            100m (1%)     0 (0%)       64Mi (0%)        200Mi (0%)     5d12h
  kyverno-system              kyverno-background-controller-65cd987565-szzmn            100m (1%)     0 (0%)       64Mi (0%)        200Mi (0%)     5d12h
  kyverno-system              kyverno-cleanup-controller-5d9f8df4d-669ml                100m (1%)     0 (0%)       64Mi (0%)        200Mi (0%)     5d12h
  kyverno-system              kyverno-cleanup-controller-5d9f8df4d-8bk6k                100m (1%)     0 (0%)       64Mi (0%)        200Mi (0%)     5d12h
  kyverno-system              kyverno-reports-controller-5c5cbf88f9-dzgnr               100m (1%)     0 (0%)       64Mi (0%)        256Mi (0%)     5d12h
  kyverno-system              kyverno-reports-controller-5c5cbf88f9-w2j4c               100m (1%)     0 (0%)       64Mi (0%)        256Mi (0%)     5d12h
  livelink-db                 livelink-db-01-cluster-1                                  100m (1%)     100m (1%)    6Gi (21%)        6Gi (21%)      3d23h
  livelink-db                 livelink-db-01-cluster-3                                  100m (1%)     100m (1%)    6Gi (21%)        6Gi (21%)      3d23h
  livelink-db                 livelink-db-site-custom-cluster-1                         100m (1%)     100m (1%)    2Gi (7%)         2Gi (7%)       3d23h
  livelink-db                 livelink-db-timescaledb-cluster-1                         100m (1%)     100m (1%)    4Gi (14%)        4Gi (14%)      3d23h
  vmware-system-csi           vsphere-csi-node-77xgr                                    0 (0%)        0 (0%)       0 (0%)           0 (0%)         3d23h
Allocated resources:
  (Total limits may be over 100 percent, i.e., overcommitted.)
  Resource           Requests       Limits
  --------           --------       ------
  cpu                4665m (58%)    8640m (109%)
  memory             28376Mi (99%)  39168Mi (137%)
  ephemeral-storage  150Mi (0%)     6Gi (27%)
  hugepages-1Gi      0 (0%)         0 (0%)
  hugepages-2Mi      0 (0%)         0 (0%)
Events:              <none>
mesac2@CPC-mesac-4BM7S:~$
mesac2@CPC-mesac-4BM7S:~$ kubectl explain contourdeployment.spec.envoy --api-version=projectcontour.io/v1alpha1 | grep -i priority
mesac2@CPC-mesac-4BM7S:~$ kubectl get pod envoy-oee-gateway-67t2p -n livelink-system -o jsonpath='{range .spec.containers[*]}{.name}{": "}{.resources.requests.memory}{"\n"}{end}'
shutdown-manager: 50Mi
envoy: 50Mi
mesac2@CPC-mesac-4BM7S:~$ kubectl get clusters.postgresql.cnpg.io -A
NAMESPACE     NAME                              AGE   INSTANCES   READY   STATUS                     PRIMARY
ami-mde-db    ami-mde-db-mde-prd-cluster        69d   3           3       Cluster in healthy state   ami-mde-db-mde-prd-cluster-3
livelink-db   livelink-db-01-cluster            40d   3           3       Cluster in healthy state   livelink-db-01-cluster-2
livelink-db   livelink-db-platform-cluster      40d   3           3       Cluster in healthy state   livelink-db-platform-cluster-2
livelink-db   livelink-db-site-custom-cluster   40d   3           3       Cluster in healthy state   livelink-db-site-custom-cluster-3
livelink-db   livelink-db-timescaledb-cluster   40d   3           3       Cluster in healthy state   livelink-db-timescaledb-cluster-2
mesac2@CPC-mesac-4BM7S:~$ kubectl get pods -n livelink-db -L role,cnpg.io/instanceRole -o wide
NAME                                READY   STATUS    RESTARTS   AGE     IP             NODE
                               NOMINATED NODE   READINESS GATES   ROLE      INSTANCEROLE
livelink-db-01-cluster-1            1/1     Running   0          3d23h   100.64.18.62   adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6   <none>           <none>            replica   replica
livelink-db-01-cluster-2            1/1     Running   0          3d23h   100.64.21.11   adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-bx9ct   <none>           <none>            primary   primary
livelink-db-01-cluster-3            1/1     Running   0          3d23h   100.64.18.65   adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6   <none>           <none>            replica   replica
livelink-db-platform-cluster-1      1/1     Running   0          41h     100.64.21.33   adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-bx9ct   <none>           <none>            replica   replica
livelink-db-platform-cluster-2      1/1     Running   0          3d23h   100.64.21.25   adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-bx9ct   <none>           <none>            primary   primary
livelink-db-platform-cluster-3      1/1     Running   0          41h     100.64.25.18   adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-9s8g7   <none>           <none>            replica   replica
livelink-db-site-custom-cluster-1   1/1     Running   0          3d23h   100.64.18.64   adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6   <none>           <none>            replica   replica
livelink-db-site-custom-cluster-2   1/1     Running   0          41h     100.64.25.20   adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-9s8g7   <none>           <none>            replica   replica
livelink-db-site-custom-cluster-3   1/1     Running   0          3d23h   100.64.21.24   adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-bx9ct   <none>           <none>            primary   primary
livelink-db-timescaledb-cluster-1   1/1     Running   0          3d23h   100.64.18.61   adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6   <none>           <none>            replica   replica
livelink-db-timescaledb-cluster-2   1/1     Running   0          3d23h   100.64.21.10   adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-bx9ct   <none>           <none>            primary   primary
livelink-db-timescaledb-cluster-3   1/1     Running   0          3d23h   100.64.21.23   adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-bx9ct   <none>           <none>            replica   replica
mesac2@CPC-mesac-4BM7S:~$ kubectl get pods -n ami-mde-db -L role,cnpg.io/instanceRole -o wide
NAME                           READY   STATUS    RESTARTS   AGE     IP             NODE
                          NOMINATED NODE   READINESS GATES   ROLE      INSTANCEROLE
ami-mde-db-mde-prd-cluster-1   1/1     Running   0          3d23h   100.64.18.63   adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6   <none>           <none>            replica   replica
ami-mde-db-mde-prd-cluster-2   1/1     Running   0          3d23h   100.64.18.66   adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6   <none>           <none>            replica   replica
ami-mde-db-mde-prd-cluster-3   1/1     Running   0          3d23h   100.64.21.14   adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-bx9ct   <none>           <none>            primary   primary
mesac2@CPC-mesac-4BM7S:~$ kubectl describe node adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-9s8g7 | grep -A6 "Allocated resources"
Allocated resources:
  (Total limits may be over 100 percent, i.e., overcommitted.)
  Resource           Requests       Limits
  --------           --------       ------
  cpu                3920m (49%)    18390m (232%)
  memory             16896Mi (59%)  44040Mi (155%)
  ephemeral-storage  100Mi (0%)     4Gi (18%)
mesac2@CPC-mesac-4BM7S:~$ kubectl explain contourdeployment.spec.envoy --api-version=projectcontour.io/v1alpha1 --recursive | grep -i -B2 -A2 priority
mesac2@CPC-mesac-4BM7S:~$ kubectl get crd contourdeployments.projectcontour.io -o yaml | grep -i priority
mesac2@CPC-mesac-4BM7S:~$


4. My team did the next:
stuve revisando por qué el pod de Envoy está en Pending en adan1-atlas-prd.
1. Primero vimos el error del pod:
kubectl describe pod envoy-oee-gateway-67t2p -n livelink-system
El scheduler reporta Insufficient memory y que 8 nodos no cumplen con NodeAffinity.
2. Revisamos la memoria del worker:
kubectl describe node adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6
Encontramos que tiene el 99% de la memoria reservada por requests, aunque el consumo real es mucho menor.
3. Revisamos la regla de afinidad de Envoy:
kubectl get pod envoy-oee-gateway-67t2p \
  -n livelink-system \
  -o jsonpath='{.spec.affinity}{"\n"}'
Encontramos una regla requiredDuringSchedulingIgnoredDuringExecution que obliga al pod a ejecutarse exclusivamente en ese worker.
4. Revisamos los workers disponibles:
kubectl get nodes -o wide
El clúster tiene otros workers disponibles, pero Envoy no puede utilizarlos por esa regla.
5. Revisamos las bases de datos PostgreSQL:
kubectl get pods -n livelink-db \
  -l cnpg.io/cluster=livelink-db-01-cluster \
  -o wide
Encontramos que dos instancias de PostgreSQL comparten ese worker.
6. Verificamos sus recursos:
kubectl get cluster livelink-db-01-cluster \
  -n livelink-db \
  -o jsonpath='{.spec.resources}{"\n"}'
Cada instancia reserva 6 GiB de RAM.
En conclusión, tenemos una restricción de afinidad que obliga a Envoy a ejecutarse en un nodo con la memoria casi totalmente comprometida. El scheduler sigue reportando falta de memoria, aunque debemos validar la diferencia con la capacidad calculada.
Creo que deberíamos revisar la regla de afinidad y la distribución de las cargas antes de modificar algo en producción.
 
tambien revise la afinidad y las prioridades de los otros pods del worker.
Para revisar la afinidad de un pod usamos:
kubectl get pod <pod-name> -n <namespace> -o yaml
Y para consultar las prioridades de los pods del nodo:
kubectl get pods -A \
  --field-selector spec.nodeName=adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6 \
  -o custom-columns='NAMESPACE:.metadata.namespace,NAME:.metadata.name,PRIORITY:.spec.priority'
Encontramos que Envoy tiene prioridad 0, varios pods también tienen prioridad 0 y PostgreSQL tiene 20500.
 
Por eso, una prioridad de 1000 podría ser una buena opción: pondría a Envoy por encima de los pods con prioridad 0, pero por debajo de PostgreSQL.
La regla de NodeAffinity no representa un conflicto, porque Envoy seguiría asignado al mismo worker. Lo que cambiaría sería su capacidad de desplazar pods de menor prioridad para liberar memoria.
Antes de aplicarlo, debemos validar cuáles serían los pods afectados por la preemption.

Another team
The pod is pending because two things are happening at the same time:
The pod is restricted by node affinity to only a small set of nodes.
At least one of those eligible nodes does not have enough free allocatable memory to satisfy the pod’s request.
So even though the pod is valid, Kubernetes cannot find a node where it both fits and matches the affinity rules.
Recommended step-by-step approach
1. Confirm the scheduling constraints
Check exactly what the pod requires:
bash
 
Shell
kubectl get pod <pod-name> -n <namespace> -o yaml
Look for:
affinity
resources.requests.memory
resources.limits.memory
any tolerations
whether it is a DaemonSet or Deployment
If it is a DaemonSet, remember:
it will try to place one pod on every eligible node
if one eligible node cannot fit it, that pod stays pending on that node
2. Check the eligible nodes and available memory
List the nodes that match the affinity rules:
bash
 
Shell
kubectl get nodes --show-labels
Then inspect memory pressure and allocatable memory on each eligible node:
bash
 
Shell
kubectl describe node <node-name> | egrep "Allocatable:|MemoryPressure|memory"kubectl top node <node-name>
Compare:
requested memory from running pods
allocatable memory on the node
actual usage and headroom
If the node is near capacity, the pod will not schedule even if the node is otherwise eligible.
3. Decide the fastest remediation
You usually have three options.
Option A: Increase capacity
Best if the gateway is important and should always be schedulable.
Actions:
add another node to the eligible node pool
resize the node pool to larger memory nodes
increase memory capacity on the dedicated gateway nodes
This is the most reliable fix if the DaemonSet is expected to run consistently.
Option B: Redistribute workloads
Best if the node is full because other workloads are consuming too much memory.
Actions:
move lower-priority workloads off the gateway nodes
reduce memory requests for noncritical pods if they are oversized
rebalance workloads across other pools
This helps if the current node pool is being overcommitted by unrelated workloads.
Option C: Adjust affinity
Best if the gateway does not truly need such a narrow node selection.
Actions:
broaden the node affinity to include more nodes
use node labels to target a larger eligible pool
avoid overly restrictive affinity unless the workload truly needs it
Be careful here: loosening affinity may solve scheduling but could reduce isolation or performance guarantees.
4. Use PriorityClass for the gateway
If the gateway is critical, give it a higher scheduling priority so it can preempt less important workloads when needed.
What to do:
create or update a PriorityClass
assign it to the gateway pod template
ensure preemption behavior is acceptable in your cluster
This is useful when the gateway must win over other workloads.
However, priority alone does not create capacity. If the eligible node is genuinely full and preemption is not helpful, you still need memory headroom or more capacity.
5. Review memory requests, not just actual usage
Scheduling is based primarily on memory requests, not just observed runtime usage.
So even if the node seems to have some free memory, the pod can still fail if:
the total requested memory from all running pods exceeds allocatable memory
the gateway request is larger than the remaining request budget
Check:
bash
 
Shell
kubectl get pods -n <namespace> -o custom-columns=NAME:.metadata.name,REQUESTS:.spec.containers[*].resources.requests.memory
If requests are inflated, reduce them where appropriate.
6. If this is a DaemonSet, verify the design intent
Since a DaemonSet runs one pod per eligible node, ask:
do we truly need the gateway on only 2 or 3 nodes?
is that node set intentionally small?
should the gateway run on a dedicated pool?
If the answer is yes, then the node pool should be sized specifically for the DaemonSet.
If not, broaden the eligible node set.
7. Validate the fix before merging
If you are testing in a dev branch or Helm chart, validate with:
a dry-run manifest review
a staging deployment
kubectl describe pod after rollout
confirm the pod leaves Pending and reaches Running
For example:
bash
 
Shell
kubectl rollout restart ds/<daemonset-name> -n <namespace>kubectl get pods -n <namespace> -w


Best-practice prevention plan
1. Reserve enough headroom on gateway nodes
Do not run gateway nodes at 99 percent of allocatable memory.
Keep a buffer for:
system overhead
sudden pod bursts
DaemonSet placement
node-level processes
A common operational practice is to keep significant headroom, especially on dedicated infrastructure.
2. Use a dedicated node pool for gateway traffic
If the gateway is critical, isolate it on a node pool sized for:
the gateway DaemonSet
kube/system overhead
expected growth
This prevents general workloads from starving the gateway.
3. Set a PriorityClass for critical networking components
Make sure critical pods can out-prioritize less important work.
Use this carefully:
it helps only if there is something to preempt
it should be part of a broader capacity plan
don’t rely on priority alone as the only protection
4. Keep requests realistic
Avoid over-requesting memory for noncritical workloads.
If requests are too high, scheduling becomes artificially constrained.
Review:
actual usage
request/limit ratios
historical peaks
Then tune accordingly.
5. Add monitoring and alerts
Set alerts for:
node allocatable memory consumption
memory pressure
pending pods
failed scheduling events
DaemonSet unavailable pods
This lets you catch the issue before the pod becomes stuck for days.
6. Treat node affinity as a capacity decision
If a workload can only run on a few nodes, those nodes must be sized as a dedicated service tier.
In other words:
narrow affinity means you need stronger capacity planning
broad affinity means more flexibility, but less isolation
Recommended remediation order for your case
If I were prioritizing the fix, I would suggest:
Confirm the gateway pod’s memory request and affinity rules.
Check how much memory is already requested on the eligible nodes.
Increase memory capacity on the eligible node pool if the gateway must remain restricted.
If capacity cannot be increased quickly, move or reduce lower-priority workloads.
Add or raise PriorityClass for the gateway so it can preempt less important pods.
Only widen affinity if the gateway does not require such a strict node set.
Short version you can send to the team
The pod is pending because the DaemonSet is restricted by node affinity to a small set of nodes, and one of the eligible nodes does not have enough allocatable memory for the pod request. To prevent this, we should either increase capacity on the gateway node pool, redistribute or reduce memory usage of lower-priority workloads, or relax affinity if the gateway does not need to stay on such a narrow set of nodes. For critical traffic, adding a higher PriorityClass is also recommended so the gateway can preempt lower-priority pods when necessary.

5. I have the repo of the cluster
https://code.medtronic.com/io-shared-services/atlas/kubernetes-management/adan1-atlas-prd
Name	Last commit	Last update
.gitlab/merge_request_templates	Added Hosted Zone ID and copier first
2 months ago
.port	chore: update from cluster template v1.71.1
22 hours ago
cluster	chore(): Update HelmRelease manifest for platform cnpg-postgres-cluster [ci skip]
1 day ago
crds	Added Hosted Zone ID and copier first
2 months ago
external-secret-store	chore: update from cluster template v1.71.0
1 week ago
flux	Add Flux v2.6.1 component manifests
2 months ago
.copier-answers.yml	chore: update from cluster template v1.71.1
22 hours ago
.editorconfig	Added Hosted Zone ID and copier first
2 months ago
.gitlab-ci.yml	Added Hosted Zone ID and copier first
2 months ago
.sops.yaml	Added Hosted Zone ID and copier first
2 months ago
README.md	Added Hosted Zone ID and copier first
2 months ago
adan1-atlas-prd.yaml	normalize k8s release version name

7. Claude (AI) did the next:
The explain | grep returned nothing, so this Contour version's ContourDeployment has no priorityClassName field. I'm reverting my earlier chart changes.

values.yaml+0-6
gatewayclass.yaml+0-16
Thought for a few seconds
I've removed the PriorityClass changes I added earlier: the new contour.envoy values block and the template parts in gatewayclass.yaml. Both files should now match the original. The Envoy DaemonSet is generated by Contour, so a priority class can't be set from this chart. That means moving a DB replica is the fix.

What your output shows
The Envoy pod only needs 100Mi (two containers at 50Mi each), but xq6j6 has only about 11Mi free.
All 15 Postgres instances run on just 3 nodes, and spread unevenly:
Node	Primaries	Replicas
xq6j6	0	6: livelink-db-01 ×2, ami-mde ×2, site-custom, timescaledb
bx9ct	5 (every primary)	3
9s8g7	0	2
High-availability risk:
livelink-db-01 and ami-mde-db have both replicas on xq6j6. Losing that node would leave each cluster with only its primary.
timescaledb and platform have their primary and a replica together on bx9ct.
All five primaries are on bx9ct, so losing that node triggers five failovers at once.
Fix
Move one replica off xq6j6. The best choice is ami-mde-db-mde-prd-cluster-2: it's a replica, it's the smallest (2Gi), and its twin (member 1) is on the same node.

Check that 9s8g7 has at least 2Gi of unrequested memory:
If it doesn't have room, stop here and escalate for more capacity.
Move the replica (needs permission to cordon nodes and delete pods in ami-mde-db):
Wait until it's Running and 1/1 on another node.
Envoy should then schedule into the freed 2Gi. If the replica's volume can't attach on the new node, the replica will stay Pending. Check kubectl describe pod and kubectl get events -n ami-mde-db.
Prevent it happening again
Required anti-affinity on each CNPG Cluster (in the repos that own the databases):
There are exactly 3 worker nodes, so with this setting one more node loss leaves instances Pending. Plan this with the DB owners.
Spread the primaries: use CNPG switchovers (kubectl cnpg promote) so the five primaries aren't all on bx9ct.
Capacity: about 22Gi of guaranteed DB memory per node, on 28Gi allocatable nodes, leaves no headroom. Ask for a 4th worker node or bigger nodes.
Envoy priority: a Kyverno mutate policy could set priorityClassName on the envoy-* DaemonSets. The Contour provisioner may undo that change, so test it on dev first.