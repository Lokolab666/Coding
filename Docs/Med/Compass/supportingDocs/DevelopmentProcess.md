# Software Development Process

### Grails Development
  * JSESSION_ID
  * JNDI naming
  * Error page mapping
  * Default icon for site (Medtronic as default added to base app?)

### HTML/CSS Visual Design Templates
  * HTML/CSS Visual Design Templates are located within the [Base Project](https://git.web.medtronic.com/gitWebDevelopment/base24xgrailsapp/tree/develop/web-static).
  * Use GDSM for corner cases that are not covered in the design templates.

### Database
  * Process to promote
  * How to write scripts and naming conventions for database objects - [Database Scripting](DatabaseScripting.md)
  * TODO: Kelli wrote a utility that would look at the schema and generate scripts

### Build/deploy
  * Use Jenkins WACD jobs for [build](https://cm2.medtronic.com:8080/job/BCP_Web/job/WACD/job/WACD2.0_build_app/build) and [deploy](https://cm2.medtronic.com:8080/job/BCP_Web/job/WACD/job/WACD2.0_deploy/build).

### Contrast Protect and Assess
  * Contrast Assess is used in the test and stage environments for vulnerability assessment and reporting.
  * Contrast Protect is used in the production environment for runtime application self-protection (RASP).
  * Contrast may be accessed from here: [https://app.contrastsecurity.com/Contrast/](https://app.contrastsecurity.com/Contrast/). Use your Medtronic email address to start the sign on process.

### Code Review
  * Use this Jenkins job to create the Code Review evidence:  [http://jenkins.web.medtronic.com:8080/job/CodeReviewGenerator/](http://jenkins.web.medtronic.com:8080/job/CodeReviewGenerator/)
  * Follow the MSCM guidelines for loading this document into [D2](https://mrcsd2.medtronic.com/D2/) and routing for approval
  * If the document in D2 is created in the wrong directory, use the following form to request the document to be moved. Frequently, if an error occurs during creation of a document, the document is located in the MRCS/Temp directory.  [https://medtronicprod.service-now.com/it/?id=mdtit_sc_cat_item&sys_id=8d5697e81bb09c5033ef99f91d4bcb31](https://medtronicprod.service-now.com/it/?id=mdtit_sc_cat_item&sys_id=8d5697e81bb09c5033ef99f91d4bcb31)

### Release Preparation
  * Follow the GitLab training, specifically the [Medtronic GitFlow overview](https://mrcsd2.medtronic.com/d2anonserv/getcontentbynamestatus?docbase_name=mrcs&auth=crts_wmuser&format=c2pdf&name=30049547&status=Effective), to prepare the release candidate with a release branch.
  * Remove ```-SNAPSHOT``` from the ```BUILD_RELEASE``` value of the ```build-version.properties``` file within that newly created release branch. This file is located at the root of the source code.
  * Use Jenkins to build and deploy the release candidate to the test/stage environment.
  * Use the [RFC request form](http://sitebuilder2/is/WebSolutions/developer/Lists/Database%20Schema%20Code%20Promotion/Open%20Items.aspx) to request a new RFC/CHG Change Request from our Release Manager.
  * The RFC request form will require a Test Summary to be attached. This Test Summary may be obtained from the testing team member on your project.

### Contacts and Assignment Groups
  * [Contacts and Assignment Groups](contacts.md)


### Password management (CyberArk)
  * [Use CyberArk](docs/authentication-and-authorization/cyberark-process.md) for saving and retrieving database passwords, service account passwords, and other secrets.

### edata directory

### web-static directory

### Log Viewer
  * [Log Viewer - On-prem Weblogic Applications](http://wwwpi.corp.medtronic.com/filer/showFiles.html)
  * [Argo dev](https://argo-obs-dev.medtronic.com/_dashboards)
  * [Argo prod](tbd)
  * [Argo quarantine](tbd)
