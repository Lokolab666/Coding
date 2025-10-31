withCredentials([
    usernamePassword(
        credentialsId: GlobVars.accessWasco,
        usernameVariable: 'USER_WAS',
        passwordVariable: 'PASS_WAS'
    )
]) {
    // Primero, sube el script Jython
    sh 'scp get_server_info.jy svc.col.wasuat.service@10.171.183.6:/tmp/'

    // Luego, genera un wrapper shell en el remoto que reciba credenciales vía stdin o variable local
    sh '''
        ssh svc.col.wasuat.service@10.171.183.6 bash << 'EOF'
            read -r USER_INPUT
            read -r PASS_INPUT
            /IBM/WebSphere/AppServer/bin/wsadmin.sh \
                -username "$USER_INPUT" \
                -password "$PASS_INPUT" \
                -lang jython \
                -profileName DmgrDIG \
                -f /tmp/get_server_info.jy
        EOF
    ''' + " <<< \$'${USER_WAS}\n${PASS_WAS}'"
}


------------------------------------

withCredentials([
    usernamePassword(
        credentialsId: GlobVars.accessWasco,
        usernameVariable: 'USER_WAS',
        passwordVariable: 'PASS_WAS'
    )
]) {
    def output = sh(
        script: """
            ${WSADMIN_PATH} \
                -username "\$USER_WAS" \
                -password "\$PASS_WAS" \
                -lang jython \
                -profileName DmgrDIG \
                -f get_server_info.jy
        """,
        returnStdout: true
    ).trim()
}
