My PoC working for add a new egress file, but I have a new use-case. That is with an existing file. So, I've the original and the new file with this changes, and its ticket, and I need add this new option. My PoC is already working with the creation of new yaml file (according with the rules in a JSON), and do its process correctly.

Original files in my PoC that are working.
.gitlab-ci.yaml
variables:
  SOPS_VERSION: "3.11.0"
  OUTPUT_PATH: "policies/cluster/customer-components/cluster-scoped/"
  FILE_NAME: "antrea_clusternetworkpolicy_egress-livelink.yaml"

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
      TARGET_BASE_BRANCH="test-automation"
      TARGET_PROJECT_PATH_ENC="io-shared-services%2Fatlas%2Fkubernetes-management%2Fabrc1l-atlas-dev"

    - TARGET_REPO_URL="https://${GIT_USERNAME}:${GIT_WRITE_TOKEN}@code.medtronic.com/io-shared-services/atlas/kubernetes-management/abrc1l-atlas-dev.git"

    
    - mkdir -p /tmp/target && cd /tmp/target
    - git clone "$TARGET_REPO_URL" repo
    - cd repo

    
    - mkdir -p "$OUTPUT_PATH"
    - cp -v "$CI_PROJECT_DIR/$OUTPUT_PATH$FILE_NAME" "$OUTPUT_PATH/"



    - echo "TICKET_ID:${TICKET_ID:-<unset>}"
    - echo "BRANCH_NAME:${BRANCH_NAME:-<unset>}"
    - git rev-parse --show-toplevel || echo "Not inside a git repo!"
    - echo "Current_branch:$(git rev-parse --abbrev-ref HEAD || echo 'unknown')"
    - git status
    - ls -lah "$CI_PROJECT_DIR/$OUTPUT_PATH" || true
    - test -f "$CI_PROJECT_DIR/$OUTPUT_PATH$FILE_NAME" && echo "File exists" || echo "File missing"
    - git log --oneline -5 || true


    - git add "$OUTPUT_PATH"
    - git commit -m "Auto-generate egress rule for $TICKET_ID"

    - git switch -c "feature/egress-rule-$TICKET_ID"
    - git push -u origin "feature/egress-rule-$TICKET_ID"

    - |
      curl --request POST \
        --header "PRIVATE-TOKEN: $GIT_WRITE_TOKEN" \
        --header "Content-Type: application/json" \
        "$CI_API_V4_URL/projects/$TARGET_PROJECT_PATH_ENC/merge_requests" \
        --data "{\"source_branch\":\"$BRANCH_NAME\",\"target_branch\":\"main\",\"title\":\"[PoC] Egress Rule: $TICKET_ID\",\"description\":\"Automated policy generation for $TICKET_ID\"}"
    - echo "Merge Request created!"
  only:
    - main

parse_input.py
#!/usr/bin/env python3

import json
import os
import sys
from typing import List, Tuple, Set

from ruamel.yaml import YAML
from ruamel.yaml.scalarstring import DoubleQuotedScalarString as DQS
from ruamel.yaml.scalarstring import PlainScalarString as PSS
from ruamel.yaml.comments import CommentedSeq


def load_ticket_data() -> dict:
    """
    Read MOCK_TICKET_DATA from env and parse as JSON.
    Expected structure:
    {
        "ticket_number": "INC-123",
        "cluster": "prod",
        "rules": [
        {
            "source_namespace": "ns-a",
            "destination_fqdn": "example.com",
            "ports": "80, 443",
            "purpose": "Web access"
        },
        ...
        ]
    }
    """
    raw_data = os.environ.get("MOCK_TICKET_DATA")
    if not raw_data:
        raise ValueError("Environment variable MOCK_TICKET_DATA is not set.")
    try:
        return json.loads(raw_data)
    except json.JSONDecodeError as e:
        raise ValueError(f"MOCK_TICKET_DATA is not valid JSON: {e}") from e


