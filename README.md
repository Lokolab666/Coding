# Operational model — Incident escalation for on-prem Kubernetes (Atlas) using ServiceNow

Below is a complete, ready-to-use operational model for incident escalation tailored to your context (ServiceNow + on-prem Kubernetes / Atlas). It includes the escalation levels, stakeholder roles (including the requested IDs 11 / 12 / 13), responsibilities, timelines, and runbook-style actions for each severity. You can paste this into your SOP/KB or use it to build ServiceNow workflows and notification rules.

---

# 1 — Scope & assumptions

* Platform: On-prem Kubernetes (Atlas).
* Ticketing: ServiceNow is the single source of truth for incident records.
* Alerts can be created either automatically (monitoring/alerting) or manually (user / support).
* This model covers operational incident handling from detection → resolution → post-incident review (RCA).
* Stakeholder IDs: you provided “11 12 13 architect vendor” — I mapped those to concrete roles below so they can be referenced in ServiceNow, runbooks and escalation matrices.

---

# 2 — Stakeholders & roles (including IDs 11, 12, 13)

| Stakeholder ID | Role name                                              | Primary responsibilities                                                                                                   |
| -------------: | ------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------- |
|             01 | L1 Support (Service Desk)                              | Ticket intake, initial validation, basic triage, update ServiceNow, escalate if beyond scope.                              |
|             02 | L2 / Platform Operations (DevOps)                      | Troubleshoot kubernetes nodes, pods, networking, storage; implement known fixes; update runbooks.                          |
|             03 | L3 / Site Reliability Engineering (SRE)                | Deep platform debugging, cluster control plane, etcd, kubelet, CNI, etc.                                                   |
|             11 | Architect                                              | Provide architecture guidance, risk assessment for config changes, approve emergency changes, make design-level decisions. |
|             12 | Vendor (third-party HW/SW support)                     | Provide vendor escalation for hardware, firmware, supported software components; accept vendor SR and track progress.      |
|             13 | Platform / Product Owner                               | Business impact decisions, prioritization, customer communications, final sign-off on SLAs & RCA.                          |
|            MGT | Management Escalation (Ops Manager / Incident Sponsor) | Escalate to leadership, approve cross-team resource allocation, stakeholder communications.                                |
|          COMMS | Communications / Customer Success                      | External/internal communications, status pages, customer notifications.                                                    |

> Note: replace placeholder contacts (emails/phones) in the final document with real on-call routing entries and ServiceNow group names.

---

# 3 — Severity classification & SLAs (suggested)

Define severity in ServiceNow (Priority mapping used by runbooks):

* **P1 (Critical / Major Incident)** — Cluster control plane down, multiple customer services down, data loss, or security compromise.

  * Response: Incident Commander within **15 minutes**, full war room.
  * Target: mitigation / workaround within **1 hour** (adjust to your org SLAs).
* **P2 (High)** — Production service degraded or large subset of customers impacted.

  * Response: Affected platform owner & L2 within **30 minutes**.
  * Target: mitigation within **4 hours**.
* **P3 (Medium)** — Single service degraded, limited users affected.

  * Response: L1/L2 triage within **2 hours**.
  * Target: resolution within **2 business days**.
* **P4 (Low)** — Non-urgent requests, configuration changes, feature requests.

  * Response: Route to backlog / change process.

(Adjust exact timing to your organizational SLAs. Use these as policy defaults.)

---

# 4 — Incident lifecycle & escalation flow (high level)

1. **Detection / Intake**

   * Automated alert (monitoring) or manual report → create ServiceNow incident (automatically if integrated) with required fields: `Short description`, `Impact`, `Urgency`, `Priority`, `Service CI` (Atlas-K8s), `Affected Pods/Namespaces`, `Cluster node(s)`, `Logs / Correlation IDs`, `Monitoring alert IDs`, `Initial steps taken`.
