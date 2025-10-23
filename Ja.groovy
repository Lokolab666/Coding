
package com.cardif.jenkins.was

class WASRepStages implements Serializable {
  def steps
  WASRepStages(steps) { this.steps = steps }

  void materializeResources(String propsText) {
    steps.sh 'rm -rf .wasrep && mkdir -p .wasrep'
    def repScript = steps.libraryResource('was/rep_config.py')
    steps.writeFile file: '.wasrep/rep_config.py', text: repScript
    steps.writeFile file: '.wasrep/props.properties', text: propsText ?: ''
  }

  void uploadToServer(String server, String sshCredsId) {
    steps.sshagent(credentials: [sshCredsId]) {
      steps.sh """
        set -euxo pipefail
        scp -o StrictHostKeyChecking=no .wasrep/rep_config.py ${server}:/tmp/rep_config.py
        scp -o StrictHostKeyChecking=no .wasrep/props.properties ${server}:/tmp/props.properties
      """
    }
  }

  void runWsadmin(String server, String wsadminPath, String cell, String node, String repName,
                  String sshCredsId, String soapCredsId = null) {
    def detect = """
      WSADM="${wsadminPath}";
      [ -z "$WSADM" ] && WSADM="$(command -v wsadmin.sh || true)";
      if [ -z "$WSADM" ]; then
        WSADM="$(find /opt /IBM -type f -name wsadmin.sh 2>/dev/null | head -n1 || true)";
      fi
      echo "[INFO] wsadmin.sh: $WSADM"
      [ -x "$WSADM" ] || { echo "[ERROR] wsadmin.sh no encontrado"; exit 2; }
    """.stripIndent()

    steps.sshagent(credentials: [sshCredsId]) {
      if (soapCredsId) {
        steps.withCredentials([steps.usernamePassword(credentialsId: soapCredsId,
                                                     usernameVariable: 'WAS_USER',
                                                     passwordVariable: 'WAS_PASS')]) {
          steps.sh """
            set -euxo pipefail
            ssh -o StrictHostKeyChecking=no ${server} bash -lc '
              set -e
              ${detect}
              "$WSADM" -lang jython -conntype SOAP -host ${server} -port 9043 \\
                       -user "$WAS_USER" -password "$WAS_PASS" \\
                       -f /tmp/rep_config.py /tmp/props.properties ${cell} ${node} ${repName} | tee /tmp/rep_config.out
            '
          """
        }
      } else {
        steps.sh """
          set -euxo pipefail
          ssh -o StrictHostKeyChecking=no ${server} bash -lc '
            set -e
            ${detect}
            "$WSADM" -lang jython -f /tmp/rep_config.py /tmp/props.properties ${cell} ${node} ${repName} | tee /tmp/rep_config.out
          '
        """
      }
    }
  }

  void fetchLogs(String server, String sshCredsId) {
    steps.sshagent(credentials: [sshCredsId]) {
      steps.sh """
        set -euxo pipefail
        scp -o StrictHostKeyChecking=no ${server}:/tmp/rep_config.out .wasrep/ || true
      """
    }
    steps.archiveArtifacts artifacts: '.wasrep/**', fingerprint: true
  }
}












#!/usr/bin/env groovy
import com.cardif.jenkins.cmn.*
import com.cardif.jenkins.was.*

/**
 * Wrapper estilo shared-lib:
 * WasRepApply(
 *   SERVER: 'dmgr-host',
 *   CELL: 'mycell',
 *   NODE: 'mynode',
 *   REP_NAME: 'REP1',
 *   PROPERTIES: 'k1=v1\nk2=v2',       // key=value por línea
 *   WSADMIN_PATH: '',                 // opcional
 *   SSH_CREDS_ID: 'was-ssh-key',      // Jenkins cred (SSH)
 *   SOAP_CREDS_ID: ''                 // opcional (WAS user/pass). Si vacío, usa soap.client.props del perfil
 * )
 */
def call(Map args) {
  def common = new com.cardif.jenkins.cmn.CommonStages(this)
  def was    = new com.cardif.jenkins.was.WASRepStages(this)

  // parámetros con defaults
  def SERVER       = args.SERVER ?: ''
  def CELL         = args.CELL ?: ''
  def NODE         = args.NODE ?: ''
  def REP_NAME     = args.REP_NAME ?: 'REP1'
  def PROPERTIES   = (args.PROPERTIES ?: '').toString()
  def WSADMIN_PATH = (args.WSADMIN_PATH ?: '').toString()
  def SSH_CREDS_ID = args.SSH_CREDS_ID ?: (GlobVars?.sshCreds ?: 'was-ssh-key')
  def SOAP_CREDS_ID= (args.SOAP_CREDS_ID ?: '').toString()

  node(GlobVars.master) {
    try {
      cleanWs()
      properties([buildDiscarder(logRotator(numToKeepStr: '30')), disableConcurrentBuilds()])

      timestamps {
        ansiColor('xterm') {
          common.printOutput("=== WAS REP APPLY ===", "Y")
          common.printOutput("SERVER=${SERVER} CELL=${CELL} NODE=${NODE} REP=${REP_NAME}", "G")

          stage('Materializar recursos (resources/)') {
            was.materializeResources(PROPERTIES)
            sh 'ls -la .wasrep'
          }

          stage('Subir al DMGR por SSH') {
            was.uploadToServer(SERVER, SSH_CREDS_ID)
          }

          stage('Ejecutar wsadmin (create/ensure REP + props + save + sync)') {
            was.runWsadmin(SERVER, WSADMIN_PATH, CELL, NODE, REP_NAME, SSH_CREDS_ID, SOAP_CREDS_ID)
          }

          stage('Traer logs y publicar artefactos') {
            was.fetchLogs(SERVER, SSH_CREDS_ID)
            common.printOutput('Ver .wasrep/rep_config.out (lista de propiedades ANTES/DESPUÉS).', 'G')
          }

          common.printOutput('[SUCCESS] REP asegurado y propiedades aplicadas.', 'G')
        }
      }

      // si tu lib tiene esta función global, la llamamos; si no existe, no falla.
      try { postBuildActions() } catch (ignored) {}

      cleanWs()
      return this

    } catch (e) {
      common.printOutput("Se presentó un error, contacte al equipo de Automatización de SSC", "Y")
      common.printOutput("${e}", "Y")
      try { postBuildActions() } catch (ignored) {}
      cleanWs()
      throw e
    }
  }
}










@Library('SSC_Automation') _

WasRepApply(
  SERVER: 'was-admin-lam-co-assurance.staging.echonet',
  CELL:   'MyCell',
  NODE:   'MyNode',
  REP_NAME: 'ConsumerPreSaleProvider',
  PROPERTIES: '''
bootstrap.servers=saox1p1mkf01:9092,saox1p1mkf02:9092
schema.registry.url=https://saox1p1mkf12:8081,https://saox1p1mkf13:8081
kafka.topic=COL_DigitalSales_PRD_PolicySold
''',
  WSADMIN_PATH: '',
  SSH_CREDS_ID: 'was-ssh-key',
  SOAP_CREDS_ID: '' // si tu perfil ya tiene soap.client.props, déjalo vacío
)



