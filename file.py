# Inputs
propNameToDelete = "testCustom"

# Get propertySet (create is NOT needed for delete)
psId = AdminConfig.showAttribute(reeId, "propertySet")
print "PropertySet:", psId

if (psId is None) or (str(psId).strip() == ""):
    print "No propertySet found. Nothing to delete."
else:
    # Find the property by name
    propId = None
    props = AdminConfig.list("J2EEResourceProperty", psId)
    if props:
        for pid in props.splitlines():
            if AdminConfig.showAttribute(pid, "name") == propNameToDelete:
                propId = pid
                break

    if propId:
        AdminConfig.remove(propId)
        AdminConfig.save()
        print "Deleted property:", propNameToDelete, "->", propId
    else:
        print "Property not found:", propNameToDelete
