 kubectl get node "$NODE" -o wide
NAME                                                 STATUS   ROLES    AGE   VERSION            INTERNAL-IP   EXTERNAL-IP   OS-IMAGE             KERNEL-VERSION       CONTAINER-RUNTIME
adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6   Ready    <none>   5d    v1.34.1+vmware.1   172.19.0.62   <none>        Ubuntu 22.04.5 LTS   5.15.0-160-generic   containerd://2.1.4+vmware.3-fips
mesac2@CPC-mesac-4BM7S:~$ kubectl describe node adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6 | egrep "Allocatable:|memory"
  MemoryPressure           False   Thu, 15 Oct 2026 16:34:55 +0000   Thu, 15 Oct 2026 06:37:05 +0000   KubeletHasSufficientMemory   kubelet has sufficient memory available
  memory:             32862028Ki
Allocatable:
  memory:             29069132Ki
  memory             27724Mi (97%)  37032Mi (130%)
mesac2@CPC-mesac-4BM7S:~$ kubectl get node "$NODE" -o wide
NAME                                                 STATUS   ROLES    AGE   VERSION            INTERNAL-IP   EXTERNAL-IP   OS-IMAGE             KERNEL-VERSION       CONTAINER-RUNTIME
adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6   Ready    <none>   5d    v1.34.1+vmware.1   172.19.0.62   <none>        Ubuntu 22.04.5 LTS   5.15.0-160-generic   containerd://2.1.4+vmware.3-fips
mesac2@CPC-mesac-4BM7S:~$ kubectl -n "$NS" get pod "$POD" -o json |
jq '{
  phase: .status.phase,
  node: .spec.nodeName,
  conditions: .status.conditions,
  initContainers: .status.initContainerStatuses,
  containers: .status.containerStatuses
}'
{
  "phase": "Pending",
  "node": "adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6",
  "conditions": [
    {
      "lastProbeTime": null,
      "lastTransitionTime": "2026-10-15T14:30:07Z",
      "observedGeneration": 2,
      "status": "False",
      "type": "PodReadyToStartContainers"
    },
    {
      "lastProbeTime": null,
      "lastTransitionTime": "2026-10-15T08:32:19Z",
      "message": "containers with incomplete status: [envoy-initconfig dynatrace-operator]",
      "observedGeneration": 2,
      "reason": "ContainersNotInitialized",
      "status": "False",
      "type": "Initialized"
    },
    {
      "lastProbeTime": null,
      "lastTransitionTime": "2026-10-15T08:32:19Z",
      "message": "containers with unready status: [shutdown-manager envoy]",
      "observedGeneration": 2,
      "reason": "ContainersNotReady",
      "status": "False",
      "type": "Ready"
    },
    {
      "lastProbeTime": null,
      "lastTransitionTime": "2026-10-15T08:32:19Z",
      "message": "containers with unready status: [shutdown-manager envoy]",
      "observedGeneration": 2,
      "reason": "ContainersNotReady",
      "status": "False",
      "type": "ContainersReady"
    },
    {
      "lastProbeTime": null,
      "lastTransitionTime": "2026-10-15T08:30:08Z",
      "observedGeneration": 2,
      "status": "True",
      "type": "PodScheduled"
    }
  ],
  "initContainers": [
    {
      "allocatedResources": {
        "cpu": "25m",
        "memory": "50Mi"
      },
      "containerID": "containerd://37988fbb95f8a1053e3364c5a68e38120bd3e8540f91856224dd1d3d5bef6447",
      "image": "case.artifacts.medtronic.com/ext-docker-hub-remote/bitnamilegacy/contour:1.32.1-debian-12-r0",
      "imageID": "case.artifacts.medtronic.com/ext-docker-hub-remote/bitnamilegacy/contour@sha256:ab0665888c91c754030d46e7bbaf9a661bd1f16bbbbafcef59ce0fafdba871f1",
      "lastState": {},
      "name": "envoy-initconfig",
      "ready": false,
      "resources": {
        "limits": {
          "cpu": "50m",
          "memory": "100Mi"
        },
        "requests": {
          "cpu": "25m",
          "memory": "50Mi"
        }
      },
      "restartCount": 0,
      "started": false,
      "state": {
        "terminated": {
          "containerID": "containerd://37988fbb95f8a1053e3364c5a68e38120bd3e8540f91856224dd1d3d5bef6447",
          "exitCode": 2,
          "finishedAt": "2026-10-15T14:26:50Z",
          "reason": "Error",
          "startedAt": "2026-10-15T14:25:16Z"
        }
      },
      "user": {
        "linux": {
          "gid": 65534,
          "supplementalGroups": [
            65534
          ],
          "uid": 65534
        }
      },
      "volumeMounts": [
        {
          "mountPath": "/certs",
          "name": "envoycert",
          "readOnly": true,
          "recursiveReadOnly": "Disabled"
        },
        {
          "mountPath": "/config",
          "name": "envoy-config"
        }
      ]
    },
    {
      "image": "case.artifacts.medtronic.com/ext-docker-aws-remote/dynatrace/dynatrace-operator:v1.9.0",
      "imageID": "",
      "lastState": {},
      "name": "dynatrace-operator",
      "ready": false,
      "restartCount": 0,
      "started": false,
      "state": {
        "waiting": {
          "reason": "PodInitializing"
        }
      },
      "volumeMounts": [
        {
          "mountPath": "/mnt/bin",
          "name": "oneagent-bin",
          "readOnly": true,
          "recursiveReadOnly": "Disabled"
        },
        {
          "mountPath": "/mnt/config",
          "name": "dynatrace-config"
        },
        {
          "mountPath": "/mnt/input",
          "name": "dynatrace-input",
          "readOnly": true,
          "recursiveReadOnly": "Disabled"
        }
      ]
    }
  ],
  "containers": [
    {
      "image": "case.artifacts.medtronic.com/ext-docker-hub-remote/bitnamilegacy/envoy:1.34.5-debian-12-r0",
      "imageID": "",
      "lastState": {},
      "name": "envoy",
      "ready": false,
      "restartCount": 0,
      "started": false,
      "state": {
        "waiting": {
          "reason": "PodInitializing"
        }
      },
      "volumeMounts": [
        {
          "mountPath": "/certs",
          "name": "envoycert",
          "readOnly": true,
          "recursiveReadOnly": "Disabled"
        },
        {
          "mountPath": "/config",
          "name": "envoy-config",
          "readOnly": true,
          "recursiveReadOnly": "Disabled"
        },
        {
          "mountPath": "/admin",
          "name": "envoy-admin"
        },
        {
          "mountPath": "/opt/dynatrace/oneagent-paas",
          "name": "oneagent-bin",
          "readOnly": true,
          "recursiveReadOnly": "Disabled"
        },
        {
          "mountPath": "/etc/ld.so.preload",
          "name": "dynatrace-config"
        },
        {
          "mountPath": "/var/lib/dynatrace",
          "name": "dynatrace-config"
        }
      ]
    },
    {
      "image": "case.artifacts.medtronic.com/ext-docker-hub-remote/bitnamilegacy/contour:1.32.1-debian-12-r0",
      "imageID": "",
      "lastState": {},
      "name": "shutdown-manager",
      "ready": false,
      "restartCount": 0,
      "started": false,
      "state": {
        "waiting": {
          "reason": "PodInitializing"
        }
      },
      "volumeMounts": [
        {
          "mountPath": "/admin",
          "name": "envoy-admin"
        },
        {
          "mountPath": "/opt/dynatrace/oneagent-paas",
          "name": "oneagent-bin",
          "readOnly": true,
          "recursiveReadOnly": "Disabled"
        },
        {
          "mountPath": "/etc/ld.so.preload",
          "name": "dynatrace-config"
        },
        {
          "mountPath": "/var/lib/dynatrace",
          "name": "dynatrace-config"
        }
      ]
    }
  ]
}
mesac2@CPC-mesac-4BM7S:~$ kubectl -n "$NS" describe pod "$POD"
Name:                      envoy-oee-gateway-67t2p
Namespace:                 livelink-system
Priority:                  0
Service Account:           envoy-oee-gateway
Node:                      adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6/172.19.0.62
Start Time:                Thu, 15 Oct 2026 08:32:19 +0000
Labels:                    app=envoy-oee-gateway
                           app.kubernetes.io/component=ingress-controller
                           app.kubernetes.io/instance=oee-gateway
                           app.kubernetes.io/managed-by=contour-gateway-provisioner
                           app.kubernetes.io/name=contour
                           controller-revision-hash=79d99f94f8
                           gateway.networking.k8s.io/gateway-name=oee-gateway
                           pod-template-generation=1
                           projectcontour.io/owning-gateway-name=oee-gateway
