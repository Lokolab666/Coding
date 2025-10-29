vars/autoAUT.ramalCreate.groovy

#!/usr/bin/env groovy
import com.cardif.jenkins.cmn.*
import com.cardif.jenkins.auto.*

/**
 * Uso desde Jenkinsfile:
 *   autoAUT.ramalCreate('<labelNodo>', '''fullname;firstname;profilename;settype;password''')
 *
 * Ejemplo:
 *   autoAUT.ramalCreate('SAOS001RD01P', 'Juan Perez;Juan;Perfil_Comercial;IP Touch 8068S;Secr3t2025')
 */
def call(String server, String userPlainText) {
  def commonStgs = new com.cardif.jenkins.cmn.CommonStages(this)
  def autoAUT   = new com.cardif.jenkins.auto.AutomationRamalCreate(this)

  node(server) {
    cleanWs()
    try {
      commonStgs.stgInitialize()

      stage('Create Ramal User') {
        autoAUT.createUser(userPlainText)
      }

      commonStgs.printOutput("Usuario creado (o actualizado) en Ramal.", "G")
    } catch (e) {
      commonStgs.printOutput("Error en creación de usuario Ramal: ${e}", "Y")
      postBuildActions()
      throw e
    } finally {
      cleanWs()
    }
  }

  return this
}



src/com/cardif/jenkins/auto/AutomationRamalCreate.groovy

package com.cardif.jenkins.auto

import groovy.json.*
import com.cardif.jenkins.cmn.GlobVars

class AutomationRamalCreate implements Serializable {
  private final Script command
  private final Random rnd = new Random()

  AutomationRamalCreate(Script command) {
    this.command = command
  }

  /**
   * userPlainText formato:
   *   fullname;firstname;profilename;settype;password
   * - settype default: "IP Touch 8068S"
   * - El TUI será automatizado con expect.
   */
  void createUser(String userPlainText) {
    command.echo "[INFO] Datos recibidos: ${userPlainText}"

    // --- Parseo de datos de entrada ---
    def parts = userPlainText.split(';')*.trim()
    if (parts.size() < 5) {
      throw new IllegalArgumentException("Formato inválido. Esperado: fullname;firstname;profilename;settype;password")
    }
    def fullName   = parts[0]
    def firstName  = parts[1]
    def profile    = parts[2]
    def setTypeIn  = parts[3]
    def password   = parts[4]

    def setType = setTypeIn ? setTypeIn : "IP Touch 8068S"   // por defecto

    // utf-8 directory name (usamos fullName tal cual)
    def utf8DirName = fullName

    // --- Variables de entorno requeridas ---
    def tuiCmd = command.env.RAMAL_TUI_CMD ?: "/opt/ramal/bin/ramal_tui"
    def softphoneCheckCmd = command.env.SOFTPHONE_CHECK_CMD ?: ""  // opcional

    command.echo "[INFO] RAMAL_TUI_CMD=${tuiCmd}"
    if (softphoneCheckCmd) command.echo "[INFO] SOFTPHONE_CHECK_CMD=${softphoneCheckCmd}"

    // --- Generar número de directorio aleatorio disponible (4 dígitos) ---
    def dirNumber = pickAvailableDirectoryNumber(softphoneCheckCmd)

    command.echo "[INFO] Número de directorio elegido: ${dirNumber}"

    // --- Generar script Expect para automatizar el TUI ---
    def expectScript = buildExpectScript(
      tuiCmd,
      dirNumber,
      fullName,
      firstName,
      utf8DirName,
      profile,
      setType,
      password
    )

    // Guardar y ejecutar
    command.writeFile file: 'ramal_create.exp', text: expectScript
    command.sh "chmod +x ramal_create.exp"

    // Ejecutar con reintento básico si falla por número ocupado
    Integer attempts = 0
    Integer maxAttempts = 5
    Boolean done = false
    String lastOutput = ""

    while (!done && attempts < maxAttempts) {
      attempts++
      command.echo "[INFO] Intento ${attempts}/${maxAttempts} de creación en TUI..."
      def rc = command.sh(script: './ramal_create.exp > tui_output.log 2>&1 || true', returnStatus: true)
      lastOutput = command.sh(script: 'tail -n +1 tui_output.log', returnStdout: true).trim()

      if (rc == 0 && looksSuccessful(lastOutput)) {
        command.echo "[INFO] Creación OK en intento ${attempts}."
        done = true
      } else {
        command.echo "[WARN] Falló intento ${attempts}. Analizando salida..."
        command.echo lastOutput

        // Heurística: si la salida sugiere "número en uso", elige otro y reintenta
        if (lastOutput =~ /en uso|ocupado|already exists|duplicate|ya asignado/i) {
          dirNumber = pickAvailableDirectoryNumber(softphoneCheckCmd)
          command.echo "[INFO] Nuevo número de directorio: ${dirNumber}"
          // Regenerar expect con el nuevo número
          expectScript = buildExpectScript(tuiCmd, dirNumber, fullName, firstName, utf8DirName, profile, setType, password)
          command.writeFile file: 'ramal_create.exp', text: expectScript
          command.sh "chmod +x ramal_create.exp"
        } else {
          // Otro error: salir
          break
        }
      }
    }

    if (!done) {
      throw new RuntimeException("No se pudo completar la creación del usuario. Revisa 'tui_output.log'.")
    }

    // --- Verificación simple post-creación (opcional) ---
    // Aquí podrías implementar otra corrida expect que navegue a "Consult/Modify"
    // y valide que el usuario/dirNumber existe. (Ya lo hacemos dentro del flow principal.)
    command.echo "[INFO] Proceso finalizado. Número asignado: ${dirNumber}"
    command.print "DIRECTORY_NUMBER=${dirNumber}"
  }

