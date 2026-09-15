Worked for 5m 9s

Sí hay patrones bastante claros. Analicé los tickets del Excel separando solicitudes operativas de incidentes. No tomaría como señal para el agente los accesos rutinarios a GitLab Dedicated/code.medtronic, creación de repositorios, altas de usuarios, provisioning estándar, etc. Ese volumen solo contaminaría el modelo; debería desviarse directamente a workflows/runbooks.

Después de ese filtro, las familias más repetidas son aproximadamente: GitOps/reconciliation/config drift ~33 casos, WAF/HTTP/Ingress ~22, problemas reales de conectividad ~20, CI/CD/runners ~20, inestabilidad de workloads/pods ~15, credenciales/tokens ~9, storage ~8, database health/performance ~8, JFrog/Artifactory health ~7 y saturación de recursos Kubernetes ~7. Las categorías se solapan porque un mismo incidente puede tener, por ejemplo, un síntoma de pipeline y una causa de red.

Lo importante: no construiría un LLM que esté revisando métricas continuamente y decida por sí solo si algo está mal. Eso sería frágil y generaría falsos positivos. La detección debe seguir siendo determinística con Prometheus/Grafana, CloudWatch, Kubernetes Events, Flux, GitLab, JFrog, WAF, etc. El agente debe entrar después: validar la señal, correlacionar evidencias, compararla contra incidentes históricos y generar una alerta explicando qué ocurre, impacto probable y acción recomendada.

Top 10 para construir el agente de alertas tempranas

#	Ticket / patrón	Qué pasó	Qué debería validar el agente	Alerta / posible solución	Profundidad

1	INC13427579 – K8s Memory Saturation	Workers estaban en 86–92% de memoria y un pod pedía 8 Gi aunque podía funcionar con 2 Gi.	Requests/limits vs allocatable, utilización real, Pending Pods, scheduler events.	“Cluster llegará a saturación; pod X está sobreaprovisionado”. Recomendar ajuste de requests o scale-up.	Media
2	INC13689939 – Pod Pending / capacity	Ignition quedó Pending por memoria insuficiente. Se resolvió agregando un nodo.	FailedScheduling, memoria disponible, requests pendientes, tendencia de capacidad.	Alertar antes de que el servicio caiga cuando capacidad reservada supere 75–85%.	Baja
3	INC13867492 – JFrog full capacity	Artifactory llegó a capacidad y bloqueó uploads, afectando releases. Hay tickets similares INC13866644 e INC13869810.	Filesystem usage, crecimiento diario, errores push, JFrog health.	“Artifactory alcanzará 90% en X días”. Abrir acción de expansión antes del outage.	Baja
4	INC13121612 – WAF 403	Uploads bloqueados por reglas WAF; hubo que agregar excepciones por URI. Hay varios 403 similares en Argo.	WAF rule ID, URI, método, blocked count, ALB status, si request llegó al pod.	“403 originado en WAF, regla X, path Y; no es application failure”. Proponer excepción específica.	Alta
5	INC13401509 – Flux token	Deployment fallaba porque el token usado por Flux necesitaba rotación.	Expiry de tokens, 401/403, Flux Ready=False, SecretStore y último refresh.	Avisar 30/14/7 días antes del vencimiento o detectar auth failures inmediatamente.	Baja
6	INC14171174 – GitLab Runner down	Runner #156 quedó down; SolarWinds/automation terminó recuperándolo.	Runner online/offline, jobs queued, última ejecución, host CPU/memory/network.	“Runner offline >3 min; N jobs esperando”. Ejecutar health-check y sugerir restart/failover.	Baja
7	INC14241834 – Terraform/SOPS duplicate config	Configuración SOPS duplicada bloqueó el MR del certificado.	tofu validate, duplicate variables/resources, cambios generados vs configuración existente.	Detectarlo en MR antes de merge/pipeline y señalar exactamente archivo/recurso duplicado.	Media
8	INC14197870 – HiveMQ rolling restart / drift	HiveMQ llevaba ~42 h en rolling restart. Parecía volume mount, pero la causa probable era StatefulSet spec drift durante reconciliación.	StatefulSet vs CR/operator spec, rollout status, volume mounts, node, events, reconciliation history.	“Rolling update no progresa; spec generado difiere de desired state”.	Alta
9	INC14114825 – GitLab timeout / anomalous traffic	Un proceso Python generó casi 10 millones de requests contra code.medtronic.com, causando CPU elevada y timeouts globales.	Requests/source IP, rate vs baseline, CPU, connections, 5xx/timeouts.	Detectar source con tráfico 5–10× baseline y correlacionarlo con degradación del servicio.	Alta
10	INC13797089 / INC13560543 – DB capacity/performance	Hay señales de free storage e IOPS insuficientes; en un caso las cargas bajaron a 24 rows/sec.	FreeStorageSpace, IOPS, latency, connections, throughput y tendencia.	“Storage agotable en 7 días” o “latencia/IOPS fuera del baseline; posible degradación en X horas”.	Media


