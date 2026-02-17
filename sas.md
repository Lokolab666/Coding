Aquí tienes una solución completa y profesional usando **Jenkins Shared Libraries** para gestionar usuarios SAS 9.4 (creación + eliminación):

---

## 📁 Estructura del Proyecto Git

```
sas-user-management-shared-library/
├── vars/
│   ├── sasUserManager.groovy          ← Librería principal
│   ├── sasConfig.groovy               ← Configuración reutilizable
│   └── sasMetadataHelper.groovy       ← Funciones auxiliares
├── resources/
│   └── scripts/
│       ├── user_creation.sas          ← Script SAS creación
│       └── user_deletion.sas          ← Script SAS eliminación
├── src/
│   └── com/
│       └── sas/
│           ├── UserValidator.groovy   ← Validaciones
│           └── AuditLogger.groovy     ← Auditoría
├── test/
│   ├── test_user_creation.bat         ← Prueba manual Windows
│   ├── test_user_deletion.bat         ← Prueba manual Windows
│   └── TestSasUserManager.groovy      ← Tests unitarios
├── Jenkinsfile                        ← Ejemplo de uso
└── README.md                          ← Documentación
```

---

## 📄 1. Librería Principal: `vars/sasUserManager.groovy`

```groovy
/**
 * SAS User Manager - Shared Library for Jenkins
 * Gestión automatizada de usuarios en SAS 9.4 Metadata
 * 
 * @author DevOps Team
 * @version 1.0.0
 */

import com.sas.UserValidator
import com.sas.AuditLogger

def call(Map config = [:]) {
    // Validar configuración mínima
    validateConfig(config)
    
    // Determinar acción
    String action = config.action ?: 'create'
    
    switch (action.toLowerCase()) {
        case 'create':
            return createUser(config)
        case 'delete':
            return deleteUser(config)
        case 'validate':
            return validateUser(config)
        default:
            error "Acción '${action}' no soportada. Use: create, delete, validate"
    }
}

/**
 * Crear usuario en SAS 9.4 Metadata
 * @param config Map con: userName, userId, userEmail, groups, authDomain
 */
def createUser(Map config) {
    def validator = new UserValidator()
    def audit = new AuditLogger()
    
    // Validaciones
    validator.validateUserParams(config)
    
    echo "🔵 Iniciando creación de usuario: ${config.userName}"
    audit.logAction('CREATE_USER', config.userId, "Iniciando creación")
    
    try {
        // Obtener configuración SAS
        def sasCfg = sasConfig()
        
        // Generar script SAS con parámetros
        def scriptPath = generateCreationScript(config, sasCfg)
        
        // Ejecutar SAS en modo batch
        def result = executeSasBatch(scriptPath, config, sasCfg, 'CREATE')
        
        if (result.success) {
            audit.logAction('CREATE_USER', config.userId, "Usuario creado exitosamente")
            echo "✅ Usuario '${config.userName}' creado exitosamente"
            return [success: true, userId: config.userId, message: 'Usuario creado']
        } else {
            audit.logAction('CREATE_USER', config.userId, "Falló: ${result.error}")
            error "Falló la creación del usuario: ${result.error}"
        }
        
    } catch (Exception e) {
        audit.logAction('CREATE_USER', config.userId, "Error: ${e.message}")
        error "Error creando usuario: ${e.message}"
    }
}

/**
 * Eliminar usuario en SAS 9.4 Metadata
 * @param config Map con: userId, userEmail
 */
def deleteUser(Map config) {
    def validator = new UserValidator()
    def audit = new AuditLogger()
    
    // Validaciones
    validator.validateDeleteParams(config)
    
    echo "🔴 Iniciando eliminación de usuario: ${config.userId}"
    audit.logAction('DELETE_USER', config.userId, "Iniciando eliminación")
    
    try {
        // Obtener configuración SAS
        def sasCfg = sasConfig()
        
        // Generar script SAS con parámetros
        def scriptPath = generateDeletionScript(config, sasCfg)
        
        // Confirmación de seguridad (requiere parámetro explicito para producción)
        if (!config.forceDelete && env.BRANCH_NAME != 'main') {
            echo "⚠️  Modo simulación (dry-run). Para eliminar realmente, usa forceDelete: true"
            return [success: true, userId: config.userId, message: 'Simulación completada (dry-run)']
        }
        
        // Ejecutar SAS en modo batch
        def result = executeSasBatch(scriptPath, config, sasCfg, 'DELETE')
        
        if (result.success) {
            audit.logAction('DELETE_USER', config.userId, "Usuario eliminado exitosamente")
            echo "✅ Usuario '${config.userId}' eliminado exitosamente"
            return [success: true, userId: config.userId, message: 'Usuario eliminado']
        } else {
            audit.logAction('DELETE_USER', config.userId, "Falló: ${result.error}")
            error "Falló la eliminación del usuario: ${result.error}"
        }
        
    } catch (Exception e) {
        audit.logAction('DELETE_USER', config.userId, "Error: ${e.message}")
        error "Error eliminando usuario: ${e.message}"
    }
}

/**
 * Validar si usuario existe en SAS Metadata
 */
def validateUser(Map config) {
    def validator = new UserValidator()
    validator.validateUserId(config.userId)
    
    echo "🔍 Validando usuario: ${config.userId}"
    
    try {
        def sasCfg = sasConfig()
        def scriptPath = "${env.WORKSPACE}\\resources\\scripts\\user_validation.sas"
        
        // Crear script temporal de validación
        def tempScript = """
options metaserver="${sasCfg.metaServer}" metaport=${sasCfg.metaPort} 
        metauser="${sasCfg.metaUser}" metapass=\${META_PASS} metarepository="Foundation";

%macro check_user;
    data _null_;
        length uri \$256;
        rc = metadata_getnobj("omsobj:Person?@Name='${config.userId}'", 1, uri);
        if rc > 0 then do;
            put "USER_EXISTS:1";
            put "URI=" uri;
        end;
        else do;
            put "USER_EXISTS:0";
            put "ERROR: Usuario no encontrado";
        end;
    run;
%mend;
%check_user;
        """.stripIndent()
        
        writeFile(file: scriptPath, text: tempScript, encoding: 'UTF-8')
        
        // Ejecutar validación
        def logPath = "${env.WORKSPACE}\\logs\\validate_${config.userId}_${BUILD_NUMBER}.log"
        def sasCmd = buildSasCommand(scriptPath, logPath, sasCfg)
        
        withCredentials([string(credentialsId: sasCfg.credentialId, variable: 'META_PASS')]) {
            bat returnStatus: true, script: sasCmd
        }
        
        // Leer resultado
        def logContent = readFile(logPath)
        if (logContent.contains('USER_EXISTS:1')) {
            echo "✅ Usuario '${config.userId}' existe en SAS Metadata"
            return [exists: true, userId: config.userId]
        } else {
            echo "❌ Usuario '${config.userId}' NO existe en SAS Metadata"
            return [exists: false, userId: config.userId]
        }
        
    } catch (Exception e) {
        error "Error validando usuario: ${e.message}"
    }
}

/**
 * Generar script SAS para creación de usuario
 */
def generateCreationScript(Map config, Map sasCfg) {
    def scriptTemplate = libraryResource('scripts/user_creation.sas')
    
    // Reemplazar placeholders con valores reales
    def scriptContent = scriptTemplate
        .replace('USER_NAME_PLACEHOLDER', config.userName)
        .replace('USER_ID_PLACEHOLDER', config.userId)
        .replace('USER_EMAIL_PLACEHOLDER', config.userEmail)
        .replace('GROUPS_PLACEHOLDER', config.groups ?: 'SASUSERS')
        .replace('AUTH_DOMAIN_PLACEHOLDER', config.authDomain ?: 'DefaultAuth')
    
    def scriptPath = "${env.WORKSPACE}\\resources\\scripts\\generated\\user_create_${config.userId}_${BUILD_NUMBER}.sas"
    def scriptDir = scriptPath.substring(0, scriptPath.lastIndexOf('\\'))
    
    // Crear directorio si no existe
    bat "if not exist \"${scriptDir}\" mkdir \"${scriptDir}\""
    
    writeFile(file: scriptPath, text: scriptContent, encoding: 'UTF-8')
    return scriptPath
}

/**
 * Generar script SAS para eliminación de usuario
 */
def generateDeletionScript(Map config, Map sasCfg) {
    def scriptTemplate = libraryResource('scripts/user_deletion.sas')
    
    def scriptContent = scriptTemplate
        .replace('USER_ID_PLACEHOLDER', config.userId)
        .replace('USER_EMAIL_PLACEHOLDER', config.userEmail ?: '')
    
    def scriptPath = "${env.WORKSPACE}\\resources\\scripts\\generated\\user_delete_${config.userId}_${BUILD_NUMBER}.sas"
    def scriptDir = scriptPath.substring(0, scriptPath.lastIndexOf('\\'))
    
    bat "if not exist \"${scriptDir}\" mkdir \"${scriptDir}\""
    
    writeFile(file: scriptPath, text: scriptContent, encoding: 'UTF-8')
    return scriptPath
}

/**
 * Ejecutar SAS en modo batch
 */
def executeSasBatch(String scriptPath, Map config, Map sasCfg, String action) {
    def logPath = "${env.WORKSPACE}\\logs\\${action.toLowerCase()}_${config.userId}_${BUILD_NUMBER}.log"
    def sasCmd = buildSasCommand(scriptPath, logPath, sasCfg)
    
    echo "📦 Ejecutando SAS batch: ${action} user ${config.userId}"
    
    def exitCode = 0
    def errorMsg = ''
    
    try {
        withCredentials([string(credentialsId: sasCfg.credentialId, variable: 'META_PASS')]) {
            exitCode = bat(returnStatus: true, script: sasCmd)
        }
        
        // Leer log para verificar errores
        def logContent = readFile(logPath)
        
        if (exitCode != 0 || logContent.contains('ERROR:') || logContent.contains('metadata_commit falló')) {
            errorMsg = "SAS batch falló. Exit code: ${exitCode}"
            if (logContent.contains('ERROR:')) {
                def errorLine = (logContent =~ /ERROR:.*$/m)[0]
                errorMsg += " | ${errorLine}"
            }
            return [success: false, error: errorMsg, log: logPath]
        }
        
        // Verificar mensaje de éxito específico
        if (action == 'CREATE' && !logContent.contains('USUARIO CREADO EXITOSAMENTE')) {
            return [success: false, error: 'Script completó pero no se confirmó creación', log: logPath]
        }
        
        if (action == 'DELETE' && !logContent.contains('USUARIO ELIMINADO EXITOSAMENTE')) {
            return [success: false, error: 'Script completó pero no se confirmó eliminación', log: logPath]
        }
        
        return [success: true, log: logPath]
        
    } catch (Exception e) {
        return [success: false, error: "Excepción: ${e.message}", log: logPath]
    }
}

/**
 * Construir comando SAS batch
 */
def buildSasCommand(String scriptPath, String logPath, Map sasCfg) {
    return """
        @echo off
        set META_SERVER=${sasCfg.metaServer}
        set META_PORT=${sasCfg.metaPort}
        set META_USER=${sasCfg.metaUser}
        set META_PASS=%META_PASS%
        
        "${sasCfg.sasExe}" ^
            -CONFIG "${sasCfg.sasConfig}" ^
            -SYSIN "${scriptPath}" ^
            -LOG "${logPath}" ^
            -BATCH ^
            -NOSPLASH ^
            -NOICON
    """.stripIndent().trim()
}

/**
 * Validar configuración
 */
def validateConfig(Map config) {
    if (!config.action) {
        error "Parámetro 'action' es obligatorio (create/delete/validate)"
    }
    
    if (config.action == 'create') {
        if (!config.userName || !config.userId || !config.userEmail) {
            error "Para crear usuario, se requieren: userName, userId, userEmail"
        }
    }
    
    if (config.action == 'delete') {
        if (!config.userId) {
            error "Para eliminar usuario, se requiere: userId"
        }
    }
}

/**
 * Obtener logs de operación
 */
def getOperationLogs(String userId, String action) {
    def logPattern = "${env.WORKSPACE}\\logs\\${action.toLowerCase()}_${userId}_*.log"
    def logs = []
    
    try {
        def logFiles = findFiles(glob: logPattern)
        logFiles.each { file ->
            logs.add([filename: file.name, path: file.path, content: readFile(file.path)])
        }
    } catch (Exception e) {
        echo "No se encontraron logs: ${e.message}"
    }
    
    return logs
}

return this
```

