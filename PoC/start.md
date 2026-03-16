Your current PoC solves “create a new YAML from JSON.” This new use case is different: it is “open an existing YAML, append a new egress rule, preserve the rest, avoid duplicates, then propose the change.” That is not a small extension. It is a different workflow.

There are also two problems in what you showed.

First, your generated YAML example has broken indentation. In this section:

```yaml
spec:
  egress:
    - action: Allow
    appliedTo:
      - namespaceSelector:
        matchExpressions:
```

`appliedTo`, `enableLogging`, `name`, `to`, and `ports` should be nested under the same list item as `action`. The valid structure is more like:

```yaml
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
```

Second, your new desired output cannot be produced from the JSON you provided. You want this:

```yaml
name: STG MECC V2 NFS write access
```

but your JSON says:

```json
"purpose": "destination_fqdn"
```

That is not a formatting issue. That is a data-model issue. So either:

1. the ticket parser must produce the real rule name, or
2. you need a mapping table in code, or
3. you must accept that the rule name will come from `purpose`.

Right now, your input and expected output contradict each other.

What you should change

Do not keep the old model of “generate a full file and copy it into the repo.” For existing files, that is the wrong approach. You need this logic:

1. Clone target repo.
2. Open the existing YAML file from the repo.
3. Parse it.
4. Build one new `egress` entry from the ticket.
5. Check whether an equivalent rule already exists.
6. Append it only if missing.
7. Write the YAML back.
8. Commit in a feature branch.
9. Create MR.

That is the correct model.

A better Python approach

Instead of `build_policy_doc()` returning a whole document, add a function that builds only one egress rule, and another function that merges it into an existing file.

Use this version as the base. It is closer to what you actually need.

