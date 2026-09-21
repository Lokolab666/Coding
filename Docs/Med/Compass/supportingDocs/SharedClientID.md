# Shared Client ID Setup #

## External users ##
__Technology:__ Medtronic CIAM (Okta)

There are no shared Client IDs for external user authentication. Each application will require its own setup. See [External User Authentication](ExternalUserAuthentication.md) for instructions on requesting an application to be configured.

<hr>

## Internal Users ##
__Technology:__  Azure AD

| Name | Client ID| Attributes |
|---|---|---|
|MDT_WEBAPP_DIRKEY<br>(stage)|eff21357-eb1b-45ae-9d68-5a00c11c9086|mdtDirectoryKeyI|
|MDT_WEBAPP_DIRKEY<br>(prod)|de1122aa-25bf-4644-8cf0-9250f07e01f8|mdtDirectoryKeyI|
|MDT_WEBAPP_BASIC<br>(stage)|eff21357-eb1b-45ae-9d68-5a00c11c9086|openid|https://graph.microsoft.com/v1.0/me?$select=usageLocation,onPremisesSamAccountName,userPrincipalName,mail,displayName,givenName,surname,extension_cead0570ad5e46cf85961433abcd0dd9_mdtPersonStatus,extension_cead0570ad5e46cf85961433abcd0dd9_mdtDirectoryKeyI|
|MDT_WEBAPP_BASIC<br>(prod)|de1122aa-25bf-4644-8cf0-9250f07e01f8||https://graph.microsoft.com/v1.0/me?$select=usageLocation,onPremisesSamAccountName,userPrincipalName,mail,displayName,givenName,surname,extension_dd0e729ea66c48d882b651e1b6b2178b_mdtPersonStatus,extension_dd0e729ea66c48d882b651e1b6b2178b_mdtDirectoryKeyI|

