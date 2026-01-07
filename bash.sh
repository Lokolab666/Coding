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
  PREFIX="${CLUSTER%%-*}"            # abrc1 from abrc1-atlas-preview-prd
  SERVER="${PREFIX}-${DOMAIN}"       # abrc1-tkg.corp.medtronic.com

  echo "============================================================"
  echo "CLUSTER: $CLUSTER"
  echo "SERVER:  $SERVER"
  echo "PREFIX:  $PREFIX"
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
  echo "[2/3] Selecting contexts for prefix '$PREFIX' (won't match '${PREFIX}l', etc.)..."

  # Match contexts that start exactly with PREFIX followed by '-', '.' or end-of-string:
  #   abrc1-dev-ns ✅
  #   abrc1-atlas-preview-prd ✅
  #   abrc1-tkg.corp.medtronic.com ✅
  #   abrc1l-dev-ns ❌ (won't match)
  mapfile -t CONTEXTS < <(
    kubectl config get-contexts -o name \
      | grep -E "^${PREFIX}(-|\\.|$)" \
      || true
  )

  if [ "${#CONTEXTS[@]}" -eq 0 ]; then
    echo "ERROR: No contexts found for prefix '$PREFIX' after login."
    echo
    continue
  fi

  printf "Contexts found (%d):\n" "${#CONTEXTS[@]}"
  printf " - %s\n" "${CONTEXTS[@]}"

  echo
  echo "[3/3] Validating 'kubectl get ns' in each context..."
  for CTX in "${CONTEXTS[@]}"; do
    echo
    echo ">>> CONTEXT: $CTX"

    if ! kubectl config use-context "$CTX" >/dev/null 2>&1; then
      echo "ERROR: Could not switch to context: $CTX"
      continue
    fi

    # Validate connectivity/permissions by listing namespaces (with a timeout)
    if kubectl get ns --request-timeout=20s >/dev/null 2>&1; then
      echo "OK: kubectl get ns succeeded"
    else
      echo "FAIL: kubectl get ns failed (no access / auth / cluster unreachable)"
      # Optional: show the error detail
      kubectl get ns --request-timeout=20s
    fi
  done

  echo
done
