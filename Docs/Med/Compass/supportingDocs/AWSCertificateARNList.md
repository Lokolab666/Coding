# List of ARN's for Certificates created in AWS

## Argo Non-Prod
|Hostname|AWS ARN|ALB Name|
|---|---|---|
|erdd.staging.argo-dev.eks.mdtcloud.io|arn:aws:acm:us-east-1:389242548790:certificate/eb03f221-2da2-420b-a4c1-961449bc5b70|webdev-nonprod|
|erg.staging.argo-dev.eks.mdtcloud.io|arn:aws:acm:us-east-1:389242548790:certificate/9d591acb-c4db-4b40-a0e2-ab4a1d868c02|webdev-nonprod|
|gfas.release.argo-dev.eks.mdtcloud.io|arn:aws:acm:us-east-1:389242548790:certificate/78f92eb2-0371-4641-a2e8-f677857f75b7|webdev-nonprod|
|manuals-staging.medtronic.com|arn:aws:acm:us-east-1:872019488961:certificate/dd441ef5-2de1-4c21-8383-6e1e46ab3baa|intnon001|
|manuals-testing.medtronic.com|arn:aws:acm:us-east-1:389242548790:certificate/7bdbc180-ece2-487d-ac26-b42d8da54a61|intnon001|
|registration.dev.argo-dev.eks.mdtcloud.io|arn:aws:acm:us-east-1:389242548790:certificate/ad23dfbc-f30b-4575-949e-c58da59d704c|webdev-nonprod|
|registration.testing.argo-dev.eks.mdtcloud.io<BR>registration.staging.argo-dev.eks.mdtcloud.io|arn:aws:acm:us-east-1:389242548790:certificate/e7b0b9ec-18cf-4c70-a907-fe9792fdb742|webdev-nonprod|
|toolmanagement.testing.argo-dev.eks.mdtcloud.io<BR>toolmanagement.staging.argo-dev.eks.mdtcloud.io|arn:aws:acm:us-east-1:389242548790:certificate/7de11834-5e40-497e-8db1-edcb85cecff2|webdev-nonprod|
|usermanager.staging.argo-dev.eks.mdtcloud.io|arn:aws:acm:us-east-1:389242548790:certificate/3bb0a22a-eb1c-4167-834d-25f82d237f05|webdev-nonprod|
|usermanager.testing.argo-dev.eks.mdtcloud.io|arn:aws:acm:us-east-1:389242548790:certificate/cd5f8c61-a704-475a-8cab-717b28908148|webdev-nonprod|
|worldwiderevenue.testing.argo-dev.eks.mdtcloud.io|arn:aws:acm:us-east-1:389242548790:certificate/e2e0e301-8a85-4aa3-a06b-539fa7804abe|webdev-nonprod|


### How to request a new ARN for Argo Environment ###
Certificate ARNs for the default hostnames will be automatically created during namespace creation if one does not already exist.

For custom domain names: see Argo playbook's Custom Hostname page. Include URLs for each environment needed by the application.

**Short description:** Create AWS certificates in Argo Non-Prod for [AppName]

**Description:**
Please create the AWS SSL certificate for the following hostnames and send the ARNs.

* AppName.testing.argo-dev.eks.mdtcloud.io
* AppName.staging.argo-dev.eks.mdtcloud.io
* AppName.staging.argo-quarantine-prd.eks.mdtcloud.io
* AppName.production.argo-production-prd.eks.mdtcloud.io

## Rancher (Retiring)

