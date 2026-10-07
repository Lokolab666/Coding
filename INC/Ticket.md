#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
INVENTORY_CSV="${INVENTORY_CSV:-${SCRIPT_DIR}/atlas-tanzu-inventory.csv}"

LEGACY_PLUGIN="kubectl-vsphere"
V9_PLUGIN="kubectl-vsphere-9"
DOMAIN_SUFFIX="tkg.corp.medtronic.com"
V9_AVAILABLE=1

usage() {
  cat <<EOF
Usage: ${0##*/} (--cluster <name> [--cluster <name> ...] | --all)

Logs in to Tanzu Kubernetes clusters listed in ${INVENTORY_CSV}.

Options:
  --cluster <name>  Cluster to log in to. Repeatable.
  --all             Log in to every cluster in the inventory CSV.
  -h, --help        Show this help.

Required environment variables:
  KUBECTL_VSPHERE_USERNAME
  KUBECTL_VSPHERE_PASSWORD
EOF
}

log() { printf '%s\n' "$*" >&2; }

die() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

check_environment() {
  local missing=()
  [[ -n "${KUBECTL_VSPHERE_USERNAME:-}" ]] || missing+=("KUBECTL_VSPHERE_USERNAME")
  [[ -n "${KUBECTL_VSPHERE_PASSWORD:-}" ]] || missing+=("KUBECTL_VSPHERE_PASSWORD")
  if ((${#missing[@]} > 0)); then
    die "unset or empty environment variable(s): ${missing[*]}"
  fi
}

# Prints the major version reported by `<plugin> version`.
plugin_major_version() {
  local plugin="$1" raw
  raw="$("$plugin" version 2>/dev/null | sed -n 's/.*version \([0-9][0-9.]*\).*/\1/p' | head -n 1)"
  [[ -n "$raw" ]] || return 1
  printf '%s\n' "${raw%%.*}"
}

check_plugins() {
  local major
  command -v "$LEGACY_PLUGIN" >/dev/null 2>&1 || die "executable not found on PATH: ${LEGACY_PLUGIN}"
  major="$(plugin_major_version "$LEGACY_PLUGIN")" ||
    die "could not parse version output of ${LEGACY_PLUGIN}"
  ((major < 9)) ||
    die "${LEGACY_PLUGIN} reports major version ${major}; expected less than 9"

  # v9 retry pathway is optional: disable it silently rather than exiting if missing or unsuitable.
  if ! command -v "$V9_PLUGIN" >/dev/null 2>&1; then
    V9_AVAILABLE=0
  elif ! major="$(plugin_major_version "$V9_PLUGIN")"; then
    V9_AVAILABLE=0
  elif ((major < 9)); then
    V9_AVAILABLE=0
  fi
}

trim() {
  local s="$1"
  s="${s#"${s%%[![:space:]]*}"}"
  s="${s%"${s##*[![:space:]]}"}"
  printf '%s\n' "$s"
}

# Emits "<cluster>,<namespace>,<plugin>" for every data row of the inventory CSV.
read_inventory() {
  local cluster namespace plugin first=1
  while IFS=, read -r cluster namespace plugin || [[ -n "${cluster:-}" ]]; do
    if ((first)); then
      first=0
      continue
    fi
    cluster="$(trim "${cluster:-}")"
    namespace="$(trim "${namespace:-}")"
    plugin="$(trim "${plugin:-}")"
    [[ -n "$cluster" ]] || continue
    printf '%s,%s,%s\n' "$cluster" "$namespace" "$plugin"
  done <"$INVENTORY_CSV"
}

# Prints "<namespace>,<plugin>" for a known cluster; fails if not found.
inventory_lookup() {
  local wanted="$1" row rest
  while IFS= read -r row; do
    if [[ "${row%%,*}" == "$wanted" ]]; then
      rest="${row#*,}"
      printf '%s\n' "$rest"
      return 0
    fi
  done < <(read_inventory)
  return 1
}

# Rewrites only the plugin column for an existing cluster row; namespace is preserved verbatim.
update_inventory_plugin() {
  local cluster="$1" plugin="$2" tmp
  tmp="$(mktemp "${INVENTORY_CSV}.XXXXXX")"
  awk -F, -v cluster="$cluster" -v plugin="$plugin" '
    NR == 1 { print; next }
    {
      name = $1
      gsub(/^[ \t]+|[ \t]+$/, "", name)
      if (name == cluster) { print $1 "," $2 ", " plugin } else { print }
    }
  ' "$INVENTORY_CSV" >"$tmp"
  mv "$tmp" "$INVENTORY_CSV"
  log "Updated ${INVENTORY_CSV##*/}: ${cluster} now uses ${plugin}"
}

append_inventory_entry() {
  local cluster="$1" namespace="$2" plugin="$3"
  if [[ -s "$INVENTORY_CSV" && -n "$(tail -c 1 "$INVENTORY_CSV")" ]]; then
    printf '\n' >>"$INVENTORY_CSV"
  fi
  printf '%s, %s, %s\n' "$cluster" "$namespace" "$plugin" >>"$INVENTORY_CSV"
  log "Added ${INVENTORY_CSV##*/} entry: ${cluster}, ${namespace}, ${plugin}"
}

record_inventory_plugin() {
  local cluster="$1" namespace="$2" plugin="$3" known="$4"
  if ((known)); then
    update_inventory_plugin "$cluster" "$plugin"
  else
    append_inventory_entry "$cluster" "$namespace" "$plugin"
  fi
}

run_login() {
  local plugin="$1" cluster="$2" server="$3" namespace="${4:-}"
  local -a args=(
    login
    --server "$server"
    -u "${KUBECTL_VSPHERE_USERNAME}"
    --tanzu-kubernetes-cluster-name "$cluster"
  )
  [[ -z "$namespace" ]] || args+=(--tanzu-kubernetes-cluster-namespace "$namespace")
  args+=(--insecure-skip-tls-verify)
  "$plugin" "${args[@]}"
}

login_cluster() {
  local cluster="$1" plugin server slug known=1 namespace="" entry
  slug="${cluster%%-*}"
  server="${slug}-${DOMAIN_SUFFIX}"

  if entry="$(inventory_lookup "$cluster")" && [[ -n "$entry" ]]; then
    namespace="${entry%%,*}"
    plugin="${entry#*,}"
  fi

  if [[ -z "${plugin:-}" ]]; then
    known=0
    namespace="${slug}-prod-ns"
    plugin="$LEGACY_PLUGIN"
    log "NOTE ${cluster}: no entry in ${INVENTORY_CSV##*/}; imputing namespace ${namespace} and starting with ${LEGACY_PLUGIN}"
  fi

  log "==> ${cluster} (${server}) using ${plugin}${namespace:+, namespace ${namespace}}"
  if run_login "$plugin" "$cluster" "$server" "$namespace"; then
    ((known)) || append_inventory_entry "$cluster" "$namespace" "$plugin"
    return 0
  fi

  if [[ "$plugin" != "$LEGACY_PLUGIN" ]]; then
    log "FAILED ${cluster}"
    return 1
  fi

  if ((! V9_AVAILABLE)); then
    log "FAILED ${cluster} (${V9_PLUGIN} unavailable for retry)"
    return 1
  fi

  log "Retrying ${cluster} with ${V9_PLUGIN}"
  if run_login "$V9_PLUGIN" "$cluster" "$server" "$namespace"; then
    record_inventory_plugin "$cluster" "$namespace" "$V9_PLUGIN" "$known"
    return 0
  fi

  log "FAILED ${cluster} with both ${LEGACY_PLUGIN} and ${V9_PLUGIN}"
  return 1
}

main() {
  local all=0
  local -a clusters=()

  while (($# > 0)); do
    case "$1" in
    --cluster)
      [[ $# -ge 2 && -n "${2:-}" ]] || die "--cluster requires a value"
      clusters+=("$2")
      shift 2
      ;;
    --cluster=*)
      [[ -n "${1#*=}" ]] || die "--cluster requires a value"
      clusters+=("${1#*=}")
      shift
      ;;
    --all)
      all=1
      shift
      ;;
    -h | --help)
      usage
      exit 0
      ;;
    *)
      usage >&2
      die "unknown argument: $1"
      ;;
    esac
  done

  if ((all)) && ((${#clusters[@]} > 0)); then
    die "--all and --cluster are mutually exclusive"
  fi
  if ((all == 0)) && ((${#clusters[@]} == 0)); then
    usage >&2
    die "specify --all or at least one --cluster"
  fi

  check_environment
  check_plugins

  [[ -r "$INVENTORY_CSV" ]] || die "inventory CSV not readable: ${INVENTORY_CSV}"

  if ((all)); then
    local row
    while IFS= read -r row; do
      clusters+=("${row%%,*}")
    done < <(read_inventory)
    ((${#clusters[@]} > 0)) || die "no clusters found in ${INVENTORY_CSV}"
  fi

  local cluster failures=0
  for cluster in "${clusters[@]}"; do
    login_cluster "$cluster" || failures=$((failures + 1))
  done

  if ((failures > 0)); then
    die "${failures} of ${#clusters[@]} cluster login(s) failed"
  fi
  log "All ${#clusters[@]} cluster login(s) succeeded"
}

main "$@"
