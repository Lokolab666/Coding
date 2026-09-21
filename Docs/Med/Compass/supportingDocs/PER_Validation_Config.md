# Policy Exception Request (PER)

### Quarantine cluster requirements ###
!!!danger
Applications must make every effort to remediate __all__ vulnerabilities and move to the Production cluster. The Quarantine cluster is meant for legacy applications that __CANNOT__ remediate all vulnerabilities without an application re-write.
!!!
   * Valid date range for the PER (Policy Exception Request) is required for Quarantine cluster deployment.
   * You must submit a PER form and contact Web Application Support - CTS with updated PER extension dates once received.
   * PER form can be found on the following link: https://medtronicprod.service-now.com/it/?id=mdtit_sc_cat_item&sys_id=84c6c452e7600300dd926217c2f6a980")

### PER configuration - Web App Support ###
Upon receiving approved PER number and date from an application team:
1. Search for the PER in ServiceNow (https://medtronicprod.service-now.com/now/nav/ui/classic/params/target/sn_compliance_policy_exception_list.do).
1. Confirm the "Valid to" date matches what the application team provided.
   ![](../docs/static/security/sn-per-list.PNG)
1. Confirm "Status(state)" shows "Approved". If any other status, let application team know their PER has not yet been approved and they must work with their assigned approver (shown in ServiceNow).
   ![](../docs/static/security/sn-per-approver.PNG)
1. If PER shows approved, update the application's configuration in the JenkinsConfigs/Prometheus/projects.json file.
   * Add details to the application's config in the following format:
        ```json
            "PER": {
                "number": "<PER number>",
                "validTo": "<yyy-MM-dd HH:mm:ss>"
            }
        ```

### Application's projects.json config example
```
{
    "name": "registration",
    "groupId": "com.medtronic.web",
    ...
    ...
    ...
    "PER": {
      "number": "PER0002015",
      "validTo": "2023-10-31 23:59:59"
    }
  }
  ```
