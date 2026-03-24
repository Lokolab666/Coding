Your pipeline is overwriting because it generates the YAML in an empty workspace, not from the file that already exists in the target repo.

That means this sequence is happening:

1. generate_job runs parse_input.py.


2. parse_input.py looks for policies/.../antrea_clusternetworkpolicy_egress-livelink.yaml inside the CI workspace.


3. That file is not there, so load_existing_yaml() returns None.


4. The script creates a brand-new document with only the new rule.


5. propose_job later clones the real repo and copies that brand-new file on top of the existing one.



So the old rule is not “removed by GitLab.” You never loaded it in the first place.

There are also three flaws in the current design:

First, normalize_yaml_indentation() is brittle and dangerous. It rewrites lines by string matching and even hardcodes TCP when it sees - protocol:. That is not a parser. It is a file corrupter waiting to happen.

Second, your current JSON cannot produce name: Egress rule for MECC V2. You do not send that value anywhere. With the JSON you showed, the best your code can do is use purpose: destination_fqdn. So your expected output and your input contract do not match.

Third, metadata.labels.ticket is a single value for the whole policy file. Every new ticket will overwrite the previous one. If you need ticket history, that label design is wrong.

What you should change

Do not generate the file before cloning the target repo. Generate against the real file from the branch you want to extend.

A safer flow is:

generate_job: clone target repo, checkout TARGET_BASE_BRANCH, load the existing YAML from that repo, append the new rule, save it, store the modified file as artifact.

propose_job: clone again, checkout same base, create feature branch, copy the already-modified file artifact, commit, push, create MR.


Also, add a field like name or rule_name in the JSON. Without that, your desired output is impossible.

Use JSON like this instead:

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
      "name": "Egress rule for MECC V2"
    }
  ]
}

Updated parse_input.py

This version does four things correctly:

loads the existing file if it exists

appends instead of replacing

builds one rule per JSON rule

removes the indentation hack


#!/usr/bin/env python3
import json
import os
from pathlib import Path
from copy import deepcopy

from ruamel.yaml import YAML
from ruamel.yaml.comments import CommentedMap, CommentedSeq
from ruamel.yaml.scalarstring import DoubleQuotedScalarString as DQS
from ruamel.yaml.scalarstring import PlainScalarString as PSS

yaml = YAML()
yaml.preserve_quotes = True
yaml.default_flow_style = False
yaml.indent(mapping=2, sequence=4, offset=2)


def load_ticket_data():
    raw = os.environ.get("MOCK_TICKET_DATA")
    if not raw:
        raise ValueError("MOCK_TICKET_DATA is not set")
    return json.loads(raw)


def parse_ports(port_string):
    if not port_string:
        return []

    cleaned = str(port_string).replace(" ", ",").replace(";", ",")
    result = set()

    for token in cleaned.split(","):
        token = token.strip()
        if not token:
            continue
        try:
            port = int(token)
            if 1 <= port <= 65535:
                result.add(port)
        except ValueError:
            pass

    return sorted(result)


def build_namespace_selector(namespace):
    values = CommentedSeq([DQS(namespace)])
    values.fa.set_flow_style()

    match_expression = CommentedMap()
    match_expression["key"] = DQS("kubernetes.io/metadata.name")
    match_expression["operator"] = DQS("In")
    match_expression["values"] = values

    namespace_selector = CommentedMap()
    namespace_selector["matchExpressions"] = CommentedSeq([match_expression])

    applied_to_item = CommentedMap()
    applied_to_item["namespaceSelector"] = namespace_selector

    applied_to = CommentedSeq([applied_to_item])
    return applied_to


def build_destinations(rule_data):
    to_seq = CommentedSeq()

    fqdn = (rule_data.get("destination_fqdn") or "").strip()
    ip = (rule_data.get("destination_ip") or "").strip()

    if fqdn:
        item = CommentedMap()
        item["fqdn"] = fqdn
        to_seq.append(item)
    elif ip:
        item = CommentedMap()
        item["ipBlock"] = CommentedMap()
        item["ipBlock"]["cidr"] = f"{ip}/32"
        to_seq.append(item)
    else:
        raise ValueError("Each rule must include destination_fqdn or destination_ip")

    return to_seq


def build_ports(rule_data):
    ports = parse_ports(rule_data.get("ports", ""))
    if not ports:
        raise ValueError("Each rule must include at least one valid port")

    port_seq = CommentedSeq()
    for port in ports:
        item = CommentedMap()
        item["protocol"] = "TCP"
        item["port"] = port
        port_seq.append(item)

    return port_seq