```python
#!/usr/bin/env python3

import json
import os
import sys
from typing import List, Set, Tuple

from ruamel.yaml import YAML
from ruamel.yaml.scalarstring import DoubleQuotedScalarString as DQS
from ruamel.yaml.comments import CommentedSeq, CommentedMap


def load_ticket_data() -> dict:
    raw_data = os.environ.get("MOCK_TICKET_DATA")
    if not raw_data:
        raise ValueError("Environment variable MOCK_TICKET_DATA is not set.")
    try:
        return json.loads(raw_data)
    except json.JSONDecodeError as e:
        raise ValueError(f"MOCK_TICKET_DATA is not valid JSON: {e}") from e


def parse_ports(port_field: str) -> List[int]:
    if not port_field:
        return []
    cleaned = port_field.replace(";", ",").replace(" ", ",")
    ports: Set[int] = set()

    for token in cleaned.split(","):
        token = token.strip()
        if not token:
            continue
        try:
            port = int(token)
            if 1 <= port <= 65535:
                ports.add(port)
        except ValueError:
            continue

    return sorted(ports)


def extract_rule_data(rule: dict) -> Tuple[List[str], List[str], List[int], str]:
    namespaces: Set[str] = set()
    fqdns: Set[str] = set()
    ports: Set[int] = set()

    ns = (rule.get("source_namespace") or "").strip()
    if ns:
        namespaces.add(ns)

    fqdn = (rule.get("destination_fqdn") or "").strip().lower()
    if fqdn:
        fqdns.add(fqdn)

    for p in parse_ports(rule.get("ports", "")):
        ports.add(p)

    purpose = (rule.get("purpose") or "").strip()
    if not purpose:
        purpose = "Generated egress rule"

    return sorted(namespaces), sorted(fqdns), sorted(ports), purpose


def build_match_expression(namespaces: List[str]) -> CommentedMap:
    values_seq = CommentedSeq([DQS(ns) for ns in namespaces])
    values_seq.fa.set_flow_style()

    return CommentedMap({
        "key": DQS("kubernetes.io/metadata.name"),
        "operator": DQS("In"),
        "values": values_seq,
    })


def build_egress_rule(namespaces: List[str], fqdns: List[str], ports: List[int], purpose: str) -> CommentedMap:
    applied_to = CommentedSeq()
    applied_to.append(
        CommentedMap({
            "namespaceSelector": CommentedMap({
                "matchExpressions": CommentedSeq([
                    build_match_expression(namespaces)
                ])
            })
        })
    )

    to_seq = CommentedSeq()
    for fqdn in fqdns:
        to_seq.append(CommentedMap({"fqdn": fqdn}))

    ports_seq = CommentedSeq()
    for port in ports:
        ports_seq.append(CommentedMap({
            "protocol": "TCP",
            "port": port
        }))

    rule = CommentedMap({
        "action": "Allow",
        "appliedTo": applied_to,
        "enableLogging": True,
        "name": purpose,
        "to": to_seq,
        "ports": ports_seq,
    })

    return rule


def rule_signature(rule: dict) -> tuple:
    """
    Normalize a rule so we can compare existing vs new.
    We intentionally ignore formatting and field order.
    """
    namespaces = []
    for applied in rule.get("appliedTo", []):
        ns_selector = applied.get("namespaceSelector", {})
        for expr in ns_selector.get("matchExpressions", []):
            if expr.get("key") == "kubernetes.io/metadata.name" and expr.get("operator") == "In":
                namespaces.extend(expr.get("values", []))

    fqdns = sorted(
        item.get("fqdn", "").strip().lower()
        for item in rule.get("to", [])
        if item.get("fqdn")
    )

    ports = sorted(
        int(item.get("port"))
        for item in rule.get("ports", [])
        if str(item.get("protocol", "")).upper() == "TCP" and item.get("port") is not None
    )

    return (
        tuple(sorted(set(str(x).strip() for x in namespaces if str(x).strip()))),
        tuple(fqdns),
        tuple(ports),
    )


def load_yaml_file(path: str):
    yaml = YAML()
    yaml.preserve_quotes = True
    yaml.indent(mapping=2, sequence=4, offset=2)
    yaml.width = 120

    with open(path, "r", encoding="utf-8") as f:
        data = yaml.load(f)

    return yaml, data


def save_yaml_file(yaml: YAML, path: str, data) -> None:
    with open(path, "w", encoding="utf-8") as f:
        yaml.dump(data, f)


def append_rule_to_existing_policy(file_path: str, new_rule: CommentedMap) -> bool:
    yaml, data = load_yaml_file(file_path)

    if not data:
        raise ValueError(f"YAML file is empty: {file_path}")

    spec = data.setdefault("spec", CommentedMap())
    egress = spec.setdefault("egress", CommentedSeq())

    new_sig = rule_signature(new_rule)

    for existing_rule in egress:
        if rule_signature(existing_rule) == new_sig:
            print("Rule already exists. No changes made.")
            return False

    egress.append(new_rule)
    save_yaml_file(yaml, file_path, data)
    print("Rule appended successfully.")
    return True


def create_new_policy_doc(ticket_number: str, cluster: str, new_rule: CommentedMap, resource_name: str):
    doc = CommentedMap({
        "apiVersion": "crd.antrea.io/v1beta1",
        "kind": "ClusterNetworkPolicy",
        "metadata": CommentedMap({
            "name": resource_name,
            "labels": CommentedMap({
                "ticket": DQS(str(ticket_number)),
                "cluster": DQS(str(cluster)),
            }),
        }),
        "spec": CommentedMap({
            "egress": CommentedSeq([new_rule]),
            "priority": 80,
            "tier": "atlas-tenants",
        }),
    })
    return doc


def write_new_policy(file_path: str, doc) -> None:
    yaml = YAML()
    yaml.version = (1, 2)
    yaml.preserve_quotes = True
    yaml.explicit_start = True
    yaml.default_flow_style = False
    yaml.indent(mapping=2, sequence=4, offset=2)
    yaml.width = 120

    os.makedirs(os.path.dirname(file_path) or ".", exist_ok=True)
    with open(file_path, "w", encoding="utf-8") as f:
        yaml.dump(doc, f)


def main() -> None:
    data = load_ticket_data()

    ticket_number = data.get("ticket_number", "UNKNOWN")
    cluster = data.get("cluster", "unknown")
    rules = data.get("rules", [])

    if not rules:
        raise ValueError("No rules provided in MOCK_TICKET_DATA.")

    # For now: one rule from the first item.
    # If later you want multiple appends, iterate over all rules.
    rule = rules[0]

    namespaces, fqdns, ports, purpose = extract_rule_data(rule)

    if not namespaces:
        raise ValueError("source_namespace is required.")
    if not fqdns:
        raise ValueError("destination_fqdn is required.")
    if not ports:
        raise ValueError("At least one valid port is required.")

    new_rule = build_egress_rule(namespaces, fqdns, ports, purpose)

    output_dir = os.environ.get("OUTPUT_PATH", "policies/cluster/customer-components/cluster-scoped/")
    file_name = os.environ.get("FILE_NAME", "antrea_clusternetworkpolicy_egress-livelink.yaml")
    full_path = os.path.join(output_dir, file_name)

    mode = os.environ.get("POLICY_MODE", "append").strip().lower()
    resource_name = os.environ.get("RESOURCE_NAME", "egress-livelink-mecc")

    if os.path.exists(full_path) and mode == "append":
        changed = append_rule_to_existing_policy(full_path, new_rule)
        print(f"Append mode finished. Changed={changed}")
    else:
        doc = create_new_policy_doc(ticket_number, cluster, new_rule, resource_name)
        write_new_policy(full_path, doc)
        print(f"New policy created at: {full_path}")


if __name__ == "__main__":
    main()
```

