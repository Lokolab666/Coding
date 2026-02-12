# The Tanzu Atlas Playbook: A Tiered Support and Priority-Based Framework for Internal Kubernetes Incidents

## Support Structure & Role Accountability

The operational framework for the Tanzu Atlas platform is built upon a multi-tiered support structure designed to efficiently triage and resolve incidents based on their technical complexity. This structure does not rigidly assign individuals to a single tier but instead leverages the diverse skill sets of its personnel, allowing them to operate across L1, L2, and L3 based on the nature of the incident . The primary goal is to ensure that routine issues are resolved quickly by the appropriate knowledge level, while complex, high-severity problems are escalated to personnel with the requisite deep expertise in Kubernetes architecture, Tanzu platform components, and underlying infrastructure. The defined roles—Support Engineer, Evangelist, Team Lead, and Kube-Admin—are the human assets that populate this structure, with their specific responsibilities and areas of focus delineated below. The entire support model operates within the confines of the ServiceNow ITSM platform, which serves as the central nervous system for tracking, communication, and process adherence [[3,9]].

The tiered support model is explicitly designed around Kubernetes-specific failure modes and operational tasks. L1 support functions as the first line of defense, focusing on well-documented, repeatable procedures. Their success is measured by their ability to leverage existing knowledge to prevent minor issues from escalating. L2 support acts as the architectural problem-solving layer, tasked with diagnosing non-standard configurations and debugging intricate interactions between platform components. Finally, L3 represents the pinnacle of technical authority, comprising platform architects and crisis managers who handle critical outages and guide major strategic initiatives. This structure ensures a logical flow of information and effort, optimizing resource utilization and resolution speed. The framework also extends beyond internal teams to include a formalized Stakeholder Engagement Framework, which defines communication protocols for interacting with technical peers, management, and external vendors like VMware and AWS, ensuring all parties are appropriately informed throughout an incident lifecycle [[2]].

The following table details the responsibilities, typical ticket complexity, and mapping of roles to each support tier. This matrix reflects the user's directive that roles can span multiple tiers depending on the context and the individual's current workload and expertise.

| Support Tier | Description | Primary Responsibilities | Typical Ticket Complexity Range | Mapped Roles |
| :--- | :--- | :--- | :--- | :--- |
| **L1 (Tier 1)** | First point of contact for routine, well-defined Kubernetes tasks. Focuses on resolving tickets using documented playbooks and KB articles. Acts as a filter to prevent low-complexity issues from consuming higher-tier resources. | Triage incoming tickets; categorize into types like Egress Access, GitLab CI Access, or Node Provisioning; execute predefined runbooks for common requests; validate configurations against established standards (e.g., checking new node requests against AWS quotas); communicate status updates via standardized ServiceNow templates. | Low (P3/P4). Tasks are procedural and have known solutions. | **Support Engineer** (Primary), **Kube-Admin** (as needed during off-hours or for overflow). |
| **L2 (Tier 2)** | Second line of support for complex configuration changes, debugging sessions, and architectural questions related to the Tanzu platform itself. Responsible for understanding how different components interact to diagnose root causes. | Troubleshoot non-standard incidents (e.g., FluxCD integration failures, intermittent pod restarts); debug namespace-level issues involving RBAC, Network Policies, or Pod Disruption Budgets; perform Root Cause Analysis (RCA) on recurring L1 issues to develop new playbooks; collaborate with architects and SMEs on more involved requests. | Medium (P2). Requires analytical skills and deeper Kubernetes knowledge than L1. | **Support Engineer** (Advanced), **Team Lead**, **Kube-Admin** (for specific technical deep-dives). |
| **L3 (Tier 3)** | Highest level of support, reserved for critical platform-wide failures and strategic architectural guidance. Possesses deep expertise in the Tanzu stack, including underlying infrastructure and integrated services. | Respond to P1 (Critical) incidents involving cluster availability, security breaches, or production application outages; lead major architectural initiatives and provide guidance to lower tiers; serve as the primary escalation point for unresolved P2 tickets; engage with vendors (e.g., VMware Tanzu support) for complex issues. | High (P1). Involves platform-wide impact and requires expert-level diagnosis and intervention. | **Evangelist**, **Team Lead**, **Kube-Admin**. |

