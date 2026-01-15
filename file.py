# update_war.py  (run with: wsadmin -lang jython -f update_war.py MyApp /path/to/app.war true)

import sys

appName  = sys.argv[0]
warPath  = sys.argv[1]
deployWS = (len(sys.argv) > 2 and sys.argv[2].lower() == "true")

opts = ['-operation', 'update', '-contents', warPath]

# Deploy web services during update (mainly relevant for JAX-RPC style web services)
if deployWS:
    opts += ['-deployws']   # option applies to update/updateInteractive too

print("Updating app %s from %s" % (appName, warPath))
print(AdminApp.update(appName, 'app', opts))

print("Saving configuration...")
AdminConfig.save()

# ND tip: push config to nodes (safe to call in ND; may not exist in Base)
try:
    AdminNodeManagement.syncActiveNodes()
except:
    pass

print("Done.")





import sys

# -------------------------
# Args
# -------------------------
if len(sys.argv) < 3:
    print("Usage: manage_app_noclass.py <appName> <nodeName> <serverName>")
    sys.exit(1)

appName   = sys.argv[0]
nodeName  = sys.argv[1]
serverName= sys.argv[2]

print("App: %s | Node: %s | Server: %s" % (appName, nodeName, serverName))

# -------------------------
# Validate app exists
# -------------------------
installedApps = AdminApp.list().splitlines()
if appName not in installedApps:
    raise Exception("Application not found: %s\nInstalled apps: %s" % (appName, ", ".join(installedApps)))

# -------------------------
# List modules
# -------------------------
print("\n=== Modules (AdminApp.listModules) ===")
try:
    # '-server' shows extra markers and server mapping style output
    print(AdminApp.listModules(appName, '-server'))
except:
    # Fallback
    print(AdminApp.listModules(appName))

# -------------------------
# Set APPLICATION classloader mode to PARENT_LAST
# Path: Deployment -> deployedObject -> classloader -> mode
# -------------------------
print("\n=== Setting application classloader to PARENT_LAST ===")
depId = AdminConfig.getid("/Deployment:%s/" % appName)
if not depId:
    raise Exception("Could not find Deployment config for app: %s" % appName)

deployedObjectId = AdminConfig.showAttribute(depId, "deployedObject")
if not deployedObjectId:
    raise Exception("Could not resolve deployedObject for app: %s" % appName)

classloaderId = AdminConfig.showAttribute(deployedObjectId, "classloader")
if not classloaderId:
    raise Exception("Could not resolve classloader for app: %s" % appName)

print("Current classloader:\n%s" % AdminConfig.showall(classloaderId))
AdminConfig.modify(classloaderId, [["mode", "PARENT_LAST"]])
print("Updated classloader:\n%s" % AdminConfig.showall(classloaderId))

# -------------------------
# Set WAR MODULE(S) classloaderMode to PARENT_LAST (WebModuleDeployment)
# This is the "Manage modules" / per-web-module classloader setting
# -------------------------
print("\n=== Setting WAR module classloaderMode to PARENT_LAST (if any) ===")

modulesAttr = AdminConfig.showAttribute(deployedObjectId, "modules")

# wsadmin sometimes returns either:
#   [obj1 obj2 obj3]
# or a whitespace-separated string; normalize it
modulesList = []
if modulesAttr:
    s = str(modulesAttr).strip()
    if s.startswith("[") and s.endswith("]"):
        s = s[1:-1].strip()
    if s:
        modulesList = s.split()

changedCount = 0

for m in modulesList:
    # Only touch web modules
    if "WebModuleDeployment" in m:
        currentMode = AdminConfig.showAttribute(m, "classloaderMode")
        if currentMode != "PARENT_LAST":
            AdminConfig.modify(m, [["classloaderMode", "PARENT_LAST"]])
            changedCount += 1
            print("Updated WebModuleDeployment to PARENT_LAST: %s" % m)
        else:
            print("Already PARENT_LAST: %s" % m)

print("WAR modules changed: %d" % changedCount)

# -------------------------
# Save + (optional) sync nodes (ND)
# -------------------------
print("\n=== Saving configuration ===")
AdminConfig.save()

print("\n=== Sync active nodes (ND only; safe to attempt) ===")
try:
    AdminNodeManagement.syncActiveNodes()
    print("Node sync requested.")
except:
    print("Node sync not available or not needed (probably WAS Base).")

# -------------------------
# Start the application on a specific server
# Requires the target server JVM to be running.
# Uses ApplicationManager MBean in that server process.
# -------------------------
print("\n=== Starting application ===")
query = "type=ApplicationManager,node=%s,process=%s,*" % (nodeName, serverName)
appManager = AdminControl.queryNames(query)

if not appManager:
    raise Exception(
        "Could not find ApplicationManager MBean using query:\n%s\n"
        "Is server %s/%s running?" % (query, nodeName, serverName)
    )

AdminControl.invoke(appManager, "startApplication", appName)
print("Start invoked for application: %s" % appName)

print("\nDone.")

