Podili, Sravan Kumar
0 minutes 4 seconds0:04
Podili, Sravan Kumar 0 minutes 4 seconds
All right, let's start the session.
Podili, Sravan Kumar 0 minutes 8 seconds
So good morning and good evening, everyone. Thanks for joining the call. Let me share my screen.
Podili, Sravan Kumar 0 minutes 33 seconds
I hope you're able to see my screen.

Swain, Pauras
0 minutes 38 seconds0:38
Swain, Pauras 0 minutes 38 seconds
Yes.

Podili, Sravan Kumar
0 minutes 39 seconds0:39
Podili, Sravan Kumar 0 minutes 39 seconds
Yep.
Podili, Sravan Kumar 0 minutes 41 seconds
So I think most of us already know that we have created one upgraded version of our old Argo platform. So we named it as Compass CI. So it's just a random name that we have created for it. So it's going to.
Podili, Sravan Kumar 1 minute 1 second
accelerate the development with the enterprise grade, the CICD capabilities and it is going to add a lot more value and it actually benefits a lot to the application teams who are going to get on board to these to this compass platform.
Podili, Sravan Kumar 1 minute 20 seconds
Also.
Podili, Sravan Kumar 1 minute 24 seconds
Just a minute.
Podili, Sravan Kumar 1 minute 28 seconds
Yeah, so these are the difficulties or challenges that application teams are facing as of now today. So there are some manual and inconsistent releases with inconsistent versioning and manual steps, slowing the deployments and increasing address and.
Podili, Sravan Kumar 1 minute 47 seconds
Security found too late. We all know this issue that the vulnerabilities are often getting discovered late in the development cycles, increasing the remediation risk, and the due rates are also not properly defined, right? And environment confusion, so there are unclear promotion paths.
Podili, Sravan Kumar 2 minutes 5 seconds
There is no defined path to promote the code, and application teams are allowed to create their own paths randomly as per their wish. So that has been very inconsistent with the legacy Argo platform.
Podili, Sravan Kumar 2 minutes 26 seconds
We identified that issue and we wanted to sort it out. And then constant context switching. So developers jump between IDs, GIT labs, security tools and monitoring dashboards to understand builds, vulnerabilities and runtime issues. So that is going to get sorted with this new compass platform.
Podili, Sravan Kumar 2 minutes 46 seconds
So with the new compass CICD pipeline, what application teams are going to get is basically they can get automated semantic versioning. That means there is no more manual version pumps. So earlier, whenever any change that application team does, they have to manually
Podili, Sravan Kumar 3 minutes 8 seconds
bump the version in order to create a new image. So that is going to take that is going to be taken care through the automated semantic versioning in the new compass CSC pipeline and multi environment GitOps deployments. So in this
Podili, Sravan Kumar 3 minutes 27 seconds
Competitive platform, and there are actually standard environments that are going to be there, testing, staging, release, and production, and all the application teams, environments, and configuration version in GIT only, and the teams will have full control.
Podili, Sravan Kumar 3 minutes 48 seconds
And then built-in security scanning. So this was already there in the previous Argo platform as well. So container and image scanning, SAST, dependency, runtime contrast. So all those were also there. But with the new compass CA pipeline, they are going to get
Podili, Sravan Kumar 4 minutes 7 seconds
more visibility towards the vulnerability issues and all in GitLab itself. So that's what mentioned in the next point as well. GitLab native integrations. So issues, environments, pipelines, all are there and they can also add some custom jobs, stages and
Podili, Sravan Kumar 4 minutes 28 seconds
variables as needed. So our templates, whatever the templates that we are providing, they are actually extensible and GCSO compliance. So we all know that we have to comply according to the GCSO standards. So our compass CAC pipeline automatically.
Podili, Sravan Kumar 4 minutes 47 seconds
enforces the security policies and best practices as per GCS for recommendations.
Podili, Sravan Kumar 4 minutes 55 seconds
So this is how compass here works. So these are the stages, pipeline stages that you can say. So whenever the developers push their code to their GIT repositories, then pipeline will get triggered and these are the stages that it will go through. So the lint and check, basically it will.
Podili, Sravan Kumar 5 minutes 15 seconds
check the code, it will verify the code, and then it builds the image, and then it will run the test on top of that image, and it will do some security scans and do the automated versioning, and then deploy and verify. So this means faster releases, fewer errors, and full compliance.
Podili, Sravan Kumar 5 minutes 34 seconds
And so, coming to the differences, so you may get a question like, what are the basic differences between world Argo platform and the new compass platform? So, basically, these are the differences, like in Argo we have Medtronic specific patterns and conventions, but when it comes to compass.
Podili, Sravan Kumar 5 minutes 54 seconds
We have open portable standards like less proprietary coupling means.
Podili, Sravan Kumar 6 minutes
Earlier, we used to manage all application teams manifest files, Kubernetes manifest files, and we used to take the ownership of their application manifest, but with the Competitive platform, we are going to...
Podili, Sravan Kumar 6 minutes 19 seconds
Allow the application teams to have control and ownership on Kubernetes manifest by themselves, so all the Kubernetes manifest are going to be residing in their repositories, right? And higher platform dependency and limited standardization. So, earlier with Argo.
Podili, Sravan Kumar 6 minutes 39 seconds
There is a huge dependency on platform side and nothing was really...
Podili, Sravan Kumar 6 minutes 48 seconds
given to application teams when it comes to ownership. So now, with the new Compass CICD platform, we don't want to take that much of ownership. We wanted application teams to have the ownership of their own manifest files or code.
Podili, Sravan Kumar 7 minutes 7 seconds
Whatever it may be, and everything will be controlled and managed by themselves only, so...
Podili, Sravan Kumar 7 minutes 15 seconds
And with the Compass AI platform, there will be reusable templates and consistent configuration patterns as well. And with Argo, there is a stable onboarding for new teams. So
Podili, Sravan Kumar 7 minutes 31 seconds
For whenever a new team comes, then there are actually a lot of steps involved, and they have to get the access to the tools, and then they have to raise some requests with some RTMs, so...
All those manual steps is actually, you know, delaying the process of onboarding, so that that is now sorted with the compass platform. So, with compass AI, the onboard is onboarding is going to be very faster and everything is going to be self-service.
Podili, Sravan Kumar 8 minutes 10 seconds
And then with Argo, more manual troubleshooting and handoffs, of course. So.
Podili, Sravan Kumar 8 minutes 18 seconds
It was actually not really automated as expected when it comes to Argo, but in Compass CI, the troubleshooting is going to be much easier and there is going to be better with observability as well and visibility to
Podili, Sravan Kumar 8 minutes 37 seconds
the issues and everything. And so with Argo, the AIS test was actually very non-effective or less effective due to proprietary context. So as I said earlier, earlier with Argo platform, we used to warn everything and we used to
Podili, Sravan Kumar 8 minutes 58 seconds
do everything on our own, but with the compass here platform, now we have a GitHub Copilot and the Agents, so they can just assist effectively and now they can troubleshoot whatever the issues that may be in their code or pipeline configuration or in their Kubernetes manifest.
Podili, Sravan Kumar 9 minutes 17 seconds
and they can do the RCA and they also get the support from documentation as well. So all of this results in fast onboarding, better developer productivity, and easier support with the AI assisted workflows.
Podili, Sravan Kumar 9 minutes 35 seconds
O.
Podili, Sravan Kumar 9 minutes 37 seconds
As I said earlier, with the legacy Argo pipeline, there was no automated semantic versioning. Versions has to be warmed up manually, so...
Podili, Sravan Kumar 9 minutes 51 seconds
Now with the compass platform, they never want to need to manage version numbers again. So conventional commits, as you can see, they can just, you know, automatically get versioned with this type of commit format.
Podili, Sravan Kumar 10 minutes 10 seconds
So they can just for suppose if the committees just feed feature, feed means feature, then it will just bump up a minor version. And if it is a fixed, then it will just bump up a patch. And then if it is feed exclamatory,
Podili, Sravan Kumar 10 minutes 30 seconds
Then it is going to create a major version, so we have already created documentation for this, and in the documentation everything is actually very clearly defined and explained, and what are the benefits because of this, so this.
Podili, Sravan Kumar 10 minutes 49 seconds
This will actually create the automatic change log and grid tags will get created automatically and immutable artifacts with proper versions will get generated and address will different commit to deployment is another advantage.
Podili, Sravan Kumar 11 minutes 5 seconds
And automated security gates, of course. So we have a lot of security tools in place. So when it comes to Docker file validation, it will find best practices and security standards. And it will run when...
Podili, Sravan Kumar 11 minutes 25 seconds
any Docker file change committed and the SAS DS for code security and quality issues. Of course, for every commit, the SAST will run and review policy Kubernetes misconfigurations and security issues if it finds any Kubernetes manifest misconfigurations and security issues.
Podili, Sravan Kumar 11 minutes 43 seconds
It will run when any YAML file change is committed and dependency scanning. Of course, any vulnerable libraries are there and if there is any package that is
Podili, Sravan Kumar 12 minutes 3 seconds
you know, that has high risk, then it will just find it out. Also, it will run for every build and container scanning, image vulnerabilities, of course, the CVs. So it is going to be, again, a PV in case of compass.
Podili, Sravan Kumar 12 minutes 23 seconds
platform and for every image build, this container scanning is going to run and it will find the CVS or image vulnerabilities and it will generate a report as well. And contrast security, it is for runtime vulnerabilities. If there are any vulnerabilities that are getting generated during runtime, then
Podili, Sravan Kumar 12 minutes 43 seconds
It will get caught by contrast and this will run post deployment, so...
Podili, Sravan Kumar 12 minutes 53 seconds
automatic GitLab Issue creation. So this is the new feature that will help the application teams to identify the CVEs or vulnerabilities in the GitLab issues itself. So it will create issues for critical high, medium, low vulnerabilities.
Podili, Sravan Kumar 13 minutes 13 seconds
and assigns Q rates based on severity and first seen date to align with the GCSO policies and it will track the remediation progress as well. So in GIT lab issues, right now it is work items actually, so it will, you know, track the progress of the remediation as well.
Podili, Sravan Kumar 13 minutes 32 seconds
Initially, it will be assigned as to do, but it will have stages like in progress, in review, done, completed like that. So you can also work on those issues in GitLab itself. You can add the comments and there will be a commit history or a change.
Podili, Sravan Kumar 13 minutes 52 seconds
Change log as well in those issues that you can see and you can it can also validate the policy as well for suppose if there is any CV that cannot be remediated within the timeline then and if the application needs to get applied to production or quarantine then they have to go through this policy from request.
Podili, Sravan Kumar 14 minutes 12 seconds
format, right? So once they generate that PER, then they can just incorporate that PER as a label, as you can see here. And immediately, once the PER is attached to this CVE, then our pipeline will ignore that CVE and it will skip it.
Podili, Sravan Kumar 14 minutes 35 seconds
and it will allow the pipeline to run successfully and it will not show the issue again. So that is one of the new features. And yeah, so environments, so this is another feature. So each environment has protected branches, approval gates, health checks, environment dashboards in GitLab.
So as you can see, there is a section called environments in every GitLab project. So in that environment section, they have to create these environments according to the application team's requirement, like in which environments they would like to deploy their applications.
Podili, Sravan Kumar 15 minutes 15 seconds
So accordingly, they have to create the environments. So basically, I think day one production will be there by default, obviously. And apart from day one production, there could be another environments like staging or testing or release. So it basically depends on application teams.
Podili, Sravan Kumar 15 minutes 37 seconds
requirement and their wish. So accordingly, they have to create these environments. And once they create these environments, then they can just see the deployment history in the environment itself, just like this. You can see who triggered it.
Podili, Sravan Kumar 15 minutes 56 seconds
and the commit and the job. So everything can be seen here.
Podili, Sravan Kumar 16 minutes 1 second
And so, so basically, these are these are the GitLab features spotlight, like environments, dashboards are there under match request. We we'll do the security scan which results in line scan scan reports.
Podili, Sravan Kumar 16 minutes 21 seconds
Are getting generated and automatic policy checks before merge as well, and issues of course the GIT lab with the GIT lab issues vulnerability issues will get automatically created, and the due rate tracking is also there for remediation.
Podili, Sravan Kumar 16 minutes 41 seconds
which follows GCSO policies to block vulnerable deployments, and then it will verify policy exceptions as well. It will verify whether they are approved or not before deployment itself, and it will generate release modes so far auto-generated.
Podili, Sravan Kumar 17 minutes 2 seconds
from commits and publish to GIT Lab releases.
Podili, Sravan Kumar 17 minutes 9 seconds
So basically these are the features that are there in the new GitLab dedicated. So as application teams are going to get migrated from GitLab to directed GitLab, so these are all the features that
Podili, Sravan Kumar 17 minutes 28 seconds
Application teams are going to get when they migrate from old legacy Argo platform to new compass here platform, so.
Podili, Sravan Kumar 17 minutes 40 seconds
Yep.
Podili, Sravan Kumar 17 minutes 42 seconds
And.
Podili, Sravan Kumar 17 minutes 45 seconds
Yeah, basically, they can just have full control with safety rails, like deploy with any standard Kubernetes workload and use standard K, stocks, CA tools, and commit resources. And the teams can maintain autonomy while meeting policies.
Podili, Sravan Kumar 18 minutes 5 seconds
and no custom proprietary mechanisms, just proven Kubernetes patterns. So verification is built in. This was already discussed. So, and disaster recovery as well. So completing structural version link it, redapply from any point in time, and no manual reconstruction.
Podili, Sravan Kumar 18 minutes 24 seconds
So I will walk you through the sample project, application project, and I will also walk you through the documentation now.
Podili, Sravan Kumar 18 minutes 35 seconds
And yeah, so if any application team wants to.
Podili, Sravan Kumar 18 minutes 41 seconds
come to this compass platform, one should deploy their application or host their applications on compass platform. They can just go to this documentation or I will open this documentation now. So basically this is compass playbook. I will also share this.
Podili, Sravan Kumar 19 minutes
With the Team.
Podili, Sravan Kumar 19 minutes 13 seconds
Yeah, so this documentation is not only for the application teams, it is also for us, L3 team, and also for you guys, L1 team. So all of us can, you know, treat this as a bible and...
Podili, Sravan Kumar 19 minutes 34 seconds
each one of us has to go to this documentation and get well-versed with everything, every step, so that we can assist application teams in a better way, right? So if any application teams comes to you with any request, then
Podili, Sravan Kumar 19 minutes 54 seconds
You can just guide them through this documentation. You can just give them this documentation and go through this, ask them to go through this, right? And so what are the most important things that
Podili, Sravan Kumar 20 minutes 13 seconds
We have to focus on in this documentation. I will just quickly walk you through those particular pages, right? So.
Podili, Sravan Kumar 20 minutes 24 seconds
Before starting, so basically this is just, you know, giving you the...
Podili, Sravan Kumar 20 minutes 32 seconds
definitions or introductions like what is Compass CI platform, right? Whether Compass CI is right for their applications or not. So all those things. And it has some nice FAQ page, so explaining what is Compass CI and key benefits.
Podili, Sravan Kumar 20 minutes 51 seconds
Is compass here related to the external Argo CD tool or not? So it has a lot of FAQs which can answer a lot of questions from the application team. I can just go through it later. And coming to the getting started.
Podili, Sravan Kumar 21 minutes 9 seconds
application teams first, they first has to go to this page, tooling and access. They may have to get access to some of the tools, maybe Visual Studio Code, Docker Desktop, GIT, of course, and then recommit.
Podili, Sravan Kumar 21 minutes 30 seconds
Um...
Podili, Sravan Kumar 21 minutes 33 seconds
And then...
Podili, Sravan Kumar 21 minutes 37 seconds
Of course, here, yeah.
Podili, Sravan Kumar 21 minutes 41 seconds
Uh...
In order to ensure each application team person has the same set of tools, they can just add these files. This is one of the useful tool.
Podili, Sravan Kumar 22 minutes 4 seconds
And then.
Podili, Sravan Kumar 24 minutes 18 seconds
They have to go to this, you know, prerequisites page where they can just...
Podili, Sravan Kumar 24 minutes 29 seconds
You can just go through what are the must have and what is the best practice. So we have given some recommendations. So these are the categories like organizational and data app should be in ServiceNow, CMDB, and the cost center
Podili, Sravan Kumar 24 minutes 48 seconds
needs to be assigned.
Podili, Sravan Kumar 24 minutes 52 seconds
And when it comes to architecture, they need to ensure that their application can be content raised, and their application has health check endpoints, and it should have a non-root user. So the recommended approach is stateless design, minimal base images.
Podili, Sravan Kumar 25 minutes 13 seconds
So likewise, we have mentioned a few recommendations and organization requirements. Of course, we have given here a detailed explanation why ServiceNow CMDB configuration is required and how to get it right. So application teams can go through this and if at all they need
Podili, Sravan Kumar 25 minutes 33 seconds
any help you can just guide them to this page.
Podili, Sravan Kumar 25 minutes 38 seconds
I will just quickly go through the most important page.
Podili, Sravan Kumar 25 minutes 46 seconds
for suppose any application team comes to you and asks you to onboard their application, which is a new application for this compass CI platform, then you can just come to this page. So here it is clearly mentioned that this page is for the platform and platform support team only.
Podili, Sravan Kumar 26 minutes 7 seconds
And it consolidates required setup for Flux GitHub onboarding support, platform managed tokens, and cross project CA access tools. So it also explains when to use this runbook. So if you're onboarding a new application, team or group to compass CA, enabling Flux register and Flux verify jobs for a new report.
Podili, Sravan Kumar 26 minutes 29 seconds
and troubleshooting cross project permission failures. So during these situations only, you have to use this particular runbook. If not, for suppose there may be a situation like application
Podili, Sravan Kumar 26 minutes 48 seconds
team, I mean their applications might have already been deployed to old Argo platform and they want to migrate to the new compass platform. So in that case here there is a there is another runbook called Argo to compass migration plan.
Podili, Sravan Kumar 27 minutes 8 seconds
So here we have clearly mentioned that when to use this runbook. So they have to use this runbook when migrating an existing Argo CSV application on old on-prem GitLab to new GitLab dedicated. So it will have
Podili, Sravan Kumar 27 minutes 28 seconds
Phases, migration phases, as you can see, so...
Podili, Sravan Kumar 27 minutes 33 seconds
Uh.
Podili, Sravan Kumar 27 minutes 35 seconds
Migration has to be done in a step-by-step or face-by-face manner. So, what are those steps and what are those phases? Everything is clearly mentioned here, but before this, I would like to walk you through this platform team runbook.
Podili, Sravan Kumar 27 minutes 51 seconds
Oh.
Podili, Sravan Kumar 27 minutes 53 seconds
So.
Podili, Sravan Kumar 27 minutes 57 seconds
So all these below steps, whatever are mentioned here, it is actually clearly shown in a recording.
Podili, Sravan Kumar 28 minutes 11 seconds
This recording is not found here.
Podili, Sravan Kumar 28 minutes 16 seconds
Okay, I will show you that recording later. So let me walk you through these steps. So basically, these are platform managed responsibilities, means our responsibilities. So we have to provide access to this semantic release project. I will show you what that semantic release project is.
Podili, Sravan Kumar 28 minutes 39 seconds
Welcome to Rubik Lab.
Podili, Sravan Kumar 28 minutes 46 seconds
Yeah. So earlier in the old legacy Argo platform, we used to have three different repositories like manage namespace, build app image, and deploy image, right? So those are our Argo pipeline
Podili, Sravan Kumar 29 minutes 1 second
our platform related repositories where we have incorporated all the logic of CICD in those three projects. But you can just imagine or think like this semantic release project is a replacement of those three projects. Okay, so it has dot git labs here.
which has its own stages.
Podili, Sravan Kumar 29 minutes 29 seconds
like, yeah, Lynch check test build release, and it has a lot of YAML files as well, like containers dot YAML and flux registration dot YAML, which are very important.
Podili, Sravan Kumar 29 minutes 45 seconds
Pipeline.yaml.
Podili, Sravan Kumar 29 minutes 47 seconds
So basically, every application team needs to get access to this semantic release project. So that is what it is explained here. So Shared semantic release project access for template consumption. So if any application team wants to consume the templates present in this semantic release project, then
Podili, Sravan Kumar 30 minutes 7 seconds
We have to provide them access to this project, so that is what it is explained here. So it is it is clearly shown that completing this step and just all pipeline executions from the client CICD lab group are able to access and use the semantic release projects templates. So how it is done? Everything is explained here, so we have to share.
Podili, Sravan Kumar 30 minutes 27 seconds
BCP web DEVOPS the semantic release with the onboarding group at reporter access level. So if we go to or suppose.
Podili, Sravan Kumar 30 minutes 47 seconds
Yeah, so here.
Podili, Sravan Kumar 30 minutes 56 seconds
Yeah, here in this project members section, you can see that there are actually...
Podili, Sravan Kumar 31 minutes 4 seconds
Some groups like.
Podili, Sravan Kumar 31 minutes 16 seconds
Yeah, here you can see right invited group plus KAM and we have given reporter level access to this. So basically what you have to do is here you have to click on invite a group and here you have to search for that group.
Podili, Sravan Kumar 34 minutes 5 seconds
So.
Podili, Sravan Kumar 34 minutes 8 seconds
Yeah, so you have to click on add.
Podili, Sravan Kumar 34 minutes 12 seconds
group or project and you have to select the group. OK, what are the group it is already? Some groups are already added. You can see here, right? So these are all the application team groups, OK? And similarly, you have to add those groups and go to fine-grained permissions and you have to.
Podili, Sravan Kumar 34 minutes 33 seconds
For these repositories, you have to allow read permissions. That's it. Just leave remaining things as is, and then click on Add.
Podili, Sravan Kumar 34 minutes 44 seconds
So.
Podili, Sravan Kumar 34 minutes 47 seconds
That is what I explained here. Everything is clearly explained, and next thing is configure group level CICD variables. Currently, we have automated this process. We can see the same here. So we have created two Python files. One is the Shared WAS.
Podili, Sravan Kumar 35 minutes 6 seconds
and token vas.py. This shared vas.py is what are the shared or common CICD variables that are required for every application team are grouped across the board.
Podili, Sravan Kumar 35 minutes 25 seconds
For suppose, let me show you.
Podili, Sravan Kumar 35 minutes 30 seconds
this code.defile.com.
Podili, Sravan Kumar 35 minutes 44 seconds
So this is BCP web is a group, right? And for this, if you go to CICD.
Podili, Sravan Kumar 35 minutes 52 seconds
Variables expand.
Podili, Sravan Kumar 35 minutes 56 seconds
Currently there are no variables in this. I think they got already moved to GIT Lab dedicated. Let me show you.
Podili, Sravan Kumar 36 minutes 10 seconds
So, let's take...
Podili, Sravan Kumar 36 minutes 13 seconds
Clinical RDC.
Podili, Sravan Kumar 36 minutes 19 seconds
Of clinical good. OK.
Podili, Sravan Kumar 36 minutes 24 seconds
Go to CICD.
Podili, Sravan Kumar 36 minutes 27 seconds
Where to go?
Podili, Sravan Kumar 36 minutes 29 seconds
Yeah, so you can see, right? So there are a few variables that are out there, composite, TFC, API token. Basically, these are tokens, variables are also there. Let me check the...
Podili, Sravan Kumar 36 minutes 41 seconds
Do you see BabuVPE? Yes, ready.
Podili, Sravan Kumar 36 minutes 46 seconds
It records.
Podili, Sravan Kumar 36 minutes 48 seconds
Yeah, so basically these are the variables that every application group need to have. These variables has artifactory password, username, CA registry base, compass CA, TFC org. So basically these are the variables that each and every application team or group need to have.
Podili, Sravan Kumar 37 minutes 8 seconds
So instead of we giving them the values or we going to each of their application teams groups and we creating them manually, we have created this automation. Shared vars.vy, which will automatically create those CA/CD variables inside their
Podili, Sravan Kumar 37 minutes 28 seconds
application group and the token vast or PV, which will take care of creating the required tokens for each application team group so that there are the CICD pipelines will work effectively without any errors or failures. And if for suppose these CACD variables or tokens are not there.
Podili, Sravan Kumar 37 minutes 51 seconds
Then application will not get deployed and the CICD pipelines will not run because those variables are required for all the automation pipelines and for all these CICD pipelines to run properly without any issues or errors. OK, so we have already created these.
Podili, Sravan Kumar 38 minutes 11 seconds
Oh.
Podili, Sravan Kumar 38 minutes 12 seconds
Python automation files and how to use it, how to run it, we have explained clearly. Python is required and AWS CLA is required in local and how to install it and how to check the versions. And once you have the required tools, then how to run those files. We have explained everything clearly. Just go through this once.
Podili, Sravan Kumar 38 minutes 33 seconds
We have also created one video recording as well for that. I will give you the permission to all those video recordings later. You can also go through them.
Podili, Sravan Kumar 38 minutes 47 seconds
So, yeah, this documentation basically explains each and everything, so how to onboard a new application to.
Podili, Sravan Kumar 38 minutes 56 seconds
a compass here platform. So first of all, if any application team comes to you, you have to guide them to this particular page in the documentation, the cloud repository configuration. So basically
every application team who comes to Compass CA platform, they have to configure their repository as mentioned in this page. Okay, so this page has to be done by application team only. It's not your responsibility or
Podili, Sravan Kumar 39 minutes 35 seconds
our responsibility. Only application team has to do this configuration. Okay. So it has everything mentioned and explained clearly. Okay. Set up. I mean, you can see, right, set up your application repository so compass CA can build, release and deploy your workloads and
Podili, Sravan Kumar 39 minutes 55 seconds
It has everything explained in detail, like what is the brand strategy that they have to follow and what are the protector branches that they have to configure? What is the definition of those branches?
Podili, Sravan Kumar 40 minutes 8 seconds
right, and how to set up a protected branches, how to set up GitLab environments. Everything is there in this page. They can just go to this page, go through each and every step in detail, and they can do it all by themselves. If at all they get stuck in between, then you can just pitch in and
Podili, Sravan Kumar 40 minutes 28 seconds
You can guide them by just going through this documentation itself. OK. So you I mean I would request all of you to go to this page once or twice so that you will you will know what application team has to do, what are the steps they have to follow.
Podili, Sravan Kumar 40 minutes 48 seconds
So that if at all they get stuck in between, then you can just chime in and guide them effectively, right? So, whenever any new application team comes, then this GIT Lab repository configuration has to be done by them, and you can guide them to do it, and once it is done, then there are some.
Podili, Sravan Kumar 41 minutes 8 seconds
Platform team responsibilities are there. So this page, this has to be done by us. As I mentioned earlier, it has.
Podili, Sravan Kumar 41 minutes 17 seconds
Detailed steps, as you can see, you can just go through it and, you know, configure their application.
Podili, Sravan Kumar 41 minutes 27 seconds
pipelines, everything. Coming to Argo to compass migration plan. This is for the new application teams that wants to get onboarded to the compass AI platform. But what about already existing applications that are there in our Argo platform?
Podili, Sravan Kumar 41 minutes 47 seconds
So they want to get migrated to the compass platform, right? Argo platform is going to be, you know, deprecated or decommissioned very soon.
Podili, Sravan Kumar 42 minutes
We have to migrate application teams from old Argo legacy platform to the new compass platform. So for that, this is the plan or steps.
Podili, Sravan Kumar 42 minutes 14 seconds
OK.
Podili, Sravan Kumar 42 minutes 16 seconds
So it has clearly mentioned platform manage responsibilities. Yeah, I think someone has a question.
UW

