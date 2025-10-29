vars/AUT089_createUser.groovy

#!/usr/bin/env groovy
import com.cardif.jenkins.cmn.*
import com.cardif.jenkins.auto.*

def call(String server, String payloadPath, String env = 'CERT') {
  def commonStgs = new com.cardif.jenkins.cmn.CommonStages(this)
  def auto      = new com.cardif.jenkins.auto.AutomationAUT089(this)

  node(server) {
    cleanWs()
    try {
      commonStgs.stgInitialize()
      auto.createUser(payloadPath, env)
    } catch (e) {
      commonStgs.printOutput(e.toString(), "Y")
    } finally {
      postBuildActions()
      cleanWs()
    }
  }
  return this
}


src/com/cardif/jenkins/auto/AutomationAUT089.groovy

package com.cardif.jenkins.auto

import groovy.json.*
import com.cardif.jenkins.cmn.GlobVars

class AutomationAUT089 implements Serializable {
  private final Script command
  AutomationAUT089(Script command) { this.command = command }

  void createUser(String payloadPath, String env) {
    def commonStg  = new com.cardif.jenkins.cmn.CommonStages(command)
    def utilsStgs  = new com.cardif.jenkins.utils.UtilsStages(command)

    // === AUTH: OAuth2 + mTLS ===
    command.stage("Auth (OAuth2 + mTLS)") {
      String base = (env?.toUpperCase() == 'PROD')
        ? 'https://api-insurconnect.b3.com.br'
        : 'https://api-insurconnect-cert.b3.com.br'

      command.withCredentials([
        file  (credentialsId: 'INSURCONNECT_P12',           variable: 'P12_FILE'),
        string(credentialsId: 'INSURCONNECT_P12_PASSWORD',  variable: 'P12_PASS'),
        string(credentialsId: 'INSURCONNECT_CLIENT_ID',     variable: 'CLIENT_ID'),
        string(credentialsId: 'INSURCONNECT_CLIENT_SECRET', variable: 'CLIENT_SECRET'),
      ]) {
        String tokenResp = command.sh(
          script: """
            set -euo pipefail
            curl --silent --show-error \\
              --cert-type P12 --cert "$P12_FILE:$P12_PASS" \\
              --header "Content-Type: application/x-www-form-urlencoded" \\
              --data-urlencode "grant_type=client_credentials" \\
              --data-urlencode "client_id=$CLIENT_ID" \\
              --data-urlencode "client_secret=$CLIENT_SECRET" \\
              -X POST "$base/seguros/api/oauth/token"
          """,
          returnStdout: true
        ).trim()

        def json = new groovy.json.JsonSlurper().parseText(tokenResp)
        if (!json?.access_token) {
          throw new RuntimeException("No se obtuvo access_token: ${tokenResp}")
        }
        command.env.INSURCONNECT_TOKEN = json.access_token as String
        commonStg.printOutput("Token obtenido. expira en ${json.expires_in}s", "G")
      }
    }

    // === CREATE USER ===
    command.stage("Create user (InsurConnect Admin API)") {
      command.sh "test -f '${payloadPath}'"

      // ⚠️ Path parametrizado hasta tener el Swagger oficial de “usuarios”
      String apiPath = (command.params?.INSURCONNECT_USERS_PATH ?: '/seguranca/api/v1/usuarios')

      String base = (env?.toUpperCase() == 'PROD')
        ? 'https://api-insurconnect.b3.com.br'
        : 'https://api-insurconnect-cert.b3.com.br'

      command.withCredentials([
        file  (credentialsId: 'INSURCONNECT_P12',          variable: 'P12_FILE'),
        string(credentialsId: 'INSURCONNECT_P12_PASSWORD', variable: 'P12_PASS')
      ]) {
        String resp = command.sh(
          script: """
            set -euo pipefail
            curl --silent --show-error --write-out '\\n%{http_code}' \\
              --cert-type P12 --cert "$P12_FILE:$P12_PASS" \\
              --header "Authorization: Bearer $INSURCONNECT_TOKEN" \\
              --header "Content-Type: application/json" \\
              --data "@${payloadPath}" \\
              -X POST "$base${apiPath}"
          """,
          returnStdout: true
        ).trim()

        def lines  = resp.readLines()
        def status = lines[-1] as String
        def body   = (lines.size() > 1) ? lines[0..-2].join('\\n') : ''

        commonStg.printOutput("HTTP ${status}", status.startsWith('2') ? "G" : "R")
        if (!status.startsWith('2')) {
          throw new RuntimeException("Fallo creación de usuario: HTTP ${status} -> ${body}")
        }
        command.echo "Respuesta: ${body}"
      }
    }
  }
}


