Jenkinsfile
vars/
├── createRamalUser.groovy
├── deleteRamalUser.groovy
src/
└── com/
    └── cardif/
        └── jenkins/
            └── auto/
                └── RamalAutomation.groovy

vars/createRamalUser.groovy
#!/usr/bin/env groovy

import com.cardif.jenkins.cmn.*
import com.cardif.jenkins.auto.*

def call(String userDataFile) {
    def commonStgs = new com.cardif.jenkins.cmn.CommonStages(this)
    def ramalAuto = new com.cardif.jenkins.auto.RamalAutomation(this)

    node('your-agent-label') {
        cleanws()
        try {
            commonStgs.stgInitialize()
            ramalAuto.createUser(userDataFile)
        } catch (e) {
            commonStgs.printOutput(e, "Y")
            currentBuild.result = 'FAILURE'
        } finally {
            cleanws()
        }
    }
}




vars/deleteRamalUser.groovy
#!/usr/bin/env groovy

import com.cardif.jenkins.cmn.*
import com.cardif.jenkins.auto.*

def call(String directoryNumber) {
    def commonStgs = new com.cardif.jenkins.cmn.CommonStages(this)
    def ramalAuto = new com.cardif.jenkins.auto.RamalAutomation(this)

    node('your-agent-label') {
        cleanws()
        try {
            commonStgs.stgInitialize()
            ramalAuto.deleteUser(directoryNumber)
        } catch (e) {
            commonStgs.printOutput(e, "Y")
            currentBuild.result = 'FAILURE'
        } finally {
            cleanws()
        }
    }
}

src/com/cardif/jenkins/auto/RamalAutomation.groovy
package com.cardif.jenkins.auto

import com.cardif.jenkins.cmn.CommonStages
import com.cardif.jenkins.utils.UtilsStages

class RamalAutomation {
    private final Script script

    RamalAutomation(Script script) {
        this.script = script
    }

    def commonStg = new com.cardif.jenkins.cmn.CommonStages(script)
    def utilsStgs = new com.cardif.jenkins.utils.UtilsStages(script)

    void createUser(String userDataFile) {
        script.stage("Create Ramal User") {
            // Leer datos del usuario
            def userData = readFile(userDataFile).trim()
            def fields = userData.split(';')
            def fullname = fields[0]
            def firstname = fields[1]
            def profilename = fields[2]
            def settype = fields[3] // Ej: "IP Touch 80685"

            // Generar número de directorio disponible
            def directoryNumber = generateAvailableDirectoryNumber()

            // Aquí iría la lógica para interactuar con la terminal de Ramal
            // como enviar comandos a través de Putty o SSH
            // Por simplicidad, se usará una simulación de comandos
            script.echo "Creating user: ${fullname}, Number: ${directoryNumber}"

            // Simulación de comandos (esto debes adaptarlo a tu sistema real)
            // Ejemplo:
            // script.sh """
            // expect -c '
            // spawn ssh user@SAOS001RD01P
            // expect "login:"
            // send "your_user\r"
            // expect "password:"
            // send "your_password\r"
            // expect "$ "
            // send "your_ramal_command\r"
            // ...
            // '
            // """

            // Pasos:
            // 1. Abrir sesión en Ramal
            // 2. Ir a Creación
            // 3. Ingresar datos: directory number, name, etc.
            // 4. Seleccionar Set Type
            // 5. Confirmar
            // 6. Ir a Consult/Modify
            // 7. Ingresar "" en Terminal Ethernet Address
            // 8. Validar en SoftPhone

            script.echo "User ${fullname} created with directory number ${directoryNumber}"
        }
    }

    void deleteUser(String directoryNumber) {
        script.stage("Delete Ramal User") {
            script.echo "Deleting user with directory number: ${directoryNumber}"
            // Lógica para eliminar usuario
            // Similar a createUser, pero con comandos de eliminación
        }
    }

    private String generateAvailableDirectoryNumber() {
        // Simulación: generar número aleatorio de 4 dígitos y verificar disponibilidad
        int num
        boolean available = false
        Random random = new Random()
        while (!available) {
            num = random.nextInt(9000) + 1000 // 1000 - 9999
            available = checkNumberAvailability(num.toString())
        }
        return num.toString()
    }

    private boolean checkNumberAvailability(String number) {
        // Simulación: verificar si el número está disponible
        // En tu caso, aquí harías una verificación real, tal vez con SoftPhone o una API
        script.echo "Checking availability for number: ${number}"
        // Supongamos que 1234 y 5678 están ocupados
        return !(['1234', '5678'].contains(number))
    }
}


John Doe;John;StandardProfile;IP Touch 80685
vars/deleteRamalUser.groovy
