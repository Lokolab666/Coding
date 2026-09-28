 curl -sS \
>   -o /dev/null \
>   -D - \
>   --connect-timeout 10 \
>   --max-time 30 \
>   "https://edcmdt-test.medtronic.com/oracleclinicalrdcsso/"
HTTP/2 302 
date: Mon, 28 Sep 2026 16:49:48 GMT
content-type: text/html; charset=iso-8859-1
content-length: 584
location: https://edcmdt-test.medtronic.com/oracleclinicalrdcsso/login.html?target_link_uri=https%3A%2F%2Fedcmdt-test.medtronic.com%3A443%2Foracleclinicalrdcsso%2F&method=get&oidc_callback=https%3A%2F%2Fedcmdt-test.medtronic.com%2Foracleclinicalrdcsso%2Fredirect&x_csrf=V2YNkAmaFI0
server: Apache
set-cookie: x_csrf=V2YNkAmaFI0; Path=/; Secure; HttpOnly; SameSite=Strict

AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ curl -sS \
>   -D - \
>   --connect-timeout 10 \
>   --max-time 30 \
>   "https://edcmdt-test.medtronic.com/oracleclinicalrdcsso/build.json?ALB"
HTTP/2 200 
date: Mon, 28 Sep 2026 16:49:59 GMT
content-type: application/json;charset=utf-8
server: Apache
vary: Origin,Access-Control-Request-Method,Access-Control-Request-Headers

