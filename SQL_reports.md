Para automatizar la asignación/eliminación del rol **Browser** a un usuario en **Paginated Reports de SQL Server 2019 Reporting Services (SSRS)** usando **Jenkins/Groovy**, debes interactuar con la **API de SSRS** (SOAP o REST). El proceso manual que describes se puede replicar programáticamente mediante los endpoints de seguridad (`Policies`).

## 🔑 Enfoque recomendado

SSRS 2019 expone dos APIs:
- **SOAP** (`ReportService2010.asmx`): Soporta gestión completa de políticas (recomendado para este caso).
- **REST** (`/api/v2.0/`): Limitado para gestión de roles en carpetas/reports (no soporta políticas granulares en todas las versiones).

**Usaremos SOAP** ya que permite manipular políticas con precisión mediante los métodos `GetPolicies` y `SetPolicies`.

---

## ✅ Solución con Groovy en Jenkins

### 1. Requisitos previos
- Habilitar autenticación **Windows/NTLM** o **Basic Auth** en SSRS.
- Almacenar credenciales en **Jenkins Credentials** (tipo `Username with password`).
- Tener instalado el plugin **HTTP Request Plugin** o usar librerías nativas de Groovy.

### 2. Script Groovy (ejecutable en Jenkins Pipeline)

```groovy
@Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7.1')
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.POST
import static groovyx.net.http.ContentType.TEXT
import groovy.xml.XmlUtil
import jenkins.model.Jenkins
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import org.jenkinsci.plugins.plaincredentials.StringCredentials

// ================= CONFIGURACIÓN =================
def reportServerUrl = 'http://tu-servidor-ssrs/ReportServer' // Sin /ReportService2010.asmx
def folderPath = '/TuCarpeta/PaginatedReports'                // Ruta del folder/report
def targetUser = 'DOMINIO\\usuario'                           // Usuario a gestionar
def assignRole = true // true = asignar Browser, false = eliminar
def credentialId = 'ssrs-credentials'                         // ID credencial Jenkins

// ================= OBTENER CREDENCIALES =================
def creds = CredentialsProvider.findCredentialById(
    credentialId,
    StandardUsernamePasswordCredentials.class,
    Jenkins.instance
)
if (!creds) {
    error "Credencial '${credentialId}' no encontrada en Jenkins"
}
def username = creds.username
def password = creds.password.getPlainText()

// ================= FUNCIONES SOAP =================
def createSoapEnvelope(action, body) {
    return """<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xmlns:xsd="http://www.w3.org/2001/XMLSchema">
  <soap:Header>
    <ServerInfoHeader xmlns="http://schemas.microsoft.com/sqlserver/2010/03/20/ReportServer" />
  </soap:Header>
  <soap:Body>
    ${body}
  </soap:Body>
</soap:Envelope>"""
}

def callSoapService(endpoint, soapAction, envelope) {
    def http = new HTTPBuilder("${reportServerUrl}/${endpoint}")
    http.auth.basic username, password
    
    def response = http.request(POST, TEXT) {
        headers.'SOAPAction' = "\"http://schemas.microsoft.com/sqlserver/2010/03/20/ReportServer/${soapAction}\""
        headers.'Content-Type' = 'text/xml; charset=utf-8'
        body = envelope
        
        response.success = { resp, reader ->
            return reader.text
        }
        response.failure = { resp, reader ->
            error "SOAP Error ${resp.status}: ${reader?.text ?: 'Sin respuesta'}"
        }
    }
    return response
}

// ================= PASO 1: Obtener políticas actuales =================
def getExistingPolicies() {
    def body = """<GetPolicies xmlns="http://schemas.microsoft.com/sqlserver/2010/03/20/ReportServer">
                    <ItemPath>${folderPath}</ItemPath>
                  </GetPolicies>"""
    def envelope = createSoapEnvelope('GetPolicies', body)
    def rawResponse = callSoapService('ReportService2010.asmx', 'GetPolicies', envelope)
    
    // Parsear XML respuesta
    def xml = new XmlSlurper().parseText(rawResponse)
    xml.declareNamespace(soap: 'http://schemas.xmlsoap.org/soap/envelope/')
    xml.declareNamespace(rs: 'http://schemas.microsoft.com/sqlserver/2010/03/20/ReportServer')
    
    return xml.'**'.findAll { it.name() == 'Policy' }?.collect { policy ->
        [
            GroupUserName: policy.GroupUserName.text(),
            Roles: policy.Roles.Role.Name.collect { it.text() }
        ]
    } ?: []
}

// ================= PASO 2: Modificar políticas =================
def updatePolicies(existingPolicies) {
    def policies = existingPolicies.collect { it } // Clonar
    
    if (assignRole) {
        // Asignar rol Browser
        def existing = policies.find { it.GroupUserName == targetUser }
        if (existing) {
            if (!existing.Roles.contains('Browser')) {
                existing.Roles << 'Browser'
            }
        } else {
            policies << [
                GroupUserName: targetUser,
                Roles: ['Browser']
            ]
        }
    } else {
        // Eliminar rol Browser
        policies = policies.collect { policy ->
            if (policy.GroupUserName == targetUser) {
                policy.Roles = policy.Roles - 'Browser'
            }
            return policy
        }.findAll { !(it.GroupUserName == targetUser && it.Roles.empty) } // Eliminar si sin roles
    }
    
    return policies
}

// ================= PASO 3: Aplicar políticas =================
def applyPolicies(policies) {
    def policyXml = policies.collect { p ->
        "<Policy>" +
        "<GroupUserName>${XmlUtil.escapeXml(p.GroupUserName)}</GroupUserName>" +
        "<Roles>" +
        p.Roles.collect { "<Role><Name>${XmlUtil.escapeXml(it)}</Name></Role>" }.join('') +
        "</Roles>" +
        "</Policy>"
    }.join('')
    
    def body = """<SetPolicies xmlns="http://schemas.microsoft.com/sqlserver/2010/03/20/ReportServer">
                    <ItemPath>${folderPath}</ItemPath>
                    <Policies>${policyXml}</Policies>
                  </SetPolicies>"""
    def envelope = createSoapEnvelope('SetPolicies', body)
    callSoapService('ReportService2010.asmx', 'SetPolicies', envelope)
    return true
}

// ================= EJECUCIÓN =================
try {
    echo "Obteniendo políticas actuales de: ${folderPath}"
    def currentPolicies = getExistingPolicies()
    echo "Políticas actuales: ${currentPolicies}"
    
    echo "${assignRole ? 'Asignando' : 'Eliminando'} rol Browser para ${targetUser}"
    def updatedPolicies = updatePolicies(currentPolicies)
    echo "Nuevas políticas: ${updatedPolicies}"
    
    applyPolicies(updatedPolicies)
    echo "✅ Operación completada exitosamente"
} catch (Exception e) {
    error "❌ Falló la operación: ${e.message}"
}
```

