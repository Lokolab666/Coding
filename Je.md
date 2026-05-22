
if(
  greaterOrEquals(
    int(outputs('Compose_QueueStateItem')?['LastAssignedOrder']),
    variables('varMaxQueueOrder')
  ),
  1,
  add(int(outputs('Compose_QueueStateItem')?['LastAssignedOrder']), 1)
)


















Columna	Tipo	Ejemplo

Title	Texto	MainIncidentQueue
LastAssignedOrder	Número	5
LastAssignedCode	Texto	MARCELA
LastAssignedName	Texto	Marcela
LastIncident	Texto	INC13763770
MaxQueueOrder	Número	7
UpdatedAtText	Texto	2026-05-21 16:20


Crea un solo registro:

Title: MainIncidentQueue
LastAssignedOrder: 0
LastAssignedCode: EMPTY
LastAssignedName: EMPTY
LastIncident: EMPTY
MaxQueueOrder: 7
UpdatedAtText: EMPTY

Si LastAssignedOrder = 0, el primer ticket normal irá al miembro con QueueOrder = 1.

3. Lista Incidents

Esta lista reemplaza el Excel operativo.

Columna	Tipo	Valores / ejemplo

Title	Texto	INC13723435
IncidentNumber	Texto	INC13723435
EntryType	Choice	Normal, India, Weekend, AfterHours, MergedEmail, ManualOverride
EmailCode	Texto	código especial recibido por correo
AlreadyAssignedCode	Texto	CRISTIAN
AlreadyAssignedName	Texto	Cristian
SuggestedAssigneeCode	Texto	DAVID
SuggestedAssigneeName	Texto	David
FinalAssigneeCode	Texto	DAVID
FinalAssigneeName	Texto	David
Counted	Sí/No	Yes / No
Status	Choice	New, Assigned, CreditCreated, Merged, Cancelled, Error
SkippedMembers	Varias líneas de texto	Cristian skipped by credit


Campos eliminados de Incidents:

Priority
Reason
ServiceNowLink

Para el caso de Merged, no lo trataría como un ticket normal. Como dices que llega por correo con un código especial, lo manejaría así:

EntryType = MergedEmail
EmailCode = código especial del correo
Counted = No
Status = Merged

Si ese requisito de correo realmente consume trabajo, entonces pueden poner:

EntryType = MergedEmail
EmailCode = código especial
Counted = Yes

Pero por defecto yo lo dejaría como Counted = No. Si todo lo que llega por correo empieza a contar como trabajo, la cola se va a contaminar.

4. Lista Credits

Esta lista maneja los saltos.

Columna	Tipo	Ejemplo

Title	Texto	CRISTIAN - INC13686579
MemberCode	Texto	CRISTIAN
MemberName	Texto	Cristian
CreditType	Choice	India, Weekend, AfterHours, MergedEmail, Manual
ReferenceIncident	Texto	INC13686579
ReferenceEmailCode	Texto	código especial
Status	Choice	Available, Consumed, Cancelled
ConsumedByIncident	Texto	INC13751883
ConsumedAtText	Texto	2026-05-22 10:30


También eliminé Reason de Credits. No tiene sentido quitar Reason de Incidents y dejarlo como campo libre en Credits; eso vuelve inconsistente el modelo.

5. Flow principal: Assign Normal Incident

Power Automate debe correr cuando se crea un item en Incidents. El conector de SharePoint incluye triggers como “When an item is created” y acciones como “Get items” para leer datos de listas. 

Crea el flujo:

Create
→ Automated cloud flow
→ Flow name: Assign Normal Incident
→ Trigger: SharePoint - When an item is created

Trigger:

Site Address: sitio donde están las listas
List Name: Incidents

En el trigger, configura concurrencia:

Settings
→ Concurrency Control: On
→ Degree of Parallelism: 1

Esto sigue siendo necesario. Sin eso, dos tickets creados al mismo tiempo podrían leer el mismo LastAssignedOrder y asignarse mal. Power Automate documenta límites y comportamiento de concurrencia en flujos; para esta cola, prefiero seguridad antes que velocidad. 

Trigger condition:

