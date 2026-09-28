Worked for 1m 57s

The correct target is:

Host: edcmdt-test.medtronic.com

Cluster: Argo DEV

Namespace: argo-oracleclinicalrdcsso-dev


Do not start with the database. The ticket mixes three different paths: client → application URL, Kubernetes → database, and legacy application servers → application. First identify where the failure occurs.

Also confirm the server names: mspm7aappddd0136 and mspm7appd0137 do not follow the same naming pattern and may contain typos.

1. Establish the Kubernetes baseline

Run this first in Cloud9:

export KUBECONFIG="$HOME/.kube/argo-dev.yaml"
K=./kubectl
NS=argo-oracleclinicalrdcsso-dev
HOST=edcmdt-test.medtronic.com

date -u
$K config current-context

$K -n "$NS" get deploy,rs,pods,svc,endpoints,endpointslices,ingress -o wide

$K -n "$NS" get events \
  --sort-by=.metadata.creationTimestamp | tail -n 60

Then collect the pod state clearly:

$K -n "$NS" get pods -o custom-columns='NAME:.metadata.name,READY:.status.containerStatuses[*].ready,RESTARTS:.status.containerStatuses[*].restartCount,STATUS:.status.phase,NODE:.spec.nodeName,IP:.status.podIP,STARTED:.status.startTime'

Do not assume the previous “healthy 2/2” result remains valid.

2. Validate hostname → Ingress → ALB

$K -n "$NS" get ingress -o custom-columns='NAME:.metadata.name,CLASS:.spec.ingressClassName,HOSTS:.spec.rules[*].host,ADDRESS:.status.loadBalancer.ingress[*].hostname'

$K -n "$NS" describe ingress

Confirm that:

edcmdt-test.medtronic.com appears under the Ingress rules.

An ALB hostname appears under ADDRESS.

The certificate annotation includes the correct ACM certificate.

No reconciliation or ALB errors appear in Events.


Validate the external endpoint from Cloud9:

dig +short "$HOST"

curl -v \
  --connect-timeout 10 \
  --max-time 30 \
  -o /dev/null \
  "https://$HOST/"

Check the certificate:

echo | openssl s_client \
  -connect "${HOST}:443" \
  -servername "$HOST" 2>/dev/null |
openssl x509 -noout -subject -issuer -dates -ext subjectAltName

Do not use curl -k initially because it hides certificate problems.

3. Check recent deployment changes

for deployment in $($K -n "$NS" get deployments -o name); do
  echo "===== $deployment ====="
  $K -n "$NS" rollout status "$deployment" --timeout=15s
  $K -n "$NS" rollout history "$deployment"
done

Review ReplicaSet creation dates and images:

$K -n "$NS" get replicasets \
  --sort-by=.metadata.creationTimestamp \
  -o custom-columns='NAME:.metadata.name,CREATED:.metadata.creationTimestamp,DESIRED:.spec.replicas,READY:.status.readyReplicas,IMAGE:.spec.template.spec.containers[*].image'

Check Flux reconciliation:

$K get \
  gitrepositories.source.toolkit.fluxcd.io,kustomizations.kustomize.toolkit.fluxcd.io \
  -A

Kubernetes can show the current revision and recent rollouts, but it cannot reliably prove “what changed.” GitLab and Flux repository history are authoritative for that.

4. Inspect application logs

for pod in $($K -n "$NS" get pods -o name); do
  echo "===== $pod ====="

  $K -n "$NS" logs "$pod" \
    --all-containers \
    --since=4h \
    --tail=500 2>&1 |
  grep -Ei 'ORA-|JDBC|SQL|timeout|refused|unreachable|SSL|PKIX|exception|error' |
  tail -n 100
done

If a container restarted, also check its previous instance:

$K -n "$NS" logs <pod-name> \
  --all-containers \
  --previous \
  --tail=300

5. Validate the database only if indicated

First find the actual JDBC host and port. Do not assume Oracle port 1521:

$K -n "$NS" get deployments,configmaps -o yaml |
grep -niE 'MDTOPT|mspldb666|jdbc:oracle|oracle:thin'

Do not paste passwords, tokens, or decoded Secret values into the ticket.

Select a running application pod:

POD=$($K -n "$NS" get pods \
  --field-selector=status.phase=Running \
  -o jsonpath='{.items[0].metadata.name}')

echo "$POD"

Test DNS from the pod:

