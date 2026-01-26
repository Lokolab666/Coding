Operational Support Model — Detailed Formal Descriptions

On-Prem Kubernetes (Atlas) — ServiceNow

Below is an expanded, formal, and comprehensive description of the Operational Support Model you requested. It includes long-form descriptions for every major section (roles, responsibilities, processes, SLAs, escalation, governance, tools, communications, metrics, training, and appendices). You can paste these sections directly into a formal SOP, governance playbook, or ServiceNow knowledge article set.


---

1. Executive Summary

This document establishes the Operational Support Model for the On-Prem Kubernetes (Atlas) platform and its associated services (Argo CI/CD, Source Code management, Artifactory, database services). It codifies responsibilities across support levels (L1, L2, L3), named roles (Evangelist, EM Lead / EM Support, Team Leads, Support Engineers), vendor engagement, and management stakeholders. ServiceNow is the authoritative source for incident, problem, and change management records. The model is designed to ensure consistent, auditable, and predictable operational support aligned with the client’s Service Level Agreement requirements and internal governance expectations.


---

2. Scope and Applicability

This Operational Support Model applies to:

All production and designated pre-production clusters of On-Prem Kubernetes (Atlas).

Platform services associated with the Kubernetes environment (Argo CI/CD, GitLab, Artifactory, Port.io, etc.).

All incidents, service requests, and operational changes raised through ServiceNow and assigned to the following support groups:

Infra-SourceCode IT Support-Global-L1

Infra-Argo IT Support-Global-L1

Infra-Kubernetes Platform IT Support-Global-L1

Infra-Manufacturing IT Support-Global-L1 (On-Prem Kubernetes)

Database



This model does not supersede security incident response procedures or vendor SLAs for externally managed services; those operate in parallel and reference their specific contractual escalation routes.


---

3. Detailed Support Levels & Responsibilities

3.1 Level 1 (L1) — Global IT Support (First Line)

Role Description (Formal):
L1 is the canonical first-line responder. L1 functions as the service desk for platform-related incidents and service requests across the Atlas ecosystem. The L1 team validates incoming records, performs initial troubleshooting steps defined in the runbooks, and ensures incidents are enriched with required telemetry to enable rapid downstream diagnosis.

Primary Responsibilities:

Acknowledge and log incidents in ServiceNow within the defined Response Time SLA.

Perform initial triage to validate the incident (determine false positive, reproducible, severity).

Execute documented L1 procedures and common runbook steps (check node and pod status, confirm monitoring alert details, gather relevant logs, and capture screenshots).

Populate ServiceNow incident fields completely: short description, impacted service CI, impacted namespace/pod/node, monitoring alert links, attached artifacts (logs, screenshots), initial triage notes, and suggested priority.

Apply straightforward known fixes and documented workarounds. If a known fix is used, document precisely which KB/playbook was applied and the resolution steps.

Route and escalate to the correct L2 group when rising above L1 scope, including providing summary of actions taken and artifacts collected.


Escalation Criteria to L2 (Formalized):

The required remediation is outside L1 authority or documented procedures.

Resolution has not been reached within the defined L1 time threshold (per SLA).

Investigation requires system-level credentials or changes (for example, kube-adm level or control-plane intervention).

Incident involves customer-facing outage requiring immediate, experienced operational intervention.



---

3.2 Level 2 (L2) — Team Leads (Advanced Technical Support)

Role Description (Formal):
L2 engineers are subject matter experts for platform operations. They possess in-depth knowledge of cluster operations, networking, storage, CI/CD interactions, and pipeline behavior. L2 leads the effort for sustained remediation work beyond initial triage and ensures appropriate technical escalation, documentation, and temporary mitigation when necessary.

Primary Responsibilities:

Receive escalations from L1 with complete context and artifacts.

Perform deep diagnostics (pod logs, kubelet and kube-api logs, CNI traces, storage health, monitoring timelines).

Validate environment and reproduce the failure in controlled conditions where possible.

Apply intermediate-level remediation steps and orchestrate safe operational fixes (draining or cordoning nodes, increasing replicas, rolling restart of pods, temporary traffic reroutes).

Coordinate cross-team actions (database teams, network ops, CI/CD owners) and create action items in ServiceNow where those teams must act.

Engage the Architect or Evangelist (per governance) when architectural implications or emergency change approvals are required.