What this fixes

It changes the responsibility of the script from “always create a whole YAML” to “append when the file already exists, create otherwise.” That is the missing capability.

It also prevents duplicate rules based on the real functional content:

* namespaces
* destination FQDNs
* TCP ports

That matters, because otherwise repeated pipeline runs for the same ticket will keep appending the same block and polluting the policy.

The bigger problem in your pipeline

Your current pipeline structure is still biased toward file generation. That is why it will fight you on the existing-file use case.

This part is weak:

```yaml
generate_job:
  script:
    - python3 scripts/parse_input.py
```

because `parse_input.py` works only against files already present in the CI workspace. But the real existing file lives in the target repo, not in your PoC repo. So for append mode, the file must be available before the Python script runs.

That means one of these two designs is required.

Design A, the better one:
clone target repo in `generate_job`, modify file there, save as artifact, then commit in `propose_job`.

Design B:
keep cloning only in `propose_job`, but then run the Python merge script inside that cloned repo before commit.

Design A is cleaner because the generation/merge logic belongs in `generate`, not in `propose`.

A practical `.gitlab-ci.yml` adjustment

This is a safer structure for your use case:

```yaml
variables:
  SOPS_VERSION: "3.11.0"
  OUTPUT_PATH: "policies/cluster/customer-components/cluster-scoped/"
  FILE_NAME: "antrea_clusternetworkpolicy_egress-livelink.yaml"
  TARGET_BASE_BRANCH: "main"
  TARGET_PROJECT_PATH_ENC: "io-shared-services%2Fatlas%2Fkubernetes-management%2Fabrc1l-atlas-dev"

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
    - echo "$MOCK_TICKET_DATA" | python3 -m json.tool

generate_job:
  stage: generate
  script:
    - export TARGET_REPO_URL="https://${GIT_USERNAME}:${GIT_WRITE_TOKEN}@code.medtronic.com/io-shared-services/atlas/kubernetes-management/abrc1l-atlas-dev.git"
    - mkdir -p /tmp/target
    - cd /tmp/target
    - git clone --branch "$TARGET_BASE_BRANCH" "$TARGET_REPO_URL" repo
    - cd repo
    - mkdir -p "$OUTPUT_PATH"
    - export POLICY_MODE="append"
    - export OUTPUT_PATH="$PWD/$OUTPUT_PATH"
    - python3 "$CI_PROJECT_DIR/scripts/parse_input.py"
    - echo "Result file:"
    - cat "$OUTPUT_PATH$FILE_NAME"
  artifacts:
    paths:
      - /tmp/target/repo
    expire_in: 1 hour

propose_job:
  stage: propose
  script:
    - export TICKET_ID=$(echo "$MOCK_TICKET_DATA" | python3 -c "import sys,json; print(json.load(sys.stdin).get('ticket_number','UNKNOWN'))")
    - export BRANCH_NAME="feature/egress-rule-$TICKET_ID"
    - cd /tmp/target/repo
    - git config --global user.name "Cristian Fandino"
    - git config --global user.email "cristian.a.fandinomesa@medtronic.com"
    - git config --global --add safe.directory "$PWD"
    - git switch -c "$BRANCH_NAME"
    - git status
    - git add "$OUTPUT_PATH$FILE_NAME"
    - |
      if git diff --cached --quiet; then
        echo "No changes detected. Skipping commit and MR creation."
        exit 0
      fi
    - git commit -m "Append egress rule for $TICKET_ID"
    - git push -u origin "$BRANCH_NAME"
    - |
      curl --request POST \
        --header "PRIVATE-TOKEN: $GIT_WRITE_TOKEN" \
        --header "Content-Type: application/json" \
        "$CI_API_V4_URL/projects/$TARGET_PROJECT_PATH_ENC/merge_requests" \
        --data "{\"source_branch\":\"$BRANCH_NAME\",\"target_branch\":\"$TARGET_BASE_BRANCH\",\"title\":\"[PoC] Egress Rule: $TICKET_ID\",\"description\":\"Automated policy update for $TICKET_ID\"}"
  only:
    - main
```

