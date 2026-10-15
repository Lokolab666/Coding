
      Worked for 2m 12s

Revisé el archivo. La causa está bastante clara: el pod de Envoy no inicia porque el nodo asignado no tiene memoria disponible a nivel de requests, aunque el nodo no tenga MemoryPressure.

Nodo afectado: adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6

Memoria allocatable: aproximadamente 28,387.8 Mi

Requests actuales: 28,376 Mi

Espacio restante: aproximadamente 11.8 Mi

Envoy necesita: 100 Mi

Déficit: aproximadamente 88 Mi


El mensaje 8 NodeAffinity es esperado para un pod creado por un DaemonSet: Kubernetes fija cada pod del DaemonSet a su nodo correspondiente. No debes eliminar esa afinidad del pod. 

Qué recomiendo hacer

1. Validar la configuración real del DaemonSet

NS=livelink-system
POD=envoy-oee-gateway-67t2p
NODE=adan1-atlas-prd-worker-node-pool-zh8pf-n6hkk-xq6j6

kubectl -n "$NS" get pod "$POD" \
  -o jsonpath='{.metadata.ownerReferences[0].kind}{" "}{.metadata.ownerReferences[0].name}{"\n"}'

DS=$(kubectl -n "$NS" get pod "$POD" \
  -o jsonpath='{.metadata.ownerReferences[0].name}')

kubectl -n "$NS" get ds "$DS" -o json |
jq '{
  nodeSelector: .spec.template.spec.nodeSelector,
  affinity: .spec.template.spec.affinity,
  tolerations: .spec.template.spec.tolerations,
  priorityClassName: .spec.template.spec.priorityClassName,
  requests: [.spec.template.spec.containers[] |
    {name: .name, requests: .resources.requests}]
}'

Esto confirma qué nodos son realmente elegibles y si existe alguna PriorityClass.

2. Encontrar un workload seguro para reubicar

Primero revisa todos los pods del nodo:

kubectl get pods -A --field-selector spec.nodeName="$NODE" -o json |
jq -r '.items[] | [
  .metadata.namespace,
  .metadata.name,
  (.metadata.ownerReferences[0].kind // "-"),
  (.metadata.ownerReferences[0].name // "-"),
  (.spec.priority // 0),
  ([.spec.containers[].resources.requests.memory // "0"] | join("+"))
] | @tsv' | column -t

kubectl top node "$NODE"
kubectl top pods -A --containers --sort-by=memory
kubectl get pdb -A

Busca preferentemente un pod:

Stateless.

Con múltiples réplicas.

Con request de memoria superior a 100Mi.

Sin un PDB que impida moverlo.

Que pueda ejecutarse en otro nodo.


Mover un workload stateless es menos riesgoso que eliminar una réplica PostgreSQL.

3. Si es necesario mover un pod

Hazlo solamente después de confirmar el propietario, las réplicas, el PDB y la capacidad del nodo destino:

kubectl cordon "$NODE"

kubectl -n <namespace> delete pod <approved-stateless-pod>

kubectl -n <namespace> get pods -o wide -w
kubectl -n livelink-system get pods -o wide -w

Una vez que el workload y Envoy estén Running:

kubectl uncordon "$NODE"

kubectl -n livelink-system get ds,pods -o wide
kubectl describe node "$NODE" | sed -n '/Allocated resources:/,/Events:/p'

No eliminaría todavía ami-mde-db-mde-prd-cluster-2. Kubernetes podría recrearlo en el mismo nodo, y eliminar directamente un pod de CloudNativePG puede evitar las protecciones del PDB. Eso requiere aprobación y procedimiento del equipo de base de datos. 

4. Revisar CloudNativePG

for ns in ami-mde-db livelink-db; do
  kubectl -n "$ns" get clusters.postgresql.cnpg.io -o json |
  jq -r '.items[] | [
    .metadata.name,
    (.spec.affinity.enablePodAntiAffinity // true),
    (.spec.affinity.podAntiAffinityType // "preferred"),
    (.spec.affinity.topologyKey // "kubernetes.io/hostname"),
    (.spec.affinity.nodeSelector // {})
  ] | @json'
done

kubectl get storageclass
kubectl get pv -o wide
kubectl get pdb -A | grep -E 'ami-mde|livelink'

Actualmente existen problemas de distribución:

Las dos réplicas de livelink-db-01 están en xq6j6.

Las dos réplicas de ami-mde están en xq6j6.

Algunos primarios y réplicas están juntos en bx9ct.

Todos los cinco primarios están concentrados en bx9ct.


CloudNativePG usa normalmente anti-affinity preferred; cambiarla a required mejora la separación, pero puede dejar instancias Pending si no existe capacidad suficiente. 

Solución permanente recomendada

El orden correcto sería:

1. Aumentar la capacidad del worker pool —idealmente agregar otro nodo— mediante el repositorio GitOps de adan1-atlas-prd.


2. Redistribuir las instancias PostgreSQL con participación del equipo DBA.


3. Configurar anti-affinity o topology spreading declarativamente.


4. Revisar y ajustar los memory requests sobredimensionados.


5. Crear alertas cuando los requests superen 80–85% de la capacidad.


6. Evaluar una PriorityClass para Envoy únicamente como protección futura.



No implementaría ahora priorityClassName en ContourDeployment: el CRD instalado no expone ese campo. Además, una prioridad de 1000 podría desalojar cualquier workload de prioridad 0; los PDB se respetan solamente como mejor esfuerzo durante preemption. 

Conclusión: libera al menos 100Mi moviendo un pod stateless aprobado para recuperar Envoy; después agrega capacidad y corrige la distribución de CloudNativePG. La PriorityClass no es la solución inmediata y no crea memoria adicional.