#!/usr/bin/env bash
set -euo pipefail

VSPHERE_USER="mesac2@ent.core.medtronic.com"
DOMAIN="tkg.corp.medtronic.com"
VERBOSE_LEVEL="3"
REQ_TIMEOUT="20s"

# Optional: do logins first (keeps your auth fresh / ensures contexts exist).
# If you DON'T want login every time, set DO_LOGIN=0.
DO_LOGIN=1

CLUSTERS=(
  "abrc1-atlas-preview-prd"
  "abrcil-atlas-stg"
  "amvw1-atlas-dev"
  "awbyl-atlas-prd"
  "egwy2-atlas-prd"
  "egwy3-atlas-prd"
  "eutrl-atlas-prd"
)

if [[ "$DO_LOGIN" -eq 1 ]]; then
  echo "=== Step 1: Logging in to all listed clusters ==="
  for CLUSTER in "${CLUSTERS[@]}"; do
    PREFIX="${CLUSTER%%-*}"              # e.g., abrc1
    SERVER="${PREFIX}-${DOMAIN}"         # e.g., abrc1-tkg.corp.medtronic.com

    echo
    echo "LOGIN -> CLUSTER: $CLUSTER | SERVER: $SERVER"
    if ! kubectl vsphere login \
        --server="$SERVER" \
        -u "$VSPHERE_USER" \
        --tanzu-kubernetes-cluster-name "$CLUSTER" \
        --insecure-skip-tls-verify \
        -v "$VERBOSE_LEVEL"; then
      echo "WARN: login failed for $CLUSTER ($SERVER). Continuing..."
    fi
  done
  echo
fi

echo "=== Step 2: Running 'kubectl get ns' for ALL contexts in kubeconfig ==="

mapfile -t ALL_CONTEXTS < <(kubectl config get-contexts -o name)

if [[ "${#ALL_CONTEXTS[@]}" -eq 0 ]]; then
  echo "ERROR: No contexts found in kubeconfig."
  exit 1
fi

echo "Total contexts: ${#ALL_CONTEXTS[@]}"
echo

FAILS=0

for CTX in "${ALL_CONTEXTS[@]}"; do
  echo "------------------------------------------------------------"
  echo "CONTEXT: $CTX"

  if ! kubectl config use-context "$CTX" >/dev/null 2>&1; then
    echo "FAIL: cannot switch to context"
    ((FAILS+=1))
    continue
  fi

  if kubectl get ns --request-timeout="$REQ_TIMEOUT" >/dev/null 2>&1; then
    echo "OK"
  else
    echo "FAIL (details below)"
    kubectl get ns --request-timeout="$REQ_TIMEOUT" || true
    ((FAILS+=1))
  fi
done

echo "============================================================"
echo "Done. Failed contexts: $FAILS"
exit 0
