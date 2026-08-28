Cleanup applied to app_ciam_cic-profilemanagement.tf:

KEPT (authoritative, line ~265–294): aws_secretsmanager_secret.sops_age_key_ciam_cic_profilemanagement, variable "ciam_cic_profilemanagement_sops_key_value", and aws_secretsmanager_secret_version.sops_age_key_ciam_cic_profilemanagement_version managing compass-ci/ciam/cic-profilemanagement/sops-age-key.
REMOVED (generated APP_BASELINE duplicate, old lines ~521–529): the second variable "ciam_cic_profilemanagement_sops_key_value", aws_secretsmanager_secret.sops_age_key_cic_profilemanagement, and aws_secretsmanager_secret_version.sops_age_key_cic_profilemanagement_version. Replaced with an explanatory comment so the automation/reviewers know the SOPS config is intentionally centralized.
RETAINED: IRSA baseline — aws_iam_policy.compass_ci_cic_profilemanagement_nonprod_policy and module.eks_oidc_nonprod_cic_profilemanagement (unique names, standard per-app baseline), plus the # END AUTOGEN: APP_BASELINE marker.
Verification: a repo-wide search now shows only one declaration of ciam_cic_profilemanagement_sops_key_value and no remaining references to sops_age_key_cic_profilemanagement, so the duplicate-declaration error is resolved.

Guardrail check for the next plan (!250): since the removed resources were never applied (no state entries), the plan should show no Secrets Manager changes — no new secret versions, no destroys of the existing secret/replica — only the ACM certificate and Route53 validation records from !250. If the plan does show any aws_secretsmanager_secret* action, stop and investigate before applying (it would indicate the duplicate had partially landed in state and needs a terraform state rm, not an apply).

Claude Opus 5 • 1x

