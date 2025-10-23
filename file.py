# resources/was/rep_config.py
# Uso:
#   wsadmin -lang jython -f rep_config.py <propsFile> <cell> <node> <repName>

import sys

def wsadminToList(inlist):
    if inlist is None:
        return []
    s = str(inlist).strip()
    if s.startswith('[') and s.endswith(']'):
        s = s[1:-1]
    if len(s) == 0:
        return []
    return [x for x in s.split() if x]

def ensure_rep(cell, node, repName):
    nodeId = AdminConfig.getid('/Cell:%s/Node:%s/' % (cell, node))
    if not nodeId:
        print('[ERROR] Node no encontrado: %s/%s' % (cell, node)); sys.exit(1)
    repId = AdminConfig.getid('/Cell:%s/Node:%s/ResourceEnvironmentProvider:%s/' % (cell, node, repName))
    if repId:
        print('[INFO] REP ya existe: %s' % repId); return repId
    repId = AdminConfig.create('ResourceEnvironmentProvider', nodeId, [['name', repName]])
    print('[INFO] REP creado: %s' % repId)
    return repId

def ensure_prop_set(repId):
    propSet = AdminConfig.showAttribute(repId, 'propertySet')
    if not propSet:
        propSet = AdminConfig.create('J2EEResourcePropertySet', repId, [])
        print('[INFO] PropertySet creado: %s' % propSet)
    else:
        print('[INFO] PropertySet existente: %s' % propSet)
    return propSet

def find_prop(propSet, name):
    items = wsadminToList(AdminConfig.showAttribute(propSet, 'resourceProperties'))
    for pid in items:
        if name == AdminConfig.showAttribute(pid, 'name'):
            return pid
    return None

def ensure_property(propSet, name, value=None, description=None, required=None):
    pid = find_prop(propSet, name)
    if pid:
        mods = []
        if value is not None:       mods.append(['value', value])
        if description is not None: mods.append(['description', description])
        if required is not None:    mods.append(['required', required])
        if mods:
            AdminConfig.modify(pid, mods)
            print('[INFO] Propiedad actualizada: %s' % name)
        else:
            print('[INFO] Propiedad ya existía sin cambios: %s' % name)
        return pid
    attrs = [['name', name]]
    if value is not None:       attrs.append(['value', value])
    if description is not None: attrs.append(['description', description])
    if required is not None:    attrs.append(['required', required])
    pid = AdminConfig.create('J2EEResourceProperty', propSet, attrs)
    print('[INFO] Propiedad creada: %s (%s)' % (name, pid))
    return pid

def list_properties(propSet, title):
    print('=== %s ===' % title)
    items = wsadminToList(AdminConfig.showAttribute(propSet, 'resourceProperties'))
    for pid in items:
        n = AdminConfig.showAttribute(pid, 'name')
        v = AdminConfig.showAttribute(pid, 'value')
        d = AdminConfig.showAttribute(pid, 'description')
        r = AdminConfig.showAttribute(pid, 'required')
        print(' - name=%s | value=%s | description=%s | required=%s' % (n, v, d, r))

def sync_node(node):
    try:
        obj = AdminControl.completeObjectName('type=NodeSync,process=nodeagent,node=%s,*' % node)
        if obj:
            print('[INFO] Sincronizando node %s ...' % node)
            print(AdminControl.invoke(obj, 'sync'))
        else:
            print('[WARN] NodeSync MBean no encontrado (nodeagent no activo?): %s' % node)
    except:
        print('[WARN] Error al sincronizar node: %s' % node)

def parse_props_file(path):
    props = []
    f = open(path, 'r')
    for raw in f.readlines():
        line = raw.strip()
        if len(line) == 0 or line.startswith('#'): continue
        if '=' not in line:
            print('[WARN] Línea ignorada (sin "="): %s' % line); continue
        k, v = line.split('=', 1)
        props.append( (k.strip(), v.strip()) )
    f.close()
    return props

# --- main ---
if len(sys.argv) < 4:
    print('Uso: wsadmin -f rep_config.py <propsFile> <cell> <node> <repName>'); sys.exit(2)

propsFile = sys.argv[0]; cell = sys.argv[1]; node = sys.argv[2]; repName = sys.argv[3]
print('[INFO] Cell=%s Node=%s REP=%s props=%s' % (cell, node, repName, propsFile))

repId  = ensure_rep(cell, node, repName)
pSetId = ensure_prop_set(repId)

list_properties(pSetId, 'PROPERTIES (ANTES)')
for (k, v) in parse_props_file(propsFile):
    ensure_property(pSetId, k, v, None, None)

AdminConfig.save()
print('[INFO] Cambios guardados.')
sync_node(node)
list_properties(pSetId, 'PROPERTIES (DESPUÉS)')