Hay varios tickets que deberían tratarse como una misma familia. Por ejemplo, INC13427579 + INC13689939 + INC13539375 + INC14206510 no necesitan cuatro procedimientos distintos: son un único proceso de Kubernetes Capacity & Scheduling Health. De igual forma INC13867492 + INC13866644 + INC13869810 son un proceso de Artifact Repository Capacity, y los numerosos Flux Failure/Kustomization Failure deberían entrar a un único flujo de GitOps Reconciliation Health.

Los casos de menor profundidad son buenos candidatos para automatización casi completa: disk/storage, token/certificate expiration, runner offline, Flux Ready=False, Pending pods por recursos, queue depth y database free storage. Ahí las señales son objetivas.

Los que requieren más profundidad son WAF 403/5xx, conectividad entre múltiples capas, configuration drift, performance degradations y anomalías como INC14114825. Un timeout no significa necesariamente “firewall”: puede ser DNS, SG, Cilium, ALB, aplicación, saturación o incluso un cliente abusivo. En esos casos el agente debe juntar evidencia antes de declarar una causa.

Cómo implementaría el agente

La arquitectura que veo más viable sería:

Telemetry/Event → Detector → Evidence Collector → Correlation Agent → ServiceNow/Teams Alert → Runbook

Prometheus/Grafana, CloudWatch, WAF, Kubernetes Events, Flux, GitLab, JFrog y las métricas DB siguen detectando condiciones objetivas. Cuando una condición cruza un umbral, se dispara el agente.

El agente entonces consulta en modo read-only el contexto necesario. Por ejemplo, ante un Pending Pod, recogería node capacity, requests/limits, scheduler events, taints, affinity y estado de Flux. Ante un 403 recogería WAF logs, regla que bloqueó, URI, método y ALB logs. Ante un pipeline failure revisaría runner, red, Artifactory, Flux y errores del pipeline.

La salida no debería decir simplemente “Memory issue”. Debería generar algo como:

> Early Warning – awby1-atlas-prd
Riesgo: High
Síntoma: 3 pods Pending durante 6 min.
Evidencia: worker memory requests 87%, FailedScheduling: Insufficient memory.
Patrón histórico: similar a INC13427579 / INC13689939.
Impacto probable: nuevos deployments no podrán programarse.
Acción recomendada: revisar requests de los 5 pods de mayor consumo; si son válidos, iniciar scale-up.
Confianza: 94%.



Ahí está el valor real del agente: no solo alertar, sino contestar inmediatamente qué pasó, por qué cree que está pasando, qué evidencia tiene, qué impacto tendrá y cómo abordarlo.

Para una primera versión, concentraría el MVP en cinco detectores: Kubernetes capacity/scheduling, Flux reconciliation, JFrog/storage, GitLab runner health y WAF 403/5xx. Con esos cinco ya cubres una parte considerable de los incidentes técnicamente repetitivos del Excel y, a diferencia de los accesos a GitLab, son problemas donde una alerta anticipada sí puede evitar un incidente.












Worked for 34s

Revisé el documento del modelo operativo y lo contrasté con la documentación disponible en el Drive compartido, especialmente con el flujo de requests, las respuestas estándar de soporte y la documentación técnica de Tanzu/Atlas. El modelo actual es bastante sólido en estructura, pero todavía mezcla tres cosas que deberían separarse mejor: modelo de soporte, modelo de gobierno y modelo técnico de plataforma.

