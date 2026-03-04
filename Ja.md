#!/usr/bin/env python3
import json
import os
from datetime import datetime

def load_ticket_data():
    """Cargar datos del ticket desde variable de entorno"""
    raw_data = os.environ.get('MOCK_TICKET_DATA')
    if not raw_data:
        raise ValueError("MOCK_TICKET_DATA no está definida")
    return json.loads(raw_data)

def extract_unique_values(rules):
    """Extraer valores ÚNICOS de namespaces, FQDNs y puertos"""
    namespaces = set()
    fqdns = set()
    ports = set()
    purpose = ""
    
    for rule in rules:
        namespaces.add(rule.get('source_namespace'))
        fqdns.add(rule.get('destination_fqdn'))
        purpose = rule.get('purpose', 'NFS access')
        
        # Parsear puertos (manejar espacios y comas)
        port_str = rule.get('ports', '')
        for port in port_str.replace(' ', ',').split(','):
            if port.strip():
                ports.add(int(port.strip()))
    
    return list(namespaces), sorted(fqdns), sorted(ports), purpose

def generate_yaml(ticket_number, cluster, namespaces, fqdns, ports, purpose):
    """Generar YAML de Antrea ClusterNetworkPolicy"""
    
    yaml_content = f"""apiVersion: crd.antrea.io/v1beta1
kind: ClusterNetworkPolicy
metadata:
  name: egress-livelink-mecc
  labels:
    ticket: {ticket_number}
    cluster: {cluster}
spec:
  egress:
  - action: Allow
    appliedTo:
    - namespaceSelector:
        matchExpressions:
          - key: "kubernetes.io/metadata.name"
            operator: "In"
            values: {json.dumps(namespaces)}
    enableLogging: true
    name: {purpose.upper()}
    to:
"""
    
    # Agregar FQDNs (lista única)
    for fqdn in fqdns:
        yaml_content += f"      - fqdn: {fqdn}\n"
    
    yaml_content += "    ports:\n"
    
    # Agregar puertos (lista única, SIN duplicados)
    for port in ports:
        yaml_content += f"      - protocol: TCP\n"
        yaml_content += f"        port: {port}\n"
    
    yaml_content += """  priority: 80
  tier: atlas-tenants
"""
    
    return yaml_content

def main():
    # Cargar datos
    data = load_ticket_data()
    ticket_number = data.get('ticket_number', 'UNKNOWN')
    cluster = data.get('cluster', 'unknown')
    rules = data.get('rules', [])
    
    # Extraer valores únicos
    namespaces, fqdns, ports, purpose = extract_unique_values(rules)
    
    print(f"✅ Ticket: {ticket_number}")
    print(f"✅ Cluster: {cluster}")
    print(f"✅ Namespaces únicos: {namespaces}")
    print(f"✅ FQDNs únicos: {fqdns}")
    print(f"✅ Puertos únicos: {ports}")
    
    # Generar YAML
    yaml_content = generate_yaml(ticket_number, cluster, namespaces, fqdns, ports, purpose)
    
    # Guardar archivo
    output_path = os.environ.get('OUTPUT_PATH', 'policies/cluster/customer-components/cluster-scoped/')
    file_name = os.environ.get('FILE_NAME', 'antrea_clusternetworkpolicy_egress-livelink.yaml')
    
    os.makedirs(output_path, exist_ok=True)
    full_path = f"{output_path}{file_name}"
    
    with open(full_path, 'w') as f:
        f.write(yaml_content)
    
    print(f"✅ Archivo generado: {full_path}")

if __name__ == '__main__':
    main()
