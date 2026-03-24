The error is expected. Your current script still does this:

def load_existing_yaml(path):
    file_path = Path(path)
    if not file_path.exists():
        return None

    with file_path.open("r") as f:
        return yaml.load(f)

So it tries to parse invalid YAML directly and dies. There is no repair step in the version you pasted.

And the file is clearly malformed. Not just one line. These are all wrong inside spec.egress:

appliedTo: is misindented

matchExpressions: is misindented

operator: and values: are misindented

each port: line is misindented


Your repo file is this broken shape:

egress:
    - action: Allow
    appliedTo:
      - namespaceSelector:
        matchExpressions:
          - key: "kubernetes.io/metadata.name"
          operator: "In"
          values: ["livelink-ignition"]
    enableLogging: true
    ...
    ports:
      - protocol: TCP
      port: 2049

That is not valid YAML.

Use this fix. Replace your current load_existing_yaml() with a version that repairs this known malformed Antrea policy structure before parsing.

Also add these two imports:

from ruamel.yaml.parser import ParserError
from ruamel.yaml.scanner import ScannerError

Then add this function and replace load_existing_yaml().

def repair_known_policy_yaml(raw_text: str) -> str:
    fixed = []
    in_egress = False
    in_ports = False
    in_to = False

    for line in raw_text.splitlines():
        stripped = line.strip()

        if not stripped:
            fixed.append("")
            continue

        # top-level / already good sections
        if stripped == "---":
            fixed.append("---")
            continue

        if stripped.startswith(("apiVersion:", "kind:", "metadata:", "spec:")):
            fixed.append(stripped)
            continue

        if stripped.startswith(("name:", "labels:")) and not in_egress:
            fixed.append(f"  {stripped}")
            continue

        if stripped.startswith(("ticket:", "cluster:")) and not in_egress:
            fixed.append(f"    {stripped}")
            continue

        if stripped == "egress:":
            in_egress = True
            in_ports = False
            in_to = False
            fixed.append("  egress:")
            continue

        if stripped.startswith(("priority:", "tier:")):
            in_egress = False
            in_ports = False
            in_to = False
            fixed.append(f"  {stripped}")
            continue

        if in_egress:
            if stripped.startswith("- action:"):
                in_ports = False
                in_to = False
                fixed.append(f"    {stripped}")
                continue

            if stripped == "appliedTo:":
                fixed.append("      appliedTo:")
                continue

            if stripped == "- namespaceSelector:":
                fixed.append("        - namespaceSelector:")
                continue

            if stripped == "matchExpressions:":
                fixed.append("            matchExpressions:")
                continue

            if stripped.startswith("- key:"):
                fixed.append(f"              {stripped}")
                continue

            if stripped.startswith("operator:"):
                fixed.append(f"                {stripped}")
                continue

            if stripped.startswith("values:"):
                fixed.append(f"                {stripped}")
                continue

            if stripped.startswith("enableLogging:"):
                in_ports = False
                in_to = False
                fixed.append(f"      {stripped}")
                continue

            if stripped.startswith("name:"):
                in_ports = False
                in_to = False
                fixed.append(f"      {stripped}")
                continue

            if stripped == "to:":
                in_to = True
                in_ports = False
                fixed.append("      to:")
                continue

            if stripped.startswith(("- fqdn:", "- ipBlock:")):
                fixed.append(f"        {stripped}")
                continue

            if stripped.startswith("cidr:"):
                fixed.append(f"          {stripped}")
                continue

            if stripped == "ports:":
                in_ports = True
                in_to = False
                fixed.append("      ports:")
                continue

            if stripped.startswith("- protocol:"):
                fixed.append(f"        {stripped}")
                continue

            if stripped.startswith("port:"):
                fixed.append(f"          {stripped}")
                continue

        # fallback: keep original line if it doesn't match any known broken pattern
        fixed.append(line)

    return "\n".join(fixed) + "\n"


def load_existing_yaml(path):
    file_path = Path(path)
    if not file_path.exists():
        return None

    raw = file_path.read_text()

    try:
        return yaml.load(raw)
    except (ParserError, ScannerError) as e:
        print(f"[WARN] Invalid YAML detected in {path}. Trying repair...")
        print(f"[WARN] Parser error: {e}")

        repaired = repair_known_policy_yaml(raw)

        print("[INFO] Repaired YAML preview:")
        for idx, line in enumerate(repaired.splitlines(), start=1):
            print(f"{idx:>4} {line}")

        # Parse repaired text
        return yaml.load(repaired)

That is the immediate fix.

There is also a second issue you are ignoring: your expected new rule name is impossible from the JSON you showed earlier. Your JSON had:

"purpose": "destination_fqdn"

So your script will generate:

name: destination_fqdn

not:

name: Egress rule for MECC V2

If you want that exact name, send it explicitly in the JSON:

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

One more thing: this repair is a temporary shield, not a clean design. The real problem is that the source file in the repo is malformed. The proper fix is to correct that file once in the target branch. Otherwise you are building automation on corrupted input, which is a bad idea.

If you want, I can give you the full final parse_input.py already merged with this repair logic so you can paste it as a single file.
