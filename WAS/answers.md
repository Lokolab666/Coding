./kubectl --kubeconfig ~/.kube/argo-dev.yaml -n argo-worldwiderevenue-dev get pod worldwiderevenue-dev-77949c68dd-chclq  -o jsonpath='{.spec.containers[*].name}{"\n"}'
argo-app
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ ./kubectl --kubeconfig ~/.kube/argo-dev.yaml -n argo-worldwiderevenue-dev get pod worldwiderevenue-dev-77949c68dd-chclq -o jsonpath='{.spec.initContainers[*].name}{"\n"}'argo-app-otel-init
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ ./kubectl --kubeconfig ~/.kube/argo-dev.yaml -n argo-worldwiderevenue-dev describe pod worldwiderevenue-dev-77949c68dd-chclq
Name:                 worldwiderevenue-dev-77949c68dd-chclq
Namespace:            argo-worldwiderevenue-dev
Priority:             10000
Priority Class Name:  dev-application
Node:                 ip-10-210-90-202.ec2.internal/10.210.90.202
Start Time:           Thu, 12 Mar 2026 16:47:33 +0000
Labels:               app.kubernetes.io/instance=worldwiderevenue-dev
                      app.kubernetes.io/name=app-tomcat
                      com.medtronic.web/pod-type=web
                      com.medtronic.web/tracking=16Aug2021
                      kubernetes.io/metadata.name=worldwiderevenue-dev
                      pod-template-hash=77949c68dd
Annotations:          kubectl.kubernetes.io/restartedAt: 2026-03-12T16:47:27Z
Status:               Running
IP:                   100.64.55.123
IPs:
  IP:           100.64.55.123
Controlled By:  ReplicaSet/worldwiderevenue-dev-77949c68dd
Init Containers:
  argo-app-otel-init:
    Container ID:  containerd://77d6dfba6268c0923780857af8b8754d51a3a8a89ba1a06853ee5f2b53b83496
    Image:         case.artifacts.medtronic.com/bcp_web-docker-releases-virtual/com/medtronic/web/base/busybox-app-tomcat-init:stable
    Image ID:      case.artifacts.medtronic.com/bcp_web-docker-releases-virtual/com/medtronic/web/base/busybox-app-tomcat-init@sha256:ba90cedb344c9eae12a744c340c5850e69d07dceaea58338f478b464e2d65b74
    Port:          <none>
    Host Port:     <none>
    Command:
      /bin/sh
      -c
    Args:
      echo "adding OpenTelemetry agent..."
      cp /agents/opentelemetry.jar /otel/opentelemetry.jar
      echo "OpenTelemetry agent added!"
      
    State:          Terminated
      Reason:       Completed
      Exit Code:    0
      Started:      Thu, 12 Mar 2026 16:47:34 +0000
      Finished:     Thu, 12 Mar 2026 16:47:34 +0000
    Ready:          True
    Restart Count:  0
    Environment:    <none>
    Mounts:
      /otel from otel-volume (rw)
Containers:
  argo-app:
    Container ID:   containerd://7303b4665ce9fb515485508a608f50dc9444de098c849416e35cf4d71b4dbbfc
    Image:          case.artifacts.medtronic.com/bcp_web-docker-releases-virtual/com/medtronic/web/app/bcp_web/finance/worldwiderevenue:03.09.01
    Image ID:       case.artifacts.medtronic.com/bcp_web-docker-releases-virtual/com/medtronic/web/app/bcp_web/finance/worldwiderevenue@sha256:d047313fb4f579e395242e4d5bde830cdef7704fb2cb7e54ee5872289df891ed
    Port:           8081/TCP
    Host Port:      0/TCP
    State:          Waiting
      Reason:       CrashLoopBackOff
    Last State:     Terminated
      Reason:       Error
      Exit Code:    99
      Started:      Thu, 12 Mar 2026 21:02:14 +0000
      Finished:     Thu, 12 Mar 2026 21:02:22 +0000
    Ready:          False
    Restart Count:  53
    Limits:
      cpu:     3
      memory:  4000Mi
    Requests:
      cpu:     100m
      memory:  1500Mi
    Environment Variables from:
      secrets             Secret     Optional: true
      conjur              Secret     Optional: true
      configmap           ConfigMap  Optional: false
      otel-configmap      ConfigMap  Optional: false
      argo-mdt-configmap  ConfigMap  Optional: false
    Environment:
      MEDTRONIC_VERSION:               03.09.01
      MEDTRONIC_SNAPSHOT:              Release
      MEDTRONIC_DEPLOY_NUMBER:         3687597
      MEDTRONIC_JOBS_POD:              FALSE
      MEDTRONIC_APP_TYPE:              java
      CONTRAST__APPLICATION__VERSION:  WorldWideRevenue-03.09.01-Release
      JAVA_TOOL_OPTIONS:               -javaagent:/otel/opentelemetry.jar
    Mounts:
      /appDataDir/log4j-init-file.xml from secretsvol (ro,path="log4j-init-file.xml")
      /contrast/contrast_security.yml from secretsvol (ro,path="contrast_security.yml")
      /opt/java/openjdk/lib/security/cacerts from secretsvol (ro,path="cacerts")
      /otel from otel-volume (rw)
      /run/apache2 from run-apache2 (rw)
      /tmp from tmp (rw)
      /var/cache/mod_auth_openidc/metadata from providers-vol (rw)
      /var/log from var-log (rw)
      /var/log/apache2 from var-log-apache2 (rw)
      /var/mntfiles/build.json from secretsvol (ro,path="build.json")
      /var/www/localhost/htdocs/login.html from secretsvol (ro,path="login.html")