---

## 🛠️ Pipeline Jenkins Ejemplo

```groovy
pipeline {
    agent any
    stages {
        stage('Gestionar Rol Browser en SSRS') {
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'ssrs-credentials',
                        usernameVariable: 'SSRS_USER',
                        passwordVariable: 'SSRS_PASS'
                    )]) {
                        // Ejecutar script Groovy aquí (o llamar a un archivo .groovy)
                        // El script anterior puede guardarse como manage-ssrs-role.groovy
                        // y ejecutarse con: groovy manage-ssrs-role.groovy
                    }
                }
            }
        }
    }
}
```

---

## ⚠️ Consideraciones importantes

1. **Seguridad**:
   - Nunca hardcodees credenciales. Usa **Jenkins Credentials**.
   - Si usas autenticación Windows, configura el plugin **Active Directory** o usa Kerberos en el nodo Jenkins.

2. **Rutas en SSRS**:
   - Las rutas son case-sensitive y deben empezar con `/` (ej: `/Sales/Reports`).
   - Para reports específicos: `/Carpeta/ReporteNombre`.

3. **Roles disponibles**:
   - `Browser`: Solo lectura/ejecución.
   - `Content Manager`: Administración completa.
   - `Publisher`, `Report Builder`, etc.

4. **Alternativa con PowerShell**:
   Si prefieres, puedes ejecutar desde Groovy un script PowerShell usando el módulo `ReportingServicesTools`:
   ```groovy
   bat 'powershell -File manage-ssrs.ps1 -User "DOMINIO\\usuario" -FolderPath "/Carpeta" -AssignRole $true'
   ```

5. **Firewall/Proxy**:
   Asegúrate de que el nodo Jenkins tenga acceso al puerto 80/443 del servidor SSRS.

---

## 🔍 Verificación manual post-ejecución

1. Accede a: `http://tu-servidor-ssrs/Reports`
2. Navega a la carpeta/report
3. Haz clic en **⋯ → Manage → Security**
4. Confirma que el usuario aparezca con el rol **Browser** (o sin él si eliminaste).

Con este enfoque tendrás un proceso 100% automatizado, auditable y reproducible para gestión de permisos en SSRS mediante Jenkins.
