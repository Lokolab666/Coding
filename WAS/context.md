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
