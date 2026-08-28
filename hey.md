Mostly yes, but I would not approve it exactly as written. The code cleanup itself matches the agreed resolution; the verification/guardrail section has two important problems.

The cleanup is correct:

Keep the original ciam_cic_profilemanagement_sops_key_value variable and the existing sops_age_key_ciam_cic_profilemanagement secret/version.

Remove the generated duplicate variable and the generated sops_age_key_cic_profilemanagement secret/version.

Keep the IRSA baseline.

Keep the # END AUTOGEN: APP_BASELINE marker.

Adding a comment explaining why SOPS is intentionally absent from the generated block is a good idea.


That also aligns with the platform's SOPS model: Flux depends on the Age key to decrypt SOPS-managed secrets, so avoiding unintended rotation/replacement is important. 

The first problem is this sentence:

> “the plan should show ... only the ACM certificate and Route53 validation records from !250.”



That expectation conflicts with retaining the IRSA baseline.

If MR !249 introduced these new resources:

aws_iam_policy.compass_ci_cic_profilemanagement_nonprod_policy
module.eks_oidc_nonprod_cic_profilemanagement

and Terraform has been unable to plan/apply since !249 was merged, then those resources may not exist in state yet.

Therefore after fixing the syntax error, Terraform may legitimately show something like:

+ aws_iam_policy.compass_ci_cic_profilemanagement_nonprod_policy
+ module.eks_oidc_nonprod_cic_profilemanagement...
+ aws_acm_certificate...
+ Route53/certificate-related resources...

That does not automatically mean the cleanup is wrong.

The actual guardrail should be:

There must be NO create/update/delete/replace actions against the
existing authoritative SOPS Secrets Manager resources.

Expected plan changes may include:
- the retained IRSA baseline from !249, if not already applied
- certificate/DNS resources from !250

This is the most important correction.

The second problem is more serious:

> “If the plan does show any aws_secretsmanager_secret* action ... it would indicate the duplicate had partially landed in state and needs a terraform state rm”



No. Do not automatically run terraform state rm.

That conclusion is too aggressive.

A duplicate Terraform variable is a configuration-validation error. Terraform must successfully load and validate the configuration before it can create a plan or apply it. So if every run after !249 failed on:

Duplicate variable declaration

the duplicated block from !249 could not have been “partially applied” by those failed runs.

If Secrets Manager appears in the next plan, stop, yes—but investigate why.

Possible causes include:

Configuration drift
State/config mismatch
A previously existing state entry
A renamed Terraform resource address
Manual AWS changes
Secret-version differences
Provider-generated differences
Resources created before !249

terraform state rm does not delete the AWS secret. It tells Terraform:

> “Forget that Terraform owns this resource.”



If the configuration still declares that secret afterward, Terraform can then attempt to create it again. For a production/shared SOPS key, that is exactly the kind of action you do not want.

Instead, if secrets appear in the plan, first inspect state:

terraform state list | grep -E \
'sops_age_key|ciam_cic_profilemanagement|cic_profilemanagement'

You want to find the authoritative addresses, presumably something similar to:

aws_secretsmanager_secret.sops_age_key_ciam_cic_profilemanagement

aws_secretsmanager_secret_version.sops_age_key_ciam_cic_profilemanagement_version

You should not expect the removed generated addresses:

aws_secretsmanager_secret.sops_age_key_cic_profilemanagement

aws_secretsmanager_secret_version.sops_age_key_cic_profilemanagement_version

If one unexpectedly exists in state, then determine what real AWS resource it points to before modifying state.

For example:

terraform state show \
  aws_secretsmanager_secret.sops_age_key_cic_profilemanagement

Only after understanding that mapping should anyone consider state rm, state mv, or import.

The difference is critical:

state rm
Terraform forgets a real resource.

state mv
Terraform keeps ownership but changes its Terraform address.

import
Terraform begins managing an existing real resource at an address.

You cannot decide which one is correct merely because a secret appears in the plan.

I would also add two verification steps before even looking at the full plan.

First:

terraform fmt -check
terraform validate

The original blocker is a configuration error, so terraform validate should demonstrate that the duplicate declaration is actually gone.

Then search specifically:

grep -n \
'variable "ciam_cic_profilemanagement_sops_key_value"' \
app_ciam_cic-profilemanagement.tf

Expected:

exactly 1 result

And:

grep -n 'sops_age_key_cic_profilemanagement' \
app_ciam_cic-profilemanagement.tf

Expected:

0 results

Be careful with that second search because the correct resource contains:

sops_age_key_ciam_cic_profilemanagement
                  ^^^^

while the removed one is:

sops_age_key_cic_profilemanagement

They're annoyingly similar.

The explanatory note is conceptually good, but I would clean it up slightly. The version you pasted has formatting artifacts:

cic_profilemanag ement
...
non-prod I # environments
...
MR 1249

It should say MR !249, not MR 1249.

I would use:

# NOTE: SOPS configuration for this application is intentionally not defined
# in this APP_BASELINE block.
#
# The authoritative SOPS resources are defined earlier in this file:
# - variable.ciam_cic_profilemanagement_sops_key_value
# - aws_secretsmanager_secret.sops_age_key_ciam_cic_profilemanagement
# - aws_secretsmanager_secret_version.sops_age_key_ciam_cic_profilemanagement_version
#
# They manage compass-ci/ciam/cic-profilemanagement/sops-age-key for the
# application's non-production environments (dev and testing).
#
# The duplicate SOPS resources generated by the onboarding automation in
# MR !249 were removed because they duplicated the existing configuration
# and prevented Terraform configuration validation.

# END AUTOGEN: APP_BASELINE

One additional concern: putting a manually maintained note inside an AUTOGEN region may or may not survive the next onboarding automation run. The original defect happened precisely because the generator relies on markers. I would inspect how that automation handles content between its start/end markers. If it regenerates the entire section, your explanatory comment could disappear and, worse, SOPS might be generated again.

So my assessment is:

Removal of duplicate variable        CORRECT
Removal of duplicate secret          CORRECT
Removal of duplicate secret version  CORRECT
Keep original SOPS block              CORRECT
Keep IRSA baseline                    REASONABLE / review plan
Keep END AUTOGEN marker               CORRECT
Explanatory note                      GOOD, but check generator behavior
Repo-wide duplicate search            GOOD
"Only ACM/Route53 in plan"            TOO STRICT / likely incorrect
"Secret action => state rm"           INCORRECT / unsafe

The plan review is the decisive validation. If you send me the output of the next Terraform Plan—particularly the Plan: X to add, Y to change, Z to destroy section and the resource addresses—I can tell you line-by-line whether it is safe to apply.