[comment]: <> (|MDT_WEBAPP_GROUP<br>&#40;stage&#41;|58991d60-f3b5-4312-b9bd-b7a832baa2e2|openid|https://graph.microsoft.com/v1.0/me/memberof?$select=displayName|)

[comment]: <> (|MDT_WEBAPP_GROUP<br>&#40;prod&#41;|861e5b53-393d-46bc-b804-d4a1a1e14185||https://graph.microsoft.com/v1.0/me/memberof?$select=displayName&$top=999|)

[comment]: <> (|MDT_WEBAPP_ROLE|||Pending we will update later on this.|)

[comment]: <> (|MDT_WEBAPP_DIRKEYOUTLOOK<br>&#40;stage&#41;|0db2f0ec-8524-46ea-ae2b-6035f76e2072|openid email profile phone user.read calendars.read|https://graph.microsoft.com/v1.0/me?$select=extension_cead0570ad5e46cf85961433abcd0dd9_mdtDirectoryKeyI|)

[comment]: <> (|MDT_WEBAPP_DIRKEYOUTLOOK<br>&#40;prod&#41;|a8cf1742-8949-46ca-9ae7-92126ce46111||https://graph.microsoft.com/v1.0/me?$select=extension_dd0e729ea66c48d882b651e1b6b2178b_mdtDirectoryKeyI|)


Azure AD has only one non-production environment.

[comment]: <> (__Endpoints for non-production Azure AD:__)

[comment]: <> (* __Authorize endpoint:__ https://login.microsoftonline.com/0a29d274-1367-4a8f-99c5-90c3dc7d4043/oauth2/v2.0/authorize)

[comment]: <> (* __Token endpoint:__ https://login.microsoftonline.com/0a29d274-1367-4a8f-99c5-90c3dc7d4043/oauth2/v2.0/token)

[comment]: <> (* __User Info endpoint:__ This is unique URL for each app model and included them above. )

[comment]: <> (* __OIDC Metadata:__ https://login.microsoftonline.com/0a29d274-1367-4a8f-99c5-90c3dc7d4043/v2.0/.well-known/openid-configuration)

[comment]: <> (<hr>)

[comment]: <> (__Endpoints for production Azure AD:__)

[comment]: <> (* __Authorize endpoint:__ https://login.microsoftonline.com/d73a39db-6eda-495d-8000-7579f56d68b7/oauth2/v2.0/authorize)

[comment]: <> (* __Token endpoint:__ https://login.microsoftonline.com/d73a39db-6eda-495d-8000-7579f56d68b7/oauth2/v2.0/token)

[comment]: <> (* __User Info endpoint:__ This is unique URL for each app model and included them above. )

[comment]: <> (* __OIDC Metadata:__ https://login.microsoftonline.com/d73a39db-6eda-495d-8000-7579f56d68b7/v2.0/.well-known/openid-configuration)

<hr>

## Client Secrets ##

Client Secrets for the shared Client IDs can be found in CyberArk.  [This](../Security/cyberark-process.md) shows the process for adding items to CyberArk.

<hr>

## Azure AD Certificates ##

For unit testing locally, SSL certificates can be downloaded from the browser when visiting one of the Microsoft Graph URLs, such as this [example.](https://graph.microsoft.com/v1.0/me?%24select=extension_cead0570ad5e46cf85961433abcd0dd9_mdtDirectoryKeyI)

Download and import them into your JDK keystore following the methods described [here.](../References/mdt-ca-to-keystore.md)

<hr>

## Sample Attributes ##

__MDTEXT_WEBAPP_DIRKEY:__ Sample of user info endpoint call.

External Users:
```json
{
    "sub": "xxxxxxxxxx",
    "mdt_directorykey": "x200079124"
}
```

Internal User:
```json
{
    "sub": "xxxxxxxx",
    "mdt_directorykey": "00325324"
}
```

__MDTEXT_WEBAPP_BASIC:__
External User:
```json
{
    "sub": "xxxxxxxx",
    "name": "Krishn Dhanekula",
    "family_name": "Dhanekula",
    "given_name": "Krishn",
    "mdt_directorykey": "x200079124",
    "user_id": "dhanekulak_x200079124",
    "preferred_username": "dhanekula.chaitanya+1@gmail.com",
    "email": "dhanekula.chaitanya+1@gmail.com"
}
```
Internal User:
```json
{
    "sub": "xxxxxxxxxx",
    "name": "Krishna Dhanekula",
    "family_name": "Dhanekula",
    "given_name": "Krishna",
    "mdt_directorykey": "00325324",
    "user_id": "dhanek2",
    "preferred_username": "dhanek2",
    "email": "krishna.c.dhanekula@medtronic.com"
}
```

__MDTEXT_WEBAPP_GROUP:__
External User:
```json
{
    "sub": "xxxxxxxxxxxxxxxxxxxxxx",
    "name": "Krishn Dhanekula",
    "family_name": "Dhanekula",
    "given_name": "Krishn",
    "mdt_directorykey": "x200079124",
    "user_id": "dhanekulak_x200079124",
    "preferred_username": "DHANEKULA.CHAITANYA+1@GMAIL.COM",
    "email": "dhanekula.chaitanya+1@gmail.com",
    "groups": [
        "Challenging Test Group",
        "Test Group"
    ]
}
```
Internal User:
```json
{
    "sub": "xxxxxxxxxxxxxxxxxxx",
    "name": "Krishna Dhanekula",
    "family_name": "Dhanekula",
    "given_name": "Krishna",
    "mdt_directorykey": "00325324",
    "user_id": "dhanek2",
    "preferred_username": "dhanek2",
    "email": "krishna.c.dhanekula@medtronic.com",
    "groups": [
        "AEM-ADMIN",
        "AWS_864383036631_VR360-S3-RO",
        "ConnectedCareMobilitySupportuser",
        "CRHFSoftware_Sonarqube_Admin_GS",
        "Diabetes-DocuSign",
        "EBIZ-WEBFARM01-ADMINISTRATORS-GS",
		"gbs-twin-cities-gs-d"
    ]
}
```

__MDTEXT_WEBAPP_ROLE:__

External user:
```json
{
    "sub": "xxxxxxxxxxxxxxxxx",
    "name": "Krishn Dhanekula",
    "family_name": "Dhanekula",
    "given_name": "Krishn",
    "mdt_directorykey": "x200079124",
    "user_id": "dhanekulak_x200079124",
    "preferred_username": "dhanekula.chaitanya+1@gmail.com",
    "email": "dhanekula.chaitanya+1@gmail.com",
    "role": [
        "Clinical HCP"
    ]
}
```
Internal User:
```json
{
    "sub": "xxxxxxxxxxxxxxxxxxx",
    "name": "Krishna Dhanekula",
    "family_name": "Dhanekula",
    "given_name": "Krishna",
    "mdt_directorykey": "00325324",
    "user_id": "dhanek2",
    "preferred_username": "dhanek2",
    "email": "krishna.c.dhanekula@medtronic.com",
    "role": [
        "testcorpeDRS",
        "xomedVRC",
        "RapidST"
    ]
}
```

__MDT_WEBAPP_DIRKEY:__
```json
{
    "@odata.context": "https://graph.microsoft.com/v1.0/$metadata#users(extension_cead0570ad5e46cf85961433abcd0dd9_mdtDirectoryKeyI)/$entity",
    "extension_cead0570ad5e46cf85961433abcd0dd9_mdtDirectoryKeyI": "00232071"
}
```

__MDT_WEBAPP_GROUP:__
```json
{
    "@odata.context": "https://graph.microsoft.com/v1.0/$metadata#directoryObjects(displayName)",
    "value": [
        {
            "@odata.type": "#microsoft.graph.group",
            "displayName": "TestIDMGroup"
        },
        {
            "@odata.type": "#microsoft.graph.group",
            "displayName": "gbsMPXR-ACI-Korea-30"
        },
        {
            "@odata.type": "#microsoft.graph.group",
            "displayName": "RSAPOC"
        },
        {
            "@odata.typ...

```

__MDT_WEBAPP_BASIC:__
```json
{
    "@odata.context": "https://graph.microsoft.com/v1.0/$metadata#users(usageLocation,onPremisesSamAccountName,userPrincipalName,mail,displayName,givenName,surname,extension_cead0570ad5e46cf85961433abcd0dd9_mdtPersonStatus,extension_cead0570ad5e46cf85961433abcd0dd9_mdtDirectoryKeyI)/$entity",
    "usageLocation": "US",
    "onPremisesSamAccountName": "takura1",
    "userPrincipalName": "takura1@stgmedtronic.com",
    "mail": "takura1@medtronic.com",
    "displayName": "Takur, Ajith",
    "givenName": "Ajith Singh",
    "surname": "Takur",
    "extension_cead0570ad5e46cf85961433abcd0dd9_mdtDirectoryKeyI": "00232071",
    "extension_cead0570ad5e46cf85961433abcd0dd9_mdtPersonStatus": "A"
}
```
__MDT_WEBAPP_ROLE:__ Pending
