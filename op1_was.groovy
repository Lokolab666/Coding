variables:
  RUN_AIKIDO_SCAN: "true"
  AIKIDO_LOCAL_SCANNER_TOKEN: $AIKIDO_LOCAL_SCANNER_TOKEN
  TRIVY_DB_REPOSITORY: "ghcr.io/aquasecurity/trivy-db"

default:
  timeout: 3h

before_script:
  - echo "🔍 Checking access to ghcr.io"
  - curl -v https://ghcr.io 2>&1 | grep "HTTP/2 200" || (echo "❌ ghcr.io unreachable" && exit 1)

include:
  - project: 'CST_Aible_Hub/converged-planning/cp-cicd-tools'
    ref: $AIKIDO_TEMPLATE_REF
    file: '/templates/aikido-scan.yml'

stages:
  - code-check
  - docker-image-build
  - monitor



before_script:
  - curl -v https://ghcr.io 2>&1 | grep "HTTP/2 200" || (echo "❌ ghcr.io unreachable" && exit 1)




  ## 📊 Diagrama de Flujo Consolidado (7 Pasos Principales)

```mermaid
flowchart TD
    A[Inicio: Job Jenkins<br>con número de factura] --> B[Preparar entorno Python]
    
    subgraph B [Paso 1: Preparar entorno Python]
        B1[Crear venv + instalar oracledb<br>sin Oracle Client]
    end
    
    B --> C[Cargar queries SQL]
    
    subgraph C [Paso 2: Cargar queries SQL]
        C1[Leer archivos .sql limpios<br>(solo SELECT puro)]
    end
    
    C --> D[Ejecutar en Oracle]
    
    subgraph D [Paso 3: Ejecutar en Oracle]
        D1[Conectar en modo thin +<br>inyectar :factura como bind variable]
        D2{¿Éxito?}
        D2 -- No --> D3[Error: conexión/credenciales/sintaxis]
        D2 -- Sí --> D4[Obtener resultados + metadata]
    end
    
    D4 --> E[Procesar y validar]
    
    subgraph E [Paso 4: Procesar y validar]
        E1[Formatear valores:<br>NULL→'', fecha→dd/mm/yyyy]
        E2[Validar formato fecha<br>en campos FEC_/FECHA_*]
        E3[Unir con separador ' / ']
    end
    
    E3 --> F{¿Errores<br>de validación?}
    
    F -- Sí --> G[Registrar errores<br>y marcar fallido]
    F -- No --> H[Generar TXT]
    
    subgraph H [Paso 5: Generar TXT]
        H1[Escribir header + datos<br>en output/query_factura.txt]
    end
    
    G --> I[Reporte final]
    H1 --> I
    
    subgraph I [Paso 6: Reporte final]
        I1[Resumen: registros procesados,<br>queries ejecutadas, errores]
    end
    
    I --> J{¿Todas OK?}
    
    J -- No --> K[Paso 7: Job FALLIDO<br>+ logs detallados]
    J -- Sí --> L[Paso 7: Job ÉXITO<br>+ archivar artefactos TXT]
    
    K --> M[Fin del proceso]
    L --> M
    
    %% Estilos
    classDef startend fill:#27ae60,stroke:#219653,color:white
    classDef process fill:#3498db,stroke:#2980b9,color:white
    classDef decision fill:#e74c3c,stroke:#c0392b,color:white
    classDef error fill:#e67e22,stroke:#d35400,color:white
    classDef success fill:#2ecc71,stroke:#27ae60,color:white
    
    class A,M startend
    class B1,C1,D1,D4,E1,E2,E3,H1,I1 process
    class D2,F,J decision
    class D3,G,K error
    class L success
```

---

## 📋 Tabla Resumida de Pasos Consolidados

