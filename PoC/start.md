I need update a file using my CD/CI, but when this is working, dont override the changes, I want like the next:
This is the original file
--- 
apiVersion: crd.antrea.io/v1beta1
kind: ClusterNetworkPolicy
metadata:
  name: egress-livelink-mecc
  labels:
    ticket: "INC-MECC-003"
    cluster: "abrc1l-atlas-stg"
spec:
  egress:
    - action: Allow
    appliedTo:
      - namespaceSelector:
        matchExpressions:
          - key: "kubernetes.io/metadata.name"
          operator: "In"
          values: ["livelink-ignition"]
    enableLogging: true
    name: destination_fqdn
    to:
      - fqdn: abrc1-avis3gw-test.corp.medtronic.com
    ports:
      - protocol: TCP
      port: 2049
      - protocol: TCP
      port: 20048
      - protocol: TCP
      port: 32803
  priority: 80
  tier: atlas-tenants

New changes
{
  "ticket_number": "INC-MECC-005",
  "requester": "network-team@medtronic.com",
  "cluster": "abrc1l-atlas-stg",
  "rules": [
    {
      "source_namespace": "livelink-ignition",
      "destination_ip": "10.76.217.22",
      "destination_fqdn": "stg.mecc.livelink.medtronic.com",
      "ports": "443",
      "purpose": "destination_fqdn"
    }
  ]
}

The currently result is:
apiVersion: crd.antrea.io/v1beta1
kind: ClusterNetworkPolicy
metadata:
  name: egress-livelink-mecc
  labels:
    ticket: "INC-MECC-005"
    cluster: "abrc1l-atlas-stg"
spec:
  egress:
  - action: Allow
    appliedTo:
    - namespaceSelector:
        matchExpressions:
        - key: "kubernetes.io/metadata.name"
          operator: "In"
          values: ["livelink-ignition"]
    enableLogging: true
    name: destination_fqdn
    to:
    - fqdn: stg.mecc.livelink.medtronic.com
    ports:
    - protocol: TCP
      port: 443
  priority: 80
  tier: atlas-tenants 

But I want the next result:
apiVersion: crd.antrea.io/v1beta1
kind: ClusterNetworkPolicy
metadata:
  name: egress-livelink-mecc
  labels:
    ticket: "INC-MECC-005"
    cluster: "abrc1l-atlas-stg"
spec:
  egress:
  - action: Allow
    appliedTo:
    - namespaceSelector:
        matchExpressions:
          - key: "kubernetes.io/metadata.name"
            operator: "In"
            values: ["livelink-ignition"]
    enableLogging: true
    name: Egress rule for MECC V2 
    to:
      - fqdn: stg.mecc.livelink.medtronic.com
    ports:
      - protocol: TCP
        port: 443
  - action: Allow
    appliedTo:
    - namespaceSelector:
        matchExpressions:
          - key: "kubernetes.io/metadata.name"
            operator: "In"
            values: ["livelink-ignition"]
    enableLogging: true
    name: STG MECC V2 NFS write access
    to:
      - fqdn: abrc1-avis3gw-test.corp.medtronic.com
    ports:
      - protocol: TCP
        port: 2049
      - protocol: TCP
        port: 20048
      - protocol: TCP
        port: 32803
  priority: 80
  tier: atlas-tenants
 
The next are the files:
gitlab/ci
variables:
  SOPS_VERSION: "3.11.0"
  OUTPUT_PATH: "policies/cluster/customer-components/cluster-scoped/"
  FILE_NAME: "antrea_clusternetworkpolicy_egress-livelink.yaml"
  TARGET_BASE_BRANCH: "feature/egress-rule-INC-MECC-003"

stages:
  - parse
  - generate
  - propose


before_script:
  - echo "Installing Python dependencies..."
  #- python -m pip install --upgrade pip setuptools wheel
  - python3 --version
  - |
    if command -v apk >/dev/null; then
      apk add --no-cache git wget
    else
      apt-get update && apt-get install -y git wget
    fi
  - wget https://github.com/getsops/sops/releases/download/v${SOPS_VERSION}/sops-v${SOPS_VERSION}.linux.amd64 -O /usr/local/bin/sops
  - chmod +x /usr/local/bin/sops
  - python3 -m venv venv
  - source venv/bin/activate
  - pip install --no-cache-dir --extra-index-url $PIP_INDEX_URL --trusted-host $PIP_TRUSTED_HOST -r requirements.txt
  - git --version


# ========== STAGE 1: PARSE INPUT ==========
parse_job:
  #image: python:3.12-slim
  stage: parse
  script:
    - echo "Parsing ticket data..."
    - echo $MOCK_TICKET_DATA

generate_job:
  stage: generate
  script:
    - echo "Generating Antrea ClusterNetworkPolicy..."
    - python3 scripts/parse_input.py
    - echo "Generated file:"
    - cat $OUTPUT_PATH$FILE_NAME
  artifacts:
    paths:
      - $OUTPUT_PATH


