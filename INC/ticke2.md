Worked for 1m 31s

El problema ya no parece estar en Tanzu/Antrea. La política del clúster permite TCP y UDP 44818, y los intentos reales del pod Ignition salen como Allow. El siguiente punto de investigación es el IE3400/L2NAT y la comunicación EtherNet/IP con el PLC. 

Correcciones a las conclusiones de la otra IA

La otra IA llegó demasiado rápido a algunas conclusiones:

Traceflow: ForwardedOutOfOverlay solo prueba que Antrea deja salir el paquete; no prueba que llegue al PLC.

El exit=0 prueba que se completó un handshake TCP con algo que responde por 10.76.36.40:44818. No identifica necesariamente al PLC.

Un firewall con acción Accept no prueba que exista respuesta de aplicación; hay que revisar bytes enviados/recibidos y el motivo de terminación de la sesión.

Que la prueba EtherNet/IP artesanal no devuelva información tampoco demuestra por sí sola que otro dispositivo esté respondiendo.

Una tabla ARP vacía en el PLC es una señal útil, pero no prueba definitiva: depende de la traducción L2NAT, expiración de ARP y de qué dirección debería observar realmente el PLC.

No se debe reiniciar el IE3400 ni limpiar tablas indiscriminadamente en producción sin autorización OT.


Puertos requeridos

La documentación oficial de Ignition indica que el Allen-Bradley Logix Driver utiliza tráfico saliente TCP 44818. No aparece un puerto UDP separado para negociación normal ni UDP 2222 para este driver. 

Por tanto:

TCP 44818 es el requisito principal para lectura de tags.

Mantendría UDP 44818 porque el ticket lo solicitó expresamente y ya está implementado.

No agregaría UDP 2222 ni otros puertos sin evidencia concreta del fabricante o capturas que muestren que Ignition intenta utilizarlos.


La falta de registro DNS para abrc1-mceovenvacuum017-plc.iiot.medtronic.com tampoco causa la falla actual porque Ignition utiliza directamente 10.76.36.40 y la política ya contiene 10.76.36.40/32.

Qué debes hacer ahora

1. No realizar más cambios en Kubernetes

Desde tu lado ya está comprobado:

El workload correcto es ignition-abrc1l-atlas-stg-0.

Está en livelink-ignition.

TCP 44818 está permitido.

UDP 44818 está permitido.

La política por IP está aplicada.

Los intentos reales coinciden con el ciclo ReconnectWait/Reconnecting.


No agregues nuevas reglas “por si acaso”.

2. Solicitar validación precisa del IE3400

El IE3400 soporta L2NAT uno-a-uno. Cisco indica que el tráfico no traducido puede descartarse por defecto y que deben verificarse la instancia, interfaz, VLAN y estadísticas de traducción. 

El equipo OT/IIoT debe ejecutar durante un intento activo:

show l2nat instance
show l2nat interface
show l2nat statistics
show ip arp
show mac address-table

Deben confirmar específicamente:

Qué dispositivo posee actualmente 10.76.36.40.

La MAC asociada y si todavía corresponde al Secomea SiteManager.

Traducción de destino:


10.76.36.40 -> <IP real del PLC en 172.24.53.0/...>

Traducción de origen:


10.76.226.3 -> 172.24.53.17

Que la instancia L2NAT esté aplicada a la interfaz uplink y VLAN correctas.

Que los contadores de traducción aumenten mientras Ignition reintenta.

Que el PLC tenga IP, máscara y gateway correctos.


La traducción del origen es especialmente importante. Si el IE3400 no traduce 10.76.226.3 hacia 172.24.53.17, el PLC puede no tener una ruta de retorno válida.

3. Pedir detalles completos de la sesión al firewall

No es suficiente con ver Action=Accept. Solicita:

Bytes cliente → servidor.

Bytes servidor → cliente.

SYN, SYN-ACK, FIN o RST observados.

Session end reason.

NAT aplicado.

Interfaz de salida.

Si existe TCP proxy/SYN proxy.


Si hay SYN-ACK pero cero bytes de respuesta después del payload EtherNet/IP, el bloqueo o fallo está después del firewall.

4. Hacer captura simultánea

La prueba definitiva es una captura en ambos lados durante el mismo intento:

Desde el pod o nodo Kubernetes.

En la interfaz externa del IE3400.

En la interfaz/VLAN interna hacia el PLC.


Si el contenedor dispone de tcpdump:

kubectl exec -n livelink-ignition \
  ignition-abrc1l-atlas-stg-0 -c ignition -- \
  sh -c 'command -v tcpdump'

Si está disponible:

kubectl exec -n livelink-ignition \
  ignition-abrc1l-atlas-stg-0 -c ignition -- \
  tcpdump -nn -i any \
  'host 10.76.36.40 and tcp port 44818' \
  -s 0 -c 100

Interpretación:

SYN sale, pero no regresa SYN-ACK: red, firewall o NAT.

Handshake completo, Ignition envía payload, pero no hay respuesta: IE3400, PLC o ruta de retorno.

Handshake y respuesta EtherNet/IP completos: revisar configuración del LogixDriver.

Solo handshake sin payload: revisar Ignition; el driver no está iniciando correctamente la sesión.


Si no hay tcpdump, solicita un contenedor de diagnóstico aprobado o una captura SPAN del equipo OT.

5. Revisar Ignition

Solicita al cliente:

Error exacto anterior a ReconnectWait.

Configuración del dispositivo:

Driver: LogixDriver.

Host: 10.76.36.40.

Port: 44818.

Local Address.

Connection Path.

Timeout.


Exportación de WARN/ERROR filtrada por el nombre del dispositivo.


Para un 1769-L38ERM con Ethernet integrado, una ruta o slot configurados innecesariamente también podrían impedir la sesión aun cuando TCP funcione.

Mensaje para el equipo OT/IIoT

> Hi team,

The Tanzu portion has been validated. The Ignition pod ignition-abrc1l-atlas-stg-0 in namespace livelink-ignition is allowed to reach 10.76.36.40 over TCP and UDP 44818. Antrea logs show the actual TCP reconnect attempts being allowed and forwarded out of the cluster.

The remaining uncertainty is at the IE3400/L2NAT boundary. A TCP handshake succeeds, but we have not confirmed a valid EtherNet/IP response from the PLC. The PLC reportedly does not show an ARP entry for the expected translated source 172.24.53.17.

Could you please validate during an active Ignition retry:

which MAC/device currently owns 10.76.36.40;

the L2NAT destination mapping from 10.76.36.40 to the PLC’s actual 172.24.53.x address;

the source translation from 10.76.226.3 to 172.24.53.17;

that the L2NAT instance is attached to the correct uplink and VLAN;

the L2NAT counters on both directions;

the TCP session bytes and return traffic;

whether any stale ARP/MAC entry remains from the previous Secomea SiteManager IP conflict.


A simultaneous packet capture on the IE3400 outside and inside interfaces would determine exactly where the EtherNet/IP exchange stops.



Mi recomendación es transferir la investigación activa al equipo OT/IIoT y dejar el ticket de Tanzu en espera, no cerrado, hasta que confirmen la traducción y la captura. No hay otro cambio justificado en la política del clúster.







or approver as indicated in the request.

Once the Merge Request is approved, merge the branch into main.

Notes
Make sure the YAML values match the request details exactly.
Reuse existing patterns from similar Antrea policies when possible, these can be found on clusters where this customer has already deployed their application(s).
Validate the namespace, FQDN, port, and protocol before submitting the Merge Request.


2. Look the request:
Preferred contact: Email
Email address: colby.brueggeman@medtronic.com
Time zone: America/Chicago
Preferred time of day to be contacted: Morning (8AM to 12) Afternoon (12 PM to 4 PM)
User's device: 
Location of issue: Tanzu cluster: abrc1l-atlas-stg.tkg.mdtcloud.io
Detailed description of issue:
I need the Tanzu egress policy updated to allow an Ignition gateway script to read data from a PLC. The Ignition gateway is running on the Tanzu cluster at abrc1l-atlas-stg.tkg.mdtcloud.io. A firewall rule has been requested to allow traffic from egress.abrc1l-atlas-stg.tkg.mdtcloud.io to PLC IP address 10.76.36.40 over TCP and UDP port 44818. The Tanzu team needs to update the egress policy to permit this connection.

Here are the details from firewall ticket RITM5683368:
Source IP or Network: 10.76.226.3
Source FQDN: egress.abrc1l-atlas-stg.tkg.mdtcloud.io
Destination IP or Network: 10.76.36.40
Destination FQDN: abrc1-mceovenvacuum017-plc.iiot.medtronic.com
Port/Protocol: TCP
Port Number: 44818
Other Port Protocol: UDP
Connection Purpose: Reading data from TM Braze Oven PLC for SPC testing.

Please route to the same group that handled INC14392366.

3.Look the firewall request:
What do you need to request?
Add Access Rules
 Select an available option
Firewall Policy Change Request
Application Name
Ignition - MECC


