Acceso a la base de datos
¿Existe acceso programático a las bases de datos de PIMS (ej.: mediante JDBC/ODBC, cliente SQL CLI, o API REST)? ¿O solo es posible mediante interfaces gráficas manuales (como SQL Developer)?
Motor de base de datos
¿Qué motor utiliza PIMS? (ej.: Oracle, SQL Server, PostgreSQL). Esto define cómo ejecutar las queries y manejar tipos de datos (fechas, formatos numéricos).
Origen del número de carga/factura
¿El valor usado en IR.GPIR_NUMBER IN (...) se extrae directamente de un campo del ticket de ServiceNow? Si es así, ¿cuál es el nombre del campo y es accesible mediante API?
Almacenamiento y versión de las queries
¿Los archivos SQL (Datosgenerales.sql, DetalleEmisionHogar.sql, etc.) están en un repositorio accesible (Git, red compartida) o deben mantenerse dentro del script de automatización? ¿Requieren ajustes dinámicos (ej.: inyección del número de factura)?
Formato de salida esperado
¿En qué formato debe entregarse el resultado al equipo de soporte? (ej.: CSV con separador /, Excel con hojas específicas). ¿Debe incluir transformaciones como formato de fecha dd/mm/yyyy hh24:mi?
Validación automática de datos
Además del formato de fecha y uso de / como separador, ¿existen otras reglas de validación estructurada? (ej.: rangos de valores, campos obligatorios, cruce con otras fuentes). ¿Hay scripts existentes que ya realicen estas validaciones?
Escalado a "región"
¿La derivación a región es 100% manual hoy? ¿O existe algún mecanismo (ej.: cola de mensajes, API interna, bandeja compartida) que permita automatizar el envío de la query configurada y recibir el resultado?
Entorno de pruebas
¿Existe un entorno de PIMS (no productivo) donde se puedan ejecutar queries de prueba con datos sintéticos para validar la automatización sin riesgo?
Herramientas y lenguajes disponibles
¿Qué tecnologías están permitidas o ya se usan en tu organización para automatizaciones similares? (ej.: Python con cx_Oracle/pyodbc, PowerShell, RPA como UiPath/ Automation Anywhere).
Seguridad y permisos
¿Qué credenciales o roles son necesarios para ejecutar estas queries? ¿Deben gestionarse mediante vaults (ej.: HashiCorp Vault) o variables de entorno seguras?
