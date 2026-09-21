---
label: FAQ
icon: question
order: 140
---

# Frequently Asked Questions

## 🚀 Getting Started

### What is Compass CI?
Compass CI is an internally developed, shared CI/CD pipeline and AWS EKS (Kubernetes) hosting environment for web-based applications. Through configuration files in your GitLab project, you can provision environments, build container images, and deploy your applications automatically.

**Key Benefits:**
- Automated CI/CD workflows
- Self-service environment provisioning
- Scalable Kubernetes hosting
- GitLab version control integration
- Independent environment management
- Consistent deployment standards
- Enhanced security with automated scanning
- Flexible configuration

### Is Compass CI related to the external Argo CD tool?
No, Compass CI (formerly called "Argo") and the external [Argo CD](https://argo-cd.readthedocs.io/en/stable/) tool are completely separate. They happen to share similar names, but:
- **Compass CI** is Medtronic's internal platform with custom CI/CD pipelines
- **Argo CD** is an external open-source GitOps continuous delivery tool

Compass CI uses **Flux CD** (not Argo CD) for GitOps-based application deployment.

### How do I get started with Compass CI?
Follow the [Getting Started](./getting-started/index.md) guide. The quick summary:
1. Containerize your application (create Dockerfile)
2. Add health check endpoints (`/health`, `/ready`)
3. Create `.gitlab-ci.yml` for build/deploy automation
4. Push to GitLab - that's it! Pipeline runs automatically

**Time needed:** 10-15 minutes for initial setup

### What if my application already has a CI/CD pipeline?
You can migrate gradually:
1. Keep existing pipeline initially
2. Update `.gitlab-ci.yml` gradually to include various Compass CI jobs
3. Test in DEV environment first
4. Once validated, fully migrate

**Guidance:** See [CI/CD Pipeline Overview](cicd-pipeline/index.md)

### What are the requirements for using Compass CI?
To use the platform, your application must meet these requirements:

**1. Containerization:**
- Application must be containerizable (provide a Dockerfile)
- You're responsible for creating and maintaining your Dockerfile

**2. Security Requirements:**
- Container images must pass vulnerability scans
- Must comply with standard K8s best practices
- No Critical, High, or Medium severity vulnerabilities without approved Policy Exception Requests (PERs)
- Must comply with [GCISO security standards](https://medtronic.sharepoint.com/sites/GSO/SitePages/Application-Security-Testing.aspx)

**3. Health Endpoints:**
- Implement `/health` (liveness probe)
- Implement `/ready` (readiness probe)

**4. Platform Updates:**
- Test your application quarterly when Kubernetes platform receives updates
- Ensure compatibility with platform upgrades

**5. Regular Patching:**
- Keep dependencies updated
- Remediate vulnerabilities within due dates
- Pipeline blocks vulnerable workloads from deployment - no exceptions

**6. External AWS Services:**
- If you need S3, RDS, etc., they must be created in your own AWS account
- Engage [Cloud Services](https://cloudservices.medtronic.com/aws/documentation/account/account/index.html) for account setup

**7. Application Support:**
- Compass CI is a platform team, not an application development/support team
- Ensure your application has dedicated development and support resources

See [Getting Started](getting-started/index.md) for onboarding details.

### Do I need to modify my application code?
Minimal changes needed:
- ✅ Add `health` and `ready` endpoints
- ✅ Add environment variable support
- ❌ No framework changes required
- ❌ No major refactoring needed

## 🐳 Docker & Containerization

### What base image should I use?
Compass CI recommends using minimal base images for better security and performance. Here are some **examples** (not an exhaustive list):
- **Node.js:** `node:18-alpine`
- **Python:** `python:3.11-slim`
- **Java:** `eclipse-temurin:17-alpine`
- **Go:** `golang:1.20-alpine`

**Benefits:** Smaller size, fewer vulnerabilities, faster deployments

**Using External Repositories:**
You can pull from external registries configured in JFrog Artifactory. Reference the full list of [available external repositories](https://documentation.shared-services-prd.eks.mdtcloud.io/cicd-platform/jfrog/external-repositories/).

**Example Dockerfile with external image:**
```dockerfile
FROM case.artifacts.medtronic.com/ext-docker-hub-remote/eclipse-temurin:17-alpine

WORKDIR /app
COPY target/myapp.jar /app/
EXPOSE 8080

CMD ["java", "-jar", "myapp.jar"]
```

The `ext-docker-hub-remote` prefix routes through JFrog Artifactory, providing caching and security scanning benefits.

### How do I include secrets in my container?
**Don't!** Use Kubernetes Secrets instead:
```yaml
env:
  - name: DATABASE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: app-secrets
        key: db-password
```

This prevents secrets from being baked into the image.

### Can I build images locally and skip GitLab CI?
Not recommended for production. GitLab CI provides:
- Automated vulnerability scanning
- Consistent build environment
- Automated versioning
- Audit trail of all builds

**For development:** Local builds OK, push to GitLab before production deployment

## 🔄 CI/CD Pipeline

### How often does the pipeline run?
**Automatically on every commit to main branch**
- Merge request pipelines: Validate changes before merge
- Main branch: Build, test, deploy to DEV
- Releases: Automatic semantic versioning

### Can I manually trigger a pipeline?
Yes, in GitLab:
1. Go to CI/CD → Pipelines
2. Click "Run pipeline" button
3. Select branch
4. Click "Create pipeline"

### How long does a typical pipeline take?

- Build image: 2-5 minutes
- Security scans: 1-2 minutes
- Deploy: 2-3 minutes
- **Total:** 5-10 minutes typical

### Can I skip the pipeline?
Not recommended. Pipeline ensures:
- Code builds successfully
- Security scanning complete
- Automated tests pass
- Consistent deployments

**Exception:** Use `[ci skip]` in commit message only for urgent fixes, then trigger manual pipeline later

### Can I customize the CI/CD pipeline?
Yes! Compass CI uses centrally managed components that you can customize:

**Shared Pipeline Components:**
The platform provides reusable jobs for common tasks:
- Building and pushing container images
- Security scanning (Trivy, SAST, dependency scanning)
- Deploying to environments
- Prerequisites validation

**Your `.gitlab-ci.yml`:**
You control your pipeline configuration and can:
- Add custom stages and jobs
- Define specific build steps
- Configure job rules and conditions
- Pass variables to shared components
- Run custom tests and validations
- Integrate with external tools

**Example Customization:**
```yaml
# Use shared build job
build-image:
  extends: .build-image
  variables:
    BUILD_ARGS: "--build-arg NODE_ENV=production"

# Add custom test stage
custom-tests:
  stage: test
  script:
    - npm run integration-tests
    - npm run e2e-tests
```

**Download Template:**
[!file .gitlab-ci.yml template](static/templates/.gitlab-ci.yml)

See [Pipeline Configuration](cicd-pipeline/pipeline-stages.md) for full customization options.

## 🌍 Environments & Deployment

### How do I deploy to a specific environment?

- **DEV:** Automatic on every `dev` branch commit
- **TESTING**: Automatic on every `testing` branch commit
- **STAGING:**: Automatic on every merge to `staging` branch
- **RELEASE:**: Automatic on every merge to `release` branch
- **PRODUCTION:**: Manually after merge to `main` or `production` branch

### Can I deploy the same version multiple times?
Not directly (immutable versions). Instead:
1. Update code
2. Commit with message like "fix: typo in error message"
3. semantic-release auto-increments version
4. Deploy new version

### How do I rollback to a previous version?

Update the `newTag` in your environment's `kustomization.yaml`:
```bash
# Navigate to the environment folder
cd k8s/staging/  # or k8s/production/, k8s/dev/, etc.

# Edit kustomization.yaml and change newTag to previous version
# Change: newTag: "1.2.3"
# To: newTag: "1.2.2"

# Commit and push
git add kustomization.yaml
git commit -m "chore: rollback to version 1.2.2"
git push
```

FluxCD will automatically reconcile and deploy the previous version.

### Can I deploy without going through DEV first?
Not recommended. Pipeline flow ensures:
1. **DEV:** Validate changes work, initial scans for vulnerabilities
2. **STAGING:** User acceptance testing
3. **PRODUCTION:** Stable release

Direct to production risks broken deployments affecting customers.

## 🔐 Security & Compliance

### What happens if my container has vulnerabilities?
Deployment blocks automatically based on your `DATA_CLASSIFICATION`:

**For HIGHLY_SENSITIVE applications:**
- ALL vulnerabilities (Critical, High, Medium, Low) must be remediated or have approved PERs
- 30-day deadline for Critical/High, 90-day for Medium/Low

**For SENSITIVE, INTERNAL_USE_ONLY, or PUBLIC applications:**
- Critical, High, and Medium vulnerabilities must be remediated or have approved PERs
- Low severity issues are tracked but don't block deployment (no due date)

**Process:**
1. Build succeeds and creates container image
2. Trivy/SAST/Dependency scans run automatically
3. Vulnerabilities are created as GitLab Work items with due dates
4. If overdue issues exist, pipelines executed against staging/release/production branches will fail
5. You must fix vulnerabilities OR submit Policy Exception Request (PER)

**To fix:** Update dependencies or apply code fixes, then re-deploy. Pipeline rescans automatically.

See [Security & GitLab Work items](cicd-pipeline/security-gitlab-issues.md) for complete workflow.

### What is DATA_CLASSIFICATION and how do I set it?
`DATA_CLASSIFICATION` determines your application's security policy enforcement level. It must be set in your `.gitlab-ci.yml`:

```yaml
variables:
  DATA_CLASSIFICATION: SENSITIVE  # or HIGHLY_SENSITIVE, INTERNAL_USE_ONLY, PUBLIC
```

**How to determine your classification:**
1. Complete the [Security and Privacy Intake Request Form](https://medtronicprod.service-now.com/it/?id=sc_cat_item_order_guide&sys_id=10bd221adb726c1014c354f94896192d)
2. GCISO will assess your application's data handling
3. You'll receive a Privacy Impact Assessment (PIA) number
4. Use the classification from the assessment in your pipeline configuration

**Classifications:**
- **HIGHLY_SENSITIVE:** PHI, financial data, credentials - ALL vulnerabilities must be fixed
- **SENSITIVE:** Proprietary business data - Critical/High/Medium must be fixed
- **INTERNAL_USE_ONLY:** Internal tools - Critical/High/Medium must be fixed
- **PUBLIC:** Public-facing content - Critical/High/Medium must be fixed

See [Data Classification](getting-started/gitlab-repository-configuration.md#determining-your-data-classification) for details.

### How long does a PER approval take?
Typically 1-3 business days<br/>
**Tip:** Submit PER early if you anticipate needing it

### Can I deploy with Medium vulnerabilities?
Yes, with conditions:
- Medium vulnerabilities require PER approval (if deployment blocked)
- Less urgent than Critical/High
- Document justification in PER

### What's the difference between Trivy and Contrast?

- **Trivy:** Static scanning of container image (OS packages, libraries)
- **Contrast:** Dynamic scanning of running application (application code vulnerabilities)

Both are required before production deployment.

## 🏥 Monitoring & Troubleshooting

### How do I view my application logs?
Multiple options:

**Centralized in Grafana:**
1. Open [Grafana Dashboard](https://g-05c60f9e9b.grafana-workspace.us-east-1.amazonaws.com/)
2. Search logs with queries or view log related panels for your application(s)

**See:** [Monitoring Guide](monitoring/index.md) for details

### How often are health checks performed?

- **Liveness probe (/health):** Every 10 seconds (depending on application configuration)
- **Readiness probe (/ready):** Every 5 seconds during deploy, then 10 seconds (depending on application configuration)

3 consecutive failures trigger pod restart (for liveness)

### What should `/health` endpoint return?

```
HTTP 200 OK
Content: {"status": "ok"}
```

This signals that your application is alive and can handle traffic.

### What if my application is slow to start?
Increase `initialDelaySeconds` in probe config:
```yaml
livenessProbe:
  httpGet:
    path: /health
    port: 8080
  initialDelaySeconds: 60  # Wait 60s before checking
  periodSeconds: 10
```

This gives your application time to fully start before Kubernetes checks it.

## 📦 Versioning & Releases

### How does versioning work?
Compass CI uses **semantic-release**:
- Analyzes commit messages
- Automatically determines version bump
- Formats: MAJOR.MINOR.PATCH (e.g., 2.3.1)

**Commit messages:**
- `fix: ...` → Patch version bump (2.3.1 → 2.3.2)
- `feat: ...` → Minor version bump (2.3.1 → 2.4.0)
- `BREAKING CHANGE: ...` → Major version bump (2.3.1 → 3.0.0)

### Can I manually set the version?
No, semantic-release controls versioning automatically. This ensures:
- Consistent versioning across all teams
- Semantic versioning standards followed
- Automated changelog generation

### How do I create a release?
Automatically on main branch merge:
1. Merge code to main
2. semantic-release analyzes commits
3. Automatic version bump
4. Release created in GitLab
5. Tag pushed (e.g., v2.3.2)

No manual steps needed!

## 🏢 Organization & Governance

### Who can deploy to production?
Anyone with code access can trigger production deployments through Git tags. However:
- Code must pass all pipeline checks
- Security scans must pass
- Changes should follow review process
- Teams can enforce approval gates via branch protection

**Governance:** Configure in GitLab → Settings → Repository → Protected Branches

### How do I restrict who can deploy?
GitLab project settings:
1. Settings → Repository → Protected Branches
2. Add protection to main/master/release branches
3. Require approvals before merge
4. Restrict who can approve

### Can I integrate with Teams for notifications?
Yes! GitLab integrations:
1. Go to Settings → Integrations → Microsoft Teams notifications
2. Configure Teams webhook
3. Select which events to notify

### What types of applications does Compass CI support?
Compass CI supports any containerized applications including:
- **Java:** Grails, SpringBoot
- **JavaScript:** ReactJS, AngularJS, NextJS
- **Python:** Django, Flask
- **Go, .NET, and other languages**
- **Cron Jobs:** Scheduled batch processes

Any application that can be containerized and meets the [security requirements](security/index.md) can run using Compass CI.

### Can my application connect to on-premises servers?
Yes, with proper network connectivity requests. Connectivity depends on:
- The Nexpose/Rapid7 security score of the target server
- Firewall rule approval
- **Note:** Direct connections to 'CORE' systems are not allowed

Test connectivity using the connectivity tester tools:
- [Argo Dev Cluster Connectivity Tester](https://einstein.argo-dev.eks.mdtcloud.io/einstein/network)
- [Argo Quarantine/Prod Connectivity Tester](https://einstein.argo-prd.eks.mdtcloud.io/einstein/network)

See [Connectivity Request Process](how-to/connectivity-request-process.md) for details.

### How many environments can I have?
Up to 5 application environments:
1. **DEV** - Initial development and testing
2. **TESTING** - QA and integration testing
3. **STAGING** - Pre-production validation
4. **RELEASE** - Release candidate testing
5. **PRODUCTION** - Live environment

**Recommended setup:**
- **argo-dev cluster:** DEV or TESTING (for platform upgrade testing)
- **argo-prd cluster:** STAGING and PRODUCTION

Not all environments need to be active at once - spin up and down as needed.

### How do I shut down an environment temporarily?
To reduce costs during extended periods of non-use, scale your workloads to zero replicas:

**Steps:**
1. Edit your environment's manifests (e.g., `k8s/staging/deployment.yaml`)
2. Set `spec.replicas: 0` for all Deployments/StatefulSets
3. Commit and push:
   ```bash
   git add k8s/staging/
   git commit -m "chore: scale down staging environment"
   git push
   ```
4. FluxCD automatically scales down your pods
5. To restart, set `replicas` back to desired count and push

**What happens:**
- ✅ Pods stop running (compute costs eliminated)
- ✅ Services, ConfigMaps, Secrets remain (no cost)
- ⚠️ PersistentVolumeClaims remain (storage costs continue)
- ⚠️ To eliminate storage costs, contact **Infra-Argo-Global** via ServiceNow to request PVC deletion

**Warning:** Deleting PVCs permanently deletes data. Ensure backups exist before requesting deletion.

See [Deployments Guide](how-to/deployments.md#shutting-down-an-environment) for detailed instructions.

### What EKS clusters are available?
Compass CI currently operates in two AWS accounts with multi-tenanted clusters. Multiple application teams share these clusters, with isolation provided through Kubernetes namespaces, network policies, and resource quotas.

For detailed cluster information including network configuration, connectivity inventory, and architecture diagrams, see the [Platform Inventory](platform-inventory/index.md).

**Additional Clusters:**
The platform can onboard additional clusters as needed based on:
- Specific compliance or isolation requirements
- Regional deployment needs
- Capacity planning and scaling requirements

Contact the [Platform Team](mailto:dl.itargocoreteam@medtronic.com) to discuss dedicated cluster requirements.

### Does Compass CI have a Disaster Recovery plan?
Yes, Compass CI has a comprehensive Disaster Recovery strategy documented in [D2 (Document ID: 09025b4181807899)](https://mrcsd2.medtronic.com/D2/?docbase=mrcs&locateId=09025b4181807899). For a step-by-step runbook, see the [Disaster Recovery Process Reference Page](references/aws-disaster-recovery-process.md).

**Recovery Approach:**
Our disaster recovery strategy relies on rapid, automated recovery of deployments and infrastructure:

**Infrastructure as Code (IaC):**
- Terraform and CloudFormation ensure consistent, repeatable infrastructure
- All infrastructure configurations are version-controlled
- Enables quick replication in alternate regions

**Application State Management:**
- FluxCD tracks the current state of all applications in the cluster
- All application configurations stored in Git (FluxConfig repository)
- Declarative approach ensures known-good state can be restored

**Regional Failover:**
- In the event of a regional AWS outage, infrastructure can be replicated to an alternate region
- FluxCD automatically reconciles application deployments to match desired state
- Minimizes downtime and reduces manual intervention

**Recovery Time:**
- Infrastructure: ~30-60 minutes to provision in alternate region
- Applications: Automatic deployment via FluxCD once infrastructure is ready
- Total failover time depends on application count and complexity

**Your Responsibility:**
- Ensure your application data is backed up (databases, S3 buckets in your AWS account)
- Test your application's recovery procedures
- Document any external dependencies that may affect recovery

For specific DR testing or questions, contact the [Platform Team](mailto:dl.itargocoreteam@medtronic.com).

### Do you have infrastructure diagrams?
Yes! Architecture diagrams are available for download.

**Cluster Infrastructure Diagrams:**
See the [Platform Inventory](platform-inventory/index.md) for cluster-specific architecture diagrams, including network topology, load balancers, ingress configuration, and application flow.

**Sample Application Diagram:**
[!file Application Logical Diagram Template](static/architecture/Argo-Prod-Application-Logical-Diagram-EXAMPLE.drawio)

Use this template to document your application's architecture and customize it for your specific needs.

## 💰 Cost & Resources

### How much does Compass CI cost?
Compass CI costs include:

**Current Model:**
- Platform hosting costs are currently covered by the Compass CI team
- **No per-build fees** - use the pipeline as much as needed
- Average cost: ~$530/month per application team for infrastructure and 24/7 support for the CI platform and infrastructure

**AWS Services:**
- AWS services specific to your application (S3, RDS, CloudFront, etc.) will be charged back to your cost center

**Cost Center Assignment:**
- Specify your `COST_CENTER` variable in `.gitlab-ci.yml`

See [GitLab Repository Configuration](getting-started/gitlab-repository-configuration.md) for variable setup.

### How can I optimize pipeline costs?

1. Use minimal base images (Alpine, distroless)
2. Cache dependencies
3. Parallelize tests where possible
4. Use smaller resource requests for dev environments
5. Clean up old images regularly
6. Shut down application environments when not in use

### Can I limit resource usage?
Yes, in Kubernetes manifests:
```yaml
resources:
  requests:
    cpu: "250m"        # Minimum needed
    memory: "256Mi"
  limits:
    cpu: "1000m"       # Maximum allowed
    memory: "1Gi"
```

## 🤝 Support & Help

### Where do I get help?
Multiple resources:
1. **Documentation:** [Compass CI Playbook](README.md)
2. **Troubleshooting:** [Troubleshooting Guide](troubleshooting-and-support/troubleshooting-guide.md)
3. **Platform Team:** For infrastructure/pipeline questions
4. **Your Team:** For application-specific issues

### How do I report a bug?
Create GitLab work item:
1. Go to project → Work items
2. Click "New work item"
3. Describe problem and include:
   - Pipeline/pod logs
   - Steps to reproduce
   - Expected vs actual behavior

### Can I request a feature?
Yes! Create GitLab work item with label "feature-request" and describe:
- What you want to do
- Why it's needed
- How it would help

## 📚 More Resources

- [Getting Started](getting-started/index.md)
- [CI/CD Pipeline](cicd-pipeline/index.md)
- [Security Guide](security/index.md)
- [Monitoring](monitoring/index.md)
- [Troubleshooting](troubleshooting-and-support/troubleshooting-guide.md)
