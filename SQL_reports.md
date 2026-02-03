manage_ssrs_role.py

#!/usr/bin/env python3
"""
Gestiona rol Browser en SSRS 2019 usando API REST interna (NO SOAP)
Ejecutar desde nodo Jenkins (NO requiere instalación en servidor SSRS)
"""
import os
import sys
import json
import argparse
import requests
from requests_ntlm import HttpNtlmAuth
from urllib.parse import quote

def get_item_id(base_url, username, password, item_path):
    """
    Obtiene el itemId interno de SSRS a partir de la ruta (/Carpeta/Reporte)
    """
    search_url = f"{base_url.rstrip('/')}/api/search/query"
    
    payload = {
        "searchQuery": item_path.split('/')[-1],  # Nombre del item
        "searchConditions": [
            {
                "fieldName": "Path",
                "operator": "Equals",
                "value": item_path
            }
        ],
        "searchFlags": 0
    }
    
    response = requests.post(
        search_url,
        json=payload,
        auth=HttpNtlmAuth(username, password),
        verify=False  # Solo en entornos con certificados internos; usar True en producción
    )
    response.raise_for_status()
    
    results = response.json()
    if not results.get('Items'):
        raise Exception(f"Item no encontrado: {item_path}")
    
    return results['Items'][0]['Id']

def get_security(base_url, username, password, item_id):
    """
    Obtiene políticas actuales de un item
    """
    url = f"{base_url.rstrip('/')}/api/security/items/{item_id}"
    
    response = requests.get(
        url,
        auth=HttpNtlmAuth(username, password),
        verify=False
    )
    response.raise_for_status()
    
    return response.json()

def set_security(base_url, username, password, item_id, policies):
    """
    Actualiza políticas de seguridad
    """
    url = f"{base_url.rstrip('/')}/api/security/items/{item_id}"
    
    payload = {
        "Policies": policies,
        "SecurityDataVersion": 1
    }
    
    response = requests.post(
        url,
        json=payload,
        auth=HttpNtlmAuth(username, password),
        verify=False
    )
    response.raise_for_status()
    
    return response.json()

def manage_browser_role(
    reports_url,
    username,
    password,
    item_path,
    target_user,
    assign=True
):
    """
    Asigna o elimina rol Browser para un usuario en SSRS 2019
    """
    # Paso 1: Obtener itemId desde ruta
    print(f"Buscando item: {item_path}")
    item_id = get_item_id(reports_url, username, password, item_path)
    print(f"✓ Item ID: {item_id}")
    
    # Paso 2: Obtener políticas actuales
    print("Obteniendo políticas actuales...")
    security = get_security(reports_url, username, password, item_id)
    policies = security.get('Policies', [])
    print(f"Políticas actuales: {len(policies)} usuarios")
    
    # Paso 3: Modificar políticas
    user_found = False
    for policy in policies:
        if policy['GroupUserName'].lower() == target_user.lower():
            user_found = True
            roles = [r['Name'] for r in policy.get('Roles', [])]
            
            if assign:
                if 'Browser' not in roles:
                    roles.append('Browser')
                    print(f"✓ Agregando rol Browser a {target_user}")
                else:
                    print(f"ℹ️ {target_user} ya tiene rol Browser")
            else:
                if 'Browser' in roles:
                    roles.remove('Browser')
                    print(f"✓ Eliminando rol Browser de {target_user}")
                else:
                    print(f"ℹ️ {target_user} no tiene rol Browser")
            
            # Actualizar roles en política
            policy['Roles'] = [{'Name': r} for r in roles]
            break
    
    # Si no existe el usuario y se quiere asignar
    if assign and not user_found:
        policies.append({
            "GroupUserName": target_user,
            "Roles": [{"Name": "Browser"}],
            "GroupUserNameEditable": False
        })
        print(f"✓ Creando nueva asignación para {target_user}")
    
    # Eliminar políticas vacías (sin roles)
    policies = [p for p in policies if p.get('Roles')]
    
    # Paso 4: Aplicar cambios
    print("Aplicando cambios de seguridad...")
    set_security(reports_url, username, password, item_id, policies)
    print(f"✅ Operación completada: {'ASIGNADO' if assign else 'ELIMINADO'} rol Browser para {target_user}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Gestionar rol Browser en SSRS 2019 (API REST)')
    parser.add_argument('--reports-url', required=True, 
                        help='URL del Web Portal de SSRS (ej: http://ssrs-server/Reports)')
    parser.add_argument('--username', required=True, help='Usuario Windows (ej: DOMINIO\\usuario)')
    parser.add_argument('--password', required=True, help='Contraseña')
    parser.add_argument('--item-path', required=True, 
                        help='Ruta completa del item (ej: /PaginatedReports/MiReporte)')
    parser.add_argument('--target-user', required=True, 
                        help='Usuario a gestionar (ej: DOMINIO\\usuario)')
    parser.add_argument('--assign', action='store_true', help='Asignar rol (default: eliminar)')
    
    args = parser.parse_args()
    
    try:
        # Desactivar warnings de SSL (solo para entornos con certificados internos)
        import urllib3
        urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
        
        manage_browser_role(
            reports_url=args.reports_url,
            username=args.username,
            password=args.password,
            item_path=args.item_path,
            target_user=args.target_user,
            assign=args.assign
        )
        sys.exit(0)
    except Exception as e:
        print(f"❌ Error: {str(e)}")
        sys.exit(1)







pipeline {
    agent {
        // Nodo Jenkins CON Python (NO es el servidor SSRS)
        // Requisito mínimo: Python 3.6+ con requests y requests-ntlm
        label 'jenkins-agent-with-python'
    }
    parameters {
        choice(name: 'ACTION', choices: ['assign', 'remove'], description: 'Acción')
        string(name: 'TARGET_USER', defaultValue: 'DOMINIO\\usuario', description: 'Usuario destino')
        string(name: 'ITEM_PATH', defaultValue: '/PaginatedReports/MiReporte', description: 'Ruta en SSRS')
    }
    stages {
        stage('Preparar entorno Python') {
            steps {
                // Solo se ejecuta UNA VEZ en el nodo Jenkins (no en SSRS)
                sh '''
                    python3 -m venv ssrs-env
                    source ssrs-env/bin/activate
                    pip install requests requests-ntlm
                '''
            }
        }
        
        stage('Gestionar Rol Browser') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'ssrs-admin-creds',
                        usernameVariable: 'SSRS_USER',
                        passwordVariable: 'SSRS_PASS'
                    )
                ]) {
                    sh '''
                        source ssrs-env/bin/activate
                        python manage_ssrs_role.py \
                            --reports-url "http://ssrs-server/Reports" \
                            --username "${SSRS_USER}" \
                            --password "${SSRS_PASS}" \
                            --item-path "${ITEM_PATH}" \
                            --target-user "${TARGET_USER}" \
                            ${ACTION == "assign" ? "--assign" : ""}
                    '''
                }
            }
        }
    }
}
