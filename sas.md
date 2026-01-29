## Automatización del User Management en SAS 9.4: Opciones Realistas para Jenkins

**Importante contexto inicial:** SAS 9.4 **NO tiene una CLI nativa ni API REST para gestión de usuarios** como sí existe en SAS Viya (que cuenta con `sas-admin` CLI y APIs REST completas). [[4]] El proceso manual que describes (RDP → SAS Management Console → User Manager) es efectivamente el método estándar documentado para SAS 9.4. [[7]]

Sin embargo, existen alternativas para automatizar parcial o totalmente este proceso. Aquí te presento las opciones viables:

---

### 🔧 Opción 1: Scripts SAS con Metadata DATA Step Functions (Recomendado para Jenkins)

Puedes crear un script `.sas` que use funciones de metadata para:
- Crear usuarios (`METADATA_NEWOBJ` con clase `Person`)
- Asignar grupos/roles (`METADATA_ADDASSOC`)
- Crear logins con Authentication Domain "Default" (`METADATA_NEWOBJ` con clase `Login`)

**Ejemplo de estructura del script:**
```sas
/* Conexión al metadata server */
options metaserver="tu-servidor" 
        metaport=8561 
        metauser="sasadm@saspw" 
        metapass="tu-password" 
        metarepository="Foundation";

/* 1. Crear usuario (Person) */
data _null_;
  length uri $256;
  rc = metadata_newobj("Person", "NuevoUsuario", "", uri);
  rc = metadata_setattr(uri, "Name", "NuevoUsuario");
  rc = metadata_setattr(uri, "ID", "nuevousuario@dominio.com");
  rc = metadata_commit();
run;

/* 2. Asignar a grupo (ej: SASUSERS) */
data _null_;
  length groupuri useruri $256;
  /* Obtener URI del grupo SASUSERS */
  rc = metadata_getnobj("omsobj:SASUserGroup?@Name='SASUSERS'", 1, groupuri);
  /* Obtener URI del usuario creado */
  rc = metadata_getnobj("omsobj:Person?@Name='NuevoUsuario'", 1, useruri);
  /* Asociar usuario al grupo */
  rc = metadata_addassoc(groupuri, "Member", useruri);
  rc = metadata_commit();
run;

/* 3. Crear Login con Authentication Domain "Default" */
data _null_;
  length loginuri useruri authdomainuri $256;
  rc = metadata_getnobj("omsobj:Person?@Name='NuevoUsuario'", 1, useruri);
  rc = metadata_getnobj("omsobj:AuthenticationDomain?@Name='DefaultAuth'", 1, authdomainuri);
  rc = metadata_newobj("Login", "Login_NuevoUsuario", useruri, loginuri);
  rc = metadata_setattr(loginuri, "UserID", "nuevousuario"); /* userID sin @dominio */
  rc = metadata_addassoc(loginuri, "Domain", authdomainuri);
  rc = metadata_commit();
run;
```

**Integración con Jenkins:**
```groovy
pipeline {
    agent { label 'sas-server' } // Nodo con SAS instalado
    stages {
        stage('Crear Usuario SAS') {
            steps {
                sh '''
                    # Ejecutar script SAS en batch mode
                    /opt/sas/sas94/SASFoundation/9.4/sas -sysin /ruta/user_creation.sas \
                      -log /ruta/user_creation.log \
                      -config /opt/sas/config/Lev1/SASApp/sasv9.cfg
                '''
                // Validar éxito leyendo el log
                sh 'grep -q "ERROR:" /ruta/user_creation.log && exit 1 || exit 0'
            }
        }
    }
}
```

