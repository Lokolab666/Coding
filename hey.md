Look the replies. Maybe Im confusing, because 
John Mehawej
Additional comments•2026-08-21 15:38:09
We currently develop and test our docker containers on our local development machines (macbook pros). We do not have an external repository for our docker containers currently. Our goal is to get to AWS so we can fire up the containers using Lambda. We want it to be automated so that when we commit any code to our main branch in gitlab, the CICD pipeline pushes it to artifactory and makes it available to our AWS environment.

My username is mehawj2 , and my coworker who will also be using this is mattse3 .


Cristian Fandiño Mesa
Additional comments•2026-08-21 14:40:25
John,
To help us determine the right repository configuration, could you please let us know where your Docker containers are currently hosted? If this a personal Docker or If they are hosted in a Docker Hub repository (or another external container registry), we can create a remote repository in Artifactory. This will allow Artifactory to act as a proxy/mirror for your existing container repository. In that case, please provide the repository URL.

Additionally, please provide the following information for access configuration:
- Username (if you will be using a user account), or
- Name and email address (if you require a service account for pipeline authentication)

Once we have these details, we can proceed with the repository and access setup.

John Mehawej
Additional comments•2026-08-21 12:49:27
Hi Cristian: The reason I'm trying to use artifactory is so that I can make my docker containers available to run in my AWS dev environment. Does that mean I should use a local repository or a remote repository?

We will use a User Account to authenticate unless a service account is absolutely necessary

Cristian Fandiño Mesa
Additional comments•2026-08-20 10:48:30
Hi John,

Repository Creation
Please specify whether you need a Local or Remote repository:
Local Repository: Used when artifacts are uploaded directly into Artifactory. In this case, you would provide the files, and they would be stored in the repository.
Remote Repository: Used when Artifactory acts as a proxy/cache for an existing external repository (for example, Docker Hub or another container registry). If this is your use case, please provide the repository URL so we can create the remote repository configuration.

User or Service Account
Please also let us know how your pipeline will authenticate:
User Account: Please provide the username and email.
Service Account: Please provide the service account details (name, email address, and any other relevant information). Once created, the service account can be used within your pipeline configuration.

For additional guidance on configuring Artifactory within your pipeline, please refer to the documentation here:

https://it-sharedservices.medtronic.gitlab-dedicated.site/documentation/jfrog/

Once we have the above information, we'll be happy to assist with the repository setup and access configuration.

Thank you.

John Mehawej
Additional comments•2026-08-20 08:15:19
If I need an Artifactory repository to publish containers to our AWS environment (details listed below), please create it! The repository could be called something like "pdna-aitools-dev" or whatever your naming convention is.

AWS account: corp-pdna-aitools-dev-mdt (293968602262)
AWS region: us-east-1
Deployment target: Amazon ECS Express Mode

Cristian Fandiño Mesa
Additional comments•2026-08-19 15:34:04
Hey John,

To clarify, if your goal is to configure your pipeline to publish or pull artifacts from Artifactory, please refer to the documentation available here:
https://it-sharedservices.medtronic.gitlab-dedicated.site/documentation/jfrog/

If you require a new Artifactory repository, please provide the repository details (such as the repository name, purpose, and environment), and we can create it for you.
Please let us know whether you need help with configuring your pipeline to use an existing repository or creating a new repository, and we'll be happy to assist.

For the repo, https://code.medtronic.com/PDNA_AI/training-gpt-chrome-extension, I don't have access. My username is mesac2


John Mehawej
Additional comments•2026-08-19 14:55:27
It's currently Dev only. Here is the repo: https://code.medtronic.com/PDNA_AI/training-gpt-chrome-extension

Cristian Fandiño Mesa
Additional comments•2026-08-19 14:13:01
Hey John ,

If you need information on how to configure Artifactory in your pipeline, please refer to the documentation here: https://it-sharedservices.medtronic.gitlab-dedicated.site/documentation/jfrog/
Additionally, if you need a specific Artifactory repository to be created, please provide the repository details and we can create it for you.

I have a few questions to help me better understand the setup:

Could you share the GitLab repository URL/path?
Is this application currently Dev-only, or are there plans to have Test and Prod repositories/environments as well?

Thanks!