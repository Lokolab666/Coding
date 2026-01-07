existing = None
reps = AdminConfig.list("ResourceEnvironmentProvider", scopeId)

if reps:
    for repId in reps.splitlines():
        if AdminConfig.showAttribute(repId, "name") == repName:
            existing = repId
            break

if existing:
    print "Already exists:", repName, "->", existing
else:
    # 3) Create it
    newRep = AdminConfig.create("ResourceEnvironmentProvider", scopeId, [["name", repName]])
    AdminConfig.save()
    print "Created:", repName, "->", newRep





foundRef = None
refs = AdminConfig.list('Referenceable', existing)

if refs:
    for refId in refs.splitlines():
        fcn = AdminConfig.showAttribute(refId, 'factoryClassname')
        cn  = AdminConfig.showAttribute(refId, 'classname')
        if fcn == fcnWanted and cn == cnWanted:
            foundRef = refId
            break

if foundRef:
    print "Referenceable already exists ->", foundRef
else:
    newref = AdminConfig.create('Referenceable', existing, rpAttr)
    print "Created Referenceable ->", newref
    AdminConfig.save()





# -------------------------
# Inputs
# -------------------------
cellName    = "YOUR_CELL"
clusterName = "View_logs"
repName     = "Logs"

reeName     = "hello"
jndiNameVal = "hello"     # change if you want something like "logs/hello" or "myjndi/hello"

# -------------------------
# 1) Resolve cluster scope
# -------------------------
scopeId = AdminConfig.getid("/Cell:%s/ServerCluster:%s/" % (cellName, clusterName))
if not scopeId:
    # fallback if cell is not needed in your environment
    scopeId = AdminConfig.getid("/ServerCluster:%s/" % clusterName)

if not scopeId:
    raise Exception("Cluster not found: %s (cell=%s)" % (clusterName, cellName))

print "Cluster scope ID:", scopeId

# -------------------------
# 2) Find the REP (Logs) under that cluster scope
# -------------------------
repId = None
reps = AdminConfig.list("ResourceEnvironmentProvider", scopeId)

if reps:
    for rid in reps.splitlines():
        if AdminConfig.showAttribute(rid, "name") == repName:
            repId = rid
            break

if not repId:
    raise Exception("ResourceEnvironmentProvider '%s' not found under cluster '%s'" % (repName, clusterName))

print "REP ID:", repId

# -------------------------
# 3) Check if REE already exists under that REP
# -------------------------
existingReeId = None
rees = AdminConfig.list("ResourceEnvEntry", repId)

if rees:
    for eid in rees.splitlines():
        if AdminConfig.showAttribute(eid, "name") == reeName:
            existingReeId = eid
            break

if existingReeId:
    print "ResourceEnvEntry already exists:", reeName, "->", existingReeId
else:
    # IBM-required attributes: name + jndiName :contentReference[oaicite:1]{index=1}
    attrs = [
        ['name', reeName],
        ['jndiName', jndiNameVal]
    ]

    newReeId = AdminConfig.create('ResourceEnvEntry', repId, attrs)
    print "Created ResourceEnvEntry:", newReeId

    AdminConfig.save()
    print "Saved."