**Documentación relevante:**
- [SAS 9.4 Language Interfaces to Metadata](https://documentation.sas.com/doc/en/lrmeta/9.4/) - Funciones METADATA_* [[176]]
- Los macros `%MDUEXTR` sirven para **consultar** usuarios, pero no para crearlos fácilmente [[15]]

---

### 🔧 Opción 2: SAS Java Metadata Interface (JMI)

Si prefieres Java en lugar de SAS:
- Usa la librería `sas.svc.connection.jar` y `sas.metadata.remote.jar`
- Ejemplo básico:
```java
IMetadataConnection conn = new MetadataConnection();
conn.setMetadataServer("tu-servidor");
conn.setMetadataPort(8561);
conn.setMetadataUser("sasadm@saspw");
conn.setMetadataPassword("tu-password");
conn.connect();

// Crear usuario usando MdFactory
IMdFactory factory = conn.getFactory();
IOMObject user = factory.createObject("Person");
user.setStringProperty("Name", "NuevoUsuario");
user.setStringProperty("ID", "nuevousuario@dominio.com");
conn.commit();
```

**Integración con Jenkins:** Ejecuta el JAR como paso del pipeline usando `java -jar tu-app.jar`.

**Documentación:** [SAS Java Metadata Interface](https://documentation.sas.com/doc/en/omaref/9.4/) [[128]]

---

### 🔧 Opción 3: User Import Macros (Bulk Load)

SAS proporciona macros para carga masiva de usuarios desde archivos:
- Ubicación: `SASFoundation/9.4/core/sasmacro` [[234]]
- Macros principales: `%MDUEXTR` (exportar), `%MDUIMPRT` (importar)
- Requiere archivo de entrada con estructura específica (XML o CSV)

**Limitación:** Es más adecuado para migraciones masivas que para creación individual on-demand.

---

### 🔧 Opción 4: Auto-provisioning vía Web Authentication (SAS 9.4M3+)

Si usas autenticación web (CAS, LDAP, etc.):
- Los usuarios se crean **automáticamente** al primer login exitoso [[137]]
- Configuración en `web.xml` del mid-tier
- **Ventaja:** No requiere scripts manuales
- **Desventaja:** No permite asignar grupos/roles específicos durante la creación (solo grupos por defecto)

---

### ⚠️ Limitaciones Críticas de SAS 9.4

| Característica | SAS 9.4 | SAS Viya |
|---------------|---------|----------|
| CLI nativa para users | ❌ No existe | ✅ `sas-admin` |
| REST API para users | ❌ No existe | ✅ `/identities/users` |
| Herramientas batch específicas para users | ❌ Solo funciones genéricas de metadata | ✅ Comandos específicos |
| Auto-provisioning avanzado | ⚠️ Limitado a web auth | ✅ Completo con políticas |

---

### ✅ Recomendación Práctica para tu Caso

1. **Para automatización en Jenkins:**
   - Usa **Opción 1 (scripts SAS batch)** si ya tienes SAS instalado en el servidor Jenkins agent
   - Ventaja: Reutiliza tu conocimiento SAS existente
   - Desventaja: Requiere gestionar credenciales de `sasadm` de forma segura (usar Jenkins Credentials Store)

2. **Si planeas migrar a SAS Viya en el futuro:**
   - Considera evaluar SAS Viya 202x, donde el user management es nativamente automatizable con:
     ```bash
     sas-admin --output json identities create-user \
       --id nuevousuario \
       --given-name "Nuevo" \
       --family-name "Usuario"
     sas-admin identities add-members \
       --group "SASUSERS" \
       nuevousuario
     ```

3. **Herramientas de terceros:**
   - [Metacoda Plug-ins](https://www.metacoda.com/) ofrece herramientas CLI para gestión de seguridad en SAS 9.4 (de pago, pero muy robustas)

---

### 🔐 Consideraciones de Seguridad para Jenkins

- **Nunca almacenes contraseñas en texto plano** en pipelines
- Usa Jenkins Credentials Binding para `sasadm` password:
  ```groovy
  withCredentials([string(credentialsId: 'sasadm-pass', variable: 'SASPASS')]) {
      sh "sas -sysin script.sas -metapass $SASPASS ..."
  }
  ```
- Restringe el acceso al job de user management a usuarios autorizados en Jenkins
- Registra todas las operaciones de creación de usuarios para auditoría

¿Necesitas que te ayude a desarrollar un script SAS completo con las funciones de metadata para tu caso específico?














Respecto a la automatización de permisos en los Paginated Reports de SSRS, investigué la opción de usar Stored Procedures + ODBC como alternativa a SOAP.
Conclusión: No es viable ni recomendable por estos motivos de negocio:
No existen Stored Procedures oficiales para gestión de seguridad
⚠️ Riesgo alto de inconsistencias: Los cambios hechos directamente en la base de datos no se reflejan inmediatamente en SSRS, lo que podría generar permisos "fantasma" (asignados en BD pero no funcionales en la interfaz) o viceversa.
⚠️ Pérdida de soporte de Microsoft: Si detectan modificaciones directas en la base de SSRS, Microsoft podría negarse a brindar soporte técnico en caso de incidentes críticos.
⚠️ Sin auditoría: No quedaría registro en los logs oficiales de SSRS, imposibilitando rastrear quién asignó/eliminó permisos (riesgo para cumplimiento).