Escalation Criteria to L3:

Situation demands code-level fixes or engineering changes not safe to perform in production without dev involvement.

Root cause appears architectural (persistent design issue) or requires patching platform components.

Vendor dependencies are implicated (hardware, proprietary drivers, or vendor-supplied controllers).



---

3.3 Level 3 (L3) — Evangelist / Platform Engineering (Deep Technical & Architecture)

Role Description (Formal):
L3 provides the highest level of operational expertise and system ownership for platform evolution and permanent problem resolution. The Evangelist acts as a cross-functional engineering liaison who synthesizes operational findings into long-term risk reduction and platform improvements.

Primary Responsibilities:

Lead root cause analysis for complex or recurring incidents.

Develop and validate permanent fixes, patches, or code changes, collaborating with platform engineering and development teams.

Approve or propose architectural changes; define mitigation strategies and verify compatibility and safety of emergency fixes.

Author and evolve playbooks and KBs to reflect permanent fixes and improvements.

Serve as the escalation point for vendor coordination where deep component or third-party involvement is required.



---

3.4 Medtronic Development / Architecture Team

Role Description (Formal):
This team defines long-term architectural direction, approves changes with systemic impact, and ensures that operational fixes align with the strategic platform roadmap.

Primary Responsibilities:

Review critical incident remediation proposals that have long-term architectural impact.

Approve emergency changes when required and coordinate formal follow-up through the change advisory process (CAB).

Participate in RCAs for incidents with architectural root causes and assign remediation owners.



---

3.5 Vendor Support

Role Description (Formal):
Vendors are engaged when incidents involve third-party hardware, managed services, or proprietary software. Their contractual SLAs and escalation processes apply in tandem with internal incident management.

Primary Responsibilities:

Accept vendor SRs (service requests) raised by the internal team.

Provide vendor diagnostics, patches, or action plans.

Maintain vendor case IDs and timelines in the ServiceNow incident record.

Coordinate with L2/L3 to validate vendor’s proposed fix before production application.



---

4. Stakeholders and Governance

4.1 Technical Stakeholders

Support Team (L1 / L2 / L3)

Evangelist (Platform & Source Code)

Team Leads (Kubernetes, Source Code)

Support Engineers (Kubernetes, Source Code)

Medtronic Architecture Team

Database Team

Infrastructure & Network Operations

Vendor Technical Contacts


4.2 Management-Level Stakeholders

Support Manager / Operations Manager — operational performance, resourcing, SLA compliance.

Product Owner / Service Owner — business priorities, customer communication, accept sign-offs.

Program / Delivery Manager — program-level coordination and escalation for multi-project impacts.

Customer Success / Business Relationship Manager — client-facing communications regarding SLA performance and escalations.

EM Lead / EM Support — governance, project execution oversight, risk and change approvals.


4.3 Named Role Assignments (Current)

Juan Pablo Mosquera — Platform Evangelist (Source Code)

William Urrea — Support Engineer (Source Code)

Daniel Cure — SER (Kubernetes)

Sebastian Guerrero — Support Engineer (Kubernetes)

Cristian Fandiño — Support Engineer (Kubernetes)


(These role assignments should be kept current in the Support Roster and on-call schedules in ServiceNow.)


---

5. Incident Management Process (Formal Flow)

This process is the institutionalized lifecycle for incident handling. It must be enforced by policy, supported by ServiceNow workflows, and embedded into operational runbooks.

1. Detection and Intake

Incident is either auto-created by monitoring or manually reported by a user. The incident must include automatic enrichment (monitoring alert IDs, metrics, timestamps).

ServiceNow fields should be prepopulated where possible (impact, urgency, CI).



2. Classification and Prioritization

Apply priority mapping (Impact x Urgency → Priority). Suggested priority mapping: P1 (Critical), P2 (High), P3 (Medium), P4 (Low).

Tag incident with service CI = Atlas-K8s and affected namespace/pod/node.



3. Assignment to L1

L1 acknowledges and sets initial status. L1 must perform the required L1 checklist within the Response Time SLA.



4. L1 Triage

Validate, eliminate false positives, and execute known fixes. If resolved, perform closure steps and KB updates where applicable.



5. L2 Escalation & Investigation

For unresolved or platform-level incidents, escalate to L2 with all collected artifacts. L2 executes advanced diagnostics and temporary mitigations.



