# 🚀 PoC: GitLab Automation for Antrea Egress Rule Generation

Perfecto. Con los datos reales de tu ticket y el archivo de ejemplo, puedo crear un **PoC completo y específico** para tu caso de uso. Este PoC transformará los datos del ticket en un **Antrea ClusterNetworkPolicy** listo para revisión.

---

## 1. 📊 Mapeo de Datos (Input → Output)

| Campo del Ticket | Transformación | Campo en YAML |
|------------------|----------------|---------------|
| `Source Namespace` | → | `spec.egress[].appliedTo[].namespaceSelector` |
| `Destination FQDN` | → | `spec.egress[].to[].fqdn` |
| `TCP/UDP/other port(s)` | Split por coma → | `spec.egress[].ports[]` (uno por puerto) |
| `Purpose of the Connection` | → | `spec.egress[].name` |
| `MECC Prod Cluster` | → | `metadata.name` (prefijo) + `spec.tier` |

---

## 2. 📂 Estructura del Repositorio GitLab

```
network-policy-automation/
├── .gitlab-ci.yml                    # Pipeline definition
├── templates/
│   └── antrea_egress_policy.yaml.j2  # Jinja2 template
├── scripts/
│   ├── parse_input.py                # Parsea el JSON de entrada
│   └── validate_policy.py            # Valida reglas (opcional)
├── policies/
│   └── cluster/customer-components/cluster-scoped/  # Output path
└── README.md
```

---

## 3. ⚙️ Variables CI/CD (GitLab Settings)

En **Settings → CI/CD → Variables**, configura:

| Key | Value | Type | Masked |
|-----|-------|------|--------|
| `MOCK_TICKET_DATA` | (JSON abajo) | Variable | No |
| `GITLAB_TOKEN` | `glpat-xxxxxxxxxxxx` | Variable | **Sí** |
| `TARGET_BRANCH` | `main` | Variable | No |

**Contenido de `MOCK_TICKET_DATA`:**
```json
{
  "ticket_number": "INC-MECC-001",
  "requester": "network-team@medtronic.com",
  "cluster": "abrc1-atlas-preview-prd",
  "rules": [
    {
      "source_namespace": "livelink-ignition",
      "destination_ip": "10.76.217.22",
      "destination_fqdn": "abrc1-avis3gw-test.corp.medtronic.com",
      "ports": "2049,20048,32803",
      "purpose": "enable NFS write access"
    },
    {
      "source_namespace": "livelink-ignition",
      "destination_ip": "10.76.217.9",
      "destination_fqdn": "meccstoragegw-prod.medtronic.com",
      "ports": "2049,20048,32803",
      "purpose": "enable NFS write access"
    }
  ]
}
```

---

## 4. 📝 Archivos del PoC

### A. Plantilla Jinja2 (`templates/antrea_egress_policy.yaml.j2`)

```yaml
apiVersion: crd.antrea.io/v1beta1
kind: ClusterNetworkPolicy
metadata:
  name: egress-livelink-mecc
  labels:
    ticket: {{ ticket_number }}
    cluster: {{ cluster }}
spec:
  egress:
  - action: Allow
    appliedTo:
    - namespaceSelector:
        matchExpressions:
          - key: "kubernetes.io/metadata.name"
            operator: "In"
            values: ["{{ source_namespace }}"]
    enableLogging: true
    name: {{ purpose | upper }}
    to:
{% for fqdn in destination_fqdns %}
      - fqdn: {{ fqdn }}
{% endfor %}
    ports:
{% for port in ports_list %}
      - protocol: TCP
        port: {{ port }}
{% endfor %}
  priority: 80
  tier: atlas-tenants
```

### B. Script de Parseo (`scripts/parse_input.py`)

```python
#!/usr/bin/env python3
import json
import os
import sys

def parse_ticket_data():
    """Parse MOCK_TICKET_DATA from CI/CD variables"""
    raw_data = os.environ.get('MOCK_TICKET_DATA')
    if not raw_data:
        print("❌ ERROR: MOCK_TICKET_DATA not found")
        sys.exit(1)
    
    try:
        data = json.loads(raw_data)
        print(f"✅ Ticket: {data.get('ticket_number')}")
        print(f"✅ Cluster: {data.get('cluster')}")
        print(f"✅ Rules count: {len(data.get('rules', []))}")
        return data
    except json.JSONDecodeError as e:
        print(f"❌ ERROR: Invalid JSON - {e}")
        sys.exit(1)

def extract_unique_values(rules):
    """Extract unique namespaces, FQDNs, and ports from all rules"""
    namespaces = set()
    fqdns = set()
    ports = set()
    
    for rule in rules:
        namespaces.add(rule.get('source_namespace'))
        fqdns.add(rule.get('destination_fqdn'))
        port_str = rule.get('ports', '')
        for port in port_str.replace(' ', ',').split(','):
            if port.strip():
                ports.add(int(port.strip()))
    
    return list(namespaces), sorted(fqdns), sorted(ports)

if __name__ == '__main__':
    data = parse_ticket_data()
    namespaces, fqdns, ports = extract_unique_values(data.get('rules', []))
    
    # Export as environment variables for CI
    with open(os.environ.get('CI_ENVIRONMENT_FILE', '/dev/null'), 'w') as f:
        f.write(f"EXPORTED_NAMESPACES={','.join(namespaces)}\n")
        f.write(f"EXPORTED_FQDNS={','.join(fqdns)}\n")
        f.write(f"EXPORTED_PORTS={','.join(map(str, ports))}\n")
    
    print(f"✅ Namespaces: {namespaces}")
    print(f"✅ FQDNs: {fqdns}")
    print(f"✅ Ports: {ports}")
```