2. **Initial Triage (L1)**

   * Validate alert (false positive?), gather basic telemetry: `kubectl get nodes/pods`, check cluster metrics/dashboards.
   * If resolvable by known playbook → L1 fixes and resolves ticket.
   * If not → escalate to L2 (assign to Platform Operations) and update ticket with triage notes.
3. **Investigation (L2 / L3)**

   * L2 performs deeper troubleshooting (pod logs, events, node health, CNI, storage).
   * If the cause is platform-level (etcd, kube-api, CNI bug), escalate immediately to L3 and notify Architect (11) if architectural impact or emergency change is required.
4. **Major Incident Declaration**

   * If P1 conditions apply, declare Major Incident in ServiceNow: assign an **Incident Commander (IC)**, open a war room, enable incident bridge, add stakeholders: COMMS, MGT, Architect (11), Vendor (12) as needed, and Platform Owner (13).
5. **Mitigation & Recovery**

   * Apply approved mitigations / runbook actions. If vendor/hardware is implicated, open vendor SR and escalate to vendor (12).
   * Document all actions in ServiceNow (step, who, timestamp).
6. **Resolution & Recovery**

   * Confirm services restored. Close incident after verification steps and stakeholder sign-off (Platform Owner (13) and IC).
7. **Post-Incident**

   * Open a Post-Incident Review (RCA) ticket; classify root cause, corrective actions and schedule follow-up changes or process improvements. Architect (11) and Vendor (12) participate for technical RCA aspects; Platform Owner (13) owns business impact notes.

---

# 5 — Detailed escalation matrix (who to call and when)

| Trigger / Condition                                   |                                      Primary action | Escalate to (if not resolved)                      |                                                         When to escalate |
| ----------------------------------------------------- | --------------------------------------------------: | -------------------------------------------------- | -----------------------------------------------------------------------: |
| Automated monitor: Node unreachable / kube-api errors |                                L1 triage (validate) | L2 if continues > 15 min                           |                                             Immediately after validation |
| Multiple namespaces failing / service down            |         L2 investigate, create Major Incident if P1 | L3 + Architect (11) + COMMS + MGT                  |                               If customer service outage / >5% customers |
| Storage backend failing (persistent volumes)          |                                 L2/L3 investigation | Vendor (12) if HW/third-party storage              | After initial troubleshooting shows hardware / vendor driver implication |
| Security incident (suspected compromise)              | Activate security incident process; isolate cluster | Security team + MGT + Architect (11) + Vendor (12) |                                                  Immediate (treat as P1) |
| Emergency configuration rollback needed               |                      IC authorizes emergency change | Architect (11) for approval; MGT notified          |                                           As soon as rollback plan ready |

Include names/phone/email/on-call group in ServiceNow contact fields.

---

# 6 — Runbook actions by severity (concise, copyable steps)

## P1 (Major Incident) — quick runbook

1. Mark incident **P1** in ServiceNow and set Major Incident flag.
2. Assign Incident Commander (IC); IC creates war room and incident bridge (record the bridge link in ticket).
3. Notify stakeholders: COMMS, MGT, Architect (11), Vendor (12), Platform Owner (13).
4. Gather telemetry: `kubectl get nodes -o wide`, `kubectl get pods --all-namespaces`, control-plane logs, monitoring dashboard snapshots. Attach to ticket.
5. Apply immediate mitigation (scale replicas, restart kube-components, isolate node) from approved emergency playbooks. Document every step in ticket.
6. If vendor involvement required: open vendor SR, include ticket number and attach logs, and request vendor priority escalation.
7. After service restored: IC coordinates verification, records exact time of recovery, and moves to RCA planning.

## P2 (High)

1. L2 owns ticket; perform focused troubleshooting: pod logs, events, recent deployments.
2. Apply known fix or workaround; if not available escalate to L3.
3. Update customer and Platform Owner (13) periodically (every agreed cadence).
4. If root cause is config/design, involve Architect (11) and schedule change as per CAB.

## P3 / P4

* Follow regular operational procedures; create change requests for fixes that require scheduled maintenance.

---

# 7 — ServiceNow configuration recommendations (fields, automation)

