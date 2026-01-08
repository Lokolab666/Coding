def logs = sh(
  script: '/opt/IBM/WebSphere/AppServer/bin/wsadmin.sh -lang jython -f backup.py 2>&1',
  returnStdout: true
).trim()
