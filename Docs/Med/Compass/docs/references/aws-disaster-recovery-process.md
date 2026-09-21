---
label: Disaster Recovery Process
icon: alert
order: 10
---

# Disaster Recovery Process (AWS us-east-1)

## Scope
This page summarizes the disaster recovery (DR) strategy and the required recovery actions for an outage of the AWS us-east-1 region. It covers the platform recovery steps, responsibilities, and the actions required of applications that run on the platform.

## Purpose
Provide a clear, actionable DR runbook so technical and application support teams can recover the EKS environment following a catastrophic event in AWS us-east-1.

## Assumptions
- Critical public infrastructure is largely unaffected.
- An alternate AWS region or availability zones (AZs) capability is maintained.
- Qualified recovery personnel are available.
- Backups and data protection are current and available.

## Definitions
- **Disaster**: An event that materially impacts services for more than 8 hours (or forecasted to exceed 8 hours), requiring recovery at an alternate location.
- **RTO**: 24 hours.
- **RPO**: Less than 24 hours.

## Environment Summary
- Runs in AWS us-east-1 across three AZs.
- EC2 instances are backed up via AWS Backup with daily full backups.
- Backups are protected across AZs within us-east-1 but not across regions.
- On-premises databases may be external dependencies for applications on the platform, but are not dependencies for the platform itself.

## Disaster Declaration Criteria
Declare a disaster when:
- us-east-1 is unavailable, or all three AZs are unavailable, and
- the impact has exceeded 8 hours or is forecasted to exceed 8 hours.

## Recovery Strategy
### Scenario A: One or more AZs unavailable (region still available)
EKS should reschedule workloads and scale nodes in the remaining healthy AZs. No platform team action is required unless there is a broader control-plane impact or capacity shortfall. Impact depends on how client teams have configured workloads (e.g., multi-AZ node groups, autoscaling, pod affinity/anti-affinity, and storage that is AZ-specific such as EBS).

#### Client actions for AZ-specific constraints
- **EBS-backed workloads**
	1. Identify pods using EBS-backed PersistentVolumeClaims bound to the impacted AZ.
	2. Restore or recreate volumes in a healthy AZ (from snapshots if needed) and rebind PVCs.
	3. Redeploy the workload and validate data integrity and application health.
- **Pod affinity/anti-affinity constraints**
	1. Review affinity rules that pin workloads to nodes or AZs in the impacted zone.
	2. Temporarily relax or update affinity rules to allow scheduling in healthy AZs.
	3. Redeploy workloads and confirm replicas are spread across available AZs.
- **Node selectors, taints, and tolerations**
	1. Identify workloads with node selectors, topology constraints, or taints that target the impacted AZ.
	2. Update selectors/tolerations to allow scheduling in healthy AZs.
	3. Redeploy and verify placement across available AZs.
- **Zonal load balancer or ingress annotations**
	1. Review service annotations and ingress settings that pin traffic to a single AZ.
	2. Update annotations/settings to allow multi-AZ routing.
	3. Validate traffic routing and health checks.
- **PodDisruptionBudgets or replica counts too low**
	1. Review PDBs and replica counts that prevent rescheduling during AZ loss.
	2. Adjust PDBs/replicas to allow placement in remaining AZs.
	3. Verify availability after changes.

### Scenario B: us-east-1 region unavailable
Rebuild the environment using Terraform stacks in an alternate AWS region, then reconfigure connections to on-premises databases.

## Step-by-Step Recovery Actions (us-east-1 Down)
### Phase 0: Declaration (0-8 hours)
1. Validate outage impact and duration estimates.
2. Engage the Global IT Disaster Recovery Management Team.
3. Declare disaster if impact is or will be greater than 8 hours.
4. Notify application owners and platform stakeholders.

### Phase 1: Platform Recovery (0-24 hours)
1. Coordinate with the Cloud Services Team to identify and approve the alternate AWS region (preferred: us-east-2 based on the nearest standard region in the [Cloud Services routing guidance](https://cloudservices.medtronic.com/aws/documentation/foundation_standards/index.html?h=east#routing).
2. Shared Services Team deploys the EKS platform in the alternate region.
3. Compass CI Team applies Terraform stacks for platform services and any additional in-account objects maintained by the team.
4. Compass CI Team copies Flux GitOps configs to a new folder tied to the new cluster and reconcile to redeploy platform components and applications.
7. All teams validate platform and application core functionality.

### Phase 2: Application Recovery (in parallel)
1. Application owners validate external dependencies and credentials.
2. Review DNS and app-specific connectivity/configuration for region changes; request updates from the appropriate teams (network, DNS, IAM, etc.).
3. Update application configuration to point to the alternate region endpoints.
4. Reconfigure connections to on-premises databases.
5. **For applications using SOPS encryption**: Coordinate with GitOps team to update SecretStore region configuration. See [SOPS Key Advanced Operations - DR Guidance](sops-key-advanced-operations.md#dr-guidance) for details.
6. Verify health endpoints, readiness, and service functionality.

### Phase 3: Validation and Handover
1. Perform smoke tests for platform services and priority applications.
2. Confirm RPO and data integrity expectations with application owners.
3. Communicate recovery status and any residual impacts.

## Responsibilities
- **Global IT Disaster Recovery Management Team**: Coordinate response and recovery.
- **Cloud Services Team**: Assist with alternate region selection and approvals.
- **Shared Services Team**: Deploy EKS in the alternate region.
- **Compass CI Team**: Run Terraform for platform and in-account services; manage Flux GitOps configuration for the new cluster.
- **Application Owners**: Review DNS and app-specific configs; request network/DNS/IAM changes; recover and validate their applications and dependencies.

## Application Team Requirements
Applications on the platform must:
- Maintain container images and deployment manifests required for redeploy.
- Support configuration changes for region-specific endpoints.
- Validate health/readiness probes after recovery.
- Coordinate on data validation and reconcile any data loss within the RPO window.
- Maintain runbooks for app-specific dependencies (e.g., external services, secrets, DNS, databases).
- **For teams using SOPS encryption**: Understand the cross-region failover process for accessing replicated secrets. See [SOPS Key Advanced Operations - DR Guidance](sops-key-advanced-operations.md#dr-guidance).

## Recovery Timeline Summary
- **Disaster declaration threshold**: 8 hours of impact or forecasted impact.
- **Platform RTO**: 24 hours to restore platform services.
- **RPO**: Less than 24 hours of data loss tolerance.

## Testing and Maintenance
- DR tests should not impact production or non-production environments.
- Review and update this process annually or when recovery exercises indicate changes.

## References
- [D2 Disaster Recovery Strategy (Document ID: 09025b4181807899)](https://mrcsd2.medtronic.com/D2/?docbase=mrcs&locateId=09025b4181807899)
- Global IT Disaster Recovery Plan SOP (MITSS0003-11898)
- Global Records & Information Management Retention Schedule (70654)