Conditions:
  Type                        Status
  PodReadyToStartContainers   True 
  Initialized                 True 
  Ready                       False 
  ContainersReady             False 
  PodScheduled                True 
Volumes:
  tmp:
    Type:       EmptyDir (a temporary directory that shares a pod's lifetime)
    Medium:     
    SizeLimit:  5Mi
  otel-volume:
    Type:       EmptyDir (a temporary directory that shares a pod's lifetime)
    Medium:     
    SizeLimit:  25Mi
  var-log:
    Type:       EmptyDir (a temporary directory that shares a pod's lifetime)
    Medium:     
    SizeLimit:  50Mi
  var-log-apache2:
    Type:       EmptyDir (a temporary directory that shares a pod's lifetime)
    Medium:     
    SizeLimit:  50Mi
  run-apache2:
    Type:       EmptyDir (a temporary directory that shares a pod's lifetime)
    Medium:     
    SizeLimit:  1Mi
  secretsvol:
    Type:        Secret (a volume populated by a Secret)
    SecretName:  secrets-files
    Optional:    false
  providers-vol:
    Type:        Secret (a volume populated by a Secret)
    SecretName:  secrets-providers
    Optional:    true
QoS Class:       Burstable
Node-Selectors:  <none>
Tolerations:     node.kubernetes.io/not-ready:NoExecute op=Exists for 300s
                 node.kubernetes.io/unreachable:NoExecute op=Exists for 300s
Events:
  Type     Reason   Age                       From     Message
  ----     ------   ----                      ----     -------
  Normal   Pulled   82m (x27 over 4h)         kubelet  (combined from similar events): Successfully pulled image "case.artifacts.medtronic.com/bcp_web-docker-releases-virtual/com/medtronic/web/app/bcp_web/finance/worldwiderevenue:03.09.01" in 1.067s (1.067s including waiting). Image size: 632444253 bytes.
  Normal   Pulling  3m2s (x54 over 4h17m)     kubelet  Pulling image "case.artifacts.medtronic.com/bcp_web-docker-releases-virtual/com/medtronic/web/app/bcp_web/finance/worldwiderevenue:03.09.01"
  Warning  BackOff  2m26s (x1131 over 4h17m)  kubelet  Back-off restarting failed container argo-app in pod worldwiderevenue-dev-77949c68dd-chclq_argo-worldwiderevenue-dev(8f8c679e-fb96-4667-a71a-c94dfc16c4a1)
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ ./kubectl --kubeconfig ~/.kube/argo-dev.yaml -n argo-worldwiderevenue-dev exec -it worldwiderevenue-dev-77949c68dd-chclq -c argo-app -- sh
error: unable to upgrade connection: container not found ("argo-app")
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ ./kubectl --kubeconfig ~/.kube/argo-dev.yaml -n argo-worldwiderevenue-dev exec -it worldwiderevenue-dev-77949c68dd-chclq -c argo-app-otel-init -- sh
error: unable to upgrade connection: container not found ("argo-app-otel-init")
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ ./kubectl --kubeconfig ~/.kube/argo-dev.yaml -n argo-worldwiderevenue-dev get deploy worldwiderevenue-dev -o yaml      
apiVersion: apps/v1
kind: Deployment
metadata:
  annotations:
    deployment.kubernetes.io/revision: "79"
    meta.helm.sh/release-name: worldwiderevenue-dev
    meta.helm.sh/release-namespace: argo-worldwiderevenue-dev
  creationTimestamp: "2025-08-13T11:47:30Z"
  generation: 97
  labels:
    app.kubernetes.io/instance: worldwiderevenue-dev
    app.kubernetes.io/managed-by: Helm
    app.kubernetes.io/name: app-tomcat
    app.kubernetes.io/version: v1
    com.medtronic.web/tracking: 16Aug2021
    helm.sh/chart: argo-app-1.0.9
    helm.toolkit.fluxcd.io/name: worldwiderevenue-dev
    helm.toolkit.fluxcd.io/namespace: argo-worldwiderevenue-dev
    kubernetes.io/metadata.name: worldwiderevenue-dev
    restart-required-on-secret-change: "true"
  name: worldwiderevenue-dev
  namespace: argo-worldwiderevenue-dev
  resourceVersion: "1371641260"
  uid: 821203f5-5c0d-4bb7-ac6b-1f26291a5dc4
spec:
  progressDeadlineSeconds: 600
  replicas: 2
  revisionHistoryLimit: 10
  selector:
    matchLabels:
      app.kubernetes.io/instance: worldwiderevenue-dev
      app.kubernetes.io/name: app-tomcat
      com.medtronic.web/pod-type: web
      com.medtronic.web/tracking: 16Aug2021
      kubernetes.io/metadata.name: worldwiderevenue-dev
  strategy:
    rollingUpdate:
      maxSurge: 2
      maxUnavailable: 0
    type: RollingUpdate
  template:
    metadata:
      annotations:
        kubectl.kubernetes.io/restartedAt: "2026-03-12T16:47:27Z"
      labels:
        app.kubernetes.io/instance: worldwiderevenue-dev
        app.kubernetes.io/name: app-tomcat
        com.medtronic.web/pod-type: web
        com.medtronic.web/tracking: 16Aug2021
        kubernetes.io/metadata.name: worldwiderevenue-dev
    spec:
      automountServiceAccountToken: false
      containers:
      - env:
        - name: MEDTRONIC_VERSION
          value: 03.09.01
        - name: MEDTRONIC_SNAPSHOT
          value: Release
        - name: MEDTRONIC_DEPLOY_NUMBER
          value: "3687597"
        - name: MEDTRONIC_JOBS_POD
          value: "FALSE"
        - name: MEDTRONIC_APP_TYPE
          value: java
        - name: CONTRAST__APPLICATION__VERSION
          value: WorldWideRevenue-03.09.01-Release
        - name: JAVA_TOOL_OPTIONS
          value: -javaagent:/otel/opentelemetry.jar
        envFrom:
        - secretRef:
            name: secrets
            optional: true
        - secretRef:
            name: conjur
            optional: true
        - configMapRef:
            name: configmap
        - configMapRef:
            name: otel-configmap
        - configMapRef:
            name: argo-mdt-configmap
        image: case.artifacts.medtronic.com/bcp_web-docker-releases-virtual/com/medtronic/web/app/bcp_web/finance/worldwiderevenue:03.09.01
        imagePullPolicy: Always
        name: argo-app
        ports:
        - containerPort: 8081
          name: http
          protocol: TCP
        resources:
          limits:
            cpu: "3"
            memory: 4000Mi
          requests:
            cpu: 100m
            memory: 1500Mi
        securityContext:
          allowPrivilegeEscalation: false
          capabilities:
            drop:
            - ALL
          privileged: false
          readOnlyRootFilesystem: true
          seccompProfile:
            type: RuntimeDefault
        terminationMessagePath: /dev/termination-log
        terminationMessagePolicy: File
        volumeMounts:
        - mountPath: /tmp
          name: tmp
        - mountPath: /otel
          name: otel-volume
        - mountPath: /var/log
          name: var-log
        - mountPath: /var/log/apache2
          name: var-log-apache2
        - mountPath: /run/apache2
          name: run-apache2
        - mountPath: /var/www/localhost/htdocs/login.html
          name: secretsvol
          readOnly: true
          subPath: login.html
        - mountPath: /opt/java/openjdk/lib/security/cacerts
          name: secretsvol
          readOnly: true
          subPath: cacerts
        - mountPath: /appDataDir/log4j-init-file.xml
          name: secretsvol
          readOnly: true
          subPath: log4j-init-file.xml
        - mountPath: /var/cache/mod_auth_openidc/metadata
          name: providers-vol
        - mountPath: /var/mntfiles/build.json
          name: secretsvol
          readOnly: true
          subPath: build.json
        - mountPath: /contrast/contrast_security.yml
          name: secretsvol
          readOnly: true
          subPath: contrast_security.yml
      dnsPolicy: ClusterFirst
      enableServiceLinks: false
      initContainers:
      - args:
        - |
          echo "adding OpenTelemetry agent..."
          cp /agents/opentelemetry.jar /otel/opentelemetry.jar
          echo "OpenTelemetry agent added!"
        command:
        - /bin/sh
        - -c
        image: case.artifacts.medtronic.com/bcp_web-docker-releases-virtual/com/medtronic/web/base/busybox-app-tomcat-init:stable
        imagePullPolicy: IfNotPresent
        name: argo-app-otel-init
        resources: {}
        securityContext:
          allowPrivilegeEscalation: false
          capabilities:
            drop:
            - ALL
          privileged: false
          seccompProfile:
            type: RuntimeDefault
        terminationMessagePath: /dev/termination-log
        terminationMessagePolicy: File
        volumeMounts:
        - mountPath: /otel
          name: otel-volume
      priorityClassName: dev-application
      restartPolicy: Always
      schedulerName: default-scheduler
      securityContext:
        fsGroup: 1000
        runAsGroup: 1000
        runAsNonRoot: true
        runAsUser: 1000
      serviceAccount: default
      serviceAccountName: default
      terminationGracePeriodSeconds: 30
      volumes:
      - emptyDir:
          sizeLimit: 5Mi
        name: tmp
      - emptyDir:
          sizeLimit: 25Mi
        name: otel-volume
      - emptyDir:
          sizeLimit: 50Mi
        name: var-log
      - emptyDir:
          sizeLimit: 50Mi
        name: var-log-apache2
      - emptyDir:
          sizeLimit: 1Mi
        name: run-apache2
      - name: secretsvol
        secret:
          defaultMode: 292
          items:
          - key: login.html
            path: login.html
          - key: cacerts
            path: cacerts
          - key: log4j-init-file.xml
            path: log4j-init-file.xml
          - key: contrast_security.yml
            path: contrast_security.yml
          - key: build.json
            path: build.json
          secretName: secrets-files
      - name: providers-vol
        secret:
          defaultMode: 292
          items:
          - key: dev.login.medtronic.com-2Foauth2-2Fausqxu4lutnO83mOb1d6.client
            path: dev.login.medtronic.com%2Foauth2%2Fausqxu4lutnO83mOb1d6.client
          - key: dev.login.medtronic.com-2Foauth2-2Fausqxu4lutnO83mOb1d6.conf
            path: dev.login.medtronic.com%2Foauth2%2Fausqxu4lutnO83mOb1d6.conf
          - key: dev.login.medtronic.com-2Foauth2-2Fausqxu4lutnO83mOb1d6.provider
            path: dev.login.medtronic.com%2Foauth2%2Fausqxu4lutnO83mOb1d6.provider
          - key: dev.login.medtronic.com.client
            path: dev.login.medtronic.com.client
          - key: dev.login.medtronic.com.conf
            path: dev.login.medtronic.com.conf
          - key: dev.login.medtronic.com.provider
            path: dev.login.medtronic.com.provider
          - key: login.medtronic.com-2Foauth2-2Faus16gc5wjVvJqXUg417.client
            path: login.medtronic.com%2Foauth2%2Faus16gc5wjVvJqXUg417.client
          - key: login.medtronic.com-2Foauth2-2Faus16gc5wjVvJqXUg417.conf
            path: login.medtronic.com%2Foauth2%2Faus16gc5wjVvJqXUg417.conf
          - key: login.medtronic.com-2Foauth2-2Faus16gc5wjVvJqXUg417.provider
            path: login.medtronic.com%2Foauth2%2Faus16gc5wjVvJqXUg417.provider
          - key: login.medtronic.com.client
            path: login.medtronic.com.client
          - key: login.medtronic.com.conf
            path: login.medtronic.com.conf
          - key: login.medtronic.com.provider
            path: login.medtronic.com.provider
          - key: login.microsoftonline.com-2F0a29d274-1367-4a8f-99c5-90c3dc7d4043-2Fv2.0.client
            path: login.microsoftonline.com%2F0a29d274-1367-4a8f-99c5-90c3dc7d4043%2Fv2.0.client
          - key: login.microsoftonline.com-2F0a29d274-1367-4a8f-99c5-90c3dc7d4043-2Fv2.0.conf
            path: login.microsoftonline.com%2F0a29d274-1367-4a8f-99c5-90c3dc7d4043%2Fv2.0.conf
          - key: login.microsoftonline.com-2F0a29d274-1367-4a8f-99c5-90c3dc7d4043-2Fv2.0.provider
            path: login.microsoftonline.com%2F0a29d274-1367-4a8f-99c5-90c3dc7d4043%2Fv2.0.provider
          - key: login.microsoftonline.com-2Fd73a39db-6eda-495d-8000-7579f56d68b7-2Fv2.0.client
            path: login.microsoftonline.com%2Fd73a39db-6eda-495d-8000-7579f56d68b7%2Fv2.0.client
          - key: login.microsoftonline.com-2Fd73a39db-6eda-495d-8000-7579f56d68b7-2Fv2.0.conf
            path: login.microsoftonline.com%2Fd73a39db-6eda-495d-8000-7579f56d68b7%2Fv2.0.conf
          - key: login.microsoftonline.com-2Fd73a39db-6eda-495d-8000-7579f56d68b7-2Fv2.0.provider
            path: login.microsoftonline.com%2Fd73a39db-6eda-495d-8000-7579f56d68b7%2Fv2.0.provider
          - key: stage.login.medtronic.com-2Foauth2-2Faus12k2r9jvwY2zRl417.client
            path: stage.login.medtronic.com%2Foauth2%2Faus12k2r9jvwY2zRl417.client
          - key: stage.login.medtronic.com-2Foauth2-2Faus12k2r9jvwY2zRl417.conf
            path: stage.login.medtronic.com%2Foauth2%2Faus12k2r9jvwY2zRl417.conf
          - key: stage.login.medtronic.com-2Foauth2-2Faus12k2r9jvwY2zRl417.provider
            path: stage.login.medtronic.com%2Foauth2%2Faus12k2r9jvwY2zRl417.provider
          - key: stage.login.medtronic.com.client
            path: stage.login.medtronic.com.client
          - key: stage.login.medtronic.com.conf
            path: stage.login.medtronic.com.conf
          - key: stage.login.medtronic.com.provider
            path: stage.login.medtronic.com.provider
          - key: test.login.medtronic.com-2Foauth2-2Faus1n3gftlEI6n8B70x7.client
            path: test.login.medtronic.com%2Foauth2%2Faus1n3gftlEI6n8B70x7.client
          - key: test.login.medtronic.com-2Foauth2-2Faus1n3gftlEI6n8B70x7.conf
            path: test.login.medtronic.com%2Foauth2%2Faus1n3gftlEI6n8B70x7.conf
          - key: test.login.medtronic.com-2Foauth2-2Faus1n3gftlEI6n8B70x7.provider
            path: test.login.medtronic.com%2Foauth2%2Faus1n3gftlEI6n8B70x7.provider
          - key: test.login.medtronic.com.client
            path: test.login.medtronic.com.client
          - key: test.login.medtronic.com.conf
            path: test.login.medtronic.com.conf
          - key: test.login.medtronic.com.provider
            path: test.login.medtronic.com.provider
          optional: true
          secretName: secrets-providers
status:
  conditions:
  - lastTransitionTime: "2026-03-12T15:48:36Z"
    lastUpdateTime: "2026-03-12T16:47:37Z"
    message: ReplicaSet "worldwiderevenue-dev-77949c68dd" has successfully progressed.
    reason: NewReplicaSetAvailable
    status: "True"
    type: Progressing
  - lastTransitionTime: "2026-03-12T16:50:54Z"
    lastUpdateTime: "2026-03-12T16:50:54Z"
    message: Deployment does not have minimum availability.
    reason: MinimumReplicasUnavailable
    status: "False"
    type: Available
  observedGeneration: 97
  replicas: 2
  unavailableReplicas: 2
  updatedReplicas: 2