6. L3 / Vendor Engagement

If the incident requires code changes, architecture decisions, or vendor intervention, escalate to L3/Evangelist and open vendor SRs with complete logs and ServiceNow linkage.



7. Mitigation and Resolution

Apply fix or workaround. Verify service restoration across affected dimensions (functional tests, monitoring metrics, user confirmation).



8. Verification and Closure

Confirm resolution, update incident with closure notes and remediation steps, and set status to Resolved. If further work is required (change requests), create follow-up CRs/PROB tickets.



9. Post-Incident Review (RCA)

For P1/P2 or repeat incidents, schedule RCA within 48–72 hours. Produce an RCA document that includes timeline, root cause, corrective and preventive actions, owners, and due dates. Attach RCA to the incident record.





---

6. Service Level Agreements (SLAs) — Formal Definitions & Suggested Targets

> Note: The numerical targets below are recommended baseline values and should be validated/negotiated with the client and reflected in contractual SLAs.



6.1 Response Time (SLA Definition)

Formal Definition:
Response Time measures the elapsed time between the point at which an incident is assigned to the support group in ServiceNow and the moment when a support agent takes ownership and marks the incident status as Work in Progress.

Start time: Incident assignment timestamp to one of the relevant support groups.
Stop time: Incident status transition to Work in Progress with an assigned agent.

Suggested Targets:

P1 (Critical): ≤ 15 minutes

P2 (High): ≤ 30 minutes

P3 (Medium): ≤ 2 hours

P4 (Low): ≤ 8 business hours


Communication Template (Initial Update — English):

> “Hi, my name is [First Last]. Thank you for reaching out. I will be working on your incident and will provide the next update within [cadence].”



Template (Spanish):

> “Hola, mi nombre es [Nombre]. Gracias por contactarnos. Estaré trabajando en su incidente y proporcionaré la siguiente actualización en [plazo].”



6.2 Resolution Time (SLA Definition)

Formal Definition:
Resolution Time measures total elapsed time from assignment to the support group until the incident status is set to Resolved in ServiceNow. The SLA may be paused for valid waiting states.

Start time: Incident assignment to the support group.
End time: Incident state transitions to Resolved.

SLA Pause States (Allowed only when justified):

Awaiting Customer Feedback

Pending Change

Awaiting Vendor

Pending Vendor Information


Suggested Targets:

P1 (Critical): Initial mitigation within 1 hour; full resolution within 4–8 hours (depending on severity).

P2 (High): Full resolution or workaround within 4 hours where feasible; otherwise defined remediation plan in 24 hours.

P3 (Medium): Resolution within 2 business days.

P4 (Low): Next available maintenance window or backlog schedule.



---

7. Escalation Model — Formal Matrix

This section codifies when and how to escalate, who to notify, and expected timelines.

7.1 Escalation Triggers

Time-based escalation: If the incident remains in a given state past the SLA threshold, escalate to the next support level and notify management.

Impact-based escalation: If incident causes broad service impact (multiple namespaces, >x% customers, production outage), immediately escalate to declare P1, assign Incident Commander, and notify COMMS and Management.

Complexity escalation: If incident requires code changes, architecture evaluation, or vendor involvement, escalate to L3/Evangelist and Vendor.


7.2 Notification Recipients (Example)

Immediate: Assigned L2 on-call, Platform Owner (Service Owner), Team Lead.

P1: Incident Commander, Evangelist (L3), Architect, Vendor contact, COMMS, Support Manager, Delivery Manager.

Stakeholder escalation path: L1 → L2 → L3 / Evangelist → Architect / EM Lead → Program/Delivery Manager → Executive Sponsor.


7.3 Escalation Communication Cadence for Major Incidents (P1)

Initial notification: Immediate on declaration.

Status updates: Every 15 minutes until mitigation is applied; after mitigation, every 30–60 minutes until stable.

Post-incident: RCA scheduled within 48–72 hours with cross-functional attendance.



---

8. Communications and War Room Procedures

8.1 War Room Activation

When: A Major Incident is declared (P1) or when multiple services are critically impacted.
Who activates: Incident Commander, or the on-call L3/Evangelist in absence of designated IC.
How to activate: Create an incident bridge (conference call / chat channel) and post the bridge link in the ServiceNow incident. Notify stakeholders per the escalation matrix.

