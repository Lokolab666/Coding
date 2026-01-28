pción 1: Scripts SAS con Metadata DATA Step Functions (Recomendado para Jenkins)
Puedes crear un script .sas que use funciones de metadata para:
Crear usuarios (METADATA_NEWOBJ con clase Person)
Asignar grupos/roles (METADATA_ADDASSOC)
Crear logins con Authentication Domain "Default" (METADATA_NEWOBJ con clase Login)
Ejemplo de estructura del script:

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

Integracion
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

Documentacion relevante
https://documentation.sas.com/doc/en/lrmeta/9.4/?spm=a2ty_o01.29997173.0.0.76825171470sK4

  