### C. Pipeline (`.gitlab-ci.yml`)

```yaml
stages:
  - parse
  - generate
  - propose

variables:
  OUTPUT_PATH: "policies/cluster/customer-components/cluster-scoped/"
  FILE_NAME: "antrea_clusternetworkpolicy_egress-livelink-mecc.yaml"

# ========== STAGE 1: PARSE INPUT ==========
parse_job:
  stage: parse
  script:
    - echo "📥 Parsing ticket data..."
    - python3 scripts/parse_input.py
    - echo "✅ Parse complete"
  artifacts:
    reports:
      dotenv: parse_output.env

# ========== STAGE 2: GENERATE POLICY ==========
generate_job:
  stage: generate
  script:
    - echo "📝 Generating Antrea ClusterNetworkPolicy..."
    - mkdir -p $OUTPUT_PATH
    - |
      python3 -c "
      import json
      import os
      from jinja2 import Template
      
      # Load template
      with open('templates/antrea_egress_policy.yaml.j2', 'r') as f:
          template = Template(f.read())
      
      # Load ticket data
      data = json.loads(os.environ.get('MOCK_TICKET_DATA'))
      
      # Extract unique values
      rules = data.get('rules', [])
      namespaces = list(set(r['source_namespace'] for r in rules))
      fqdns = list(set(r['destination_fqdn'] for r in rules))
      ports = []
      for r in rules:
          for p in r.get('ports', '').replace(' ', ',').split(','):
              if p.strip():
                  ports.append(int(p.strip()))
      
      # Render
      output = template.render(
          ticket_number=data.get('ticket_number'),
          cluster=data.get('cluster'),
          source_namespace=namespaces[0] if namespaces else 'default',
          destination_fqdns=fqdns,
          ports_list=sorted(ports),
          purpose=rules[0].get('purpose', 'NFS access') if rules else 'Unknown'
      )
      
      # Write file
      output_path = os.environ.get('OUTPUT_PATH')
      file_name = os.environ.get('FILE_NAME')
      with open(f'{output_path}{file_name}', 'w') as f:
          f.write(output)
      
      print(f'✅ Generated: {output_path}{file_name}')
      "
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
    - |
      TICKET_ID=$(echo $MOCK_TICKET_DATA | python3 -c "import sys,json; print(json.load(sys.stdin).get('ticket_number','UNKNOWN'))")
      BRANCH_NAME="feature/egress-rule-$TICKET_ID"
      echo "BRANCH_NAME=$BRANCH_NAME" >> build.env
    - git checkout -b $BRANCH_NAME
    - git add $OUTPUT_PATH
    - git commit -m "Auto-generate egress rule for $TICKET_ID

    Source: GitLab PoC Automation
    Cluster: abrc1-atlas-preview-prd
    Namespace: livelink-ignition
    "
    - git push -u origin $BRANCH_NAME
    - |
      echo "📬 Creating Merge Request..."
      curl --request POST \
        --header "PRIVATE-TOKEN: $GITLAB_TOKEN" \
        --header "Content-Type: application/json" \
        "$CI_API_V4_URL/projects/$CI_PROJECT_ID/merge_requests" \
        --data @- << EOF
      {
        "source_branch": "$BRANCH_NAME",
        "target_branch": "$TARGET_BRANCH",
        "title": "[PoC] Egress Rule: $TICKET_ID - MECC V2 Prod",
        "description": "## Automated Policy Generation\n\n**Ticket:** $TICKET_ID\n**Cluster:** abrc1-atlas-preview-prd\n**Namespace:** livelink-ignition\n**Purpose:** NFS write access\n\n### Changes\n- Created: \`$OUTPUT_PATH$FILE_NAME\`\n\n### Review Checklist\n- [ ] FQDNs validated\n- [ ] Ports match requirements (2049, 20048, 32803)\n- [ ] Namespace correct\n\n---\n*Generated by GitLab PoC Automation*",
        "remove_source_branch": true
      }
      EOF
    - echo "✅ Merge Request created successfully!"
  only:
    - main
```

