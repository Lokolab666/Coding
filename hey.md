 kubectl get deployment itops-nginx -n cis-genai-poc-system -o jsonpath='{.spec.template.spec.nodeSelector}{"\n"}'
{"kubernetes.io/hostname":"awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-5pltm"}
mesac2@CPC-mesac-4BM7S:~$ kubectl get deployment itops-nginx -n cis-genai-poc-system -o jsonpath='{.metadata.labels}{"\n"}{.metadata.annotations}{"\n"}'
{"app":"itops-nginx","app.kubernetes.io/managed-by":"Helm"}
{"deployment.kubernetes.io/revision":"3","meta.helm.sh/release-name":"itops-nginx","meta.helm.sh/release-namespace":"cis-genai-poc-system"}

kubectl get pods -A -o json | jq -r '
.items[]
| select(.spec.nodeSelector["kubernetes.io/hostname"] != null)
| [
    .metadata.namespace,
    .metadata.name,
    .spec.nodeSelector["kubernetes.io/hostname"]
  ]
| @tsv'
cis-genai-poc-system    itops-nginx-86c87cf945-btgzn    awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-5pltm
mesac2@CPC-mesac-4BM7S:~$ kubectl get deployment itops-nginx -n cis-genai-poc-system -o yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  annotations:
    deployment.kubernetes.io/revision: "3"
    meta.helm.sh/release-name: itops-nginx
    meta.helm.sh/release-namespace: cis-genai-poc-system
  creationTimestamp: "2026-07-15T14:22:41Z"
  generation: 3
  labels:
    app: itops-nginx
    app.kubernetes.io/managed-by: Helm
  name: itops-nginx
  namespace: cis-genai-poc-system
  resourceVersion: "372027693"
  uid: 93b0f9c7-5861-4e1e-89d1-7fdc441c4dc0
spec:
  progressDeadlineSeconds: 600
  replicas: 1
  revisionHistoryLimit: 10
  selector:
    matchLabels:
      app: itops-nginx
  strategy:
    rollingUpdate:
      maxSurge: 25%
      maxUnavailable: 25%
    type: RollingUpdate
  template:
    metadata:
      annotations:
        kubectl.kubernetes.io/restartedAt: "2026-07-27T11:07:57Z"
      creationTimestamp: null
      labels:
        app: itops-nginx
    spec:
      containers:
      - image: case.artifacts.medtronic.com/genai-project-team-docker-dev-local/itops-nginx-medtronics:Version1
        imagePullPolicy: IfNotPresent
        name: itops-nginx
        ports:
        - containerPort: 8080
          protocol: TCP
        - containerPort: 8443
          protocol: TCP
        resources:
          limits:
            cpu: "1"
            memory: 2Gi
          requests:
            cpu: 500m
            memory: 1Gi
        securityContext:
          allowPrivilegeEscalation: false
          capabilities:
            drop:
            - ALL
          runAsGroup: 101
          runAsNonRoot: true
          runAsUser: 101
          seccompProfile:
            type: RuntimeDefault
        terminationMessagePath: /dev/termination-log
        terminationMessagePolicy: File
        volumeMounts:
        - mountPath: /etc/nginx/nginx.conf
          name: itops-nginx-config
          subPath: nginx.conf
        - mountPath: /etc/ssl/certs
          name: tls
          readOnly: true
        - mountPath: /var/cache/nginx
          name: nginx-cache
        - mountPath: /run
          name: run-dir
      dnsPolicy: ClusterFirst
      nodeSelector:
        kubernetes.io/hostname: awby1-atlas-prd-worker-node-pool-gdgxc-p7m2b-5pltm
      restartPolicy: Always
      schedulerName: default-scheduler
      securityContext: {}
      terminationGracePeriodSeconds: 30
      volumes:
      - configMap:
          defaultMode: 420
          name: itops-nginx-config
        name: itops-nginx-config
      - name: tls
        secret:
          defaultMode: 420
          secretName: itops-tls
      - emptyDir: {}
        name: nginx-cache
      - emptyDir: {}
        name: run-dir
status:
  conditions:
  - lastTransitionTime: "2026-07-15T14:22:41Z"
    lastUpdateTime: "2026-07-27T11:08:00Z"
    message: ReplicaSet "itops-nginx-86c87cf945" has successfully progressed.
    reason: NewReplicaSetAvailable
    status: "True"
    type: Progressing
  - lastTransitionTime: "2026-08-17T19:29:49Z"
    lastUpdateTime: "2026-08-17T19:29:49Z"
    message: Deployment does not have minimum availability.
    reason: MinimumReplicasUnavailable
    status: "False"
    type: Available
  observedGeneration: 3
  replicas: 1
  unavailableReplicas: 1
  updatedReplicas: 1