Urrea, William
42 minutes 25 seconds42:25
Urrea, William 42 minutes 25 seconds
Yes, thank you, Podili. Regardless to this going, it sees some clarifications on access, regardless to the to the block, the possible blockers that exist in the migration activities from Ronald execution, because in several.
Urrea, William 42 minutes 44 seconds
cases that we place in, I mean, from our support team, we facing cases that refers to the executive blockers that exist in on the current compass CI platform for not able to execute the deployment activities. So some of the clients
Urrea, William 43 minutes 3 seconds
ask ourselves when those blockers are solved or that sees an action items that can able to allow it to solve it because we know that it sees deadlines regardless to this activity. So that's my question regardless to that.

Podili, Sravan Kumar
43 minutes 21 seconds43:21
Podili, Sravan Kumar 43 minutes 21 seconds
No, I did not get your question clearly. Could you please repeat it?
UW
Urrea, William
43 minutes 26 seconds43:26
Urrea, William 43 minutes 26 seconds
Oh yeah, sure. I mean, if for getting responsible to the clients that ask ourselves from in this in this aspect, it sees some items that can allow us to unblock the deployment execution on the runners in the new platform in the new Compass CI.

Podili, Sravan Kumar 46 minutes 33 seconds
Right.
UW
Urrea, William
46 minutes 35 seconds46:35
Urrea, William 46 minutes 35 seconds
OK, you.