La principal observación es esta: hoy el documento describe bien “quién hace qué”, pero todavía no define con suficiente precisión “qué tipo de ticket entra por cada camino, cuándo deja de ser incidente y pasa a request/problem/change, ni qué evidencia mínima debe existir antes de escalar”. Esa ambigüedad puede generar sobreescalamiento, SLA artificialmente altos y dependencia innecesaria de L2/L3.

El modelo actual declara ServiceNow como sistema autoritativo para incidentes, problemas, cambios y conocimiento, y también define L1–L3, escalamiento, RCA, war rooms, handovers y métricas. Eso está correctamente planteado como base. 

Mi propuesta de mejora sería la siguiente.

Área de mejora	Situación actual	Riesgo	Recomendación

Clasificación inicial	El flujo arranca casi directamente como incidente	Requests, information requests y backlog items pueden terminar tratados como incidentes	Añadir un “Intake Decision Gate” antes de L1: Incident / Service Request / Information Request / Change / Problem
Escalamiento L1→L2	Basado en complejidad, permisos o tiempo	Puede ser subjetivo	Definir criterios técnicos verificables y un “Minimum Escalation Package” obligatorio
L2/L3	L2 y L3 están bien descritos, pero algunos roles se solapan	Riesgo de que Evangelist termine siendo un L3 operativo permanente	Separar L3 Engineering de Architecture/Evangelist
Vendor	Está bien documentado por producto	Falta definir el punto exacto de engagement	Crear Vendor Engagement Criteria y Vendor Evidence Pack
Requests recurrentes	El modelo los trata como trabajo de soporte	Mucho volumen repetitivo puede consumir L1	Crear rutas de auto-servicio para accesos, egress, namespaces y CI/CD
Knowledge Management	SOP/Playbook/KB están definidos	Falta criterio para saber cuál crear	Añadir taxonomy y “documentation trigger” después de cada tipo de caso
Problem Management	Existe en RACI y RCA	Se activa principalmente en P1/P2	Añadir triggers por recurrencia, no solo severidad
Métricas	MTTA, MTTR, SLA, FCR, repeat incidents	Pueden incentivar cerrar rápido en vez de resolver bien	Añadir Reopen Rate, Escalation Quality y Automation/Deflection Rate
Arquitectura	Incluida dentro de escalamiento técnico	Puede participar demasiado pronto	Architecture debe entrar por criterios de diseño/riesgo, no solo porque L3 no resuelva
Operational Cycles	Daily/Weekly/Monthly bien planteados	Pueden convertirse en reuniones de seguimiento	Cada ciclo debe tener decisiones y outputs obligatorios


La primera mejora que haría es introducir formalmente un “Request Intake & Classification Layer”. La documentación interna ya tiene esta lógica y, de hecho, es más madura que la del modelo operativo actual. El documento request-flow.md distingue explícitamente entre “Fix something” → Incident, “I need this” → Service Request e “Information Request”, y además decide si un request debe ir a backlog, ServiceNow, otro grupo o documentación de self-help. 

Por eso, en lugar de:

Customer → ServiceNow → L1 → L2 → L3

recomendaría:

Customer / Monitoring
→ Intake
→ Classification
→ Routing
→ Execution
→ Escalation if required
→ Resolution
→ Knowledge / Problem / Improvement loop

Y el Classification Gate debería tener al menos estas cinco salidas:

1. Incident: algo funcionaba y ahora está roto.


2. Service Request: el usuario necesita que algo sea habilitado, creado o configurado.


3. Information Request: solicita orientación, documentación o explicación.


4. Change: requiere modificar una configuración controlada.


5. Problem: existe recurrencia o causa sistémica que debe investigarse fuera del incidente puntual.



Esto es importante porque buena parte de los ejemplos de tickets que mostraste anteriormente —egress rules, firewall access, namespace access, GitLab CI access, NFS rules— no son realmente incidentes si el acceso nunca existió. Son Service Requests o Changes. Si se manejan como incidentes, el MTTR deja de representar confiabilidad real de la plataforma.

La segunda mejora es formalizar un “Minimum Escalation Package”. Actualmente el documento dice que L1 recopila logs, timestamps y screenshots, y que L2 realiza troubleshooting profundo.  Eso es correcto, pero demasiado abierto. El ticket no debería poder escalarse a L2 solamente con “not working”.