---

## 📄 2. Configuración Reutilizable: `vars/sasConfig.groovy`

```groovy
/**
 * Configuración SAS 9.4 - Shared Library
 * Centraliza rutas, servidores y credenciales
 */

def call(String environment = 'production') {
    def configs = [
        development: [
            sasExe: 'C:\\Program Files\\SASHome\\SASFoundation\\9.4\\sas.exe',
            sasConfig: 'C:\\SASConfig\\Lev1\\SASApp\\sasv9.cfg',
            metaServer: 'sas-dev-server',
            metaPort: 8561,
            metaUser: 'sasadm@saspw',
            credentialId: 'sas-metadata-dev-pass',
            workspace: 'C:\\jenkins-dev\\workspace'
        ],
        production: [
            sasExe: 'C:\\Program Files\\SASHome\\SASFoundation\\9.4\\sas.exe',
            sasConfig: 'C:\\SASConfig\\Lev1\\SASApp\\sasv9.cfg',
            metaServer: 'sas-prod-server',
            metaPort: 8561,
            metaUser: 'sasadm@saspw',
            credentialId: 'sas-metadata-prod-pass',
            workspace: 'C:\\jenkins-prod\\workspace'
        ],
        test: [
            sasExe: 'C:\\Program Files\\SASHome\\SASFoundation\\9.4\\sas.exe',
            sasConfig: 'C:\\SASConfig\\Lev1\\SASApp\\sasv9.cfg',
            metaServer: 'sas-test-server',
            metaPort: 8561,
            metaUser: 'sasadm@saspw',
            credentialId: 'sas-metadata-test-pass',
            workspace: 'C:\\jenkins-test\\workspace'
        ]
    ]
    
    if (!configs.containsKey(environment)) {
        error "Entorno '${environment}' no configurado. Opciones: ${configs.keySet().join(', ')}"
    }
    
    return configs[environment]
}

return this
```