Podili, Sravan Kumar
46 minutes 36 seconds46:36
Podili, Sravan Kumar 46 minutes 36 seconds
Once it is enabled, then their pipeline jobs can run on those runners, and those runners has access and connectivity to all the external tools like Artifactory or AWS accounts, whatever it will be.
UW
Urrea, William
46 minutes 55 seconds46:55
Urrea, William 46 minutes 55 seconds
OK, but what about if the if that application team have configured a custom a custom runners on the previous platform? What about the what about those things have should be proceed for enabling those custom runners?

Podili, Sravan Kumar
47 minutes 12 seconds47:12
Podili, Sravan Kumar 47 minutes 12 seconds
Custom runners, I think that's a different requirement. I would say if, yeah, I would say if at all they want to have a custom runner, then they have to raise a ticket to our EKS platform team, okay? And they can guide them how to configure custom runner.
UW
Urrea, William
47 minutes 14 seconds47:14
Urrea, William 47 minutes 14 seconds
Yes.
Urrea, William 47 minutes 17 seconds
Yeah.

Podili, Sravan Kumar
47 minutes 32 seconds47:32
Podili, Sravan Kumar 47 minutes 32 seconds
and whether it is possible or not. And if at all it is possible, then how to configure it. I think they can guide it, guide them on that. Okay. But that is not in our scope. Okay. In our Competitive platform team scope. So for their project, they need some runners
UW
Urrea, William
47 minutes 44 seconds47:44
Urrea, William 47 minutes 44 seconds
Okay.

