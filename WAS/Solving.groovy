stage('SSH config local') {
  sh '''#!/usr/bin/env bash
set -euo pipefail
cat > .ssh_config <<'EOF'
Host was-dmgr
  HostName 10.171.183.6
  User svc.col.wasuat.service
  StrictHostKeyChecking no
  UserKnownHostsFile /dev/null
  LogLevel ERROR
EOF
'''
}

// Usar el alias
sshagent(credentials: ['ssh-to-was']) {
  sh '''
  scp -F .ssh_config get_server_info.py was-dmgr:/tmp/get_server_info.py
  '''
  withCredentials([usernamePassword(credentialsId: 'was-soap-creds', usernameVariable: 'WAS_USER', passwordVariable: 'WAS_PASS')]) {
    sh '''#!/usr/bin/env bash
set -euo pipefail
ssh -F .ssh_config was-dmgr 'bash -lc "
  set -e
  set +H
  WSADM=\\"$(command -v wsadmin.sh || true)\"
  if [ -z \\"$WSADM\\" ]; then WSADM=\\"$(find /opt /IBM -type f -name wsadmin.sh 2>/dev/null | head -n1)\\"; fi
  \\"$WSADM\\" -lang jython -conntype SOAP -host 10.171.183.6 -port 9043 \\
              -user \\"$WAS_USER\\" -password \\"$WAS_PASS\\" \\
              -f /tmp/get_server_info.py | tee /tmp/get_server_info.out
"'''
  }
}
