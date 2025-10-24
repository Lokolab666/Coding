// vars/getWebsphereServerInfo.groovy

def call(String wsadminPath = '/ruta/predeterminada/a/wsadmin.sh') { // Puedes pasar la ruta como parámetro o usar una variable de entorno
    echo "Obteniendo información de Cell y Node del servidor WebSphere..."

    // Script Jython para obtener la información
    String jythonScript = '''
import sys
try:
    # Obtener el nombre de la Cell
    cellName = AdminControl.getCell()
    print("CELL_NAME=" + cellName)

    # Obtener el nombre del Node
    nodeName = AdminControl.getNode()
    print("NODE_NAME=" + nodeName)

    # Opcional: Imprimir otros detalles si es necesario
    # serverName = AdminControl.getServerName()
    # print("SERVER_NAME=" + serverName)

except Exception as e:
    print("Error obteniendo la información del servidor: " + str(e))
    sys.exit(1)
'''

    // Escribir el script temporalmente
    writeFile file: 'get_server_info.jy', text: jythonScript

    // Variables para almacenar la salida
    def cellName = ""
    def nodeName = ""

    try {
        // Ejecutar wsadmin.sh con el script Jython
        // Es crucial que wsadmin se conecte al servidor correcto (local o remoto).
        // Este ejemplo asume que wsadmin se puede conectar al servidor donde se está ejecutando el pipeline
        // o que se le pasan credenciales/hostname para conectarse remotamente.
        // Por defecto, wsadmin -conntype NONE se conecta al servidor local si está corriendo dentro de él.
        // Si te conectas remotamente, necesitas algo como:
        // sh "${wsadminPath} -lang jython -host <hostname> -port <port> -username <user> -password <password> -f get_server_info.jy"
        // Para conexión local (si Jenkins está en el servidor WAS o puede acceder al proceso):
        def output = sh(
            script: "${wsadminPath} -lang jython -f get_server_info.jy",
            returnStdout: true
        ).trim()

        echo "Salida de wsadmin:\n${output}"

        // Parsear la salida para obtener los nombres
        output.split('\n').each { line ->
            if (line.startsWith('CELL_NAME=')) {
                cellName = line.split('=', 2)[1]
            } else if (line.startsWith('NODE_NAME=')) {
                nodeName = line.split('=', 2)[1]
            }
        }

        if (!cellName || !nodeName) {
            error("No se pudo obtener CELL_NAME o NODE_NAME del servidor. Salida: ${output}")
        }

        echo "Cell obtenida: ${cellName}"
        echo "Node obtenida: ${nodeName}"

    } catch (Exception e) {
        error("Error ejecutando wsadmin para obtener la información del servidor: ${e.getMessage()}")
        throw e // Re-lanza para detener el pipeline
    } finally {
        // Borrar el script temporal
        sh "rm -f get_server_info.jy"
    }

    // Devolver la información como un Map para que sea fácil de usar
    return [cell: cellName, node: nodeName]
}

return this