Podili, Sravan Kumar
47 minutes 53 seconds47:53
Podili, Sravan Kumar 47 minutes 53 seconds
for their application deployments. So we are providing these shared runners, OK? And first of all, why do they need a dedicated custom runner? OK, that is the question. So they can raise a ticket with the EKS platform team and they can get answers to all their questions.
UW
Urrea, William
48 minutes 13 seconds48:13
Urrea, William 48 minutes 13 seconds
OK, so yeah, but yeah, this is my last question in regards to this. If in this case, if the project try to execute it using the shared runner, but the runner has a blocker, how we can suggest that the client, the application team should be proceed for?

Podili, Sravan Kumar
48 minutes 13 seconds48:13
Podili, Sravan Kumar 48 minutes 13 seconds
And help from them, OK?
Podili, Sravan Kumar 48 minutes 35 seconds
Again, they...
UW
Urrea, William
48 minutes 35 seconds48:35
Urrea, William 48 minutes 35 seconds
Right there, the alarm drop.

Podili, Sravan Kumar
48 minutes 38 seconds48:38
Podili, Sravan Kumar 48 minutes 38 seconds
Yeah. Again, if the runner is not able to connect to any external required tool, then for suppose to any external database, they are unable to connect to it. Then they can just raise a ticket to the EKS platform team regarding the runner issue and they can just sort it out.
Podili, Sravan Kumar 48 minutes 57 seconds
are not being maintained by our team, okay? So there is a separate team who maintains the runners and their configurations, so they can raise a ticket to that team and they can get it sorted.
UW
Urrea, William
49 minutes 13 seconds49:13
Urrea, William 49 minutes 13 seconds
OK, OK, that's that's the final question that I got. Thank you for the.