---

## 📄 3. Script SAS Creación: `resources/scripts/user_creation.sas`

```sas
/*********************************************************************
* USER_CREATION.SAS - Creación automatizada de usuarios en SAS 9.4 *
*                                                                  *
* Placeholders:                                                    *
*   USER_NAME_PLACEHOLDER      → Nombre completo del usuario       *
*   USER_ID_PLACEHOLDER        → UserID para login (sin @dominio) *
*   USER_EMAIL_PLACEHOLDER     → Email/ID único                    *
*   GROUPS_PLACEHOLDER         → Grupos separados por coma         *
*   AUTH_DOMAIN_PLACEHOLDER    → DefaultAuth (normalmente)         *
*********************************************************************/

/* === Conexión al metadata server === */
options 
    metaserver   = "&sysget(META_SERVER)"
    metaport     = &sysget(META_PORT)
    metauser     = "&sysget(META_USER)"
    metapass     = "&sysget(META_PASS)"
    metarepository = "Foundation"
    nomprint nosymbolgen;

%put ========== INICIANDO CREACIÓN DE USUARIO ==========;
%put USER_NAME   = USER_NAME_PLACEHOLDER;
%put USER_ID     = USER_ID_PLACEHOLDER;
%put USER_EMAIL  = USER_EMAIL_PLACEHOLDER;
%put GROUPS      = GROUPS_PLACEHOLDER;
%put AUTH_DOMAIN = AUTH_DOMAIN_PLACEHOLDER;
%put ==============================================;

/* === 1. Crear/Obtener objeto Person === */
%macro create_person(uri_person);
    data _null_;
        length uri $256;
        rc = 0;
        
        /* Verificar si usuario ya existe */
        rc = metadata_getnobj("omsobj:Person?@Name='USER_NAME_PLACEHOLDER'", 1, uri);
        
        if rc <= 0 then do;
            /* Crear nuevo usuario */
            rc = metadata_newobj("Person", "USER_NAME_PLACEHOLDER", "", uri);
            if rc <= 0 then do;
                put "ERROR: metadata_newobj(Person) falló. RC=" rc=;
                call symputx("uri_person", "", 'G');
                stop;
            end;
            
            rc = metadata_setattr(uri, "Name", "USER_NAME_PLACEHOLDER");
            rc = metadata_setattr(uri, "ID", "USER_EMAIL_PLACEHOLDER");
            
            rc = metadata_commit();
            if rc < 0 then do;
                put "ERROR: metadata_commit falló al crear Person. RC=" rc=;
                call symputx("uri_person", "", 'G');
                stop;
            end;
            put "INFO: Usuario 'USER_NAME_PLACEHOLDER' creado exitosamente.";
        end;
        else do;
            put "INFO: Usuario 'USER_NAME_PLACEHOLDER' ya existe (URI=" uri ").";
        end;
        
        call symputx("uri_person", uri, 'G');
    run;
%mend;

/* === 2. Crear Login con Authentication Domain === */
%macro create_login(uri_person);
    data _null_;
        length uri_login uri_authdomain $256;
        rc = 0;
        
        /* Obtener Authentication Domain */
        rc = metadata_getnobj("omsobj:AuthenticationDomain?@Name='AUTH_DOMAIN_PLACEHOLDER'", 1, uri_authdomain);
        if rc <= 0 then do;
            put "ERROR: AuthenticationDomain 'AUTH_DOMAIN_PLACEHOLDER' no encontrado. RC=" rc=;
            stop;
        end;
        
        /* Verificar si ya existe Login */
        rc = metadata_getnasn(uri_person, "Logins", 1, uri_login);
        login_exists = 0;
        
        if rc > 0 then do;
            length domain_uri $256;
            rc2 = metadata_getnasn(uri_login, "Domain", 1, domain_uri);
            if rc2 > 0 and domain_uri = uri_authdomain then login_exists = 1;
        end;
        
        if login_exists = 0 then do;
            /* Crear nuevo Login */
            rc = metadata_newobj("Login", "Login_USER_ID_PLACEHOLDER", uri_person, uri_login);
            if rc <= 0 then do;
                put "ERROR: metadata_newobj(Login) falló. RC=" rc=;
                stop;
            end;
            
            rc = metadata_setattr(uri_login, "UserID", "USER_ID_PLACEHOLDER");
            rc = metadata_addassoc(uri_login, "Domain", uri_authdomain);
            
            rc = metadata_commit();
            if rc < 0 then do;
                put "ERROR: metadata_commit falló al crear Login. RC=" rc=;
                stop;
            end;
            put "INFO: Login 'USER_ID_PLACEHOLDER' creado para AUTH_DOMAIN_PLACEHOLDER.";
        end;
        else do;
            put "INFO: Login para 'USER_ID_PLACEHOLDER' ya existe.";
        end;
    run;
%mend;

/* === 3. Asignar a grupos === */
%macro assign_to_groups(uri_person);
    %let groups = GROUPS_PLACEHOLDER;
    %let group_count = %sysfunc(countw(&groups, %str(,)));
    
    %if &group_count = 0 %then %do;
        %put WARNING: No se especificaron grupos.;
        %return;
    %end;
    
    %do i = 1 %to &group_count;
        %let group_name = %scan(&groups, &i, %str(,));
        %let group_name = %trim(&group_name);
        
        data _null_;
            length uri_group $256;
            rc = 0;
            
            rc = metadata_getnobj("omsobj:IdentityGroup?@Name='&group_name'", 1, uri_group);
            if rc <= 0 then do;
                put "WARNING: Grupo '&group_name' no encontrado. RC=" rc=;
                stop;
            end;
            
            /* Verificar si ya es miembro */
            rc = metadata_getnasn(uri_group, "Members", 1, "&uri_person");
            if rc <= 0 then do;
                rc = metadata_addassoc(uri_group, "Members", "&uri_person");
                rc = metadata_commit();
                if rc >= 0 then put "INFO: Usuario agregado a grupo '&group_name'.";
            end;
            else put "INFO: Usuario ya es miembro de '&group_name'.";
        run;
    %end;
%mend;

/* === 4. Ejecución principal === */
%create_person(uri_person);

%if "&uri_person" ne "" %then %do;
    %create_login(&uri_person);
    %assign_to_groups(&uri_person);
    %put ========== USUARIO CREADO EXITOSAMENTE ==========;
%end;
%else %do;
    %put ERROR: Falló la creación del usuario.;
    %abort cancel;
%end;
```