Para Atlas/Kubernetes, propondría exigir como mínimo:

Cluster

Environment

Namespace

Affected workload/application

Timestamp and timezone

Error message

Business impact

Reproducibility

kubectl get pods

relevant kubectl describe

events

logs when applicable

network source/destination/port for connectivity issues

recent deployment/change

related pipeline or repository

troubleshooting already performed

expected vs actual behavior


Si falta información que depende del usuario, el ticket debería pasar a Awaiting Customer Feedback; si la información existe y L1 simplemente no la recopiló, no debería escalarse.

Esto tendría un impacto directo sobre “Escalation Quality”, una métrica que recomiendo añadir.

La tercera mejora es separar “Support Level” de “Organizational Role”. Aquí veo una debilidad en el documento. En la matriz actual aparecen Juan Pablo Mosquera, William Urrea y Daniel Cure agrupados bajo “L3 / Platform Evangelists”, mientras que Daniel también aparece como Kubernetes Support Engineer/SRE y William como Source Code Support Engineer.  

Eso genera una ambigüedad importante: una persona no debería convertirse automáticamente en L3 por su nombre o conocimiento. El nivel debe corresponder al tipo de intervención requerida.

Recomendaría separar:

Support Tier: L1 → L2 → L3 Engineering

Functional Role: Support Engineer / SRE / Team Lead / Evangelist / Architect / Vendor Liaison / Operations Manager

Eso permite que, por ejemplo, Daniel pueda actuar como L2 en una incidencia operacional y como L3 en otra, dependiendo de la complejidad. Es un modelo más realista y escalable.

La cuarta mejora es redefinir el rol del Evangelist. Actualmente el documento le asigna deep troubleshooting, architecture alignment, permanent fixes y vendor liaison.  Eso puede terminar convirtiendo al Evangelist en “la persona a la que se manda todo lo difícil”.

Yo lo limitaría a:

recurring/systemic defects,

platform capability gaps,

cross-domain technical decisions,

self-service/platform adoption,

reusable solution patterns,

platform improvement backlog.


El troubleshooting profundo debería pertenecer a L3 Engineering/SRE. Architecture debería intervenir únicamente cuando exista cambio de diseño, riesgo estructural o desviación de estándar.

La quinta mejora es aprovechar mucho más el enfoque GitOps/IaC que ya aparece en la documentación de Atlas. La documentación técnica de Tanzu muestra que el provisioning depende de Terraform, Route53, GitLab variables, repositorios de management, Port y Flux bootstrap.  Además, la documentación de dependencias establece explícitamente Infrastructure as Code, GitOps y HA como requisitos de la plataforma. 

Eso significa que el modelo operativo debería tener una regla clara:

> Manual production remediation should be exceptional. Any persistent configuration change must be reconciled back to the authoritative Git/IaC source.



Ahora mismo el documento permite acciones como node drains, configuration overrides y routing workarounds, pero no establece suficientemente el “reconciliation step”. 

Yo agregaría una fase después de mitigation:

Operational mitigation
→ Service restored
→ Determine whether runtime differs from desired state
→ Update Git/IaC configuration
→ Deploy through approved pipeline
→ Validate reconciliation
→ Close

Esto evita configuration drift.

La sexta mejora es crear un “Known Request Catalog”. La documentación de respuestas estándar muestra que ya existen casos que no requieren análisis profundo. Por ejemplo, ciertos accesos GitLab deben redirigirse al formulario correcto de ServiceNow y resolverse con una respuesta estandarizada; también existen reglas específicas para acceso a equipos, permisos elevados, Artifactory y membresías LDAP. 

Esto debería formar parte formal del modelo operativo.

Por ejemplo:

Known Request Catalog

GitLab team access

GitLab permissions

Artifactory repository access

Namespace access

Egress/firewall request

CI runner external destination

New cluster request

Additional node/capacity

Flux onboarding

Certificate request

Database connectivity request


Cada uno debería tener:

classification,

assignment group,

prerequisites,

mandatory ticket fields,

SOP/playbook,

expected resolver,

expected SLA,

whether approval/change is required,

whether it is self-service capable.


Esto reduciría drásticamente el trabajo repetitivo.

La séptima mejora es endurecer Problem Management. El documento exige RCA para P1/P2 en 48–72 horas y vinculación a PRB.  Eso está bien, pero deja fuera una categoría que suele ser más costosa: los P3 repetitivos.