@and(
  equals(triggerBody()?['EntryType']?['Value'],'Normal'),
  equals(triggerBody()?['Status']?['Value'],'New'),
  empty(triggerBody()?['FinalAssigneeCode'])
)

Si en tu tenant los Choice no salen con Value, usa:

@and(
  equals(triggerBody()?['EntryType'],'Normal'),
  equals(triggerBody()?['Status'],'New'),
  empty(triggerBody()?['FinalAssigneeCode'])
)

6. Variables del Flow principal

Inicializa estas variables:

varAssigned
Boolean
false

varCandidateOrder
Integer
0

varMaxQueueOrder
Integer
7

varCandidateCode
String
vacío

varCandidateName
String
vacío

varSkippedMembers
String
vacío

varLoopCounter
Integer
0

varIncidentNumber
String
IncidentNumber del item creado

Ya no inicialices nada relacionado con prioridad ni razón.

7. Leer QueueState

Agrega acción:

SharePoint → Get items

Configuración:

List Name: QueueState
Filter Query: Title eq 'MainIncidentQueue'
Top Count: 1

Luego agrega:

Compose → QueueStateItem

Expression:

first(body('Get_items')?['value'])

Si Power Automate nombra la acción como Get_items_QueueState, ajusta el nombre.

Ahora setea:

Set variable → varMaxQueueOrder

Expression:

int(outputs('QueueStateItem')?['MaxQueueOrder'])

Y calcula el siguiente candidato:

Set variable → varCandidateOrder

Expression:

if(
  greaterOrEquals(
    int(outputs('QueueStateItem')?['LastAssignedOrder']),
    variables('varMaxQueueOrder')
  ),
  1,
  add(int(outputs('QueueStateItem')?['LastAssignedOrder']), 1)
)

8. Loop de asignación

Agrega:

Control → Do until

Condición:

varAssigned is equal to true

Configuración del loop:

Count: 20
Timeout: PT5M

Dentro del loop:

Increment variable → varLoopCounter
Increment by: 1

Busca el candidato:

SharePoint → Get items
List Name: TeamMembers

Filter Query:

QueueOrder eq @{variables('varCandidateOrder')} and Active eq 1

Si falla, usa expresión:

concat('QueueOrder eq ', variables('varCandidateOrder'), ' and Active eq 1')

Condición:

length(body('Get_candidate_member')?['value']) is greater than 0

Si no encuentra candidato activo, avanza al siguiente:

Set variable → varCandidateOrder

Expression:

if(
  greaterOrEquals(variables('varCandidateOrder'), variables('varMaxQueueOrder')),
  1,
  add(variables('varCandidateOrder'), 1)
)

Si sí encuentra candidato:

Compose → CandidateItem

Expression:

first(body('Get_candidate_member')?['value'])

Setea:

varCandidateCode = outputs('CandidateItem')?['MemberCode']
varCandidateName = outputs('CandidateItem')?['MemberName']

9. Revisar créditos disponibles

Agrega:

SharePoint → Get items
List Name: Credits

Filter Query:

concat(
  'MemberCode eq ''',
  variables('varCandidateCode'),
  ''' and Status eq ''Available'''
)

Condición:

length(body('Get_available_credit')?['value']) is greater than 0

Si tiene crédito

Significa que se salta esa persona.

Compose → CreditItem

Expression:

first(body('Get_available_credit')?['value'])

Actualiza el crédito:

SharePoint → Update item
List Name: Credits
Id: outputs('CreditItem')?['ID']

Campos:

Title: valor actual del CreditItem
MemberCode: valor actual del CreditItem
MemberName: valor actual del CreditItem
CreditType: valor actual del CreditItem
ReferenceIncident: valor actual del CreditItem
ReferenceEmailCode: valor actual del CreditItem
Status: Consumed
ConsumedByIncident: varIncidentNumber
ConsumedAtText: utcNow()

Después actualiza varSkippedMembers:

Append to string variable → varSkippedMembers

Value:

@{variables('varCandidateName')} skipped by available credit.

Luego avanza al siguiente candidato:

Set variable → varCandidateOrder

Expression:

if(
  greaterOrEquals(variables('varCandidateOrder'), variables('varMaxQueueOrder')),
  1,
  add(variables('varCandidateOrder'), 1)
)

No asigna todavía. Solo saltó a alguien.