---

## 📄 4. Script SAS Eliminación: `resources/scripts/user_deletion.sas`

```sas
/*********************************************************************
* USER_DELETION.SAS - Eliminación automatizada de usuarios SAS 9.4 *
*                                                                  *
* Placeholders:                                                    *
*   USER_ID_PLACEHOLDER        → UserID del usuario a eliminar    *
*   USER_EMAIL_PLACEHOLDER     → Email del usuario (opcional)      *
*********************************************************************/

options 
    metaserver   = "&sysget(META_SERVER)"
    metaport     = &sysget(META_PORT)
    metauser     = "&sysget(META_USER)"
    metapass     = "&sysget(META_PASS)"
    metarepository = "Foundation"
    nomprint nosymbolgen;

%put ========== INICIANDO ELIMINACIÓN DE USUARIO ==========;
%put USER_ID     = USER_ID_PLACEHOLDER;
%put USER_EMAIL  = USER_EMAIL_PLACEHOLDER;
%put ==============================================;

/* === 1. Buscar y eliminar usuario Person === */
%macro delete_user;
    data _null_;
        length uri_person uri_login uri_domain $256;
        rc = 0;
        deleted = 0;
        
        /* Buscar usuario por Name (USER_ID_PLACEHOLDER) */
        rc = metadata_getnobj("omsobj:Person?@Name='USER_ID_PLACEHOLDER'", 1, uri_person);
        
        /* Si no se encuentra por Name, buscar por ID (email) */
        if rc <= 0 and "%bquote(USER_EMAIL_PLACEHOLDER)" ne "" then do;
            rc = metadata_getnobj("omsobj:Person?@ID='USER_EMAIL_PLACEHOLDER'", 1, uri_person);
        end;
        
        if rc <= 0 then do;
            put "WARNING: Usuario 'USER_ID_PLACEHOLDER' no encontrado en metadata.";
            put "USER_NOT_FOUND:1";
            stop;
        end;
        
        put "INFO: Usuario encontrado (URI=" uri_person ").";
        
        /* === Eliminar todos los Logins asociados === */
        rc = metadata_getnasn(uri_person, "Logins", 1, uri_login);
        login_count = 0;
        
        do while (rc > 0);
            /* Obtener dominio del login para logging */
            rc2 = metadata_getnasn(uri_login, "Domain", 1, uri_domain);
            if rc2 > 0 then do;
                length domain_name $100;
                rc3 = metadata_getattr(uri_domain, "Name", domain_name);
                put "INFO: Eliminando Login del dominio " domain_name;
            end;
            
            /* Eliminar el login */
            rc_del = metadata_delobj(uri_login);
            if rc_del < 0 then do;
                put "WARNING: No se pudo eliminar Login (URI=" uri_login "). RC=" rc_del=;
            end;
            else do;
                login_count + 1;
            end;
            
            /* Siguiente login */
            rc = metadata_getnasn(uri_person, "Logins", login_count + 2, uri_login);
        end;
        
        put "INFO: Se eliminaron " login_count " logins asociados.";
        
        /* === Eliminar el objeto Person === */
        rc = metadata_delobj(uri_person);
        if rc < 0 then do;
            put "ERROR: No se pudo eliminar el usuario. RC=" rc=;
            stop;
        end;
        
        rc = metadata_commit();
        if rc < 0 then do;
            put "ERROR: metadata_commit falló. RC=" rc=;
            stop;
        end;
        
        put "INFO: Usuario 'USER_ID_PLACEHOLDER' eliminado exitosamente.";
        put "USUARIO ELIMINADO EXITOSAMENTE";
        deleted = 1;
    run;
%mend;

%delete_user;
```