Podili, Sravan Kumar
49 minutes 17 seconds49:17
Podili, Sravan Kumar 49 minutes 17 seconds
Sure. Thank you. This GitLab repository configuration has to be done by the application teams. Once it is done, then you can just help the application teams by following this platform and support runbook.
Podili, Sravan Kumar 49 minutes 36 seconds
And configure everything if it is a new application, and if it all is a it is an existing application, and if it wants to get migrated from the old Argo platform to new composite platform, and we have clearly mentioned this migration plan, so you can just go through this documentation. It has it has all the details explained.
In detail, and...
Podili, Sravan Kumar 49 minutes 58 seconds
Once it is done, then if at all any team faces any issues, like they need some clarity and they don't know what is what and what to do when or where, then
Podili, Sravan Kumar 50 minutes 18 seconds
You can just go to this documentation once for suppose if at all they ask you about Kubernetes manifest, what are the Kubernetes manifest that they have to create in their repository? So and what is the structure? You can just come to this page and you know show them that this is how.
Podili, Sravan Kumar 50 minutes 36 seconds
their Kubernetes directory structure should be. It should have some base Folder and then it should have these environmental overlays. What are the environments they want to have like production release staging according to their requirement.
Podili, Sravan Kumar 50 minutes 56 seconds
they can create this folder structure and they should have these files. I can show it to you one example.
Podili, Sravan Kumar 51 minutes 12 seconds
So this is one of the sample projects that we have created for our own purpose. You can also come to this repository for reference. You can take this repository as a reference. Okay, so here
Podili, Sravan Kumar 51 minutes 32 seconds
Yeah, we have created this case folder, right? And in this, there are actually a few folders like base, and inside this base, there are actually a few keywords manifest files out there. And these are the environment overlays, okay? And these environment overlays are created for, you know, patching, okay?
Podili, Sravan Kumar 51 minutes 51 seconds
instead of creating all the configuration files and messing up with all those files, they can just do some patching in these environment overlays without touching any of these main files. OK, so to get clarity on that, just go through this documentation. So likewise there are there is.
Podili, Sravan Kumar 52 minutes 12 seconds
documentation for everything.
Podili, Sravan Kumar 52 minutes 15 seconds
Like, these are explanations for what are deployment, service configuration, volumes, and storage. Okay. And when it comes to CICD pipeline, what are the pipeline stages and jobs that are there? So everything is clearly mentioned here. So these are all the pipeline stages.
Podili, Sravan Kumar 52 minutes 35 seconds
What is the stage, purpose, and the key jobs, and whether it will get blocked or not, if there is an issue. So just go through this and coming to semantic versioning. So this is one of the new features that I have spoken about. So how semantic versioning works in Competitive platform, what is semantic versioning?
Podili, Sravan Kumar 52 minutes 56 seconds
as you can see, what is bug fix, new major, new feature, minor, what is major, right? So it has everything explained in detail. And
Podili, Sravan Kumar 53 minutes 12 seconds
Troubleshooting and support. Of course, you have the troubleshooting guide, just like how you used to have it in Argo platform. It has a lot of issues already defined here.
Podili, Sravan Kumar 53 minutes 28 seconds
You can just go through it, go to this, and...
Podili, Sravan Kumar 53 minutes 32 seconds
And one more thing is SOPS. So SOPS is nothing but secret operations. So if at all application team wants to use this SOPS setup, then they can do that. But currently with our Flux registration step, currently the SOPS configuration is already
Podili, Sravan Kumar 53 minutes 54 seconds
taken care of in the pipeline itself, in the automation itself. So that's what mentioned here. The standard onboarding use flux register jobs for the from the app pipeline, which will automatically provision the SOPS key. So what is SOPS key? What is private key? What is age? What is
Podili, Sravan Kumar 54 minutes 12 seconds
public key and how to encrypt or decrypt those secrets. So you can do some research and also you can go through this page. So what I would say is I would request each one of you to go through this documentation, this compass CA playbook. Okay.
Podili, Sravan Kumar 54 minutes 30 seconds
I think maybe we can have another call next week, maybe on Tuesday or Thursday. By then, I would like to see each one of you already gone through this documentation. I will also add you to one Team channel where all the
Podili, Sravan Kumar 54 minutes 53 seconds
KT recordings are there which are already recorded by Ash. OK, and I will provide you that link as well. So please go through those recordings as well. It will give you much more clarity and much more better understanding. So please go to this documentation and those recordings and come up with the questions if you have any in the next call.
Podili, Sravan Kumar 55 minutes 13 seconds
OK, and what I what I would like to expect is...
Podili, Sravan Kumar 55 minutes 19 seconds
So currently, Argo to compass migrations are going on. And we L3 team are only working on those Argo to compass migrations. And there are actually a lot of applications out there in our old Argo platform. And I would like your
Podili, Sravan Kumar 55 minutes 38 seconds
tell one team to take over those tasks as soon as possible. So I I want each one of you to, you know, leverage yourself, wrap up yourself by going through this documentation and recordings right and take up those tasks as soon as possible. OK, so. I think I'm done from my side and I have a.
Podili, Sravan Kumar 56 minutes
call, another call in the next two minutes. So just go to this documentation and go to those recordings, which I'm going to provide it to you. And if you have any questions, then we can discuss those questions in the next call.
Podili, Sravan Kumar 56 minutes 15 seconds
Okay.

Gorai, Sangram
56 minutes 18 seconds56:18
Gorai, Sangram 56 minutes 18 seconds
Sure, Sravan.

Podili, Sravan Kumar
56 minutes 21 seconds56:21
Podili, Sravan Kumar 56 minutes 21 seconds
Yeah, okay. Thank you. Thank you, Ron, for joining the call. Bye.

Swain, Pauras
56 minutes 21 seconds56:21
Swain, Pauras 56 minutes 21 seconds
Absolutely.

Gorai, Sangram
56 minutes 21 seconds56:21
Gorai, Sangram 56 minutes 21 seconds
Thank you.