# OOO Reference Guide - Paternity Leave (8/10 - 10/23)

> This document summarizes priorities, responsibilities, escalation paths, and recommended practices for the team while I am on paternity leave from **8/10 through 10/23**.

---

## Priorities

1. **Support and escalations come first.** Keep the support team unblocked, responsive, and covered across multiple people.
2. **Then execute Compass CI delivery work** using JIRA priority and migration timelines.
3. **Pull from highest priority first** and escalate blockers early.

### Initiative Buckets (After Support)

1. **Compass CI onboarding automations (top delivery focus)**
   - Reduce manual onboarding effort through pipeline/template automation.
2. **Support team enablement and training on Compass CI**
   - Run training/demo sessions and capture troubleshooting runbooks in the playbook.
3. **Client onboarding/migration aligned to GitLab migrations**
   - Prioritize client teams with active or upcoming GitLab Dedicated migration dates.
      - Tracker spreadsheet of existing groups/apps using Argo is [here](https://medtronic.sharepoint.com/:x:/r/sites/WebDevModernization/Shared%20Documents/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)/Compass%20CI/Migration%20Planning/argo-group-projects-for-migration.xlsx?d=wf25c99e20ba14df880cb50b4940690fe&csf=1&web=1&e=v9dmJI)
   - **Jose Chaves** (jose.chaves@medtronic.com) is the point of contact for GitLab Dedicated migration schedules and team migration dates.
   - [Runbook](https://compass-ci.medtronic.com/references/platform-onboarding-runbook/) for onboarding a new GitLab group to the pipeline and our responsibilities (group-level variable setups, token creation, etc.)
   - [Argo > Compass Migration](https://compass-ci.medtronic.com/references/argo-to-compass-migration-plan/) details end-to-end steps for migrations.
      1. [Video Walkthrough](https://medtronic.sharepoint.com/:v:/r/sites/WebDevModernization/Shared%20Documents/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)/Compass%20CI/Demos%20and%20Recordings/Group%20Variable%20and%20Token%20Automation.mp4?csf=1&web=1&e=0cQYBc) outlining Group CI/CD variables and tokens for [Migration Phase 0](https://compass-ci.medtronic.com/references/argo-to-compass-migration-plan/#platform-team-responsibilities)
         - Relevant section: `0:00` - `20:00` (platform team variable & token setup)
      2. [Video Walkthrough](https://medtronic.sharepoint.com/:v:/r/sites/WebDevModernization/Shared%20Documents/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)/Compass%20CI/Demos%20and%20Recordings/End-to-End%20Project%20Migration.mp4?csf=1&web=1&e=sleTjJ) of [Migration Phase 1](https://compass-ci.medtronic.com/references/argo-to-compass-migration-plan/#phase-1-repository-modernization-and-automation-prerequisites)
         - Relevant sections:
            - `0:20:33` - `1:18:00` (app repo setup)
            - `1:24:00` - `1:29:00` (webhook setup)
      2. [Video Walkthrough](https://medtronic.sharepoint.com/:v:/r/sites/WebDevModernization/Shared%20Documents/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)/Compass%20CI/Demos%20and%20Recordings/End-to-End%20Project%20Migration.mp4?csf=1&web=1&e=sleTjJ) of [Migration Phase 2](https://compass-ci.medtronic.com/references/argo-to-compass-migration-plan/#phase-1-repository-modernization-and-automation-prerequisites)
         - Relevant section: `1:05:00` - `1:18:00` (flux env registration)
      3. [Video Walkthrough](https://medtronic.sharepoint.com/:v:/r/sites/WebDevModernization/Shared%20Documents/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)/Compass%20CI/Demos%20and%20Recordings/End-to-End%20Project%20Migration.mp4?csf=1&web=1&e=sleTjJ) of [Migration Phase 3](https://compass-ci.medtronic.com/references/argo-to-compass-migration-plan/#phase-3-k8s-parity-mapping-and-migration)
         - Relevant section: `1:18:00` - `1:24:00` (k8s file migration)
      4. [Video Walkthrough](https://medtronic.sharepoint.com/:v:/r/sites/WebDevModernization/Shared%20Documents/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)/Compass%20CI/Demos%20and%20Recordings/End-to-End%20Project%20Migration.mp4?csf=1&web=1&e=sleTjJ) of [Migration Phase 4](https://medtronic.sharepoint.com/:v:/r/sites/WebDevModernization/Shared%20Documents/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)/Compass%20CI/Demos%20and%20Recordings/Secrets%20Base64%20to%20SOPs%20Conversion.mp4?csf=1&web=1&e=Sfsbrp)
         - Relevant section: `1:31:00` - `1:36:00`
4. **Dynatrace replacement for Grafana + onboarding automation**
   - Replicate dashboards in Dynatrace and automate future onboarding/setup.
5. **Manufacturing POC enablement (lowest priority / nice-to-have)**
   - Coordinate with **Anthony Leis** (anthony.leis@medtronic.com) if Manufacturing wants to pilot Compass CI.
   - Start small with a single job/template POC to validate fit and support model before expanding scope.
   - Only pull this work after support coverage, onboarding automations, and active client migration commitments are on track.

### Operating Rhythm (Daily/Weekly)

1. Triage support queue and escalations first; assign clear owner and backup.
2. Select backlog stories by JIRA priority in this order: **0-1**, then **2**, then **3+**.
3. Within each priority level, choose work from the initiative buckets above.
4. In weekly sync, mark each goal as **On Track**, **At Risk**, or **Blocked**, with owner and next action.

### Goals by Return Date (10/23)

1. **Onboarding cycle time:** Standard onboarding for a new app/app environment is consistently **< 1 business day**.
2. **Dynatrace transition complete:** Existing Grafana dashboards are replicated in Dynatrace; access is granted/communicated; docs are updated to remove Grafana references and include Dynatrace access steps.
3. **Argo-to-Compass parity achieved:** All current Argo pipeline capabilities have an equivalent implemented in Compass CI, including app/env onboarding, AWS certificate creation, chargebacks, and related operational workflows. Once equivalent capabilities are in place, we can continue to build out new features, enhancements, or additional automations to reduce manual burden on us and the support team.
4. **Support readiness complete:** Support team is trained on Compass CI and has required access to GitLab Dedicated, Terraform Cloud, and Dynatrace.
5. **Testing standard enforced:** All new or changed automations/job templates include associated test coverage (unit, integration, or pipeline validation as appropriate) to ensure changes to our pipeline does not break or have any negative impacts on client teams.

---

## Points of Contact

### Internal Escalation Path
1. Escalate within support resources internally first -> **Urrea, William** and **Mosquera, Juan** are support team leads.
2. If Argo team needs to be engaged -> **Podili, Sravan Kumar** to coordinate and assign within the Argo team.
3. For BCP Web team approvals, you may have to manually add users through the directly in GitLab or submit tickets to `Infra-SourceCode-Global`.
4. For approval access to Argo AWS accounts (restricted to our team, CapGemini, and CTS): **Campbell, Kevin** can approve in my absence.
   - For CTS, they may be able to use the link to trigger the automated approval response: https://code.medtronic.com/bcp_web/common/Playbook/-/blob/develop/supportingDocs/onboarding/Support.md?ref_type=heads
   - If that does not work, they need approval from Kevin.
5. If additional resources or escalation are required -> **Tadikamalla, Venkata Prathibha** and/or **Mahlum, David**.
   - In a true emergency only, **Mahlum, David** can reach me directly. I may not have network access and will be unable to actively assist.
6. **RITM approval requests** (e.g., Grafana access requests) that normally route to me for approval can be re-routed to **Mahlum, David** in my absence. The submitting user must fill out the [Approval Delegation form](https://medtronicprod.service-now.com/it?id=sc_cat_item&sys_id=a3eab70b1bcef45010958734604bcb2f) for their RITM to redirect the approval.
7. Additional team contacts:
   - https://compass-ci.medtronic.com/troubleshooting-and-support/contacts/
7. For Manufacturing POC interest in Compass CI -> coordinate with **Anthony Leis** (anthony.leis@medtronic.com).
   - Keep scope intentionally small to start (single job/template).
   - Treat as lowest-priority initiative until support, automation readiness, and active migrations are stable.

---

## Team Responsibilities

- Route day-to-day questions and issue coordination through Sravan.
- If ownership is unclear, assign owner and backup the same day.
- Raise blockers early instead of waiting for work to stall.

---

## Key Resources

- [Compass CI Playbook](https://compass-ci.medtronic.com)
  - Canonical documentation and onboarding guidance.
- [New Cluster Onboarding Checklist](../docs/platform-inventory/index.md#adding-a-new-cluster)
  - Source of truth for cluster onboarding steps in the playbook.
- [New Cluster and TF Approval Bot Runbook](../docs/references/new-cluster-and-tf-approval-bot-runbook.md)
  - Reference for Terraform MR auto-approval and auto-merge bot setup (Part 2).
- [Semantic Release Project](https://medtronic.gitlab-dedicated.com/bcp_web/devops/semantic-release)
  - Source for Compass CI pipeline job templates.
- [flux-gitops](https://medtronic.gitlab-dedicated.com/bcp_web/devops/fluxconfigs/flux-gitops)
  - GitOps source of truth for app registration and reconciliation manifests.
- [Einstein Project](https://medtronic.gitlab-dedicated.com/bcp_web/common/einstein)
  - Fully migrated reference application.
  - Additional pilot examples: [Contract GPT ReactJS](https://medtronic.gitlab-dedicated.com/PC_AI_Lab/contract-gpt-reactjs), [Contract GPT Backend](https://medtronic.gitlab-dedicated.com/PC_AI_Lab/contract-gpt-backend).
- [Compass CI Demos / Working Session Recordings](https://medtronic.sharepoint.com/:f:/r/sites/WebDevModernization/Shared%20Documents/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)/Compass%20CI?csf=1&web=1&e=bR6X5j)
- [Compass CI Marketing Slide Deck](https://medtronic.sharepoint.com/:p:/r/sites/WebDevModernization/Shared%20Documents/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)/Compass%20CI/Marketing/Compass-CI-Marketing-Deck-Branded.pptx?d=wae80d361eb2f418fb39abffa7d89366f&csf=1&web=1&e=9HlQoK)

---

## AI Usage (Quick Guidance)

- Use Copilot first for troubleshooting, cross-repo flow analysis, and draft implementation plans before escalating.
- Keep related repos open in one workspace for better context.
- For detailed setup and prompt examples, use the Compass CI Playbook tooling/access section.

---


## Transcription of the Meetings
Group Variable and Token Automation

0:04
All right.

0:05
So I was going to record a session on kind of the automations that I just finished up for setting the group level CICD variables as well as the group specific tokens that are required in order to use our new pipeline.

0:31
And so I'm in the process of deploying these changes to the new playbook, but they'll exist under the references platform and support onboarding runbook.

0:45
So in terms of what's changed in here, the first two steps are still the same.

0:51
We've got a link here on the videos for those steps.

0:55
I'll link this video once we get to the step three process.

1:00
So updates are really around step three and four and kind of some automations that are now available to make that a little bit easier.

1:10
So in terms of how to help teams configure their group level CICD variables, we've got some new Python scripts that can be run from one of our local machines to do that.

1:27
Maybe eventually we'll put together kind of a Git lab version of being able to run that, but for now, just a simple run from us or the support team in the future from our local machines to set it up.

1:42
So there's a little expandable shin here that calls out kind of the prerequisites of being able to run this these two scripts locally on your machine.

1:55
So it requires Python as well as the AWSCLI and has some examples of validation checks you can run to verify that those two things are installed.

2:08
So make sure you've got those downloaded and installed correctly on your machine first.

2:16
And I put a note here that you know, in terms of Step 3, for the group level CICD variables, they can be set at the highest level of the group to kind of avoid any duplication and take advantage of the inheritance inheritance that GitLab has for the subgroups.

2:36
And then teams can always override those variables at those subgroup or project levels if they need to.

2:42
So in terms of how to actually use it, there's one script for the group level CICD variables and a separate script for the actual tokens that'll be set.

2:55
And that's there's a note on the tokens piece, but that's because, you know, we may wanna set the tokens at a lower level than the these shared CICD variables.

3:08
So to run the shared variables automation, you'll want to clone our semantic release repository on your local machine 'cause that's where the scripts are.

3:23
So I can show what that looks like here.

3:29
So I've got the semantic release project cloned.

3:32
You'll see there's a script section and underneath that there's a group vars section that's got our two scripts inside of it.

3:44
So you'll need to clone that and have a terminal to it.

3:47
So in my case, I've got a bash terminal here for semantic release.

3:54
If you don't have a terminal open, you can hit the + in Visual Studio, and I usually use the Git Bash terminal as my default, so he'll open that.

4:11
You'll need to pull your temporary credentials for the Argo dev account from the AWS start page, and that is because it's going to go out to AWS to pull the default variable values from a secret that's inside of AWS.

4:31
So I put that secret inside our dev account.

4:35
So that's the one you'll wanna pull the credentials for.

4:41
So I'll open my portal start page, grab my access keys and set them in my terminal.

5:02
OK, so that's done.

5:04
And if we go look at the Secrets Manager console for that dev account, you'll see here there's a secret in here called Compass CI Onboarding Group Vars.

5:22
So if we were to ever need to update any values on the secrets that are in here, this is the secret to update them on.

5:31
And then the next time you run this automation script, it will pull the latest values from here.

5:37
So if we look at what's actually in here, it's got basically a Jason input of variables.

5:46
So it's got the name.

5:48
So this will be what the variable key is inside of GitLab.

5:52
When it sets it, it sets a description, it sets the value, and then it's got kind of the settings for what should be done with it inside of GitLab.

6:02
Whether it's a visible variable, whether it's a masked and hidden variable or just masked.

6:11
Those are the available settings for visibility, whether it's protected or not, if it needs to be expanded or not, and then scopes if it applies.

6:23
So for most of these, the scope is always just star.

6:26
It applies to every scope that a team might have, but in terms of certain ones like the external ID and roll arns, those are have specific scopes set on them and so you can kind of customize it.

6:47
It should, you know, basically be able to to handle any settings you need to set for creating variables.

6:56
And this is kind of the full set of the ones that are needed in order to onboard a team.

7:02
So if you need to update them, you'll have to do it inside the secret in AWS to do that.

7:09
So once we've set those credentials, which we just did, you'll need to have the an owner of the group that's onboarding create a group access token for their group.

7:25
So in this run through, I worked with someone from the CIM group and had them create a token.

7:36
I told them they could set it with a short expiration.

7:39
We just need it to be valid enough to run these for long enough to run these automation scripts.

7:44
And then you know, if it expires, that's completely fine.

7:48
So it shouldn't have kind of an owner permission.

7:52
Basically have the scope of API so that it can generate all these CICD variables on its own.

8:01
So then you'll need to take that token that the team gives you and you'll set it inside your terminal as well.

8:15
So I am gonna paste one in for example and in my example I am running this for the CIM group.

8:28
So this target group path should match their group name inside of GitLab.

8:37
So take that from the path for them and set it with using that.

8:47
So in this case their group name is just CIM.

8:52
So that's what I'll set it to.

8:58
So we've set the group token and target path and then we'll we can run the shared variables script.

9:10
And as it calls out here, the first run of it is run with a dry run.

9:18
So it'll just output kind of what it's going to change, if anything.

9:24
And then you can run it again with dry run false to basically do the apply of whatever changes.

9:30
So you can kind of preview exactly what it's going to do before it actually runs any changes to that group.

9:39
And you'll see here it also takes in the name of that variable.

9:43
So in this case, I've already actually run the apply.

9:47
So it should just kind of come back with, hey, all the variables exist already and that basically it's not going to do any changes to that group.

9:55
So let's run it with the dry run true.

9:59
And you'll see it's using those AWS credentials to kind of authenticate to the AWS account and grab that secret we need to look at.

10:17
And in this case, it's going to see that basically all of these keys already exist in the target group, so it's going to skip them.

10:26
And so you'll see the summary here just says it's skipped 32 items.

10:33
So if there was a gap and just one of these didn't exist, for example, it would, you know, show that maybe 31 exists and it would be skipped and one would be created so that you kind of have that preview of exactly what's going to change.

10:50
And then like I said, to actually apply them, you would run this with dry run false, and that would actually apply the changes.

11:03
But in this case, it wouldn't apply any changes because all of them already exist.

11:07
But you may need to do that.

11:08
Say if you update one of the variables and we need to apply it to a group, you would want to run that with DRY RUN false.

11:18
It also is smart enough to not overwrite any values if they exist.

11:25
So if you did update a value and you know you need to overwrite the existing variables.

11:32
I've got a kind of expandable section here that specifically talks about overwrite scenarios.

11:41
So in that case, there's a additional input here called allow overwrite, which you would set to true and then it would prompt you before it allows that overwrite.

11:55
So we could just kind of look at that and then ignore.

12:00
So let's just paste that in and kind of show an example of what that looks like.

12:12
So here it's going to say overwrite mode is enabled.

12:16
Existing variables might be updated in place, and you have to type in overwrite to continue.

12:23
But in this case, I'm just going to type C to cancel.

12:27
So just kind of gives a confirmation that it's going to make changes if you were to enable that overwrite mode.

12:35
But in this case, we confirmed all the variables are there and exist, so it's not going to create any.

12:45
And then once you've actually run it with that dry run false and applied all the variables, you can have the onboarding team kind of confirm under their group level that those variables exist.

12:59
And since we aren't admins, we, you know, won't be able to necessarily do that check ourselves.

13:04
So that's why we have them kind of do that confirmation as it kind of calls out here.

13:11
The shared variable values are stored in that onboarding group bars secret in AWS.

13:19
So if you needed to add or update those defaults for future runs, then you have to go update that payload in the AWS secret and then rerun that script to update the values.

13:37
So then similarly moving on from the kind of shared group level variables, the couple tokens that are needed for different automations that we have, there's a separate script for that piece.

13:57
And that's the reason for that is basically what I talked about earlier.

14:02
So I've got a note here, you know, just depending on the size and structure of the group that's onboarding, you know, we might want to provision those tokens at a subgroup level.

14:13
So for example, for BCP web, you know, we've got a lot of different groups underneath here that are kind of segregated.

14:26
You know, they aren't really owned by the same teams necessarily.

14:30
And so, you know, they may want, we may want to separate tokens based on, you know, the, the subgroup level instead of just one token for BCP web.

14:45
So you know, we might have in this case 1 token for the clinical.

14:51
We might run it one time for the clinical subgroup, one time for the commercial subgroup, common DevOps, etcetera.

14:59
And similarly for some other groups like they don't exist yet in the new Git lab instance.

15:10
But another kind of good example of that would probably be the quality Web Apps group.

15:17
So specifically for example, Emanuels has a whole set of applications for themselves and then the rest of these are kind of stand alone projects.

15:29
So we may want to create those tokens for Emanuels separately from these other projects.

15:37
So the tokens for that reason can be run at a different level than the than the shared CICD variables.

15:52
So in this case for the CIM group, it's kind of a small team.

15:58
They have a small set of projects that's all the same team that's working on those projects.

16:03
So it's safe to kind of create the tokens at their top level group.

16:10
So in order to run the token script, there's a couple variables that you need to have.

16:19
So you need a token that can create tokens on both our Flux repo and our Terraform group.

16:33
So from what I've tested, those should be a personal access token.

16:43
So I created one earlier called Compass CI Onboarding Group Token so you can create your own.

16:51
Grab the variable value and you will set that as a parameter when we're running the token script.

17:06
It can be the same between these two if you know the person that's that's executing.

17:11
This has the right permissions on both of these projects, which I think we all should have if we're owners of the ECP web group, you need the Terraform Cloud organization.

17:27
In this case, for us, it's MDTIT PROD.

17:31
You need the Terraform Cloud team ID and it should look something like team dash and an identifier since it's kind of, you know, something we we won't want to expose to teams.

17:45
I didn't write the actual value inside of this playbook page.

17:49
So in Terraform Cloud, you can click on the Teams page and I'm not logged in, so I'll log in and if I click on the team, in this case, it would be the IT Argo Admins team.

18:16
You'll see in the URL bar up here.

18:18
This is the basically the team identifier that we need.

18:23
So that's what we'll need in order to put into the parameters for this token run.

18:32
And then you'll need an actual Terraform Cloud team API token.

18:37
And so for these, I just when you run, need to run it for a new group.

18:44
For example, first CIMI just created a new temp token.

18:50
And I just, you know, put a description that it's a temp token for onboarding the CIM group.

18:54
And I gave it a very short expiration of one week.

18:58
So create a new team token, tell it that it's a temp token for onboarding whatever group it is.

19:07
And then, you know, I mean, you could even set it to a very even shorter expiration, just needs to be valid long enough to run the scripts.

19:15
So once this script runs, it will set these other three tokens on that group that you run it against.

19:28
So same as the above step, we have the the target group path and group token, which we've already got set in our terminal because I'm just gonna use the same terminal I already set those in.

19:41
And then we need our personal access token for these two.

19:48
Like I said, they can be the same value depending on you know your level of access, but I think for us we should all have be able to use the same token.

20:00
For those you need your Terraform Cloud admin token, the temporary one we created and you need that team ID.

20:08
So I will copy in my values for these.

20:25
See team Oh, I'm going to paste those into my terminal and then I'm going to go back to the instructions.

20:51
And here's where we can execute a dry run of the token generation.

21:03
And you'll see here in this case, I've already run it.

21:06
So it went and checked that the group already actually has all three of those tokens set, so it was going to skip them.

21:18
But for a brand new team it would just say it was going to create 3 tokens.

21:22
So again, you can run it then in apply mode to apply those changes.

21:30
And if we needed to overwrite or rotate those tokens, maybe they expired and the team needs new tokens, you'll just need them to provide, you know, a new group token for their group and grab all those variables again.

21:49
And then you can, similarly to the last one, set that allow overwrite to true and that would regenerate the tokens or rotate them and reset them on their group.

22:07
So once you've basically run those two scripts with that DRY run false, it should create all the variables they need.

22:17
Then in order to, you know, have them fully run the rest of their automations for onboarding their applications and any other job templates that require those tokens and variables.

Group Variable and Token Automation

0:04
All right.

0:05
So I was going to record a session on kind of the automations that I just finished up for setting the group level CICD variables as well as the group specific tokens that are required in order to use our new pipeline.

0:31
And so I'm in the process of deploying these changes to the new playbook, but they'll exist under the references platform and support onboarding runbook.

0:45
So in terms of what's changed in here, the first two steps are still the same.

0:51
We've got a link here on the videos for those steps.

0:55
I'll link this video once we get to the step three process.

1:00
So updates are really around step three and four and kind of some automations that are now available to make that a little bit easier.

1:10
So in terms of how to help teams configure their group level CICD variables, we've got some new Python scripts that can be run from one of our local machines to do that.

1:27
Maybe eventually we'll put together kind of a Git lab version of being able to run that, but for now, just a simple run from us or the support team in the future from our local machines to set it up.

1:42
So there's a little expandable shin here that calls out kind of the prerequisites of being able to run this these two scripts locally on your machine.

1:55
So it requires Python as well as the AWSCLI and has some examples of validation checks you can run to verify that those two things are installed.

2:08
So make sure you've got those downloaded and installed correctly on your machine first.

2:16
And I put a note here that you know, in terms of Step 3, for the group level CICD variables, they can be set at the highest level of the group to kind of avoid any duplication and take advantage of the inheritance inheritance that GitLab has for the subgroups.

2:36
And then teams can always override those variables at those subgroup or project levels if they need to.

2:42
So in terms of how to actually use it, there's one script for the group level CICD variables and a separate script for the actual tokens that'll be set.

2:55
And that's there's a note on the tokens piece, but that's because, you know, we may wanna set the tokens at a lower level than the these shared CICD variables.

3:08
So to run the shared variables automation, you'll want to clone our semantic release repository on your local machine 'cause that's where the scripts are.

3:23
So I can show what that looks like here.

3:29
So I've got the semantic release project cloned.

3:32
You'll see there's a script section and underneath that there's a group vars section that's got our two scripts inside of it.

3:44
So you'll need to clone that and have a terminal to it.

3:47
So in my case, I've got a bash terminal here for semantic release.

3:54
If you don't have a terminal open, you can hit the + in Visual Studio, and I usually use the Git Bash terminal as my default, so he'll open that.

4:11
You'll need to pull your temporary credentials for the Argo dev account from the AWS start page, and that is because it's going to go out to AWS to pull the default variable values from a secret that's inside of AWS.

4:31
So I put that secret inside our dev account.

4:35
So that's the one you'll wanna pull the credentials for.

4:41
So I'll open my portal start page, grab my access keys and set them in my terminal.

5:02
OK, so that's done.

5:04
And if we go look at the Secrets Manager console for that dev account, you'll see here there's a secret in here called Compass CI Onboarding Group Vars.

5:22
So if we were to ever need to update any values on the secrets that are in here, this is the secret to update them on.

5:31
And then the next time you run this automation script, it will pull the latest values from here.

5:37
So if we look at what's actually in here, it's got basically a Jason input of variables.

5:46
So it's got the name.

5:48
So this will be what the variable key is inside of GitLab.

5:52
When it sets it, it sets a description, it sets the value, and then it's got kind of the settings for what should be done with it inside of GitLab.

6:02
Whether it's a visible variable, whether it's a masked and hidden variable or just masked.

6:11
Those are the available settings for visibility, whether it's protected or not, if it needs to be expanded or not, and then scopes if it applies.

6:23
So for most of these, the scope is always just star.

6:26
It applies to every scope that a team might have, but in terms of certain ones like the external ID and roll arns, those are have specific scopes set on them and so you can kind of customize it.

6:47
It should, you know, basically be able to to handle any settings you need to set for creating variables.

6:56
And this is kind of the full set of the ones that are needed in order to onboard a team.

7:02
So if you need to update them, you'll have to do it inside the secret in AWS to do that.

7:09
So once we've set those credentials, which we just did, you'll need to have the an owner of the group that's onboarding create a group access token for their group.

7:25
So in this run through, I worked with someone from the CIM group and had them create a token.

7:36
I told them they could set it with a short expiration.

7:39
We just need it to be valid enough to run these for long enough to run these automation scripts.

7:44
And then you know, if it expires, that's completely fine.

7:48
So it shouldn't have kind of an owner permission.

7:52
Basically have the scope of API so that it can generate all these CICD variables on its own.

8:01
So then you'll need to take that token that the team gives you and you'll set it inside your terminal as well.

8:15
So I am gonna paste one in for example and in my example I am running this for the CIM group.

8:28
So this target group path should match their group name inside of GitLab.

8:37
So take that from the path for them and set it with using that.

8:47
So in this case their group name is just CIM.

8:52
So that's what I'll set it to.

8:58
So we've set the group token and target path and then we'll we can run the shared variables script.

9:10
And as it calls out here, the first run of it is run with a dry run.

9:18
So it'll just output kind of what it's going to change, if anything.

9:24
And then you can run it again with dry run false to basically do the apply of whatever changes.

9:30
So you can kind of preview exactly what it's going to do before it actually runs any changes to that group.

9:39
And you'll see here it also takes in the name of that variable.

9:43
So in this case, I've already actually run the apply.

9:47
So it should just kind of come back with, hey, all the variables exist already and that basically it's not going to do any changes to that group.

9:55
So let's run it with the dry run true.

9:59
And you'll see it's using those AWS credentials to kind of authenticate to the AWS account and grab that secret we need to look at.

10:17
And in this case, it's going to see that basically all of these keys already exist in the target group, so it's going to skip them.

10:26
And so you'll see the summary here just says it's skipped 32 items.

10:33
So if there was a gap and just one of these didn't exist, for example, it would, you know, show that maybe 31 exists and it would be skipped and one would be created so that you kind of have that preview of exactly what's going to change.

10:50
And then like I said, to actually apply them, you would run this with dry run false, and that would actually apply the changes.

11:03
But in this case, it wouldn't apply any changes because all of them already exist.

11:07
But you may need to do that.

11:08
Say if you update one of the variables and we need to apply it to a group, you would want to run that with DRY RUN false.

11:18
It also is smart enough to not overwrite any values if they exist.

11:25
So if you did update a value and you know you need to overwrite the existing variables.

11:32
I've got a kind of expandable section here that specifically talks about overwrite scenarios.

11:41
So in that case, there's a additional input here called allow overwrite, which you would set to true and then it would prompt you before it allows that overwrite.

11:55
So we could just kind of look at that and then ignore.

12:00
So let's just paste that in and kind of show an example of what that looks like.

12:12
So here it's going to say overwrite mode is enabled.

12:16
Existing variables might be updated in place, and you have to type in overwrite to continue.

12:23
But in this case, I'm just going to type C to cancel.

12:27
So just kind of gives a confirmation that it's going to make changes if you were to enable that overwrite mode.

12:35
But in this case, we confirmed all the variables are there and exist, so it's not going to create any.

12:45
And then once you've actually run it with that dry run false and applied all the variables, you can have the onboarding team kind of confirm under their group level that those variables exist.

12:59
And since we aren't admins, we, you know, won't be able to necessarily do that check ourselves.

13:04
So that's why we have them kind of do that confirmation as it kind of calls out here.

13:11
The shared variable values are stored in that onboarding group bars secret in AWS.

13:19
So if you needed to add or update those defaults for future runs, then you have to go update that payload in the AWS secret and then rerun that script to update the values.

13:37
So then similarly moving on from the kind of shared group level variables, the couple tokens that are needed for different automations that we have, there's a separate script for that piece.

13:57
And that's the reason for that is basically what I talked about earlier.

14:02
So I've got a note here, you know, just depending on the size and structure of the group that's onboarding, you know, we might want to provision those tokens at a subgroup level.

14:13
So for example, for BCP web, you know, we've got a lot of different groups underneath here that are kind of segregated.

14:26
You know, they aren't really owned by the same teams necessarily.

14:30
And so, you know, they may want, we may want to separate tokens based on, you know, the, the subgroup level instead of just one token for BCP web.

14:45
So you know, we might have in this case 1 token for the clinical.

14:51
We might run it one time for the clinical subgroup, one time for the commercial subgroup, common DevOps, etcetera.

14:59
And similarly for some other groups like they don't exist yet in the new Git lab instance.

15:10
But another kind of good example of that would probably be the quality Web Apps group.

15:17
So specifically for example, Emanuels has a whole set of applications for themselves and then the rest of these are kind of stand alone projects.

15:29
So we may want to create those tokens for Emanuels separately from these other projects.

15:37
So the tokens for that reason can be run at a different level than the than the shared CICD variables.

15:52
So in this case for the CIM group, it's kind of a small team.

15:58
They have a small set of projects that's all the same team that's working on those projects.

16:03
So it's safe to kind of create the tokens at their top level group.

16:10
So in order to run the token script, there's a couple variables that you need to have.

16:19
So you need a token that can create tokens on both our Flux repo and our Terraform group.

16:33
So from what I've tested, those should be a personal access token.

16:43
So I created one earlier called Compass CI Onboarding Group Token so you can create your own.

16:51
Grab the variable value and you will set that as a parameter when we're running the token script.

17:06
It can be the same between these two if you know the person that's that's executing.

17:11
This has the right permissions on both of these projects, which I think we all should have if we're owners of the ECP web group, you need the Terraform Cloud organization.

17:27
In this case, for us, it's MDTIT PROD.

17:31
You need the Terraform Cloud team ID and it should look something like team dash and an identifier since it's kind of, you know, something we we won't want to expose to teams.

17:45
I didn't write the actual value inside of this playbook page.

17:49
So in Terraform Cloud, you can click on the Teams page and I'm not logged in, so I'll log in and if I click on the team, in this case, it would be the IT Argo Admins team.

18:16
You'll see in the URL bar up here.

18:18
This is the basically the team identifier that we need.

18:23
So that's what we'll need in order to put into the parameters for this token run.

18:32
And then you'll need an actual Terraform Cloud team API token.

18:37
And so for these, I just when you run, need to run it for a new group.

18:44
For example, first CIMI just created a new temp token.

18:50
And I just, you know, put a description that it's a temp token for onboarding the CIM group.

18:54
And I gave it a very short expiration of one week.

18:58
So create a new team token, tell it that it's a temp token for onboarding whatever group it is.

19:07
And then, you know, I mean, you could even set it to a very even shorter expiration, just needs to be valid long enough to run the scripts.

19:15
So once this script runs, it will set these other three tokens on that group that you run it against.

19:28
So same as the above step, we have the the target group path and group token, which we've already got set in our terminal because I'm just gonna use the same terminal I already set those in.

19:41
And then we need our personal access token for these two.

19:48
Like I said, they can be the same value depending on you know your level of access, but I think for us we should all have be able to use the same token.

20:00
For those you need your Terraform Cloud admin token, the temporary one we created and you need that team ID.

20:08
So I will copy in my values for these.

20:25
See team Oh, I'm going to paste those into my terminal and then I'm going to go back to the instructions.

20:51
And here's where we can execute a dry run of the token generation.

21:03
And you'll see here in this case, I've already run it.

21:06
So it went and checked that the group already actually has all three of those tokens set, so it was going to skip them.

21:18
But for a brand new team it would just say it was going to create 3 tokens.

21:22
So again, you can run it then in apply mode to apply those changes.

21:30
And if we needed to overwrite or rotate those tokens, maybe they expired and the team needs new tokens, you'll just need them to provide, you know, a new group token for their group and grab all those variables again.

21:49
And then you can, similarly to the last one, set that allow overwrite to true and that would regenerate the tokens or rotate them and reset them on their group.

22:07
So once you've basically run those two scripts with that DRY run false, it should create all the variables they need.

22:17
Then in order to, you know, have them fully run the rest of their automations for onboarding their applications and any other job templates that require those tokens and variables.

