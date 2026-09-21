---
label: CMDB Configuration
icon: note
---
# Request a new CMDB Configuration Item

Below are instructions on how to request a new Business Application Configuration Item (CI) in the Configuration Management Database (CMDB) within ServiceNow. A CI is required for deploying changes to production.

## Service Now Request
Start with a ServiceNow request - [it.medtronic.com](https://it.medtronic.com)

Search for [Business Application Configuration Item Request](https://medtronicprod.service-now.com/it?id=sc_cat_item&sys_id=bdf2c212dba92380f05f69c3ca961915)

### Application Form Fields

| Field Name | Description |
|---|---|
|Application Name|Enter the Application Name|
|Description of Business Application|Description|
|Which environments are you requesting be created?|Select all environments you will plan to create or have created |
|Will this Business Application be supported by Global IT for the Business IT?|IT|
|Do you need a new support group created for this application?| Yes or No if you don't have one |
|Support Group|Enter a support group specific to your application |
|What is the hosting method for this Business Application?|Public Cloud - PAAS|
|Application Vendor|Medtronic|
|Hosting Vendor|Medtronic|
|Cost Center|Enter the appropriate Cost Center|
|Business Sponsor|Enter the application-specific Business Sponsor|
|IT Manager|Enter the Technical application owner responsible for the technical aspects of the application|
|IT Lead|Enter the individual responsible for managing and maintaining the CI records for your application|


### Compliance Information

The answers to each of the required compliance questions are application-specific. The project manager and other team members may need to be consulted to answer these correctly.

## Define Relationships to other Configuration Items

After the Business Application CI is created, you will want to confirm the Business Application record exists, as well as Mapped Application Service records for each application environment you have.

Example:
![](../static/troubleshooting/servicenow-cmdb-business-ci.png)

You will then need to go back to the CI to add any application-specific dependencies for each environment. Some common dependencies to add include:
* Argo cluster(s) - Dev, Prod / Quarantine
    * Argo - Prod
    * Argo - Dev
    * Argo - Quarantine
* Authentication Providers:
  * Internal MDT Users:
    * CIs:
      * Azure AD - STAGE
      * Azure AD - PROD
  * External Users:
    * CIs (depending on what CIAM environments are configured for your application):
      * CIAM - Platform - DEV
      * CIAM - Platform - TEST
      * CIAM - Platform - STAGE
      * CIAM - Platform - PROD
* Database
    * On-prem WEB12 instances (CI Class is Oracle Instance):
      * CIs (depending on which DB instance is used by this application environment):
        * web12@mspldb305
        * web12s@mspldb515
        * web12t@mspldb291
        * web12r@mspldb291
* Restricted Enterprise Directory View (CI Class is Oracle Catalog)
    * CIs:
      * WS_RESTRICTED_ENT_DIR_VW
    * __Note:__ use of the RESTRICTED_ENT_DIR_VW should be replaced wherever possible with headers that are passed to the application. This is a built-in feature of the Argo pipeline. See []

==- How to Create a New Relationship between CIs
1. On each Mapped Application Service Record for your application (e.g. "CIAM - Platform - PROD"), click the Plus sign under Related Items to add a new CI relationship.
2. Once in the Related Items area, click on "Runs on (parent)..." from the suggested relationship types.
3. Change the filter to search for the Argo cluster that hosts this application environment (e.g. "Argo - Prod", "Argo - Dev"). The easiest way to filter is to remove all default filters except for one. Then change the filter to "Name" and search for "Argo -" to find the records.
    ![](../static/troubleshooting/servicenow-mas-filter-example.png)
4. Use the checkbox next to the results in the Configuration Items table to select it.
5. Use the Plus sign on the Relationships table to add the relationship.
    ![](../static/troubleshooting/servicenow-mas-relationship-add.png)
6. Scroll to the bottom of the screen and select "Save and Exit" to save the change.
7. You should then see a relationship that looks like the following, showing that this application environment Runs on Argo - Prod:
    ![](../static/troubleshooting/servicenow-cmdb-runs-on-saved.png)
===