Si no tiene crédito

Aquí se asigna el ticket.

Actualiza el item de Incidents:

SharePoint → Update item
List Name: Incidents
Id: ID del trigger

Campos:

Title: Title actual
IncidentNumber: IncidentNumber actual
EntryType: Normal
EmailCode: valor actual, normalmente vacío
AlreadyAssignedCode: valor actual, normalmente vacío
AlreadyAssignedName: valor actual, normalmente vacío
SuggestedAssigneeCode: varCandidateCode
SuggestedAssigneeName: varCandidateName
FinalAssigneeCode: varCandidateCode
FinalAssigneeName: varCandidateName
Counted: Yes
Status: Assigned
SkippedMembers: varSkippedMembers

Actualiza QueueState:

SharePoint → Update item
List Name: QueueState
Id: outputs('QueueStateItem')?['ID']

Campos:

Title: MainIncidentQueue
LastAssignedOrder: varCandidateOrder
LastAssignedCode: varCandidateCode
LastAssignedName: varCandidateName
LastIncident: varIncidentNumber
MaxQueueOrder: varMaxQueueOrder
UpdatedAtText: utcNow()

Publica en Teams:

Microsoft Teams → Post message in a chat or channel

Power Automate puede enviar mensajes a canales o chats de Teams usando el conector de Microsoft Teams. 

Mensaje:

New incident assignment

Incident: @{variables('varIncidentNumber')}
Assigned to: @{variables('varCandidateName')}

Skipped:
@{variables('varSkippedMembers')}

Finalmente:

Set variable → varAssigned
Value: true

10. Flow 2: Create Credit From External Assignment

Este flujo reemplaza la lógica de India, weekend, after-hours y merged por correo.

Crea:

Create
→ Automated cloud flow
→ Trigger: SharePoint - When an item is created
→ List: Incidents

Trigger condition:

@and(
  equals(triggerBody()?['Status']?['Value'],'New'),
  or(
    equals(triggerBody()?['EntryType']?['Value'],'India'),
    equals(triggerBody()?['EntryType']?['Value'],'Weekend'),
    equals(triggerBody()?['EntryType']?['Value'],'AfterHours'),
    equals(triggerBody()?['EntryType']?['Value'],'MergedEmail')
  )
)

Si Choice no funciona con Value, usa la versión simple.

Agrega condición:

Counted is equal to true

Si Counted = Yes

Crear item en Credits:

SharePoint → Create item
List Name: Credits

Campos:

Title: @{triggerBody()?['AlreadyAssignedName']} - @{triggerBody()?['IncidentNumber']}
MemberCode: AlreadyAssignedCode
MemberName: AlreadyAssignedName
CreditType: EntryType
ReferenceIncident: IncidentNumber
ReferenceEmailCode: EmailCode
Status: Available
ConsumedByIncident: vacío
ConsumedAtText: vacío

Actualizar el incidente:

Status: CreditCreated

Publicar en Teams:

Credit created

Incident: @{triggerBody()?['IncidentNumber']}
Email code: @{triggerBody()?['EmailCode']}
Member: @{triggerBody()?['AlreadyAssignedName']}
Type: @{triggerBody()?['EntryType']?['Value']}
Credit: 1 skip

Si Counted = No

Aquí no se crea crédito.

Actualizar Incidents:

Status:
- Merged, si EntryType = MergedEmail
- Assigned, si es India/Weekend/AfterHours pero no cuenta

Publicar en Teams:

External incident registered but not counted

Incident: @{triggerBody()?['IncidentNumber']}
Email code: @{triggerBody()?['EmailCode']}
Type: @{triggerBody()?['EntryType']?['Value']}
Queue impact: none

Esto mantiene visible el caso sin mover la cola.

11. Cómo manejar el requisito que llega por correo con código especial

Aquí tienes dos opciones.

La opción simple: el QM crea manualmente el registro en Incidents.

IncidentNumber: INCxxxxx o código interno
EntryType: MergedEmail
EmailCode: código especial del correo
AlreadyAssignedCode: miembro afectado, si aplica
AlreadyAssignedName: nombre
Counted: No
Status: New

El Flow 2 lo procesará y lo marcará como Merged sin generar crédito.