Annotations:               dynakube.dynatrace.com/injected: true
                           metadata-enrichment.dynatrace.com/injected: true
                           metadata.dynatrace.com: {}
                           metadata.dynatrace.com/k8s.workload.kind: daemonset
                           metadata.dynatrace.com/k8s.workload.name: envoy-oee-gateway
                           oneagent.dynatrace.com/injected: true
Status:                    Terminating (lasts 125m)
Termination Grace Period:  300s
IP:                        100.64.18.67
IPs:
  IP:           100.64.18.67
Controlled By:  DaemonSet/envoy-oee-gateway
Init Containers:
  envoy-initconfig:
    Container ID:  containerd://37988fbb95f8a1053e3364c5a68e38120bd3e8540f91856224dd1d3d5bef6447
    Image:         case.artifacts.medtronic.com/ext-docker-hub-remote/bitnamilegacy/contour:1.32.1-debian-12-r0
    Image ID:      case.artifacts.medtronic.com/ext-docker-hub-remote/bitnamilegacy/contour@sha256:ab0665888c91c754030d46e7bbaf9a661bd1f16bbbbafcef59ce0fafdba871f1
    Port:          <none>
    Host Port:     <none>
    Command:
      contour
    Args:
      bootstrap
      /config/envoy.json
      --xds-address=contour-oee-gateway
      --xds-port=8001
      --xds-resource-version=v3
      --resources-dir=/config/resources
      --envoy-cafile=/certs/ca.crt
      --envoy-cert-file=/certs/tls.crt
      --envoy-key-file=/certs/tls.key
      --overload-max-heap=0
      --overload-downstream-max-conn=0
    State:          Terminated
      Reason:       Error
      Exit Code:    2
      Started:      Thu, 15 Oct 2026 14:25:16 +0000
      Finished:     Thu, 15 Oct 2026 14:26:50 +0000
    Ready:          False
    Restart Count:  0
    Limits:
      cpu:     50m
      memory:  100Mi
    Requests:
      cpu:     25m
      memory:  50Mi
    Environment:
      CONTOUR_NAMESPACE:  livelink-system (v1:metadata.namespace)
    Mounts:
      /certs from envoycert (ro)
      /config from envoy-config (rw)
  dynatrace-operator:
    Container ID:
    Image:           case.artifacts.medtronic.com/ext-docker-aws-remote/dynatrace/dynatrace-operator:v1.9.0
    Image ID:
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
      --attribute=k8s.workload.kind=daemonset
      --attribute=k8s.workload.name=envoy-oee-gateway
      --attribute=dt.kubernetes.workload.kind=daemonset
      --attribute=dt.kubernetes.workload.name=envoy-oee-gateway
      --metadata-enrichment
      --attribute-container={"container_image.registry":"case.artifacts.medtronic.com","container_image.repository":"ext-docker-hub-remote/bitnamilegacy/contour","container_image.tags":"1.32.1-debian-12-r0","k8s.container.name":"shutdown-manager"}
      --attribute-container={"container_image.registry":"case.artifacts.medtronic.com","container_image.repository":"ext-docker-hub-remote/bitnamilegacy/envoy","container_image.tags":"1.34.5-debian-12-r0","k8s.container.name":"envoy"}
      --attribute=k8s.namespace.name=livelink-system
      --attribute=k8s.cluster.uid=728fa293-1692-4ed9-a786-b99e6b27650e
      --attribute=k8s.cluster.name=adan1-atlas-prd
      --attribute=dt.entity.kubernetes_cluster=KUBERNETES_CLUSTER-1EFC92E539F95C3C
      --attribute=k8s.pod.name=$(K8S_PODNAME)
      --attribute=k8s.pod.uid=$(K8S_PODUID)
      --attribute=k8s.node.name=$(K8S_NODE_NAME)
      --attribute=dt.kubernetes.cluster.id=728fa293-1692-4ed9-a786-b99e6b27650e
    State:          Waiting
      Reason:       PodInitializing
    Ready:          False
    Restart Count:  0
    Limits:
      cpu:     100m
      memory:  60Mi
    Requests:
      cpu:     30m
      memory:  30Mi
    Environment:
      K8S_PODNAME:    envoy-oee-gateway-67t2p (v1:metadata.name)
      K8S_PODUID:      (v1:metadata.uid)
      K8S_NODE_NAME:   (v1:spec.nodeName)
    Mounts:
      /mnt/bin from oneagent-bin (ro)
      /mnt/config from dynatrace-config (rw)
      /mnt/input from dynatrace-input (ro)