Project Summary
Reading data from TM Braze Oven PLC for SPC testing.
Would you like to provide the access rule definitions in an attachment?
No
Define Access Rules
Source IP or Network	Source FQDN	Destination IP or Network	Destination FQDN	Port/Protocol	Port Number	Other Port Protocol	Connection Purpose
10.76.226.3	egress.abrc1l-atlas-stg.tkg.mdtcloud.io	10.76.36.40	abrc1-mceovenvacuum017-plc.iiot.medtronic.com	TCP	44818	UDP	Reading data from TM Braze Oven PLC for SPC testing.


4. We did the changes, but the customer has problems:
It does not seem to be able to connect. 
 Image
I see no logs triggered for this connection:
 Image 2

 5. Check if in this repo all is OK
 6. If all is Ok, give me some commands for test the egress rule (I have access into the cluster)
 7. Isnot, do the changes

GitHub Copilot: Searched for regex `10.76.36.40|44818`, 3 results

Read [](file:///c%3A/Users/mesac2/OneDrive%20-%20Medtronic%20PLC/Documents/Repositories/abrc1l-atlas-stg-main%20%281%29/abrc1l-atlas-stg-main/cluster/customer-components/cluster-scoped/antrea_clusternetworkpolicy_egress-livelink-mecc.yaml)

Read [](file:///c%3A/Users/mesac2/OneDrive%20-%20Medtronic%20PLC/Documents/Repositories/abrc1l-atlas-stg-main%20%281%29/abrc1l-atlas-stg-main/cluster/customer-components/cluster-scoped/antrea_clusternetworkpolicy_egress-livelink.yaml#1-1), lines 1 to 120

Searched for regex `stg\.mecc\.livelink|action: (Drop|Reject)|kind: Tier`, 2 results

Read [](file:///c%3A/Users/mesac2/OneDrive%20-%20Medtronic%20PLC/Documents/Repositories/abrc1l-atlas-stg-main%20%281%29/abrc1l-atlas-stg-main/cluster/customer-components/cluster-scoped/antrea_clusternetworkpolicy_egress-base.yaml)

Read [](file:///c%3A/Users/mesac2/OneDrive%20-%20Medtronic%20PLC/Documents/Repositories/abrc1l-atlas-stg-main%20%281%29/abrc1l-atlas-stg-main/cluster/customer-components/cluster-scoped)

Searched for regex `mecc|hostnames` (`**/cluster/customer-components/{livelink-ignition,v2containers-ignition,livelink-system,v2containers-system}/**`), no results

Searched for regex `v2containers-ignition|TKG_MECC`, 2 results

Using "Replace String in File"


```

```
I found one real problem in the repo and fixed it. I haven't tested the fix on the cluster.

## What I found

**1. The rule only allows the PLC by hostname, but Ignition connects by IP.** The "STG TM Braze Oven Ignition to PLC" rule in antrea_clusternetworkpolicy_egress-livelink-mecc.yaml uses `fqdn: abrc1-mceovenvacuum017-plc.iiot.medtronic.com`. Antrea only matches a hostname rule after it sees the pod look that name up in DNS. The Ignition device (your first screenshot) points straight at `10.76.36.40`, so no lookup happens and the rule never matches. That's why TCP and UDP 44818 were never actually allowed by this rule.

**2. TCP 44818 may already be allowed by another policy.** In antrea_clusternetworkpolicy_egress-livelink.yaml, the "CIDRs for MECC IIoT VLANs" rule allows `livelink-ignition` to reach `10.76.36.0/23` on TCP 44818. If the gateway runs in `livelink-ignition` and TCP still fails, the likely cause is outside the policy:
- the firewall request RITM5683368 isn't done yet, or
- the gateway pod runs in a different namespace (for example `v2containers-ignition`), which neither rule covers.

**3. The second screenshot isn't Antrea's log.** It's the Ignition gateway's own log. Antrea writes its policy logs on each node, so having no entries there doesn't tell you anything about the policy.

## Change made
In the PLC rule, I kept the hostname and added the IP:
```yaml
      - ipBlock:
          cidr: 10.76.36.40/32  # RITM5683368
```

## Commands to test
```powershell
# 1. Confirm which namespace the gateway pod runs in (the rule only covers livelink-ignition)
kubectl get pods -A -o wide | Select-String ignition

# 2. Confirm the policy was applied after Flux reconciles
kubectl get acnp egress-livelink-mecc -o yaml | Select-String -Pattern "10.76.36.40","44818" -Context 0,2
kubectl get acnp egress-livelink-mecc egress-livelink egress-base

# 3. Test TCP from the gateway pod (bash /dev/tcp; use nc if it's available)
kubectl exec -n livelink-ignition <ignition-pod> -- bash -c "timeout 5 bash -c '</dev/tcp/10.76.36.40/44818' ; echo exit=`$?"
#   exit=0 means it connected. A timeout means traffic is being dropped (Antrea or the firewall).

# 4. Antrea traceflow (shows whether Antrea allows or drops the packet)
kubectl get pods -n kube-system -l component=antrea-controller
kubectl exec -n kube-system <antrea-controller-pod> -- antctl traceflow -S livelink-ignition/<ignition-pod> -D 10.76.36.40 -f "tcp,tcp_dst=44818"

# 5. Antrea's per-node policy logs, on the node running the gateway pod
kubectl get pod -n livelink-ignition <ignition-pod> -o wide        # find the node
kubectl get pods -n kube-system -l component=antrea-agent -o wide  # agent on that node
kubectl exec -n kube-system <antrea-agent-pod> -c antrea-agent -- grep 10.76.36.40 /var/log/antrea/networkpolicy/np.log
```

**How to read the results:**
- If traceflow or `np.log` shows the connection **allowed** but the TCP test still times out, the problem is the network firewall. Follow up on RITM5683368, from source `10.76.226.3` to `10.76.36.40`.
- If it shows **dropped** (by `deny-quad-zero`), the pod is probably in a namespace the rule doesn't cover. Add that namespace to `values:` in the rule.

Next steps: create a branch, commit, push, open the Merge Request, and add the reviewer the ticket asks for.

Made changes.

mesac2_mdt: Select-String: command not found

GitHub Copilot: `Select-String` only exists in PowerShell, so your terminal is probably bash (for example a Linux jump host). Here are the same commands using `grep`:

```bash
# 1. Find which namespace the gateway pod runs in
kubectl get pods -A -o wide | grep -i ignition

# 2. Confirm the policy was applied
kubectl get acnp egress-livelink-mecc -o yaml | grep -E -A2 "10.76.36.40|44818"
kubectl get acnp egress-livelink-mecc egress-livelink egress-base

# 3. Test TCP from the gateway pod
kubectl exec -n livelink-ignition <ignition-pod> -- bash -c 'timeout 5 bash -c "</dev/tcp/10.76.36.40/44818"; echo exit=$?'
#   exit=0 means it connected; exit=124 means it timed out (traffic dropped)

# 4. Antrea traceflow
kubectl get pods -n kube-system -l component=antrea-controller
kubectl exec -n kube-system <antrea-controller-pod> -- antctl traceflow -S livelink-ignition/<ignition-pod> -D 10.76.36.40 -f "tcp,tcp_dst=44818"

# 5. Antrea policy logs on the node running the gateway pod
kubectl get pod -n livelink-ignition <ignition-pod> -o wide
kubectl get pods -n kube-system -l component=antrea-agent -o wide
kubectl exec -n kube-system <antrea-agent-pod> -c antrea-agent -- grep 10.76.36.40 /var/log/antrea/networkpolicy/np.log
```

If you are actually in Windows PowerShell, the original commands should work there. In bash, use `grep` instead of `Select-String`.

mesac2_mdt: kubectl get pods -A -o wide | grep -i ignition
livelink-ignition                 ignition-abrc1l-atlas-stg-0                                       1/1     Running            0                 174d    192.168.23.4     abrc1l-atlas-stg-worker-node-pool-64d89-bbhp7-nrlf4   <none>           <none>
livelink-ignition                 network-debug-pod                                                 0/1     Completed          0                 197d    192.168.14.19    abrc1l-atlas-stg-worker-node-pool-64d89-bbhp7-kkln5   <none>           <none>
mesac2@CPC-mesac-4BM7S:~$ kubectl get acnp egress-livelink-mecc -o yaml | grep -E -A2 "10.76.36.40|44818"
    - port: 44818
      protocol: TCP
    - port: 44818
      protocol: UDP
    to:
mesac2@CPC-mesac-4BM7S:~$ kubectl get acnp egress-livelink-mecc egress-livelink egress-base
NAME                   TIER            PRIORITY   DESIRED NODES   CURRENT NODES   AGE
egress-livelink-mecc   atlas-tenants   80         1               1               281d
egress-livelink        atlas-tenants   80         6               6               567d
egress-base            application     100        13              13              567d

GitHub Copilot: The gateway pod is `ignition-abrc1l-atlas-stg-0` in `livelink-ignition`, which is the namespace the PLC rule covers.

The `grep` shows the PLC rule's ports (TCP and UDP 44818) but no `10.76.36.40`. So my IP change isn't on the cluster yet; it needs to be merged and reconciled by Flux. Until then, the PLC rule only allows the hostname.

TCP 44818 should already be allowed by `egress-livelink`, through its rule for `10.76.36.0/23`. Both policies are in tier `atlas-tenants`, which Antrea checks before the `deny-quad-zero` drop in the `application` tier. If TCP still fails, the likely cause is the network firewall rather than Antrea.

Run these next to confirm:

```bash
# 1. Test TCP from the gateway pod
kubectl exec -n livelink-ignition ignition-abrc1l-atlas-stg-0 -- bash -c 'timeout 5 bash -c "</dev/tcp/10.76.36.40/44818"; echo exit=$?'

# 2. Antrea traceflow (shows whether Antrea allows or drops the packet)
CTRL=$(kubectl get pods -n kube-system -l component=antrea-controller -o name | head -1)
kubectl exec -n kube-system $CTRL -- antctl traceflow -S livelink-ignition/ignition-abrc1l-atlas-stg-0 -D 10.76.36.40 -f "tcp,tcp_dst=44818"

# 3. Antrea policy log on the pod's node (abrc1l-atlas-stg-worker-node-pool-64d89-bbhp7-nrlf4)
AGENT=$(kubectl get pods -n kube-system -l component=antrea-agent -o wide | grep nrlf4 | awk '{print $1}')
kubectl exec -n kube-system $AGENT -c antrea-agent -- grep 10.76.36.40 /var/log/antrea/networkpolicy/np.log | tail -20
```

If `bash` isn't available in the pod, try `sh` in the first command instead.

**How to read the results:**

| Result | Meaning | Action |
|---|---|---|
| `exit=0` | TCP works | The problem is in Ignition's device setup or the PLC itself |
| `exit=124` and traceflow/`np.log` shows **Allow** (`egress-livelink` or `egress-livelink-mecc`) | Antrea lets it through; the network firewall blocks it | Follow up on RITM5683368 (from `10.76.226.3` to `10.76.36.40`, TCP and UDP 44818) |
| `exit=124` and it shows **Drop** (`egress-base` / `deny-quad-zero`) | Antrea blocks it | Merge the `ipBlock` change and check again |

Merge the `ipBlock` change either way: it's the only rule that allows UDP 44818 to the PLC by IP.

mesac2_mdt:  kubectl exec -n livelink-ignition ignition-abrc1l-atlas-stg-0 -- bash -c 'timeout 5 bash -c "</dev/tcp/10.76.36.40/44818"; echo exit=$?'
Defaulted container "ignition" out of: ignition, seed-volume (init), dynatrace-operator (init)
ERROR: ld.so: object '/opt/dynatrace/oneagent-paas/agent/lib64/liboneagentproc.so' from LD_PRELOAD cannot be preloaded (cannot open shared object file): ignored.
ERROR: ld.so: object '/opt/dynatrace/oneagent-paas/agent/lib64/liboneagentproc.so' from /etc/ld.so.preload cannot be preloaded (cannot open shared object file): ignored.
ERROR: ld.so: object '/opt/dynatrace/oneagent-paas/agent/lib64/liboneagentproc.so' from LD_PRELOAD cannot be preloaded (cannot open shared object file): ignored.
ERROR: ld.so: object '/opt/dynatrace/oneagent-paas/agent/lib64/liboneagentproc.so' from /etc/ld.so.preload cannot be preloaded (cannot open shared object file): ignored.
ERROR: ld.so: object '/opt/dynatrace/oneagent-paas/agent/lib64/liboneagentproc.so' from LD_PRELOAD cannot be preloaded (cannot open shared object file): ignored.
ERROR: ld.so: object '/opt/dynatrace/oneagent-paas/agent/lib64/liboneagentproc.so' from /etc/ld.so.preload cannot be preloaded (cannot open shared object file): ignored.
exit=0
mesac2@CPC-mesac-4BM7S:~$ CTRL=$(kubectl get pods -n kube-system -l component=antrea-controller -o name | head -1)
mesac2@CPC-mesac-4BM7S:~$ kubectl exec -n kube-system $CTRL -- antctl traceflow -S livelink-ignition/ignition-abrc1l-atlas-stg-0 -D 10.76.36.40 -f "tcp,tcp_dst=44818"
name: livelink-ignition-ignition-abrc1l-atlas-stg-0-to-10.76.36.40-lw76v4st
phase: Succeeded
source: livelink-ignition/ignition-abrc1l-atlas-stg-0
destination: 10.76.36.40
results:
- node: abrc1l-atlas-stg-worker-node-pool-64d89-bbhp7-nrlf4
  timestamp: 1792607719
  observations:
  - component: SpoofGuard
    action: Forwarded
    srcPodIP: 192.168.23.4
  - component: NetworkPolicy
    componentInfo: EgressRule
    action: Forwarded
    networkPolicy: AntreaClusterNetworkPolicy:egress-livelink
    networkPolicyRule: CIDRs for MECC IIoT VLANs
  - component: Forwarding
    componentInfo: Output
    action: ForwardedOutOfOverlay
mesac2@CPC-mesac-4BM7S:~$ AGENT=$(kubectl get pods -n kube-system -l component=antrea-agent -o wide | grep nrlf4 | awk '{print $1}')
mesac2@CPC-mesac-4BM7S:~$ kubectl exec -n kube-system $AGENT -c antrea-agent -- grep 10.76.36.40 /var/log/antrea/networkpolicy/np.log | tail -20
2026/10/21 18:30:20.372033 AntreaPolicyEgressRule AntreaClusterNetworkPolicy:egress-livelink CIDRs for MECC IIoT VLANs Egress Allow 14398 livelink-ignition/ignition-abrc1l-atlas-stg-0 192.168.23.4 57226 10.76.36.40 44818 TCP 60 <nil>
2026/10/21 18:30:38.471956 AntreaPolicyEgressRule AntreaClusterNetworkPolicy:egress-livelink CIDRs for MECC IIoT VLANs Egress Allow 14398 livelink-ignition/ignition-abrc1l-atlas-stg-0 192.168.23.4 47122 10.76.36.40 44818 TCP 60 <nil>
2026/10/21 18:30:56.571994 AntreaPolicyEgressRule AntreaClusterNetworkPolicy:egress-livelink CIDRs for MECC IIoT VLANs Egress Allow 14398 livelink-ignition/ignition-abrc1l-atlas-stg-0 192.168.23.4 46594 10.76.36.40 44818 TCP 60 <nil>
2026/10/21 18:31:14.671593 AntreaPolicyEgressRule AntreaClusterNetworkPolicy:egress-livelink CIDRs for MECC IIoT VLANs Egress Allow 14398 livelink-ignition/ignition-abrc1l-atlas-stg-0 192.168.23.4 42304 10.76.36.40 44818 TCP 60 <nil>
2026/10/21 18:31:32.771651 AntreaPolicyEgressRule AntreaClusterNetworkPolicy:egress-livelink CIDRs for MECC IIoT VLANs Egress Allow 14398 livelink-ignition/ignition-abrc1l-atlas-stg-0 192.168.23.4 51900 10.76.36.40 44818 TCP 60 <nil>
2026/10/21 18:31:50.872343 AntreaPolicyEgressRule AntreaClusterNetworkPolicy:egress-livelink CIDRs for MECC IIoT VLANs Egress Allow 14398 livelink-ignition/ignition-abrc1l-atlas-stg-0 192.168.23.4 57574 10.76.36.40 44818 TCP 60 <nil>
2026/10/21 18:32:08.971930 AntreaPolicyEgressRule AntreaClusterNetworkPolicy:egress-livelink CIDRs for MECC IIoT VLANs Egress Allow 14398 livelink-ignition/ignition-abrc1l-atlas-stg-0 192.168.23.4 55948 10.76.36.40 44818 TCP 60 <nil>
2026/10/21 18:32:27.072100 AntreaPolicyEgressRule AntreaClusterNetworkPolicy:egress-livelink CIDRs for MECC IIoT VLANs Egress Allow 14398 livelink-ignition/ignition-abrc1l-atlas-stg-0 192.168.23.4 37800 10.76.36.40 44818 TCP 60 <nil>
2026/10/21 18:32:45.172071 AntreaPolicyEgressRule AntreaClusterNetworkPolicy:egress-livelink CIDRs for MECC IIoT VLANs Egress Allow 14398 livelink-ignition/ignition-abrc1l-atlas-stg-0 192.168.23.4 53486 10.76.36.40 44818 TCP 60 <nil>
2026/10/21 18:33:03.272236 AntreaPolicyEgressRule AntreaClusterNetworkPolicy:egress-livelink CIDRs for MECC IIoT VLANs Egress Allow 14398 livelink-ignition/ignition-abrc1l-atlas-stg-0 192.168.23.4 37040 10.76.36.40 44818 TCP 60 <nil>
2026/10/21 18:33:21.371951 AntreaPolicyEgressRule AntreaClusterNetworkPolicy:egress-livelink CIDRs for MECC IIoT VLANs Egress Allow 14398 livelink-ignition/ignition-abrc1l-atlas-stg-0 192.168.23.4 50334 10.76.36.40 44818 TCP 60 <nil>
2026/10/21 18:33:39.471501 AntreaPolicyEgressRule AntreaClusterNetworkPolicy:egress-livelink CIDRs for MECC IIoT VLANs Egress Allow 14398 livelink-ignition/ignition-abrc1l-atlas-stg-0 192.168.23.4 33830 10.76.36.40 44818 TCP 60 <nil>
2026/10/21 18:33:57.571884 AntreaPolicyEgressRule AntreaClusterNetworkPolicy:egress-livelink CIDRs for MECC IIoT VLANs Egress Allow 14398 livelink-ignition/ignition-abrc1l-atlas-stg-0 192.168.23.4 48638 10.76.36.40 44818 TCP 60 <nil>
2026/10/21 18:34:15.671868 AntreaPolicyEgressRule AntreaClusterNetworkPolicy:egress-livelink CIDRs for MECC IIoT VLANs Egress Allow 14398 livelink-ignition/ignition-abrc1l-atlas-stg-0 192.168.23.4 60878 10.76.36.40 44818 TCP 60 <nil>
2026/10/21 18:34:33.771373 AntreaPolicyEgressRule AntreaClusterNetworkPolicy:egress-livelink CIDRs for MECC IIoT VLANs Egress Allow 14398 livelink-ignition/ignition-abrc1l-atlas-stg-0 192.168.23.4 46046 10.76.36.40 44818 TCP 60 <nil>
2026/10/21 18:34:48.648420 AntreaPolicyEgressRule AntreaClusterNetworkPolicy:egress-livelink CIDRs for MECC IIoT VLANs Egress Allow 14398 livelink-ignition/ignition-abrc1l-atlas-stg-0 192.168.23.4 43634 10.76.36.40 44818 TCP 60 <nil>
2026/10/21 18:34:51.871892 AntreaPolicyEgressRule AntreaClusterNetworkPolicy:egress-livelink CIDRs for MECC IIoT VLANs Egress Allow 14398 livelink-ignition/ignition-abrc1l-atlas-stg-0 192.168.23.4 37244 10.76.36.40 44818 TCP 60 <nil>
2026/10/21 18:35:09.972530 AntreaPolicyEgressRule AntreaClusterNetworkPolicy:egress-livelink CIDRs for MECC IIoT VLANs Egress Allow 14398 livelink-ignition/ignition-abrc1l-atlas-stg-0 192.168.23.4 60926 10.76.36.40 44818 TCP 60 <nil>
2026/10/21 18:35:19.482330 AntreaPolicyEgressRule AntreaClusterNetworkPolicy:egress-livelink CIDRs for MECC IIoT VLANs Egress Allow 14398 livelink-ignition/ignition-abrc1l-atlas-stg-0 192.168.23.4 51227 10.76.36.40 44818 TCP 40 <nil>
2026/10/21 18:35:28.072119 AntreaPolicyEgressRule AntreaClusterNetworkPolicy:egress-livelink CIDRs for MECC IIoT VLANs Egress Allow 14398 livelink-ignition/ignition-abrc1l-atlas-stg-0 192.168.23.4 38858 10.76.36.40 44818 TCP 60 <nil>
mesac2@CPC-mesac-4BM7S:~$

GitHub Copilot: The network path is fine. Antrea and the firewall both let the gateway reach the PLC on TCP 44818, so the failure is in Ignition's device setup or the PLC itself.

## What the results show

| Check | Result |
|---|---|
| TCP test from the gateway pod | `exit=0`: the connection to `10.76.36.40:44818` opened |
| Traceflow | **Forwarded**, by `egress-livelink` / "CIDRs for MECC IIoT VLANs" |
| `np.log` | The gateway pod connects every ~18 seconds and Antrea **allows** every attempt |

The `ld.so` / Dynatrace errors are harmless and unrelated.

Because the connection itself succeeds, the firewall request RITM5683368 is already working too.

A new attempt every ~18 seconds suggests Ignition connects, then fails during the device handshake and keeps retrying. That fits a driver setup problem.

## About the repo change
The `ipBlock` I added isn't needed for TCP, because `egress-livelink` already allows that address range. It only adds UDP 44818 to the PLC. Ignition's Allen-Bradley drivers use UDP 44818 only for device discovery, not for reading tags. You can either:
- keep it, so the ticket's request (TCP and UDP) is fully covered, or
- discard it, since it doesn't affect this issue.

## What to tell the customer
The network is open and their gateway is reaching the PLC on TCP 44818 (logged every ~18 seconds). They should check the Ignition side:

1. **Device status:** look at Config → OPC UA → Device Connections, or Status → Connections → Devices, and note the exact state (Connecting, Faulted, and so on).
2. **Driver type:** the device must use the driver that matches the PLC (Logix vs. Micro800 vs. Legacy Allen-Bradley). The wrong driver produces exactly this pattern: the connection opens, then the handshake fails.
3. **Slot number / Connection Path:** slot `0` only works if the processor is in slot 0. For a ControlLogix in a chassis, set the correct slot or path.
4. **Logs:** Ignition's log messages don't include the IP, which is why the filter on `10.76.36.40` found nothing. They should filter by the device name `Braze Oven MCEOVENVACUUM017` or by driver logger names such as `LogixDriver`. Raising those loggers to DEBUG will show the exact error.
5. **PLC side:** check that the PLC isn't out of CIP connections and that nothing on the plant network blocks it.

mesac2_mdt: Reply of customer:
Where do you see the connection being logged every 18 seconds? 
The status is "ReconnectWait" and "Reconnecting"
The driver is the LogixDriver, which I believe is appropriate for this Allen Bradley 1769-L38ERM.
I believe the slot is always slot 0 for a compact logix. 
Debug logs produce a ton of data, I don't see anything immediately, but can check back later today. 
Wouldn't plant-network restrictions be related to firewall or network connectivity? What are you referring to with the last bullet? 

GitHub Copilot: Here are answers you can send back to the customer:

**1. Where the ~18-second connections are logged.** They come from the Kubernetes network policy logs, not from Ignition. Antrea, the cluster's network layer, records every new outbound connection. The gateway pod `ignition-abrc1l-atlas-stg-0` opens a new TCP connection to `10.76.36.40:44818` roughly every 18 seconds, and every one is **allowed**. That timing lines up with your `ReconnectWait` → `Reconnecting` loop.

We also opened a test TCP connection from inside the gateway pod to `10.76.36.40:44818`, and it connected. So the cluster and the firewall are not blocking the PLC.

**2. LogixDriver and slot 0.** You're right on both. LogixDriver is correct for a 1769-L38ERM, and slot 0 is correct for CompactLogix.

**3. The "plant-network" bullet.** That was poorly worded on my part, and you're right that it's connectivity. Since the TCP connection succeeds, network restrictions aren't the cause. What I should have said is: check the **PLC itself**. For example:
- whether it has run out of CIP connections because other clients (HMIs, other gateways) are using them
- whether something on the controller is rejecting the session

**4. Debug logs.** Try filtering the gateway logs by the device name `Braze Oven MCEOVENVACUUM017`, and look at WARN/ERROR entries first. The driver's messages don't include the IP, which is why searching for `10.76.36.40` returned nothing.

---

**For you, before replying:** this command checks whether the PLC actually answers EtherNet/IP requests, not just accepts the TCP connection. It sends a basic identity request from the gateway pod:

```bash
kubectl exec -n livelink-ignition ignition-abrc1l-atlas-stg-0 -- bash -c 'exec 3<>/dev/tcp/10.76.36.40/44818; printf "\x63\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00" >&3; timeout 3 head -c 200 <&3 | strings' 2>/dev/null
```

- **The reply includes `1769-L38ERM`:** the PLC responds to the gateway end to end. The problem is in the driver session or the PLC configuration, so it belongs to the customer or controls team.
- **No output:** something accepts the TCP connection but doesn't respond like the PLC. That could be a device or firewall in between, so it's worth checking with the network team under RITM5683368.

Include the result in your reply; it backs up the conclusion.

mesac2_mdt: When you say "the gateway is successfully reaching the PLC" what exactly is the gateway/where did that test (assuming tcptraceroute) get run from?

GitHub Copilot: "The gateway" means the Ignition gateway itself, the pod `ignition-abrc1l-atlas-stg-0` in namespace `livelink-ignition`. That's the Ignition gateway the customer uses (TKG_MECC_DEV, `stg.mecc.livelink.medtronic.com`). The test wasn't a tcptraceroute. All checks were run from inside that pod or about its traffic:

| Test | Where it ran | What it proves |
|---|---|---|
| `bash -c '</dev/tcp/10.76.36.40/44818'` | Inside the Ignition container, via `kubectl exec` | A TCP connection to `10.76.36.40:44818` opened (`exit=0`) |
| Antrea traceflow | Simulated packet from that pod | The cluster's network policy forwards the traffic out of the cluster (rule `egress-livelink` / "CIDRs for MECC IIoT VLANs") |
| Antrea `np.log` | The cluster node running the pod (`...-nrlf4`) | Ignition's own connection attempts to `10.76.36.40:44818` happen about every 18 seconds and are allowed |

Leaving the cluster, the traffic appears with the source IP from the firewall ticket (`10.76.226.3`, `egress.abrc1l-atlas-stg.tkg.mdtcloud.io`). I haven't confirmed that on the cluster.

**A correction to my earlier wording:** "successfully reaching the PLC" overstated it. The test proves only that *something* at `10.76.36.40:44818` accepted the TCP connection. A firewall or other device in between can accept a connection on the PLC's behalf. The test doesn't prove the PLC itself answered.

The EtherNet/IP identity request from my previous message tells these apart. Run it from the same pod:
- **Reply includes `1769-L38ERM`:** the PLC itself is answering, so the issue is on the Ignition driver or PLC side.
- **No reply:** something in between is accepting the connection and then dropping the data. The network or firewall team should check that under RITM5683368.

Suggested reply to the customer:
> The test was run from inside the Ignition gateway container (`ignition-abrc1l-atlas-stg-0`) on the Tanzu cluster, using a direct TCP connect to 10.76.36.40:44818, which succeeded. The cluster's network policy logs also show your gateway's own attempts leaving the cluster about every 18 seconds, and all are allowed. Traffic leaves the cluster from 10.76.226.3. We are confirming whether the PLC itself responds at the EtherNet/IP level, or whether a device in between is accepting the connection.

mesac2_mdt: Okay great, and did we confirm they tried connecting from a pod in the livelink-ignition namespace on abrc1l-atlas-stg when they got the error they're still reporting now?

GitHub Copilot: No, we haven't confirmed it directly. The evidence points strongly to that pod, but one detail suggests it might not be the same gateway.

## What points to that pod
- It's the only Ignition gateway running in the cluster. The other pod in `livelink-ignition`, `network-debug-pod`, is a completed debug pod.
- The policy log shows this pod connecting to `10.76.36.40:44818` about every 18 seconds, today. That fits the `ReconnectWait` → `Reconnecting` loop the customer reports.
- No other workload in the cluster showed traffic to that IP.

## What doesn't fit
- The customer's screenshot shows a gateway named **TKG_MECC_DEV**, opened at **`stg.mecc.livelink.medtronic.com`**.
- The repo's first rule ("Egress rule for MECC V2") lets `livelink-ignition` connect *out* to `stg.mecc.livelink.medtronic.com:443`. That suggests the URL belongs to a separate service the pod talks to, not to the pod itself. It could also be a hairpin back into the cluster, so this needs checking.

## Commands to confirm
```bash
# 1. Gateway name configured in the pod (should be TKG_MECC_DEV)
kubectl exec -n livelink-ignition ignition-abrc1l-atlas-stg-0 -c ignition -- sh -c 'grep -ri "gateway.name\|systemName" /usr/local/bin/ignition/data/gateway.xml* /usr/local/bin/ignition/data/*.conf 2>/dev/null | head'
kubectl get pod -n livelink-ignition ignition-abrc1l-atlas-stg-0 -o yaml | grep -i -A1 "GATEWAY_SYSTEM_NAME\|gateway.name"

# 2. Which hostnames route to this pod
kubectl get httproute,ingress,gateway -n livelink-ignition -o yaml | grep -iE "hostname|host:|mecc"

# 3. Where the customer's URL resolves vs. the cluster's ingress
nslookup stg.mecc.livelink.medtronic.com
kubectl get svc -A | grep -iE "LoadBalancer"

# 4. Whether this gateway has the Braze Oven device configured
kubectl exec -n livelink-ignition ignition-abrc1l-atlas-stg-0 -c ignition -- sh -c 'grep -ril "MCEOVENVACUUM017\|10.76.36.40" /usr/local/bin/ignition/data 2>/dev/null | head'
```

**How to read the results:**
- **The name is `TKG_MECC_DEV`, the URL resolves to a load balancer in this cluster, and the device is found in the pod:** it's confirmed. The customer's error comes from this pod, and our tests apply to it.
- **Any of those don't match:** the customer's gateway runs somewhere else, for example on EKS, or on another cluster that sends traffic out through this one. In that case our tests don't cover their gateway, and the attempts every 18 seconds belong to a different gateway.

Until these checks come back, tell the customer it's very likely but not yet verified.

mesac2_mdt: kubectl exec -n livelink-ignition ignition-abrc1l-atlas-stg-0 -c ignition -- sh -c 'grep -ri "gateway.name\|systemName" /usr/local/bin/ignition/data/gateway.xml* /usr/local/bin/ignition/data/*.conf 2>/dev/null | head'
ERROR: ld.so: object '/opt/dynatrace/oneagent-paas/agent/lib64/liboneagentproc.so' from LD_PRELOAD cannot be preloaded (cannot open shared object file): ignored.
ERROR: ld.so: object '/opt/dynatrace/oneagent-paas/agent/lib64/liboneagentproc.so' from /etc/ld.so.preload cannot be preloaded (cannot open shared object file): ignored.
ERROR: ld.so: object '/opt/dynatrace/oneagent-paas/agent/lib64/liboneagentproc.so' from LD_PRELOAD cannot be preloaded (cannot open shared object file): ignored.
ERROR: ld.so: object '/opt/dynatrace/oneagent-paas/agent/lib64/liboneagentproc.so' from /etc/ld.so.preload cannot be preloaded (cannot open shared object file): ignored.
mesac2@CPC-mesac-4BM7S:~$ kubectl get pod -n livelink-ignition ignition-abrc1l-atlas-stg-0 -o yaml | grep -i -A1 "GATEWAY_SYSTEM_NAME\|gateway.name"
mesac2@CPC-mesac-4BM7S:~$ kubectl get httproute,ingress,gateway -n livelink-ignition -o yaml | grep -iE "hostname|host:|mecc"
    hostnames:
    - stg.mecc.livelink.medtronic.com
    - ignition.mecc.livelink.medtronic.com
    hostnames:
mesac2@CPC-mesac-4BM7S:~$ nslookup stg.mecc.livelink.medtronic.com
Server:         10.255.255.254
Address:        10.255.255.254#53

stg.mecc.livelink.medtronic.com canonical name = ignition.abrc1l-atlas-stg.tkg.mdtcloud.io.
Name:   ignition.abrc1l-atlas-stg.tkg.mdtcloud.io
Address: 10.76.225.131

mesac2@CPC-mesac-4BM7S:~$ kubectl get svc -A | grep -iE "LoadBalancer"
ami-system           envoy-ami-gateway                                  LoadBalancer   10.99.36.77      10.76.225.145   443:31370/TCP                                                                                           242d
atlas-system         envoy-cnpg-gateway                                 LoadBalancer   10.106.128.182   10.76.225.136   5432:30826/TCP                                                                                          413d
atlas-testing        envoy-test-letsencrypt-gateway                     LoadBalancer   10.100.89.201    10.76.225.139   443:32548/TCP                                                                                           370d
contour-system       contour-envoy                                      LoadBalancer   10.105.54.16     10.76.225.135   80:31721/TCP,443:32358/TCP                                                                              634d
livelink-system      envoy-livelink-gateway                             LoadBalancer   10.104.218.62    10.76.225.131   80:30613/TCP,1883:32265/TCP,8883:30512/TCP,8060:31603/TCP,443:32127/TCP,5432:31350/TCP,5433:31043/TCP   453d
traefik-gateway      traefik-gateway                                    LoadBalancer   10.99.12.202     10.76.225.143   5432:31941/TCP                                                                                          324d
mesac2@CPC-mesac-4BM7S:~$ kubectl exec -n livelink-ignition ignition-abrc1l-atlas-stg-0 -c ignition -- sh -c 'grep -ril "MCEOVENVACUUM017\|10.76.36.40" /usr/local/bin/ignition/data 2>/dev/null | head'
ERROR: ld.so: object '/opt/dynatrace/oneagent-paas/agent/lib64/liboneagentproc.so' from LD_PRELOAD cannot be preloaded (cannot open shared object file): ignored.
ERROR: ld.so: object '/opt/dynatrace/oneagent-paas/agent/lib64/liboneagentproc.so' from /etc/ld.so.preload cannot be preloaded (cannot open shared object file): ignored.
ERROR: ld.so: object '/opt/dynatrace/oneagent-paas/agent/lib64/liboneagentproc.so' from LD_PRELOAD cannot be preloaded (cannot open shared object file): ignored.
ERROR: ld.so: object '/opt/dynatrace/oneagent-paas/agent/lib64/liboneagentproc.so' from /etc/ld.so.preload cannot be preloaded (cannot open shared object file): ignored.


GitHub Copilot: Mostly confirmed. The customer's URL leads to this cluster's `livelink-ignition` namespace. Two checks found nothing, and I think the paths I gave you were wrong, not that the gateway is somewhere else.

## Confirmed
- `stg.mecc.livelink.medtronic.com` is an alias (CNAME) for `ignition.abrc1l-atlas-stg.tkg.mdtcloud.io`, which resolves to `10.76.225.131`.
- `10.76.225.131` is the `envoy-livelink-gateway` LoadBalancer in **this cluster**.
- An HTTPRoute in `livelink-ignition` serves the hostname `stg.mecc.livelink.medtronic.com`.
- `ignition-abrc1l-atlas-stg-0` is the only Ignition pod running. The policy log shows it trying to connect to `10.76.36.40:44818` about every 18 seconds, which matches the customer's `ReconnectWait` → `Reconnecting` loop.

## Why the gateway name and device checks found nothing
I guessed the Ignition install path (`/usr/local/bin/ignition/data`). Newer images often use `/usr/local/ignition`. Also, some Ignition versions keep the gateway config in a binary internal database rather than text files. So the empty results don't show the device is missing. The pod's own connection attempts to `10.76.36.40` already show the device is configured there.

## Two checks to finish it
```bash
# 1. Which service the HTTPRoute sends traffic to (should be the ignition pod's service)
kubectl get httproute -n livelink-ignition -o yaml | grep -A6 -i "backendRefs"
kubectl get svc -n livelink-ignition -o wide

# 2. Find the real data directory, then search it for the device and the gateway name
kubectl exec -n livelink-ignition ignition-abrc1l-atlas-stg-0 -c ignition -- sh -c 'for d in /usr/local/ignition/data /usr/local/bin/ignition/data /var/lib/ignition/data; do [ -d "$d" ] && echo "DATA=$d" && grep -rIl -a "MCEOVENVACUUM017\|10.76.36.40\|TKG_MECC_DEV" "$d" 2>/dev/null | head; done' 2>/dev/null
```
The `2>/dev/null` at the end also hides the Dynatrace `ld.so` warnings, which are harmless.

**What to expect:** the route should point to the service in front of `ignition-abrc1l-atlas-stg-0`, and the second command should list at least one file. If so, it's confirmed: the customer is configuring this gateway, and the failing device connections come from this pod in `livelink-ignition`.

## Suggested reply to the customer
> Confirmed: `stg.mecc.livelink.medtronic.com` routes to the Ignition gateway running in the `livelink-ignition` namespace on abrc1l-atlas-stg. The connectivity tests ran from inside that same gateway container, and the cluster's network logs show that gateway trying to connect to 10.76.36.40:44818 about every 18 seconds, matching your Reconnecting status. All of those attempts are allowed, and a direct TCP connection to the PLC port succeeds.

The EtherNet/IP identity test I gave you two replies ago is still worth running. It's the next step to show whether the PLC itself answers or a device in between accepts the connection.

mesac2_mdt: My leader help is:
I would also ask them to research whether Ignition uses a separate port for negotiating connections, some apps will do that. Also worth checking if we can pass both UDP traffic from pods in that namespace to the PLC on port 44818
I also don't see an A record for abrc1-mceovenvacuum017-plc.iiot.medtronic.com so not sure how we got 10.76.36.40 - did the customer give that IP?
 
Yes, the customer provides us with the IP address
 

 With no DNS record its possible antrea isn't allowing traffic to that host since our egress rule gives that FQDN as the destination rather than an IP - but you said the connection worked so perhaps that's not an issue
 
I can get a tcptraceroute to work for that IP which is interesting, but UDP is blocked somewhere (likely the IIOT firewall)
 
Not sure if they need UDP, again I'd ask for all ports and protocols they know Ignition to use both for connection negotiation/establishment (control sequence) and for data streaming 
 
If they list anything besides TCP/44818 then we need to make adjustments
 
 Also let them know there is no A record for abrc1-mceovenvacuum017-plc.iiot.medtronic.com at present
 

 So, your work is give me a message for the customer, or give me my work according with the leader reply

GitHub Copilot: The leader's points line up with what we found. Here is a message for the customer, followed by your own work.

## Message for the customer

> Hi Colby,
>
> Here's where we are on the egress for the TM Braze Oven PLC (RITM5683368):
>
> 1. **The cluster allows the connection.** The Ignition gateway behind `stg.mecc.livelink.medtronic.com` runs in the `livelink-ignition` namespace on abrc1l-atlas-stg. We tested from inside that gateway container, and a TCP connection to `10.76.36.40:44818` succeeds. The cluster's network logs also show your gateway trying to connect about every 18 seconds, which matches the ReconnectWait/Reconnecting status, and all of those attempts are allowed.
> 2. **There is no DNS record for the PLC hostname.** `abrc1-mceovenvacuum017-plc.iiot.medtronic.com` currently has no A record. We are using the IP you provided (10.76.36.40). If you want to use the hostname in Ignition, a DNS record will need to be requested.
> 3. **UDP appears to be blocked between the cluster and the PLC.** TCP 44818 reaches the PLC, but UDP 44818 seems to be blocked somewhere on the path, most likely at the IIoT firewall.
> 4. **We need the full list of ports and protocols.** Some applications use a separate port to set up the connection and another to send data. Could you confirm every port and protocol the Ignition LogixDriver needs to talk to the 1769-L38ERM, for both connection setup and data reads? If anything beyond TCP 44818 is needed (for example UDP 44818 or UDP 2222), we'll update the cluster policy and request the matching firewall changes.
>
> Also, if you can share the WARN/ERROR entries from the debug logs for the device `Braze Oven MCEOVENVACUUM017`, that would help pinpoint where the session fails.
>
> Thanks!

## Your work

1. **Merge the `ipBlock` change already in the repo.** The leader is right: with no DNS record, the hostname rule in `egress-livelink-mecc` never matches anything. TCP works today only because of the `10.76.36.0/23` range in `egress-livelink`. The `10.76.36.40/32` entry I added makes the PLC rule match by IP, and it is the only rule that lets pods in `livelink-ignition` send **UDP 44818** to the PLC. Create a branch, commit, open the Merge Request, and add the approver.
2. **After Flux applies it, confirm the cluster side lets UDP through:**
   ```bash
   kubectl get acnp egress-livelink-mecc -o yaml | grep -A1 "10.76.36.40"
   CTRL=$(kubectl get pods -n kube-system -l component=antrea-controller -o name | head -1)
   kubectl exec -n kube-system $CTRL -- antctl traceflow -S livelink-ignition/ignition-abrc1l-atlas-stg-0 -D 10.76.36.40 -f "udp,udp_dst=44818"
   ```
   If the traceflow shows `Forwarded` by `egress-livelink-mecc`, any remaining UDP block is outside the cluster (the IIoT firewall).
3. **Wait for the customer's port list.**
   - **Only TCP 44818:** nothing more on our side; the remaining issue is in Ignition or the PLC.
   - **UDP 44818 needed:** follow up on RITM5683368 with the network or IIoT firewall team for UDP from `10.76.226.3` to `10.76.36.40`.
   - **Other ports (for example UDP 2222):** add them to the PLC rule in the repo and request the matching firewall rule.

For background: Ignition's LogixDriver normally reads tags over **TCP 44818** only. UDP 44818 is used for device discovery, and UDP 2222 for a different kind of I/O traffic that this driver usually doesn't need. The customer should still confirm the list, as the leader asked.

mesac2_mdt: I did the chnges
Commands:
 kubectl get acnp egress-livelink-mecc -o yaml | grep -A1 "10.76.36.40"
        cidr: 10.76.36.40/32
  - action: Allow
mesac2@CPC-mesac-4BM7S:~$ CTRL=$(kubectl get pods -n kube-system -l component=antrea-controller -o name | head -1)
mesac2@CPC-mesac-4BM7S:~$ kubectl exec -n kube-system $CTRL -- antctl traceflow -S livelink-ignition/ignition-abrc1l-atlas-stg-0 -D 10.76.36.40 -f "udp,udp_dst=44818"
name: livelink-ignition-ignition-abrc1l-atlas-stg-0-to-10.76.36.40-gc9s85ml
phase: Succeeded
source: livelink-ignition/ignition-abrc1l-atlas-stg-0
destination: 10.76.36.40
results:
- node: abrc1l-atlas-stg-worker-node-pool-64d89-bbhp7-nrlf4
  timestamp: 1792790842
  observations:
  - component: SpoofGuard
    action: Forwarded
    srcPodIP: 192.168.23.4
  - component: NetworkPolicy
    componentInfo: EgressRule
    action: Forwarded
    networkPolicy: AntreaClusterNetworkPolicy:egress-livelink-mecc
    networkPolicyRule: STG TM Braze Oven Ignition to PLC
  - component: Forwarding
    componentInfo: Output
    action: ForwardedOutOfOverlay
mesac2@CPC-mesac-4BM7S:~$

Customer reply:
Just got back from the plant floor. Tried my change, but no luck. There was a Secomea SiteManager switch in the equipment which was conflicting with the IE3400's IP. I have unplugged the SiteManager, but still do not see a successful connection in Ignition. 
 
When I look at the information on the PLC, I see the ARP table is non populated with any entry for the local IP of the IE3400 of 172.24.53.17. I believe this would suggest that the message from the Ignition server is not making it to the PLC. Is this an accurate assessment? If yes, can it be confirmed that the IE3400 is passing along data as expected? 

GitHub Copilot: The cluster side is finished. UDP 44818 is now allowed by `egress-livelink-mecc` / "STG TM Braze Oven Ignition to PLC", and TCP 44818 was already allowed by `egress-livelink`. The customer's new information changes the picture, though.

## What the customer's finding means

**Their assessment is very likely right.** I'm assuming the IE3400 does NAT, translating `10.76.36.40` to the PLC's address in `172.24.53.x`; the network team needs to confirm that. If so, the PLC should have an ARP entry for `172.24.53.17` as soon as any request is forwarded to it. An empty ARP table means the PLC never received anything through the IE3400.

**This also explains our earlier result.** Our TCP test on Oct 21 ran while the Secomea SiteManager was using the IE3400's IP. It's likely the **SiteManager**, not the PLC, accepted that connection. That would explain why TCP connected but Ignition never established a session.

**Who can check the IE3400.** It's on the plant network, outside the cluster. The cluster team can't see its NAT rules or forwarding. That belongs to the IIoT/OT network team.

## Re-test now that the SiteManager is unplugged

```bash
# 1. Does anything still answer on TCP 44818?
kubectl exec -n livelink-ignition ignition-abrc1l-atlas-stg-0 -c ignition -- bash -c 'timeout 5 bash -c "</dev/tcp/10.76.36.40/44818"; echo exit=$?' 2>/dev/null

# 2. Does the PLC itself answer? (EtherNet/IP identity request)
kubectl exec -n livelink-ignition ignition-abrc1l-atlas-stg-0 -c ignition -- bash -c 'exec 3<>/dev/tcp/10.76.36.40/44818; printf "\x63\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00" >&3; timeout 3 head -c 200 <&3 | strings' 2>/dev/null

# 3. Did the attempts still leave the cluster as allowed?
AGENT=$(kubectl get pods -n kube-system -l component=antrea-agent -o wide | grep nrlf4 | awk '{print $1}')
kubectl exec -n kube-system $AGENT -c antrea-agent -- grep 10.76.36.40 /var/log/antrea/networkpolicy/np.log | tail -5
```

**How to read the results:**
- **Test 1 times out (`exit=124`) and the log shows Allow:** traffic leaves the cluster but nothing answers. The problem is on the path or at the IE3400 (NAT, VLAN or routing). Send it to the network team.
- **Test 2 returns `1769-L38ERM`:** the PLC now answers end to end, so Ignition should connect.
- **Test 1 connects but test 2 returns nothing:** another device still answers at that IP.

## Message for the customer

> Hi Colby,
>
> Thanks for the update. That's a valuable finding.
>
> **Cluster side is complete.** Policy now allows TCP and UDP 44818 from the Ignition gateway to 10.76.36.40, and traceflow shows that traffic leaving the cluster as allowed.
>
> **Your assessment is likely correct.** If the PLC has no ARP entry for the IE3400's local IP (172.24.53.17), traffic from Ignition isn't reaching the PLC through the IE3400. It also likely explains our earlier test. While the SiteManager was using the IE3400's IP, it was probably the SiteManager, not the PLC, that answered on port 44818. That would explain the TCP success without an Ignition connection.
>
> **On the IE3400:** we can only see traffic up to where it leaves the cluster (source 10.76.226.3). We can't see into the plant network or the IE3400's configuration. The IIoT/OT network team will need to verify:
> - the NAT/translation of 10.76.36.40 to the PLC's 172.24.53.x address,
> - that the IE3400 is forwarding traffic to the PLC's port and VLAN,
> - that the firewall rule for RITM5683368 (10.76.226.3 → 10.76.36.40, TCP/UDP 44818) is in place.
>
> It may also help to clear the ARP/NAT tables on the IE3400 and power-cycle it, since it may still have stale entries from the SiteManager conflict.
>
> We are re-testing from the gateway now that the SiteManager is unplugged and will share the results.

When you send the re-test results to the network team, include that the traffic leaves the cluster as `10.76.226.3`.

mesac2_mdt:  kubectl exec -n livelink-ignition ignition-abrc1l-atlas-stg-0 -c ignition -- bash -c 'timeout 5 bash -c "</dev/tcp/10.76.36.40/44818"; echo exit=$?' 2>/dev/null
exit=0
mesac2@CPC-mesac-4BM7S:~$ kubectl exec -n livelink-ignition ignition-abrc1l-atlas-stg-0 -c ignition -- bash -c 'exec 3<>/dev/tcp/10.76.36.40/44818; printf "\x63\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00" >&3; timeout 3 head -c 200 <&3 | strings' 2>/dev/null
mesac2@CPC-mesac-4BM7S:~$ AGENT=$(kubectl get pods -n kube-system -l component=antrea-agent -o wide | grep nrlf4 | awk '{print $1}')
mesac2@CPC-mesac-4BM7S:~$
kubectl exec -n kube-system $AGENT -c antrea-agent -- grep 10.76.36.40 /var/log/antrea/networkpolicy/np.log | tail -5
2026/10/23 21:30:11.672120 AntreaPolicyEgressRule AntreaClusterNetworkPolicy:egress-livelink CIDRs for MECC IIoT VLANs Egress Allow 14398 livelink-ignition/ignition-abrc1l-atlas-stg-0 192.168.23.4 56768 10.76.36.40 44818 TCP 60 <nil>
2026/10/23 21:30:24.361106 AntreaPolicyEgressRule AntreaClusterNetworkPolicy:egress-livelink CIDRs for MECC IIoT VLANs Egress Allow 14398 livelink-ignition/ignition-abrc1l-atlas-stg-0 192.168.23.4 54686 10.76.36.40 44818 TCP 60 <nil>
2026/10/23 21:30:30.771779 AntreaPolicyEgressRule AntreaClusterNetworkPolicy:egress-livelink CIDRs for MECC IIoT VLANs Egress Allow 14398 livelink-ignition/ignition-abrc1l-atlas-stg-0 192.168.23.4 55648 10.76.36.40 44818 TCP 60 <nil>
2026/10/23 21:30:32.889423 AntreaPolicyEgressRule AntreaClusterNetworkPolicy:egress-livelink CIDRs for MECC IIoT VLANs Egress Allow 14398 livelink-ignition/ignition-abrc1l-atlas-stg-0 192.168.23.4 55650 10.76.36.40 44818 TCP 60 <nil>
2026/10/23 21:30:49.872206 AntreaPolicyEgressRule AntreaClusterNetworkPolicy:egress-livelink CIDRs for MECC IIoT VLANs Egress Allow 14398 livelink-ignition/ignition-abrc1l-atlas-stg-0 192.168.23.4 58830 10.76.36.40 44818 TCP 60 <nil>

GitHub Copilot: The cluster side is fine, but these results suggest the PLC is not the device answering on port 44818.

## What the results show

| Test | Result | Meaning |
|---|---|---|
| TCP connect to `10.76.36.40:44818` | `exit=0` | Something at that IP accepts the TCP connection |
| EtherNet/IP identity request | **No reply** | Whatever accepted the connection did not answer like a PLC |
| Antrea log | **Allow** | The traffic leaves the cluster normally |

A 1769-L38ERM always answers the identity request with its product name. Together with the PLC's empty ARP table, this suggests the IE3400 or a firewall in the path accepts the connection and the request never reaches the PLC.

The second test filters its output through `strings`, which can hide a short reply. Run this once to see the raw bytes:

```bash
kubectl exec -n livelink-ignition ignition-abrc1l-atlas-stg-0 -c ignition -- bash -c 'exec 3<>/dev/tcp/10.76.36.40/44818; printf "\x63\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00" >&3; timeout 5 od -c <&3 | head -20' 2>/dev/null
```
- **No output:** nothing replied. This confirms the problem is beyond the cluster.
- **Output that includes `1769`:** the PLC does answer, and the problem is on the Ignition side.

## Message for the network (IIoT/OT) team

> Hi team,
>
> We need help with RITM5683368, the Ignition connection to the TM Braze Oven PLC (1769-L38ERM).
>
> - **Source:** Ignition gateway on Tanzu cluster abrc1l-atlas-stg. Traffic leaves the cluster as **10.76.226.3** (`egress.abrc1l-atlas-stg.tkg.mdtcloud.io`).
> - **Destination:** **10.76.36.40**, TCP/UDP 44818. The customer reports this IP is translated by the IE3400 to the PLC's local address in 172.24.53.x.
> - **Cluster side:** the traffic is allowed and leaves the cluster (confirmed in our network policy logs).
> - **What we see:** a TCP connection to 10.76.36.40:44818 opens, but an EtherNet/IP identity request gets no reply. The PLC also has no ARP entry for the IE3400's local IP (172.24.53.17), so traffic doesn't seem to reach the PLC.
> - **Background:** a Secomea SiteManager had been using the IE3400's IP. It has now been unplugged.
>
> Could you please check:
> 1. The IE3400's NAT/translation for 10.76.36.40 to the PLC's address
> 2. Forwarding and VLAN settings on the IE3400 toward the PLC
> 3. Stale ARP or NAT entries left from the SiteManager conflict (clearing them or reloading the switch may help)
> 4. That the firewall rule for 10.76.226.3 → 10.76.36.40 on TCP/UDP 44818 is in place, and which device is actually accepting the TCP connection

## Message for the customer

> Hi Colby,
>
> Your assessment looks correct. From inside the Ignition gateway, a TCP connection to 10.76.36.40:44818 opens, but an EtherNet/IP identity request gets no reply. A 1769-L38ERM would normally answer it. Together with the empty ARP table on the PLC, this indicates the traffic isn't getting past the IE3400 to the PLC.
>
> The cluster side is complete: TCP and UDP 44818 are allowed, and our logs show the traffic leaving the cluster as 10.76.226.3. We are engaging the IIoT/OT network team to check the IE3400's NAT and forwarding, including any stale entries from the SiteManager conflict.

mesac2_mdt: Look the firewall reply


GitHub Copilot: The firewall log shows the corporate firewall is not blocking this traffic. The problem is beyond the firewall, on the plant network.

## What the firewall log shows

| Field | Value |
|---|---|
| Firewall | `fw-abrc1-m1-2` |
| Source | `egress.abrc1l-atlas-stg.tkg.mdtcloud.io` (10.76.226.3) |
| Destination | 10.76.36.40 |
| Service | TCP 44818 |
| Action | **Accept**, by rule "Ignition access…" (the RITM5683368 rule) |
| Frequency | About every 19 seconds, over 1,914 hits in 7 days |

- **The firewall rule works.** The ~19-second interval matches Ignition's reconnect attempts and the cluster's network policy log, so it's the same traffic seen at each step.
- **No UDP entries appear.** The query didn't filter by service, so Ignition is apparently sending only TCP 44818 to the PLC. UDP isn't part of this problem.
- **"Accept" only means the firewall let the traffic through.** It doesn't show that the PLC received or answered it.

## Where the problem is

Here is what we know at each point on the path:

| Point on the path | Status |
|---|---|
| Ignition pod → cluster network policy | Allowed |
| Cluster → firewall `fw-abrc1-m1-2` | Accepted |
| Firewall → 10.76.36.40 | Something completes the TCP connection |
| … → PLC (172.24.53.x) | **Nothing arrives**: no EtherNet/IP reply, and the PLC's ARP table is empty |

So a device at 10.76.36.40 accepts the TCP connection, but it isn't the PLC. That points to the IE3400 itself (its NAT not forwarding to the PLC), or another device that still holds 10.76.36.40 after the SiteManager conflict.

## Message for the network (IIoT/OT) team

> Hi team,
>
> About RITM5683368: the firewall `fw-abrc1-m1-2` is accepting TCP 44818 from 10.76.226.3 to 10.76.36.40 (rule "Ignition access…"), about every 19 seconds. A TCP connection to 10.76.36.40:44818 opens, but the PLC (1769-L38ERM) never responds at the EtherNet/IP level and has no ARP entry for the IE3400's local IP 172.24.53.17. So the traffic stops between the firewall and the PLC.
>
> Could you please check:
> 1. **Which device owns 10.76.36.40:** check the ARP/MAC entry on the L3 gateway for that subnet and compare it with the IE3400's MAC. A Secomea SiteManager was using that IP earlier, so there may be a stale ARP entry or a duplicate IP.
> 2. **The IE3400's NAT** from 10.76.36.40 to the PLC's 172.24.53.x address, including translation counters while Ignition is retrying.
> 3. **The IE3400's ARP and NAT tables:** clear them, or reload the switch.

## Message for the customer

> Hi Colby,
>
> The network firewall logs confirm that traffic from the Ignition gateway (10.76.226.3) to 10.76.36.40 on TCP 44818 is allowed, about every 19 seconds, matching your Reconnecting status. Only TCP 44818 shows up, so UDP isn't a factor.
>
> Your assessment is correct. The traffic passes the cluster and the firewall but doesn't reach the PLC: a device at 10.76.36.40 accepts the connection, but the PLC never answers. We've asked the IIoT/OT network team to check which device owns 10.76.36.40 and to verify the IE3400's NAT to the PLC, including stale entries left from the SiteManager conflict.

No repo changes are needed.

mesac2_mdt: Look again the leader reply, and check if I need do something from my side
So this is egress, but it does look like the rule added in the MR you linked should cover this pattern
Okay great, and did we confirm they tried connecting from a pod in the livelink-ignition namespace on abrc1l-atlas-stg when they got the error they're still reporting now?
 
I would also ask them to research whether Ignition uses a separate port for negotiating connections, some apps will do that. Also worth checking if we can pass both UDP traffic from pods in that namespace to the PLC on port 44818
 I also don't see an A record for abrc1-mceovenvacuum017-plc.iiot.medtronic.com so not sure how we got 10.76.36.40 - did the customer give that IP?

 With no DNS record its possible antrea isn't allowing traffic to that host since our egress rule gives that FQDN as the destination rather than an IP - but you said the connection worked so perhaps that's not an issue
 
I can get a tcptraceroute to work for that IP which is interesting, but UDP is blocked somewhere (likely the IIOT firewall)
 
Not sure if they need UDP, again I'd ask for all ports and protocols they know Ignition to use both for connection negotiation/establishment (control sequence) and for data streaming 
 
If they list anything besides TCP/44818 then we need to make adjustments
 

GitHub Copilot: Almost everything your leader asked for is done. Two things are left: get the customer's full port list, and tell them about the missing DNS record.

## Status of each point

| Leader's point | Status | Evidence |
|---|---|---|
| The rule in the MR covers this egress traffic | ✅ Done | The rule now includes `10.76.36.40/32` for TCP and UDP 44818, and it's live on the cluster. The TCP traceflow showed `egress-livelink` forwarding the traffic; the UDP traceflow showed `egress-livelink-mecc` forwarding it. |
| Confirm the failing gateway is a pod in `livelink-ignition` on abrc1l-atlas-stg | ✅ Done | `stg.mecc.livelink.medtronic.com` resolves to `envoy-livelink-gateway` (10.76.225.131) in this cluster. The HTTPRoute in `livelink-ignition` serves that hostname. The only Ignition pod is `ignition-abrc1l-atlas-stg-0`, and the network policy log shows its reconnect attempts. |
| Allow UDP 44818 from that namespace to the PLC | ✅ Done in the cluster | Traceflow shows UDP forwarded by "STG TM Braze Oven Ignition to PLC". The IIoT firewall still blocks UDP, but that only matters if Ignition needs UDP. |
| No DNS record; did the customer provide the IP? | ✅ Answered | Yes, the customer provided 10.76.36.40. The `ipBlock` entry covers the IP even though the hostname has no DNS record. |
| The hostname-based rule may not match without DNS | ✅ Resolved | That's why the `ipBlock` was added. TCP was already allowed by the `10.76.36.0/23` range in `egress-livelink`. |
| The leader's tcptraceroute works | ✅ Matches our results | Our TCP test succeeded, and the firewall `fw-abrc1-m1-2` shows **Accept** about every 19 seconds. |
| Ask for **all** ports and protocols Ignition uses, for connection setup and for data | ⏳ **Pending, needs the customer's answer** | The firewall log shows only TCP 44818 from the gateway, so Ignition doesn't seem to be trying anything else. The customer still needs to confirm in writing. |
| Tell the customer there's no A record for the PLC hostname | ⏳ **Pending, unless you already sent it** | Include it in the message below. |

## What you still need to do

1. **Send this to the customer.** It covers the two pending items and the earlier findings:

> Hi Colby,
>
> A few follow-ups on RITM5683368:
>
> 1. **Ports and protocols:** could you confirm every port and protocol the Ignition LogixDriver uses to talk to the 1769-L38ERM, for both connection setup and data reads? Today the cluster allows TCP and UDP 44818, and the firewall logs show only TCP 44818 from the gateway. If anything else is required (for example UDP 2222 or another port), we'll adjust the policy and firewall.
> 2. **DNS:** there is currently no A record for `abrc1-mceovenvacuum017-plc.iiot.medtronic.com`. We're using the IP you provided (10.76.36.40). If you want to use the hostname, a DNS record will need to be requested.
> 3. **Current status:** the traffic passes the cluster and the firewall (`fw-abrc1-m1-2` accepts TCP 44818 from 10.76.226.3), but the PLC never answers and has no ARP entry for the IE3400. The IIoT/OT network team is checking the IE3400's NAT and which device currently holds 10.76.36.40.

2. **If the customer lists anything besides TCP 44818:**
   - add those ports to the "STG TM Braze Oven Ignition to PLC" rule in antrea_clusternetworkpolicy_egress-livelink-mecc.yaml, then open a new MR;
   - request the matching IIoT firewall rule (the leader saw UDP blocked there).

3. **If it's only TCP 44818:** nothing else is needed from the cluster team. Keep the ticket open while the network team checks the IE3400 and the 10.76.36.40 issue, and reply to your leader with the table above.

mesac2_mdt: And what could be the question for my leader?

GitHub Copilot: These are the open decisions where your leader's input helps most:

1. **Ownership and escalation.** The traffic passes the cluster and is accepted by `fw-abrc1-m1-2`, but it never reaches the PLC. Should I open a separate ticket to the IIoT/OT network team about the IE3400 NAT and who owns 10.76.36.40? Or should I hand the incident to them and close our part?

2. **Your tcptraceroute result.** When your tcptraceroute to 10.76.36.40 worked, which hop answered last, and did it show a MAC or vendor? Our EtherNet/IP identity request got no reply, so we think the IE3400 or another device is accepting the connection, not the PLC. Your result could confirm that.

3. **Where UDP was blocked.** You saw UDP blocked "somewhere (likely the IIoT firewall)". Do you know which device or hop? If the customer confirms they need UDP 44818, should I request it on RITM5683368 or open a new firewall request?

4. **The UDP rule in the MR.** The firewall log shows only TCP 44818 from the gateway. Should we keep UDP 44818 in `egress-livelink-mecc` in case it's needed later, or remove it until the customer confirms it?

5. **DNS record.** Should we ask the customer to request an A record for `abrc1-mceovenvacuum017-plc.iiot.medtronic.com`? Or is the `ipBlock` with the IP they gave us enough, and we drop the hostname from the rule?

6. **Ticket status.** While we wait for the customer's port list and the network team's IE3400 check, should I keep the ticket open on our side or set it to pending the customer?

Questions 1 and 2 matter most, since they decide who takes the next step.