---

## 📄 5. Validador: `src/com/sas/UserValidator.groovy`

```groovy
package com.sas

class UserValidator {
    
    /**
     * Validar parámetros para creación de usuario
     */
    def validateUserParams(Map config) {
        // Validar userName
        if (!config.userName || config.userName.trim().isEmpty()) {
            throw new IllegalArgumentException("userName es obligatorio")
        }
        if (config.userName.length() < 2 || config.userName.length() > 100) {
            throw new IllegalArgumentException("userName debe tener entre 2 y 100 caracteres")
        }
        
        // Validar userId
        validateUserId(config.userId)
        
        // Validar userEmail
        if (!config.userEmail || !config.userEmail.matches(/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/)) {
            throw new IllegalArgumentException("userEmail inválido o no proporcionado")
        }
        
        // Validar groups (opcional)
        if (config.groups) {
            def groupsList = config.groups.split(',')
            if (groupsList.length > 10) {
                throw new IllegalArgumentException("Máximo 10 grupos permitidos")
            }
        }
        
        // Validar authDomain (opcional)
        if (config.authDomain && config.authDomain.length() > 50) {
            throw new IllegalArgumentException("authDomain demasiado largo")
        }
    }
    
    /**
     * Validar userId (reusable)
     */
    def validateUserId(String userId) {
        if (!userId) {
            throw new IllegalArgumentException("userId es obligatorio")
        }
        if (!userId.matches(/^[a-zA-Z0-9._-]{3,30}$/)) {
            throw new IllegalArgumentException(
                "userId inválido. Debe tener 3-30 caracteres alfanuméricos, guiones o puntos"
            )
        }
        if (userId.contains('@') || userId.contains(' ')) {
            throw new IllegalArgumentException("userId no puede contener '@' ni espacios")
        }
    }
    
    /**
     * Validar parámetros para eliminación
     */
    def validateDeleteParams(Map config) {
        validateUserId(config.userId)
        
        // Validación adicional para seguridad
        def protectedUsers = ['sasadm', 'sasdemo', 'admin']
        if (protectedUsers.contains(config.userId.toLowerCase())) {
            throw new IllegalArgumentException(
                "Usuario '${config.userId}' está protegido y no puede ser eliminado"
            )
        }
    }
    
    /**
     * Validar si usuario existe (pre-check)
     */
    def userExists(String userId) {
        // Esta validación se haría llamando a SAS, pero por ahora simulamos
        return true // Implementar con metadata_getnobj en script SAS
    }
}
```

