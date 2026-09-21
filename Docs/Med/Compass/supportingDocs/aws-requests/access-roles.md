# AWS Account Access
This page explains how to assist when application teams request access to specific AWS resources (such as S3 buckets, RDS databases, etc.) within our AWS accounts. Use this guide when an application team needs direct access to cloud objects for development, testing, or troubleshooting in non-production environments. Follow the steps below to request a new IAM role and permissions through ServiceNow, ensuring you provide all required details for a smooth approval and setup process.

## Adding a new role
__Time to Complete__: ~ 1 week (due to required CHG requests and turnaround time from IAM / Cloud teams)

To add a new role to one or more of the AWS accounts:

1. Gather the following information:
    - Generate the required AWS IAM policy or gather all required permissions users in this role will need in the AWS account.
    - The list of users that need to be assigned to this role. Add yourself to the list so you have a way to confirm permissions once fulfilled for troubleshooting purposes.
    - A role name - descriptive enough to understand who should be assigned to this role, such as the specific application it applies to.
    - The AWS account(s) name / number where this role is needed.

2. Once you have gathered the above information, [submit a ServiceNow ticket](https://medtronicprod.service-now.com/now/nav/ui/classic/params/target/incident.do%3Fsys_id%3D-1%26sysparm_query%3Dactive%3Dtrue%26sysparm_stack%3Dincident_list.do%3Fsysparm_query%3Dactive%3Dtrue) assigned to `Cloud-Global` with the following details.

    <details>

    <summary><b>Example Request (with existing policies)</b></summary>
    In the below example, an existing IAM policy contained the desired S3 permissions, so it was provided for the Cloud team to replicate.

    ```text
    Assignment Group: Cloud-Global
    Configuration Item: AWS
    Category: Account / Security
    Subcategory: license_access

    Short description: Create new Role and AD Group for it-argo-dev-mdt AWS Account

    Description:
    Please setup a new SSO role and associated AD group for ACM Strat Alliance App Users in it-argo-dev-mdt (#389242548790) account and assign the below users to this role / AD group.

    New Role Name: AWSREF_ACM-Strat-Alliance-App-Role

    Attach the following existing policies in the account to the permission set (or create a new inline policy with the same permissions) for this new SSO role:
    acm-strat-alliance-dev-s3-policy
    ViewOnlyAccess

    Add the following users to the role:
    Kumar, Podili (kumarp133)
    Ranjan, Abhishek (ranjaa9)
    Gupta, Udayan (guptau7)
    ```
    </details>
    <br/>
    <details>

    <summary><b>Example Request (with inline policy provided)</b></summary>
    If a current policy does not exist, provide them the JSON for the policy needed.

    ```text
    Assignment Group: Cloud-Global
    Configuration Item: AWS
    Category: Account / Security
    Subcategory: license_access

    Short description: Create new Role and AD Group for it-argo-dev-mdt AWS Account

    Description:
    Please setup a new SSO role and associated AD group for ACM Strat Alliance App Users in it-argo-dev-mdt (#389242548790) account and assign the below users to this role / AD group.

    New Role Name: AWSREF_ACM-Strat-Alliance-App-Role

    Attach the following existing policies in the account to the permission set (or create a new inline policy with the same permissions) for this new SSO role:
    - ViewOnlyAccess
    - and the below policy:
    {
        "Version": "2012-10-17",
        "Statement": [
            {
                "Action": [
                    "s3:GetObject",
                    "s3:PutObject",
                    "s3:DeleteObject",
                    "s3:ListBucket"
                ],
                "Resource": [
                    "arn:aws:s3:::mdt-acm-strat-alliance-dev-389242548790-us-east-1",
                    "arn:aws:s3:::mdt-acm-strat-alliance-dev-389242548790-us-east-1/*"
                ],
                "Effect": "Allow",
                "Sid": "VisualEditor0"
            },
            {
                "Action": [
                    "kms:Decrypt",
                    "kms:ReEncrypt*",
                    "kms:GenerateDataKey",
                    "kms:Encrypt",
                    "kms:DescribeKey"
                ],
                "Resource": [
                    "arn:aws:kms:us-east-1:389242548790:key/a9d0c721-0a49-453f-8e41-cbae82b82778"
                ],
                "Effect": "Allow",
                "Sid": "KmsEncryptandDecrypt"
            }
        ]
    }

    Add the following users to the role:
    Montebello, Ash (montea4)
    Kumar, Podili (kumarp133)
    Ranjan, Abhishek (ranjaa9)
    Gupta, Udayan (guptau7)
    ```
    </details>

3. Request fulfillment involves Cloud and IAM teams performing the following actions. Typical fulfilment time is ~ 1 week as it requires scheduled CHG records.
    - New AD role creation (IAM team)
    - SSO role creation in AWS (Cloud-Global team)

4. Once the request has been fulfilled, using the [AWS start screen](https://medtronicsso.awsapps.com/start/#/?tab=accounts), confirm you see the new role in the list. If not, respond on the ticket to request Cloud-Global team to have them review the setup again.<br/>
![](../../docs/static/troubleshooting/aws-role-creation-start-screen-sample.png)

5. Log in to the account with those permissions. Confirm the permissions are working as expected. If possible, check permissions using AWS CLI (for example, if the role is for r/w to a given S3, using credentials from the start screen to upload and remove a file from that S3 bucket).

6. Send the application team users the following template message:
    ```
    Subject: AWS Access - [Application Name] Role Setup Complete

    Hello,

    Your AWS access for [Application Name] has been successfully set up. Please use the following information to access the AWS resources:

    1. AWS Console Access:
    - Navigate to: https://medtronicsso.awsapps.com/start/#/?tab=accounts
    - Select the AWS account "[AWS Account Name]"
    - Select the role "[Role Name]" (e.g., ACM-Strat-Alliance-AppRole)
    - Once inside the console, ensure you select "United States (N. Virginia)" (us-east-1) from the top-right dropdown for regions.
    - Your permissions give you:
            - View-only access to most account objects
            - [List additional permissions here]

    2. Programmatic (CLI/SDK) Access:
    - Navigate to: https://medtronicsso.awsapps.com/start/#/?tab=accounts
    - Select the AWS account "[AWS Account Name]"
    - Find your role and click the "Access keys" link next to it
    - Select your operating system (Windows, macOS, or Linux)
    - Copy the displayed commands and run them in your terminal/command prompt
    - Set an additional environment variable "AWS_REGION" = "us-east-1"
    - These credentials are temporary (typically valid for 4 hours) and will need to be refreshed periodically. Additional AWS CLI information can be found in AWS documentation: https://docs.aws.amazon.com/cli/v1/userguide/cli-chap-welcome.html

    If you encounter any issues or have questions, please contact dl.itargocoreteam@medtronic.com for assistance.

    ```

## Updating an existing role permissions
__Time to Complete__: ~ 2-3 days (due to required CHG requests and turnaround time from Cloud team)

Updates to roles can also only be done by the `Cloud-Global` team through a ServiceNow ticket. In some cases, as teams continue development of their applications, additional objects or permissions are needed that weren't originally requested.

1. To update a role, gather the following information:
    - Generate the required AWS IAM policy or gather all updated permissions users in this role will need.
    - The existing role name in the AWS account (e.g. AWSReservedSSO_ACM-Strat-Alliance-AppRole_77815100424ef188).
        - Use the AWS console to navigate to IAM > Roles and search for roles prefixed with `AWSReservedSSO_` to find all SSO related roles.
    - The AWS account(s) names / numbers where the permissions need to be updated (it-argo-dev-mdt, it-argo-prod-mdt)

2. Once you have gathered the above information, [submit a ServiceNow ticket](https://medtronicprod.service-now.com/now/nav/ui/classic/params/target/incident.do%3Fsys_id%3D-1%26sysparm_query%3Dactive%3Dtrue%26sysparm_stack%3Dincident_list.do%3Fsysparm_query%3Dactive%3Dtrue) assigned to `Cloud-Global` with the following details.

    <details>

    <summary><b>Example Request (with existing policies)</b></summary>
    In the below example, an existing IAM policy with all required permissions exists in the account, so it was provided for the Cloud team to replicate.

    ```text
    Assignment Group: Cloud-Global
    Configuration Item: AWS
    Category: Account / Security
    Subcategory: license_access

    Short description: Update AWSReservedSSO_ACM-Strat-Alliance-AppRole_77815100424ef188 Role for it-argo-dev-mdt AWS Account

    Description:
    Please update the permission set for the AWSReservedSSO_ACM-Strat-Alliance-AppRole_77815100424ef188 Role in it-argo-dev-mdt (#389242548790) account with the following.

    Attach the following existing policies in the account to the permission set (or update the existing inline policy with the same permissions):
    acm-strat-alliance-dev-s3-policy
    ViewOnlyAccess
    ```
    </details>
    <br/>
    <details>

    <summary><b>Example Request (with inline policy provided)</b></summary>
    In the below example, an inline policy with updated permissions to S3 and CloudWatch is given.

    ```text
    Assignment Group: Cloud-Global
    Configuration Item: AWS
    Category: Account / Security
    Subcategory: license_access

    Short description: Add Policy to Permission Set Role

    Description:
    Please update the permission set for the AWSReservedSSO_ACM-Strat-Alliance-AppRole_77815100424ef188 Role in it-argo-dev-mdt (#389242548790) account with the following.

    Update the permission set for this existing SSO role with the following permissions:
    - ViewOnlyAccess
    - and the below policy:
    {
        "Version": "2012-10-17",
        "Statement": [
            {
                "Action": [
                    "s3:GetObject",
                    "s3:PutObject",
                    "s3:DeleteObject",
                    "s3:ListBucket"
                ],
                "Resource": [
                    "arn:aws:s3:::mdt-acm-strat-alliance-dev-389242548790-us-east-1",
                    "arn:aws:s3:::mdt-acm-strat-alliance-dev-389242548790-us-east-1/*"
                ],
                "Effect": "Allow",
                "Sid": "VisualEditor0"
            },
            {
                "Action": [
                    "kms:Decrypt",
                    "kms:ReEncrypt*",
                    "kms:GenerateDataKey",
                    "kms:Encrypt",
                    "kms:DescribeKey"
                ],
                "Resource": [
                    "arn:aws:kms:us-east-1:389242548790:key/a9d0c721-0a49-453f-8e41-cbae82b82778"
                ],
                "Effect": "Allow",
                "Sid": "KmsEncryptandDecrypt"
            },
            {
                "Action": [
                    "logs:GetLogEvents",
                    "logs:FilterLogEvents",
                    "logs:StartQuery",
                    "logs:GetQueryResults",
                    "logs:DescribeLogGroups",
                    "logs:DescribeLogStreams"
                ],
                "Resource": [
                    "arn:aws:logs:us-east-1:389242548790:log-group:/aws/containerinsights/TF-argo-dev/application",
                    "arn:aws:logs:us-east-1:389242548790:log-group:/aws/containerinsights/TF-argo-dev/application:*"
                ],
                "Effect": "Allow",
                "Sid": "CloudWatchLogsAccess"
            }
        ]
    }
    ```
    </details>

3. Request fulfillment involves the Cloud performing the following actions. Typical fulfilment time is ~ 2-3 days as it requires scheduled CHG records.
    - SSO role policy update in AWS (Cloud-Global team)

4. Once the request has been fulfilled, using the [AWS start screen](https://medtronicsso.awsapps.com/start/#/?tab=accounts) log in to the account using the given role. Confirm the permissions are working as expected. If possible, check permissions using AWS CLI (for example, if the update was to add permissions for r/w to a given S3, using credentials from the start screen to upload and remove a file from that S3 bucket).

6. Send the application team users the following template message (update all content in brackets before sending):
    ```
    Subject: AWS Access - [Application Name] Permission Updates Complete

    Hello,

    Your AWS permissions have been successfully updated. Please use the following information to access the AWS resources:

    1. AWS Console Access:
    - Navigate to: https://medtronicsso.awsapps.com/start/#/?tab=accounts
    - Select the AWS account "[AWS Account Name]"
    - Select the role "[Role Name]" (e.g., ACM-Strat-Alliance-AppRole)
    - Once inside the console, ensure you select "United States (N. Virginia)" (us-east-1) from the top-right dropdown for regions.
    - Your permissions give you:
            - View-only access to most account objects
            - [List additional added permissions here]

    2. Programmatic (CLI/SDK) Access:
    - Navigate to: https://medtronicsso.awsapps.com/start/#/?tab=accounts
    - Select the AWS account "[AWS Account Name]"
    - Find your role and click the "Access keys" link next to it
    - Select your operating system (Windows, macOS, or Linux)
    - Copy the displayed commands and run them in your terminal/command prompt
    - Set an additional environment variable "AWS_REGION" = "us-east-1"
    - These credentials are temporary (typically valid for 4 hours) and will need to be refreshed periodically. Additional AWS CLI information can be found in AWS documentation: https://docs.aws.amazon.com/cli/v1/userguide/cli-chap-welcome.html

    If you encounter any issues or have questions, please contact dl.itargocoreteam@medtronic.com for assistance.

    ```

## Add users to an existing role
__Time to Complete__: ~ 1-2 days (due to turnaround time from IAM team)

User management is all done through Active Directory (AD) roles. Application teams can submit their own requests to add additional users to an existing role by submitting a [ServiceNow ticket](https://medtronicprod.service-now.com/it?id=mdtit_begin_a_help_ticket&sys_id=814afddedb1fd700fd835f30cf961997) with the following details:

```text
Assignment Group: Identity and Access Management-Global
Configuration Item: Azure AD - PROD
Category: Application
Subcategory: License/Access

Short description: Add user(s) to AWSREF_ACM-Strat-Alliance-App-Role

Description:
Please add the below users to the AWSREF_ACM-Strat-Alliance-App-Role AD role:

Montebello, Ash (montea4)
Kumar, Podili (kumarp133)
```

## Current Roles
The below list outlines the current set of application-specific roles that have been configured.

|AD Role Name| AWS Role Name | AWS Account(s) | Description |Main Contact(s)|
|-----|--------|--------|--------|--------|
|AWSREF_ACM-Strat-Alliance-App-Role| [ACM-Strat-Alliance-AppRole](https://389242548790-64fexlsm.us-east-1.console.aws.amazon.com/iam/home?region=us-east-1#/roles/details/AWSReservedSSO_ACM-Strat-Alliance-AppRole_77815100424ef188?section=permissions) | it-argo-dev-mdt | ACM IT - Strategic Alliance Web Portal Application<br/>- R/W + encrypt/decrypt on [dev S3 bucket](https://389242548790-64fexlsm.us-east-1.console.aws.amazon.com/s3/buckets/mdt-acm-strat-alliance-dev-389242548790-us-east-1?region=us-east-1&tab=objects&bucketType=general) | David Kanevsky<br/>Gedalia Kliger |