The Stakeholder Engagement Framework is a critical component of this operational model, ensuring that communication is targeted, efficient, and appropriate for the audience's needs. It is structured into three distinct layers: Technical, Management, and Vendor stakeholders.

The **Technical Stakeholders** group includes individuals directly involved in the development and operation of systems that interface with the Tanzu Atlas platform. This includes Cluster Architects, who define the long-term vision and design of the clusters; GitLab/FluxCD Subject Matter Experts (SMEs), who are crucial for troubleshooting any issues related to the GitOps workflow; and Network Teams, whose policies and firewall rules can directly impact egress access and inter-cluster communication . Communication with this group is highly technical, focusing on diagnostics, potential root causes, and proposed resolutions. For example, when investigating a FluxCD integration issue, direct collaboration with the GitLab SME would be necessary to review repository configurations and pipeline logs.

The **Management Stakeholders** group comprises Area Leads, Vendor Managers, and broader IT Leadership. The primary Area Leads for two domains are included in this group, along with vendor managers and other IT leadership figures . Communication with this audience is summarized and business-impact focused, especially during P1 incidents. Updates are provided at a high level, emphasizing the current status, estimated time to resolution, and any escalations. For instance, during a P1 incident where a production cluster is partially unavailable, the Team Lead would notify the Area Leads via a dedicated channel, providing concise updates without requiring them to parse technical logs. This ensures management can make informed decisions about business continuity and resource allocation without being bogged down in technical minutiae.

Finally, the **Vendor Stakeholders** group consists of official support channels for the Tanzu platform and its underlying infrastructure. This primarily includes VMware Tanzu support channels and AWS escalation paths for issues related to the cloud provider's services [[1]]. The framework mandates the use of dedicated channels and formalized processes for vendor engagement to ensure issues are handled correctly and SLAs are tracked. For example, if an L2 engineer determines that a persistent control plane instability is likely a bug in Tanzu Kubernetes Grid Integrated Edition, they must follow a strict protocol to open a case with VMware, which would then be escalated to the L3 Evangelist for coordination [[7]]. The existence of a dedicated email address for VMware Cloud customers (`vmc-services-notices@vmware.com`) underscores the importance of having a formal liaison process for vendor communications [[2]]. This layered approach to stakeholder engagement ensures that the right people receive the right information at the right time, facilitating a smooth and transparent incident resolution process.

## Incident Classification & Priority Triggers

A precise and Kubernetes-native incident classification system is fundamental to the operational model, enabling a rapid and proportionate response to internal platform failures. The four-tier priority scale—P1 (Critical), P2 (High), P3 (Medium), and P4 (Low)—is defined exclusively by the technical severity and scope of the impact on the Tanzu Atlas platform, not by the perceived business urgency of the requestor . This technical-first approach ensures that the most severe threats to platform stability, security, and availability are addressed with the highest level of urgency and resource commitment. Each priority level is assigned a specific set of response roles and has a corresponding list of common ticket examples drawn directly from real-world operational scenarios. The entire classification process is managed within ServiceNow, where initial triage assigns a tentative priority, which can be formally reclassified based on deeper technical investigation [[3]].

The P1 (Critical) designation is reserved for incidents that cause a total or near-total loss of service for one or more Tanzu Atlas clusters or represent a significant threat to platform security. These events require an immediate, coordinated response from the highest level of technical experts. Common examples of P1 incidents include the complete failure of the Karpenter autoscaler to provision nodes, leading to a gridlock in workload scheduling ; a control plane instability event that prevents users from accessing the Kubernetes API server; or a widespread security misconfiguration, such as an overly permissive ClusterRoleBinding, that exposes the entire platform to risk. The response team for P1 incidents is composed of the most senior personnel, including the Evangelist, Team Lead, and Kube-Admin, who act as a war room to contain the incident and restore service .

P2 (High) incidents involve a significant degradation of service for a specific application, team, or cluster feature. While not a total outage, these events severely impact productivity and require a timely resolution. Examples include a developer being denied access to a namespace due to a misconfigured Role-Based Access Control (RBAC) policy, which halts development work ; a database connectivity issue originating from the Kubernetes side, caused by a misconfigured service account or a network policy blocking traffic to the database endpoint ; or intermittent application failures resulting from resource starvation or persistent scheduling errors. The response for P2 tickets involves a combination of Support Engineers, Team Leads, and Kube-Admins, who conduct deeper investigation and implement targeted fixes .

