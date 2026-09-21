# Project/Application Setup

These are instructions for setting up a new project / web application

### Create GitLab source repository
  * Use [this Jenkins job](https://cm2.medtronic.com:8080/job/BCP_Web/job/WACD/view/All/job/CreateGitLabRepoV2/) to create a GitLab repository with the standard configuration in place.
  * Clone [base project](https://git.web.medtronic.com/gitWebDevelopment/base24xgrailsapp/tree/develop) as starting point.  Follow the README for the steps to use this.

### Jenkins setup
  * [Steps to setup](https://code.medtronic.com/bcp_web/common/Playbook/-/blob/develop/playbook/project-setup-for-build.md) a project for the Jenkins build process
  * TODO: projects.json. Does Release Manager take care of this as part of application insfrastructure setup?

### Database
  * NOTE: Contact Kevin Campbell for any new database needs.
  * [Request a new database schema](request-database-schema.md)
  * JDBC setup (connection pool / JNDI name)

### Application infrastructure setup
  * Submit a new application request to the release manager using the [New App Request Form](NewApplicationRequest.md). The [required training document](https://mrcsd2.medtronic.com/d2anonserv/getcontentbynamestatus?docbase_name=mrcs&auth=crts_wmuser&format=c2pdf&name=30195125&status=Effective) must be read and acknowledged prior to using this form.

### Request the creation of directory groups as needed.
  * For internal user groups, [request Enterprise Directory groups](RequestEnterpriseDirectoryGroup.md).
  * For external user groups, [request External Directory groups](RequestExternalDirectoryGroup.md).

### CMDB Configuration Item
  * Request a new [CMDB Configuration Item for a Business Application](request-cmdb-configuration-item.md) in Service Now

### Monitoring
TODO: Document how to request/setup Dynatrace synthetic monitoring
