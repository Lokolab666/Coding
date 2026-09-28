

hostname
CPC-mesac-4BM7S
PS C:\Users\mesac2> ipconfig | findstr /i "IPv4"
   IPv4 Address. . . . . . . . . . . : 10.213.87.36
   IPv4 Address. . . . . . . . . . . : 192.168.176.1
PS C:\Users\mesac2> Resolve-DnsName edcmdt-test.medtronic.com

Name                           Type   TTL   Section    NameHost
----                           ----   ---   -------    --------
edcmdt-test.medtronic.com      CNAME  900   Answer     internal-k8s-intnon003-128bf56b67-943687084.us-east-1.elb.amazon
                                                       aws.com

Name       : internal-k8s-intnon003-128bf56b67-943687084.us-east-1.elb.amazonaws.com
QueryType  : A
TTL        : 60
Section    : Answer
IP4Address : 10.210.90.218


Name       : internal-k8s-intnon003-128bf56b67-943687084.us-east-1.elb.amazonaws.com
QueryType  : A                                                                                                          TTL        : 60                                                                                                         Section    : Answer                                                                                                     IP4Address : 10.210.91.38                                                                                                                                                                                                                                                                                                                                               Name       : internal-k8s-intnon003-128bf56b67-943687084.us-east-1.elb.amazonaws.com                                    QueryType  : A                                                                                                          TTL        : 60                                                                                                         Section    : Answer                                                                                                     IP4Address : 10.210.91.111                                                                                                                                                                                                                                                                                                                                              Name                   : us-east-1.elb.amazonaws.com                                                                    QueryType              : SOA                                                                                            TTL                    : 60                                                                                             Section                : Authority                                                                                      NameAdministrator      : awsdns-hostmaster.amazon.com
SerialNumber           : 1
TimeToZoneRefresh      : 7200
TimeToZoneFailureRetry : 900
TimeToExpiration       : 1209600
DefaultTTL             : 60



PS C:\Users\mesac2> Test-NetConnection edcmdt-test.medtronic.com -Port 443


ComputerName     : edcmdt-test.medtronic.com
RemoteAddress    : 10.210.91.38
RemotePort       : 443
InterfaceAlias   : Ethernet
SourceAddress    : 10.213.87.36
TcpTestSucceeded : True



PS C:\Users\mesac2> curl.exe -v `
>>   --connect-timeout 10 `
>>   "https://edcmdt-test.medtronic.com/oracleclinicalrdcsso/"
* Host edcmdt-test.medtronic.com:443 was resolved.
* IPv6: (none)
* IPv4: 10.210.91.38, 10.210.91.111, 10.210.90.218
*   Trying 10.210.91.38:443...
* schannel: disabled automatic use of client certificate
* ALPN: curl offers http/1.1
* ALPN: server accepted http/1.1
* Established connection to edcmdt-test.medtronic.com (10.210.91.38 port 443) from 10.213.87.36 port 64148
* using HTTP/1.x
> GET /oracleclinicalrdcsso/ HTTP/1.1
> Host: edcmdt-test.medtronic.com
> User-Agent: curl/8.21.0
> Accept: */*
>
* Request completely sent off
< HTTP/1.1 302 Found
< Date: Mon, 28 Sep 2026 17:09:34 GMT
< Content-Type: text/html; charset=iso-8859-1
< Content-Length: 584
< Connection: keep-alive
< Server: Apache
< Set-Cookie: x_csrf=oF-9TtErk7c; Path=/; Secure; HttpOnly; SameSite=Strict
< Location: https://edcmdt-test.medtronic.com/oracleclinicalrdcsso/login.html?target_link_uri=https%3A%2F%2Fedcmdt-test.medtronic.com%3A443%2Foracleclinicalrdcsso%2F&method=get&oidc_callback=https%3A%2F%2Fedcmdt-test.medtronic.com%2Foracleclinicalrdcsso%2Fredirect&x_csrf=oF-9TtErk7c
<
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html><head>
<title>302 Found</title>
</head><body>
<h1>Found</h1>
<p>The document has moved <a href="https://edcmdt-test.medtronic.com/oracleclinicalrdcsso/login.html?target_link_uri=https%3A%2F%2Fedcmdt-test.medtronic.com%3A443%2Foracleclinicalrdcsso%2F&amp;method=get&amp;oidc_callback=https%3A%2F%2Fedcmdt-test.medtronic.com%2Foracleclinicalrdcsso%2Fredirect&amp;x_csrf=oF-9TtErk7c">here</a>.</p>
<hr>
<address>Apache Server at edcmdt-test.medtronic.com Port 8081</address>
</body></html>
* Connection #0 to host edcmdt-test.medtronic.com:443 left intact