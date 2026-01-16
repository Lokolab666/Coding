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
if len(sys.argv) < 1:
    print("Usage: manage_app_noclass.py <appName>")
    sys.exit(1)

appName = sys.argv[0]
print("App:", appName)

# -------------------------
# Validate app exists
# -------------------------
installedApps = AdminApp.list().splitlines()
if appName not in installedApps:
    raise Exception(
        "Application not found: %s\nInstalled apps: %s"
        % (appName, ", ".join(installedApps))
    )

# -------------------------
# List modules
# -------------------------
print("\n=== Modules (AdminApp.listModules) ===")
try:
    print(AdminApp.listModules(appName, "-server"))
except:
    print(AdminApp.listModules(appName))

# -------------------------
# Set APPLICATION classloader mode to PARENT_LAST
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

print("Current classloader:")
print(AdminConfig.showall(classloaderId))

AdminConfig.modify(classloaderId, [["mode", "PARENT_LAST"]])

print("Updated classloader:")
print(AdminConfig.showall(classloaderId))

# -------------------------
# Set WAR MODULE(S) classloaderMode to PARENT_LAST
# -------------------------
print("\n=== Setting WAR module classloaderMode to PARENT_LAST (if any) ===")

modulesAttr = AdminConfig.showAttribute(deployedObjectId, "modules")

modulesList = []
if modulesAttr:
    s = str(modulesAttr).strip()
    if s.startswith("[") and s.endswith("]"):
        s = s[1:-1].strip()
    if s:
        modulesList = s.split()

changedCount = 0

for m in modulesList:
    if "WebModuleDeployment" in m:
        currentMode = AdminConfig.showAttribute(m, "classloaderMode")
        if currentMode != "PARENT_LAST":
            AdminConfig.modify(m, [["classloaderMode", "PARENT_LAST"]])
            changedCount += 1
            print("Updated WebModuleDeployment:", m)
        else:
            print("Already PARENT_LAST:", m)

print("WAR modules changed:", changedCount)

# -------------------------
# Save + Sync (ND-safe)
# -------------------------
print("\n=== Saving configuration ===")
AdminConfig.save()

print("\n=== Sync active nodes (ND only; safe to attempt) ===")
try:
    AdminNodeManagement.syncActiveNodes()
    print("Node sync requested.")
except:
    print("Node sync not available or not required.")

# -------------------------
# Start application on ALL running servers
# -------------------------
print("\n=== Starting application on all running JVMs ===")

appManagers = AdminControl.queryNames("type=ApplicationManager,*")

if not appManagers:
    raise Exception("No ApplicationManager MBeans found. Are servers running?")

for mgr in appManagers.splitlines():
    try:
        AdminControl.invoke(mgr, "startApplication", appName)
        print("Start invoked on:", mgr)
    except:
        print("Failed to start app on:", mgr)

print("\nDone.")







import sys

if len(sys.argv) < 2:
    print("Usage: export_app.py <appName> <outEarPath> [exportToLocal true|false]")
    sys.exit(1)

appName = sys.argv[0]
outEar  = sys.argv[1]
exportToLocal = (len(sys.argv) > 2 and sys.argv[2].lower() == "true")

# Validate app exists
apps = AdminApp.list().splitlines()
if appName not in apps:
    raise Exception("Application not found: %s\nInstalled apps: %s" % (appName, ", ".join(apps)))

# Export
if exportToLocal:
    print("Exporting (exportToLocal) %s -> %s" % (appName, outEar))
    result = AdminApp.export(appName, outEar, '[-exportToLocal]')
else:
    print("Exporting %s -> %s" % (appName, outEar))
    result = AdminApp.export(appName, outEar)

print(result)
print("Done.")

