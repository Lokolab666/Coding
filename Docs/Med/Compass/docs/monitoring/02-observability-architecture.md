---
label: Architecture
icon: project
order: 4
---

# 🏗️ Observability Architecture – Compass CI Platform

---

## 🌐 Overview

The Compass Observability Architecture provides a **centralized, scalable, and integrated monitoring framework** across all Kubernetes-based applications running on Amazon EKS.

It ensures complete visibility across:

- 📜 Logs
- 📊 Metrics
- 🚨 Alerts
- 🔄 Deployments
- 🛡️ Security events

---

## 🧱 Architecture Components

The observability stack consists of the following core components:

| Component | Service | Purpose |
|----------|--------|--------|
| 🖥️ Compute Layer | Amazon EKS | Hosts application workloads |
| 📦 Container Runtime | Kubernetes Pods | Emits application logs |
| 📜 Logging Layer | AWS CloudWatch Logs | Central log aggregation |
| 📊 Metrics Layer | CloudWatch Container Insights | Infrastructure & Kubernetes metrics |
| 📈 Visualization Layer | Amazon Managed Grafana | Dashboards & observability UI |
| 🚨 Alerting Layer | Grafana Alerts + SNS | Alert generation & notification |
| 🔄 Deployment Monitoring | Flux CD | GitOps-based deployment tracking |

---

## 🔁 End-to-End Observability Flow

```text
Application Pods (EKS)
        ↓
Container Logs (stdout / stderr)
        ↓
Kubernetes (Fluent Bit / Container Insights)
        ↓
AWS CloudWatch Logs (Central Storage)
        ↓
Amazon Managed Grafana (Datasource)
        ↓
Dashboards / Alerts / Troubleshooting
```

---

## 🔍 Detailed Component Interaction

### 🖥️ 1. Application Layer (EKS)

Applications run as **containers inside Kubernetes Pods**.

**Log Generation:**
- Logs are written to:
  - `stdout`
  - `stderr`

**Kubernetes Metadata Enrichment:**
- Namespace
- Pod Name
- Container Name

---

### 📜 2. Logging Layer (CloudWatch)

Logs are collected using:

- Fluent Bit / Container Insights

Logs are pushed to:

- AWS CloudWatch Log Groups

#### 📁 Standard Log Group Pattern

```text
/aws/containerinsights/<cluster-name>/application
```

---

---

#### 🌍 Environment Segregation

The observability platform supports multiple environments:

- dev
- staging
- production

---

### 📊 3. Metrics Layer (Container Insights)

#### Metrics Collected:

- CPU usage
- Memory usage
- Pod metrics
- Node health

#### Metrics Availability:

- AWS CloudWatch
- Grafana dashboards

---

### 📈 4. Visualization Layer (Amazon Managed Grafana)

Grafana acts as the **central observability interface**.

#### Key Capabilities:

- 📊 Metrics visualization
- 📜 Log exploration
- 🔍 Namespace-based filtering
- 📈 Dashboard-based monitoring
- 🚨 Alert configuration

#### Datasource:

- AWS CloudWatch (Logs + Metrics)

---

### 🔄 5. Deployment Monitoring (Flux CD)

Flux CD enables **GitOps-based deployment monitoring**.

#### Capabilities:

- 🔁 Continuous reconciliation
- ⚠️ Drift detection
- ❌ Failure detection
- 📜 Deployment status visibility

---

### 🚨 6. Alerting Layer

Alerts are configured in **Grafana**.

#### Trigger Conditions:

- Metrics thresholds
- Pod failures
- Resource usage

#### Notification Flow:

```text
Grafana Alert Rule
        ↓
Notification Policy
        ↓
AWS SNS
        ↓
Email / Subscribers
```

---

## 🔗 Data Flow Breakdown

### 📜 Log Flow

```text
Pod → stdout/stderr → Fluent Bit → CloudWatch → Grafana
```

---

### 📊 Metrics Flow

```text
Kubernetes → Container Insights → CloudWatch → Grafana
```

---

### 🚨 Alert Flow

```text
Metrics / Logs → Grafana Alerts → SNS → Notification
```

---

---

## 🧭 Dashboard Interaction Model

- Dashboards are:
  - 📌 Per-application
  - ♻️ Built using shared templates

- Use dynamic variables:
  - `$namespace`
  - `$cluster`

---

## 🔐 Access & Security Model

Grafana access is controlled via:

- 👥 Teams
- 📁 Folder-level permissions

#### Data Access Scope:

- Namespace
- Application

---

## ⚙️ Key Architectural Characteristics

### ✅ Centralized Observability
- All logs and metrics are stored in **CloudWatch**

---

### ✅ Scalable Design
- Supports multiple applications and environments

---

### ✅ Namespace Isolation
- Each application is logically isolated

---

### ✅ Reusable Dashboard Templates
- Standardized observability across applications

---

### ✅ Integrated Monitoring
- Logs + Metrics + Events + Security in a unified platform

---

## ⚠️ Known Limitations / Considerations

### ❗ 1. No ServiceNow Integration (Current State)
- Incident automation is not yet implemented

---

### ❗ 2. Library Panel Dependency
- Shared panels impact multiple dashboards

---

### ❗ 3. Datasource Complexity
- Combination of:
  - CloudWatch
  - Prometheus-style queries

- Requires clear documentation and governance

---

### ❗ 4. Alert Noise / Misconfiguration
- Some alerts may show:
  - No data
  - Incorrect firing

---

## 🧭 Summary

The Compass Observability Architecture provides a **robust, scalable, and centralized monitoring framework** that enables:

- 👀 End-to-end visibility across applications
- 🚨 Real-time monitoring and alerting
- 🛠️ Efficient troubleshooting workflows
- 📐 Standardized observability practices

---