8.2 Communication Roles

Incident Commander (IC): Single point responsible for overall coordination, decisions on mitigation, and approvals for emergency changes.

Scribe: Documents actions, timestamps, decisions, and rollbacks in the ServiceNow incident notes.

Technical Lead(s): L2/L3 engineers executing mitigations.

COMMS Lead: Prepares external/internal updates and status page entries.

Vendor Liaison: Coordinates with vendor and tracks vendor SRs/case IDs.


8.3 Standard Status Update Template

Header: Incident INC-XXXXX — [Short description] — P[1/2/3/4]
Time: [UTC / Local timestamp]
Summary: [Brief description of issue and impact]
Current Status: [What is being done]
Actions Taken: [List each action, owner, and timestamp]
Next Steps: [Owners and ETA]
Bridge / Chat Link: [URL]
Scribe: [Name]


---

9. Knowledge Management & Playbooks

9.1 KB Structure and Governance

KB Article Types: SOPs, Playbooks (operational runbooks), Troubleshooting Guides, Post-Incident RCA Summaries, Architecture Notes.

Ownership: Each KB article must have an assigned owner (Team Lead or Evangelist). Owners are responsible for annual reviews or updates after significant incidents.

Quality Gate: KB articles must include: purpose, scope, preconditions, step-by-step actions, rollback steps, verification, required artifacts, and the contact list.


9.2 Playbook Example (L1 Triage)

Preconditions: Incident with CI = Atlas-K8s and severity P2/P3.

Step 1: Confirm monitoring alert with timestamp and link.

Step 2: Run kubectl get nodes -o wide and kubectl get pods --all-namespaces. Attach outputs.

Step 3: Check last GitLab deployment (commit IDs) for affected namespaces.

Step 4: If pod CrashLoopBackOff, capture logs kubectl logs <pod> --previous.

Step 5: If fix was applied, test application endpoints; attach screenshots and monitoring snapshots.

Step 6: If unresolved after X minutes, escalate to L2 with attached artifacts and triage notes.



---

10. Change Management & Emergency Changes

All changes follow the Change Management process in ServiceNow.

Emergency change policy: The Incident Commander may authorize emergency changes during a P1 given approval from the Architect or EM Lead; such changes must be recorded as Emergency Change records and reviewed in post-incident CAB.

Permanent fixes must be routed through the standard change process (CAB) with risk assessment and rollback plan.



---

11. Security, Compliance & Data Privacy

Any incident involving suspected security compromise must be managed by the Security Incident Response process immediately and escalated to the security team.

Logs and artifacts stored in ServiceNow must comply with data retention and privacy policies; remove/obfuscate PII before sharing externally.

Vendor data movements or debug sessions must be authorized and logged.



---

12. Operational Metrics, Reporting & Continuous Improvement

12.1 Metrics to Track (Formal)

SLA Compliance: % incidents meeting Response and Resolution SLAs (by priority).

MTTR: Mean Time To Repair (segmented by service and priority).

MTTA: Mean Time To Acknowledge.

Incident Volume: Trending by week/month and by root cause category.

Repeat Incidents: Count and % of recurring incidents (same root cause within defined window).

KB Coverage: % of common incidents with documented KB/playbook.

Change Success Rate: % emergency and regular changes with rollback or incident occurrence.


12.2 Reporting Cadence

Daily: Dashboard review and handover notes.

Weekly: Trend report, top 5 recurring issues, pending action items.

Monthly: SLA performance report and executive summary to Program/Delivery Manager.

Quarterly: Strategic review with Architecture, Product, and Executive stakeholders to track systemic improvements.



---

13. Training, Onboarding & Knowledge Transfer

Onboarding checklist for new support engineers: Access provisioning (ServiceNow, cluster kubeconfig with least privilege), review of playbooks and SOPs, shadowing sessions (two weeks) with L2/L3, and assessment.

Continuous training: Scheduled knowledge-sharing sessions monthly; incident walkthroughs and RCA lessons learned sessions after major incidents.

Certification: Encourage Kubernetes certification paths and internal lab exercises for hands-on scenario practice.



---

14. Roles: Expanded Formal Descriptions

14.1 EM Lead (Executive Manager Lead)