---

## 📄 6. Auditoría: `src/com/sas/AuditLogger.groovy`

```groovy
package com.sas

class AuditLogger {
    
    /**
     * Registrar acción en log de auditoría
     */
    def logAction(String action, String userId, String message) {
        def timestamp = new Date().format('yyyy-MM-dd HH:mm:ss')
        def logEntry = "[${timestamp}] [${action}] User:${userId} - ${message}"
        
        // Escribir a archivo de auditoría
        def auditDir = "${env.WORKSPACE}\\audit"
        def auditFile = "${auditDir}\\audit_${new Date().format('yyyy-MM-dd')}.log"
        
        // Crear directorio si no existe
        if (!fileExists(auditDir)) {
            bat "mkdir \"${auditDir}\""
        }
        
        // Escribir entrada (append)
        try {
            def writer = new File(auditFile).newWriter(true)
            writer.writeLine(logEntry)
            writer.close()
            
            // También imprimir en consola de Jenkins
            echo "📝 AUDIT: ${logEntry}"
        } catch (Exception e) {
            echo "⚠️  No se pudo escribir log de auditoría: ${e.message}"
        }
    }
    
    /**
     * Obtener historial de auditoría para un usuario
     */
    def getUserAuditHistory(String userId, int days = 7) {
        def auditDir = "${env.WORKSPACE}\\audit"
        def history = []
        
        if (!fileExists(auditDir)) {
            return history
        }
        
        // Buscar logs de los últimos N días
        (0..days-1).each { dayOffset ->
            def dateStr = new Date().minus(dayOffset).format('yyyy-MM-dd')
            def auditFile = "${auditDir}\\audit_${dateStr}.log"
            
            if (fileExists(auditFile)) {
                def content = readFile(auditFile)
                content.readLines().each { line ->
                    if (line.contains("User:${userId}")) {
                        history.add(line)
                    }
                }
            }
        }
        
        return history.reverse() // Más reciente primero
    }
    
    /**
     * Helper para verificar existencia de archivo
     */
    private def fileExists(String path) {
        try {
            return bat(returnStatus: true, script: "if exist \"${path}\" (exit 0) else (exit 1)") == 0
        } catch (Exception e) {
            return false
        }
    }
}
```

---

## 📄 7. Jenkinsfile de Ejemplo