def _parse_ports_field(port_field: str) -> List[int]:
    """
    Accepts a string like '80,443  2049' and returns [80, 443, 2049].
    Ignores empty tokens and non-integer values gracefully.
    """
    if not port_field:
        return []
    cleaned = port_field.replace(";", ",").replace(" ", ",")
    ports: Set[int] = set()
    for token in cleaned.split(","):
        t = token.strip()
        if not t:
            continue
        try:
            n = int(t)
            if 1 <= n <= 65535:
                ports.add(n)
        except ValueError:
            # Silently skip invalid entries like 'tcp' or ranges '80-90'
            # (extend here if you need to support ranges)
            continue
    return sorted(ports)


def extract_unique_values(rules: List[dict]) -> Tuple[List[str], List[str], List[int], str]:
    """
    Extract distinct namespaces, FQDNs, ports, and a 'purpose' from rules.
    - Namespaces: unique, sorted
    - FQDNs: unique, sorted, lowercased & stripped
    - Ports: unique, sorted (int)
    - Purpose: the last non-empty rule purpose or fallback "NFS access"
    """
    namespaces: Set[str] = set()
    fqdns: Set[str] = set()
    ports: Set[int] = set()
    purpose = "NFS access"

    for rule in rules or []:
        ns = (rule.get("source_namespace") or "").strip()
        if ns:
            namespaces.add(ns)

        fqdn = (rule.get("destination_fqdn") or "").strip().lower()
        if fqdn:
            fqdns.add(fqdn)

        rule_purpose = (rule.get("purpose") or "").strip()
        if rule_purpose:
            purpose = rule_purpose

        # Parse ports field from the rule
        for p in _parse_ports_field(rule.get("ports", "")):
            ports.add(p)

    return sorted(namespaces), sorted(fqdns), sorted(ports), purpose


def build_policy_doc(
    ticket_number: str,
    cluster: str,
    namespaces: List[str],
    fqdns: List[str],
    ports: List[int],
    purpose: str,
    resource_name: str = "egress-livelink-mecc",
) -> dict:
    """
    Construct the Antrea ClusterNetworkPolicy document as a Python mapping.
    Uses DQS to force quoting where helpful (labels/purpose).
    """
    # Build the 'to' list with FQDN objects
    to_list = [{"fqdn": fqdn} for fqdn in fqdns]

    # Build the 'ports' list with TCP entries
    ports_list = [{"protocol": "TCP", "port": int(p)} for p in ports]

    # Make 'values' a flow-style list with each item double-quoted
    values_seq = CommentedSeq([DQS(ns) for ns in namespaces])
    values_seq.fa.set_flow_style()

    # Namespace match expression (values is a list of strings)
    match_expression = {
        "key": DQS("kubernetes.io/metadata.name"),
        "operator": DQS("In"),
        "values": values_seq,  # ruamel will emit as a flow or block sequence as needed
    }

    # Full document
    doc = {
        "apiVersion": "crd.antrea.io/v1beta1",
        "kind": "ClusterNetworkPolicy",
        "metadata": {
            "name": resource_name,
            "labels": {
                # Use DQS to ensure quoted strings (prevents accidental type coercion)
                "ticket": DQS(str(ticket_number)),
                "cluster": DQS(str(cluster)),
            },
        },
        "spec": {
            "egress": [
                {
                    "action": "Allow",
                    "appliedTo": [
                        {
                            "namespaceSelector": {
                                "matchExpressions": [match_expression]
                            }
                        }
                    ],
                    "enableLogging": True,
                    "name": PSS(purpose),
                    "to": to_list,
                    "ports": ports_list,
                }
            ],
            "priority": 80,
            "tier": "atlas-tenants",
        },
    }

    return doc


