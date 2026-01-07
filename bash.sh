#!/usr/bin/env bash
set -u

DOMAIN="tkg.corp.medtronic.com"
VSPHERE_USER="mesac2@ent.core.medtronic.com"
VERBOSE_LEVEL="3"

CLUSTERS=(
  "abrc1-atlas-preview-prd"
  "abrcil-atlas-stg"
  "amvw1-atlas-dev"
  "awbyl-atlas-prd"
  "egwy2-atlas-prd"
  "egwy3-atlas-prd"
  "eutrl-atlas-prd"
)

for CLUSTER in "${CLUSTERS[@]}"; do
  PREFIX="${CLUSTER%%-*}"                  # e.g., abrc1 from abrc1-atlas-preview-prd
  SERVER="${PREFIX}-${DOMAIN}"             # e.g., abrc1-tkg.corp.medtronic.com

  echo "============================================================"
  echo "CLUSTER: $CLUSTER"
  echo "SERVER:  $SERVER"
  echo "------------------------------------------------------------"

  echo "[1/3] kubectl vsphere login..."
  if ! kubectl vsphere login \
      --server="$SERVER" \
      -u "$VSPHERE_USER" \
      --tanzu-kubernetes-cluster-name "$CLUSTER" \
      --insecure-skip-tls-verify \
      -v "$VERBOSE_LEVEL"; then
    echo "ERROR: Login failed for cluster: $CLUSTER (server: $SERVER)"
    echo "Skipping..."
    echo
    continue
  fi

  echo
  echo "[2/3] Finding contexts for this cluster..."
  mapfile -t CONTEXTS < <(kubectl config get-contexts -o name | grep -F "$CLUSTER" || true)

  # Fallback: use the current context right after login
  if [ "${#CONTEXTS[@]}" -eq 0 ]; then
    CURRENT_CTX="$(kubectl config current-context 2>/dev/null || true)"
    if [ -n "$CURRENT_CTX" ]; then
      CONTEXTS=("$CURRENT_CTX")
      echo "No matching contexts found by name; using current-context: $CURRENT_CTX"
    else
      echo "ERROR: No contexts found after login. Skipping..."
      echo
      continue
    fi
  else
    printf "Contexts found (%d):\n" "${#CONTEXTS[@]}"
    printf " - %s\n" "${CONTEXTS[@]}"
  fi

  echo
  echo "[3/3] kubectl get nodes for each context..."
  for CTX in "${CONTEXTS[@]}"; do
    echo
    echo ">>> CONTEXT: $CTX"
    if ! kubectl config use-context "$CTX" >/dev/null 2>&1; then
      echo "ERROR: Could not switch to context: $CTX"
      continue
    fi

    kubectl get nodes -o wide || echo "ERROR: kubectl get nodes failed for context: $CTX"
  done

  echo
done




chmod +x tkg_nodes.sh
./tkg_nodes.sh


