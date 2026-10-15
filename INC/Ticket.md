     
       kubectl -n "$NS" get pod "$POD"   -o jsonpath='{.metadata.ownerReferences[0].kind}{" "}{.metadata.ownerReferences[0].name}{"\n"}'
DaemonSet envoy-oee-gateway
mesac2@CPC-mesac-4BM7S:~$ DS=$(kubectl -n "$NS" get pod "$POD" \
  -o jsonpath='{.metadata.ownerReferences[0].name}')
mesac2@CPC-mesac-4BM7S:~$ kubectl -n "$NS" get ds "$DS" -o json |
jq '{
  nodeSelector: .spec.template.spec.nodeSelector,
  affinity: .spec.template.spec.affinity,
  tolerations: .spec.template.spec.tolerations,
  priorityClassName: .spec.template.spec.priorityClassName,
  requests: [.spec.template.spec.containers[] |
    {name: .name, requests: .resources.requests}]
}'
{
  "nodeSelector": null,
  "affinity": null,
  "tolerations": null,
  "priorityClassName": null,
  "requests": [
    {
      "name": "shutdown-manager",
      "requests": {
        "cpu": "25m",
        "memory": "50Mi"
      }
    },
    {
      "name": "envoy",
      "requests": {
        "cpu": "25m",
        "memory": "50Mi"
      }
    }
  ]
}


 kubectl get pods -A --field-selector spec.nodeName="$NODE" -o json |
