---
label: Alerting
icon: Bell
order: 1
---

# 🚨 Alerting & Notifications – Compass CI Platform

## 🌐 Overview

The Compass CI Platform uses **Amazon Managed Grafana alerting** integrated with **AWS SNS** to provide real-time monitoring and notifications.

Alerts are configured **per application**, aligned with dashboard structure, and logically grouped within Grafana.

---

## 🧱 Alerting Architecture

### 🔔 Alert Flow

```text
Metric / Condition Breach
        ↓
Grafana Alert Rule (per application)
        ↓
Notification Policy (label-based routing)
        ↓
AWS SNS
        ↓
Email / Subscribers
```

---

# 📊 Alert Organization

Alerts are structured per application scope within Grafana.

Each application has its own:

- Alert rules  
- Threshold configurations  
- Notification routing  

This ensures **isolation and scalability** across multiple applications.

---

## 📌 Alert Rules

- Defined directly in Grafana  
- Evaluated at regular intervals (e.g., every 1–5 minutes)  
- Triggered based on metric thresholds or conditions  

---

## 🔍 Common Alert Scenarios

- Pod CPU utilization over threshold  
- Pod memory utilization over threshold  
- Unexpected change in pod count  
- Application-level error patterns (e.g., JDBC errors/warnings)  
- Pod restart anomalies  

---

## 📡 Contact Points

Grafana uses **AWS SNS** as the notification channel.

### 🔔 Configuration

- Each contact point maps to an SNS topic  
- SNS distributes notifications to subscribed endpoints  

### 📬 Notification Channels

- Email (primary)  
- Additional subscribers (if configured in SNS)  

---

## 🔀 Notification Policies

Notification routing is handled using **label-based policies**.

### 📌 Default Behavior

- All alerts are routed to a default contact point unless overridden  

### 📌 Custom Routing

Alerts are routed based on labels such as:

```text
contacts=<group-name>
```


This enables:

- Logical separation of alerts  
- Targeted notifications per application or support group  

---

## 📦 Alert Grouping

Alerts are grouped using:

- `alertname`  
- `grafana_folder`  

This helps:

- Reduce alert noise  
- Improve readability  
- Organize alerts logically  

---

## 🔕 Mute Timings

- Currently not configured  
- Can be used to suppress alerts during maintenance windows  

---

## 📊 Alert States

Each alert can be in one of the following states:

- 🔴 **Firing** → Active issue detected  
- 🟢 **Normal** → No issue  
- ⏸️ **Paused** → Temporarily disabled  

---

## ⚠️ Known Limitations / Considerations

### ❗ Alert Noise
- Misconfigured thresholds may generate excessive alerts  

### ❗ Limited Context
- Alerts provide limited diagnostic information  
- Requires correlation with dashboards and logs  

### ❗ Manual Incident Handling
- No automatic incident management integration  
- Requires manual triaging and response  

### ❗ Dependency on Metrics
- Alert accuracy depends on CloudWatch metrics  
- Metrics latency may delay alert triggering  

---

## 🔮 Future Enhancements

### 🔗 Incident Management Integration
- Integration with external systems (e.g., ServiceNow)  
- Automated incident creation and tracking  

### 🤖 Intelligent Alerting
- AI-based anomaly detection  
- Reduction in false positives  

### ⚙️ Self-Healing Automation
- Automated remediation workflows  
- Reduced manual intervention  

---

## 🧭 Summary

The alerting setup in Compass CI:

- Is application-centric  
- Uses Grafana + AWS SNS integration  
- Supports label-based routing and grouping  
- Enables real-time monitoring and notifications  

This ensures:

- Faster detection of issues  
- Scalable alert management  
- Improved operational visibility  

