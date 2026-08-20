 kubectl get pods -A --field-selector=status.phase=Pending
NAMESPACE              NAME                                READY   STATUS    RESTARTS   AGE
cis-genai-poc-system   envoy-backend-gateway-2gw44         0/2     Pending   0          2d13h
cis-genai-poc-system   envoy-frontend-gateway-g5422        0/2     Pending   0          2d13h
cis-genai-poc-system   envoy-grafana-gateway-cm29t         0/2     Pending   0          2d13h
cis-genai-poc-system   envoy-itops-backend-gateway-j4c2m   0/2     Pending   0          2d13h
cis-genai-poc-system   envoy-keycloak-gateway-cqzn2        0/2     Pending   0          2d13h
cis-genai-poc-system   envoy-ticket-audit-gateway-fh8v6    0/2     Pending   0          2d13h
cis-genai-poc-system   itops-nginx-86c87cf945-btgzn        0/1     Pending   0          2d13h
livelink-system        envoy-livelink-gateway-5jwq5        0/2     Pending   0          3d2h
mesac2@CPC-mesac-4BM7S:~$ kubectl describe pod envoy-backend-gateway-2gw44 -n cis-genai-poc-system
Name:             envoy-backend-gateway-2gw44
Namespace:        cis-genai-poc-system
Priority:         0
Service Account:  envoy-backend-gateway
Node:             <none>
Labels:           app=envoy-backend-gateway
                  app.kubernetes.io/component=ingress-controller
                  app.kubernetes.io/instance=backend-gateway
                  app.kubernetes.io/managed-by=contour-gateway-provisioner
                  app.kubernetes.io/name=contour
                  controller-revision-hash=56979fb797
                  gateway.networking.k8s.io/gateway-name=backend-gateway
                  pod-template-generation=2
                  projectcontour.io/owning-gateway-name=backend-gateway
Annotations:      dynakube.dynatrace.com/injected: true
                  metadata-enrichment.dynatrace.com/injected: true
                  metadata.dynatrace.com: {}
                  metadata.dynatrace.com/k8s.workload.kind: daemonset
                  metadata.dynatrace.com/k8s.workload.name: envoy-backend-gateway
                  oneagent.dynatrace.com/injected: true
Status:           Pending
IP:
IPs:              <none>
Controlled By:    DaemonSet/envoy-backend-gateway
Init Containers:
  envoy-initconfig:
    Image:      case.artifacts.medtronic.com/ioss-docker-common-components-local/all-clusters/bitnami/contour:1.30.2-debian-12-r1
    Port:       <none>
    Host Port:  <none>
    Command:
      contour
    Args:
      bootstrap
      /config/envoy.json
      --xds-address=contour-backend-gateway
      --xds-port=8001
      --xds-resource-version=v3
      --resources-dir=/config/resources
      --envoy-cafile=/certs/ca.crt
      --envoy-cert-file=/certs/tls.crt
      --envoy-key-file=/certs/tls.key
      --overload-max-heap=0
    Limits:
      cpu:     50m
      memory:  100Mi
    Requests:
      cpu:     25m
      memory:  50Mi
    Environment:
      CONTOUR_NAMESPACE:  cis-genai-poc-system (v1:metadata.namespace)
    Mounts:
      /certs from envoycert (ro)
      /config from envoy-config (rw)
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
      --attribute=k8s.workload.kind=daemonset
      --attribute=k8s.workload.name=envoy-backend-gateway
      --attribute=dt.kubernetes.workload.kind=daemonset
      --attribute=dt.kubernetes.workload.name=envoy-backend-gateway
      --metadata-enrichment
      --attribute-container={"container_image.registry":"case.artifacts.medtronic.com","container_image.repository":"ioss-docker-common-components-local/all-clusters/bitnami/contour","container_image.tags":"1.30.2-debian-12-r1","k8s.container.name":"shutdown-manager"}
      --attribute-container={"container_image.registry":"case.artifacts.medtronic.com","container_image.repository":"ioss-docker-common-components-local/all-clusters/bitnami/envoy","container_image.tags":"1.31.5-debian-12-r0","k8s.container.name":"envoy"}
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
      K8S_PODNAME:    envoy-backend-gateway-2gw44 (v1:metadata.name)
      K8S_PODUID:      (v1:metadata.uid)
      K8S_NODE_NAME:   (v1:spec.nodeName)
    Mounts:
      /mnt/bin from oneagent-bin (ro)
      /mnt/config from dynatrace-config (rw)
      /mnt/input from dynatrace-input (ro)
Containers:
  shutdown-manager:
    Image:      case.artifacts.medtronic.com/ioss-docker-common-components-local/all-clusters/bitnami/contour:1.30.2-debian-12-r1
    Port:       <none>
    Host Port:  <none>
    Command:
      /bin/contour
    Args:
      envoy
      shutdown-manager
    Limits:
      cpu:     50m
      memory:  100Mi
    Requests:
      cpu:     25m
      memory:  50Mi
    Environment:
      DT_DEPLOYMENT_METADATA:  orchestration_tech=Operator-application_monitoring;script_version=v1.9.0;orchestrator_id=2d798f2e-6740-4435-8747-8fc15de6aaa6
      LD_PRELOAD:              /opt/dynatrace/oneagent-paas/agent/lib64/liboneagentproc.so
      DT_STORAGE:              /var/lib/dynatrace/oneagent
    Mounts:
      /admin from envoy-admin (rw)
      /etc/ld.so.preload from dynatrace-config (rw,path="oneagent/ld.so.preload")
      /opt/dynatrace/oneagent-paas from oneagent-bin (ro)
      /var/lib/dynatrace from dynatrace-config (rw,path="shutdown-manager")
  envoy:
    Image:      case.artifacts.medtronic.com/ioss-docker-common-components-local/all-clusters/bitnami/envoy:1.31.5-debian-12-r0
    Port:       8002/TCP
    Host Port:  0/TCP
    Command:
      envoy
    Args:
      -c
      /config/envoy.json
      --service-cluster $(CONTOUR_NAMESPACE)
      --service-node $(ENVOY_POD_NAME)
      --log-level info
      --base-id 0
    Limits:
      memory:  256Mi
    Requests:
      cpu:      25m
      memory:   50Mi
    Readiness:  http-get http://:8002/ready delay=3s timeout=1s period=4s #success=1 #failure=3
    Environment:
      CONTOUR_NAMESPACE:       cis-genai-poc-system (v1:metadata.namespace)
      ENVOY_POD_NAME:          envoy-backend-gateway-2gw44 (v1:metadata.name)
      DT_DEPLOYMENT_METADATA:  orchestration_tech=Operator-application_monitoring;script_version=v1.9.0;orchestrator_id=2d798f2e-6740-4435-8747-8fc15de6aaa6
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
  Type           Status
  PodScheduled   False
