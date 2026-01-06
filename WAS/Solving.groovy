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






clusterId = AdminConfig.getid("/Cell:%s/ServerCluster:%s/" % (cellName, clusterName))
members = AdminConfig.showAttribute(clusterId, "members")[1:-1].split()

nodeNames = {}
for m in members:
    node = AdminConfig.showAttribute(m, "nodeName")
    srv  = AdminConfig.showAttribute(m, "memberName")
    nodeNames[node] = 1
    print "%s/%s" % (node, srv)

print "Nodes in cluster:", nodeNames.keys()