{
    "name": "OracleClinicalRDCSSO",
    "version": "3.4.298t1",
    "snapshot": "SNAPSHOT",
    "buildNumber": "3871746",
    "environment": "DEV"

     for pod in $($K -n "$NS" get pods -o name); do
>   echo "===== $pod ====="
> 
>   $K -n "$NS" logs "$pod" \
>     --all-containers \
>     --since=24h \
>     --tail=3000 2>&1 |
>   grep -Ei 'oauth|oidc|auth0|provider|signin|login|authentication|ORA-|JDBC|SQL' |
>   grep -Ev 'obs-otel-agent-collector|OkHttpGrpcExporter' |
>   tail -n 150
> done
===== pod/oracleclinicalrdcsso-dev-57f47458d9-cqlh9 =====
===== pod/oracleclinicalrdcsso-dev-57f47458d9-kz5nz =====
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ for secret in secrets secrets-files secrets-providers; do
>   echo "===== $secret ====="
> 
>   $K -n "$NS" get secret "$secret" -o json |
>   jq -r '.data | keys[]'
> done
===== secrets =====
CONTRAST__API__API_KEY
RDCSSO_PW
external_PW
internal_PW
===== secrets-files =====
build.json
cacerts
contrast_security.yml
log4j-init-file.xml
login.html
===== secrets-providers =====
login.microsoftonline.com-2F0a29d274-1367-4a8f-99c5-90c3dc7d4043-2Fv2.0.client
login.microsoftonline.com-2F0a29d274-1367-4a8f-99c5-90c3dc7d4043-2Fv2.0.conf
login.microsoftonline.com-2F0a29d274-1367-4a8f-99c5-90c3dc7d4043-2Fv2.0.provider
signin-stage.medtronic.com.client
signin-stage.medtronic.com.conf
signin-stage.medtronic.com.provider



 $K -n "$NS" get deployment oracleclinicalrdcsso-dev -o json |
> jq '{
>   volumes: [.spec.template.spec.volumes[] | select(.secret)],
>   mounts: [.spec.template.spec.containers[].volumeMounts[]]
> }'
{
  "volumes": [
    {
      "name": "secretsvol",
      "secret": {
        "defaultMode": 292,
        "items": [
          {
            "key": "login.html",
            "path": "login.html"
          },
          {
            "key": "cacerts",
            "path": "cacerts"
          },
          {
            "key": "log4j-init-file.xml",
            "path": "log4j-init-file.xml"
          },
          {
            "key": "contrast_security.yml",
            "path": "contrast_security.yml"
          },
          {
            "key": "build.json",
            "path": "build.json"
          }
        ],
        "secretName": "secrets-files"
      }
    },
    {
      "name": "providers-vol",
      "secret": {
        "defaultMode": 292,
        "items": [
          {
            "key": "dev.login.medtronic.com-2Foauth2-2Fausqxu4lutnO83mOb1d6.client",
            "path": "dev.login.medtronic.com%2Foauth2%2Fausqxu4lutnO83mOb1d6.client"
          },
          {
            "key": "dev.login.medtronic.com-2Foauth2-2Fausqxu4lutnO83mOb1d6.conf",
            "path": "dev.login.medtronic.com%2Foauth2%2Fausqxu4lutnO83mOb1d6.conf"
          },
          {
            "key": "dev.login.medtronic.com-2Foauth2-2Fausqxu4lutnO83mOb1d6.provider",
            "path": "dev.login.medtronic.com%2Foauth2%2Fausqxu4lutnO83mOb1d6.provider"
          },
          {
            "key": "dev.login.medtronic.com.client",
            "path": "dev.login.medtronic.com.client"
          },
          {
            "key": "dev.login.medtronic.com.conf",
            "path": "dev.login.medtronic.com.conf"
          },
          {
            "key": "dev.login.medtronic.com.provider",
            "path": "dev.login.medtronic.com.provider"
          },
          {
            "key": "login.medtronic.com-2Foauth2-2Faus16gc5wjVvJqXUg417.client",
            "path": "login.medtronic.com%2Foauth2%2Faus16gc5wjVvJqXUg417.client"
          },
          {
            "key": "login.medtronic.com-2Foauth2-2Faus16gc5wjVvJqXUg417.conf",
            "path": "login.medtronic.com%2Foauth2%2Faus16gc5wjVvJqXUg417.conf"
          },
          {
            "key": "login.medtronic.com-2Foauth2-2Faus16gc5wjVvJqXUg417.provider",
            "path": "login.medtronic.com%2Foauth2%2Faus16gc5wjVvJqXUg417.provider"
          },
          {
            "key": "login.medtronic.com.client",
            "path": "login.medtronic.com.client"
          },
          {
            "key": "login.medtronic.com.conf",
            "path": "login.medtronic.com.conf"
          },
          {
            "key": "login.medtronic.com.provider",
            "path": "login.medtronic.com.provider"
          },
          {
            "key": "login.microsoftonline.com-2F0a29d274-1367-4a8f-99c5-90c3dc7d4043-2Fv2.0.client",
            "path": "login.microsoftonline.com%2F0a29d274-1367-4a8f-99c5-90c3dc7d4043%2Fv2.0.client"
          },
          {
            "key": "login.microsoftonline.com-2F0a29d274-1367-4a8f-99c5-90c3dc7d4043-2Fv2.0.conf",
            "path": "login.microsoftonline.com%2F0a29d274-1367-4a8f-99c5-90c3dc7d4043%2Fv2.0.conf"
          },
          {
            "key": "login.microsoftonline.com-2F0a29d274-1367-4a8f-99c5-90c3dc7d4043-2Fv2.0.provider",
            "path": "login.microsoftonline.com%2F0a29d274-1367-4a8f-99c5-90c3dc7d4043%2Fv2.0.provider"
          },
          {
            "key": "login.microsoftonline.com-2Fd73a39db-6eda-495d-8000-7579f56d68b7-2Fv2.0.client",
            "path": "login.microsoftonline.com%2Fd73a39db-6eda-495d-8000-7579f56d68b7%2Fv2.0.client"
          },
          {
            "key": "login.microsoftonline.com-2Fd73a39db-6eda-495d-8000-7579f56d68b7-2Fv2.0.conf",
            "path": "login.microsoftonline.com%2Fd73a39db-6eda-495d-8000-7579f56d68b7%2Fv2.0.conf"
          },
          {
            "key": "login.microsoftonline.com-2Fd73a39db-6eda-495d-8000-7579f56d68b7-2Fv2.0.provider",
            "path": "login.microsoftonline.com%2Fd73a39db-6eda-495d-8000-7579f56d68b7%2Fv2.0.provider"
          },
          {
            "key": "stage.login.medtronic.com-2Foauth2-2Faus12k2r9jvwY2zRl417.client",
            "path": "stage.login.medtronic.com%2Foauth2%2Faus12k2r9jvwY2zRl417.client"
          },
          {
            "key": "stage.login.medtronic.com-2Foauth2-2Faus12k2r9jvwY2zRl417.conf",
            "path": "stage.login.medtronic.com%2Foauth2%2Faus12k2r9jvwY2zRl417.conf"
          },
          {
            "key": "stage.login.medtronic.com-2Foauth2-2Faus12k2r9jvwY2zRl417.provider",
            "path": "stage.login.medtronic.com%2Foauth2%2Faus12k2r9jvwY2zRl417.provider"
          },
          {
            "key": "stage.login.medtronic.com.client",
            "path": "stage.login.medtronic.com.client"
          },
          {
            "key": "stage.login.medtronic.com.conf",
            "path": "stage.login.medtronic.com.conf"
          },
          {
            "key": "stage.login.medtronic.com.provider",
            "path": "stage.login.medtronic.com.provider"
          },
          {
            "key": "test.login.medtronic.com-2Foauth2-2Faus1n3gftlEI6n8B70x7.client",
            "path": "test.login.medtronic.com%2Foauth2%2Faus1n3gftlEI6n8B70x7.client"
          },
          {
            "key": "test.login.medtronic.com-2Foauth2-2Faus1n3gftlEI6n8B70x7.conf",
            "path": "test.login.medtronic.com%2Foauth2%2Faus1n3gftlEI6n8B70x7.conf"
          },
          {
            "key": "test.login.medtronic.com-2Foauth2-2Faus1n3gftlEI6n8B70x7.provider",
            "path": "test.login.medtronic.com%2Foauth2%2Faus1n3gftlEI6n8B70x7.provider"
          },
          {
            "key": "test.login.medtronic.com.client",
            "path": "test.login.medtronic.com.client"
          },
          {
            "key": "test.login.medtronic.com.conf",
            "path": "test.login.medtronic.com.conf"
          },
          {
            "key": "test.login.medtronic.com.provider",
            "path": "test.login.medtronic.com.provider"
          },
          {
            "key": "signin-dev.medtronic.com.client",
            "path": "signin-dev.medtronic.com.client"
          },
          {
            "key": "signin-dev.medtronic.com.conf",
            "path": "signin-dev.medtronic.com.conf"
          },
          {
            "key": "signin-dev.medtronic.com.provider",
            "path": "signin-dev.medtronic.com.provider"
          },
          {
            "key": "signin-test.medtronic.com.client",
            "path": "signin-test.medtronic.com.client"
          },
          {
            "key": "signin-test.medtronic.com.conf",
            "path": "signin-test.medtronic.com.conf"
          },
          {
            "key": "signin-test.medtronic.com.provider",
            "path": "signin-test.medtronic.com.provider"
          },
          {
            "key": "signin-stage.medtronic.com.client",
            "path": "signin-stage.medtronic.com.client"
          },
          {
            "key": "signin-stage.medtronic.com.conf",
            "path": "signin-stage.medtronic.com.conf"
          },
          {
            "key": "signin-stage.medtronic.com.provider",
            "path": "signin-stage.medtronic.com.provider"
          },
          {
            "key": "signin.medtronic.com.client",
            "path": "signin.medtronic.com.client"
          },
          {
            "key": "signin.medtronic.com.conf",
            "path": "signin.medtronic.com.conf"
          },
          {
            "key": "signin.medtronic.com.provider",
            "path": "signin.medtronic.com.provider"
          }
        ],
        "optional": true,
        "secretName": "secrets-providers"
      }
    }
  ],
  "mounts": [
    {
      "mountPath": "/tmp",
      "name": "tmp"
    },
    {
      "mountPath": "/otel",
      "name": "otel-volume"
    },
    {
      "mountPath": "/var/log",
      "name": "var-log"
    },
    {
      "mountPath": "/var/log/apache2",
      "name": "var-log-apache2"
    },
    {
      "mountPath": "/run/apache2",
      "name": "run-apache2"
    },
    {
      "mountPath": "/var/www/localhost/htdocs/login.html",
      "name": "secretsvol",
      "readOnly": true,
      "subPath": "login.html"
    },
    {
      "mountPath": "/opt/java/openjdk/lib/security/cacerts",
      "name": "secretsvol",
      "readOnly": true,
      "subPath": "cacerts"
    },
    {
      "mountPath": "/appDataDir/log4j-init-file.xml",
      "name": "secretsvol",
      "readOnly": true,
      "subPath": "log4j-init-file.xml"
    },
    {
      "mountPath": "/var/cache/mod_auth_openidc/metadata",
      "name": "providers-vol"
    },
    {
      "mountPath": "/var/mntfiles/build.json",
      "name": "secretsvol",
      "readOnly": true,
      "subPath": "build.json"
    },
    {
      "mountPath": "/contrast/contrast_security.yml",
      "name": "secretsvol",
      "readOnly": true,
      "subPath": "contrast_security.yml"
    }
  ]
}

 curl.exe -v `
>>   --connect-timeout 10 `
>>   "https://edcmdt-test.medtronic.com/oracleclinicalrdcsso/"
* Host edcmdt-test.medtronic.com:443 was resolved.
* IPv6: (none)
* IPv4: 10.210.90.218, 10.210.91.111, 10.210.91.38
*   Trying 10.210.90.218:443...
* schannel: disabled automatic use of client certificate
* ALPN: curl offers http/1.1
* ALPN: server accepted http/1.1
* Established connection to edcmdt-test.medtronic.com (10.210.90.218 port 443) from 10.213.87.36 port 50929
* using HTTP/1.x
> GET /oracleclinicalrdcsso/ HTTP/1.1
> Host: edcmdt-test.medtronic.com
> User-Agent: curl/8.21.0
> Accept: */*
>
* Request completely sent off
< HTTP/1.1 302 Found
< Date: Mon, 28 Sep 2026 16:54:52 GMT
< Content-Type: text/html; charset=iso-8859-1
< Content-Length: 584
< Connection: keep-alive
< Server: Apache
< Set-Cookie: x_csrf=KFZoXo7NLd8; Path=/; Secure; HttpOnly; SameSite=Strict
< Location: https://edcmdt-test.medtronic.com/oracleclinicalrdcsso/login.html?target_link_uri=https%3A%2F%2Fedcmdt-test.medtronic.com%3A443%2Foracleclinicalrdcsso%2F&method=get&oidc_callback=https%3A%2F%2Fedcmdt-test.medtronic.com%2Foracleclinicalrdcsso%2Fredirect&x_csrf=KFZoXo7NLd8
<
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html><head>
<title>302 Found</title>
</head><body>
<h1>Found</h1>
<p>The document has moved <a href="https://edcmdt-test.medtronic.com/oracleclinicalrdcsso/login.html?target_link_uri=https%3A%2F%2Fedcmdt-test.medtronic.com%3A443%2Foracleclinicalrdcsso%2F&amp;method=get&amp;oidc_callback=https%3A%2F%2Fedcmdt-test.medtronic.com%2Foracleclinicalrdcsso%2Fredirect&amp;x_csrf=KFZoXo7NLd8">here</a>.</p>
<hr>
<address>Apache Server at edcmdt-test.medtronic.com Port 8081</address>
</body></html>
* Connection #0 to host edcmdt-test.medtronic.com:443 left intact

