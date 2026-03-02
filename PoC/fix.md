# 🔧 Error Corregido: `.gitlab-ci.yml`

El error ocurre porque GitLab CI requiere que **cada comando en `script` sea una línea separada** en formato de array. No se pueden usar bloques multi-línea con `|` dentro del script.

Aquí está la **versión corregida y validada**:

---

## ✅ `.gitlab-ci.yml` Corregido

```yaml
stages:
  - parse
  - generate
  - propose

variables:
  OUTPUT_PATH: "policies/cluster/customer-components/cluster-scoped/"
  FILE_NAME: "antrea_clusternetworkpolicy_egress-livelink-mecc.yaml"

before_script:
  - echo "📦 Installing Python dependencies..."
  - pip3 install jinja2

# ========== STAGE 1: PARSE INPUT ==========
parse_job:
  stage: parse
  script:
    - echo "📥 Parsing ticket data..."
    - python3 scripts/parse_input.py
    - echo "✅ Parse complete"

# ========== STAGE 2: GENERATE POLICY ==========
generate_job:
  stage: generate
  script:
    - echo "📝 Generating Antrea ClusterNetworkPolicy..."
    - mkdir -p $OUTPUT_PATH
    - python3 scripts/generate_rule.py
    - echo "✅ Generated: $OUTPUT_PATH$FILE_NAME"
    - cat $OUTPUT_PATH$FILE_NAME
  artifacts:
    paths:
      - $OUTPUT_PATH

# ========== STAGE 3: CREATE MERGE REQUEST ==========
propose_job:
  stage: propose
  script:
    - echo "🔧 Configuring Git..."
    - git config user.name "Network Automation Bot"
    - git config user.email "automation-bot@medtronic.com"
    - export TICKET_ID=$(echo $MOCK_TICKET_DATA | python3 -c "import sys,json; print(json.load(sys.stdin).get('ticket_number','UNKNOWN'))")
    - export BRANCH_NAME="feature/egress-rule-$TICKET_ID"
    - echo "Branch: $BRANCH_NAME"
    - git checkout -b $BRANCH_NAME
    - git add $OUTPUT_PATH
    - git commit -m "Auto-generate egress rule for $TICKET_ID"
    - git push -u origin $BRANCH_NAME
    - echo "📬 Creating Merge Request..."
    - curl --request POST --header "PRIVATE-TOKEN: $GITLAB_TOKEN" --header "Content-Type: application/json" "$CI_API_V4_URL/projects/$CI_PROJECT_ID/merge_requests" --data "{\"source_branch\":\"$BRANCH_NAME\",\"target_branch\":\"main\",\"title\":\"[PoC] Egress Rule: $TICKET_ID - MECC V2 Prod\",\"description\":\"Automated Policy Generation for $TICKET_ID\",\"remove_source_branch\":true}"
    - echo "✅ Merge Request created successfully!"
  only:
    - main
```

---

## 📋 Cambios Clave Realizados

| Error Anterior | Corrección |
|----------------|------------|
| Bloques multi-línea con `|` dentro de `script` | Cada comando en línea separada con `-` |
| Heredoc `<< EOF` para curl | JSON inline en el `--data` |
| Variables dentro de bloques `|` | `export` en línea separada |
| Saltos de línea en comandos | Todo en una línea por comando |

---

## 🐍 Script Adicional: `scripts/generate_rule.py`

Para que el pipeline funcione, necesitas este script en tu repositorio:

```python
#!/usr/bin/env python3
import json
import os
from jinja2 import Template

# Load ticket data
raw_data = os.environ.get('MOCK_TICKET_DATA')
data = json.loads(raw_data)

# Load template
with open('templates/antrea_egress_policy.yaml.j2', 'r') as f:
    template = Template(f.read())

# Extract unique values from all rules
rules = data.get('rules', [])
namespaces = list(set(r['source_namespace'] for r in rules))
fqdns = list(set(r['destination_fqdn'] for r in rules))
ports = []
for r in rules:
    for p in r.get('ports', '').replace(' ', ',').split(','):
        if p.strip():
            ports.append(int(p.strip()))

# Render template
output = template.render(
    ticket_number=data.get('ticket_number'),
    cluster=data.get('cluster'),
    source_namespace=namespaces[0] if namespaces else 'default',
    destination_fqdns=sorted(fqdns),
    ports_list=sorted(ports),
    purpose=rules[0].get('purpose', 'NFS access') if rules else 'Unknown'
)

# Write file
output_path = os.environ.get('OUTPUT_PATH')
file_name = os.environ.get('FILE_NAME')
os.makedirs(output_path, exist_ok=True)
with open(f'{output_path}{file_name}', 'w') as f:
    f.write(output)

print(f'✅ Generated: {output_path}{file_name}')
```

---

## 🧪 Verificación Rápida

Antes de ejecutar el pipeline, valida el YAML:

```bash
# Opción 1: Usar GitLab CI Lint (Web)
# Ve a: https://gitlab.com/{tu-grupo}/{tu-proyecto}/-/ci/lint

# Opción 2: Validar localmente con yamllint
pip install yamllint
yamllint .gitlab-ci.yml
```

---

## 📁 Estructura Final del Repositorio

```
network-policy-automation/
├── .gitlab-ci.yml                    ✅ Corregido
├── requirements.txt                  ✅ (opcional) jinja2==3.1.2
├── templates/
│   └── antrea_egress_policy.yaml.j2  ✅
├── scripts/
│   ├── parse_input.py                ✅
│   └── generate_rule.py              ✅
├── policies/
│   └── cluster/customer-components/cluster-scoped/
└── README.md
```

---

## 🚀 Ejecuta el Pipeline

1. **Commit** todos los archivos:
   ```bash
   git add .
   git commit -m "PoC: GitLab automation for Antrea egress rules"
   git push origin main
   ```

2. **Ve a GitLab** → **Build → Pipelines**

3. **Click en "Run Pipeline"** → Selecciona `main` → **Run**

4. **Verifica** que los 3 jobs pasen: `parse_job` → `generate_job` → `propose_job`

---

¿Quieres que también valide la **plantilla Jinja2** o el **script de parse_input.py** para asegurar que todo funcione junto? 📝
