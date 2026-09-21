# Base Image Cleanup Automation

## 1. Overview

The **Base Image Cleanup Automation** is designed to identify and safely
clean up unused and outdated Docker base images stored in JFrog
Artifactory.

Over the past 3+ years, multiple base images have been generated
monthly, leading to a large accumulation of legacy images. This
automation helps control storage growth, improve security posture, and
prevent usage of outdated base images.

The solution runs in **safe dry-run mode by default**, ensuring no
accidental deletions occur without explicit approval.

------------------------------------------------------------------------

## 2. Business Objective

1.  Reduce JFrog storage consumption by removing unused legacy base
    images
2.  Prevent usage of outdated and vulnerable base images
3.  Enforce image retention policies consistently
4.  Automate cleanup instead of manual intervention
5.  Provide visibility and auditability before deletion

------------------------------------------------------------------------

## 3. Repository Structure

``` text
├── cleanup.py
├── requirements.txt
├── .gitlab-ci.yml
├── README.md
```

------------------------------------------------------------------------

## 4. Problem Statement

-   Base images are created at a rate of ~6 images per month
-   Images have been accumulating for **3+ years**
-   Many older images are no longer referenced
-   Older images carry higher vulnerability risk

**Estimated impact:** ~220 legacy images

------------------------------------------------------------------------

## 5. Cleanup Scope

### 5.1 Artifacts Covered

-   Docker base images in JFrog
-   Associated legacy artifacts (e.g., old Tomcat versions)

### 5.2 Cleanup Rules

Images are eligible if:

1. Older than retention period  
2. Not the latest version  
3. Not the only remaining version


------------------------------------------------------------------------

## 6. Retention Policy

| Variable        | Description                     |
|-----------------|---------------------------------|
| RETENTION_DAYS  | Minimum age before cleanup      |
| KEEP_LAST       | Latest versions to retain       |


## 7. Execution Modes

### 7.1 Dry Run Mode (Default)

-   No deletions performed
-   Logs eligible images only

### 7.2 Deletion Mode

-   Enabled after approval
-   Performs actual deletion

------------------------------------------------------------------------

## 8. CI/CD Integration

-   Pipeline does **not** run on push
-   Triggered manually or via schedule

------------------------------------------------------------------------

## 9. Required CI Variables

| Variable             | Description          |
|----------------------|----------------------|
| ARTIFACTORY_USERNAME | JFrog username       |
| ARTIFACTORY_API_KEY  | JFrog API key        |
| RETENTION_DAYS       | Retention threshold  |
| KEEP_LAST            | Versions to retain   |
| DRY_RUN              | Safety flag          |


## 10. Security Considerations

1.  No hardcoded credentials
2.  Secrets stored in CI variables
3.  Dry run enforced initially

------------------------------------------------------------------------

## 11. Expected Impact

-   Removal of ~220 legacy images
-   Reduced JFrog storage usage
-   Improved security posture

------------------------------------------------------------------------

## 12. Point of Contact

-   Ash Montebello
-   Abhishek Ranjan
