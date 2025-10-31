withCredentials([
    usernamePassword(
        credentialsId: GlobVars.accessWasco,
        usernameVariable: 'USER_WAS',
        passwordVariable: 'PASS_WAS'
    )
]) {
    sh '''
        ssh svc.col.wasuat.service@10.171.183.6 \
        /IBM/WebSphere/AppServer/bin/wsadmin.sh \
        -username "$USER_WAS" \
        -password "$PASS_WAS" \
        -lang jython \
        -profileName DmgrDIG \
        -f get_server_info.jy
    '''
}

----------------------------------------------------------
  OPCION 2
// Dentro de tu clase o método
def getWebsphereServerInfo(String wsadminPath, String userEnvVarName, String passEnvVarName) { // Recibe los nombres de las variables de entorno
    // Obtiene los valores de las variables de entorno *dentro* del script sh
    // IMPORTANTE: No uses la interpolación de Groovy para construir el comando con la contraseña directamente.
    // En lugar de eso, pasa las variables de entorno al shell y úsalas allí.

    // Ejemplo de cómo construir el script sh de forma segura:
    // 1. Define un script aquí-doc o una cadena compleja si es necesario, evitando la interpolación directa de contraseñas.
    // 2. Usa 'env' para acceder a las variables de entorno DENTRO del comando sh.

    // Opción 1: Usar un script bash inline con acceso a variables de entorno
    // Jenkins inyecta automáticamente las credenciales de withCredentials como variables de entorno
    // (por ejemplo, WAS_USER y WAS_PASS) dentro del bloque con withCredentials.
    // El script sh puede acceder a ellas como $WAS_USER, $WAS_PASS

    // IMPORTANTE: Para evitar problemas con caracteres especiales en la contraseña,
    // debes usar 'printf %q' para escaparla o pasarla como argumento separado si es posible.
    // Una forma más robusta es usar printf para construir la línea de comando internamente en bash.

    def script = ''' // Comillas triples para cadena multi-línea y evitar interpolación de Groovy
        set +x # Deshabilita temporalmente la impresión de comandos si está activa para seguridad
        # Accede a las variables de entorno inyectadas por Jenkins
        USER="$''' + userEnvVarName + '''"' # Accede a la variable de entorno pasada como nombre
        PASS="$''' + passEnvVarName + '''" # Accede a la variable de entorno pasada como nombre
        # Imprime para depuración (ESTO NO DEBE HACERSE CON LA CONTRASEÑA REAL)
        # echo "Debug: User is $USER"
        # NO HAGAS: echo "Debug: Pass is $PASS" <- ESTO EXHIBIRIA EL SECRETO

        # Ejecuta el comando ssh, pasando la contraseña de forma segura
        # Usamos printf %q para escapar correctamente argumentos que puedan contener caracteres especiales
        # en bash. Esto previene problemas de parsing y posibles inyecciones.
        ssh svc.col.wasuat.service@10.171.183.6 "/IBM/WebSphere/AppServer/bin/wsadmin.sh" \
            -username "$USER" \
            -password "$PASS" \
            -lang jython \
            -profileName DmgrDIG \
            -f get_server_info.jy
        # El shell manejará la contraseña "$PASS" internamente, no se mostrará en el log Jenkins si está bien configurado.
    '''

    try {
        // Ejecuta el script bash
        def output = sh(
            script: script,
            returnStdout: true
        )

        // Procesa la salida
        String cellName = null
        String nodeName = null

        output.split('\n').each { line ->
            if (line.trim().startsWith("CELL_NAME=")) { // Ajusta el prefijo exacto
                cellName = line.split('=', 2)[1].trim()
            } else if (line.trim().startsWith("NODE_NAME=")) { // Ajusta el prefijo exacto
                nodeName = line.split('=', 2)[1].trim()
            }
        }

        if (!cellName || !nodeName) {
            error("No se pudo obtener CELL_NAME o NODE_NAME del servidor. Salida: ${output}")
        }

        echo "Cell obtenida: ${cellName}"
        echo "Node obtenida: ${nodeName}"

        return [cell: cellName, node: nodeName]

    } catch (Exception e) {
        error("Error ejecutando wsadmin para obtener la información del servidor: ${e.getMessage()}")
        throw e
    } finally {
        // Limpia archivos temporales si es necesario
        sh "rm -f get_server_info.jy" // Asegúrate que este archivo se haya creado localmente o en el contexto correcto
    }
}