La opción más automática: crear un flujo con Outlook.

Trigger:
Outlook - When a new email arrives

Condición:
Subject/body contains código especial

Acción:
Create item en Incidents
EntryType = MergedEmail
EmailCode = código detectado
Counted = No
Status = New

No te recomiendo empezar por esta opción si todavía no tienen claro el patrón del correo. Primero validen manualmente 1 o 2 semanas. Automatizar correos mal estructurados suele traer falsos positivos.

12. Flow 3: Daily Queue Summary

Este sigue útil, pero sin auditoría ni prioridad.

Crea flujo programado:

Create
→ Scheduled cloud flow
→ Every day
→ 8:00 AM

Acciones:

Get items → QueueState
Get items → Credits where Status eq 'Available'
Post message in Teams

Mensaje:

Daily queue status

Last assigned: Marcela
Last incident: INC13763770
Current queue order: 5

Available credits:
- Cristian: AfterHours - INC13686579
- David: Weekend - INC13689939
- Marcela: MergedEmail - CODE123

Reminder:
Normal incidents must be registered as EntryType = Normal.
External assignments must be registered as India, Weekend, AfterHours or MergedEmail.

13. Lo que cambia frente al diseño anterior

Antes	Ahora

Se manejaban prioridades	Se eliminan completamente
Incidents tenía Priority	Eliminado
Incidents tenía Reason	Eliminado
Incidents tenía ServiceNowLink	Eliminado
Había AuditLog	Eliminado
Merged era un estado genérico	Ahora es MergedEmail con EmailCode
Créditos tenían explicación libre	Se controla con CreditType, ReferenceIncident y ReferenceEmailCode


14. Pruebas ajustadas

Prueba 1: ticket normal.

IncidentNumber: INC1001
EntryType: Normal
Counted: Yes
Status: New

Resultado esperado:

FinalAssigneeName = primer miembro de la cola
Status = Assigned
QueueState avanza
Teams publica asignación

Prueba 2: India que sí cuenta.

IncidentNumber: INC2001
EntryType: India
AlreadyAssignedCode: CRISTIAN
AlreadyAssignedName: Cristian
Counted: Yes
Status: New

Resultado esperado:

Se crea crédito Available para Cristian
Status = CreditCreated
QueueState no cambia

Prueba 3: AfterHours que sí cuenta.

IncidentNumber: INC3001
EntryType: AfterHours
AlreadyAssignedCode: DAVID
AlreadyAssignedName: David
Counted: Yes
Status: New

Resultado esperado:

Se crea crédito Available para David
QueueState no cambia

Prueba 4: Merged recibido por correo que no cuenta.

IncidentNumber: EMAIL-001
EntryType: MergedEmail
EmailCode: MERGED-CODE-123
AlreadyAssignedCode: CRISTIAN
AlreadyAssignedName: Cristian
Counted: No
Status: New

Resultado esperado:

No se crea crédito
Status = Merged
QueueState no cambia
Teams informa que no impacta la cola

Prueba 5: consumo de crédito.

Si Cristian tiene crédito y le toca en cola:

Cristian se salta
Su crédito pasa a Consumed
El ticket normal se asigna al siguiente miembro
QueueState avanza hasta el asignado real

15. Regla operativa final para el equipo

La regla queda más corta:

1. Ticket normal: EntryType = Normal.
2. Ticket asignado por India: EntryType = India.
3. Ticket de fin de semana: EntryType = Weekend.
4. Ticket de 6 p. m. a 7 p. m.: EntryType = AfterHours.
5. Requisito/caso merged recibido por correo: EntryType = MergedEmail y EmailCode obligatorio.
6. Solo EntryType = Normal mueve la cola.
7. India, Weekend, AfterHours y MergedEmail no mueven la cola.
8. Esos casos solo generan crédito si Counted = Yes.
9. MergedEmail por defecto debe ser Counted = No.
10. Los créditos se consumen automáticamente cuando a esa persona le vuelve a tocar.

Mi observación directa: quitar auditoría está bien para arrancar rápido, pero no resuelve el problema cultural de fondo. Si alguien cambia Counted, QueueState o créditos manualmente, no habrá forma clara de reconstruir lo ocurrido. Para MVP sirve; para operación estable, es débil.
