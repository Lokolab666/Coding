# -------------------------
# INPUTS
# -------------------------
cellName    = "YOUR_CELL"
clusterName = "View_logs"
repName     = "Logs"
reeName     = "testvalidationentry"

# Custom properties you want to ensure exist (name, value)
propsToApply = [
    ["propA", "valueA"],
    ["propB", "valueB"],
    ["hello", "world"]
]

# If True: update value when property exists
# If False: skip when property exists
updateIfExists = 1

defaultType     = "java.lang.String"
defaultRequired = "false"

# -------------------------
# 1) Resolve cluster scope
# -------------------------
scopeId = AdminConfig.getid("/Cell:%s/ServerCluster:%s/" % (cellName, clusterName))
if not scopeId:
    scopeId = AdminConfig.getid("/ServerCluster:%s/" % clusterName)

if not scopeId:
    raise Exception("Cluster not found: %s (cell=%s)" % (clusterName, cellName))

print "Cluster scope:", scopeId

# -------------------------
# 2) Find REP by name under cluster scope
# -------------------------
repId = None
reps = AdminConfig.list("ResourceEnvironmentProvider", scopeId)
if reps:
    for rid in reps.splitlines():
        if AdminConfig.showAttribute(rid, "name") == repName:
            repId = rid
            break

if not repId:
    raise Exception("REP not found under cluster scope: %s" % repName)

print "REP:", repId

# -------------------------
# 3) Find REE by name under REP
# -------------------------
reeId = None
rees = AdminConfig.list("ResourceEnvEntry", repId)
if rees:
    for eid in rees.splitlines():
        if AdminConfig.showAttribute(eid, "name") == reeName:
            reeId = eid
            break

if not reeId:
    raise Exception("REE not found under REP '%s': %s" % (repName, reeName))

print "REE:", reeId

# -------------------------
# 4) Get or create propertySet
# -------------------------
psId = AdminConfig.showAttribute(reeId, "propertySet")
if not psId:
    psId = AdminConfig.create("J2EEResourcePropertySet", reeId, [])
    # Link back (safe even if WAS auto-links)
    AdminConfig.modify(reeId, [["propertySet", psId]])

print "PropertySet:", psId

# -------------------------
# 5) Build existing properties map: name -> propertyId
# -------------------------
existingProps = {}
props = AdminConfig.list("J2EEResourceProperty", psId)
if props:
    for pid in props.splitlines():
        pn = AdminConfig.showAttribute(pid, "name")
        if pn:
            existingProps[pn] = pid

# -------------------------
# 6) Create (or update) without repeating names
# -------------------------
for p in propsToApply:
    pname = p[0]
    pval  = p[1]

    if existingProps.has_key(pname):
        pid = existingProps[pname]
        if updateIfExists:
            AdminConfig.modify(pid, [["value", pval]])
            print "Updated property:", pname, "->", pval
        else:
            print "Skipped (already exists):", pname
    else:
        attrs = [
            ["name", pname],
            ["type", defaultType],
            ["value", pval],
            ["required", defaultRequired]
        ]
        pid = AdminConfig.create("J2EEResourceProperty", psId, attrs)
        existingProps[pname] = pid
        print "Created property:", pname, "->", pval

AdminConfig.save()
print "Saved."