def build_egress_rule(rule_data):
    source_namespace = (rule_data.get("source_namespace") or "").strip()
    if not source_namespace:
        raise ValueError("Each rule must include source_namespace")

    rule_name = (
        (rule_data.get("name") or "").strip()
        or (rule_data.get("rule_name") or "").strip()
        or (rule_data.get("purpose") or "").strip()
        or "egress-rule"
    )

    rule = CommentedMap()
    rule["action"] = "Allow"
    rule["appliedTo"] = build_namespace_selector(source_namespace)
    rule["enableLogging"] = True
    rule["name"] = PSS(rule_name)
    rule["to"] = build_destinations(rule_data)
    rule["ports"] = build_ports(rule_data)

    return rule


def ensure_base_document(doc, ticket, cluster):
    if doc is None:
        doc = CommentedMap()
        doc["apiVersion"] = "crd.antrea.io/v1beta1"
        doc["kind"] = "ClusterNetworkPolicy"

        metadata = CommentedMap()
        metadata["name"] = "egress-livelink-mecc"

        labels = CommentedMap()
        labels["ticket"] = DQS(ticket)
        labels["cluster"] = DQS(cluster)

        metadata["labels"] = labels
        doc["metadata"] = metadata

        spec = CommentedMap()
        spec["egress"] = CommentedSeq()
        spec["priority"] = 80
        spec["tier"] = "atlas-tenants"
        doc["spec"] = spec
        return doc

    if "metadata" not in doc:
        doc["metadata"] = CommentedMap()

    if "labels" not in doc["metadata"]:
        doc["metadata"]["labels"] = CommentedMap()

    doc["metadata"]["labels"]["ticket"] = DQS(ticket)
    doc["metadata"]["labels"]["cluster"] = DQS(cluster)

    if "spec" not in doc:
        doc["spec"] = CommentedMap()

    if "egress" not in doc["spec"]:
        doc["spec"]["egress"] = CommentedSeq()

    if "priority" not in doc["spec"]:
        doc["spec"]["priority"] = 80

    if "tier" not in doc["spec"]:
        doc["spec"]["tier"] = "atlas-tenants"

    return doc


def load_existing_yaml(path):
    file_path = Path(path)
    if not file_path.exists():
        return None

    with file_path.open("r") as f:
        return yaml.load(f)


def to_plain(obj):
    if isinstance(obj, dict):
        return {k: to_plain(v) for k, v in obj.items()}
    if isinstance(obj, list):
        return [to_plain(v) for v in obj]
    return obj


def rules_equal(rule_a, rule_b):
    return to_plain(rule_a) == to_plain(rule_b)


def rule_exists(doc, new_rule):
    for existing in doc["spec"]["egress"]:
        if rules_equal(existing, new_rule):
            return True
    return False


def save_yaml(doc, path):
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w") as f:
        yaml.dump(doc, f)


def main():
    data = load_ticket_data()
    ticket = data["ticket_number"]
    cluster = data["cluster"]
    incoming_rules = data.get("rules", [])

    full_path = os.environ.get("POLICY_FULL_PATH")
    if not full_path:
        output_path = os.environ.get("OUTPUT_PATH", ".")
        file_name = os.environ.get("FILE_NAME", "policy.yaml")
        full_path = str(Path(output_path) / file_name)

    doc = load_existing_yaml(full_path)
    doc = ensure_base_document(doc, ticket, cluster)

    for rule_data in incoming_rules:
        new_rule = build_egress_rule(rule_data)
        if not rule_exists(doc, new_rule):
            doc["spec"]["egress"].append(new_rule)

    save_yaml(doc, full_path)
    print(f"Saved -> {full_path}")


if __name__ == "__main__":
    main()

Updated .gitlab-ci.yml

The important change is that generate_job now clones the target repo first and runs the script against the real file.

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

parse_job:
  stage: parse
  script:
    - echo "Parsing ticket data..."
    - echo "$MOCK_TICKET_DATA"