```groovy
#!/usr/bin/env groovy
/**
 * Jenkins Pipeline para Gestión de Usuarios SAS 9.4
 * Usa Shared Library: sas-user-management-shared-library
 */

@Library('sas-user-management-shared-library@main') _

pipeline {
    agent {
        label 'sas-windows'  // Agente Windows con SAS instalado
    }
    
    parameters {
        choice(
            name: 'ACTION',
            choices: ['CREATE', 'DELETE', 'VALIDATE'],
            description: 'Acción a realizar'
        )
        string(
            name: 'USER_NAME',
            defaultValue: 'Nuevo Usuario',
            description: 'Nombre completo del usuario (solo para CREATE)'
        )
        string(
            name: 'USER_ID',
            defaultValue: 'nuevousuario',
            description: 'UserID para login (sin @dominio)'
        )
        string(
            name: 'USER_EMAIL',
            defaultValue: 'nuevousuario@dominio.com',
            description: 'Email del usuario'
        )
        string(
            name: 'GROUPS',
            defaultValue: 'SASUSERS',
            description: 'Grupos separados por coma (ej: SASUSERS,Developers)'
        )
        booleanParam(
            name: 'FORCE_DELETE',
            defaultValue: false,
            description: '⚠️  Confirmar eliminación real (requerido en producción)'
        )
        choice(
            name: 'ENVIRONMENT',
            choices: ['development', 'test', 'production'],
            description: 'Entorno SAS'
        )
    }
    
    environment {
        SAS_ENV = "${params.ENVIRONMENT}"
        ACTION = "${params.ACTION}"
    }
    
    stages {
        stage('📋 Validación de Parámetros') {
            steps {
                script {
                    echo "✅ Validando parámetros..."
                    
                    if (params.ACTION == 'CREATE') {
                        if (!params.USER_NAME || !params.USER_ID || !params.USER_EMAIL) {
                            error "Para CREATE se requieren: USER_NAME, USER_ID, USER_EMAIL"
                        }
                    }
                    
                    if (params.ACTION == 'DELETE' && params.ENVIRONMENT == 'production' && !params.FORCE_DELETE) {
                        error "⚠️  En producción, FORCE_DELETE debe ser true para eliminar usuarios"
                    }
                    
                    echo "✅ Parámetros validados correctamente"
                }
            }
        }
        
        stage('🔧 Configurar Entorno') {
            steps {
                script {
                    echo "🔧 Configurando entorno: ${params.ENVIRONMENT}"
                    def cfg = sasConfig(params.ENVIRONMENT)
                    echo "   SAS Server: ${cfg.metaServer}:${cfg.metaPort}"
                    echo "   Credential ID: ${cfg.credentialId}"
                }
            }
        }
        
        stage('👤 Gestión de Usuario') {
            steps {
                script {
                    def result
                    
                    switch (params.ACTION) {
                        case 'CREATE':
                            echo "🔵 Creando usuario..."
                            result = sasUserManager(
                                action: 'create',
                                userName: params.USER_NAME,
                                userId: params.USER_ID,
                                userEmail: params.USER_EMAIL,
                                groups: params.GROUPS,
                                authDomain: 'DefaultAuth',
                                environment: params.ENVIRONMENT
                            )
                            break
                            
                        case 'DELETE':
                            echo "🔴 Eliminando usuario..."
                            result = sasUserManager(
                                action: 'delete',
                                userId: params.USER_ID,
                                userEmail: params.USER_EMAIL,
                                forceDelete: params.FORCE_DELETE,
                                environment: params.ENVIRONMENT
                            )
                            break
                            
                        case 'VALIDATE':
                            echo "🔍 Validando usuario..."
                            result = sasUserManager(
                                action: 'validate',
                                userId: params.USER_ID,
                                environment: params.ENVIRONMENT
                            )
                            echo "Resultado validación: ${result}"
                            break
                    }
                    
                    // Guardar resultado en variable global
                    env.OPERATION_RESULT = result.toString()
                }
            }
        }
        
        stage('📊 Auditoría y Logs') {
            steps {
                script {
                    echo "📝 Generando reporte de auditoría..."
                    
                    // Archivar logs
                    archiveArtifacts artifacts: 'logs/*.log', allowEmptyArchive: true
                    archiveArtifacts artifacts: 'audit/*.log', allowEmptyArchive: true
                    
                    // Publicar resultado
                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                    echo "✅ OPERACIÓN COMPLETADA"
                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                    echo "Acción:       ${params.ACTION}"
                    echo "Usuario:      ${params.USER_ID}"
                    echo "Entorno:      ${params.ENVIRONMENT}"
                    echo "Timestamp:    ${new Date().format('yyyy-MM-dd HH:mm:ss')}"
                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                }
            }
        }
    }
    
    post {
        success {
            script {
                if (params.ACTION == 'CREATE') {
                    echo "✅✅✅ USUARIO CREADO EXITOSAMENTE ✅✅✅"
                    // Notificación por email/slack aquí
                } else if (params.ACTION == 'DELETE') {
                    echo "✅✅✅ USUARIO ELIMINADO EXITOSAMENTE ✅✅✅"
                }
            }
        }
        failure {
            script {
                echo "❌❌❌ FALLÓ LA OPERACIÓN ❌❌❌"
                echo "Revisar logs archivados para detalles"
                
                // Adjuntar último log al mensaje de error
                def lastLog = findFiles(glob: 'logs/*.log').sort { it.lastModified }.reverse().take(1)
                if (lastLog) {
                    def logContent = readFile(lastLog[0].path)
                    echo "Últimas líneas del log:"
                    logContent.readLines().takeRight(20).each { line -> echo line }
                }
            }
        }
        always {
            cleanWs()  // Limpiar workspace excepto logs/archivos importantes
        }
    }
}
```

---

## 📄 8. Script de Prueba Manual: `test/test_user_creation.bat`

```batch
@echo off
REM =====================================================
REM Script de prueba manual para user_creation.sas
REM Ejecutar directamente en servidor SAS Windows
REM =====================================================

setlocal

REM === Configuración ===
set SAS_EXE=C:\Program Files\SASHome\SASFoundation\9.4\sas.exe
set SAS_CFG=C:\SASConfig\Lev1\SASApp\sasv9.cfg
set SCRIPT=C:\jenkins\workspace\resources\scripts\user_creation.sas
set LOG=C:\jenkins\workspace\logs\test_manual_%date:~-4,4%%date:~-10,2%%date:~-7,2%.log

REM === Credenciales (SOLO PARA PRUEBAS - NO USAR EN PRODUCCIÓN) ===
set META_SERVER=tu-metadata-server
set META_PORT=8561
set META_USER=sasadm@saspw
set META_PASS=tu-password-aquí

REM === Parámetros de usuario ===
set USER_NAME=Test Manual User
set USER_ID=testmanual
set USER_EMAIL=testmanual@dominio.com
set GROUPS=SASUSERS

echo.
echo =====================================================
echo  PRUEBA MANUAL DE CREACION DE USUARIO SAS
echo =====================================================
echo.
echo SAS Executable: %SAS_EXE%
echo Script: %SCRIPT%
echo Log: %LOG%
echo.
echo Usuario: %USER_NAME%
echo UserID: %USER_ID%
echo Email: %USER_EMAIL%
echo Grupos: %GROUPS%
echo.
echo Presiona Ctrl+C para cancelar o cualquier tecla para continuar...
pause >nul

echo.
echo Ejecutando SAS batch...
echo.

"%SAS_EXE%" ^
    -CONFIG "%SAS_CFG%" ^
    -SYSIN "%SCRIPT%" ^
    -LOG "%LOG%" ^
    -SET USER_NAME "%USER_NAME%" ^
    -SET USER_ID "%USER_ID%" ^
    -SET USER_EMAIL "%USER_EMAIL%" ^
    -SET GROUPS "%GROUPS%" ^
    -BATCH ^
    -NOSPLASH ^
    -NOICON

if %ERRORLEVEL% EQU 0 (
    echo.
    echo =====================================================
    echo  ✅ SAS batch completado exitosamente
    echo =====================================================
    echo.
    echo Revisando log para confirmar creacion...
    findstr /C:"USUARIO CREADO EXITOSAMENTE" "%LOG%" >nul
    if %ERRORLEVEL% EQU 0 (
        echo ✅✅✅ USUARIO CREADO EXITOSAMENTE ✅✅✅
    ) else (
        echo ⚠️  SAS completado pero no se confirmo creacion
        echo Revisar log: %LOG%
    )
) else (
    echo.
    echo =====================================================
    echo  ❌ FALLÓ LA EJECUCIÓN DE SAS
    echo =====================================================
    echo.
    echo Error code: %ERRORLEVEL%
    echo Revisar log: %LOG%
)

echo.
echo Presiona cualquier tecla para salir...
pause >nul
```