$K -n "$NS" exec "$POD" -- getent hosts mspldb666

After obtaining the configured database port:

$K -n "$NS" exec "$POD" -- \
  sh -c 'nc -vz -w 5 mspldb666 <DB_PORT>'

If nc is unavailable, do not install utilities in the application container; use an approved temporary diagnostic pod or test through the existing application logs.

Interpretation

Result	Likely layer

NXDOMAIN or no DNS result	DNS problem, before Kubernetes
DNS does not correspond to the Ingress ALB	Stale or incorrect DNS record
Certificate expired or hostname missing from SAN	ACM/certificate configuration
HTTP 403, especially Server: awselb/2.0	WAF/ALB rule; inspect WAF logs
HTTP 502 or 503	Service endpoints, readiness, target group or pod failure
HTTP 200/302 from Cloud9 and pods healthy	Source-specific problem from the reported servers
HTTP 500 plus ORA-/JDBC errors	Application-to-database failure
DB hostname resolves but port times out	Firewall, security group, NetworkPolicy or DB listener
Restarts/failed readiness	Application/runtime problem


If Cloud9 receives 200 or 302, the ticket still lacks enough evidence. The requester must provide:

Exact error or screenshot.

Full URL/path being accessed.

Failure timestamp and timezone.

Whether all three application servers fail.

curl -v https://edcmdt-test.medtronic.com/ output from an affected server.

Source IP of the affected server.

Confirmed spellings of all three server names.

Expected database port and Oracle service name.


Paste the outputs from sections 1 and 2 first. Those results will determine whether we continue with WAF, pods, or database connectivity.






Worked for 21s

Possibly, but there is currently no evidence of a Secrets Provider failure.

The earlier EDC problem involving signin-stage.medtronic.com.provider was an OIDC/Auth0 provider definition, not Kubernetes Secrets Store CSI. The word “provider” is being used for two unrelated components.

A Secrets Provider problem normally produces:

Pods stuck in ContainerCreating.

FailedMount events.

SecretProviderClass not found.

failed to mount secrets store objects.

AWS AccessDenied/IRSA errors.

Application startup failures caused by missing credentials.


Run these checks:

export KUBECONFIG="$HOME/.kube/argo-dev.yaml"
K=./kubectl
NS=argo-oracleclinicalrdcsso-dev

$K -n "$NS" get pods
$K -n "$NS" get events \
  --sort-by=.metadata.creationTimestamp |
grep -Ei 'secret|provider|csi|mount|accessdenied|failed'

Check whether the application actually uses the Secrets Store CSI driver:

$K -n "$NS" get pods -o yaml |
grep -nEi 'secrets-store|secretProviderClass|csi:|secretName'

Check the relevant resources:

$K -n "$NS" get secretproviderclasses.secrets-store.csi.x-k8s.io 2>/dev/null

$K -n "$NS" get externalsecrets.external-secrets.io 2>/dev/null

$K -n "$NS" get secrets \
  -o custom-columns='NAME:.metadata.name,TYPE:.type,CREATED:.metadata.creationTimestamp'

For every pod, extract only the event section:

for pod in $($K -n "$NS" get pods -o name); do
  echo "===== $pod ====="
  $K -n "$NS" describe "$pod" | sed -n '/Events:/,$p'
done

If CSI is being used, check its platform components:

$K -n kube-system get daemonsets,pods |
grep -Ei 'secret|csi|provider'

Also inspect the application logs for missing/invalid credentials:

for pod in $($K -n "$NS" get pods -o name); do
  echo "===== $pod ====="

  $K -n "$NS" logs "$pod" \
    --all-containers \
    --since=4h \
    --tail=500 2>&1 |
  grep -Ei 'secret|credential|password|accessdenied|ORA-|JDBC|authentication|failed'
done

Interpretation:

FailedMount or SecretProviderClass error → Secrets Provider/CSI issue.

AWS AccessDenied → likely service-account/IRSA permission issue.

Secret mounted but ORA-01017 appears → secret value or DB credential is invalid, not a CSI availability problem.

Pods remain healthy 2/2, no secret events, and the URL returns 403/timeout → investigate ALB/WAF/DNS instead.

Pods are healthy but return 500 with JDBC errors → investigate DB credentials/connectivity.


Do not decode or paste Secret values. Send the pod status, filtered events, SecretProviderClass status, and filtered logs; that will confirm or eliminate this hypothesis.
