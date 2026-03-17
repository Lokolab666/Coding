Supported Authentication Providers
There are multiple ways to include authentication with your application.

All tomcat_httpd flavor applications that were built using the legacy Jenkins pipeline will have a default page provided when deployed located under /<ContextRoot>/snoop.jsp that will provide details of all the available headers and user information available to the application once a user is logged in.
Using the OIDConALB setting to allow the load balancer to handle authentication and require less coding and configuration within the application itself.
#OpenID Connect
The below table outlines the current set of supported OIDC providers for the pipeline. The Provider String is what is needed to define which provider you wish to use within the environmentInfo.json for the application.

Provider String the string that will be used as the value for the "provider" key in the environmentInfo.json file. This can be different for each defined environment block and provider block within that file.

Default Scope defines the scope that is included by default when using the provider. Depending on application needs, this may need to be overridden in the appInfo.json by defining the scope parameter for the provider.

Name	Provider String	Description	Client ID(s)	Default Scope
Medtronic Internal - Non-prod	login.microsoftonline.com%2F0a29d274-1367-4a8f-99c5-90c3dc7d4043%2Fv2.0	Non-prod internal provider (Entra ID / Azure AD) for Medtronic employee authentication. Provides mdtdirectorykeyi of the user as a header once user is logged in.	MDT_WEBAPP_BASIC/MDT_WEBAPP_DIRKEY: eff21357-eb1b-45ae-9d68-5a00c11c9086

MDT_WEBAPP_DIRKEYOUTLOOK: 0db2f0ec-8524-46ea-ae2b-6035f76e2072

MDT_WEBAPP_GROUP: 58991d60-f3b5-4312-b9bd-b7a832baa2e2	openid email profile phone
Medtronic Internal - Production	login.microsoftonline.com%2Fd73a39db-6eda-495d-8000-7579f56d68b7%2Fv2.0	Production internal provider (Entra ID / Azure AD) for Medtronic employee authentication. Provides mdtdirectorykeyi of the user as a header once user is logged in.	MDT_WEBAPP_BASIC/MDT_WEBAPP_DIRKEY: de1122aa-25bf-4644-8cf0-9250f07e01f8

MDT_WEBAPP_DIRKEYOUTLOOK: a8cf1742-8949-46ca-9ae7-92126ce46111

MDT_WEBAPP_GROUP: 861e5b53-393d-46bc-b804-d4a1a1e14185	openid email profile phone
CIAM/External - Non-prod (Dev)	dev.login.medtronic.com%2Foauth2%2Fausqxu4lutnO83mOb1d6	Non-prod (Dev) external provider (Okta) for Medtronic external user authentication. Provides legacy mdtdirectorykeyext and new mdtuuidext user identifiers as headers once user is logged in.	custom for each application	openid profile
CIAM/External - Non-prod (Test)	test.login.medtronic.com%2Foauth2%2Faus1n3gftlEI6n8B70x7	Non-prod (Test) external provider (Okta) for Medtronic external user authentication. Provides legacy mdtdirectorykeyext and new mdtuuidext user identifiers as headers once user is logged in.	custom for each application	openid profile
CIAM/External - Non-prod (Stage)	stage.login.medtronic.com%2Foauth2%2Faus12k2r9jvwY2zRl417	Non-prod (Stage) external provider (Okta) for Medtronic external user authentication. Provides legacy mdtdirectorykeyext and new mdtuuidext user identifiers as headers once user is logged in.	custom for each application	openid profile
CIAM/External - Production	login.medtronic.com%2Foauth2%2Faus16gc5wjVvJqXUg417	Production external provider (Okta) for Medtronic external user authentication. Provides legacy mdtdirectorykeyext and new mdtuuidext user identifiers as headers once user is logged in.	custom for each application	openid profile
#OpenID Endpoints by Provider
If using the OIDConALB setting, the below .well-known config URLs can be accessed to retrieve all of the endpoints needed for your environmentInfo.json file.

Name	Well Known Config URL
Medtronic Internal - Non-prod	https://login.microsoftonline.com/0a29d274-1367-4a8f-99c5-90c3dc7d4043/v2.0/.well-known/openid-configuration
Medtronic Internal - Production	https://login.microsoftonline.com/d73a39db-6eda-495d-8000-7579f56d68b7/v2.0/.well-known/openid-configuration
CIAM/External - Non-prod (Dev)	https://dev.login.medtronic.com/oauth2/ausqxu4lutnO83mOb1d6/.well-known/openid-configuration
CIAM/External - Non-prod (Test)	https://test.login.medtronic.com/oauth2/aus1n3gftlEI6n8B70x7/.well-known/openid-configuration
CIAM/External - Non-prod (Stage)	https://stage.login.medtronic.com/oauth2/aus12k2r9jvwY2zRl417/.well-known/openid-configuration
CIAM/External - Production	https://login.medtronic.com/oauth2/aus16gc5wjVvJqXUg417/.well-known/openid-configuration