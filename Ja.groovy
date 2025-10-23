
@Library('SSC_Automation') _   // <-- nombre de tu librería compartida en Jenkins

pipeline {
  agent { label "${params.NODE_LABEL}" }

  options {
    ansiColor('xterm')
    timestamps()
    buildDiscarder(logRotator(numToKeepStr: '30'))
    disableConcurrentBuilds()
  }

  parameters {
    choice(name: 'NODE_LABEL', choices: ['master','linux','any'], description: 'Agente Jenkins')

    // DMGR / WAS
    string(name: 'SERVER',   defaultValue: 'was-admin-lam-co-assurance.staging.echonet', description: 'Host DMGR (SSH)')
    string(name: 'CELL',     defaultValue: 'mycell', description: 'Cell')
    string(name: 'NODE',     defaultValue: 'mynode', description: 'Node (scope del REP)')
    string(name: 'REP_NAME', defaultValue: 'REP1',   description: 'Nombre del ResourceEnvironmentProvider')

    // Propiedades a aplicar (key=value por línea)
    text(name: 'PROPERTIES', defaultValue: '''bootstrap.servers=host1:9092,host2:9092
schema.registry.url=https://reg1:8081
kafka.topic=COL_DigitalSales_PRD_PolicySold
''', description: 'key=value por línea; líneas vacías o que comiencen con # se ignoran.')

    // Rutas / Credenciales
    string(name: 'WSADMIN_PATH', defaultValue: '', description: 'Ruta wsadmin.sh (opcional). Si vacío, se detecta.')
    credentials(name: 'SSH_CREDS', defaultValue: 'was-ssh-key', description: 'SSH al DMGR (usuario SO + llave)')
    credentials(name: 'WAS_SOAP_CREDS', defaultValue: '', description: 'WAS user/pass (opcional si no usas soap.client.props)')
  }

  environment {
    LC_ALL = 'C.UTF-8'
    LANG   = 'C.UTF-8'
  }

  stages {
    stage('Materializar archivos compartidos') {
      steps {
        sh 'rm -rf .wasrep && mkdir -p .wasrep'
        // rep_config.py desde la shared lib (resources/was/rep_config.py)
        script {
          def repScript = libraryResource('was/rep_config.py')
          writeFile file: '.wasrep/rep_config.py', text: repScript
        }
        // props.properties desde parámetro
        writeFile file: '.wasrep/props.properties', text: params.PROPERTIES
      }
    }

    stage('Subir archivos al DMGR') {
      steps {
        sshagent(credentials: [params.SSH_CREDS]) {
          sh """
            set -euxo pipefail
            scp -o StrictHostKeyChecking=no .wasrep/rep_config.py ${params.SERVER}:/tmp/rep_config.py
            scp -o StrictHostKeyChecking=no .wasrep/props.properties ${params.SERVER}:/tmp/props.properties
          """
        }
      }
    }

    stage('Ejecutar wsadmin (crear REP, props, save, sync)') {
      steps {
        script {
          def soap = params.WAS_SOAP_CREDS?.trim()
          if (soap) {
            withCredentials([usernamePassword(credentialsId: params.WAS_SOAP_CREDS, usernameVariable: 'WAS_USER', passwordVariable: 'WAS_PASS')]) {
              sshagent(credentials: [params.SSH_CREDS]) {
                sh """
                  set -euxo pipefail
                  ssh -o StrictHostKeyChecking=no ${params.SERVER} bash -lc '
                    set -e
                    WSADM="${params.WSADMIN_PATH}"
                    [ -z "$WSADM" ] && WSADM="$(command -v wsadmin.sh || true)"
                    if [ -z "$WSADM" ]; then
                      WSADM="$(find /opt /IBM -type f -name wsadmin.sh 2>/dev/null | head -n1 || true)"
                    fi
                    echo "[INFO] wsadmin.sh: $WSADM"
                    "$WSADM" -lang jython -conntype SOAP -host ${params.SERVER} -port 9043 \\
                             -user "$WAS_USER" -password "$WAS_PASS" \\
                             -f /tmp/rep_config.py /tmp/props.properties ${params.CELL} ${params.NODE} ${params.REP_NAME} | tee /tmp/rep_config.out
                  '
                """
              }
            }
          } else {
            sshagent(credentials: [params.SSH_CREDS]) {
              sh """
                set -euxo pipefail
                ssh -o StrictHostKeyChecking=no ${params.SERVER} bash -lc '
                  set -e
                  WSADM="${params.WSADMIN_PATH}"
                  [ -z "$WSADM" ] && WSADM="$(command -v wsadmin.sh || true)"
                  if [ -z "$WSADM" ]; then
                    WSADM="$(find /opt /IBM -type f -name wsadmin.sh 2>/dev/null | head -n1 || true)"
                  fi
                  echo "[INFO] wsadmin.sh: $WSADM"
                  "$WSADM" -lang jython -f /tmp/rep_config.py /tmp/props.properties ${params.CELL} ${params.NODE} ${params.REP_NAME} | tee /tmp/rep_config.out
                '
              """
            }
          }
        }
      }
    }

    stage('Traer logs y publicar artefactos') {
      steps {
        sshagent(credentials: [params.SSH_CREDS]) {
          sh """
            set -euxo pipefail
            scp -o StrictHostKeyChecking=no ${params.SERVER}:/tmp/rep_config.out .wasrep/ || true
          """
        }
        archiveArtifacts artifacts: '.wasrep/**', fingerprint: true
        echo '[INFO] Artefactos: .wasrep/rep_config.py, .wasrep/props.properties, .wasrep/rep_config.out'
      }
    }
  }

  post {
    success { echo '[SUCCESS] REP asegurado y propiedades aplicadas correctamente.' }
    failure { echo '[FAILURE] Revisa rep_config.out y la consola para diagnosticar.' }
  }
}