Volumes:
  envoycert:
    Type:        Secret (a volume populated by a Secret)
    SecretName:  envoycert-backend-gateway
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
    VolumeAttributes:      dynakube=awby1-atlas-prd
                           mode=app
                           retryTimeout=10m0s
  dynatrace-input:
    Type:                Projected (a volume that contains injected data from multiple sources)
    SecretName:          dynatrace-bootstrapper-config
    SecretOptionalName:  0xc00090d355
    SecretName:          dynatrace-bootstrapper-certs
    SecretOptionalName:  0xc00090d356
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
Events:
  Type     Reason            Age                   From               Message
  ----     ------            ----                  ----               -------
  Warning  FailedScheduling  12m (x63 over 4h56m)  default-scheduler  0/8 nodes are available: 1 Insufficient memory, 7 node(s) didn't satisfy plugin(s) [NodeAffinity]. preemption: 0/8 nodes are available: 1 No preemption victims found for incoming pod, 7 Preemption is not helpful for scheduling.
mesac2@CPC-mesac-4BM7S:~$ kubectl top nodes
NAME                                                 CPU(cores)   CPU%   MEMORY(bytes)   MEMORY%
awby1-atlas-prd-k8wzk-b4rlv                          409m         10%    2986Mi          10%
awby1-atlas-prd-k8wzk-vnqfl                          453m         11%    3902Mi          13%
awby1-atlas-prd-k8wzk-wdptg                          419m         10%    3544Mi          12%
awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-5pltm   325m         4%     8045Mi          28%
awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-5vbwl   357m         4%     12085Mi         42%
awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-bknvv   153m         1%     4243Mi          14%
awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-jqnvr   299m         3%     9356Mi          32%
awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-xwdnw   309m         3%     6422Mi          22%
mesac2@CPC-mesac-4BM7S:~$ kubectl top pods -A --sort-by=cpu
NAMESPACE                      NAME                                                                 CPU(cores)   MEMORY(bytes)
kube-system                    kube-apiserver-awby1-atlas-prd-k8wzk-wdptg                           153m         1718Mi
livelink-db                    livelink-db-site-custom-cluster-3                                    152m         115Mi
kube-system                    kube-apiserver-awby1-atlas-prd-k8wzk-vnqfl                           141m         2009Mi
kube-system                    kube-apiserver-awby1-atlas-prd-k8wzk-b4rlv                           128m         1461Mi
kube-system                    etcd-awby1-atlas-prd-k8wzk-wdptg                                     118m         126Mi
kube-system                    etcd-awby1-atlas-prd-k8wzk-vnqfl                                     116m         150Mi
kube-system                    etcd-awby1-atlas-prd-k8wzk-b4rlv                                     100m         137Mi
kube-system                    kube-controller-manager-awby1-atlas-prd-k8wzk-vnqfl                  46m          188Mi
dynatrace-operator             awby1-atlas-prd-activegate-0                                         38m          1503Mi
kube-system                    antrea-agent-5v2xh                                                   36m          216Mi
tkg-system                     kapp-controller-75f4dd97fd-fxgrt                                     32m          140Mi
kube-system                    antrea-agent-prgjm                                                   29m          214Mi
kube-system                    antrea-agent-7fc2r                                                   29m          173Mi
cis-genai-poc-system           contour-itops-backend-gateway-747c799f4b-ghkrx                       28m          247Mi
kube-system                    antrea-agent-bqxs6                                                   28m          176Mi
kube-system                    antrea-agent-wvt94                                                   24m          216Mi
livelink-hivemq                livelink-hivemq-0                                                    23m          1290Mi
kube-system                    antrea-agent-fshws                                                   22m          152Mi
kube-system                    antrea-agent-tfcff                                                   19m          151Mi
kyverno-system                 kyverno-reports-controller-784d5f479b-xtwqw                          18m          79Mi
livelink-hivemq                livelink-hivemq-1                                                    18m          1269Mi
livelink-system                contour-livelink-gateway-7c67cf46fb-xngkn                            17m          126Mi
kube-system                    antrea-agent-nqbjr                                                   16m          134Mi
livelink-device                vorne-board-collector-75c6898f86-r8lb5                               13m          348Mi
livelink-db                    livelink-db-01-cluster-5                                             12m          245Mi
cis-genai-poc-system           contour-backend-gateway-86bb6d5c84-pd5xg                             10m          222Mi
kube-system                    coredns-6c67484c8c-jzbj4                                             10m          21Mi
livelink-db                    livelink-db-platform-cluster-3                                       9m           190Mi
cis-genai-poc-system           contour-ticket-audit-gateway-77457fb57b-nw2r4                        9m           67Mi
cis-genai-poc-system           contour-frontend-gateway-64f845f99d-mv6j2                            9m           134Mi
cis-genai-poc-system           contour-keycloak-gateway-5db68cc469-p48k7                            9m           241Mi
cis-genai-poc-system           contour-grafana-gateway-6798cb5f64-czfgx                             9m           79Mi
kyverno-system                 kyverno-admission-controller-54bc7f8f-wxqj5                          8m           97Mi
livelink-db                    livelink-db-platform-cluster-1                                       8m           259Mi
dynatrace-operator             dynatrace-fluent-bit-vgghh                                           8m           14Mi
contour-system                 contour-contour-59fbcccf77-jz4nw                                     8m           95Mi
kube-system                    coredns-6c67484c8c-gp78m                                             8m           21Mi
cis-genai-poc-incai-db         cis-genai-poc-incai-db-cis-genai-poc-incai-cluster-2                 8m           198Mi
cis-genai-poc-db               cis-genai-poc-db-cis-genai-poc-cluster-3                             8m           208Mi
cis-genai-poc-db               cis-genai-poc-db-cis-genai-poc-cluster-4                             7m           199Mi
kyverno-system                 kyverno-admission-controller-54bc7f8f-4txhn                          7m           171Mi
livelink-db                    livelink-db-site-custom-cluster-1                                    7m           157Mi
livelink-db                    livelink-db-site-custom-cluster-2                                    7m           147Mi
cis-genai-poc-incai-db         cis-genai-poc-incai-db-cis-genai-poc-incai-cluster-1                 7m           215Mi
cis-genai-poc-system           ticket-audit-grafana-6998f4c7fc-rmrfb                                7m           134Mi
livelink-device                deployment-aapl-5f6cc64fdf-rp9sf                                     7m           50Mi
cis-genai-poc-incai-db         cis-genai-poc-incai-db-cis-genai-poc-incai-cluster-3                 6m           190Mi
cis-genai-poc-db               cis-genai-poc-db-cis-genai-poc-cluster-1                             6m           2275Mi
livelink-db                    livelink-db-platform-cluster-2                                       6m           247Mi
livelink-db                    livelink-db-01-cluster-7                                             6m           229Mi
atlas-system                   kubescape-node-agent-mcxhg                                           6m           245Mi
atlas-system                   kubescape-node-agent-l5qsp                                           6m           242Mi
cis-genai-poc-system           envoy-frontend-gateway-z5ftm                                         5m           26Mi
cis-genai-poc-system           envoy-keycloak-gateway-zfzk7                                         5m           27Mi
cis-genai-poc-system           envoy-ticket-audit-gateway-7m6pv                                     5m           30Mi
kube-system                    kube-scheduler-awby1-atlas-prd-k8wzk-vnqfl                           5m           45Mi
cis-genai-poc-system           envoy-ticket-audit-gateway-vn959                                     5m           27Mi
cis-genai-poc-system           envoy-ticket-audit-gateway-vnlpm                                     5m           27Mi
cis-genai-poc-system           envoy-keycloak-gateway-vlsgs                                         5m           27Mi
cis-genai-poc-system           envoy-keycloak-gateway-th68m                                         5m           30Mi
livelink-device                dis-567f6c97db-b66v2                                                 5m           397Mi
livelink-device                deployment-drs-64c588b775-85qr7                                      5m           30Mi
cis-genai-poc-system           keycloak-itops-keycloak-6ddc99699-s2rs7                              5m           920Mi
kube-system                    kube-scheduler-awby1-atlas-prd-k8wzk-b4rlv                           5m           44Mi
kube-system                    kube-scheduler-awby1-atlas-prd-k8wzk-wdptg                           5m           48Mi
livelink-db                    livelink-db-timescaledb-cluster-1                                    5m           267Mi
cis-genai-poc-system           envoy-itops-backend-gateway-w764r                                    5m           27Mi
cis-genai-poc-system           envoy-itops-backend-gateway-bvv7j                                    5m           26Mi
cis-genai-poc-system           envoy-grafana-gateway-vgpp5                                          5m           27Mi
cis-genai-poc-system           envoy-grafana-gateway-t8mqq                                          5m           27Mi
cis-genai-poc-system           envoy-grafana-gateway-p5zds                                          5m           28Mi
cis-genai-poc-system           envoy-frontend-gateway-cb488                                         5m           27Mi
cis-genai-poc-system           envoy-backend-gateway-bjhww                                          5m           23Mi
contour-system                 contour-envoy-258bn                                                  5m           26Mi
contour-system                 contour-envoy-8pddf                                                  5m           26Mi
contour-system                 contour-envoy-dwhcl                                                  5m           28Mi
contour-system                 contour-envoy-kchwb                                                  5m           28Mi
contour-system                 contour-envoy-wbbsh                                                  5m           28Mi
cis-genai-poc-system           envoy-backend-gateway-7zp72                                          5m           24Mi
cis-genai-poc-system           contour-ticket-audit-gateway-77457fb57b-hvk6q                        5m           27Mi
livelink-system                envoy-livelink-gateway-276sl                                         5m           44Mi
livelink-system                envoy-livelink-gateway-jfxhr                                         5m           44Mi
livelink-system                envoy-livelink-gateway-sqzwv                                         5m           49Mi
dynatrace-operator             dynatrace-fluent-bit-hk585                                           5m           59Mi
vmware-system-csi              vsphere-csi-controller-777fcf9598-k26hn                              5m           127Mi
cert-manager                   cert-manager-7c4866444b-27dgj                                        5m           37Mi
cis-genai-poc-system           contour-grafana-gateway-6798cb5f64-tnt2m                             4m           34Mi
dynatrace-operator             dynatrace-fluent-bit-rfbfx                                           4m           15Mi
dynatrace-operator             dynatrace-fluent-bit-pg8j6                                           4m           10Mi
dynatrace-operator             dynatrace-fluent-bit-nwdz2                                           4m           32Mi
livelink-db                    livelink-db-01-cluster-6                                             4m           290Mi
kube-system                    metrics-server-64d67ddb5-pvbmh                                       4m           48Mi
livelink-db                    livelink-db-timescaledb-cluster-2                                    4m           209Mi
cis-genai-poc-system           envoy-ticket-audit-gateway-g4pck                                     4m           30Mi
cis-genai-poc-system           envoy-keycloak-gateway-hrk58                                         4m           31Mi
cis-genai-poc-system           envoy-itops-backend-gateway-5cljl                                    4m           27Mi
cis-genai-poc-system           envoy-itops-backend-gateway-4tjxx                                    4m           27Mi
cis-genai-poc-system           envoy-grafana-gateway-gc749                                          4m           28Mi
cis-genai-poc-system           envoy-frontend-gateway-jvnwr                                         4m           27Mi
cis-genai-poc-system           envoy-frontend-gateway-5k9n2                                         4m           26Mi
cis-genai-poc-system           envoy-backend-gateway-l4gj2                                          4m           24Mi
cis-genai-poc-system           envoy-backend-gateway-hftgt                                          4m           24Mi
livelink-system                contour-livelink-gateway-7c67cf46fb-4tcwn                            4m           28Mi
cis-genai-poc-system           contour-frontend-gateway-64f845f99d-f59th                            4m           35Mi
kube-system                    antrea-controller-6bf7db5657-n47g9                                   4m           82Mi
livelink-system                envoy-livelink-gateway-nmvvw                                         4m           50Mi
vmware-system-csi              vsphere-csi-node-ncbtw                                               4m           24Mi
vmware-system-csi              vsphere-csi-node-vbr8f                                               4m           43Mi
vmware-system-csi              vsphere-csi-node-wdnsf                                               4m           23Mi
vmware-system-csi              vsphere-csi-node-wvnpv                                               4m           23Mi
vmware-system-csi              vsphere-csi-node-xcz72                                               4m           27Mi
atlas-system                   kubescape-node-agent-4h2pc                                           4m           226Mi
kyverno-system                 kyverno-admission-controller-54bc7f8f-qbdvc                          3m           159Mi
external-secrets               external-secrets-5f59dd8689-8zll7                                    3m           46Mi
atlas-system                   kubescape-node-agent-npxkw                                           3m           229Mi
atlas-system                   kubescape-node-agent-nsq6j                                           3m           220Mi
atlas-system                   kubescape-storage-6f6df5bc6-h4zg9                                    3m           99Mi
vmware-system-csi              vsphere-csi-node-ndsqn                                               3m           34Mi
vmware-system-csi              vsphere-csi-node-mbv5r                                               3m           37Mi
cis-genai-poc-system           contour-backend-gateway-86bb6d5c84-s5l2v                             3m           27Mi
cis-genai-poc-system           contour-itops-backend-gateway-747c799f4b-pw5wn                       3m           36Mi
cis-genai-poc-system           contour-keycloak-gateway-5db68cc469-2drfw                            3m           36Mi
cis-genai-poc-system           itops-platform-itops-rabbitmq-c54c8c785-cgml2                        3m           138Mi
livelink-db                    livelink-db-timescaledb-cluster-3                                    3m           206Mi
cis-genai-poc-system           ticket-audit-frontend-bc94cd86d-cm8jj                                3m           199Mi
confluent-system               confluent-operator-6b8c5f4d44-ff92m                                  3m           28Mi
dynatrace-operator             dynatrace-fluent-bit-7rgrd                                           3m           11Mi
dynatrace-operator             dynatrace-fluent-bit-sxp22                                           3m           7Mi
kyverno-system                 kyverno-cleanup-controller-566f85478f-z6m9r                          2m           45Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-hbwfg                                  2m           55Mi
vmware-system-csi              vsphere-csi-node-69hlt                                               2m           23Mi
vmware-system-cloud-provider   guest-cluster-cloud-provider-67f7d944f4-45l96                        2m           22Mi
dynatrace-operator             dynatrace-operator-66cf9b4d9b-bq9p5                                  2m           25Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-twdrh                                  2m           55Mi
kube-system                    kube-controller-manager-awby1-atlas-prd-k8wzk-b4rlv                  2m           19Mi
cis-genai-poc-system           itops-platform-itops-backend-69f49fb745-6vs5d                        2m           751Mi
cis-genai-poc-system           itops-platform-itops-backend-69f49fb745-fmv5h                        2m           754Mi
kube-system                    kube-proxy-2fzhj                                                     2m           19Mi
cnpg-system                    cloudnative-pg-59b4568767-l6k26                                      2m           82Mi
external-secrets               external-secrets-5f59dd8689-gkfvc                                    2m           48Mi
dynatrace-operator             dynatrace-fluent-bit-7mhps                                           2m           10Mi
kyverno-system                 kyverno-reports-controller-784d5f479b-nwx7j                          2m           34Mi
hivemq-operator                hivemq-hivemq-operator-7b4f957f5d-lb64x                              2m           1011Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-89z8m                                  2m           54Mi
confluent-system               confluent-operator-6b8c5f4d44-2ml28                                  1m           23Mi
kube-system                    kube-controller-manager-awby1-atlas-prd-k8wzk-wdptg                  1m           18Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-mtz4w                                  1m           55Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-mm54r                                  1m           56Mi
kube-system                    kube-proxy-vkpzj                                                     1m           23Mi
kube-system                    snapshot-controller-5fc8d85457-dfnsf                                 1m           8Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-fd7hw                                  1m           54Mi
atlas-system                   external-dns-758f679b65-bzxcn                                        1m           21Mi
kube-system                    kube-proxy-tsn8k                                                     1m           19Mi
kyverno-system                 kyverno-background-controller-f5c7b88c4-g4dnh                        1m           30Mi
kyverno-system                 kyverno-background-controller-f5c7b88c4-q7rzp                        1m           46Mi
kyverno-system                 kyverno-cleanup-controller-566f85478f-j4rww                          1m           19Mi
kube-system                    kube-proxy-g6lmc                                                     1m           24Mi
kube-system                    kube-proxy-c7wq8                                                     1m           23Mi
kube-system                    docker-registry-awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-xwdnw   1m           7Mi
kube-system                    docker-registry-awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-jqnvr   1m           10Mi
kube-system                    kube-proxy-bdsf8                                                     1m           18Mi
dynatrace-operator             awby1-atlas-prd-otel-collector-0                                     1m           42Mi
contour-system                 contour-gateway-provisioner-69dcd796d4-5p45s                         1m           98Mi
confluent-system               confluent-operator-6b8c5f4d44-fp9hn                                  1m           23Mi
kube-system                    docker-registry-awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-bknvv   1m           7Mi
kube-system                    kube-proxy-8qls9                                                     1m           18Mi
cnpg-system                    cloudnative-pg-59b4568767-wnqr7                                      1m           36Mi
kube-system                    kube-proxy-76rqs                                                     1m           19Mi
kube-system                    docker-registry-awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-5vbwl   1m           8Mi
cis-genai-poc-system           ticket-audit-backend-66ff54584d-f4qtl                                1m           0Mi
kube-system                    docker-registry-awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-5pltm   1m           7Mi
cis-genai-poc-system           ticket-audit-backend-66ff54584d-2n2nt                                1m           0Mi
kube-system                    docker-registry-awby1-atlas-prd-k8wzk-wdptg                          1m           29Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-p62jp                                  1m           57Mi
flux-system                    source-controller-855bcc59bb-tfgq9                                   1m           159Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-t28qm                                  1m           54Mi
cis-genai-poc-system           itops-frontend-84d8dcb9d8-djwqp                                      1m           72Mi
kube-system                    docker-registry-awby1-atlas-prd-k8wzk-vnqfl                          1m           12Mi
kube-system                    docker-registry-awby1-atlas-prd-k8wzk-b4rlv                          1m           10Mi
external-secrets               external-secrets-cert-controller-8867d79b-jsk22                      1m           86Mi
cert-manager                   cert-manager-webhook-7786f5c8dd-grwtj                                1m           14Mi
cert-manager                   cert-manager-webhook-7786f5c8dd-gk9dc                                1m           13Mi
cert-manager                   cert-manager-webhook-7786f5c8dd-d799v                                1m           13Mi
cert-manager                   cert-manager-cainjector-7579b5ff77-zlklk                             1m           66Mi
secretgen-controller           secretgen-controller-68c7b987bb-2v5gj                                1m           37Mi
cert-manager                   cert-manager-cainjector-7579b5ff77-m7w8n                             1m           63Mi
traefik-gateway                traefik-gateway-5c4c4f76fb-7lp4j                                     1m           30Mi
traefik-gateway                traefik-gateway-5c4c4f76fb-w9wnw                                     1m           23Mi
vmware-system-auth             guest-cluster-auth-svc-jcds6                                         1m           10Mi
vmware-system-auth             guest-cluster-auth-svc-kzpnt                                         1m           9Mi
vmware-system-auth             guest-cluster-auth-svc-tbh6w                                         1m           9Mi
dynatrace-operator             dynatrace-webhook-967dbdf99-mbw2v                                    1m           18Mi
cert-manager                   cert-manager-7c4866444b-z5gn5                                        1m           11Mi
dynatrace-operator             dynatrace-webhook-967dbdf99-nr96x                                    1m           17Mi
external-secrets               external-secrets-webhook-8687f4f5fc-224hm                            1m           25Mi
atlas-system                   rbac-manager-fbb78f4b4-dcdrk                                         1m           16Mi
flux-system                    helm-controller-757b969798-8lpwf                                     1m           82Mi
flux-system                    kustomize-controller-7c578fd87c-cp9rc                                1m           79Mi
atlas-system                   kubescape-operator-84c49bfbf8-9m4xg                                  1m           16Mi
flux-system                    notification-controller-77f79b6bbb-lf4mr                             1m           21Mi
livelink-device                dmp-6799d6b9f9-kwr4r                                                 0m           10Mi
mesac2@CPC-mesac-4BM7S:~$ kubectl top pods -A --sort-by=memory
NAMESPACE                      NAME                                                                 CPU(cores)   MEMORY(bytes)
cis-genai-poc-db               cis-genai-poc-db-cis-genai-poc-cluster-1                             7m           2274Mi
kube-system                    kube-apiserver-awby1-atlas-prd-k8wzk-vnqfl                           158m         2009Mi
kube-system                    kube-apiserver-awby1-atlas-prd-k8wzk-wdptg                           166m         1718Mi
dynatrace-operator             awby1-atlas-prd-activegate-0                                         64m          1502Mi
kube-system                    kube-apiserver-awby1-atlas-prd-k8wzk-b4rlv                           197m         1492Mi
livelink-hivemq                livelink-hivemq-0                                                    19m          1290Mi
livelink-hivemq                livelink-hivemq-1                                                    21m          1269Mi
hivemq-operator                hivemq-hivemq-operator-7b4f957f5d-lb64x                              2m           1011Mi
cis-genai-poc-system           keycloak-itops-keycloak-6ddc99699-s2rs7                              5m           920Mi
cis-genai-poc-system           itops-platform-itops-backend-69f49fb745-fmv5h                        2m           754Mi
cis-genai-poc-system           itops-platform-itops-backend-69f49fb745-6vs5d                        2m           751Mi
livelink-device                dis-567f6c97db-b66v2                                                 4m           397Mi
livelink-device                vorne-board-collector-75c6898f86-r8lb5                               13m          348Mi
livelink-db                    livelink-db-01-cluster-6                                             6m           290Mi
livelink-db                    livelink-db-timescaledb-cluster-1                                    15m          267Mi
livelink-db                    livelink-db-platform-cluster-1                                       22m          259Mi
cis-genai-poc-system           contour-itops-backend-gateway-747c799f4b-ghkrx                       8m           247Mi
livelink-db                    livelink-db-platform-cluster-2                                       6m           247Mi
livelink-db                    livelink-db-01-cluster-5                                             18m          245Mi
atlas-system                   kubescape-node-agent-mcxhg                                           6m           245Mi
atlas-system                   kubescape-node-agent-l5qsp                                           8m           242Mi
cis-genai-poc-system           contour-keycloak-gateway-5db68cc469-p48k7                            23m          242Mi
atlas-system                   kubescape-node-agent-npxkw                                           3m           229Mi
livelink-db                    livelink-db-01-cluster-7                                             6m           229Mi
atlas-system                   kubescape-node-agent-4h2pc                                           6m           226Mi
cis-genai-poc-system           contour-backend-gateway-86bb6d5c84-pd5xg                             9m           221Mi
atlas-system                   kubescape-node-agent-nsq6j                                           5m           220Mi
kube-system                    antrea-agent-5v2xh                                                   29m          216Mi
kube-system                    antrea-agent-wvt94                                                   26m          216Mi
cis-genai-poc-incai-db         cis-genai-poc-incai-db-cis-genai-poc-incai-cluster-1                 6m           215Mi
kube-system                    antrea-agent-prgjm                                                   33m          214Mi
livelink-db                    livelink-db-timescaledb-cluster-2                                    3m           209Mi
cis-genai-poc-db               cis-genai-poc-db-cis-genai-poc-cluster-3                             8m           208Mi
livelink-db                    livelink-db-timescaledb-cluster-3                                    2m           206Mi
cis-genai-poc-system           ticket-audit-frontend-bc94cd86d-cm8jj                                3m           199Mi
cis-genai-poc-db               cis-genai-poc-db-cis-genai-poc-cluster-4                             7m           199Mi
cis-genai-poc-incai-db         cis-genai-poc-incai-db-cis-genai-poc-incai-cluster-2                 14m          198Mi
livelink-db                    livelink-db-platform-cluster-3                                       6m           190Mi
cis-genai-poc-incai-db         cis-genai-poc-incai-db-cis-genai-poc-incai-cluster-3                 6m           190Mi
kube-system                    kube-controller-manager-awby1-atlas-prd-k8wzk-vnqfl                  44m          188Mi
kube-system                    antrea-agent-bqxs6                                                   29m          176Mi
kube-system                    antrea-agent-7fc2r                                                   31m          173Mi
kyverno-system                 kyverno-admission-controller-54bc7f8f-4txhn                          3m           171Mi
flux-system                    source-controller-855bcc59bb-tfgq9                                   1m           159Mi
kyverno-system                 kyverno-admission-controller-54bc7f8f-qbdvc                          3m           159Mi
livelink-db                    livelink-db-site-custom-cluster-1                                    19m          157Mi
kube-system                    antrea-agent-fshws                                                   17m          152Mi
kube-system                    etcd-awby1-atlas-prd-k8wzk-vnqfl                                     118m         152Mi
kube-system                    antrea-agent-tfcff                                                   20m          151Mi
livelink-db                    livelink-db-site-custom-cluster-2                                    6m           147Mi
tkg-system                     kapp-controller-75f4dd97fd-fxgrt                                     3m           140Mi
kube-system                    etcd-awby1-atlas-prd-k8wzk-b4rlv                                     105m         138Mi
cis-genai-poc-system           itops-platform-itops-rabbitmq-c54c8c785-cgml2                        3m           138Mi
cis-genai-poc-system           ticket-audit-grafana-6998f4c7fc-rmrfb                                7m           135Mi
kube-system                    antrea-agent-nqbjr                                                   14m          134Mi
cis-genai-poc-system           contour-frontend-gateway-64f845f99d-mv6j2                            10m          134Mi
kube-system                    etcd-awby1-atlas-prd-k8wzk-wdptg                                     121m         127Mi
vmware-system-csi              vsphere-csi-controller-777fcf9598-k26hn                              3m           127Mi
livelink-system                contour-livelink-gateway-7c67cf46fb-xngkn                            9m           126Mi
livelink-db                    livelink-db-site-custom-cluster-3                                    175m         115Mi
contour-system                 contour-gateway-provisioner-69dcd796d4-5p45s                         2m           98Mi
atlas-system                   kubescape-storage-6f6df5bc6-h4zg9                                    3m           97Mi
kyverno-system                 kyverno-admission-controller-54bc7f8f-wxqj5                          10m          97Mi
contour-system                 contour-contour-59fbcccf77-jz4nw                                     8m           95Mi
flux-system                    kustomize-controller-7c578fd87c-cp9rc                                12m          92Mi
external-secrets               external-secrets-cert-controller-8867d79b-jsk22                      1m           86Mi
cnpg-system                    cloudnative-pg-59b4568767-l6k26                                      2m           82Mi
kube-system                    antrea-controller-6bf7db5657-n47g9                                   7m           82Mi
flux-system                    helm-controller-757b969798-8lpwf                                     1m           82Mi
cis-genai-poc-system           contour-grafana-gateway-6798cb5f64-czfgx                             9m           79Mi
kyverno-system                 kyverno-reports-controller-784d5f479b-xtwqw                          15m          78Mi
cis-genai-poc-system           itops-frontend-84d8dcb9d8-djwqp                                      1m           72Mi
cis-genai-poc-system           contour-ticket-audit-gateway-77457fb57b-nw2r4                        8m           67Mi
cert-manager                   cert-manager-cainjector-7579b5ff77-zlklk                             1m           66Mi
cert-manager                   cert-manager-cainjector-7579b5ff77-m7w8n                             1m           63Mi
dynatrace-operator             dynatrace-fluent-bit-hk585                                           5m           60Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-p62jp                                  2m           57Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-mm54r                                  1m           56Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-twdrh                                  1m           55Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-hbwfg                                  2m           55Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-mtz4w                                  2m           55Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-fd7hw                                  1m           54Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-t28qm                                  1m           54Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-89z8m                                  1m           54Mi
livelink-system                envoy-livelink-gateway-nmvvw                                         4m           50Mi
livelink-device                deployment-aapl-5f6cc64fdf-rp9sf                                     7m           50Mi
livelink-system                envoy-livelink-gateway-sqzwv                                         5m           49Mi
kube-system                    metrics-server-64d67ddb5-pvbmh                                       3m           48Mi
external-secrets               external-secrets-5f59dd8689-gkfvc                                    2m           48Mi
kube-system                    kube-scheduler-awby1-atlas-prd-k8wzk-wdptg                           5m           48Mi
kyverno-system                 kyverno-background-controller-f5c7b88c4-q7rzp                        2m           46Mi
external-secrets               external-secrets-5f59dd8689-8zll7                                    2m           45Mi
kyverno-system                 kyverno-cleanup-controller-566f85478f-z6m9r                          2m           45Mi
kube-system                    kube-scheduler-awby1-atlas-prd-k8wzk-vnqfl                           5m           45Mi
kube-system                    kube-scheduler-awby1-atlas-prd-k8wzk-b4rlv                           5m           44Mi
livelink-system                envoy-livelink-gateway-276sl                                         5m           44Mi
livelink-system                envoy-livelink-gateway-jfxhr                                         5m           44Mi
vmware-system-csi              vsphere-csi-node-vbr8f                                               3m           43Mi
dynatrace-operator             awby1-atlas-prd-otel-collector-0                                     1m           42Mi
vmware-system-csi              vsphere-csi-node-mbv5r                                               3m           37Mi
cert-manager                   cert-manager-7c4866444b-27dgj                                        4m           37Mi
secretgen-controller           secretgen-controller-68c7b987bb-2v5gj                                1m           37Mi
cnpg-system                    cloudnative-pg-59b4568767-wnqr7                                      2m           36Mi
cis-genai-poc-system           contour-keycloak-gateway-5db68cc469-2drfw                            3m           36Mi
cis-genai-poc-system           contour-itops-backend-gateway-747c799f4b-pw5wn                       3m           36Mi
cis-genai-poc-system           contour-frontend-gateway-64f845f99d-f59th                            3m           35Mi
cis-genai-poc-system           contour-grafana-gateway-6798cb5f64-tnt2m                             4m           35Mi
vmware-system-csi              vsphere-csi-node-ndsqn                                               4m           34Mi
kyverno-system                 kyverno-reports-controller-784d5f479b-nwx7j                          2m           34Mi
dynatrace-operator             dynatrace-fluent-bit-nwdz2                                           4m           32Mi
cis-genai-poc-system           envoy-keycloak-gateway-hrk58                                         4m           31Mi
cis-genai-poc-system           envoy-ticket-audit-gateway-7m6pv                                     4m           30Mi
cis-genai-poc-system           envoy-keycloak-gateway-th68m                                         4m           30Mi
cis-genai-poc-system           envoy-ticket-audit-gateway-g4pck                                     4m           30Mi
traefik-gateway                traefik-gateway-5c4c4f76fb-7lp4j                                     2m           30Mi
livelink-device                deployment-drs-64c588b775-85qr7                                      5m           30Mi
kyverno-system                 kyverno-background-controller-f5c7b88c4-g4dnh                        1m           30Mi
kube-system                    docker-registry-awby1-atlas-prd-k8wzk-wdptg                          1m           29Mi
cis-genai-poc-system           envoy-grafana-gateway-gc749                                          4m           28Mi
contour-system                 contour-envoy-kchwb                                                  5m           28Mi
contour-system                 contour-envoy-wbbsh                                                  5m           28Mi
contour-system                 contour-envoy-dwhcl                                                  5m           28Mi
confluent-system               confluent-operator-6b8c5f4d44-ff92m                                  3m           28Mi
cis-genai-poc-system           envoy-grafana-gateway-p5zds                                          4m           28Mi
livelink-system                contour-livelink-gateway-7c67cf46fb-4tcwn                            4m           28Mi
cis-genai-poc-system           contour-backend-gateway-86bb6d5c84-s5l2v                             3m           27Mi
cis-genai-poc-system           envoy-itops-backend-gateway-4tjxx                                    4m           27Mi
cis-genai-poc-system           envoy-frontend-gateway-jvnwr                                         4m           27Mi
cis-genai-poc-system           contour-ticket-audit-gateway-77457fb57b-hvk6q                        4m           27Mi
cis-genai-poc-system           envoy-keycloak-gateway-vlsgs                                         5m           27Mi
cis-genai-poc-system           envoy-ticket-audit-gateway-vn959                                     5m           27Mi
cis-genai-poc-system           envoy-grafana-gateway-vgpp5                                          4m           27Mi
cis-genai-poc-system           envoy-frontend-gateway-cb488                                         4m           27Mi
cis-genai-poc-system           envoy-itops-backend-gateway-w764r                                    5m           27Mi
cis-genai-poc-system           envoy-itops-backend-gateway-5cljl                                    5m           27Mi
cis-genai-poc-system           envoy-keycloak-gateway-zfzk7                                         5m           27Mi
cis-genai-poc-system           envoy-ticket-audit-gateway-vnlpm                                     4m           27Mi
cis-genai-poc-system           envoy-grafana-gateway-t8mqq                                          5m           27Mi
vmware-system-csi              vsphere-csi-node-xcz72                                               4m           27Mi
cis-genai-poc-system           envoy-itops-backend-gateway-bvv7j                                    5m           26Mi
contour-system                 contour-envoy-258bn                                                  5m           26Mi
contour-system                 contour-envoy-8pddf                                                  5m           26Mi
cis-genai-poc-system           envoy-frontend-gateway-z5ftm                                         5m           26Mi
cis-genai-poc-system           envoy-frontend-gateway-5k9n2                                         4m           26Mi
dynatrace-operator             dynatrace-operator-66cf9b4d9b-bq9p5                                  1m           25Mi
external-secrets               external-secrets-webhook-8687f4f5fc-224hm                            3m           25Mi
cis-genai-poc-system           envoy-backend-gateway-7zp72                                          4m           24Mi
cis-genai-poc-system           envoy-backend-gateway-hftgt                                          5m           24Mi
vmware-system-csi              vsphere-csi-node-ncbtw                                               3m           24Mi
kube-system                    kube-proxy-g6lmc                                                     1m           24Mi
cis-genai-poc-system           envoy-backend-gateway-l4gj2                                          4m           24Mi
cis-genai-poc-system           envoy-backend-gateway-bjhww                                          5m           23Mi
traefik-gateway                traefik-gateway-5c4c4f76fb-w9wnw                                     1m           23Mi
kube-system                    kube-proxy-c7wq8                                                     1m           23Mi
confluent-system               confluent-operator-6b8c5f4d44-fp9hn                                  2m           23Mi
vmware-system-csi              vsphere-csi-node-wdnsf                                               3m           23Mi
confluent-system               confluent-operator-6b8c5f4d44-2ml28                                  1m           23Mi
kube-system                    kube-proxy-vkpzj                                                     1m           23Mi
vmware-system-csi              vsphere-csi-node-wvnpv                                               2m           23Mi
vmware-system-csi              vsphere-csi-node-69hlt                                               3m           23Mi
vmware-system-cloud-provider   guest-cluster-cloud-provider-67f7d944f4-45l96                        2m           22Mi
flux-system                    notification-controller-77f79b6bbb-lf4mr                             1m           21Mi
atlas-system                   external-dns-758f679b65-bzxcn                                        2m           21Mi
kube-system                    coredns-6c67484c8c-gp78m                                             10m          21Mi
kube-system                    coredns-6c67484c8c-jzbj4                                             9m           21Mi
kyverno-system                 kyverno-cleanup-controller-566f85478f-j4rww                          2m           19Mi
kube-system                    kube-proxy-tsn8k                                                     1m           19Mi
kube-system                    kube-controller-manager-awby1-atlas-prd-k8wzk-b4rlv                  2m           19Mi
kube-system                    kube-proxy-76rqs                                                     1m           19Mi
kube-system                    kube-proxy-2fzhj                                                     1m           19Mi
kube-system                    kube-controller-manager-awby1-atlas-prd-k8wzk-wdptg                  2m           18Mi
kube-system                    kube-proxy-8qls9                                                     1m           18Mi
kube-system                    kube-proxy-bdsf8                                                     1m           18Mi
dynatrace-operator             dynatrace-webhook-967dbdf99-mbw2v                                    1m           18Mi
dynatrace-operator             dynatrace-webhook-967dbdf99-nr96x                                    1m           17Mi
atlas-system                   kubescape-operator-84c49bfbf8-9m4xg                                  1m           16Mi
atlas-system                   rbac-manager-fbb78f4b4-dcdrk                                         1m           16Mi
dynatrace-operator             dynatrace-fluent-bit-rfbfx                                           4m           15Mi
dynatrace-operator             dynatrace-fluent-bit-vgghh                                           7m           14Mi
cert-manager                   cert-manager-webhook-7786f5c8dd-grwtj                                1m           14Mi
cert-manager                   cert-manager-webhook-7786f5c8dd-gk9dc                                1m           13Mi
cert-manager                   cert-manager-webhook-7786f5c8dd-d799v                                1m           13Mi
kube-system                    docker-registry-awby1-atlas-prd-k8wzk-vnqfl                          1m           12Mi
cert-manager                   cert-manager-7c4866444b-z5gn5                                        1m           11Mi
dynatrace-operator             dynatrace-fluent-bit-7rgrd                                           3m           11Mi
dynatrace-operator             dynatrace-fluent-bit-7mhps                                           3m           10Mi
dynatrace-operator             dynatrace-fluent-bit-pg8j6                                           3m           10Mi
livelink-device                dmp-6799d6b9f9-kwr4r                                                 0m           10Mi
kube-system                    docker-registry-awby1-atlas-prd-k8wzk-b4rlv                          1m           10Mi
vmware-system-auth             guest-cluster-auth-svc-jcds6                                         1m           10Mi
kube-system                    docker-registry-awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-jqnvr   1m           10Mi
vmware-system-auth             guest-cluster-auth-svc-kzpnt                                         1m           9Mi
vmware-system-auth             guest-cluster-auth-svc-tbh6w                                         1m           9Mi
kube-system                    snapshot-controller-5fc8d85457-dfnsf                                 1m           8Mi
kube-system                    docker-registry-awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-5vbwl   1m           8Mi
dynatrace-operator             dynatrace-fluent-bit-sxp22                                           3m           7Mi
kube-system                    docker-registry-awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-xwdnw   1m           7Mi
kube-system                    docker-registry-awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-bknvv   1m           7Mi
kube-system                    docker-registry-awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-5pltm   1m           7Mi
cis-genai-poc-system           ticket-audit-backend-66ff54584d-f4qtl                                1m           0Mi
cis-genai-poc-system           ticket-audit-backend-66ff54584d-2n2nt                                1m           0Mi
mesac2@CPC-mesac-4BM7S:~$ kubectl get resourcequota -A
NAMESPACE                    NAME                        AGE    REQUEST                                 LIMIT
cis-genai-poc-db             atlas-default               42d    cpu: 300m/50, memory: 12Gi/500Gi
cis-genai-poc-incai-db       atlas-default               27d    cpu: 300m/50, memory: 12Gi/500Gi
cis-genai-poc-incai-system   atlas-default               27d    cpu: 0/50, memory: 0/500Gi
cis-genai-poc-system         atlas-default               42d    cpu: 11055m/50, memory: 15632Mi/500Gi
flux-system                  critical-pods-flux-system   302d   pods: 3/1k
livelink-db                  atlas-default               42d    cpu: 1200m/50, memory: 42Gi/500Gi
livelink-device              atlas-default               42d    cpu: 800m/50, memory: 1536Mi/500Gi
livelink-hivemq              atlas-default               42d    cpu: 2/50, memory: 4096M/500Gi
livelink-ignition            atlas-default               42d    cpu: 0/50, memory: 0/500Gi
livelink-kafka               atlas-default               42d    cpu: 0/50, memory: 0/500Gi
livelink-system              atlas-default               42d    cpu: 310m/50, memory: 600Mi/500Gi
mesac2@CPC-mesac-4BM7S:~$ kubectl get limitrange -A
NAMESPACE                    NAME            CREATED AT
cis-genai-poc-db             atlas-default   2026-07-09T21:23:06Z
cis-genai-poc-incai-db       atlas-default   2026-07-24T08:23:19Z
cis-genai-poc-incai-system   atlas-default   2026-07-24T08:23:19Z
cis-genai-poc-system         atlas-default   2026-07-09T21:23:06Z
livelink-db                  atlas-default   2026-07-09T21:23:06Z
livelink-device              atlas-default   2026-07-09T21:23:06Z
livelink-hivemq              atlas-default   2026-07-09T21:23:06Z
livelink-ignition            atlas-default   2026-07-09T21:23:06Z
livelink-kafka               atlas-default   2026-07-09T21:23:06Z
livelink-system              atlas-default   2026-07-09T21:23:07Z
