Yes. You should ask for a little more information before executing the script.

Your first action—requesting access to the .sh file—is correct. This is a production cluster (aply3-atlas-prd), and the script is expected to modify persistent storage. I would not run a script in PROD without reviewing exactly what it patches, deletes, recreates, or waits for.

The incident seems to mean:

Current HiveMQ PVC size
        ↓
      <100Gi
        ↓
expand-hivemq-pvc.sh
        ↓
Existing PVCs → 100Gi
        +
StatefulSet volumeClaimTemplates → 100Gi
        ↓
livelink-sanity-check.sh
        ↓
Verify LiveLink/HiveMQ is healthy

There are three things you need to resolve first.

First, confirm the target namespace/resources. Your cluster contains both:

livelink-hivemq
v2containers-hivemq

The incident does not explicitly say which one the expansion script should affect. SITE_LABEL=pnl suggests it is probably related to the LiveLink environment, but that is not enough evidence to assume livelink-hivemq.

I would ask the requester:

> Can you confirm the exact HiveMQ namespace and StatefulSet(s) that should be expanded? I see both livelink-hivemq and v2containers-hivemq in aply3-atlas-prd.



That is the most important missing detail.

Second, verify your permissions. According to the internal Atlas RBAC documentation, users normally have view access in production namespaces and cannot create, modify, or delete resources with that base access. 

So before trying the script, run:

kubectl auth whoami

Then, once the namespace is confirmed—for example, if it is livelink-hivemq:

kubectl auth can-i get pvc -n livelink-hivemq
kubectl auth can-i patch pvc -n livelink-hivemq
kubectl auth can-i update pvc -n livelink-hivemq

kubectl auth can-i get statefulset -n livelink-hivemq
kubectl auth can-i patch statefulset -n livelink-hivemq
kubectl auth can-i update statefulset -n livelink-hivemq
kubectl auth can-i delete statefulset -n livelink-hivemq

The delete statefulset check is intentional. I don't yet know what their script does. Some PVC-expansion scripts that need to change a StatefulSet template use delete/recreate or orphaning techniques because simply changing the storage template can be problematic.

Do not actually delete anything. We're only asking Kubernetes:

Would mesac2 be allowed to do this?

Third, inspect the current storage configuration before modifying it.

These commands are read-only and safe to run now.

Assuming the target is livelink-hivemq:

kubectl get sts -n livelink-hivemq

Then:

kubectl get pvc -n livelink-hivemq -o wide

A more useful view is:

kubectl get pvc -n livelink-hivemq \
  -o custom-columns='NAME:.metadata.name,STATUS:.status.phase,REQUESTED:.spec.resources.requests.storage,CAPACITY:.status.capacity.storage,STORAGECLASS:.spec.storageClassName'

This tells us:

PVC
Current requested size
Actual capacity
StorageClass

Then inspect the StatefulSets:

kubectl get sts -n livelink-hivemq -o yaml

Or a shorter view:

kubectl get sts -n livelink-hivemq \
  -o jsonpath='{range .items[*]}{"STS: "}{.metadata.name}{"\n"}{range .spec.volumeClaimTemplates[*]}{"  PVC Template: "}{.metadata.name}{" -> "}{.spec.resources.requests.storage}{"\n"}{end}{end}'

You might see something like:

STS: hivemq
  PVC Template: data -> 50Gi

which would establish the before-state.

Also check which StorageClass those PVCs use:

kubectl get pvc -n livelink-hivemq \
  -o custom-columns='NAME:.metadata.name,SC:.spec.storageClassName'

Then:

kubectl get storageclass <STORAGE_CLASS_NAME> -o yaml

Look specifically for:

allowVolumeExpansion: true

This is required for Kubernetes PVC expansion. Kubernetes only permits a PVC to grow when the underlying StorageClass allows expansion; it also does not support shrinking the volume. 

So if you find:

allowVolumeExpansion: false

or there is no expansion support for that CSI/storage configuration, stop. The supplied script will not magically bypass the storage backend limitation.

There is another thing I want you to check because this is HiveMQ on Atlas. The internal documentation says HiveMQ is supported by the Atlas team, but the Atlas-specific HiveMQ documentation is currently marked as TBA. 

That makes reviewing the team's custom script even more important. There isn't an internal Atlas procedure I can use to safely infer what expand-hivemq-pvc.sh does.

I would therefore send the requester these questions before execution:

1. Confirm the exact namespace: livelink-hivemq, v2containers-hivemq, or another namespace.


2. Confirm the exact HiveMQ StatefulSet(s)/PVCs that must reach 100Gi.


3. Confirm whether this expansion is expected to be online with no HiveMQ restart/downtime, or whether a maintenance window is expected.


4. Confirm whether 100Gi is the requested size for every HiveMQ PVC or only specific data PVCs.


5. Confirm that the script in manifests/main is the approved version for aply3-atlas-prd.


6. If the script requires permissions beyond your PROD RBAC, confirm whether the Atlas team is expected to execute it or temporarily provide the required access.



I would not ask many more questions than that. We can determine most of the technical details ourselves with read-only commands.

Most importantly, do not run this yet:

TARGET_SIZE=100Gi ./scripts/hivemq-expand-pvc/expand-hivemq-pvc.sh

until we have seen the script.

There is a significant clue in their verification statement:

> “All PVCs and STS volumeClaimTemplates show 100Gi.”



Existing PVCs and StatefulSet claim templates are two related but different things. Kubernetes uses volumeClaimTemplates to create the persistent claims associated with StatefulSet pods. 

So the script probably does more than:

kubectl patch pvc ...

It must also ensure the controller's desired configuration is 100Gi, otherwise future pods/claims could retain the old desired value. That is exactly why I want to inspect the script before telling you it is safe.

For now, run these and send me the outputs:

kubectl config current-context

kubectl auth whoami

kubectl get ns livelink-hivemq --show-labels

kubectl get sts -n livelink-hivemq

kubectl get pvc -n livelink-hivemq \
  -o custom-columns='NAME:.metadata.name,STATUS:.status.phase,REQUESTED:.spec.resources.requests.storage,CAPACITY:.status.capacity.storage,STORAGECLASS:.spec.storageClassName'

kubectl auth can-i patch pvc -n livelink-hivemq
kubectl auth can-i update statefulset -n livelink-hivemq
kubectl auth can-i delete statefulset -n livelink-hivemq

Once you get access to expand-hivemq-pvc.sh, paste the complete script here. I can review it line by line and tell you exactly what it will change in aply3-atlas-prd, what the risk is, and the precise execution/rollback/verification sequence.