Required fields to include on incident form (makes escalation consistent):

* Short description (auto: alert summary)
* Impact (select from dropdown)
* Urgency (auto-map to priority)
* Priority (auto)
* CI / Service: `Atlas-K8s`
* Affected namespace/pod/node (text)
* Monitoring alert ID / Link (URL)
* Attach: logs, diagnostic snapshots
* Incident Commander (user)
* Stakeholders to notify: checkboxes (Architect (11), Vendor (12), Owner (13), COMMS, MGT)
* Major Incident: boolean
* Vendor SR link & vendor case ID

Automations:

* When `Major Incident` checked → auto-assign IC role, create incident bridge, trigger COMMS notification, open a parent Major Incident record, and page on-call groups.
* Integrate monitoring alerts to auto-create incidents with pre-filled fields and severity mapping.

---

# 8 — Communication templates (short)

**ServiceNow comment — initial acknowledgement**

> Thank you — we have received the report and opened Incident INC-XXXXX (Priority P2). Platform Operations is triaging. Next update in 30 minutes unless otherwise notified.

**Major incident public status update**

> Incident INC-XXXXX (P1): We are aware of the Atlas Kubernetes outage affecting [service list]. Incident Commander: [name]. Mitigation actions are in progress. Next update: in 15 minutes or sooner.

**Vendor escalation note**

> Vendor SR opened: [vendor_case_id]. Summary: [brief summary]. Logs and diagnostics attached in ServiceNow. Please assign to Tier 2 support and prioritize as P1.

---

# 9 — Evidence & logs to collect (attach to ticket)

* `kubectl get nodes -o wide` and `kubectl describe node <node>`
* `kubectl get pods --all-namespaces -o wide` and `kubectl logs <pod>` for failing pods
* Control plane logs (kube-api, etcd) and timestamps
* Monitoring graphs (CPU, memory, network, etc.) and alert IDs
* Recent GitLab CI pipeline deploys (commit IDs) that targeted affected namespaces
* Any error screenshots or console outputs

---

# 10 — Post-incident actions (RCA & continuous improvement)

* Within 48–72 hours: owner (Platform Owner 13) schedules RCA meeting with Architect (11), L3, Vendor (12) if involved.
* Produce an RCA document: timeline, root cause, contributing factors, corrective actions, owners and due dates. Attach to ServiceNow incident.
* Track follow-up change requests and close action items in a follow-up ticket; add updates to KB / SOP if runbook adjustments are needed.

---

# 11 — Example escalation matrix (compact, copy into KB)

1. **L1** — Validate, basic fixes — If unresolved in 15 min → → **L2**
2. **L2** — Platform ops troubleshooting — If unresolved or platform impact → → **L3** & notify **Architect (11)**
3. **L3** — Deep platform troubleshooting — If hardware or vendor-owned component → → **Vendor (12)** and **Platform Owner (13)**
4. **Major Incident** — Incident Commander appointed, COMMS & MGT notified immediately

---

# 12 — Quick checklist (for the person who receives an incident)

* [ ] Is the alert valid (false positive)?
* [ ] Create ServiceNow incident with required fields and attach telemetry.
* [ ] Set Priority based on impact/urgency matrix.
* [ ] Notify L2 / on-call platform ops.
* [ ] Escalate to Architect (11) if design/config change or P1.
* [ ] If hardware or vendor software implicated → open vendor SR (12) and attach logs.
* [ ] For P1 → start war room, assign IC, notify COMMS and MGT.
* [ ] Document every troubleshooting step in ServiceNow.
* [ ] After closure, schedule RCA with Architect (11), Vendor (12) and Platform Owner (13).

---

If you want, I can now:

* produce a printable one-page flowchart (text or diagram code) of the escalation path, or
* draft the actual ServiceNow form templates and notification rules (exact field names and example values), or
* write the exact Major Incident checklist that can be copy-pasted into the Runbook.

Pick one and I’ll produce it next (I’ll proceed immediately with what you choose).