Containers:
  shutdown-manager:
    Container ID:
    Image:         case.artifacts.medtronic.com/ext-docker-hub-remote/bitnamilegacy/contour:1.32.1-debian-12-r0
    Image ID:
    Port:          <none>
    Host Port:     <none>
    Command:
      /bin/contour
    Args:
      envoy
      shutdown-manager
    State:          Waiting
      Reason:       PodInitializing
    Ready:          False
    Restart Count:  0
    Limits:
      cpu:     50m
      memory:  100Mi
    Requests:
      cpu:     25m
      memory:  50Mi
    Environment:
      DT_DEPLOYMENT_METADATA:  orchestration_tech=Operator-application_monitoring;script_version=v1.9.0;orchestrator_id=728fa293-1692-4ed9-a786-b99e6b27650e
      LD_PRELOAD:              /opt/dynatrace/oneagent-paas/agent/lib64/liboneagentproc.so
      DT_STORAGE:              /var/lib/dynatrace/oneagent
    Mounts:
      /admin from envoy-admin (rw)
      /etc/ld.so.preload from dynatrace-config (rw,path="oneagent/ld.so.preload")
      /opt/dynatrace/oneagent-paas from oneagent-bin (ro)
      /var/lib/dynatrace from dynatrace-config (rw,path="shutdown-manager")
  envoy:
    Container ID:
    Image:         case.artifacts.medtronic.com/ext-docker-hub-remote/bitnamilegacy/envoy:1.34.5-debian-12-r0
    Image ID:
    Port:          8002/TCP
    Host Port:     0/TCP
    Command:
      envoy
    Args:
      -c
      /config/envoy.json
      --service-cluster $(CONTOUR_NAMESPACE)
      --service-node $(ENVOY_POD_NAME)
      --log-level info
      --base-id 0
    State:          Waiting
      Reason:       PodInitializing
    Ready:          False
    Restart Count:  0
    Limits:
      memory:  256Mi
    Requests:
      cpu:      25m
      memory:   50Mi
    Readiness:  http-get http://:8002/ready delay=3s timeout=1s period=4s #success=1 #failure=3
    Environment:
      CONTOUR_NAMESPACE:       livelink-system (v1:metadata.namespace)
      ENVOY_POD_NAME:          envoy-oee-gateway-67t2p (v1:metadata.name)
      DT_DEPLOYMENT_METADATA:  orchestration_tech=Operator-application_monitoring;script_version=v1.9.0;orchestrator_id=728fa293-1692-4ed9-a786-b99e6b27650e
      LD_PRELOAD:              /opt/dynatrace/oneagent-paas/agent/lib64/liboneagentproc.so
      DT_STORAGE:              /var/lib/dynatrace/oneagent
    Mounts:
      /admin from envoy-admin (rw)
      /certs from envoycert (ro)
      /config from envoy-config (ro)
      /etc/ld.so.preload from dynatrace-config (rw,path="oneagent/ld.so.preload")
      /opt/dynatrace/oneagent-paas from oneagent-bin (ro)
      /var/lib/dynatrace from dynatrace-config (rw,path="envoy")
Conditions:
  Type                        Status
  PodReadyToStartContainers   False
  Initialized                 False
  Ready                       False
  ContainersReady             False
  PodScheduled                True