jq -r '.items[] | [
  .metadata.namespace,
  .metadata.name,
  (.metadata.ownerReferences[0].kind // "-"),
  (.metadata.ownerReferences[0].name // "-"),
  (.spec.priority // 0),
  ([.spec.containers[].resources.requests.memory // "0"] | join("+"))
] | @tsv' | column -t
ami-mde-db          ami-mde-db-mde-prd-cluster-1                                            Cluster      ami-mde-db-mde-prd-cluster                  20500       2Gi
ami-mde-db          ami-mde-db-mde-prd-cluster-2                                            Cluster      ami-mde-db-mde-prd-cluster                  20500       2Gi
atlas-system        kubescape-node-agent-wvcjj                                              DaemonSet    kubescape-node-agent                        100000100   180Mi
cert-manager        cert-manager-65bb4b7446-4hk68                                           ReplicaSet   cert-manager-65bb4b7446                     1010000     200Mi
cert-manager        cert-manager-65bb4b7446-vfqzg                                           ReplicaSet   cert-manager-65bb4b7446                     1010000     200Mi
cert-manager        cert-manager-cainjector-5877b687dc-4rsms                                ReplicaSet   cert-manager-cainjector-5877b687dc          1010000     64Mi
cert-manager        cert-manager-cainjector-5877b687dc-g8bgx                                ReplicaSet   cert-manager-cainjector-5877b687dc          1010000     64Mi
cert-manager        cert-manager-webhook-5c564bbfcd-2vwnh                                   ReplicaSet   cert-manager-webhook-5c564bbfcd             1010000     32Mi
cert-manager        cert-manager-webhook-5c564bbfcd-6tkjx                                   ReplicaSet   cert-manager-webhook-5c564bbfcd             1010000     32Mi
cert-manager        cert-manager-webhook-5c564bbfcd-sv9hh                                   ReplicaSet   cert-manager-webhook-5c564bbfcd             1010000     32Mi
cnpg-system         cloudnative-pg-75d7696cb5-rvpr2                                         ReplicaSet   cloudnative-pg-75d7696cb5                   1000000     256Mi
cnpg-system         cloudnative-pg-75d7696cb5-wlz8k                                         ReplicaSet   cloudnative-pg-75d7696cb5                   1000000     256Mi
confluent-system    confluent-operator-77f55d5978-drtbv                                     ReplicaSet   confluent-operator-77f55d5978               1000000     256Mi
confluent-system    confluent-operator-77f55d5978-rqxrj                                     ReplicaSet   confluent-operator-77f55d5978               1000000     256Mi
confluent-system    confluent-operator-77f55d5978-tlvhk                                     ReplicaSet   confluent-operator-77f55d5978               1000000     256Mi
contour-system      contour-contour-88c7c6c9b-qrcnl                                         ReplicaSet   contour-contour-88c7c6c9b                   1005000     128Mi
contour-system      contour-envoy-l5xbs                                                     DaemonSet    contour-envoy                               1005000     128Mi+128Mi
contour-system      contour-gateway-provisioner-68d5bd54dc-4flnv                            ReplicaSet   contour-gateway-provisioner-68d5bd54dc      0           128Mi
dynatrace-operator  dynatrace-fluent-bit-h4bnb                                              DaemonSet    dynatrace-fluent-bit                        1000000     128Mi
dynatrace-operator  dynatrace-oneagent-csi-driver-477zn                                     DaemonSet    dynatrace-oneagent-csi-driver               1000000     100Mi+100Mi+30Mi+30Mi
gpu-operator        gpu-operator-node-feature-discovery-worker-prrfl                        DaemonSet    gpu-operator-node-feature-discovery-worker  2000001000  64Mi
kube-system         antrea-agent-9zpqt                                                      DaemonSet    antrea-agent                                2000001000  0
kube-system         kube-proxy-zh44f                                                        DaemonSet    kube-proxy                                  2000001000  0
kube-system         node-debugger-adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6-6gnm9  -            -                                           0           0
kube-system         node-debugger-adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6-vwl9c  -            -                                           0           0
livelink-db         livelink-db-01-cluster-1                                                Cluster      livelink-db-01-cluster                      20500       6Gi
livelink-db         livelink-db-01-cluster-3                                                Cluster      livelink-db-01-cluster                      20500       6Gi
livelink-db         livelink-db-site-custom-cluster-1                                       Cluster      livelink-db-site-custom-cluster             20500       2Gi
livelink-db         livelink-db-timescaledb-cluster-1                                       Cluster      livelink-db-timescaledb-cluster             20500       4Gi
livelink-hivemq     livelink-hivemq-3                                                       StatefulSet  livelink-hivemq                             0           2Gi
livelink-system     envoy-oee-gateway-67t2p                                                 DaemonSet    envoy-oee-gateway                           0           50Mi+50Mi
vmware-system-csi   vsphere-csi-node-77xgr                                                  DaemonSet    vsphere-csi-node                            2000001000  0
mesac2@CPC-mesac-4BM7S:~$ kubectl top node "$NODE"
Error from server (NotFound): nodemetrics.metrics.k8s.io "adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6" not found
mesac2@CPC-mesac-4BM7S:~$ kubectl top pods -A --containers --sort-by=memory
NAMESPACE                      POD                                                          NAME
             CPU(cores)   MEMORY(bytes)
livelink-ignition              livelink-ignition-gateway-0                                  gateway
             393m         5152Mi
livelink-ignition              livelink-ignition-gateway-0                                  ldap-seeder                    0m           0Mi
kube-system                    kube-apiserver-adan1-atlas-prd-r7ftq-whtgs                   kube-apiserver                 61m          3036Mi
kube-system                    kube-apiserver-adan1-atlas-prd-r7ftq-gtxjx                   kube-apiserver                 70m          2927Mi
kube-system                    kube-apiserver-adan1-atlas-prd-r7ftq-fqdq2                   kube-apiserver                 63m          2727Mi
gpu-operator                   nvidia-driver-daemonset-5p5k2                                nvidia-driver-ctr              0m           1812Mi
gpu-operator                   nvidia-driver-daemonset-5lksh                                nvidia-driver-ctr              0m           1807Mi
gpu-operator                   nvidia-driver-daemonset-lmwp2                                nvidia-driver-ctr              0m           1806Mi
dynatrace-operator             adan1-atlas-prd-activegate-0                                 activegate                     31m          1389Mi
livelink-hivemq                livelink-hivemq-2                                            hivemq
             16m          1183Mi
livelink-hivemq                livelink-hivemq-0                                            hivemq
             14m          1183Mi
livelink-hivemq                livelink-hivemq-1                                            hivemq
             16m          1172Mi
hivemq-operator                hivemq-hivemq-operator-6b4fdb876b-5vlgt                      hivemq-platform-operator       2m           600Mi
gpu-operator                   nvidia-dcgm-exporter-hz6lw                                   nvidia-dcgm-exporter           5m           424Mi
gpu-operator                   nvidia-dcgm-exporter-7twr8                                   nvidia-dcgm-exporter           4m           422Mi
gpu-operator                   nvidia-dcgm-exporter-wxrdh                                   nvidia-dcgm-exporter           6m           421Mi
livelink-db                    livelink-db-01-cluster-2                                     postgres
             198m         326Mi
atlas-system                   kubescape-node-agent-b6ljm                                   kubescape-node-agent           3m           258Mi
livelink-db                    livelink-db-platform-cluster-1                               postgres
             6m           244Mi
livelink-db                    livelink-db-platform-cluster-2                               postgres
             6m           240Mi
atlas-system                   kubescape-node-agent-bc9px                                   kubescape-node-agent           3m           216Mi
atlas-system                   kubescape-node-agent-98m9w                                   kubescape-node-agent           2m           209Mi
atlas-system                   kubescape-node-agent-gw4jn                                   kubescape-node-agent           2m           208Mi
livelink-db                    livelink-db-timescaledb-cluster-3                            postgres
             4m           206Mi
livelink-db                    livelink-db-timescaledb-cluster-2                            postgres
             3m           205Mi
atlas-system                   kubescape-node-agent-mnptw                                   kubescape-node-agent           2m           204Mi
livelink-db                    livelink-db-platform-cluster-3                               postgres
             3m           193Mi
kube-system                    antrea-agent-2klvj                                           antrea-ovs                     13m          112Mi
kube-system                    antrea-agent-2klvj                                           antrea-agent                   7m           73Mi
kube-system                    antrea-agent-bh5cc                                           antrea-ovs                     19m          113Mi
kube-system                    antrea-agent-bh5cc                                           antrea-agent                   12m          70Mi
kube-system                    kube-controller-manager-adan1-atlas-prd-r7ftq-gtxjx          kube-controller-manager        16m          182Mi
kube-system                    antrea-agent-jnvrj                                           antrea-ovs                     8m           109Mi
kube-system                    antrea-agent-jnvrj                                           antrea-agent                   7m           68Mi
kube-system                    antrea-agent-sqp5j                                           antrea-ovs                     8m           109Mi
kube-system                    antrea-agent-sqp5j                                           antrea-agent                   7m           67Mi
kube-system                    antrea-agent-m6tlm                                           antrea-ovs                     8m           109Mi
kube-system                    antrea-agent-m6tlm                                           antrea-agent                   7m           66Mi
kube-system                    etcd-adan1-atlas-prd-r7ftq-gtxjx                             etcd
             48m          165Mi
kube-system                    antrea-agent-rz9ks                                           antrea-agent                   7m           93Mi
kube-system                    antrea-agent-rz9ks                                           antrea-ovs                     11m          69Mi
kube-system                    etcd-adan1-atlas-prd-r7ftq-whtgs                             etcd
             54m          150Mi
livelink-db                    livelink-db-site-custom-cluster-3                            postgres
             5m           146Mi
ami-mde-db                     ami-mde-db-mde-prd-cluster-3                                 postgres
             5m           143Mi
kube-system                    etcd-adan1-atlas-prd-r7ftq-fqdq2                             etcd
             72m          142Mi
kube-system                    antrea-agent-kjq85                                           antrea-agent                   6m           73Mi
kube-system                    antrea-agent-kjq85                                           antrea-ovs
             7m           67Mi
kube-system                    antrea-agent-nwxkb                                           antrea-agent                   5m           69Mi
kube-system                    antrea-agent-nwxkb                                           antrea-ovs                     8m           68Mi
tkg-system                     kapp-controller-7689ccb896-tnwrr                             kapp-controller                2m           107Mi
tkg-system                     kapp-controller-7689ccb896-tnwrr                             kapp-controller-sidecarexec    1m           14Mi
livelink-db                    livelink-db-site-custom-cluster-2                            postgres
             287m         120Mi
gpu-operator                   gpu-feature-discovery-468tx                                  gpu-feature-discovery          1m           68Mi
gpu-operator                   gpu-feature-discovery-468tx                                  config-manager                 1m           34Mi
vmware-system-csi              vsphere-csi-controller-9778f7d45-dlzlh                       vsphere-syncer                 2m           26Mi
vmware-system-csi              vsphere-csi-controller-9778f7d45-dlzlh                       vsphere-csi-controller         1m           19Mi
vmware-system-csi              vsphere-csi-controller-9778f7d45-dlzlh                       csi-provisioner                1m           15Mi
vmware-system-csi              vsphere-csi-controller-9778f7d45-dlzlh                       csi-attacher                   1m           11Mi
vmware-system-csi              vsphere-csi-controller-9778f7d45-dlzlh                       csi-resizer                    2m           10Mi
vmware-system-csi              vsphere-csi-controller-9778f7d45-dlzlh                       csi-snapshotter                1m           10Mi
vmware-system-csi              vsphere-csi-controller-9778f7d45-dlzlh                       liveness-probe                 1m           6Mi
gpu-operator                   gpu-feature-discovery-g2k2k                                  gpu-feature-discovery          1m           64Mi
gpu-operator                   gpu-feature-discovery-g2k2k                                  config-manager                 1m           35Mi
gpu-operator                   gpu-feature-discovery-8ssdm                                  gpu-feature-discovery          3m           67Mi
gpu-operator                   gpu-feature-discovery-8ssdm                                  config-manager                 1m           30Mi
flux-system                    kustomize-controller-5f7c645849-txfdf                        manager
             29m          95Mi
kube-system                    antrea-controller-6c6bcd674d-v8b5x                           antrea-controller              3m           83Mi
gpu-operator                   gpu-operator-78cc869767-9xjdr                                gpu-operator                   1m           80Mi
gpu-operator                   nvidia-device-plugin-daemonset-gnqq4                         nvidia-device-plugin           1m           43Mi
gpu-operator                   nvidia-device-plugin-daemonset-gnqq4                         config-manager                 1m           34Mi
gpu-operator                   nvidia-device-plugin-daemonset-8pmbl                         nvidia-device-plugin           1m           42Mi
gpu-operator                   nvidia-device-plugin-daemonset-8pmbl                         config-manager                 1m           35Mi
gpu-operator                   nvidia-device-plugin-daemonset-lbm6w                         nvidia-device-plugin           1m           38Mi
gpu-operator                   nvidia-device-plugin-daemonset-lbm6w                         config-manager                 1m           35Mi
external-secrets               external-secrets-cert-controller-597c787487-wtnsg            cert-controller                1m           73Mi
kyverno-system                 kyverno-admission-controller-5655b8f7fc-mtt4s                kyverno
             9m           66Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-shwfq                          provisioner                    1m           25Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-shwfq                          server
             1m           13Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-shwfq                          liveness-probe                 1m           10Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-shwfq                          registrar
             0m           8Mi
kyverno-system                 kyverno-reports-controller-5c5cbf88f9-tx2vs                  controller                     2m           59Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-x9x2d                          provisioner                    1m           25Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-x9x2d                          server
             1m           11Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-x9x2d                          liveness-probe                 1m           10Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-x9x2d                          registrar
             0m           8Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-xprqz                          provisioner                    1m           25Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-xprqz                          server
             1m           11Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-xprqz                          liveness-probe                 1m           11Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-xprqz                          registrar
             0m           8Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-ft8zh                          provisioner                    1m           25Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-ft8zh                          server
             1m           11Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-ft8zh                          liveness-probe                 1m           10Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-ft8zh                          registrar
             0m           8Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-9n6g9                          provisioner                    1m           24Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-9n6g9                          liveness-probe                 1m           11Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-9n6g9                          server
             1m           10Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-9n6g9                          registrar
             1m           8Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-8njtl                          provisioner                    1m           24Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-8njtl                          liveness-probe                 1m           11Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-8njtl                          server
             1m           10Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-8njtl                          registrar
             1m           8Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-gvhh4                          provisioner                    1m           24Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-gvhh4                          liveness-probe                 1m           11Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-gvhh4                          server
             1m           10Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-gvhh4                          registrar
             1m           8Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-fg2gd                          provisioner                    1m           24Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-fg2gd                          liveness-probe                 1m           11Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-fg2gd                          server
             1m           10Mi
dynatrace-operator             dynatrace-oneagent-csi-driver-fg2gd                          registrar
             0m           8Mi
kyverno-system                 kyverno-admission-controller-5655b8f7fc-rst4g                kyverno
             3m           51Mi
vmware-system-csi              vsphere-csi-node-bq5jn                                       vsphere-csi-node               1m           38Mi
vmware-system-csi              vsphere-csi-node-bq5jn                                       node-driver-registrar          2m           7Mi
vmware-system-csi              vsphere-csi-node-bq5jn                                       liveness-probe                 1m           4Mi
atlas-system                   kubescape-storage-87b86b58f-2hl2c                            apiserver
             2m           48Mi
flux-system                    source-controller-7c74d5c6d8-lvv85                           manager
             4m           48Mi
gpu-operator                   nvidia-container-toolkit-daemonset-htrj8                     nvidia-container-toolkit-ctr   2m           48Mi
kube-system                    metrics-server-7fb85c98cf-gw9vf                              metrics-server                 2m           47Mi
gpu-operator                   nvidia-container-toolkit-daemonset-8njg5                     nvidia-container-toolkit-ctr   2m           47Mi
gpu-operator                   nvidia-container-toolkit-daemonset-rzxjr                     nvidia-container-toolkit-ctr   2m           47Mi
dynatrace-operator             dynatrace-fluent-bit-szxhr                                   fluent-bit                     8m           47Mi
kyverno-system                 kyverno-admission-controller-5655b8f7fc-4q724                kyverno
             2m           47Mi
kube-system                    kube-scheduler-adan1-atlas-prd-r7ftq-gtxjx                   kube-scheduler                 4m           47Mi
livelink-system                envoy-oee-gateway-fdfhr                                      envoy
             3m           34Mi
livelink-system                envoy-oee-gateway-fdfhr                                      shutdown-manager               1m           8Mi
livelink-system                envoy-oee-gateway-58ffl                                      envoy
             3m           33Mi
livelink-system                envoy-oee-gateway-58ffl                                      shutdown-manager               1m           8Mi
external-secrets               external-secrets-55777f75bd-d92tw                            external-secrets               5m           38Mi
kube-system                    kube-scheduler-adan1-atlas-prd-r7ftq-whtgs                   kube-scheduler                 4m           38Mi
kube-system                    kube-scheduler-adan1-atlas-prd-r7ftq-fqdq2                   kube-scheduler                 4m           38Mi
kyverno-system                 kyverno-background-controller-65cd987565-bbpg4               controller                     1m           38Mi
vmware-system-csi              vsphere-csi-node-9wftd                                       vsphere-csi-node               0m           23Mi
vmware-system-csi              vsphere-csi-node-9wftd                                       node-driver-registrar          1m           6Mi
vmware-system-csi              vsphere-csi-node-9wftd                                       liveness-probe                 1m           4Mi
livelink-system                contour-oee-gateway-7745c66598-t9jzh                         contour
             1m           31Mi
external-secrets               external-secrets-55777f75bd-6jpqt                            external-secrets               2m           31Mi
contour-system                 contour-envoy-xh86p                                          envoy
             4m           18Mi
contour-system                 contour-envoy-xh86p                                          shutdown-manager               1m           10Mi
kyverno-system                 kyverno-reports-controller-5c5cbf88f9-zthkr                  controller                     2m           28Mi
dynatrace-operator             adan1-atlas-prd-otel-collector-0                             collector
             1m           28Mi
kyverno-system                 kyverno-background-controller-65cd987565-m9kr7               controller                     1m           27Mi
secretgen-controller           secretgen-controller-9ff67f8bb-4mckq                         secretgen-controller           1m           27Mi
flux-system                    helm-controller-6d98cf5d6-njzr6                              manager
             1m           27Mi
dynatrace-operator             dynatrace-operator-b55659d64-c6j9k                           operator
             1m           27Mi
contour-system                 contour-envoy-46zbs                                          envoy
             4m           18Mi
contour-system                 contour-envoy-46zbs                                          shutdown-manager               1m           8Mi
contour-system                 contour-envoy-xtfhs                                          envoy
             4m           18Mi
contour-system                 contour-envoy-xtfhs                                          shutdown-manager               1m           8Mi
contour-system                 contour-envoy-jsm27                                          envoy
             4m           18Mi
contour-system                 contour-envoy-jsm27                                          shutdown-manager               1m           8Mi
vmware-system-cloud-provider   guest-cluster-cloud-provider-7d444d9946-tpgw6                guest-cluster-cloud-provider   1m           26Mi
contour-system                 contour-envoy-85mqn                                          envoy
             4m           18Mi
contour-system                 contour-envoy-85mqn                                          shutdown-manager               1m           8Mi
kyverno-system                 kyverno-cleanup-controller-5d9f8df4d-bzhf6                   controller                     6m           25Mi
external-secrets               external-secrets-webhook-c566b5766-cq554                     webhook
             2m           24Mi
traefik-gateway                traefik-gateway-6c9458d4bc-rcbl2                             traefik-gateway                1m           23Mi
vmware-system-csi              vsphere-csi-node-4z298                                       vsphere-csi-node               1m           13Mi
vmware-system-csi              vsphere-csi-node-4z298                                       node-driver-registrar          1m           5Mi
vmware-system-csi              vsphere-csi-node-4z298                                       liveness-probe                 1m           3Mi
livelink-system                contour-oee-gateway-7745c66598-4njzt                         contour
             1m           23Mi
traefik-gateway                traefik-gateway-6c9458d4bc-smxtq                             traefik-gateway                1m           23Mi
dynatrace-operator             dynatrace-fluent-bit-rtg6k                                   fluent-bit                     25m          22Mi
kube-system                    coredns-697df5f89-mpjrp                                      coredns
             5m           22Mi
gpu-operator                   gpu-operator-node-feature-discovery-master-b946897dc-mk7kp   master
             1m           22Mi
kube-system                    coredns-697df5f89-r4t5r                                      coredns
             6m           22Mi
vmware-system-csi              vsphere-csi-node-fncfv                                       vsphere-csi-node               1m           11Mi
vmware-system-csi              vsphere-csi-node-fncfv                                       node-driver-registrar          2m           5Mi
vmware-system-csi              vsphere-csi-node-fncfv                                       liveness-probe                 1m           4Mi
vmware-system-csi              vsphere-csi-node-78d5h                                       vsphere-csi-node               1m           11Mi
vmware-system-csi              vsphere-csi-node-78d5h                                       node-driver-registrar          2m           5Mi
vmware-system-csi              vsphere-csi-node-78d5h                                       liveness-probe                 1m           4Mi
vmware-system-csi              vsphere-csi-node-k8q7v                                       vsphere-csi-node               0m           11Mi
vmware-system-csi              vsphere-csi-node-k8q7v                                       node-driver-registrar          2m           5Mi
vmware-system-csi              vsphere-csi-node-k8q7v                                       liveness-probe                 1m           4Mi
flux-system                    notification-controller-8966476d6-pl9j6                      manager
             1m           21Mi
dynatrace-operator             dynatrace-fluent-bit-9jz7v                                   fluent-bit                     4m           21Mi
vmware-system-csi              vsphere-csi-node-thscd                                       vsphere-csi-node               1m           11Mi
vmware-system-csi              vsphere-csi-node-thscd                                       node-driver-registrar          1m           5Mi
vmware-system-csi              vsphere-csi-node-thscd                                       liveness-probe                 1m           4Mi
gpu-operator                   gpu-operator-node-feature-discovery-worker-rwgtw             worker
             1m           20Mi
vmware-system-csi              vsphere-csi-node-rjlcx                                       vsphere-csi-node               0m           11Mi
vmware-system-csi              vsphere-csi-node-rjlcx                                       node-driver-registrar          2m           4Mi
vmware-system-csi              vsphere-csi-node-rjlcx                                       liveness-probe                 1m           4Mi
kube-system                    kube-proxy-pxplb                                             kube-proxy                     1m           19Mi
kube-system                    kube-proxy-gmg5z                                             kube-proxy                     1m           18Mi
kube-system                    kube-proxy-7xgkm                                             kube-proxy                     1m           18Mi
kube-system                    kube-proxy-cpcsd                                             kube-proxy                     1m           18Mi
kube-system                    kube-proxy-nxcc4                                             kube-proxy                     1m           18Mi
atlas-system                   external-dns-7d99b6978-57w2c                                 external-dns                   1m           18Mi
kube-system                    kube-proxy-265gm                                             kube-proxy                     1m           18Mi
kube-system                    kube-proxy-dmjs8                                             kube-proxy                     1m           18Mi
kyverno-system                 kyverno-cleanup-controller-5d9f8df4d-zd82k                   controller                     1m           18Mi
kube-system                    kube-controller-manager-adan1-atlas-prd-r7ftq-whtgs          kube-controller-manager        1m           17Mi
kube-system                    kube-controller-manager-adan1-atlas-prd-r7ftq-fqdq2          kube-controller-manager        1m           17Mi
kube-system                    kube-proxy-lh694                                             kube-proxy                     1m           17Mi
dynatrace-operator             dynatrace-webhook-86457b7fc7-txx7m                           webhook
             1m           17Mi
atlas-system                   kubescape-operator-547fb9745f-xv4hw                          kubescape-operator             1m           15Mi
dynatrace-operator             dynatrace-webhook-86457b7fc7-dv8qh                           webhook
             1m           15Mi
gpu-operator                   gpu-operator-node-feature-discovery-worker-84lzv             worker
             1m           15Mi
gpu-operator                   gpu-operator-node-feature-discovery-worker-8t74v             worker
             1m           14Mi
gpu-operator                   gpu-operator-node-feature-discovery-worker-cb7wb             worker
             1m           14Mi
gpu-operator                   gpu-operator-node-feature-discovery-worker-lhzcn             worker
             1m           14Mi
gpu-operator                   gpu-operator-node-feature-discovery-worker-728hc             worker
             1m           14Mi
gpu-operator                   gpu-operator-node-feature-discovery-worker-vs8nt             worker
             1m           14Mi
livelink-system                reloader-647db4846c-6fpwc                                    reloader
             1m           14Mi
atlas-system                   rbac-manager-86976c7b6f-9pjdb                                rbac-manager                   1m           14Mi
gpu-operator                   gpu-operator-node-feature-discovery-worker-6lmgs             worker
             1m           13Mi
emqx-operator                  emqx-operator-controller-manager-5477575666-z4b58            manager
             1m           13Mi
dynatrace-operator             dynatrace-fluent-bit-fzllg                                   fluent-bit                     2m           11Mi
dynatrace-operator             dynatrace-fluent-bit-b6dtl                                   fluent-bit                     2m           10Mi
dynatrace-operator             dynatrace-fluent-bit-zncps                                   fluent-bit                     2m           10Mi
dynatrace-operator             dynatrace-fluent-bit-gjhr8                                   fluent-bit                     3m           9Mi
dynatrace-operator             dynatrace-fluent-bit-dj5ff                                   fluent-bit                     3m           9Mi
gpu-operator                   gpu-operator-node-feature-discovery-gc-bdbc64d47-rx2r7       gc
             1m           9Mi
vmware-system-auth             guest-cluster-auth-svc-mtkt7                                 guest-cluster-auth-service     1m           9Mi
vmware-system-auth             guest-cluster-auth-svc-sg7jg                                 guest-cluster-auth-service     1m           8Mi
vmware-system-auth             guest-cluster-auth-svc-glthd                                 guest-cluster-auth-service     1m           8Mi
kube-system                    snapshot-controller-798c455795-n2fqg                         snapshot-controller            1m           8Mi
gpu-operator                   nvidia-operator-validator-n22lc                              nvidia-operator-validator      0m           1Mi
gpu-operator                   nvidia-operator-validator-h524m                              nvidia-operator-validator      0m           1Mi
gpu-operator                   nvidia-operator-validator-rs7hh                              nvidia-operator-validator      1m           1Mi
ami-svp                        netshoot01-5c4574d9f8-8fhbd                                  netshoot
             4m           0Mi
mesac2@CPC-mesac-4BM7S:~$ kubectl get pdb -A
NAMESPACE            NAME                            MIN AVAILABLE   MAX UNAVAILABLE   ALLOWED DISRUPTIONS   AGE
atlas-system         external-dns                    N/A             1                 1                     71d
cert-manager         cert-manager                    1               N/A               1                     71d
cert-manager         cert-manager-cainjector         1               N/A               1                     71d
cert-manager         cert-manager-webhook            1               N/A               0                     71d
dynatrace-operator   dynatrace-webhook               1               N/A               1                     71d
kyverno-system       kyverno-admission-controller    1               N/A               2                     71d
kyverno-system       kyverno-background-controller   1               N/A               1                     71d
kyverno-system       kyverno-cleanup-controller      1               N/A               1                     71d
kyverno-system       kyverno-reports-controller      1               N/A               1                     71d
mesac2@CPC-mesac-4BM7S:~$