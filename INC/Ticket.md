Worked for 46s

Revisé el archivo. Aún no recomiendo eliminar ningún pod. Los resultados confirman la saturación por requests, pero falta validar el estado actual de Envoy antes de ejecutar una acción disruptiva.

Hallazgos

1. El DaemonSet de Envoy solicita exactamente 100Mi:

shutdown-manager: 50Mi

envoy: 50Mi



2. El template no tiene:

nodeSelector

affinity

tolerations

priorityClassName


Por tanto, la NodeAffinity observada en el pod no viene de una configuración incorrecta del chart; Kubernetes la genera para asociar cada pod del DaemonSet con su nodo. 


3. El nodo tiene una concentración considerable de bases de datos:

ami-mde: 4Gi

livelink-db-01: 12Gi

site-custom: 2Gi

timescaledb: 4Gi


Total de PostgreSQL solicitado: 22Gi.

También está livelink-hivemq-3 con 2Gi.


4. No existen PDB para:

CloudNativePG

HiveMQ

Confluent Operator

Envoy OEE Gateway


Esto es un riesgo serio. Borrar una réplica de PostgreSQL o HiveMQ no cuenta con protección declarativa contra una interrupción de disponibilidad.


5. kubectl top node falló porque Metrics Server no tiene un objeto de métricas para ese nodo. Eso no significa necesariamente que el nodo no exista.


6. Hay una inconsistencia importante: envoy-oee-gateway-67t2p aparece con spec.nodeName asignado al nodo. Esto podría significar que ya pasó scheduling y ahora está bloqueado en otra fase. El evento anterior de Insufficient memory puede ser histórico.



Próxima validación obligatoria

Ejecuta esto antes de mover cualquier workload:

kubectl get node "$NODE" -o wide

kubectl -n "$NS" get pod "$POD" -o json |
jq '{
  phase: .status.phase,
  node: .spec.nodeName,
  conditions: .status.conditions,
  initContainers: .status.initContainerStatuses,
  containers: .status.containerStatuses
}'

kubectl -n "$NS" describe pod "$POD"

kubectl -n "$NS" get ds "$DS" -o wide

También revisa los eventos recientes:

kubectl get events -A \
  --sort-by=.metadata.creationTimestamp |
tail -n 100

Esto determinará si el problema actual es:

FailedScheduling

ContainerCreating

Montaje de volúmenes

Image pull

Dynatrace injection

CNI/network

Otro fallo posterior al scheduling


Validación de HiveMQ

livelink-hivemq-3 parece el único workload de aplicación con suficiente request para liberar memoria, pero es un StatefulSet, no un pod stateless. No debe eliminarse sin confirmar su estado y arquitectura:

kubectl -n livelink-hivemq get sts,pods -o wide

kubectl -n livelink-hivemq get sts livelink-hivemq -o json |
jq '{
  replicas: .spec.replicas,
  readyReplicas: .status.readyReplicas,
  currentReplicas: .status.currentReplicas,
  podManagementPolicy: .spec.podManagementPolicy,
  updateStrategy: .spec.updateStrategy,
  affinity: .spec.template.spec.affinity,
  topologySpreadConstraints: .spec.template.spec.topologySpreadConstraints
}'

kubectl -n livelink-hivemq describe pod livelink-hivemq-3

No sabemos todavía por qué livelink-hivemq-3 no aparece en kubectl top. Podría estar iniciando, no tener métricas o encontrarse en un estado anormal.

Decisión recomendada

No ejecutes todavía:

kubectl delete pod ...
kubectl cordon ...

Tampoco elimines una instancia CloudNativePG. Las dos opciones pueden trasladar el problema a HiveMQ o a una base de datos y no garantizan que el pod recreado termine en otro nodo.

El orden correcto es:

1. Confirmar el estado actual de Envoy.


2. Confirmar el estado de livelink-hivemq-3.


3. Verificar capacidad por requests en los otros worker nodes.


4. Si Envoy continúa en FailedScheduling, solicitar expansión del worker pool.


5. Solo como mitigación urgente, coordinar con el propietario de HiveMQ o DBA una reubicación controlada.



Con la evidencia actual, la solución más segura sigue siendo aumentar capacidad. Mover una base de datos para liberar únicamente 88Mi sería una intervención desproporcionada y arriesgada.
















tem                   kubescape-node-agent-98m9w                                   kubescape-node-agent           2m           209Mi
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