// Jenkinsfile (Actualizado)
pipeline {
    agent any // Asegúrate que este agente tenga acceso a wsadmin y al servidor WAS

    // No necesitas parámetros para Cell y Node si los obtienes dinámicamente
    // parameters {
    //     string(name: 'WAS_CELL', defaultValue: 'mycell', description: 'Nombre de la celda WebSphere')
    //     string(name: 'WAS_NODE', defaultValue: 'mynode', description: 'Nombre del nodo WebSphere')
    //     ...
    // }

    environment {
        // Definir la ruta a wsadmin como una variable de entorno, si no es global
        WSADMIN_PATH = '/ruta/a/tu/was/profiles/AppSrv01/bin/wsadmin.sh' // Ajusta la ruta
    }

    stages {
        stage('Obtener Info del Servidor WAS') {
            steps {
                script {
                    // Llama a la función para obtener la cell y node
                    def serverInfo = getWebsphereServerInfo(env.WSADMIN_PATH)
                    
                    // Guarda los valores en variables del entorno o script para su uso posterior
                    env.DISCOVERED_CELL = serverInfo.cell
                    env.DISCOVERED_NODE = serverInfo.node
                    
                    echo "Cell descubierta: ${env.DISCOVERED_CELL}"
                    echo "Node descubierta: ${env.DISCOVERED_NODE}"
                }
            }
        }

        stage('Preparar Entorno') {
            steps {
                script {
                    echo "Iniciando automatización para Celda: ${env.DISCOVERED_CELL}, Nodo: ${env.DISCOVERED_NODE}"
                    // Puedes definir aquí otros parámetros fijos o dejarlos como parámetros del pipeline
                    params.REP_NAME = params.REP_NAME ?: 'MyNewREP' // Usa valor del parámetro o un valor por defecto
                    params.PROPERTY_NAME = params.PROPERTY_NAME ?: 'MyProperty'
                    params.PROPERTY_VALUE = params.PROPERTY_VALUE ?: 'MyValue'
                }
            }
        }

        stage('Crear Resource Environment Provider y Propiedades') {
            steps {
                script {
                    // Llama a la función definida anteriormente, pasando los valores dinámicos
                    createWebsphereVariables(
                        env.DISCOVERED_CELL, // Usar el valor descubierto
                        env.DISCOVERED_NODE, // Usar el valor descubierto
                        params.REP_NAME,
                        params.PROPERTY_NAME,
                        params.PROPERTY_VALUE
                    )
                }
            }
        }

        stage('Verificar y Sincronizar') {
            steps {
                script {
                    echo "Verificación y sincronización pendiente por implementar."
                    // Implementar la sincronización si es ND
                    // AdminControl.getNode() puede ayudar a saber si es necesario sincronizar
                    // o si se puede usar AdminNodeManagement.syncActiveNodes()
                }
            }
        }
    }

    post {
        always {
            echo 'Pipeline finalizado.'
        }
        success {
            echo 'Pipeline completado exitosamente.'
        }
        failure {
            echo 'Pipeline fallido.'
        }
    }
}







---------------------------------------------


  pipeline {
    agent any // Puedes especificar un nodo específico donde esté disponible wsadmin

    parameters {
        // Parámetros para la configuración específica
        string(name: 'WAS_CELL', defaultValue: 'mycell', description: 'Nombre de la celda WebSphere')
        string(name: 'WAS_NODE', defaultValue: 'mynode', description: 'Nombre del nodo WebSphere')
        string(name: 'REP_NAME', defaultValue: 'MyNewREP', description: 'Nombre del Resource Environment Provider')
        string(name: 'PROPERTY_NAME', defaultValue: 'MyProperty', description: 'Nombre de la propiedad J2EE')
        string(name: 'PROPERTY_VALUE', defaultValue: 'MyValue', description: 'Valor de la propiedad J2EE')
        // Agrega más parámetros según sea necesario
    }

    stages {
        stage('Preparar Entorno') {
            steps {
                script {
                    echo "Iniciando automatización para Celda: ${params.WAS_CELL}, Nodo: ${params.WAS_NODE}"
                    // Aquí podrías descargar scripts wsadmin adicionales si es necesario
                }
            }
        }

        stage('Crear Resource Environment Provider y Propiedades') {
            steps {
                script {
                    // Llama a la función definida en vars/createWebsphereVariables.groovy
                    createWebsphereVariables(
                        params.WAS_CELL,
                        params.WAS_NODE,
                        params.REP_NAME,
                        params.PROPERTY_NAME,
                        params.PROPERTY_VALUE
                        // Pasa otros parámetros aquí
                    )
                }
            }
        }

        stage('Verificar y Sincronizar') {
            steps {
                script {
                    echo "Verificación pendiente por implementar."
                    // Aquí podrías añadir pasos para verificar la creación
                    // Si es Network Deployment, sincronizar nodos
                    // AdminControl.sync() o un script wsadmin específico para sincronizar
                    // Ejemplo de comando wsadmin para sincronizar (esto es conceptual, puede requerir ajustes):
                    // sh "${WSADMIN_HOME}/wsadmin.sh -lang jython -f sync_nodes.jy"
                }
            }
        }
    }

    post {
        always {
            echo 'Pipeline finalizado.'
            // Puedes añadir pasos de limpieza aquí si es necesario
        }
        success {
            echo 'Pipeline completado exitosamente.'
        }
        failure {
            echo 'Pipeline fallido.'
        }
    }
}









