# GitLab Migration + Semantic-Release Pipeline — Phased Rollout Plan

**Generated:** 2026-04-06

**Source data:** [argononprod](https://code.medtronic.com/bcp_web/devops/fluxconfigs/argononprod/-/tree/main?ref_type=heads) main branch (clean clone)

**Repos analyzed:** 47 unique GitLab repos across 20 groups/subgroups

**Migration instructions:** [Argo to Compass CI Migration Plan](https://compass-ci.medtronic.com/references/argo-to-compass-migration-plan)

---

## Presenting to Pilot / Target Teams

Before engaging any app team, share our Compass CI slide deck as background context:

> 📊 **[Compass CI — Marketing & Overview Deck](https://medtronic.sharepoint.com/:p:/r/sites/WebDevModernization/Shared%20Documents/WebDev%20Cloud%20Solutions%20(Prometheus%20Argo%20Project)/Compass%20CI/Marketing/Compass-CI-Marketing-Deck-Branded.pptx?d=wae80d361eb2f418fb39abffa7d89366f&csf=1&web=1&e=3H2M57)**
> *(SharePoint — Medtronic internal access required)*

### Key points to communicate to every target team

1. **Your repo is copied, not moved.**
   Repositories will be copied to `medtronic.gitlab-dedicated.com` — the existing repo on the current GitLab instance stays fully active throughout the migration. No code is deleted, renamed, or redirected until the team is ready and has signed off. If there is active development during the migration window, we can set up an automated sync job to keep branches and commits flowing from the old instance to the new one in real time, so nothing is lost and no one is blocked.

2. **Our team drives the migration — you mostly just need to be aware of what's changing.**
   The Argo/Compass CI team will drive the repository import, pipeline conversion, and project repo conversion. App teams do not need to perform any migration tasks themselves. What teams *do* need to be aware of:
   - **Branch-based deployment configuration:** deployments in the new instance are controlled by branch naming and pipeline rules defined in the Compass CI standard. This may differ from any manual trigger or environment-specific job they were used to on the old instance.
   - **GitLab Work items for vulnerability tracking:** security findings (container scans, SAST, dependency checks) will surface as GitLab Work items in the new instance rather than being emailed as separate reports. Teams should expect to triage these in GitLab going forward.
   - Any other team-specific questions or workflow differences will be surfaced and addressed during the hypercare period immediately following each wave.

3. **The hosting platform is not changing — only the delivery mechanism is.**
   The backend infrastructure that runs your application (Kubernetes cluster, ingress, networking, secrets, databases) is exactly the same. Nothing moves. There are no new firewall requests to submit, no DNS changes, no new connectivity approvals, and no infrastructure re-provisioning. The only thing changing is *how code changes get from your repository to that platform* — the CI/CD pipeline and the GitLab instance that runs it.

4. **Conventional commit messages are required for automatic versioning.**
   Semantic-release drives versioning purely from commit message format. Teams will need to adopt [Conventional Commits](https://www.conventionalcommits.org/) going forward — e.g. `feat: add user search` triggers a minor bump, `fix: correct timeout` triggers a patch. Breaking changes are flagged with `BREAKING CHANGE` in the commit footer. We will provide a `commitlint` config and a short guide; this is the most significant day-to-day workflow change teams should be prepared for. Existing historical commits do not need to change — only new commits from migration point forward.

5. **Your pipeline will become a thin include file, not a monolithic script.**
   The migrated `.gitlab-ci.yml` will shrink to a small file that `include`s reusable templates from the `bcp_web/devops/semantic-release` project (language-specific build, container scanning, Flux registration, etc.). This is intentional — bug fixes, security patches, and new platform features flow to all teams automatically from the shared template. Teams do not own or maintain the complex pipeline logic themselves. That said, the templates are fully extensible: any job can be overridden or extended at the project level to accommodate team-specific build steps, custom test stages, or additional deployment logic. This is a new capability over the old approach — teams get a maintained, standards-compliant baseline and still have the flexibility to customize where their app genuinely needs it.

6. **This upgrade improves Kubernetes portability for future hosting choices.**
   The new Compass CI + semantic-release model is designed around reusable Kubernetes/GitOps patterns rather than hard-coding to one platform implementation. That gives teams a cleaner path to target different cluster backends over time (for example AKS in Azure, GKE in GCP, or team-managed EKS) with less pipeline rework. Application manifests and environment config still need to be validated per target platform, but the delivery framework is intentionally more portable than the legacy setup.

7. **Standardized pipelines make AI-assisted operations easier and safer.**
   Because the upgraded pipeline uses shared templates, consistent job naming, and documented workflow patterns, approved internal AI assistants can help teams faster with routine updates, migration adjustments, and troubleshooting guidance. This reduces time-to-fix for common CI/CD issues while keeping normal governance intact: all changes still go through merge request review, branch protections, and team approval gates.

---

## Strategy: Stale-First, Active Teams Later

We target stale repos in early waves deliberately. An app with no deployments/changes in 200–400 days:

- Is a **frozen target** — zero risk of a mid-migration commit creating divergence or cherry-pick work
- Requires **no team coordination** — no freeze window, no sprint scheduling, no stakeholder comms during execution
- Has **low blast radius** — if something goes wrong on the new instance (bad variable, missing runner tag, wrong branch protection), nobody's release is blocked
- Gives us **iteration room** — Wave 0/1 is where the semantic-release template edge cases surface; better to find them on `cronjobsample` than on `credentialing`

Active teams come in Wave 2 once mechanics are proven:

- They stress-test the real support model (secrets rotation, deploy approvals, release cadence)
- A successful migration for a visible active team is the best internal advocacy for adoption
- They require a controlled freeze window and dedicated team engagement during cutover

---

## Current Activity Snapshot (as of 2026-04-06)

| Range | Repos |
|---|---|
| Untouched > 180 days | SupplyChainOperations/ChangeRequest, bcp_web/finance/TimeTrack, Quality_Web_Apps/mpxr, bcp_web/devops/sampleapplications/cronjobsample, HR/, MDMS_Metadata_Management (×4) |
| 90–180 days | bcp_web/legacy/PriorAuthorization, EMEA_IT_Testautomation/cds, bcp_web/commercial/ProductPerformance, legal_it/PCI, bcp_web/finance/ERDD |
| 14–90 days | HR/gtat-*, bcp_web/common/newton, IAM_Team/ciam/*, bcp_web/commercial/AllegoUserManager, bcp_web/cqxm/*, ACM_IT/*, EMEA_IT_Testautomation/ofi |
| Active < 14 days | bcp_web/commercial/credentialing, legal_it/legacy/SunrayOld, bcp_web/clinical/OracleClinicalRDCSSO, bcp_web/finance/cnbs-epay, Quality_Web_Apps/emanuals (several) |

---

## Wave 0 — Migration Mechanics Pilot

**Timeline:** Week 1–2

**Goal:** Prove the full migration path end-to-end with minimal risk. Deliver a reusable runbook and validated semantic-release template before touching anything business-critical.

### Target Repos

| # | GitLab Path | Last Commit (days) | App / Team Owner (Contact) | Notes |
|---|---|---|---|---|
| 1 | `bcp_web/devops/sampleapplications/cronjobsample` | 298 | Argo/Compass CI Platform Team | Purpose-built sample app — ideal first target |
| 2 | `SupplyChainOperations/ChangeRequest` | 304 | Rebecca Hoskins (App Owner)<br/>B K, Karthik (CTS Developer) | Single env folder, isolated group |

**Order rationale:** Start with the sample app (built to be moved), then an isolated business repo with no active team, then the oldest/most isolated repo in the entire dataset. Confidence ramp.

### Deliverables
- Semantic-release `.gitlab-ci.yml` include template v1
- CI variable and runner inventory checklist
- Migration runbook v1 (steps, rollback procedure, validation checklist)
- Tag/release parity report

### Exit Gates
- ✅ 2 consecutive default-branch pipeline runs pass per repo
- ✅ Semver tag generated correctly in new instance
- ✅ Rollback procedure tested at least once
- ✅ Runbook peer-reviewed by a second team member

---

## Wave 1 — Stale Subgroup Bundle

**Timeline:** Week 3–6

**Goal:** Scale the proven migration process across a cohesive subgroup. All Wave 1 repos are untouched for 200+ days — zero coordination overhead.

### Bundle A — MDMS_Metadata_Management (4 repos, all > 200 days)

| GitLab Path | Last Commit (days) | App / Team Owner (Contact) |
|---|---|---|
| `MDMS_Metadata_Management/atlan-extract-eds` | 307 | Heidi Westfall (Owner)<br/>Boddula, Nagaraju (Developer) |
| `MDMS_Metadata_Management/atlan-extract-dev3_plm` | 215 | Heidi Westfall (Owner)<br/>Boddula, Nagaraju (Developer) |
| `MDMS_Metadata_Management/atlan-extract-dev3plm` | 214 | Heidi Westfall (Owner)<br/>Boddula, Nagaraju (Developer) |
| `MDMS_Metadata_Management/atlan-extract-windchill` | 208 | Heidi Westfall (Owner)<br/>Boddula, Nagaraju (Developer) |

Entire subgroup is stale — migrate as a cohesive unit, requiring only one set of group-level GitLab settings and one owner contact.

### Bundle B — Quality_Web_Apps (stale members only)

| GitLab Path | Last Commit (days) | App / Team Owner (Contact) |
|---|---|---|
| `Quality_Web_Apps/mpxr` | 304 | Jon Ragati (Owner)<br/>Tiruvuri, Pavan (Architect / Developer) |
| `Quality_Web_Apps/GQMS` | 243 | Jennifer Spaeth (Owner)<br/>B K, Karthik (CTS Developer) |

### Exit Gates
- ✅ 90%+ first-pass pipeline success across all Wave 1 repos
- ✅ Semver release generated and visible in new instance per repo
- ✅ Group-level settings (approvals, protected branches, variables) verified
- ✅ one subgroup owner sign-off per bundle

---

## Wave 2 — Active Teams (Co-Owned Migration)

**Timeline:** Week 7–10

**Goal:** Migrate repos with active development. Requires scheduled freeze windows, team coordination, and dedicated hypercare.

### Target Repos

| GitLab Path | Last Commit (days) | App / Team Owner (Contact) | Notes |
|---|---|---|---|
| `bcp_web/commercial/credentialing` | 4 | Lauren Parker (Owner)<br/>Sindhuja Kareddy (CTS Developer)| Hottest repo in dataset — schedule carefully |
| `bcp_web/commercial/AllegoUserManager` | 13 | Miranda Maassen (Owner)<br/>Sangala, Vamshi Krishna Reddy (CTS Developer) | Same group as credentialing — bundle if possible |
| `bcp_web/commercial/ProductComparison` | 13 | Shelley Sanders (Owner)<br/>Sangala, Vamshi Krishna Reddy (CTS Developer) | Same group |
| `HR/gtat-*` | 4 | Jon Ragati (Owner)<br/>Shaik, Peerbhasha (Developer)| Active; pair with gtat-api |
| `IAM_Team/ciam/*` | 18 | Brian Shiek (Owner)<br/>Spencer Crum (Developer)| CIAM candidate with current delivery activity |
| `PC_AI_Lab/contract-gpt-*` | 7 | Michael Pelser (Architect / Developer) | Strong active-team candidate with reachable owners (migration already started, app team testing) |
| `bcp_web/clinical/OracleClinicalRDCSSO` | 5 | Sameer Bhasin (Owner)<br/>Shaik, Peerbhasha (Developer) | Recent commits — confirm team availability |
| `legal_it/legacy/SunrayOld` | 4 | Cynthia Krag (Owner)<br/>CTS Team Owns Development (Visvakumar Manoharan) | Active legacy app; good governance test case |
| `Quality_Web_Apps/emanuals/*` | 6 | Patrick Bonten (Owner)<br/>Utkarsh Tomar (Developer) | Active eManuals applications |

### Controls Required for Active Repos
- **Freeze window:** 24–48 hours for default branch during cutover; agreed in writing with app team
- **Commit forwarding owner:** named person responsible for cherry-picking any commits that land during window
- **Team briefing:** 1-week heads-up minimum, via email + Slack/Teams channel notification
- **Hypercare:** daily pipeline monitoring for 7 days post-migration; fast-path support path

### Exit Gates
- ✅ No unresolved migration defects after 3 business days
- ✅ Scheduled releases not skipped or delayed due to migration
- ✅ App team confirms: issue tracking, deployment approvals, notifications all working in new instance
- ✅ Team lead sign-off

---

## Wave 3 — Broader Adoption / Remaining Repo Coverage

**Timeline:** Week 11+
**Goal:** Complete migration coverage for the remaining moderate-churn and follow-on repos after the pilot, stale bundles, and highest-activity teams are complete.

### Remaining Repo Bundles

#### Finance / Commercial / Legacy follow-on

| GitLab Path | Last Commit (days) | App / Team Owner (Contact) | Notes |
|---|---|---|---|
| `bcp_web/finance/ERDD` | 87 | Marie Harrison (Owner)<br/>CTS Team Owns Development (Visvakumar Manoharan) | Moderate churn; can follow after finance patterns are proven |
| `bcp_web/finance/WorldWideRevenue` | 21 | Bipen Rai (Owner)<br/>CTS Team Owns Development (Visvakumar Manoharan) | Active but less urgent than credentialing/CIAM/PC_AI_Lab |
| `bcp_web/finance/cnbs-epay` | 10 | Adam Nguyen (Owner)<br/>External Vendor Developed, Adam Handling Going forward | Active finance app; schedule with finance owner window |
| `bcp_web/commercial/ProductPerformance` | 41 | Renee Hertel (Owner)<br/>CTS Team Owns Development (Visvakumar Manoharan) | Same commercial governance model as credentialing bundle |
| `bcp_web/legacy/PriorAuthorization` | 119 | Rebecca Schutz (Owner)<br/>CTS Team Owns Development (Visvakumar Manoharan) | Lower urgency legacy repo |

#### Shared platform and engineering teams

| GitLab Path | Last Commit (days) | App / Team Owner (Contact) | Notes |
|---|---|---|---|
| `bcp_web/cqxm/protolens` | 35 | Ajit Singh (Owner)<br/> Hima Banda (Developer) | CQXM bundle |
| `bcp_web/cqxm/pss-investigation-summary` | 12 | Ajit Singh (Owner)<br/> Hima Banda (Developer) | CQXM active repo |
| `ACM_IT/acm-strat-alliance-integrations` | 19 | David Kanevsky (Owner)<br/> Kliger, Israel & Gedalia (Developer)| ACM bundle |
| `ACM_IT/colorado-tickets` | 25 | David Kanevsky (Owner)<br/> Kliger, Israel & Gedalia (Developer)| ACM bundle |
| `EMEA_IT_Testautomation/*` | 150 | Remko Heerings (Owner)<br/>Ieme Welling (Architect) | Lower-priority test automation repo |
| `mazor/tools-hhh/shraga-app/shraga-jira-proxy` | 35 | Edo Notes / Ron Ocheri | Standalone engineering utility repo |

#### Quality and support application follow-on

| GitLab Path | Last Commit (days) | App / Team Owner (Contact) | Notes |
|---|---|---|---|
| `Quality_Web_Apps/GFAS` | 25 | Tolu Oni (Owner)<br/>Jariwala, Chintan[VPE] (Developer) | Active quality app |
| `Quality_Web_Apps/InsightProductAssignment` | 26 | Patrick Bonten (Owner)<br/>CTS Team Owns Development (Visvakumar Manoharan)| Active quality app |
| `Quality_Web_Apps/ireg` | 39 | Patrick Bonten (Owner)<br/>CTS Team Owns Development (Visvakumar Manoharan) | Moderate churn |
| `SupplyChainOperations/U-Ship` | 35 | Venugopal Vengala (Owner)<br/>CTS Team Owns Development (Visvakumar Manoharan)  | Same business area as ChangeRequest |
| `HR/ERG` | 33 | Lauren Parker (Owner)<br/>CTS Team Owns Development (Visvakumar Manoharan)  | Follow after gtat active-team wave |
| `legal_it/PCI` | 84 | Cynthia Krag  (Owner)<br/>CTS Team Owns Development (Visvakumar Manoharan) | Coordinate with legal_it stakeholders |

### Wave 3 Scheduling Note

Wave 3 is intentionally flexible. Once Wave 2 proves the active-team operating model, these repos can be grouped by stakeholder availability, shared CI variables, or shared deployment patterns rather than by strict date order.

---

## Per-Repo Migration Checklist

### Pre-Migration
- [ ] Confirm default branch and all protected branches
- [ ] Inventory CI/CD variables (masked secrets, deploy tokens, runner tags)
- [ ] List all active pipeline schedules
- [ ] Note existing tags and semver baseline
- [ ] Identify and document any pipeline includes from external projects

### Execution
- [ ] Import/mirror repository to new GitLab instance
- [ ] Recreate branch protections and merge request approvals
- [ ] Recreate CI/CD variables and runner assignments
- [ ] Replace pipeline with semantic-release template include
- [ ] Run dry-run release (verify commit parsing and bump logic)
- [ ] Run and pass one actual pipeline on default branch

### Validation
- [ ] Release tag visible and correctly versioned in new instance
- [ ] Release notes generated (changelog / release description)
- [ ] Deployment stages execute with correct environment targets
- [ ] Old instance pipeline disabled or repo archived

### Hypercare
- [ ] Daily pipeline monitoring through end of wave
- [ ] Defect log reviewed and cleared
- [ ] Owner sign-off documented

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Active repo commits during Wave 2 window | Branch divergence | 24-48h freeze window + named cherry-pick owner |
| Missing CI secrets/runners on new instance | Pipeline failures | Pre-migration inventory checklist, peer-verified |
| Semantic-release behavior differs from legacy | Missed/wrong releases | Dry-run validation gate in Wave 0 before proceeding |
| Group settings misconfigured (approvals, etc.) | Broken deploy gates | Verify against old instance settings; checklist item per wave |
| App team unresponsive in Wave 2 | Blocked hypercare | Confirm team availability before scheduling; escalation path defined |
