kubectl-vsphere logout

kubectl-vsphere login \
  --server="abrc1l-tkg.corp.medtronic.com" \
  --vsphere-username="mesac2@ent.core.medtronic.com" \
  --tanzu-kubernetes-cluster-name="<WORKLOAD_CLUSTER_FROM_CSV>" \
  --tanzu-kubernetes-cluster-namespace="abrc1l-prod-ns" \
  --insecure-skip-tls-verify