P3 (Medium) incidents are functional requests or minor issues that do not cause a major disruption to production environments. These are typically new feature requests, bug fixes for non-critical applications, or requests that fall outside the standard operational playbook. Representative tickets include requests to add egress access for a specific namespace, such as "Add egress NFS access for abrc1l-atlas-stg cluster to livelink-ignition namespace" , or inquiries about integrating an application with the GitOps toolchain, like "I would need to integrate AMI app with FluxCD on MECC Stage cluster" . The response team for P3 tickets is primarily composed of Support Engineers and Team Leads, who follow established procedures or develop new ones as needed .

P4 (Low) incidents are informational requests, documentation clarifications, or simple administrative tasks. They have no direct impact on application functionality or platform stability. An example is "Whitelist snowflake-related hosts in shared gitlab runner firewall" . The response to P4 tickets is handled by Support Engineers, often as part of their routine duties.

The workflow for reclassifying an incident's priority is strictly governed by technical reassessment, as specified by the user . This process is not discretionary but is triggered by new evidence discovered during the diagnostic phase of an incident. The following table outlines the conditions and workflow for priority escalation.

| Initial Priority | Trigger for Reclassification | Required Action & Analysis | Approved By | ServiceNow Action |
| :--- | :--- | :--- | :--- | :--- |
| **P4** | Investigation reveals the request affects a critical production namespace or impacts a core platform function (e.g., authentication). | The L1/L2 engineer documents the expanded technical scope in the ticket, citing specific namespaces, services, or platform components affected. | Team Lead / Kube-Admin | Change Priority field in ServiceNow; update SLA clock. |
| **P3** | A routine request uncovers a larger, systemic issue affecting multiple clusters or exposing a security vulnerability. | The L2 engineer identifies the root cause of the systemic issue (e.g., a flawed network policy template) and quantifies its impact. | Team Lead / Evangelist | Change Priority field in ServiceNow; send notification to relevant stakeholders. |
| **P2** | A seemingly isolated issue is found to be impacting multiple applications or clusters, approaching P1 criteria. | The L2 engineer performs a correlation analysis across cluster metrics and logs to demonstrate the wider impact. | Evangelist / Kube-Admin | Change Priority field in ServiceNow; initiate P1 communication protocols. |

This structured reclassification process ensures that the priority level always accurately reflects the true technical severity of the situation, guaranteeing that resources are allocated effectively and that stakeholders are kept informed of the actual risk to the platform. For example, a P4 ticket to "Whitelist Snowflake hosts" might be reclassified to P3 if it's for a staging environment, but could escalate to P2 if the investigation reveals that the same whitelisting rule was incorrectly applied to the production cluster, potentially exposing sensitive data. This technical-first logic is the cornerstone of a robust and defensible incident management process.

## Escalation Protocols & Technical Severity Reclassification

The escalation protocol is the engine of the operational model, dictating the formal transition of responsibility and resources between support tiers. It is not merely a queue-jumping mechanism but a structured process designed to address escalating levels of complexity, risk, and required expertise. The decision to escalate from L1 to L2 or from L2 to L3 is predicated on clear, objective triggers tied to the technical investigation of the incident, aligning with the principle that all actions are driven by a reassessment of technical severity, not business pressure . This ensures that tickets are advanced to the correct level of expertise at the earliest opportunity, preventing delays caused by personnel operating outside their zone of competence. Hypothetical scenarios are used here to illustrate the practical application of these escalation triggers in a Tanzu Atlas environment.

