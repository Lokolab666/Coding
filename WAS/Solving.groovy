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




# ---- inputs ----
cellName    = "MYCELL"
clusterName = "MYCLUSTER"
repName     = "REP1"

# ---- load IBM AdminResources script library ----
# Location per IBM docs:
#   app_server_root/scriptLibraries/resources/
# Example Linux: /opt/IBM/WebSphere/AppServer/scriptLibraries/resources/AdminResources.py
# Example Windows: C:/IBM/WebSphere/AppServer/scriptLibraries/resources/AdminResources.py
execfile("/path/to/app_server_root/scriptLibraries/resources/AdminResources.py")

# ---- create at cluster scope ----
scope = "Cell=%s,Cluster=%s" % (cellName, clusterName)   # IBM-supported scope string
repId = AdminResources.createResourceEnvProviderAtScope(scope, repName, [])
print "Created REP:", repId

AdminConfig.save()
print "Saved."





cellName    = "MYCELL"
clusterName = "MYCLUSTER"
repName     = "REP1"

clusterScope = AdminConfig.getid("/Cell:%s/ServerCluster:%s/" % (cellName, clusterName))
if not clusterScope:
    clusterScope = AdminConfig.getid("/ServerCluster:%s/" % clusterName)
if not clusterScope:
    raise Exception("Cluster not found")

repAttrs = [ ["name", repName] ]   # required('ResourceEnvironmentProvider') => name :contentReference[oaicite:5]{index=5}
newrep = AdminConfig.create("ResourceEnvironmentProvider", clusterScope, repAttrs)
print newrep

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


