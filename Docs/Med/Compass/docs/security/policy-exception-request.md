---
label: Policy Exception Requests
icon: stop
order: 100
---

# Policy Exception Requests (PERs)

This guide provides comprehensive instructions for submitting a Policy Exception Request when you cannot remediate a vulnerability by its due date.

**Quick overview?** See [Security & GitLab Work items](../cicd-pipeline/security-gitlab-issues.md#policy-exceptions-when-you-cant-fix-it) for a summary of the PER process.

---

## When to Submit a PER

You may need to request a Policy Exception Request when you cannot remediate a vulnerability by its due date.

**What requires a PER depends on your application's data classification:**

**HIGHLY_SENSITIVE applications:**
Every vulnerability—Critical, High, Medium, AND Low—requires either remediation by the due date or an approved PER to deploy to staging/release/production.

**Other classifications (SENSITIVE, INTERNAL_USE_ONLY, PUBLIC):**
Only Critical, High, and Medium severity vulnerabilities require remediation or a PER. Low severity vulnerabilities do not block deployments and do not need a PER.

### Common reasons to request a PER:
- Vendor has not released a patch yet
- Fix requires major version upgrade with extensive testing
- Remediation would break critical functionality
- You need additional time to validate a complex fix
- Risk is acceptable for business reasons

## 🚀 Quick PER Process

**1. Gather Vulnerability Information**

Navigate to your GitLab project and export the vulnerability issues:
- Go to: **Project > Work items**
- Filter by **Label: vulnerability** to show only security issues
- Click the menu icon (⋮) in the top right
- Select **Export as CSV**
- This downloads a spreadsheet with all open vulnerability issues

**Note:** If your application is HIGHLY_SENSITIVE, include ALL vulnerabilities (Critical through Low). If your DATA_CLASSIFICATION is SENSITIVE, INTERNAL_USE_ONLY, or PUBLIC, you only need to request exceptions for Critical, High, and Medium vulnerabilities—Low severity does not require a PER.

For each vulnerability, gather:
- **Vulnerability ID** - CVE number or vulnerability identifier
- **Affected component** - Package, library, or OS name and current version
- **Why it cannot be fixed by the due date** - e.g., "Patch not yet available from upstream," "Requires major version upgrade with extensive testing," "Breaking change in application"
- **Compensating controls** - e.g., "Application runs in isolated container without internet access" or "Input validation prevents exploitation"
- **Proposed remediation date** - When will this be fixed?

**2. Create PER Request in LogicGate**

Navigate to the [LogicGate portal](https://medtronic.logicgate.com) (see [LogicGate PER documentation](https://medtronic.sharepoint.com/sites/GSO/SitePages/LogicGateExceptionRequest.aspx) for guidance):
- Title: "Policy Exception Request - [Application Name]"
- Description: List all remaining vulnerabilities
- Severity: Indicate Critical/High/Medium counts
- Timeline: When will each be fixed?
- Justification: Why can't you fix them now?
- **Attach the CSV file** with your vulnerability details and justifications

**3. Specify Duration**

PERs have expiration dates (typically 30-90 days):
- Valid From: Today
- Valid To: When you'll remediate (or re-assess)
- Extensions available if timeline changes

**4. Submit for Approval**

LogicGate will route to:
- GSO (Global Security Office) - Security review
- Data Owner - Risk acceptance for data touched by app
- Platform Team - Record keeping

**5. Receive Approval**

Once approved:
- Add `PER::<PER-ID>` label to each Issue that was approved
- Application can deploy with exception - pipeline will confirm PER is valid and approved before allowing deployment

## 📋 What to Include in LogicGate PER Request

You'll gather most of this information when you export the CSV above. When you create the PER in LogicGate, include:

### Application & Summary
- **Application name** and **environment(s)** needing exception (STAGING and/or PRODUCTION)
- **CSV attachment** - The spreadsheet with vulnerability details from your GitLab export

### For Each Vulnerability (from CSV)
- Vulnerability ID and affected component
- Why remediation cannot be completed by the due date
- Compensating controls in place
- Proposed remediation date

### Business Justification
- **Risk assessment** - Is this low, medium, or high risk despite the vulnerability?
- **Why deployment cannot wait** - What business need requires deploying with this exception?

### Examples

**Example 1: Vendor Not Released Patch**
```
Vulnerability: CVE-2024-12345 in Node.js Express v4.17
Status: Awaiting vendor patch
Workaround: Input validation added in application code
Timeline: Patch expected Q2 2024
```

**Example 2: Major Version Upgrade Required**
```
Vulnerability: CVE-2024-54321 in Python Django v2.2
Status: Fix requires upgrade to Django v4.0 (major version)
Impact: 2 weeks QA testing, extensive regression testing needed
Timeline: Will upgrade in next quarterly release cycle
```

## ✅ Deployment with Approved PER

Once your PER is approved in LogicGate:

1. **Label your GitLab Work items**
   - Add the `PER::<PER-ID>` label to each issue that was approved in your PER
   - Find your PER-ID in the LogicGate URL: `https://medtronic.logicgate.com/records/<PER-ID>`
   - Example: `PER::JvjsRwvN`

2. **Deploy to staging/production**
   - When you push code to staging or production branches, the pipeline will run
   - The policy evaluation job automatically confirms:
     - PER is approved in LogicGate
     - Current date is within the valid PER date range
     - Issues are labeled with the PER ID
   - If all checks pass, deployment proceeds even with overdue vulnerabilities

3. **If PER expires**
   - Deployment automatically blocks when the PER expires
   - Either remediate the vulnerabilities or renew/extend the PER in LogicGate
   - Contact the [Application Security Team](https://medtronic.sharepoint.com/sites/GSO/SitePages/Cloud-Application-Security-COE.aspx) if you need guidance

## 📞 Getting Help

- **GCISO Questions:** Email [rs.gcisosecuritycompliance@medtronic.com](mailto:)
- **LogicGate Access:** Log in to the [LogicGate portal](https://medtronic.logicgate.com)
- **Vulnerability Details:** Review Project Issues in GitLab
- **Remediation Help:** Join [GCISO Application Security Office Hours](https://medtronic.sharepoint.com/:u:/r/sites/GSO/Cloud%20and%20Application%20Security/GCISO%20Application%20Security%20Office%20Hours.ics?csf=1&web=1&e=cfpifQ) or contact the [Application Security Team](https://medtronic.sharepoint.com/sites/GSO/SitePages/Cloud-Application-Security-COE.aspx)

## 📚 Related Documentation

- [Security & GitLab Work items](../cicd-pipeline/security-gitlab-issues.md) - How to review, remediate, and track vulnerabilities
- [Security Overview](./index.md) - Security scanning tools and remediation timelines
- [Contrast Security Setup](../cicd-pipeline/security-gitlab-issues.md) - Runtime security analysis configuration