Responsibilities (Formal):
Oversee program governance, ensure alignment between operations and business objectives, approve high-impact changes, manage budgets and resource allocations, and orchestrate cross-team escalations when strategic decisions are required.

14.2 EM Support

Responsibilities (Formal):
Coordinate local project setup, manage resource plans, provide financial and operational inputs for project management, and ensure consistent operational execution at the local level.

14.3 Evangelist

Responsibilities (Formal):
Act as the platform champion; bridge developer experience and operational requirements; promote best practices for CI/CD pipelines and self-service patterns; advise on platform adoption and continuous improvement initiatives.

14.4 Team Leads (Kubernetes & Source Code)

Responsibilities (Formal):
Drive technical direction for their respective domains, own runbook content and team-level KB, lead incident handling for escalations, and coordinate with architecture for design decisions.
14.5 Support Engineers (Kubernetes & Source Code)
Responsibilities (Formal):
Deliver day-to-day operational support, execute runbooks, escalate appropriately, and contribute to KB content from operational lessons learned.
15. Appendix — Templates & Examples
15.1 ServiceNow Incident Fields & Example Values
Field
Example Value / Notes
Short description
“Atlas-K8s: namespace X – API errors / pod CrashLoopBackOff”
CI / Service
Atlas-K8s
Impact
“Multiple customers / Production”
Urgency
High
Priority
P1 / P2 / P3 / P4
Assigned group
Infra-Kubernetes Platform IT Support-Global-L1
Assigned to
[Agent username]
Status
New → In Progress → Resolved → Closed
Monitoring alert link
URL to Grafana/Datadog alert
Attached artifacts
Logs, screenshots, kube outputs, pipeline commit IDs
Incident Commander
[Name] (for P1)
Vendor SR link
[Vendor case URL]
Major Incident flag
Checkbox (true for P1)
15.2 Initial Update — ServiceNow Comment (Formal)
English:
Hello, my name is [Full Name], and I will be your point of contact for this incident. We have received your report and begun initial triage. Current activity: [short summary of what you checked]. Next update: [time cadence]. Incident reference: INC-XXXXX.
Spanish:
Hola, mi nombre es [Nombre Completo], seré su contacto para este incidente. Hemos recibido su reporte y comenzado la investigación inicial. Actividad actual: [resumen corto]. Siguiente actualización: [plazo]. Referencia del incidente: INC-XXXXX.
15.3 Vendor Escalation Note (ServiceNow Comment)
Vendor SR opened: [VendorCaseID]. Summary: [Short technical summary]. Attached: logs, diagnostics, timeline. Please prioritize per P1 status. Contact: [Vendor escalation contact]. Link: [URL]
15.4 RCA Template (Required Sections)
Title & Incident ID
Date/Time window (detection → resolution)
Summary (one paragraph)
Impact (services, customers, duration)
Timeline of events (timestamped)
Root cause (technical and contributing factors)
Corrective actions taken (short and long term)
Action items (owner, due date)
Lessons learned & KB/Playbook updates required
Approval & distribution list
15.5 Post-Incident Checklist
Verify all systems are stable and monitored for reoccurrence.
Close any vendor SRs if resolved, or keep vendor SRs linked for pending actions.
Create follow-up CR/PROB items with clear owners and schedules.
Update or create KB/playbook reflecting the resolution.
Schedule RCA and invite required stakeholders (L2/L3, Architect, Vendor as needed).
Share executive summary with management and Customer Success.
16. Continuous Improvement & Governance
Maintain a quarterly governance meeting that reviews SLA performance, recurring incidents, and strategic remediation progress.
Each quarter, prioritize technical debt and platform hardening items in the roadmap to reduce incident volume.
Maintain a living KB and require owners to review critical playbooks within 30 days of any P1/P2 incident.
17. Final Notes & Implementation Next Steps
Operationalize ServiceNow: Ensure auto-creation of incidents from monitoring, field mappings, and escalation automations for the Major Incident flag.
Populate Roster & On-Call Schedules: Maintain current named roles and contacts in ServiceNow and in the emergency contact sheets.
Publish KB and Playbooks: Ensure L1/L2 have access and that playbooks are versioned with change history.
Run a Tabletop Exercise: Validate the model by running a Major Incident tabletop exercise with all stakeholders.
Agree SLA Targets: Review recommended SLA targets with the customer, codify them into the Service Level Agreement, and implement monitoring for SLA compliance.