propose_job:
  stage: propose
  script:
    - git config --global user.name "Cristian Fandino"
    - git config --global user.email "cristian.a.fandinomesa@medtronic.com"
    - git config --global --add safe.directory "$CI_PROJECT_DIR"
    - |
      TICKET_ID=$(echo $MOCK_TICKET_DATA | python3 -c "import sys,json; print(json.load(sys.stdin).get('ticket_number','UNKNOWN'))")
      BRANCH_NAME="feature/egress-rule-$TICKET_ID"
      TARGET_PROJECT_PATH_ENC="io-shared-services%2Fatlas%2Fkubernetes-management%2Fabrc1l-atlas-dev"

    - TARGET_REPO_URL="https://${GIT_USERNAME}:${GIT_WRITE_TOKEN}@code.medtronic.com/io-shared-services/atlas/kubernetes-management/abrc1l-atlas-dev.git"

    - mkdir -p /tmp/target && cd /tmp/target
    - git clone "$TARGET_REPO_URL" repo
    - cd repo

    # --- Ensure we branch off the desired base branch ---
    - git fetch origin "$TARGET_BASE_BRANCH"
    - git checkout -B "$TARGET_BASE_BRANCH" "origin/$TARGET_BASE_BRANCH"

    # --- Create the feature branch from that base ---
    - git switch -c "$BRANCH_NAME"

    # --- Copy generated file and commit ---
    - mkdir -p "$OUTPUT_PATH"
    - cp -v "$CI_PROJECT_DIR/$OUTPUT_PATH$FILE_NAME" "$OUTPUT_PATH/"

    - echo "TICKET_ID:${TICKET_ID:-<unset>}"
    - echo "BRANCH_NAME:${BRANCH_NAME:-<unset>}"
    - echo "Base_branch:$TARGET_BASE_BRANCH"
    - git status
    - test -f "$CI_PROJECT_DIR/$OUTPUT_PATH$FILE_NAME" && echo "File exists" || echo "File missing"
    - git add "$OUTPUT_PATH"
    - git commit -m "Auto-generate egress rule for $TICKET_ID"
    - git push -u origin "$BRANCH_NAME"

    # --- Create the MR into the chosen base branch ---
    - |
      curl --request POST \
        --header "PRIVATE-TOKEN: $GIT_WRITE_TOKEN" \
        --header "Content-Type: application/json" \
        "$CI_API_V4_URL/projects/$TARGET_PROJECT_PATH_ENC/merge_requests" \
        --data "{\"source_branch\":\"$BRANCH_NAME\",\"target_branch\":\"$TARGET_BASE_BRANCH\",\"title\":\"[PoC] Egress Rule: $TICKET_ID\",\"description\":\"Automated policy generation for $TICKET_ID\"}"
    - echo "Merge Request created!

Python
#!/usr/bin/env python3
import json
import os
from pathlib import Path

from ruamel.yaml import YAML
from ruamel.yaml.comments import CommentedMap, CommentedSeq
from ruamel.yaml.scalarstring import DoubleQuotedScalarString as DQS
from ruamel.yaml.scalarstring import PlainScalarString as PSS

yaml = YAML()
yaml.preserve_quotes = True
yaml.default_flow_style = False
yaml.indent(mapping=2, sequence=2, offset=0)


# -----------------------------------------------------------------------------
#  Normalize YAML indentation BEFORE ruamel loads it
# -----------------------------------------------------------------------------
def normalize_yaml_indentation(path):
    if not Path(path).exists():
        return

    lines = Path(path).read_text().splitlines()
    fixed = []
    for line in lines:
        if line.strip().startswith("- action:"):
            fixed.append("  " + line.strip())
            continue

        if "appliedTo:" in line:
            fixed.append("    appliedTo:")
            continue

        if "namespaceSelector:" in line:
            fixed.append("    - namespaceSelector:")
            continue

        if "matchExpressions:" in line:
            fixed.append("        matchExpressions:")
            continue

        if "key:" in line or "operator:" in line or "values:" in line:
            fixed.append("          " + line.strip())
            continue

        if "ports:" in line:
            fixed.append("    ports:")
            continue

        if "- protocol:" in line:
            fixed.append("      - protocol: TCP")
            continue

        if "port:" in line and not line.strip().startswith("port:"):
            # enforce correct indentation of ports
            indent = "        "
            fixed.append(f"{indent}{line.strip()}")
            continue

        fixed.append(line)

    Path(path).write_text("\n".join(fixed))


# -----------------------------------------------------------------------------
# Load ticket JSON
# -----------------------------------------------------------------------------
def load_ticket_data():
    raw = os.environ.get("MOCK_TICKET_DATA")
    if not raw:
        raise ValueError("MOCK_TICKET_DATA is not set")
    return json.loads(raw)


# -----------------------------------------------------------------------------
# Parse ports
# -----------------------------------------------------------------------------
def parse_ports(port_string):
    if not port_string:
        return []
    cleaned = port_string.replace(" ", ",").replace(";", ",")
    result = set()
    for t in cleaned.split(","):
        t = t.strip()
        if not t:
            continue
        try:
            n = int(t)
            if 1 <= n <= 65535:
                result.add(n)
        except ValueError:
            continue
    return sorted(result)