Por ejemplo, diez incidentes P3 de egress, scheduling o certificates pueden consumir más horas que un P2 aislado.

Añadiría triggers como:

Same root cause ≥ 3 incidents in 30 days.

Same component ≥ 5 incidents in 30 days.

Manual workaround used ≥ 3 times.

Incident reopened ≥ 2 times.

Same support article used > X times but does not permanently resolve.

SLA breach caused by the same dependency ≥ 2 times.


Eso debería crear automáticamente o recomendar un Problem Record.

La octava mejora es revisar la Emergency Change Policy. Actualmente el documento establece verbal authorization durante el incidente y permite crear retrospectivamente el eCR dentro de 24 horas.  Esto puede ser válido internamente, pero es una de las partes más sensibles desde auditoría.

Yo añadiría cuatro controles:

Emergency change eligibility criteria.

Explicit rollback plan before execution.

Named approver recorded in ServiceNow before the change whenever technically possible.

Reconciliation to Git/IaC after emergency runtime change.


Y separaría “operational command” de “change”. Reiniciar un pod no necesariamente debería considerarse eCR; modificar un routing rule, firewall, cluster configuration o version sí.

La novena mejora está en los ciclos operativos. El daily/weekly/monthly está bien estructurado, con agenda y entregables.   El problema es que puede convertirse fácilmente en reuniones de reporte.

Yo haría que cada nivel tenga una función distinta:

Daily = Execute Weekly = Improve Monthly = Govern

Daily:

SLA risks

current incidents

capacity/health

handover


Weekly:

recurring issues

automation candidates

aging backlog

problem records

KB gaps

top noisy alerts


Monthly:

SLA and SLO

availability

recurring failure domains

vendor performance

platform risk

technical debt

investment decisions


La décima mejora es ampliar las métricas. Actualmente se incluyen SLA Compliance, MTTR, MTTA, Shift-Left First Contact Resolution, Repeat Incident Index y Change Success Rate.  Es un buen inicio, pero faltan métricas que midan calidad.

Yo agregaría:

KPI	Propósito

Reopen Rate	Detectar “resoluciones” que no fueron permanentes
L1 Escalation Rate	Medir cuánto trabajo realmente resuelve L1
Escalation Quality	% de escalaciones con evidence package completo
Ticket Reassignment Rate	Detectar mala clasificación
Automation / Deflection Rate	Cuánto trabajo evita llegar a un ingeniero
Known Issue Resolution Rate	Casos resueltos usando KB/SOP
Problem Elimination Rate	PRBs que eliminan recurrencia
Alert-to-Incident Conversion	Calidad de observabilidad
Noisy Alert Rate	Alertas sin acción requerida
Vendor Dependency Time	Tiempo de resolución consumido fuera del equipo
Change-Induced Incident Rate	Incidentes generados por cambios
Configuration Drift Incidents	Incidentes causados por diferencias runtime vs Git/IaC


Mi recomendación estructural sería que el documento final quedara así:

1. Purpose & Scope


2. Service Portfolio


3. Request Intake & Classification


4. Support Tier Model


5. Functional Roles & Stakeholders


6. Assignment Groups


7. Incident Management


8. Service Request Management


9. Problem Management


10. Change & Emergency Change Management


11. Vendor Management


12. Major Incident & War Room


13. Knowledge Management


14. GitOps / Configuration Reconciliation


15. Operational Cycles


16. RACI


17. SLA / OLA Model


18. Metrics & SLOs


19. Continuous Improvement


20. Appendices: Known Request Catalog, escalation packages, templates



En términos de madurez, no considero que el documento actual necesite “más contenido” indiscriminadamente. Ya tiene bastante. Lo que necesita ahora es mayor precisión operacional: separar tipos de trabajo, reducir ambigüedad en escalaciones, distinguir roles de niveles, y convertir conocimiento repetitivo en rutas estandarizadas/self-service.

La mejora con mayor impacto sería introducir estos cuatro bloques primero: Intake Classification → Minimum Escalation Package → Known Request Catalog → Problem/Automation Feedback Loop. Eso cambiaría el modelo de uno principalmente reactivo a uno de operación controlada y mejora continua.
