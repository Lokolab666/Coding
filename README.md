Below is the adapted version of your document, rewritten to strictly follow the previously defined **Ticket Resolution Documentation – Authoring Guidelines**, with a formal tone, clear structure, and improved technical clarity.

---

## GitLab CI – Firewall TCP Access Request – FQDN-Based Rules

---

### 1. Customer Request Description

The customer requested firewall access to allow **GitLab CI Jobs** from the repository:

```
https://code.medtronic.com/{Repo_Name}
```

to establish outbound TCP connections over **port 443** to the following Fully Qualified Domain Names (FQDNs):

* `https://somewebsite.com` (TCP/443)
* `https://medtronic-anothersite.sandbox.my.salesforce.com` (TCP/443)
* `https://moresites.com` (TCP/443)

The request is aligned with previously approved firewall changes and references the following related incidents:

* **INC13081091**
* **INC12465068**

---

### 2. Resolution Details (Step-by-Step)

#### 2.1 Raise a Firewall Policy Change Request

A firewall change request must be created in **ServiceNow**, following the *Firewall Policy Change Request* process.

Select **Add Access Rules** as the request type.

---

#### 2.2 ServiceNow Form Configuration

Complete the ServiceNow request form using the values below.

| Field                                                                   | Value             |
| ----------------------------------------------------------------------- | ----------------- |
| Requested for                                                           | Your user name    |
| What do you want to request?                                            | Add Access Rules  |
| Application Name                                                        | GitLab            |
| Project Summary                                                         | GitLab CI Runners |
| Would you like to provide the access rule definitions in an attachment? | No                |
| Define Access Rules                                                     | Add Access Rules  |

---

#### 2.3 Access Rule Definition

Populate the **Define Access Rules** section using the information provided in the original ticket request.

| Attribute                 | Value                                                              |
| ------------------------- | ------------------------------------------------------------------ |
| Source IP or Network      | `c-Aus_it-iosharedservices-dev` and `AWS_it-iosharedservices-prod` |
| Source FQDN               | EKS Pod NAT Gateways                                               |
| Destination IP or Network | If explicitly provided by the requester                            |
| Destination FQDN          | As specified in the request                                        |
| Connection Purpose        | Customer applications / destinations                               |

---

#### 2.4 Submit the Firewall Request

Submit the ServiceNow request and update the **original ticket comments** with the newly created firewall request reference.

**Example comment:**

```
Firewall request created to open TCP port 443.
Reference: ITH5824733
```

---

#### 2.5 Repository Configuration Updates

After submitting the firewall request, update the corresponding GitLab repository configuration.

##### 2.5.1 Application Namespace Configuration

* Navigate to the `project-root-namespace`.
* Search for the application name.
* If not found, check the `shared` folder.
* Locate the YAML file associated with the project.
* Update the `torgolis` property by adding the required FQDNs.

**Example:**

```yaml
matchName: "somewebsite.com"
matchName: "medtronic-anothersite.sandbox.my.salesforce.com"
```

---

##### 2.5.2 Cilium Network Policy Test Configuration

* Navigate to:

```
gitlab-runner-ciliumnetworkpolicy/test
```

* Open the **HTTP/HTTPS internet resources** test file.
* Append the new destinations at the end of the file using the following structure.

**Example:**

```yaml
https://somewebsite.com:
  status: 200
  allow-insecure: false
  no-follow-redirects: false
  timeout: 5000
  body: []

https://medtronic-anothersite.sandbox.my.salesforce.com:
  status: 200
  allow-insecure: false
  no-follow-redirects: false
  timeout: 5000
  body: []
```

---

#### 2.6 Merge Request

Create a **Merge Request** containing the configuration changes and wait for the required approvals before merging.

---

### 3. Visual Evidence

Screenshots may be added to illustrate:

* ServiceNow firewall request submission
* Repository configuration changes
* Merge Request creation and approval

Ensure no sensitive information is exposed.

---

### 4. Additional Notes

Once the request is completed, update the tracking entry in the **Teams shared sheet** available at the corresponding internal URL.

---

### 5. Request Reference

**Request ID:** ITH5824733
