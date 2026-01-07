
def listPropertiesByName(propertySetId):
    # Return dict: propName -> propId
    result = {}
    props = AdminConfig.list("J2EEResourceProperty", propertySetId)
    if props:
        for pid in props.splitlines():
            n = AdminConfig.showAttribute(pid, "name")
            if n:
                result[n] = pid
    return result



# -------------------------
# Inputs
# -------------------------
cellName    = "YOUR_CELL"
clusterName = "View_logs"
repName     = "Logs"
reeName     = "testvalidationentry"

# Properties you want to ensure exist (name -> value)
propsToApply = {
    "propA": "valueA",
    "propB": "valueB",
    "hello": "world"
}

defaultType     = "java.lang.String"
defaultRequired = "false"   # keep as string for wsadmin

# -------------------------
# Helpers
# -------------------------
def getClusterScopeId(cell, cluster):
    sid = AdminConfig.getid("/Cell:%s/ServerCluster:%s/" % (cell, cluster))
    if not sid:
        sid = AdminConfig.getid("/ServerCluster:%s/" % cluster)
    return sid

def findRepId(scopeId, repName):
    reps = AdminConfig.list("ResourceEnvironmentProvider", scopeId)
    if reps:
        for rid in reps.splitlines():
            if AdminConfig.showAttribute(rid, "name") == repName:
                return rid
    return None

def findReeId(repId, reeName):
    rees = AdminConfig.list("ResourceEnvEntry", repId)
    if rees:
        for eid in rees.splitlines():
            if AdminConfig.showAttribute(eid, "name") == reeName:
                return eid
    return None

def ensurePropertySet(reeId):
    ps = AdminConfig.showAttribute(reeId, "propertySet")
    if ps:
        return ps
    # Create a property set under the REE
    ps = AdminConfig.create("J2EEResourcePropertySet", reeId, [])
    # Link it back to the REE (some environments already link automatically; this is safe)
    AdminConfig.modify(reeId, [["propertySet", ps]])
    return ps

def listPropertiesByName(propertySetId):
    """Return dict: propName -> propId"""
    result = {}
    props = AdminConfig.list("J2EEResourceProperty", propertySetId)
    if props:
        for pid in props.splitlines():
            n = AdminConfig.showAttribute(pid, "name")
            if n:
                result[n] = pid
    return result

def upsertProperty(propertySetId, existingMap, name, value,
                   ptype=defaultType, required=defaultRequired):
    if name in existingMap:
        pid = existingMap[name]
        # Update value (no duplicates)
        AdminConfig.modify(pid, [["value", value]])
        print "Updated property:", name, "->", value
        return pid
    else:
        attrs = [
            ["name", name],
            ["type", ptype],
            ["value", value],
            ["required", required]
        ]
        pid = AdminConfig.create("J2EEResourceProperty", propertySetId, attrs)
        print "Created property:", name, "->", value
        return pid

# -------------------------
# Main
# -------------------------
scopeId = getClusterScopeId(cellName, clusterName)
if not scopeId:
    raise Exception("Cluster not found: %s (cell=%s)" % (clusterName, cellName))

repId = findRepId(scopeId, repName)
if not repId:
    raise Exception("REP not found in cluster scope: %s" % repName)

reeId = findReeId(repId, reeName)
if not reeId:
    raise Exception("REE not found under REP '%s': %s" % (repName, reeName))

print "REE ID:", reeId

psId = ensurePropertySet(reeId)
print "PropertySet ID:", psId

existingProps = listPropertiesByName(psId)

for k in propsToApply.keys():
    upsertProperty(psId, existingProps, k, propsToApply[k])
    # refresh map so the next props won't duplicate
    existingProps = listPropertiesByName(psId)

AdminConfig.save()
print "Saved."