  private boolean looksSuccessful(String output) {
    // Ajusta a los mensajes reales del TUI tras guardar/exitoso
    return (output =~ /Saved|success|creado|Changes applied|operación exitosa/i)
  }

  private String pickAvailableDirectoryNumber(String softphoneCheckCmd) {
    int tries = 0
    while (tries < 50) {
      tries++
      def n = 1000 + rnd.nextInt(9000) // 1000..9999
      if (isAvailable(n, softphoneCheckCmd)) return "${n}"
    }
    throw new RuntimeException("No fue posible encontrar un número de 4 dígitos disponible después de varios intentos.")
  }

  private boolean isAvailable(int number, String softphoneCheckCmd) {
    if (!softphoneCheckCmd) {
      // Sin comando de verificación, asumimos disponible y dejamos que el TUI nos diga si falla
      return true
    }
    try {
      def rc = command.sh(script: "${softphoneCheckCmd} ${number}", returnStatus: true)
      return (rc == 0)
    } catch (ignored) {
      // Si el comando falla, devolvemos true para no bloquear
      return true
    }
  }

  /**
   * Construye el script Expect que automatiza:
   *  - Abrir TUI
   *  - Menú: Users → Creation
   *  - Diligenciar: directory number, directory name (fullName), first name, utf-8 name, profile name, password/confirm
   *  - Set Type = "IP Touch 8068S" (o el que venga)
   *  - Guardar
   *  - Volver → Consult/Modify → buscar por número → poner Terminal Ethernet Address = "" → guardar
   *
   * NOTA: Ajusta los patrones `expect` a los prompts reales del TUI.
   */
  private String buildExpectScript(
    String tuiCmd,
    String dirNumber,
    String fullName,
    String firstName,
    String utf8DirName,
    String profileName,
    String setType,
    String password
  ) {
    def esc = { String s -> s.replace('\\','\\\\').replace('"','\\"') }
    return """#!/usr/bin/env expect -f
set timeout 60

# Lanzar TUI
spawn ${esc(tuiCmd)}

# --- Navegar a Users ---
expect {
  -re "(?i)users" { send "u\\r" }  ;# si hay hotkey; si es menú numerado, cambia por 'send "1\\r"'
  -re "(?i)menu"  { send "u\\r" }
  timeout { exit 90 }
}

# --- Creation ---
expect {
  -re "(?i)creation|create" { send "c\\r" }  ;# o número de opción
  timeout { exit 91 }
}

# --- Directory Number ---
expect -re "(?i)directory number.*:"
send "${esc(dirNumber)}\\r"

# --- Directory Name (fullname) ---
expect -re "(?i)directory name.*:"
send "${esc(fullName)}\\r"

# --- First Name ---
expect -re "(?i)first name.*:"
send "${esc(firstName)}\\r"

# --- UTF-8 Directory Name ---
expect -re "(?i)utf-?8.*directory name.*:"
send "${esc(utf8DirName)}\\r"

# --- Profile Name ---
expect -re "(?i)profile name.*:"
send "${esc(profileName)}\\r"

# --- Password & Confirm ---
expect -re "(?i)password.*:"
send "${esc(password)}\\r"
expect -re "(?i)confirm.*:"
send "${esc(password)}\\r"

# --- Set Type ---
expect -re "(?i)set type.*:"
send "${esc(setType)}\\r"

# --- Guardar (Save/Apply) ---
expect {
  -re "(?i)save|apply|ok" { send "\\r" }
  timeout { }
}

# Esperar confirmación
expect {
  -re "(?i)saved|success|applied|creado|changes applied" {}
  timeout { }
}

# --- Regresar y abrir Consult/Modify ---
# Back
send "\\033"   ;# ESC si aplica, o usa la tecla/flecha que corresponda
expect -re "(?i)users"
# Consult/Modify
send "m\\r"    ;# Modificar/Consultar; ajusta si es numérico

# Buscar por Directory Number
expect -re "(?i)search.*:"
send "${esc(dirNumber)}\\r"

# Campo Terminal Ethernet Address: setear a ""
expect -re "(?i)terminal ethernet address.*:"
send "\\\"\\\"\\r"

# Guardar
expect {
  -re "(?i)save|apply|ok" { send "\\r" }
  timeout { }
}

# Confirmación final
expect {
  -re "(?i)saved|success|applied|changes applied" {}
  timeout { }
}

# Salir ordenado del TUI
send "q\\r"
expect eof
exit 0
"""
  }
}

