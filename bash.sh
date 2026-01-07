#!/usr/bin/env bash
set -euo pipefail

DOMAIN="tkg.corp.medtronic.com"
VSPHERE_USER="mesac2@ent.core.medtronic.com"
VERBOSE_LEVEL="3"
REQ_TIMEOUT="20s"

CLUSTERS=(
  "abrc1-atlas-preview-prd"
  "abrcil-atlas-stg"
  "amvw1-atlas-dev"
  "awbyl-atlas-prd"
  "egwy2-atlas-prd"
  "egwy3-atlas-prd"
  "eutrl-atlas-prd"
)

get_cluster_ref_for_context() {
  local ctx="$1"
  kubectl config view -o jsonpath="{.contexts[?(@.name==\"${ctx}\")].context.cluster}"
}

list_contexts_for_cluster_ref() {
  local cluster_ref="$1"
  # Print: contextName <TAB> clusterRef, then filter by clusterRef
  kubectl config view -o jsonpath='{range .contexts[*]}{.name}{"\t"}{.context.cluster}{"\n"}{end}' \
    | awk -F'\t' -v c="$cluster_ref" '$2==c {print $1}'
}

for CLUSTER in "${CLUSTERS[@]}"; do
  PREFIX="${CLUSTER%%-*}"            # e.g., abrc1
  SERVER="${PREFIX}-${DOMAIN}"       # e.g., abrc1-tkg.corp.medtronic.com

  echo "============================================================"
  echo "TANZU CLUSTER: $CLUSTER"
  echo "SERVER:       $SERVER"
  echo "------------------------------------------------------------"

  echo "[1/3] kubectl vsphere login..."
  kubectl vsphere login \
    --server="$SERVER" \
    -u "$VSPHERE_USER" \
    --tanzu-kubernetes-cluster-name "$CLUSTER" \
    --insecure-skip-tls-verify \
    -v "$VERBOSE_LEVEL"

  echo
  echo "[2/3] Discovering all contexts that belong to THIS cluster (by kubeconfig cluster ref)..."
  BASE_CTX="$(kubectl config current-context)"
  CLUSTER_REF="$(get_cluster_ref_for_context "$BASE_CTX")"

  if [[ -z "$CLUSTER_REF" ]]; then
    echo "ERROR: Could not resolve kubeconfig cluster reference from current-context: $BASE_CTX"
    echo "Skipping..."
    echo
    continue
  fi

  mapfile -t CONTEXTS < <(list_contexts_for_cluster_ref "$CLUSTER_REF" || true)

  if [[ "${#CONTEXTS[@]}" -eq 0 ]]; then
    echo "ERROR: No contexts found for cluster ref: $CLUSTER_REF"
    echo "Skipping..."
    echo
    continue
  fi

  echo "Base context:  $BASE_CTX"
  echo "Cluster ref:   $CLUSTER_REF"
  echo "Contexts (${#CONTEXTS[@]}):"
  printf " - %s\n" "${CONTEXTS[@]}"

  echo
  echo "[3/3] Running 'kubectl get ns' in each context..."
  for CTX in "${CONTEXTS[@]}"; do
    echo
    echo ">>> CONTEXT: $CTX"
    kubectl config use-context "$CTX" >/dev/null

    if kubectl get ns --request-timeout="$REQ_TIMEOUT" >/dev/null 2>&1; then
      echo "OK: kubectl get ns succeeded"
    else
      echo "FAIL: kubectl get ns failed (details below)"
      kubectl get ns --request-timeout="$REQ_TIMEOUT" || true
    fi
  done

  echo
done