---

## 5. 🏃‍️ Ejecución Paso a Paso

### Paso 1: Crear Repositorio en GitLab
```bash
# Localmente
mkdir network-policy-automation
cd network-policy-automation
git init
git remote add origin https://gitlab.com/your-group/network-policy-automation.git
```

### Paso 2: Agregar Archivos
```bash
# Crear estructura
mkdir -p templates scripts policies/cluster/customer-components/cluster-scoped/

# Agregar archivos (copiar contenido de arriba)
# .gitlab-ci.yml, templates/, scripts/
```

### Paso 3: Configurar Variables
1. Ir a **Settings → CI/CD → Variables**
2. Agregar `MOCK_TICKET_DATA`, `GITLAB_TOKEN`, `TARGET_BRANCH`

### Paso 4: Ejecutar Pipeline
1. Ir a **Build → Pipelines**
2. Click en **"Run Pipeline"**
3. Seleccionar branch `main`
4. Click **"Run"**

### Paso 5: Verificar Resultados
| Stage | Qué Verificar |
|-------|---------------|
| `parse_job` | Logs muestran ticket, cluster, reglas |
| `generate_job` | Artifact contiene el YAML generado |
| `propose_job` | Mensaje "Merge Request created" |

### Paso 6: Revisar Merge Request
Deberías ver un MR con:
- **Título:** `[PoC] Egress Rule: INC-MECC-001 - MECC V2 Prod`
- **Branch:** `feature/egress-rule-INC-MECC-001`
- **Changes:** Nuevo archivo en `policies/cluster/customer-components/cluster-scoped/`
- **Description:** Checklist de revisión incluido

---

## 6. ✅ Output Esperado (YAML Generado)

El archivo `antrea_clusternetworkpolicy_egress-livelink-mecc.yaml` debe verse así:

```yaml
apiVersion: crd.antrea.io/v1beta1
kind: ClusterNetworkPolicy
metadata:
  name: egress-livelink-mecc
  labels:
    ticket: INC-MECC-001
    cluster: abrc1-atlas-preview-prd
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
    name: ENABLE NFS WRITE ACCESS
    to:
      - fqdn: abrc1-avis3gw-test.corp.medtronic.com
      - fqdn: meccstoragegw-prod.medtronic.com
    ports:
      - protocol: TCP
        port: 2049
      - protocol: TCP
        port: 20048
      - protocol: TCP
        port: 32803
  priority: 80
  tier: atlas-tenants
```

---

## 7. 📋 Criterios de Aceptación del PoC

| # | Criterio | Estado |
|---|----------|--------|
| 1 | Pipeline se ejecuta sin errores | ⬜ |
| 2 | YAML generado coincide con el ejemplo | ⬜ |
| 3 | FQDNs correctos (2 dominios) | ⬜ |
| 4 | Puertos correctos (2049, 20048, 32803) | ⬜ |
| 5 | Namespace correcto (livelink-ignition) | ⬜ |
| 6 | MR creado automáticamente | ⬜ |
| 7 | Branch se elimina al mergear | ⬜ |

---

## 8. 🔄 Siguientes Pasos (Post-PoC)

Una vez validado este PoC:

| Fase | Acción |
|------|--------|
| **Fase 2** | Reemplazar `MOCK_TICKET_DATA` por webhook real de ServiceNow |
| **Fase 3** | Agregar validaciones (FQDN whitelist, puerto NFS only) |
| **Fase 4** | Configurar webhook de GitLab → ServiceNow (cierre de ticket) |
| **Fase 5** | Integrar con ArgoCD/Flux para deployment automático post-merge |

---

## 📞 Soporte para el PoC

Si encuentras errores durante la ejecución:

| Error | Solución |
|-------|----------|
| `MOCK_TICKET_DATA not found` | Verificar variable en Settings → CI/CD |
| `git push failed` | Verificar `GITLAB_TOKEN` con permisos `write_repository` |
| `MR creation failed` | Verificar token con permisos `api` |
| `Jinja2 not found` | Agregar `pip install jinja2` en `before_script` |

---

# 🐍 ¿Dónde hacer `pip install jinja2` en GitLab CI/CD?

En GitLab CI/CD **no se instala manualmente** en un servidor. Hay **3 opciones** para tener Jinja2 disponible en tu pipeline:

---

## ✅ Opción 1: `before_script` en `.gitlab-ci.yml` (Recomendada para PoC)

Agrega la instalación directa en tu archivo de pipeline. Es la opción más rápida para validar el PoC.