// vars/createWebsphereVariables.groovy
def call(String wasCell, String wasNode, String repName, String propName, String propValue) {
    // Suponiendo que la lógica real está en src/WebsphereConfig/
    // y hay una clase como WebsphereResourceEnvironmentProviderManager
    def wsConfigManager = new lib.WebSphereConfig.WebsphereResourceEnvironmentProviderManager() // Ajusta la ruta según tu estructura

    try {
        wsConfigManager.createResourceEnvironmentProviderAndProperty(
            wasCell,
            wasNode,
            repName,
            propName,
            propValue
        )
    } catch (Exception e) {
        echo "Error durante la creación de variables en WebSphere: ${e.getMessage()}"
        currentBuild.result = 'FAILURE'
        throw e // Re-lanza la excepción para que el stage falle
    }
}

// Opcional: Método para ejecutar comandos wsadmin.sh
def executeWsadmin(String jythonScriptContent) {
    // Define la ruta a wsadmin.sh, puede ser un parámetro global o ambiente
    def wsadminPath = "/ruta/a/tu/was/profiles/AppSrv01/bin/wsadmin.sh" // Debe ser configurable

    // Escribe el script Jython temporalmente
    writeFile file: 'temp_wsadmin_script.jy', text: jythonScriptContent

    // Ejecuta wsadmin.sh con el script
    sh """
        ${wsadminPath} -lang jython -f temp_wsadmin_script.jy
    """

    // Borra el script temporal
    sh "rm temp_wsadmin_script.jy"
}

return this // Permite que la variable sea utilizada







// src/WebsphereConfig/WebsphereResourceEnvironmentProviderManager.groovy
package lib.WebSphereConfig

class WebsphereResourceEnvironmentProviderManager {

    def executeWsadminScript(String scriptContent) {
        // Llama al método helper en el archivo de variable
        // Suponiendo que 'executeWsadmin' está disponible en el alcance del Pipeline
        // o se pasa como closure.
        // Por simplicidad, aquí se asume que está en el 'this' del archivo vars.
        // En la práctica, necesitarías inyectar o pasar la referencia correctamente.
        this.executeWsadmin(scriptContent) // Esto requiere que 'executeWsadmin' esté disponible en el contexto correcto
    }


    def createResourceEnvironmentProviderAndProperty(String wasCell, String wasNode, String repName, String propName, String propValue) {
        // Construye el script Jython necesario basado en la documentación proporcionada
        String jythonScript = """
            # --- Paso 1: Crear Resource Environment Provider ---
            print("Obteniendo ID del nodo...")
            node = AdminConfig.getid('/Cell:\${wasCell}/Node:\${wasNode}/')
            print("Nodo encontrado: " + node)

            print("Configurando atributos para el nuevo REP...")
            repAttrs = [['name', '\${repName}']]

            print("Creando Resource Environment Provider...")
            newrep = AdminConfig.create('ResourceEnvironmentProvider', node, repAttrs)
            print("Nuevo REP creado: " + newrep)

            # --- Paso 2: Crear Propiedad J2EE ---
            print("Configurando atributos para la nueva propiedad...")
            rpAttrs = [['name', '\${propName}'], ['value', '\${propValue}']] # Añadido valor

            print("Obteniendo PropertySet del REP...")
            propSet = AdminConfig.showAttribute(newrep, 'propertySet')
            print("PropertySet encontrado: " + propSet)

            if propSet == None or propSet == "":
                print("PropertySet no encontrado, creando uno nuevo...")
                propSet = AdminConfig.create('J2EEResourcePropertySet', newrep, [])
                print("Nuevo PropertySet creado: " + propSet)

            print("Creando J2EE Resource Property...")
            newProperty = AdminConfig.create('J2EEResourceProperty', propSet, rpAttrs)
            print("Nueva propiedad creada: " + newProperty)

            # --- Paso 3: Guardar Cambios ---
            print("Guardando configuración...")
            AdminConfig.save()
            print("Configuración guardada exitosamente.")
        """

        // Ejecuta el script Jython a través de wsadmin
        executeWsadminScript(jythonScript)
    }
}

// Nota: La clase no puede tener un método main() si se va a usar como librería en Jenkins Pipeline
// La ejecución se maneja desde el archivo vars/ o el Jenkinsfile   