# -----------------------------------------------------------------------------
# Extract values
# -----------------------------------------------------------------------------
def extract_unique_values(rules):
    namespaces, fqdns, ports = set(), set(), set()
    purpose = "NFS access"

    for r in rules:
        ns = (r.get("source_namespace") or "").strip()
        fqdn = (r.get("destination_fqdn") or "").strip()
        purp = (r.get("purpose") or "").strip()

        if ns:
            namespaces.add(ns)
        if fqdn:
            fqdns.add(fqdn)
        if purp:
            purpose = purp

        for p in parse_ports(r.get("ports", "")):
            ports.add(p)

    return sorted(namespaces), sorted(fqdns), sorted(ports), purpose


# -----------------------------------------------------------------------------
# Build egress rule
# -----------------------------------------------------------------------------
def build_egress_rule(namespaces, fqdns, ports, purpose):

    values = CommentedSeq([DQS(ns) for ns in namespaces])
    values.fa.set_flow_style()

    rule = CommentedMap()
    rule["action"] = "Allow"

    applied_seq = CommentedSeq()
    ns_sel = CommentedMap()
    ns_sel["namespaceSelector"] = CommentedMap()

    match = CommentedMap()
    match["key"] = DQS("kubernetes.io/metadata.name")
    match["operator"] = DQS("In")
    match["values"] = values

    ns_sel["namespaceSelector"]["matchExpressions"] = CommentedSeq([match])
    applied_seq.append(ns_sel)

    rule["appliedTo"] = applied_seq
    rule["enableLogging"] = True

    rule["name"] = PSS(purpose)

    to_seq = CommentedSeq()
    for fq in fqdns:
        cm = CommentedMap()
        cm["fqdn"] = fq
        to_seq.append(cm)
    rule["to"] = to_seq

    port_seq = CommentedSeq()
    for p in ports:
        cm = CommentedMap()
        cm["protocol"] = "TCP"
        cm["port"] = p
        port_seq.append(cm)
    rule["ports"] = port_seq

    return rule


# -----------------------------------------------------------------------------
# Load existing YAML
# -----------------------------------------------------------------------------
def load_existing_yaml(path):
    p = Path(path)
    if not p.exists():
        return None

    normalize_yaml_indentation(path)

    try:
        with p.open("r") as f:
            doc = yaml.load(f)
    except Exception:
        return None

    if "spec" not in doc:
        doc["spec"] = CommentedMap()
    if "egress" not in doc["spec"]:
        doc["spec"]["egress"] = CommentedSeq()

    return doc


# -----------------------------------------------------------------------------
# Duplicate rule validation
# -----------------------------------------------------------------------------
def rules_equal(a, b):
    return (
        a["name"] == b["name"]
        and a["to"] == b["to"]
        and a["ports"] == b["ports"]
        and a["appliedTo"] == b["appliedTo"]
    )


def rule_exists(doc, rule):
    for r in doc["spec"]["egress"]:
        if rules_equal(r, rule):
            return True
    return False


# -----------------------------------------------------------------------------
# Save YAML
# -----------------------------------------------------------------------------
def save_yaml(doc, path):
    with open(path, "w") as f:
        yaml.dump(doc, f)


# -----------------------------------------------------------------------------
# MAIN
# -----------------------------------------------------------------------------
def main():
    data = load_ticket_data()
    ticket = data["ticket_number"]
    cluster = data["cluster"]
    rules = data["rules"]

    namespaces, fqdns, ports, purpose = extract_unique_values(rules)
    new_rule = build_egress_rule(namespaces, fqdns, ports, purpose)

    out_dir = os.environ.get("OUTPUT_PATH", ".")
    file_name = os.environ.get("FILE_NAME", "policy.yaml")
    full_path = f"{out_dir}/{file_name}"

    os.makedirs(out_dir, exist_ok=True)

    doc = load_existing_yaml(full_path)

    if doc is None:
        # Build new file
        doc = CommentedMap()
        doc["apiVersion"] = "crd.antrea.io/v1beta1"
        doc["kind"] = "ClusterNetworkPolicy"

        meta = CommentedMap()
        meta["name"] = "egress-livelink-mecc"
        labels = CommentedMap()
        labels["ticket"] = DQS(ticket)
        labels["cluster"] = DQS(cluster)
        meta["labels"] = labels
        doc["metadata"] = meta

        spec = CommentedMap()
        spec["egress"] = CommentedSeq([new_rule])
        spec["priority"] = 80
        spec["tier"] = "atlas-tenants"
        doc["spec"] = spec

    else:
        # Append correctly
        if not rule_exists(doc, new_rule):
            doc["spec"]["egress"].append(new_rule)

    save_yaml(doc, full_path)
    print(f"Saved → {full_path}")

if __name__ == "__main__":
    main()