Volumes:
  envoycert:
    Type:        Secret (a volume populated by a Secret)
    SecretName:  envoycert-oee-gateway
    Optional:    false
  envoy-config:
    Type:       EmptyDir (a temporary directory that shares a pod's lifetime)
    Medium:
    SizeLimit:  <unset>
  envoy-admin:
    Type:       EmptyDir (a temporary directory that shares a pod's lifetime)
    Medium:
    SizeLimit:  <unset>
  oneagent-bin:
    Type:              CSI (a Container Storage Interface (CSI) volume source)
    Driver:            csi.oneagent.dynatrace.com
    FSType:
    ReadOnly:          true
    VolumeAttributes:      dynakube=adan1-atlas-prd
                           mode=app
                           retryTimeout=10m0s
  dynatrace-input:
    Type:                Projected (a volume that contains injected data from multiple sources)
    SecretName:          dynatrace-bootstrapper-config
    SecretOptionalName:  0xc0006103e5
    SecretName:          dynatrace-bootstrapper-certs
    SecretOptionalName:  0xc0006103e6
  dynatrace-config:
    Type:        EmptyDir (a temporary directory that shares a pod's lifetime)
    Medium:
    SizeLimit:   <unset>
QoS Class:       Burstable
Node-Selectors:  <none>
Tolerations:     node.kubernetes.io/disk-pressure:NoSchedule op=Exists
                 node.kubernetes.io/memory-pressure:NoSchedule op=Exists
                 node.kubernetes.io/not-ready:NoExecute op=Exists
                 node.kubernetes.io/pid-pressure:NoSchedule op=Exists
                 node.kubernetes.io/unreachable:NoExecute op=Exists
                 node.kubernetes.io/unschedulable:NoSchedule op=Exists
Events:          <none>
mesac2@CPC-mesac-4BM7S:~$ kubectl -n "$NS" get ds "$DS" -o wide
NAME                DESIRED   CURRENT   READY   UP-TO-DATE   AVAILABLE   NODE SELECTOR   AGE   CONTAINERS               IMAGES
                                                                                      SELECTOR
envoy-oee-gateway   3         3         2       3            2           <none>          37d   shutdown-manager,envoy   case.artifacts.medtronic.com/ext-docker-hub-remote/bitnamilegacy/contour:1.32.1-debian-12-r0,case.artifacts.medtronic.com/ext-docker-hub-remote/bitnamilegacy/envoy:1.34.5-debian-12-r0   app=envoy-oee-gateway
mesac2@CPC-mesac-4BM7S:~$ kubectl get events -A \
  --sort-by=.metadata.creationTimestamp |
tail -n 100
default              37m         Warning   PolicyViolation              namespace/ami-mde
                                       policy require-namespace-psa-labels/validate-psa-labels fail: [KP-NS-005] Missing or invalid pod-security.kubernetes.io/audit label. Valid values: baseline, restricted (privileged provides no audit visibility)
livelink-db          37m         Warning   Unhealthy                    pod/livelink-db-site-custom-cluster-1                                        Startup probe failed: Get "https://100.64.18.64:8000/startupz": context deadline exceeded
default              36m         Warning   PolicyViolation              clusterpolicy/require-namespace-psa-labels                                   Namespace dry-run-test-namespace-p7xbz: [validate-psa-labels] fail; [KP-NS-005] Missing pod-security.kubernetes.io/enforce label. Any valid PSA level (privileged, baseline, restricted) is acceptable.
default              36m         Warning   PolicyViolation              namespace/dry-run-test-namespace-p7xbz                                       policy require-namespace-psa-labels/validate-psa-labels fail: [KP-NS-005] Missing pod-security.kubernetes.io/enforce label. Any valid PSA level (privileged, baseline, restricted) is acceptable.
confluent-system     35m         Normal    LeaderElection               lease/confluent-operator-leader-election-lock                                confluent-operator-77f55d5978-rqxrj_63a5d0a0-c018-4355-bd66-0ba6213e6a4b became leader
livelink-system      34m         Warning   HealthCheckFailed            kustomization/oee-adan
                                       health check failed after 5m0.012243228s: timeout waiting for: [Kustomization/livelink-system/oee-workloads status: 'InProgress']
flux-system          31m         Normal    ReconciliationSucceeded      kustomization/gateway-api-crds                                               Reconciliation finished in 650.612555ms, next run in 10m0s
flux-system          30m         Normal    ReconciliationSucceeded      kustomization/gateway-api-pause                                              Reconciliation finished in 103.344709ms, next run in 10m0s
default              29m         Warning   PolicyViolation              namespace/dry-run-test-namespace-v2wkf                                       policy require-namespace-psa-labels/validate-psa-labels fail: [KP-NS-005] Missing pod-security.kubernetes.io/enforce label. Any valid PSA level (privileged, baseline, restricted) is acceptable.
default              29m         Warning   PolicyViolation              clusterpolicy/require-namespace-psa-labels                                   Namespace dry-run-test-namespace-v2wkf: [validate-psa-labels] fail; [KP-NS-005] Missing pod-security.kubernetes.io/enforce label. Any valid PSA level (privileged, baseline, restricted) is acceptable.
livelink-system      8m46s       Normal    ArtifactUpToDate             helmrepository/oee-artifactory                                               artifact up-to-date with remote revision: 'sha256:b6ed28a2fb642f1264d17644a4c47e3a9efa2f66136806b71908d3be439afa53'
livelink-system      29m         Normal    GarbageCollectionSucceeded   helmrepository/oee-artifactory                                               garbage collected 1 artifacts
flux-system          29m         Normal    ReconciliationSucceeded      kustomization/flux-system
                                       Reconciliation finished in 704.159274ms, next run in 10m0s
default              28m         Warning   PolicyViolation              namespace/dry-run-test-namespace-5jbqx                                       policy require-namespace-psa-labels/validate-psa-labels fail: [KP-NS-005] Missing pod-security.kubernetes.io/enforce label. Any valid PSA level (privileged, baseline, restricted) is acceptable.
default              28m         Warning   PolicyViolation              clusterpolicy/require-namespace-psa-labels                                   Namespace dry-run-test-namespace-5jbqx: [validate-psa-labels] fail; [KP-NS-005] Missing pod-security.kubernetes.io/enforce label. Any valid PSA level (privileged, baseline, restricted) is acceptable.
kube-system          28m         Normal    Pulled                       pod/antrea-agent-9zpqt
                                       Container image "localhost:5000/tkg/packages/core/antrea@sha256:bc4541965277165ec9ec83e2a168718856c9b97e15f2ae14caaac13600145172" already present on machine
flux-system          28m         Normal    ReconciliationSucceeded      kustomization/contour-crds
                                       Reconciliation finished in 541.128101ms, next run in 10m0s
kube-system          28m         Normal    Created                      pod/antrea-agent-9zpqt
                                       Created container: antrea-agent
livelink-system      28m         Warning   HealthCheckFailed            kustomization/oee-adan
                                       health check failed after 5m0.013951819s: timeout waiting for: [Kustomization/livelink-system/oee-workloads status: 'InProgress']
confluent-system     28m         Normal    Started                      pod/confluent-operator-77f55d5978-rqxrj                                      Started container confluent-operator
confluent-system     27m         Normal    LeaderElection               lease/confluent-operator-leader-election-lock                                confluent-operator-77f55d5978-rqxrj_04ceb665-3d50-4f8c-8286-3b6406564edc became leader
flux-system          2m49s       Normal    DependencyNotReady           kustomization/customer-components                                            Dependencies do not meet ready condition, retrying in 30s
livelink-db          27m         Warning   Unhealthy                    pod/livelink-db-site-custom-cluster-1                                        (combined from similar events): Readiness probe failed: Get "https://100.64.18.64:8000/readyz": EOF
kube-system          27m         Normal    Started                      pod/antrea-agent-9zpqt
                                       Started container antrea-agent
default              25m         Warning   PolicyViolation              namespace/dry-run-test-namespace-lgjnp                                       policy require-namespace-psa-labels/validate-psa-labels fail: [KP-NS-005] Missing pod-security.kubernetes.io/enforce label. Any valid PSA level (privileged, baseline, restricted) is acceptable.
default              25m         Warning   PolicyViolation              clusterpolicy/require-namespace-psa-labels                                   Namespace dry-run-test-namespace-lgjnp: [validate-psa-labels] fail; [KP-NS-005] Missing pod-security.kubernetes.io/enforce label. Any valid PSA level (privileged, baseline, restricted) is acceptable.
cnpg-system          24m         Warning   Unhealthy                    pod/cloudnative-pg-75d7696cb5-wlz8k                                          (combined from similar events): Readiness probe failed: Get "https://100.64.18.37:9443/readyz": net/http: request canceled (Client.Timeout exceeded while awaiting headers)
cert-manager         26m         Warning   Unhealthy                    pod/cert-manager-65bb4b7446-4hk68                                            Liveness probe failed: Get "http://100.64.18.10:9403/livez": EOF (Client.Timeout exceeded while awaiting headers)
flux-system          23m         Warning   HealthCheckFailed            kustomization/common-components                                              health check failed after 5m0.013118897s: timeout waiting for: [HelmRelease/flux-system/common-resources status: 'InProgress']
cert-manager         25m         Warning   Unhealthy                    pod/cert-manager-65bb4b7446-vfqzg                                            Liveness probe failed: Get "http://100.64.18.22:9403/livez": EOF
cert-manager         24m         Normal    Pulled                       pod/cert-manager-webhook-5c564bbfcd-2vwnh                                    Container image "case.artifacts.medtronic.com/ioss-docker-common-components-local/all-clusters/jetstack/cert-manager-webhook:v1.17.1" already present on machine
livelink-system      22m         Warning   HealthCheckFailed            kustomization/oee-adan
                                       health check failed after 5m0.012054216s: timeout waiting for: [Kustomization/livelink-system/oee-workloads status: 'InProgress']
ami-mde-db           2m22s       Normal    Pulled                       pod/ami-mde-db-mde-prd-cluster-1                                             Container image "case.artifacts.medtronic.com/ext-docker-ghcr-remote/cloudnative-pg/postgresql:17.11" already present on machine
ami-mde-db           2m4s        Normal    Created                      pod/ami-mde-db-mde-prd-cluster-1                                             Created container: postgres
ami-mde-db           73s         Normal    Started                      pod/ami-mde-db-mde-prd-cluster-1                                             Started container postgres
kube-system          21m         Warning   Unhealthy                    pod/antrea-agent-9zpqt
                                       Liveness probe failed: Get "https://localhost:10350/livez": context deadline exceeded
flux-system          20m         Normal    ReconciliationSucceeded      kustomization/gateway-api-crds                                               Reconciliation finished in 660.783931ms, next run in 10m0s
flux-system          20m         Normal    ReconciliationSucceeded      kustomization/gateway-api-pause                                              Reconciliation finished in 74.597914ms, next run in 10m0s
livelink-system      19m         Warning   ReconciliationFailed         kustomization/oee-workloads
                                       Database/livelink-db/audit dry-run failed (InternalError): Internal error occurred: failed calling webhook "mdatabase.cnpg.io": failed to call webhook: Post "https://cnpg-webhook-service.cnpg-system.svc:443/mutate-postgresql-cnpg-io-v1-database?timeout=10s": dial tcp 100.65.66.226:443: connect: connection refused
ami-mde-db           9m26s       Warning   Unhealthy                    pod/ami-mde-db-mde-prd-cluster-1                                             (combined from similar events): Readiness probe failed: Get "https://100.64.18.63:8000/readyz": unexpected EOF
flux-system          19m         Normal    ReconciliationSucceeded      kustomization/flux-system
                                       Reconciliation finished in 601.484712ms, next run in 10m0s
default              18m         Warning   PolicyViolation              namespace/dry-run-test-namespace-7cp77                                       policy require-namespace-psa-labels/validate-psa-labels fail: [KP-NS-005] Missing pod-security.kubernetes.io/enforce label. Any valid PSA level (privileged, baseline, restricted) is acceptable.
default              18m         Warning   PolicyViolation              clusterpolicy/require-namespace-psa-labels                                   Namespace dry-run-test-namespace-7cp77: [validate-psa-labels] fail; [KP-NS-005] Missing pod-security.kubernetes.io/enforce label. Any valid PSA level (privileged, baseline, restricted) is acceptable.
flux-system          18m         Normal    ReconciliationSucceeded      kustomization/contour-crds
                                       Reconciliation finished in 500.330647ms, next run in 10m0s
dynatrace-operator   19m         Warning   Unhealthy                    pod/dynatrace-fluent-bit-h4bnb                                               Readiness probe failed: Get "http://100.64.18.51:2020/api/v1/health": dial tcp 100.64.18.51:2020: connect: connection refused (Client.Timeout exceeded while awaiting headers)
flux-system          17m         Warning   HealthCheckFailed            kustomization/common-components                                              health check failed after 5m0.012439322s: timeout waiting for: [HelmRelease/flux-system/common-resources status: 'InProgress']
livelink-db          19m         Warning   Unhealthy                    pod/livelink-db-site-custom-cluster-1                                        Startup probe failed: Get "https://100.64.18.64:8000/startupz": context deadline exceeded (Client.Timeout exceeded while awaiting headers)
livelink-db          19m         Warning   Unhealthy                    pod/livelink-db-timescaledb-cluster-1                                        Readiness probe failed: Get "https://100.64.18.61:8000/readyz": net/http: request canceled while waiting for connection (Client.Timeout exceeded while awaiting headers)
kube-system          19m         Warning   Unhealthy                    pod/antrea-agent-9zpqt
                                       Readiness probe failed: Get "https://localhost:10350/readyz": context deadline exceeded (Client.Timeout exceeded while awaiting headers)
livelink-system      16m         Warning   HealthCheckFailed            kustomization/oee-adan
                                       health check failed after 5m0.011490738s: timeout waiting for: [Kustomization/livelink-system/oee-workloads status: 'InProgress']
contour-system       18m         Warning   Unhealthy                    pod/contour-envoy-l5xbs
                                       Readiness probe failed: Get "http://100.64.18.56:8002/ready": dial tcp 100.64.18.56:8002: connect: connection refused (Client.Timeout exceeded while awaiting headers)
livelink-hivemq      16m         Warning   FailedMount                  pod/livelink-hivemq-3
                                       MountVolume.SetUp failed for volume "broker-configuration" : failed to sync configmap cache: timed out waiting for the condition
livelink-hivemq      16m         Warning   FailedMount                  pod/livelink-hivemq-3
                                       MountVolume.SetUp failed for volume "extension-configuration-hivemq-kafka-extension" : failed to sync configmap cache: timed out waiting for the condition
livelink-hivemq      16m         Warning   FailedMount                  pod/livelink-hivemq-3
                                       MountVolume.SetUp failed for volume "licenses" : failed to sync secret cache: timed out waiting for the condition
livelink-hivemq      16m         Warning   FailedMount                  pod/livelink-hivemq-3
                                       MountVolume.SetUp failed for volume "pod-info" : failed to sync configmap cache: timed out waiting for the condition
livelink-hivemq      16m         Warning   FailedMount                  pod/livelink-hivemq-3
                                       MountVolume.SetUp failed for volume "dynatrace-input" : failed to sync secret cache: timed out waiting for the condition
livelink-hivemq      16m         Warning   FailedMount                  pod/livelink-hivemq-3
                                       MountVolume.SetUp failed for volume "kube-api-access-9w8jq" : failed to sync configmap cache: timed out waiting for the condition
cnpg-system          5m6s        Normal    Pulled                       pod/cloudnative-pg-75d7696cb5-rvpr2                                          Container image "case.artifacts.medtronic.com/ioss-docker-common-components-local/tkg/cloudnative-pg/cloudnative-pg:1.28.4" already present on machine
contour-system       14m         Normal    Pulled                       pod/contour-envoy-l5xbs
                                       Container image "case.artifacts.medtronic.com/ext-docker-hub-remote/bitnamilegacy/envoy:1.34.5-debian-12-r0" already present on machine
contour-system       14m         Normal    Created                      pod/contour-envoy-l5xbs
                                       Created container: envoy
default              14m         Warning   PolicyViolation              clusterpolicy/require-namespace-psa-labels                                   Namespace dry-run-test-namespace-4nvhz: [validate-psa-labels] fail; [KP-NS-005] Missing pod-security.kubernetes.io/enforce label. Any valid PSA level (privileged, baseline, restricted) is acceptable.
default              14m         Warning   PolicyViolation              namespace/dry-run-test-namespace-4nvhz                                       policy require-namespace-psa-labels/validate-psa-labels fail: [KP-NS-005] Missing pod-security.kubernetes.io/enforce label. Any valid PSA level (privileged, baseline, restricted) is acceptable.
cnpg-system          13m         Normal    LeaderElection               lease/db9c8771.cnpg.io
                                       cloudnative-pg-75d7696cb5-rvpr2_038e8e76-948a-4b14-bc9d-6d13e4817401 became leader
contour-system       13m         Normal    Started                      pod/contour-envoy-l5xbs
                                       Started container envoy
cnpg-system          12m         Warning   Unhealthy                    pod/cloudnative-pg-75d7696cb5-rvpr2                                          Startup probe failed: Get "https://100.64.18.28:9443/readyz": net/http: request canceled while waiting for connection (Client.Timeout exceeded while awaiting headers)
cnpg-system          12m         Warning   Unhealthy                    pod/cloudnative-pg-75d7696cb5-rvpr2                                          Startup probe failed: Get "https://100.64.18.28:9443/readyz": context deadline exceeded
cnpg-system          9m49s       Warning   Unhealthy                    pod/cloudnative-pg-75d7696cb5-rvpr2                                          Readiness probe failed: Get "https://100.64.18.28:9443/readyz": net/http: request canceled while waiting for connection (Client.Timeout exceeded while awaiting headers)
cnpg-system          12m         Warning   Unhealthy                    pod/cloudnative-pg-75d7696cb5-rvpr2                                          Liveness probe failed: Get "https://100.64.18.28:9443/readyz": net/http: request canceled while waiting for connection (Client.Timeout exceeded while awaiting headers)
flux-system          11m         Warning   HealthCheckFailed            kustomization/common-components                                              health check failed after 5m0.012217922s: timeout waiting for: [HelmRelease/flux-system/common-resources status: 'InProgress']
cnpg-system          9m50s       Warning   Unhealthy                    pod/cloudnative-pg-75d7696cb5-rvpr2                                          Readiness probe failed: Get "https://100.64.18.28:9443/readyz": context deadline exceeded (Client.Timeout exceeded while awaiting headers)
cnpg-system          3m3s        Warning   Unhealthy                    pod/cloudnative-pg-75d7696cb5-rvpr2                                          Readiness probe failed: Get "https://100.64.18.28:9443/readyz": context deadline exceeded
cnpg-system          9m26s       Warning   Unhealthy                    pod/cloudnative-pg-75d7696cb5-rvpr2                                          Readiness probe failed: Get "https://100.64.18.28:9443/readyz": net/http: request canceled (Client.Timeout exceeded while awaiting headers)
cnpg-system          9m51s       Warning   Unhealthy                    pod/cloudnative-pg-75d7696cb5-rvpr2                                          Liveness probe failed: Get "https://100.64.18.28:9443/readyz": context deadline exceeded
livelink-system      10m         Normal    Progressing                  kustomization/oee-workloads
                                       HelmRepository/livelink-system/hivemq configured
livelink-system      10m         Normal    ReconciliationSucceeded      kustomization/oee-workloads
                                       Reconciliation finished in 5.962845401s, next run in 3m0s
livelink-system      10m         Normal    NewArtifact                  helmrepository/hivemq
                                       stored fetched index of size 312.1kB from 'https://case.artifacts.medtronic.com/artifactory/ext-helm-hivemq-virtual/'
livelink-system      10m         Normal    ReconciliationSucceeded      kustomization/oee-adan
                                       Reconciliation finished in 4m25.165946451s, next run in 10m0s
livelink-system      10m         Normal    Progressing                  kustomization/oee-adan
                                       Health check passed in 4m25.019956782s
flux-system          10m         Normal    ReconciliationSucceeded      kustomization/gateway-api-crds                                               Reconciliation finished in 660.793618ms, next run in 10m0s
cert-manager         11m         Warning   Unhealthy                    pod/cert-manager-webhook-5c564bbfcd-2vwnh                                    Liveness probe failed: Get "http://100.64.18.11:6080/livez": context deadline exceeded (Client.Timeout exceeded while awaiting headers)
flux-system          10m         Normal    ReconciliationSucceeded      kustomization/gateway-api-pause                                              Reconciliation finished in 102.516599ms, next run in 10m0s
kube-system          11m         Warning   Unhealthy                    pod/antrea-agent-9zpqt
                                       Liveness probe failed: Get "https://localhost:10350/livez": net/http: request canceled while waiting for connection (Client.Timeout exceeded while awaiting headers)
flux-system          9m41s       Normal    ReconciliationSucceeded      kustomization/flux-system
                                       Reconciliation finished in 593.131774ms, next run in 10m0s
cnpg-system          10m         Warning   Unhealthy                    pod/cloudnative-pg-75d7696cb5-rvpr2                                          Liveness probe failed: Get "https://100.64.18.28:9443/readyz": net/http: request canceled (Client.Timeout exceeded while awaiting headers)
flux-system          8m36s       Normal    ReconciliationSucceeded      kustomization/contour-crds
                                       Reconciliation finished in 420.828053ms, next run in 10m0s
cnpg-system          9m34s       Normal    Killing                      pod/cloudnative-pg-75d7696cb5-rvpr2                                          Container manager failed liveness probe, will be restarted
dynatrace-operator   8m9s        Normal    Inject                       dynakube/adan1-atlas-prd
                                       Injecting the necessary info into pod  in namespace default
kube-system          7m34s       Warning   FailedKillPod                pod/node-debugger-adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6-vwl9c   error killing pod: [failed to "KillContainer" for "debugger" with KillContainerError: "rpc error: code = DeadlineExceeded desc = stream terminated by RST_STREAM with error code: CANCEL", failed to "KillPodSandbox" for "987854f6-dfe1-4e12-b1d0-a569bf520886" with KillPodSandboxError: "rpc error: code = DeadlineExceeded desc = context deadline exceeded"]
flux-system          5m46s       Warning   HealthCheckFailed            kustomization/common-components                                              health check failed after 5m0.014533024s: timeout waiting for: [HelmRelease/flux-system/common-resources status: 'InProgress']
cert-manager         5m46s       Warning   Unhealthy                    pod/cert-manager-webhook-5c564bbfcd-2vwnh                                    Readiness probe failed: Get "http://100.64.18.11:6080/healthz": dial tcp 100.64.18.11:6080: connect: connection refused (Client.Timeout exceeded while awaiting headers)
cnpg-system          4m20s       Normal    LeaderElection               lease/db9c8771.cnpg.io
                                       cloudnative-pg-75d7696cb5-rvpr2_dbe4b809-e98b-4f35-b29c-88d37ebdcefb became leader
flux-system          4m5s        Normal    ArtifactUpToDate             helmchart/flux-system-dynatrace-operator                                     artifact up-to-date with remote revision: '1.9.0'
default              3m21s       Warning   PolicyViolation              namespace/dry-run-test-namespace-tvfqw                                       policy require-namespace-psa-labels/validate-psa-labels fail: [KP-NS-005] Missing pod-security.kubernetes.io/enforce label. Any valid PSA level (privileged, baseline, restricted) is acceptable.
default              3m21s       Warning   PolicyViolation              clusterpolicy/require-namespace-psa-labels                                   Namespace dry-run-test-namespace-tvfqw: [validate-psa-labels] fail; [KP-NS-005] Missing pod-security.kubernetes.io/enforce label. Any valid PSA level (privileged, baseline, restricted) is acceptable.
flux-system          2m37s       Normal    ArtifactUpToDate             helmchart/flux-system-gpu-operator                                           artifact up-to-date with remote revision: 'v25.10.0'
kube-system          75s         Warning   Unhealthy                    pod/antrea-agent-9zpqt
                                       Liveness probe failed: Get "https://localhost:10350/livez": net/http: request canceled (Client.Timeout exceeded while awaiting headers)
livelink-system      66s         Normal    GarbageCollectionSucceeded   helmrepository/hivemq
                                       garbage collected 1 artifacts
livelink-system      66s         Normal    ArtifactUpToDate             helmrepository/hivemq
                                       artifact up-to-date with remote revision: 'sha256:69883f6fd673ba226c7d0cdd1e7a54c5da9e62f81850748b5d6cb334a1a2f569'
flux-system          47s         Normal    ReconciliationSucceeded      kustomization/gateway-api-crds                                               Reconciliation finished in 650.469883ms, next run in 10m0s
flux-system          25s         Normal    ReconciliationSucceeded      kustomization/gateway-api-pause                                              Reconciliation finished in 90.096917ms, next run in 10m0s
mesac2@CPC-mesac-4BM7S:~$ kubectl -n livelink-hivemq get sts,pods -o wide
NAME                               READY   AGE   CONTAINERS   IMAGES
statefulset.apps/livelink-hivemq   3/4     37d   hivemq       case.artifacts.medtronic.com/ext-docker-hub-remote/hivemq/hivemq4:k8s-4.42.0

NAME                    READY   STATUS     RESTARTS   AGE     IP             NODE
                    NOMINATED NODE   READINESS GATES
pod/livelink-hivemq-0   1/1     Running    0          2d17h   100.64.25.15   adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-9s8g7   <none>           <none>
pod/livelink-hivemq-1   1/1     Running    0          2d17h   100.64.25.16   adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-9s8g7   <none>           <none>
pod/livelink-hivemq-2   1/1     Running    0          2d17h   100.64.25.17   adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-9s8g7   <none>           <none>
pod/livelink-hivemq-3   0/1     Init:0/2   0          50m     <none>         adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6   <none>           <none>
mesac2@CPC-mesac-4BM7S:~$ kubectl -n livelink-hivemq get sts livelink-hivemq -o json |
jq '{
  replicas: .spec.replicas,
  readyReplicas: .status.readyReplicas,
  currentReplicas: .status.currentReplicas,
  podManagementPolicy: .spec.podManagementPolicy,
  updateStrategy: .spec.updateStrategy,
  affinity: .spec.template.spec.affinity,
  topologySpreadConstraints: .spec.template.spec.topologySpreadConstraints
}'
{
  "replicas": 4,
  "readyReplicas": 3,
  "currentReplicas": null,
  "podManagementPolicy": "OrderedReady",
  "updateStrategy": {
    "type": "OnDelete"
  },
  "affinity": {
    "podAntiAffinity": {
      "preferredDuringSchedulingIgnoredDuringExecution": [
        {
          "podAffinityTerm": {
            "labelSelector": {
              "matchLabels": {
                "app.kubernetes.io/name": "hivemq-platform"
              }
            },
            "topologyKey": "kubernetes.io/hostname"
          },
          "weight": 100
        }
      ]
    }
  },
  "topologySpreadConstraints": null
}
mesac2@CPC-mesac-4BM7S:~$ kubectl -n livelink-hivemq describe pod livelink-hivemq-3
Name:             livelink-hivemq-3
Namespace:        livelink-hivemq
Priority:         0
Service Account:  hivemq-platform-pod-livelink-hivemq
Node:             adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6/172.19.0.62
Start Time:       Thu, 15 Oct 2026 15:47:52 +0000
Labels:           app.kubernetes.io/instance=livelink-hivemq
                  app.kubernetes.io/managed-by=Helm
                  app.kubernetes.io/name=hivemq-platform
                  app.kubernetes.io/version=4.50.0
                  apps.kubernetes.io/pod-index=3
                  controller-revision-hash=livelink-hivemq-5b69dc5847
                  helm.sh/chart=hivemq-platform-0.2.65
                  helm.toolkit.fluxcd.io/name=livelink-hivemq
                  helm.toolkit.fluxcd.io/namespace=livelink-system
                  hivemq-platform=livelink-hivemq
                  statefulset.kubernetes.io/pod-name=livelink-hivemq-3
Annotations:      dynakube.dynatrace.com/injected: true
                  kubernetes-resource-versions: {broker-configuration-v2=-2089688360, env-var-secret-hivemq=28331699}
                  meta.helm.sh/release-name: livelink-hivemq
                  meta.helm.sh/release-namespace: livelink-hivemq
                  metadata-enrichment.dynatrace.com/injected: true
                  metadata.dynatrace.com: {}
                  metadata.dynatrace.com/k8s.workload.kind: statefulset
                  metadata.dynatrace.com/k8s.workload.name: livelink-hivemq
                  oneagent.dynatrace.com/injected: true
Status:           Pending
IP:
IPs:              <none>
Controlled By:    StatefulSet/livelink-hivemq
Init Containers:
  hivemq-platform-operator-init:
    Container ID:
    Image:           case.artifacts.medtronic.com/ext-docker-hub-remote/hivemq/hivemq-platform-operator-init:1.7.2
    Image ID:
    Port:            <none>
    Host Port:       <none>
    SeccompProfile:  RuntimeDefault
    State:           Waiting
      Reason:        PodInitializing
    Ready:           False
    Restart Count:   0
    Limits:
      cpu:     250m
      memory:  100Mi
    Requests:
      cpu:        250m
      memory:     100Mi
    Environment:  <none>
    Mounts:
      /hivemq from operator-init (rw)
      /var/run/secrets/kubernetes.io/serviceaccount from kube-api-access-9w8jq (ro)
  dynatrace-operator:
    Container ID:
    Image:           case.artifacts.medtronic.com/ext-docker-aws-remote/dynatrace/dynatrace-operator:v1.9.0
    Image ID:
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
      --attribute=k8s.workload.kind=statefulset
      --attribute=k8s.workload.name=livelink-hivemq
      --attribute=dt.kubernetes.workload.kind=statefulset
      --attribute=dt.kubernetes.workload.name=livelink-hivemq
      --metadata-enrichment
      --attribute-container={"container_image.registry":"case.artifacts.medtronic.com","container_image.repository":"ext-docker-hub-remote/hivemq/hivemq4","container_image.tags":"k8s-4.42.0","k8s.container.name":"hivemq"}
      --attribute=k8s.node.name=$(K8S_NODE_NAME)
      --attribute=k8s.namespace.name=livelink-hivemq
      --attribute=k8s.cluster.uid=728fa293-1692-4ed9-a786-b99e6b27650e
      --attribute=k8s.cluster.name=adan1-atlas-prd
      --attribute=dt.entity.kubernetes_cluster=KUBERNETES_CLUSTER-1EFC92E539F95C3C
      --attribute=k8s.pod.name=$(K8S_PODNAME)
      --attribute=k8s.pod.uid=$(K8S_PODUID)
      --attribute=dt.kubernetes.cluster.id=728fa293-1692-4ed9-a786-b99e6b27650e
    State:          Waiting
      Reason:       PodInitializing
    Ready:          False
    Restart Count:  0
    Limits:
      cpu:     100m
      memory:  60Mi
    Requests:
      cpu:     30m
      memory:  30Mi
    Environment:
      K8S_PODNAME:    livelink-hivemq-3 (v1:metadata.name)
      K8S_PODUID:      (v1:metadata.uid)
      K8S_NODE_NAME:   (v1:spec.nodeName)
    Mounts:
      /mnt/bin from oneagent-bin (ro)
      /mnt/config from dynatrace-config (rw)
      /mnt/input from dynatrace-input (ro)
      /var/run/secrets/kubernetes.io/serviceaccount from kube-api-access-9w8jq (ro)
Containers:
  hivemq:
    Container ID:
    Image:           case.artifacts.medtronic.com/ext-docker-hub-remote/hivemq/hivemq4:k8s-4.42.0
    Image ID:
    Ports:           1883/TCP, 8080/TCP, 9399/TCP
    Host Ports:      0/TCP, 0/TCP, 0/TCP
    SeccompProfile:  RuntimeDefault
    Command:
      /opt/hivemq/operator/bin/run.sh
    State:          Waiting
      Reason:       PodInitializing
    Ready:          False
    Restart Count:  0
    Limits:
      cpu:     4
      memory:  8Gi
    Requests:
      cpu:      500m
      memory:   2Gi
    Liveness:   http-get http://:7979/liveness delay=15s timeout=1s period=30s #success=1 #failure=240
    Readiness:  http-get http://:7979/readiness delay=3s timeout=1s period=5s #success=1 #failure=3
    Environment:
      JAVA_OPTS:                  -XX:+UnlockExperimentalVMOptions -XX:InitialRAMPercentage=50 -XX:MaxRAMPercentage=50
      HIVEMQ_DATA_FOLDER:         /opt/hivemq/pvc/data
      CONFLUENT_KAFKA_BOOTSTRAP:  <set to the key 'CONFLUENT_KAFKA_BOOTSTRAP' in secret 'hivemq'>  Optional: false
      KAFKA_USERNAME:             <set to the key 'KAFKA_USERNAME' in secret 'hivemq'>             Optional: false
      KAFKA_PASSWORD:             <set to the key 'KAFKA_PASSWORD' in secret 'hivemq'>             Optional: false
      HIVEMQ_BIND_ADDRESS:         (v1:status.podIP)
      DT_DEPLOYMENT_METADATA:     orchestration_tech=Operator-application_monitoring;script_version=v1.9.0;orchestrator_id=728fa293-1692-4ed9-a786-b99e6b27650e
      LD_PRELOAD:                 /opt/dynatrace/oneagent-paas/agent/lib64/liboneagentproc.so
      DT_STORAGE:                 /var/lib/dynatrace/oneagent
    Mounts:
      /etc/ld.so.preload from dynatrace-config (rw,path="oneagent/ld.so.preload")
      /etc/podinfo/ from pod-info (rw)
      /opt/dynatrace/oneagent-paas from oneagent-bin (ro)
      /opt/hivemq/conf-k8s/ from broker-configuration (rw)
      /opt/hivemq/extensions/hivemq-kafka-extension/conf/ from extension-configuration-hivemq-kafka-extension (rw)
      /opt/hivemq/license from licenses (rw)
      /opt/hivemq/operator/ from operator-init (rw)
      /opt/hivemq/pvc from hivemq-data (rw)
      /var/lib/dynatrace from dynatrace-config (rw,path="hivemq")
      /var/run/secrets/kubernetes.io/serviceaccount from kube-api-access-9w8jq (ro)
Conditions:
  Type                        Status
  PodReadyToStartContainers   False
  Initialized                 False
  Ready                       False
  ContainersReady             False
  PodScheduled                True
Volumes:
  hivemq-data:
    Type:       PersistentVolumeClaim (a reference to a PersistentVolumeClaim in the same namespace)
    ClaimName:  hivemq-data-livelink-hivemq-3
    ReadOnly:   false
  licenses:
    Type:        Secret (a volume populated by a Secret)
    SecretName:  hivemq
    Optional:    false
  pod-info:
    Type:      ConfigMap (a volume populated by a ConfigMap)
    Name:      hivemq-platform-livelink-hivemq-dynamic-state
    Optional:  false
  broker-configuration:
    Type:      ConfigMap (a volume populated by a ConfigMap)
    Name:      hivemq-configuration-livelink-hivemq
    Optional:  false
  operator-init:
    Type:       EmptyDir (a temporary directory that shares a pod's lifetime)
    Medium:
    SizeLimit:  <unset>
  extension-configuration-hivemq-kafka-extension:
    Type:      ConfigMap (a volume populated by a ConfigMap)
    Name:      livelink-hivemq-kafka-ext-config
    Optional:  false
  kube-api-access-9w8jq:
    Type:                    Projected (a volume that contains injected data from multiple sources)
    TokenExpirationSeconds:  3607
    ConfigMapName:           kube-root-ca.crt
    ConfigMapOptional:       <nil>
    DownwardAPI:             true
  oneagent-bin:
    Type:              CSI (a Container Storage Interface (CSI) volume source)
    Driver:            csi.oneagent.dynatrace.com
    FSType:
    ReadOnly:          true
    VolumeAttributes:      dynakube=adan1-atlas-prd
                           mode=app
                           retryTimeout=10m0s
  dynatrace-input:
    Type:                Projected (a volume that contains injected data from multiple sources)
    SecretName:          dynatrace-bootstrapper-config
    SecretOptionalName:  0xc000b9e985
    SecretName:          dynatrace-bootstrapper-certs
    SecretOptionalName:  0xc000b9e986
  dynatrace-config:
    Type:        EmptyDir (a temporary directory that shares a pod's lifetime)
    Medium:
    SizeLimit:   <unset>
QoS Class:       Burstable
Node-Selectors:  <none>
Tolerations:     node.kubernetes.io/not-ready:NoExecute op=Exists for 300s
                 node.kubernetes.io/unreachable:NoExecute op=Exists for 300s
Events:
  Type     Reason                  Age                   From                     Message
  ----     ------                  ----                  ----                     -------
  Normal   Scheduled               50m                   default-scheduler        Successfully assigned livelink-hivemq/livelink-hivemq-3 to adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6
  Warning  FailedAttachVolume      48m                   attachdetach-controller  AttachVolume.Attach failed for volume "pvc-d1a2fffa-df71-481a-9c4b-618a9aff1480" : timed out waiting for external-attacher of csi.vsphere.vmware.com CSI driver to attach volume 62154bef-a910-4745-8fc3-5da84babc2a8-d1a2fffa-df71-481a-9c4b-618a9aff1480
  Normal   SuccessfulAttachVolume  47m                   attachdetach-controller  AttachVolume.Attach succeeded for volume "pvc-d1a2fffa-df71-481a-9c4b-618a9aff1480"
  Warning  FailedMount             16m                   kubelet                  MountVolume.SetUp failed for volume "broker-configuration" : failed to sync configmap cache: timed out waiting for the condition
  Warning  FailedMount             16m                   kubelet                  MountVolume.SetUp failed for volume "extension-configuration-hivemq-kafka-extension" : failed to sync configmap cache: timed out waiting for the condition
  Warning  FailedMount             16m                   kubelet                  MountVolume.SetUp failed for volume "pod-info" : failed to sync configmap cache: timed out waiting for the condition
  Warning  FailedMount             16m                   kubelet                  MountVolume.SetUp failed for volume "licenses" : failed to sync secret cache: timed out waiting for the condition
  Warning  FailedMount             16m                   kubelet                  MountVolume.SetUp failed for volume "dynatrace-input" : failed to sync secret cache: timed out waiting for the condition
  Warning  FailedMount             16m                   kubelet                  MountVolume.SetUp failed for volume "kube-api-access-9w8jq" : failed to sync configmap cache: timed out waiting for the condition
  Warning  FailedMount             2m20s (x30 over 48m)  kubelet                  MountVolume.SetUp failed for volume "oneagent-bin" : kubernetes.io/csi: mounter.SetUpAt failed to determine if the node service has VOLUME_MOUNT_GROUP capability: rpc error: code = Unavailable desc = connection error: desc = "transport: Error while dialing: dial unix /var/lib/kubelet/plugins/csi.oneagent.dynatrace.com/csi.sock: connect: no such file or directory"
mesac2@CPC-mesac-4BM7S:~$