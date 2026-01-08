# Inputs
propName  = "testCustom"
propValue = "true"   # keep as string
updateIfExists = 1

# 1) Get or create PropertySet (do NOT AdminConfig.modify propertySet)
psId = AdminConfig.showAttribute(reeId, "propertySet")
if (psId is None) or (str(psId).strip() == ""):
    psId = AdminConfig.create("J2EEResourcePropertySet", reeId, [])
print "PropertySet:", psId

# 2) Check if property already exists (by name)
existingPropId = None
props = AdminConfig.list("J2EEResourceProperty", psId)
if props:
    for pid in props.splitlines():
        if AdminConfig.showAttribute(pid, "name") == propName:
            existingPropId = pid
            break

# 3) Create or update in a single line
if existingPropId:
    if updateIfExists:
        AdminConfig.modify(existingPropId, [["value", propValue]])
        print "Updated property:", propName, "=", propValue
    else:
        print "Property already exists, skipped:", propName
else:
    newPropId = AdminConfig.create(
        "J2EEResourceProperty",
        psId,
        [["name", propName], ["value", propValue]]
    )
    print "Created property:", propName, "=", propValue, "->", newPropId

AdminConfig.save()
print "Saved."