Escalation from L1 to L2 is triggered when the initial handler exhausts the available knowledge base and runbooks without achieving a resolution, or when the complexity of the problem exceeds the scope of Tier 1 expertise. There are several key triggers for this transition. The first is **Knowledge Exhaustion**, where an L1 engineer follows every step in the prescribed playbook for a given task, such as adding an egress rule, only to find the configuration fails validation or produces unexpected behavior . The second trigger is the **Complexity Threshold**, which occurs when an incident involves multiple interdependent Kubernetes components. For example, a ticket reporting intermittent pod failures might actually stem from a race condition between the Karpenter autoscaler provisioning a new node, the Antrea CNI plugin configuring networking on that node, and the kubelet starting the pods [[13]]. Such a scenario requires an L2 engineer's ability to trace the interaction across these subsystems. A third trigger is **SLA Risk**, where the L1 engineer determines they cannot meet the ticket's Service Level Agreement deadline and has exhausted all obvious troubleshooting steps. At this point, escalation to L2 is necessary to bring in more advanced diagnostic capabilities before the SLA is breached.

An illustrative hypothetical scenario for L1-to-L2 escalation involves an L1 engineer receiving a P3 ticket to add egress access for the `livelink-ignition` namespace on the `abrc1l-atlas-stg` cluster . Following the standard egress access playbook—which involves creating a firewall rule and applying it to the namespace—the engineer finds that the connection to the target NFS server times out. Standard troubleshooting suggests the rule is correct. However, further investigation reveals that the NFS server uses a self-signed certificate for TLS encryption, which is not trusted by the default trust store of containers running in the namespace. This is a nuanced issue that falls outside the L1 playbook. The engineer escalates the ticket to L2, providing the full diagnostic findings. The L2 engineer then takes over, responsible for researching how to inject a custom CA certificate into the namespace's config map, a task requiring deeper Kubernetes configuration knowledge. This is a classic and necessary escalation path.

Escalation from L2 to L3/Evangelist is reserved for the most severe and architecturally significant incidents. This transition is almost always triggered by a P1 priority designation stemming from a major platform failure. Other key triggers include **Architectural Impact**, where the solution to a problem necessitates a change to the platform's core design, such as redesigning the ingress strategy for all clusters after discovering a flaw in the current Avi Load Balancer configuration [[12]]. Another trigger is the need for **Vendor Involvement**, which arises when an issue appears to be a bug in a core platform component, like Tanzu Application Platform or the underlying vSphere environment [[1]]. In such cases, the L2 engineer lacks the authority and tools to engage VMware support directly and must escalate to an L3 Evangelist, who manages these critical relationships [[2]]. Finally, an escalation is required when the L2 engineer hits an **Irresolvable Blocker**, meaning they have identified the root cause but lack the necessary permissions or tools to implement the fix—for instance, needing to patch a core component of the Tanzu Control Plane.

A hypothetical scenario for L2-to-L3 escalation begins with an L2 engineer investigating a P2 ticket reporting that manual job pods in a staging namespace are restarting intermittently . After hours of debugging, examining logs, and analyzing cluster metrics, the engineer discovers that the restarts correlate perfectly with a recent automated patching event that upgraded the version of the Antrea CNI plugin. Further research confirms this is a known race condition in the new version that can cause the CNI to fail, leading to node unresponsiveness and pod evictions [[13]]. The L2 engineer has diagnosed the problem but cannot simply downgrade the CNI version themselves; this requires a planned maintenance window and coordination with platform engineering. The engineer escalates the ticket to the L3 Evangelist, providing all diagnostic evidence. The Evangelist then takes ownership, coordinating the rollback of the CNI version, managing communication with platform engineering, and updating the incident's priority to P1 due to the potential for similar issues on other clusters. This scenario highlights the necessity of the L2-to-L3 escalation path for resolving complex, platform-wide technical debt and emergent bugs.

## ServiceNow Communication Templates & Workflow

Effective communication is paramount in incident management, ensuring clarity, transparency, and accountability at every stage of the lifecycle. Within the Tanzu Atlas operational model, all communication related to an incident is channeled through the ServiceNow platform, leveraging its workflow capabilities to enforce a standardized process [[3]]. This includes specific templates for status transitions and priority reclassifications, which are designed to be professional, informative, and action-oriented. Adherence to these templates ensures that all stakeholders—from developers to management—receive consistent and valuable updates, reducing ambiguity and accelerating resolution. The workflow is designed to move tickets logically from initial receipt to final closure, with clear checkpoints for escalation and technical validation.

