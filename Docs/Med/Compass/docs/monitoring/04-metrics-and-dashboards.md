---
label: Metrics
icon: graph
order: 2
---

# 📊 Metrics & Dashboards – Compass CI Platform

## 🌐 Overview

The Compass CI Platform provides **real-time metrics monitoring and visualization** using AWS CloudWatch and Amazon Managed Grafana.

This enables teams to gain deep visibility into:

- Application performance
- Kubernetes resource utilization
- Infrastructure health
- Operational trends

All metrics are centralized, standardized, and visualized through **application-specific Grafana dashboards**.

---

## 🎯 Objectives

The metrics and dashboards framework is designed to:

- 📊 Provide real-time visibility into system health
- 🚨 Enable proactive issue detection
- 📈 Support performance monitoring and optimization
- 🔍 Assist in debugging and root cause analysis (RCA)
- 📐 Standardize dashboards across applications
- ⚙️ Enable scalable monitoring across environments

---

## 🧱 Metrics Architecture

### 📊 Metrics Collection Flow

```text
Kubernetes (EKS)
        ↓
AWS CloudWatch Container Insights
        ↓
AWS CloudWatch Metrics
        ↓
Amazon Managed Grafana
        ↓
Dashboards / Alerts / Analysis
```

---

## 📥 Metrics Source

## 🟢 AWS CloudWatch Container Insights

Provides Kubernetes-native metrics such as:

- Node-level metrics
- Pod-level metrics
- Container-level metrics
- Cluster-level insights

---

## 📊 Types of Metrics Collected

## 🖥️ Infrastructure Metrics

- CPU Utilization
- Memory Utilization
- Disk Usage
- Network Throughput

## 📦 Kubernetes Metrics

- Pod Status (Running / Pending / Failed)
- Pod Restarts
- Container Health
- Node Availability

## 📈 Application-Level Indicators (Derived)

- Request patterns
- Error rates
- Traffic trends
- Latency indicators *(if instrumented)*

---

## 📈 Grafana Dashboard Architecture

Grafana dashboards are built using a standardized, reusable, and scalable model.

## 📌 Key Characteristics

- 📊 Per-application dashboards
- ♻️ Template-driven design
- 🔄 Dynamic variables support
- 🌍 Multi-environment compatibility

---

## 🔄 Dashboard Variables

| Variable      | Purpose                         |
|---------------|---------------------------------|
| `$cluster`    | Select Kubernetes cluster       |
| `$namespace`  | Filter application namespace    |
| `$datasource` | CloudWatch datasource           |
| `$application`| Application identifier          |

---

## 🧩 Grafana Library Panels (Reusable Components)

Amazon Managed Grafana uses **Library Panels** to enable reuse of common dashboard components across multiple dashboards.

---

## 📌 What are Library Panels?

Library Panels are shared, reusable visualization components that can be used across multiple dashboards.

---

## 📊 Examples from Compass Implementation

The platform includes reusable panels such as:

- Application logs for `${namespace}`
- CPU usage by Pod
- Memory usage by Pod
- Kubernetes Resource Count
- Pod crash indicators *(CrashLoopBackOff)*
- OOM Kill events
- Image Pull failures
- Liveness probe failures
- Event logs (`${namespace}`)
- WAF Logs / Requests
- User browser analytics
- X-Ray traces (`${xray_namespace}`)

---

## 🔄 How They Are Used

- Panels are created once in **Library Panels**
- Referenced across multiple dashboards
- Automatically updated wherever reused

---

## ⚠️ Important Behavior (Critical Insight)

> ⚠️ **Library Panels are globally shared**

Any update to a library panel:

- Reflects in all dashboards using it

This can cause:

- Data inconsistency across applications
- Unexpected dashboard changes

---

## 🚨 Known Challenge (Observed in Platform)

Changing variables like:

- `$namespace`
- `$cluster`

In shared panels can impact:

- Multiple applications simultaneously

---

## 🛠️ Recommended Approach (Adopted / Suggested)

To avoid cross-dashboard impact:

- ✅ Clone library panels per application
- ✅ Generate app-specific panel UIDs
- ✅ Avoid hardcoded shared configurations
- ✅ Use dynamic variables carefully

---

## 🧭 Dashboard Model

Compass CI uses **application-specific dashboards** in Amazon Managed Grafana.

Each application has its own dedicated dashboard, and all monitoring data is consolidated within it.

---

### 📊 Metrics & Observability Coverage

Each application dashboard provides a unified view of:

#### 🖥️ Infrastructure Metrics

- Cluster health
- Node utilization
- Resource consumption

---

#### 📦 Kubernetes & Application Metrics

- Pod health and lifecycle
- CPU & Memory usage
- Restart trends
- Namespace-level metrics

---

#### 📜 Logging & Events

- CloudWatch logs visualization
- Error tracking
- Namespace-based filtering

---

### 🎯 Key Characteristics

- 📌 One dashboard per application
- 🔄 Multi-environment support using variables (`$namespace`, `$cluster`)
- 📊 Combined view of metrics, logs, and events
- 🔍 Enables faster troubleshooting and RCA

---

## 🔍 Dashboard Usage Patterns

Common usage scenarios:

- Monitor application health
- Identify performance bottlenecks
- Detect abnormal spikes
- Correlate logs with metrics
- Validate deployments

---

## 🚨 Metrics-Based Alerting

### 📊 Common Alert Conditions

- High CPU utilization
- High memory usage
- Pod failures / crash loops
- Node unavailability

---

## 🔔 Alert Flow

```text
Metric Threshold Breach
        ↓
Grafana Alert Rule
        ↓
Notification Policy
        ↓
AWS SNS
        ↓
Email / Subscribers
```

---

## ⚙️ Dashboard Design Principles

- ✅ Consistent structure across applications
- ✅ Reusable and modular components
- ✅ Minimal manual configuration
- ✅ Clear visualization of critical metrics
- ✅ Logical grouping of panels

---

# ⚠️ Known Limitations / Considerations

## ❗ CloudWatch Latency

- Metrics may have ~1–2 minute delay

## ❗ Library Panel Coupling

- Shared panels affect multiple dashboards
- Requires careful governance

## ❗ Dashboard Automation Limitation

- Full automation of Grafana dashboards is constrained
- Due to shared library panels across dashboards
- Changes in one panel may impact multiple applications

## ❗ Limited Custom Metrics

- Requires application instrumentation

## ❗ Query Complexity

- CloudWatch queries can be complex

## ❗ Alert Noise

- Misconfigured thresholds may cause false alerts

---

# 🧭 Summary

The **Metrics & Dashboards framework in Compass CI** provides:

- 📊 Real-time visibility into infrastructure and applications
- 🚨 Proactive alerting using metrics
- 🔍 Deep observability via Grafana dashboards
- 📐 Standardized monitoring using reusable components

---

## 🚀 It enables teams to:

- Detect issues early
- Monitor system health effectively
- Optimize performance
- Maintain operational excellence

---

## 🔮 Future Enhancements

### 📊 Migration to Dynatrace

The platform is gradually evaluating **Dynatrace** as a future replacement for Amazon Managed Grafana.

### 🎯 Objectives

- Replicate existing Grafana dashboards and panels in Dynatrace
- Provide unified observability across infrastructure and applications
- Leverage advanced Dynatrace capabilities such as:
  - AI-powered monitoring
  - Automatic root cause analysis
  - End-to-end distributed tracing

---

### ⚙️ Current Approach

- Dynatrace is currently used for:
  - ✅ Infrastructure monitoring

- Application monitoring is:
  - ❗ Limited due to licensing constraints
  - 💰 Available via **chargeback model**

---

### 💼 Application Team Enablement

Application teams can opt-in for:

- Full application monitoring
- Distributed tracing
- Advanced analytics
- Additional Dynatrace features

👉 By subscribing through the **chargeback model**

---

### 🧭 Summary

Dynatrace adoption is planned to enhance observability capabilities beyond current Grafana-based monitoring, while balancing **cost, scalability, and team-specific needs**.
