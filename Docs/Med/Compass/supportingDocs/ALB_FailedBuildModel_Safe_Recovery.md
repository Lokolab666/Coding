# ALB FailedBuildModel Safe Recovery Runbook

## Purpose
Use this runbook when an application ingress ALB group change fails with a `FailedBuildModel` event.

Goal: recover routing without deleting or recreating shared base ALBs that may be used by other applications.

## Scope
- Cluster: Any Argo cluster managed by Flux.
- Namespace: Application namespace that owns the failing ingress.
- Typical trigger: App moved between ALB groups (for example internal to external, or one ALB suffix to another).

## For Application Teams
Do not manually patch base ALB ingress resources and do not disable `deletion_protection` directly in the cluster.

Submit a ticket to **Infra-Argo-Global** with:
- Cluster name
- Namespace
- Ingress name
- Target ALB group and scheme
- Pipeline/job URL
- Output showing `FailedBuildModel`

## Infra-Argo-Global Recovery Steps (Safe Mode)
1. Confirm the failing ingress and error.
```bash
kubectl --kubeconfig ~/.kube/<cluster>.yaml -n <app-namespace> get ingress <ingress-name> -o yaml
kubectl --kubeconfig ~/.kube/<cluster>.yaml -n <app-namespace> get events --field-selector involvedObject.kind=Ingress,involvedObject.name=<ingress-name>,reason=FailedBuildModel --sort-by=.lastTimestamp
```

2. Confirm base ALB ingresses in `alb-base-config` are present and healthy.
```bash
kubectl --kubeconfig ~/.kube/<cluster>.yaml -n alb-base-config get ingress
```

3. Verify Flux ownership is healthy before making changes.
```bash
flux --kubeconfig ~/.kube/<cluster>.yaml get kustomization flux-gitops-infrastructure -n flux-system
```

4. Fix only the application ingress patch in the application repo.
- Set expected annotations only:
  - `alb.ingress.kubernetes.io/group.name`
  - `alb.ingress.kubernetes.io/scheme`
- Do not edit `alb.ingress.kubernetes.io/load-balancer-attributes` as part of app reassignment.

5. Commit and push application repo change.

6. If Flux ownership migration overlap exists, protect against prune side effects first.
- Suspend old owner and set `prune:false` until ownership is stable.
- Reconcile new owner.
- Resume old owner only after overlap is removed.

7. Reconcile and verify.
```bash
flux --kubeconfig ~/.kube/<cluster>.yaml reconcile kustomization flux-gitops-infrastructure -n flux-system --with-source
kubectl --kubeconfig ~/.kube/<cluster>.yaml -n <app-namespace> get ingress <ingress-name> -o jsonpath='{.metadata.annotations.alb\.ingress\.kubernetes\.io/group\.name}{"\n"}{.status.loadBalancer.ingress[0].hostname}{"\n"}'
kubectl --kubeconfig ~/.kube/<cluster>.yaml -n <app-namespace> get events --field-selector involvedObject.kind=Ingress,involvedObject.name=<ingress-name>,reason=FailedBuildModel --sort-by=.lastTimestamp
```

8. Validate no unintended ALB churn.
```bash
kubectl --kubeconfig ~/.kube/<cluster>.yaml -n alb-base-config get ingress
```
Check that existing shared base ALB ingress objects remain present and keep expected addresses.

## Do Not Do
- Do not delete base ALB ingress resources in `alb-base-config`.
- Do not force remove ingress finalizers unless this is a namespace-termination incident run by platform owners.
- Do not run ad-hoc `kubectl annotate --overwrite` on base ALB load-balancer attributes for normal app reassignment.

## Escalation Criteria
Escalate to platform incident response if any of the following occur:
- Base ALB ingress object disappears from `alb-base-config`.
- Namespace enters `Terminating` unexpectedly.
- Multiple applications lose ALB addresses simultaneously.