def dump_yaml(doc: dict) -> str:
    """
    Dump the YAML document using ruamel.yaml with consistent style.
    Returns the YAML string.
    """
    yaml = YAML()
    yaml.version = (1, 2)  # YAML 1.2 behavior
    yaml.preserve_quotes = True
    yaml.explicit_start = True   # add '---'
    yaml.default_flow_style = False
    yaml.indent(mapping=2, sequence=2, offset=2)
    yaml.width = 120
    from io import StringIO
    stream = StringIO()
    yaml.dump(doc, stream)
    return stream.getvalue()


def write_file(path: str, content: str) -> None:
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)


def main() -> None:
    data = load_ticket_data()

    ticket_number = data.get("ticket_number", "UNKNOWN")
    cluster = data.get("cluster", "unknown")
    rules = data.get("rules", [])

    if not rules:
        print("⚠️  No rules provided in MOCK_TICKET_DATA; generated policy may be empty.", file=sys.stderr)

    namespaces, fqdns, ports, purpose = extract_unique_values(rules)

    print(f" Ticket: {ticket_number}")
    print(f" Cluster: {cluster}")
    print(f"Namespaces: {namespaces}")
    print(f"Unique FQDNs: {fqdns}")
    print(f"Unique ports: {ports}")
    print(f"Purpose: {purpose}")

    resource_name = os.environ.get("RESOURCE_NAME", "egress-livelink-mecc")
    doc = build_policy_doc(ticket_number, cluster, namespaces, fqdns, ports, purpose, resource_name=resource_name)

    yaml_content = dump_yaml(doc)

    output_dir = os.environ.get("OUTPUT_PATH", "policies/cluster/customer-components/cluster-scoped/")
    file_name = os.environ.get("FILE_NAME", "antrea_clusternetworkpolicy_egress-livelink.yaml")
    full_path = os.path.join(output_dir, file_name)

    write_file(full_path, yaml_content)
    print(f"✅ File generated: {full_path}")


if __name__ == "__main__":
    main()

MOCK_TICKET_DATA
{
  "ticket_number": "INC-MECC-002",
  "requester": "network-team@medtronic.com",
  "cluster": "abrc1-atlas-preview-prd",
  "rules": [
    {
      "source_namespace": "ami-vision-polaris",
      "destination_ip": "10.76.217.11",
      "destination_fqdn": "abrc1-qdc36gs-test.corp.medtronic.com",
      "ports": "666",
      "purpose": "enable NFS write access to ami vision"
    }
  ]
}

The result of the pipeline execution is:
File created: 
%YAML 1.2
---
apiVersion: crd.antrea.io/v1beta1
kind: ClusterNetworkPolicy
metadata:
  name: egress-livelink-mecc
  labels:
    ticket: "INC-MECC-002"
    cluster: "abrc1-atlas-preview-prd"
spec:
  egress:
    - action: Allow
    appliedTo:
      - namespaceSelector:
        matchExpressions:
          - key: "kubernetes.io/metadata.name"
          operator: "In"
          values: ["ami-vision-polaris"]
    enableLogging: true
    name: enable NFS write access to ami vision
    to:
      - fqdn: abrc1-qdc36gs-test.corp.medtronic.com
    ports:
      - protocol: TCP
      port: 666
  priority: 80
  tier: atlas-tenants


Now, lets see a new update, this is about the next JSON
{
  "ticket_number": "INC-MECC-002",
  "requester": "network-team@medtronic.com",
  "cluster": "abrc1l-atlas-stg",
  "rules": [
    {
      "source_namespace": "livelink-ignition",
      "destination_ip": "10.76.217.22",
      "destination_fqdn": "abrc1-avis3gw-test.corp.medtronic.com",
      "ports": "666",
      "purpose": "destination_fqdn"
    }
  ]
}

The original file (before the change with the data):
apiVersion: crd.antrea.io/v1beta1
kind: ClusterNetworkPolicy
metadata:
  name: egress-livelink-mecc
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
  priority: 80
  tier: atlas-tenants

And the result file (after):
apiVersion: crd.antrea.io/v1beta1
kind: ClusterNetworkPolicy
metadata:
  name: egress-livelink-mecc
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

