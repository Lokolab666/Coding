---
label: Overview
icon: pulse
order: 5
---

# 📡 Monitoring & Observability – Compass CI Platform

---

## 🌐 Overview

**Compass** is Medtronic’s Kubernetes-based CI/CD Web Application Hosting Platform running on **Amazon EKS**. Monitoring and Observability are foundational capabilities that ensure:

- High application reliability
- End-to-end operational visibility
- Strong audit and compliance readiness

This document provides a **comprehensive and structured view** of how monitoring, logging, alerting, and observability are implemented across Compass-hosted applications.

---

## 🎯 Objectives

The Compass Monitoring & Observability framework is designed to:

- ✅ Ensure **application availability and reliability**
- 📊 Provide **real-time infrastructure and application visibility**
- 🔍 Enable **rapid troubleshooting and root cause analysis (RCA)**
- 🚨 Support **automated alerting and incident detection**
- 🛡️ Maintain **audit and compliance readiness**
- 📐 Standardize **observability practices across all applications**

---

## 📌 Scope

This documentation covers the complete observability lifecycle:

- 🏗️ Observability Architecture *(EKS → CloudWatch → Grafana)*
- 📜 Logging Framework & Standards
- 📈 Metrics & Dashboard Design
- 📊 Grafana Architecture & Variables
- 🔄 Flux CD Deployment Monitoring
- 🚨 Alerting & Notification Model
- 🛠️ Troubleshooting Playbooks
- 🔐 Access Control & Governance

---

## 🧱 Platform Observability Stack

The Compass observability ecosystem is composed of the following layers:

| Layer | Tool / Service | Purpose |
|------|--------------|--------|
| 🖥️ Compute | Amazon EKS | Application hosting |
| 📜 Logging | AWS CloudWatch Logs | Central log storage |
| 📊 Metrics | CloudWatch Container Insights | Kubernetes & infra metrics |
| 📈 Visualization | Amazon Managed Grafana | Dashboards, logs, alerts |
| 🚨 Alerting | Grafana Alerts + AWS SNS | Notifications |
| 🔄 Deployment | Flux CD | GitOps deployment monitoring |

---

## ⚙️ Key Capabilities

### 🧾 1. Centralized Logging
- Logs collected from **Kubernetes pods (stdout/stderr)**
- Stored centrally in **AWS CloudWatch**
- Accessible via:
  - 📊 Grafana dashboards
  - 🔎 CloudWatch Logs Insights

---

### 📊 2. Real-Time Metrics Monitoring
- CPU, Memory, Pod, and Node metrics
- Namespace-level and pod-level visibility
- Kubernetes resource utilization tracking

---

### 📈 3. Unified Dashboards
- 📌 Per-application dashboards in Grafana
- 🌍 Multi-environment support via namespace filtering
- ♻️ Reusable templates with dynamic variables

---

### 🚨 4. Alerting & Notifications
- Alerts configured in **Grafana**
- Notifications routed via **AWS SNS**
- Enables **proactive issue detection**

---

### 🔄 5. Deployment Observability
- Flux CD provides:
  - ✅ Sync status visibility
  - 🔍 Drift detection
  - ❌ Deployment failure insights

---

### 🔐 6. Security & Traffic Observability
- 🛡️ WAF logs integrated into dashboards
- 🌐 Ingress-level request visibility
- 📡 End-to-end traffic monitoring

---

## 🔁 High-Level Observability Flow

```text
Application Pods (EKS)
        ↓
Container Logs (stdout/stderr)
        ↓
AWS CloudWatch Logs
        ↓
Amazon Managed Grafana
        ↓
Dashboards / Alerts / Troubleshooting
```

---

This documentation is intended for the following stakeholders:

- 👨‍💻 **Application Teams**
- 🛠️ **ARGO Platform Support Team**
- 🏗️ **ARGO Platform Engineering Team**
- 📊 **Leadership / Management**
- 📋 **Audit & Compliance Teams**

---

## 💼 Business Value

The Compass Observability Framework delivers measurable operational and business benefits:

- ⚡ Faster incident detection & resolution (**Reduced MTTR**)
- 📈 Improved platform reliability and stability
- 🧭 Clear ownership and operational workflows
- 🔍 Enhanced debugging and root cause analysis (RCA) capabilities
- 📐 Standardized monitoring practices across applications
- 🛡️ Strengthened audit and compliance posture

---

## 🚀 Current State vs Future Enhancements

### ✅ Current State

The current platform capabilities include:

- 📜 CloudWatch-based centralized logging
- 📊 Grafana dashboards and alerting
- 📣 SNS-based notification model
- 🔄 Flux CD deployment monitoring

---

### 🔮 Planned Enhancements

The platform roadmap includes the following enhancements:

- 🔗 ServiceNow incident integration
- 🤖 AI-based root cause analysis (RCA)
- 📊 Predictive monitoring capabilities
- ⚙️ Automated remediation framework
- 📉 MTTR analytics dashboards

---

## 🧭 Summary

The Compass Monitoring & Observability platform provides a **scalable, centralized, and standardized framework** that empowers teams to:

- 👀 Monitor systems effectively
- 🚨 Detect issues proactively
- 🛠️ Troubleshoot efficiently
- 🚀 Operate with confidence

---
