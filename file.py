# -------------------------
# INPUTS
# -------------------------
cellName    = "YOUR_CELL"
clusterName = "View_logs"
repName     = "Logs"
reeName     = "testvalidationentry"

# Where to write the backup file
# Jenkins tip: use env var WORKSPACE and build the path from it if you want.
outDir = "/tmp"

# Optional: mask sensitive values (1=yes, 0=no)
maskSensitive = 1
sensitiveKeys = ["password", "passwd", "secret", "token", "apikey", "apiKey", "key"]

# -------------------------
# 1) Resolve cluster scope
# -------------------------
scopeId = AdminConfig.getid("/Cell:%s/ServerCluster:%s/" % (cellName, clusterName))
if not scopeId:
    scopeId = AdminConfig.getid("/ServerCluster:%s/" % clusterName)
if not scopeId:
    raise Exception("Cluster not found: %s (cell=%s)" % (clusterName, cellName))

# -------------------------
# 2) Find REP
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

# -------------------------
# 3) Find REE
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

# -------------------------
# 4) Build file name with timestamp
# -------------------------
from java.text import SimpleDateFormat
from java.util import Date
ts = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())

fileName = "backup_REE_%s_%s_%s_%s_%s.txt" % (cellName, clusterName, repName, reeName, ts)

# Ensure dir ends without trailing slash issues
if outDir.endswith("/"):
    outPath = outDir + fileName
else:
    outPath = outDir + "/" + fileName

# -------------------------
# 5) Read REE attributes
# -------------------------
ree_jndi = AdminConfig.showAttribute(reeId, "jndiName")
ree_desc = AdminConfig.showAttribute(reeId, "description")
ree_cat  = AdminConfig.showAttribute(reeId, "category")  # may be empty in some versions

# -------------------------
# 6) Read custom properties
# -------------------------
psId = AdminConfig.showAttribute(reeId, "propertySet")

propLines = []
if psId and str(psId).strip() != "":
    props = AdminConfig.list("J2EEResourceProperty", psId)
    if props:
        for pid in props.splitlines():
            pn = AdminConfig.showAttribute(pid, "name")
            pt = AdminConfig.showAttribute(pid, "type")
            pv = AdminConfig.showAttribute(pid, "value")
            pr = AdminConfig.showAttribute(pid, "required")

            # Optional masking
            if maskSensitive and pn:
                lower = pn.lower()
                for sk in sensitiveKeys:
                    if sk.lower() in lower:
                        pv = "****"
                        break

            propLines.append("  - name=%s | type=%s | required=%s | value=%s" % (pn, pt, pr, pv))

# Sort for stable output
propLines.sort()

# -------------------------
# 7) Write TXT file
# -------------------------
from java.io import FileWriter, BufferedWriter, PrintWriter

pw = PrintWriter(BufferedWriter(FileWriter(outPath)))

pw.println("=== WebSphere REE Backup ===")
pw.println("timestamp=%s" % ts)
pw.println("cell=%s" % cellName)
pw.println("cluster=%s" % clusterName)
pw.println("repName=%s" % repName)
pw.println("reeName=%s" % reeName)
pw.println("")
pw.println("REE_ID=%s" % reeId)
pw.println("REE_jndiName=%s" % ree_jndi)
pw.println("REE_description=%s" % ree_desc)
pw.println("REE_category=%s" % ree_cat)
pw.println("")
pw.println("PropertySet_ID=%s" % psId)
pw.println("CustomProperties_Count=%d" % len(propLines))
pw.println("CustomProperties:")
if len(propLines) == 0:
    pw.println("  (none)")
else:
    for line in propLines:
        pw.println(line)

pw.close()

print "Backup written to:", outPath