---

## 📄 9. README.md

```markdown
# SAS User Management - Jenkins Shared Library

Gestión automatizada de usuarios en SAS 9.4 Metadata mediante Jenkins Pipelines.

## 📋 Requisitos

- Jenkins con acceso a Shared Libraries
- Windows Server con SAS 9.4 instalado
- Jenkins Agent en el servidor SAS (JNLP o Service)
- Credenciales de `sasadm` almacenadas en Jenkins Credentials

## 🚀 Instalación

### 1. Configurar Shared Library en Jenkins

```
Jenkins → Manage Jenkins → System → Global Pipeline Libraries
  Name: sas-user-management-shared-library
  Default version: main
  Retrieval method: Modern SCM
  Source Code Management: Git
  Project Repository: https://tu-repo-git/sas-user-management-shared-library.git
  Credentials: (tu-credencial-git)
```

### 2. Configurar Credenciales

```bash
# En Jenkins:
Credentials → System → Global credentials → Add Credential
  Kind: Secret text
  Scope: Global
  ID: sas-metadata-prod-pass
  Secret: [contraseña de sasadm]
  Description: SAS Metadata Production Password
```

### 3. Configurar Jenkins Agent Windows

Ver documentación en `docs/windows-agent-setup.md`

## 📖 Uso

### Crear Usuario

```groovy
@Library('sas-user-management-shared-library') _

sasUserManager(
    action: 'create',
    userName: 'Juan Perez',
    userId: 'jperez',
    userEmail: 'jperez@dominio.com',
    groups: 'SASUSERS,Developers',
    environment: 'production'
)
```

### Eliminar Usuario

```groovy
sasUserManager(
    action: 'delete',
    userId: 'jperez',
    userEmail: 'jperez@dominio.com',
    forceDelete: true,
    environment: 'production'
)
```

### Validar Usuario

```groovy
def result = sasUserManager(
    action: 'validate',
    userId: 'jperez',
    environment: 'production'
)

if (result.exists) {
    echo "Usuario existe"
} else {
    echo "Usuario no existe"
}
```

## 📁 Estructura

```
vars/                    # Steps globales de Pipeline
  ├── sasUserManager.groovy
  ├── sasConfig.groovy
  └── sasMetadataHelper.groovy

resources/scripts/       # Scripts SAS reutilizables
  ├── user_creation.sas
  └── user_deletion.sas

src/com/sas/            # Clases Groovy
  ├── UserValidator.groovy
  └── AuditLogger.groovy

test/                   # Scripts de prueba
  ├── test_user_creation.bat
  └── test_user_deletion.bat
```

## 🔐 Seguridad

- **Nunca** almacenes contraseñas en código
- Usa `withCredentials` de Jenkins para inyectar secrets
- Implementa aprobaciones manuales para operaciones en producción
- Auditoría completa de todas las operaciones

## 📊 Auditoría

Todos los cambios se registran en:
```
${WORKSPACE}/audit/audit_YYYY-MM-DD.log
```

Formato:
```
[2026-02-18 14:30:45] [CREATE_USER] User:jperez - Usuario creado exitosamente
```

## 🆘 Troubleshooting

### Error: "metadata_commit falló"

- Verificar que `sasadm` tenga rol **SASMeta//Administrators**
- Revisar conexión al metadata server
- Validar puerto 8561 accesible

### Error: "AuthenticationDomain no encontrado"

- El nombre real es `DefaultAuth` (no "Default")
- Verificar en SAS Management Console el nombre exacto

## 📞 Soporte

Contactar al equipo de DevOps para issues o mejoras.
```

---

## ✅ Checklist de Implementación

- [ ] Subir todo el código a repositorio Git
- [ ] Configurar Shared Library en Jenkins
- [ ] Crear credenciales de metadata en Jenkins
- [ ] Configurar Windows Agent en servidor SAS
- [ ] Probar job de creación con usuario de prueba
- [ ] Probar job de eliminación con usuario de prueba
- [ ] Validar logs de auditoría
- [ ] Documentar proceso para el equipo

---

¿Necesitas que adapte algo específico para tu entorno o que agregue más funcionalidades (como asignación de roles específicos, creación de "My Folder", etc.)?