| Paso | Nombre | Subpasos Consolidados | Descripción | Componente | Salida/Artefacto |
|------|--------|------------------------|-------------|------------|------------------|
| **1** | **Preparar entorno Python** | • Crear entorno virtual (`python -m venv`)<br>• Instalar `oracledb` vía pip | Aísla dependencias sin requerir instalación de Oracle Client. Driver funciona en modo *thin* (sin DLLs externas). | Python + pip (Windows CMD) | Entorno virtual listo con `oracledb` instalado |
| **2** | **Cargar queries SQL** | • Leer `datos_generales.sql`<br>• Leer `detalle_emision_hogar.sql`<br>• Leer `detalle_emision_oc.sql` | Carga archivos SQL limpios (solo `SELECT` puro, sin comandos `SET/SPOOL/COLSEP` de SQL*Plus). | Python (`open()`) | Strings con queries parametrizadas con `:factura` |
| **3** | **Ejecutar en Oracle** | • Conectar con `oracledb.connect(user, pass, dsn)`<br>• Inyectar bind variable `:factura`<br>• Ejecutar `cursor.execute()`<br>• Obtener metadata (`cursor.description`) | Conexión directa a Oracle sin cliente nativo. Uso de bind variables para seguridad (evita SQL injection). Manejo de errores de conexión/credenciales/sintaxis. | `oracledb` (modo thin) + Oracle DB | Cursor con resultados + nombres de columnas |
| **4** | **Procesar y validar** | • Formatear valores (`None→''`, `datetime→dd/mm/yyyy`)<br>• Validar regex `\d{2}/\d{2}/\d{4}` en campos FEC_/FECHA_*<br>• Unir valores con separador ` / ` | Transformación de datos crudos a formato requerido por negocio. Validación automática de formato de fechas. Detección temprana de errores de calidad. | Python (regex + datetime) | Lista de filas formateadas + lista de errores |
| **5** | **Generar TXT** | • Escribir header (nombres columnas)<br>• Escribir datos (una fila por línea)<br>• Guardar en `output/query_factura.txt` | Generación de archivo plano con formato exacto solicitado: separador ` / ` y fechas `dd/mm/yyyy`. Nomenclatura autoexplicativa para trazabilidad. | Python (`open('w', encoding='utf-8')`) | Archivos TXT listos para adjuntar a ticket |
| **6** | **Reporte final** | • Contar registros por query<br>• Consolidar errores encontrados<br>• Generar resumen ejecutivo | Log estructurado para auditoría: qué queries se ejecutaron, cuántos registros, qué validaciones fallaron. Base para decisión de éxito/fallo del job. | Python (logging) | `execution_log_YYYYMMDD_HHMMSS.txt` |
| **7** | **Decisión final** | • Si hay errores → Job FALLIDO (exit 1)<br>• Si sin errores → Job ÉXITO (exit 0) + archivar artefactos | Integración con Jenkins: estado del job determina si el proceso se considera exitoso. Artefactos archivados permiten descarga manual por el equipo de soporte. | Jenkins Pipeline | Job completado con estado claro + artefactos accesibles |

---

## 🔑 Decisiones Clave del Flujo

| Decisión | Condición | Acción |
|----------|-----------|--------|
| **¿Conexión exitosa?** | Timeout, credenciales inválidas, sintaxis SQL errónea | Abortar job con mensaje específico del error |
| **¿Query devuelve filas?** | `rowcount == 0` | Advertencia (no error fatal) → continuar proceso |
| **¿Formato fecha válido?** | Campo FEC_/FECHA_* no coincide con regex `dd/mm/yyyy` | Registrar error + marcar job como FALLIDO |
| **¿Todas las queries OK?** | `len(errores_totales) > 0` | Job FALLIDO (exit 1) con logs detallados |

---

## 📁 Estructura Final del Proyecto

```
pims-automation/
├── queries/
│   ├── datos_generales.sql          ← Solo SELECT puro + :factura
│   ├── detalle_emision_hogar.sql
│   └── detalle_emision_oc.sql
├── scripts/
│   └── ejecutar_queries.py          ← Script Python consolidado (7 pasos)
├── requirements.txt                 ← oracledb>=1.4.0
└── Jenkinsfile                      ← Pipeline con 7 etapas
```

---

## ✅ Ventajas de la Consolidación

| Antes | Después | Beneficio |
|-------|---------|-----------|
| 15+ pasos detallados | **7 pasos lógicos** | Diagrama más claro para stakeholders no técnicos |
| Subpasos fragmentados | **Flujo continuo** | Fácil de implementar en Jenkinsfile (1 stage por paso) |
| Validación dispersa | **Paso 4 centralizado** | Todas las reglas de negocio en un solo lugar |
| Generación de archivos separada | **Paso 5 unificado** | Menos I/O innecesario, mejor rendimiento |

¿Necesitas que te genere el **Jenkinsfile con 7 stages** (uno por paso) o el **script Python consolidado** listo para ejecutar la PoC?



  Met with the customer (Binu) today and stayed after the meeting to review the reported issue. We confirmed the correct database host configuration for the application and validated database connectivity from the application pod. The connection test using pgcli was successful with the following command:
export HOME=/tmp && pgcli postgresql://app_update@ami-mde-db-mde-prd-cluster-rw.ami-mde-db.svc.cluster.local:5432/app
