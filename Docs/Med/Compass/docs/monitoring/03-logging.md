---
label: Logging
icon: log
order: 3
---

# 📜 Logging Framework – Compass CI Platform

## 🌐 Overview

The Compass CI Platform provides a **centralized, scalable, and standardized logging framework** for all applications running on Amazon EKS.

All application logs are automatically collected, enriched, and stored in AWS CloudWatch, enabling seamless access through Grafana dashboards and CloudWatch Logs Insights.

---

## 🎯 Objectives

The logging framework is designed to:

- 📦 Centralize all application logs
- 🔍 Enable efficient log search and filtering
- ⚡ Support rapid debugging and RCA
- 🧭 Provide consistent logging standards across applications
- 🛡️ Ensure audit and compliance readiness


---

## 🏗️ Logging Architecture

### 🖥️ Log Generation (Application Layer)

Applications running in Kubernetes:

- Write logs to:
  - `stdout`
  - `stderr`

- Kubernetes automatically enriches logs with metadata:
  - Namespace
  - Pod Name
  - Container Name
  - Timestamp

---

### 📥 Log Collection

Logs are collected using:

- **Fluent Bit (DaemonSet)**
- **AWS Container Insights**

These agents:

- Capture logs from all running containers
- Attach Kubernetes metadata
- Forward logs to CloudWatch

---

### ☁️ Log Storage (AWS CloudWatch)

Logs are stored in **AWS CloudWatch Log Groups**

#### 📁 Standard Log Group Pattern

```text
/aws/containerinsights/<cluster-name>/application
```

---

## 🌍 Environment Segregation

Logs are logically segregated by environment:

- `dev`
- `staging`
- `production`

This ensures:

- 🔐 **Isolation**
- 📊 **Clean observability per environment**
- 🚫 **No cross-environment noise**

---

## 🔍 Accessing Logs
Logs can be accessed using the following methods:

### 📊 Grafana (Recommended)

Logs can be accessed through **Amazon Managed Grafana**

**Steps:**
1. Open [Grafana](https://g-05c60f9e9b.grafana-workspace.us-east-1.amazonaws.com/)
2. Navigate to **Explore**
3. Select **CloudWatch Logs** datasource
4. Choose log group
5. Use queries to filter logs

---

### 🔎 CloudWatch Logs Insights

Direct access via AWS Console:

**Steps:**
1. Navigate to **CloudWatch → Logs Insights**
2. Select log group
3. Run queries for analysis

---

## 🧪 Sample Log Queries

### 📌 Fetch Recent Logs
```sql
fields @timestamp, @message
| sort @timestamp desc
| limit 20
```
---

### 📌 Filter by Namespace

```sql
fields @timestamp, @message
| filter kubernetes.namespace_name = "your-namespace"
| sort @timestamp desc
```
---

### 📌 Search for Errors

```sql
fields @timestamp, @message
| filter @message like /ERROR/
| sort @timestamp desc
```
---

## 🔎 Find Your Pod Name

View pod names in Grafana:

**Steps:**
1. Open **Kubernetes / Compute Resources / Namespace (Pods)** dashboard
2. Filter by your namespace
3. Note the pod name from the dashboard

**Alternative:**
- Contact **Infra-Argo-Global** via ServiceNow for assistance

---

## 🔍 CloudWatch Logs (Advanced Queries)

For centralized log search and analysis:

### ☁️ Access CloudWatch

**Steps:**
1. Open **AWS Console**
2. Navigate to **CloudWatch → Logs → Log Groups**
3. Select log group for your environment:

```text
/aws/containerinsights/compass-dev/application
/aws/containerinsights/compass-staging/application
/aws/containerinsights/compass-prod/application
```

---

## 🧪 Search Logs

### 📌 Example 1: Find all errors

```sql
fields @timestamp, @message, kubernetes.pod_name
| filter @message like /ERROR|WARN|Exception/
| sort @timestamp desc
| limit 100
```

---

### 📌 Example 2: Search by Namespace

```sql
fields @timestamp, @message, kubernetes.namespace_name
| filter kubernetes.namespace_name = "my-app-prod"
| stats count() as error_count by kubernetes.pod_name
| sort error_count desc
```

---

### 📌 Example 3: Find Specific Error Message

```sql
fields @timestamp, @message, kubernetes.pod_name
| filter @message like /NullPointerException/
| sort @timestamp desc
```

---

### 📌 Example 4: Pod Performance Analysis

```sql
fields @timestamp, @message
| filter kubernetes.pod_name = "my-app-deployment-5d4f7c6b8f-abc12"
| stats count() as total_logs
| stats pct(@timestamp, 50) as p50, pct(@timestamp, 95) as p95
```

---

## 🔐 Access & Security

Logging access is controlled via:

- 👥 Grafana Teams
- 📁 Folder-level permissions
- 🔐 AWS IAM roles (CloudWatch access)

**Data Access Scope:**
- Namespace-level
- Application-level

---

## ⚙️ Key Characteristics

- ✅ Centralized logging (CloudWatch)
- ✅ Fully managed and scalable
- ✅ Kubernetes-native integration
- ✅ Seamless Grafana integration
- ✅ Environment isolation

---

## ⚠️ Known Considerations

### ❗ Log Retention
- Retention is managed in CloudWatch
- Must be configured per compliance requirements

---

### ❗ Query Performance
- Large log volumes may impact query time
- Use filters to optimize queries

---

### ❗ Missing Logs

**Possible causes:**
- Pod not running
- Fluent Bit issues
- Incorrect namespace filtering

---

## 📝 Best Practices

### Log Levels
Use appropriate log levels:
- **DEBUG:** Detailed diagnostic information (dev only)
- **INFO:** General informational messages
- **WARN:** Warning messages, recoverable issues
- **ERROR:** Error messages, problems requiring attention
- **FATAL:** Fatal errors causing application failure

### Sensitive Data
❌ Never log:
- Passwords or API keys
- Customer personal information
- Payment card data
- Authentication tokens

✅ Instead:
- Log that operation succeeded/failed
- Log non-sensitive context
- Use placeholders for sensitive values
- Redact sensitive data before logging

### Structured Logging
Consider structured logging for easier parsing:

```json
{
  "timestamp": "2024-01-15T10:30:45Z",
  "level": "ERROR",
  "service": "my-app",
  "request_id": "abc123",
  "message": "Database connection failed",
  "error": "timeout after 5s",
  "duration_ms": 5000
}
```

---

## 🧭 Summary

The **Compass Logging Framework** provides a robust, centralized, and scalable logging solution that enables:

- 🔍 Efficient log analysis
- 🚨 Faster issue detection
- 🛠️ Effective troubleshooting
- 📐 Standardized logging practices
- 🛡️ Strong audit and compliance support

---

## 🔗 Related Documentation

- [Monitoring & Observability](index.md)
- [Troubleshooting Guide](../troubleshooting-and-support/troubleshooting-guide.md)
- [Security Monitoring](../security/index.md)