The ServiceNow status workflow is a structured sequence of states that an incident ticket progresses through. The primary statuses are `Unassigned`, `Work in Progress`, `Awaiting Customer Feedback`, `Resolved`, and `Closed` . An additional `Escalated` state may be used internally to flag tickets moving between support tiers. The transition between these states is accompanied by specific communication templates that provide essential context.

The following template is used when a ticket is moved from `Unassigned` to `Work in Progress`. This message serves to acknowledge receipt, assign ownership, and set initial expectations.
**Template: Unassigned → Work in Progress**
`Subject: Re: [Ticket Summary] - Action Started on INC-[TicketID]`

`Hi [Requestor Name],`

`My name is [Engineer Name], and I am taking ownership of your incident ticket, INC-[TicketID]. Thank you for the detailed report.`

`I have reviewed your request regarding "[Brief summary of ticket subject]." I will be working on this immediately. My initial assessment indicates that the issue is likely related to [provide a brief, high-level technical summary, e.g., 'a recent configuration change in the namespace'].`

`I will provide the next update by [Timeframe, e.g., end of day today] or sooner if I encounter any blockers or make significant progress. Please don't hesitate to reply to this thread with any further information.`

`Best regards,`
`[Engineer Name]`
`Tanzu Atlas Support`

When a technical solution has been implemented but requires verification from the requestor, the ticket is moved to `Awaiting Customer Feedback`. This status ensures the customer is an active participant in the resolution process.
**Template: Work in Progress → Awaiting Customer Feedback**
`Subject: Update on INC-[TicketID]: Verification Required`

`Hi [Requestor Name],`

`Following up on incident ticket INC-[TicketID], I have completed the necessary changes on our end to address the issue described in your request.`

`To verify the fix and confirm that everything is functioning as expected, we kindly request your assistance with the following action: [Clearly and concisely describe the specific action needed from the customer, e.g., 'Please attempt to redeploy your application to the staging environment and let us know if the deployment succeeds.']`

`Once you have performed the verification, please update the ticket with the outcome. If you encounter any further issues or have questions, please feel free to respond directly to this email.`

`Thank you for your cooperation.`

`Best regards,`
`[Engineer Name]`
`Tanzu Atlas Support`

If the investigation reveals that the technical severity of an incident is greater than initially assessed, the priority must be reclassified. This is a critical process that adjusts SLA expectations and resource allocation. The following template is used to communicate this change.
**Template: Status Update Due to Priority Reclassification**
`Subject: URGENT: Priority Update for INC-[TicketID] - Escalated to P[NewPrio]`

`Hi [Requestor Name],`

`Following further technical investigation into your ticket (INC-[TicketID]), my team and I have determined that the actual impact is greater than our initial assessment suggested. Based on our findings, the incident priority has been officially escalated from P[OldPrio] to P[NewPrio].`

`This reclassification reflects the broader technical scope of the issue, specifically [provide a clear, non-technical reason, e.g., 'its effect on the stability of the control plane' or 'the number of namespaces affected']. We are now treating this as a higher-severity event and have mobilized the appropriate level of support accordingly.`

`Under the new P[NewPrio] SLA, we will provide another update by [New Timeframe]. We appreciate your understanding as we prioritize this matter.`

`Best regards,`
`[Engineer Name]`
`Tanzu Atlas Support`

Finally, once the issue has been fully resolved and verified by the customer, the ticket moves to `Resolved` and then `Closed`.
**Template: Work in Progress → Resolved/Closed**
`Subject: Resolved: INC-[TicketID] - Issue Addressed`

`Hi [Requestor Name],`

`This is to inform you that incident ticket INC-[TicketID] has been successfully resolved.`

`The issue, related to [briefly summarize the root cause, e.g., 'misconfigured egress firewall rules'], has been corrected. Your application/service should now be functioning as expected.`

`For future reference, a Knowledge Base article has been created documenting this resolution: [Link to KB Article, if applicable]. This may help prevent similar issues.`

`If you experience any other problems or have further questions, please do not hesitate to open a new ticket.`

`Thank you,`
`[Engineer Name]`
`Tanzu Atlas Support`

These templates, integrated into the ServiceNow workflow, form the backbone of the communication strategy. They ensure that every interaction is documented, purposeful, and contributes to a positive and productive resolution experience for all parties involved.