What is wrong in your current GitLab job

Several things.

You define:

```yaml
TARGET_BASE_BRANCH="test-automation"
```

but the MR call uses:

```json
"target_branch":"main"
```

That is inconsistent. One of them is wrong.

You commit before switching branch:

```yaml
- git add "$OUTPUT_PATH"
- git commit -m "Auto-generate egress rule for $TICKET_ID"
- git switch -c "feature/egress-rule-$TICKET_ID"
```

That is sloppy. It works sometimes, but it is the wrong order. You should create the feature branch first, then commit on that branch.

You always copy a generated file over:

```yaml
- cp -v "$CI_PROJECT_DIR/$OUTPUT_PATH$FILE_NAME" "$OUTPUT_PATH/"
```

That is valid for “new file generation,” but for “update existing file,” it is the wrong mechanism. You need mutation of the repo file, not blind replacement.

The rule-name problem you still have to solve

This matters, and it will bite you later.

In your desired result, the new appended rule is:

```yaml
name: STG MECC V2 NFS write access
```

But the input JSON says:

```json
"purpose": "destination_fqdn"
```

Your code currently uses `purpose` for `name`. So unless you change your ticket parser or your JSON contract, your script will generate:

```yaml
name: destination_fqdn
```

not the expected result.

So be precise. Decide one of these contracts:

Option 1:

```json
"purpose": "STG MECC V2 NFS write access"
```

Option 2:

```json
"rule_name": "STG MECC V2 NFS write access"
```

Option 3:
derive it from cluster + system + access type via code mapping.

Option 2 is the cleanest. Your current JSON schema is too weak for this use case.

What I would change immediately

I would not keep the logic “one JSON becomes one full policy file.” That design is already becoming a limitation.

I would refactor into:

* `build_egress_rule(...)`
* `append_rule_to_existing_policy(...)`
* `create_new_policy_doc(...)`

That gives you both modes without duplicating logic.

And I would make duplicate detection mandatory. Otherwise your automation will create policy drift through repeated identical entries.

Your target output is feasible, but not with the current architecture unchanged. The architecture itself needs to move from file generation to file mutation.

If you want, I can now give you the exact final versions of:

1. `parse_input.py`
2. `.gitlab-ci.yml`
3. a sample `MOCK_TICKET_DATA` schema that correctly supports both “new file” and “append to existing file” modes.