```yaml
# .gitlab-ci.yml
stages:
  - parse
  - generate
  - propose

variables:
  OUTPUT_PATH: "policies/cluster/customer-components/cluster-scoped/"
  FILE_NAME: "antrea_clusternetworkpolicy_egress-livelink-mecc.yaml"

# Instalación global para todos los jobs
before_script:
  - echo "📦 Installing Python dependencies..."
  - pip3 install jinja2

# ========== STAGE 1: PARSE INPUT ==========
parse_job:
  stage: parse
  script:
    - echo "📥 Parsing ticket data..."
    - python3 scripts/parse_input.py
  artifacts:
    reports:
      dotenv: parse_output.env

# ========== STAGE 2: GENERATE POLICY ==========
generate_job:
  stage: generate
  script:
    - echo "📝 Generating Antrea ClusterNetworkPolicy..."
    - mkdir -p $OUTPUT_PATH
    - python3 scripts/generate_rule.py
    - cat $OUTPUT_PATH$FILE_NAME
  artifacts:
    paths:
      - $OUTPUT_PATH

# ========== STAGE 3: CREATE MERGE REQUEST ==========
propose_job:
  stage: propose
  script:
    - git config user.name "Network Automation Bot"
    - git config user.email "automation-bot@medtronic.com"
    - |
      TICKET_ID=$(echo $MOCK_TICKET_DATA | python3 -c "import sys,json; print(json.load(sys.stdin).get('ticket_number','UNKNOWN'))")
      BRANCH_NAME="feature/egress-rule-$TICKET_ID"
    - git checkout -b $BRANCH_NAME
    - git add $OUTPUT_PATH
    - git commit -m "Auto-generate egress rule for $TICKET_ID"
    - git push -u origin $BRANCH_NAME
    - |
      curl --request POST \
        --header "PRIVATE-TOKEN: $GITLAB_TOKEN" \
        --header "Content-Type: application/json" \
        "$CI_API_V4_URL/projects/$CI_PROJECT_ID/merge_requests" \
        --data "{\"source_branch\":\"$BRANCH_NAME\",\"target_branch\":\"main\",\"title\":\"[PoC] Egress Rule: $TICKET_ID\"}"
    - echo "✅ Merge Request created!"
  only:
    - main
```

### ✅ Ventajas
- Simple y rápido
- No requiere crear archivos adicionales
- Ideal para PoC

### ⚠️ Desventajas
- Instala en cada ejecución del pipeline (agrega ~5-10 segundos)
- No es persistente entre jobs (por eso va en `before_script` global)

---

## ✅ Opción 2: Archivo `requirements.txt` + `pip install -r` (Best Practice)

Si quieres seguir mejores prácticas desde el inicio, crea un archivo de dependencias.

**Paso 1:** Crea el archivo `requirements.txt` en la raíz del repositorio:
```txt
jinja2==3.1.2
```

**Paso 2:** Actualiza tu `.gitlab-ci.yml`:
```yaml
before_script:
  - echo "📦 Installing Python dependencies..."
  - pip3 install -r requirements.txt
```

### ✅ Ventajas
- Más profesional y escalable
- Fácil agregar más librerías después (ej. `requests`, `pyyaml`)
- Versionado de dependencias

---

## ✅ Opción 3: Usar Imagen Docker con Python + Jinja2 Pre-instalado (Producción)

Para entornos de producción, usa una imagen personalizada que ya tenga las dependencias.

```yaml
# .gitlab-ci.yml
image: python:3.11-slim

variables:
  PIP_CACHE_DIR: "$CI_PROJECT_DIR/.pip-cache"

cache:
  paths:
    - .pip-cache/

before_script:
  - pip install --cache-dir $PIP_CACHE_DIR jinja2

stages:
  - parse
  - generate
  - propose
```

### ✅ Ventajas
- Más rápido (usa cache entre pipelines)
- Entorno consistente y reproducible
- Recomendado para producción

---

## 📊 Comparación de Opciones

| Opción | Tiempo Setup | Velocidad Pipeline | Recomendado para |
|--------|--------------|--------------------|------------------|
| **Opción 1** (`pip install` directo) | ⚡ 1 min | 🐢 Normal | **PoC / Pruebas** |
| **Opción 2** (`requirements.txt`) | ⚡ 2 min | 🐢 Normal | **Proyecto en crecimiento** |
| **Opción 3** (Docker + Cache) | ⏱️ 10 min | 🚀 Rápido | **Producción** |

---

## 🚀 Recomendación para tu PoC

Usa la **Opción 1** ahora para validar rápido. Cuando el PoC sea exitoso y pases a producción, migra a la **Opción 3**.

**¿Quieres que actualice el `.gitlab-ci.yml` completo del PoC con esta configuración?** 📝