|Hostname|AWS ARN|ALB Name|
|---|---|---|
|toolmanagement.testing.digitalweb.mdtio.com<BR>toolmanagement.staging.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/50fe4141-0481-4f14-a13e-e621604c6ad6|webdev-nonprod|
|usermanager.testing.digitalweb.mdtio.com<BR>usermanager.staging.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/e3ec70d4-0df5-4415-adbb-7964cb80650e|webdev-nonprod|
|erdd.testing.digitalweb.mdtio.com<BR>erdd.staging.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/d0d16ad6-87e8-4bcd-b7a5-4a95508d37b8|webdev-nonprod|
|erg.testing.digitalweb.mdtio.com<BR>erg.staging.digitalweb.mdtio.com |arn:aws:acm:us-east-1:419422262073:certificate/0af29b4f-0405-43b2-b29c-ff53edc971f2|webdev-nonprod|
|worldwiderevenue.testing.digitalweb.mdtio.com<BR>worldwiderevenue.staging.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/c61f39d0-25f3-466e-b47c-3d289b0cac11|webdev-nonprod|
|pci.staging.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/f8264f72-965f-4145-8e0e-b7d9c6cb86cd|webdev-nonprod|
|me.staging.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/f4b2c6db-f68c-4d16-a6a2-fa2ddfb416f5|webdev-nonprod|
|u-ship.staging.digitalweb.mdtio.com<BR>u-ship.testing.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/594cb42d-aafb-401a-ba73-dbc1cb095e24|webdev-nonprod|
|manuals-testing.medtronic.com|arn:aws:acm:us-east-1:419422262073:certificate/17df8312-7efa-4e09-8240-126f2b0ec646|webdev-nonprod-t|
|manuals.testing.digitalweb.mdtio.com| arn:aws:acm:us-east-1:419422262073:certificate/16e0b833-3548-4fca-b872-c844bdb6017b |webdev-nonprod|
|manuals-staging.medtronic.com|arn:aws:acm:us-east-1:419422262073:certificate/671c9c69-cde5-47c3-bbdd-a92eed92d8db|webdev-nonprod-r|
|manuals.staging.digitalweb.mdtio.com| arn:aws:acm:us-east-1:419422262073:certificate/9347e7e4-2299-47e5-9bcf-d33cf50c61f4 |webdev-nonprod|
|manuals-release.medtronic.com|arn:aws:acm:us-east-1:419422262073:certificate/4d69caac-575a-40d7-81fe-86ee8897ecec|webdev-nonprod-r|
|manuals.release.digitalweb.mdtio.com| arn:aws:acm:us-east-1:419422262073:certificate/898eeb5c-7db1-4a2d-b2eb-d1fcbac66948 |webdev-nonprod|
|manuals-api.testing.digitalweb.mdtio.com| arn:aws:acm:us-east-1:419422262073:certificate/50a67b44-2d83-40b3-aeef-62bcc94a5194 |webdev-nonprod|
|manuals-api.staging.digitalweb.mdtio.com| arn:aws:acm:us-east-1:419422262073:certificate/ec874ac1-2474-496f-9064-86265acc3b6f |webdev-nonprod|
|manuals-api.release.digitalweb.mdtio.com| arn:aws:acm:us-east-1:419422262073:certificate/ef7e3701-ab6f-4706-875e-61febdeb4dab |webdev-nonprodext-r|
|emanualsadmin.testing.digitalweb.mdtio.com| arn:aws:acm:us-east-1:419422262073:certificate/1cce3634-19c4-42f7-8e31-00bb5c4a304e |webdev-nonprod|
|emanualsadmin.staging.digitalweb.mdtio.com| arn:aws:acm:us-east-1:419422262073:certificate/4bf0b06a-f83b-4407-99d1-18cec749e14a |webdev-nonprod|
|emanualsadmin.release.digitalweb.mdtio.com| arn:aws:acm:us-east-1:419422262073:certificate/d97de418-9525-497f-a82e-46094751cbb7 |webdev-nonprod|
|gtat-ui.production.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/cbdad6e1-17af-4ba3-8987-d8709908b670|webdev-prod|
|gtat-api.production.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/14206ba0-b6ea-4ea2-a63f-76625a63f564|webdev-prod|
|gtat.production.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/ad679471-ade7-43a0-9d7f-4f5e7e2c03ce|webdev-prod|
|gtat.medtronic.com|arn:aws:acm:us-east-1:419422262073:certificate/a2fe233d-4adc-4e48-bf85-424a3628a5ff|webdev-prod|
|gtat-ui.staging.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/d70efd9d-99ee-4b55-b1b9-862d17f159ce|webdev-nonprod|
|gtat-api.staging.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/6cd8ea02-5c7d-41fb-b686-d3f8fc8e4761|webdev-nonprod|
|gtat.staging.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/2e0473a9-f4f3-4305-800e-3763074c5260|webdev-nonprod|
|gtat-ui.testing.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/891f4783-180e-44a8-84a8-a983c0d57528|webdev-nonprod|
|gtat.testing.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/0a025a31-54ea-43e8-b90f-9313f618c909|webdev-nonprod|
|gfas.testing.digitalweb.mdtio.com<BR>gfas.staging.digitalweb.mdtio.com<BR>gfas.release.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/03c6d1bf-2b0d-4130-af78-1e408463523d|webdev-nonprod|
|gfas.mdtio.com (CN)<BR>gfas.production.digitalweb.mdtio.com (SAN)|arn:aws:acm:us-east-1:419422262073:certificate/e52e1558-65fc-48e8-8a1b-5bbfa81a6023|webdev-prod|
|fcagfas.mdtio.com (CN)<BR>fcagss.production.digitalweb.mdtio.com (SAN)<BR>|arn:aws:acm:us-east-1:419422262073:certificate/8bddd1f0-2f2c-4577-b641-ed48f1a31a1a|webdev-prod|
|fcagss.release.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/47f533b4-49bb-4dda-9792-d5181deb0758|webdev-nonprod|
|insightproductassignment.dev.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/ba91a421-5aa9-4cc6-857d-3ce4329deb83|webdev-nonprod|
|insightproductassignment.testing.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/2a7ef111-76dc-4bbc-a5d4-bbb8314ec561|webdev-nonprod|
|insightproductassignment.mdtio.com (CN)<BR>insightproductassignment.production.digitalweb.mdtio.com (SAN)|arn:aws:acm:us-east-1:419422262073:certificate/20e127c2-3e39-4f98-be3f-c82adeabf0bd|webdev-prod|
|sunray.staging.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/b90a8843-e66b-4f95-a755-8c88089fdc1b|webdev-nonprod|
|myprofile-dev.medtronic.com|arn:aws:acm:us-east-1:419422262073:certificate/441e57a8-01df-4c70-819d-932b1942cb8b|webdev-nonprod|
|myprofile-test.medtronic.com|arn:aws:acm:us-east-1:419422262073:certificate/cd8ace03-19ee-4d66-9d0c-d82461274476|webdev-nonprod-ext|
|myprofile-stage.medtronic.com|arn:aws:acm:us-east-1:419422262073:certificate/4d817dee-16f5-495c-982d-4aa852301a31|webdev-nonprod-ext|
|myprofile.medtronic.com|arn:aws:acm:us-east-1:419422262073:certificate/4501f0cd-29af-40f0-852d-db6cc3262cb8|webdev-prod-ext|
|registration.dev.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/72cc7355-25fe-48fd-8bf4-966a6007d275|webdev-nonprod|
|registration.testing.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/efeeb069-0813-4ab3-ab05-2d2228a3f132|webdev-nonprod|
|registration.staging.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/6a286843-ff13-4cc1-89bd-597695c6b99a|webdev-nonprod|
|registration.production.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/cc748e33-a1dc-49bd-9cad-c5f54ed3aae0|webdev-prod-ext|
|profilemanagement-ui.dev.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/0d785310-7bad-4219-b083-d02bfdbca6ae|webdev-nonprod|
|profilemanagement-ui.testing.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/f6fa55e4-70be-43ff-8057-8e28eb8071ac|webdev-nonprod|
|profilemanagement-ui.staging.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/a2262b99-1bbf-4766-bbea-2ec14afb3d8d|webdev-nonprod|
|profilemanagement-ui.production.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/dea84f92-7d3a-402a-8d7b-058cb7c0abba|webdev-prod-ext|
|profilemanagement-api.dev.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/835633b7-8dd8-4b67-81d6-3c5d38d8e892|webdev-nonprod|
|profilemanagement-api.testing.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/37394d25-476b-4485-bb5f-e8f95b6e7456|webdev-nonprod|
|profilemanagement-api.staging.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/bd0c04cc-a310-4749-bc3a-c370af3d6f12|webdev-nonprod|
|profilemanagement-api.production.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/fcae6a6b-c4a8-46bf-92ad-6eb78e7e9742|webdev-prod-ext|
|hub.selenium.testing.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/f2740d33-133d-4ee9-9cc2-80e6892fabf0|web-dev-ext|
|hub.selenium.digitalweb.mdtio.com|arn:aws:acm:us-east-1:419422262073:certificate/02c525e9-2b8a-4efa-821a-e67d00f9dbfd|?|

### How to request a new ARN for Rancher Environment ###
Future state this will likely be automated, but for now, the manual process is stated below.

Typically this is fulfilled by Ilja Coolen or Niels Van Zwieten through an email request.

**Short description:** Create AWS certificates in AWS Rancher for [AppName]

**Description:**
Please create the AWS SSL certificate for the following hostnames and send the ARN. These hostnames can be paired to one certificate.

* AppName.testing.argo-dev.eks.mdtcloud.io
* AppName.staging.argo-dev.eks.mdtcloud.io