generate_job:
  stage: generate
  script:
    - git config --global user.name "Cristian Fandino"
    - git config --global user.email "cristian.a.fandinomesa@medtronic.com"
    - git config --global --add safe.directory "$CI_PROJECT_DIR"

    - TARGET_REPO_URL="https://${GIT_USERNAME}:${GIT_WRITE_TOKEN}@code.medtronic.com/io-shared-services/atlas/kubernetes-management/abrc1l-atlas-dev.git"
    - rm -rf "$CI_PROJECT_DIR/target-repo"
    - git clone "$TARGET_REPO_URL" "$CI_PROJECT_DIR/target-repo"

    - cd "$CI_PROJECT_DIR/target-repo"
    - git fetch origin "$TARGET_BASE_BRANCH"
    - git checkout -B "$TARGET_BASE_BRANCH" "origin/$TARGET_BASE_BRANCH"

    - export POLICY_FULL_PATH="$CI_PROJECT_DIR/target-repo/$OUTPUT_PATH$FILE_NAME"
    - cd "$CI_PROJECT_DIR"
    - python3 scripts/parse_input.py

    - echo "Generated/updated file:"
    - cat "$CI_PROJECT_DIR/target-repo/$OUTPUT_PATH$FILE_NAME"

    - mkdir -p "$CI_PROJECT_DIR/generated/$OUTPUT_PATH"
    - cp "$CI_PROJECT_DIR/target-repo/$OUTPUT_PATH$FILE_NAME" "$CI_PROJECT_DIR/generated/$OUTPUT_PATH$FILE_NAME"
  artifacts:
    paths:
      - generated/

propose_job:
  stage: propose
  script:
    - git config --global user.name "Cristian Fandino"
    - git config --global user.email "cristian.a.fandinomesa@medtronic.com"
    - git config --global --add safe.directory "$CI_PROJECT_DIR"

    - |
      TICKET_ID=$(echo "$MOCK_TICKET_DATA" | python3 -c "import sys,json; print(json.load(sys.stdin).get('ticket_number','UNKNOWN'))")
      BRANCH_NAME="feature/egress-rule-$TICKET_ID"
      TARGET_PROJECT_PATH_ENC="io-shared-services%2Fatlas%2Fkubernetes-management%2Fabrc1l-atlas-dev"

    - TARGET_REPO_URL="https://${GIT_USERNAME}:${GIT_WRITE_TOKEN}@code.medtronic.com/io-shared-services/atlas/kubernetes-management/abrc1l-atlas-dev.git"

    - rm -rf /tmp/target
    - mkdir -p /tmp/target
    - cd /tmp/target
    - git clone "$TARGET_REPO_URL" repo
    - cd repo

    - git fetch origin "$TARGET_BASE_BRANCH"
    - git checkout -B "$TARGET_BASE_BRANCH" "origin/$TARGET_BASE_BRANCH"
    - git switch -c "$BRANCH_NAME"

    - mkdir -p "$OUTPUT_PATH"
    - cp -v "$CI_PROJECT_DIR/generated/$OUTPUT_PATH$FILE_NAME" "$OUTPUT_PATH/"

    - git status
    - git add "$OUTPUT_PATH$FILE_NAME"
    - git commit -m "Auto-update egress rule for $TICKET_ID"
    - git push -u origin "$BRANCH_NAME"

    - |
      curl --request POST \
        --header "PRIVATE-TOKEN: $GIT_WRITE_TOKEN" \
        --header "Content-Type: application/json" \
        "$CI_API_V4_URL/projects/$TARGET_PROJECT_PATH_ENC/merge_requests" \
        --data "{\"source_branch\":\"$BRANCH_NAME\",\"target_branch\":\"$TARGET_BASE_BRANCH\",\"title\":\"[PoC] Egress Rule: $TICKET_ID\",\"description\":\"Automated policy update for $TICKET_ID\"}"

    - echo "Merge Request created!"

What result this gives you

If the existing file already contains:

STG MECC V2 NFS write access


and the incoming JSON contains a new rule with:

name: Egress rule for MECC V2


then the output will keep the old NFS rule and append the new 443 rule.

That is the behavior you want.

One more thing you should not ignore

You are currently targeting this base branch:

TARGET_BASE_BRANCH: "feature/egress-rule-INC-MECC-003"

That means you are stacking a new feature branch on top of another feature branch. That is fine only if that previous feature branch is the approved source of truth you want to extend.

If INC-MECC-003 is already merged, then this is a bad idea. Base from main or the real integration branch instead. Otherwise your new MR depends on a non-final branch and can become messy fast.

And the ticket label issue remains. With this design, the file label will become:

ticket: "INC-MECC-005"

So you are losing INC-MECC-003 at file metadata level even though its rule remains in spec.egress. If auditability matters, use an annotation like this instead of pretending one label can represent multiple change tickets:

metadata:
  annotations:
    change-tickets: "INC-MECC-003,INC-MECC-005"

Your current expectation about preserving rules is correct. Your current expectation about preserving ticket history is not, because the schema you designed does not support it.

Send the current repository file path and one real example of the target branch state if you want the script adjusted to also preserve comments and ordering exactly as your repo expects.    - git config --global --add safe.directory "$CI_PROJECT_DIR"
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

