 curl -sS -o /dev/null -D - \
"http://edcmdt-test.medtronic.com/rdcadfsrnd/faces/Login?setUpDone=Y&mode=P&display_descpId=Y&display_docnum=Y&db=jdbc/rdcmdtopt"
HTTP/1.1 301 Moved Permanently
Server: awselb/2.0
Date: Tue, 29 Sep 2026 17:13:40 GMT
Content-Type: text/html
Content-Length: 134
Connection: keep-alive
Location: https://edcmdt-test.medtronic.com:443/rdcadfsrnd/faces/Login?setUpDone=Y&mode=P&display_descpId=Y&display_docnum=Y&db=jdbc/rdcmdtopt

mesac2@CPC-mesac-4BM7S:~$ curl -sS -o /dev/null -D - \
"https://edcmdt-test.medtronic.com/rdcadfsrnd/faces/Login?setUpDone=Y&mode=P&display_descpId=Y&display_docnum=Y&db=jdbc/rdcmdtopt"
HTTP/2 200
date: Tue, 29 Sep 2026 17:14:00 GMT
content-type: text/html; charset=UTF-8
server: Apache
x-content-type-options: nosniff
x-oracle-dms-ecid: 00jinYNX2SiFw0zqVv0BZzEz3xy1jlYxP0000mW0037g_
x-frame-options: sameorigin
x-oracle-dms-rid: 0:1
x-xss-protection: 1; mode=block
x-content-type-options: nosniff
set-cookie: RDCAMBERID=l1TuKLOvQsgh7kg6G1BVPfP-xBRppQzWmIU9QW1m2ZNmUA-tF-x6!695749915; path=/; secure; HttpOnly
set-cookie: BIGipServeredcmdt-dev-servers_443_pool=2213032714.47873.0000; path=/; Httponly; Secure



$K -n "$NS" exec "$POD" -- sh -c '
> grep -RniE "ProxyPass|ProxyPassReverse|rdcadfsrnd|localhost:8080" \
> /etc/apache2 /usr/local/apache2/conf 2>/dev/null
> '
Defaulted container "argo-app" out of: argo-app, argo-app-otel-init (init)
/etc/apache2/httpd.conf:694:    ProxyPass /oracleclinicalrdcsso/logout_redirect !
/etc/apache2/httpd.conf:695:    ProxyPass /oracleclinicalrdcsso/login.html !
/etc/apache2/httpd.conf:696:    ProxyPass /oracleclinicalrdcsso/logged_out.html !
/etc/apache2/httpd.conf:705:    ProxyPass /oracleclinicalrdcsso http://localhost:8080/oracleclinicalrdcsso connectiontimeout=10 timeout=600
/etc/apache2/httpd.conf:706:    ProxyPassReverse /oracleclinicalrdcsso http://localhost:8080/oracleclinicalrdcsso
/etc/apache2/httpd.conf:708:    ProxyPass /rdcadfsrnd https://${envRDCServerName}/rdcadfsrnd
/etc/apache2/httpd.conf:709:    ProxyPassReverse /rdcadfsrnd https://${envRDCServerName}/rdcadfsrnd
/etc/apache2/httpd.conf:711:    ProxyPass /edc_helpdesk_docs54 https://${envRDCDocServerName}/edc_helpdesk_docs54
/etc/apache2/httpd.conf:712:    ProxyPassReverse /edc_helpdesk_docs54 https://${envRDCDocServerName}/edc_helpdesk_docs54
/etc/apache2/httpd.conf:714:    ProxyPass /Site_Training54 https://${envRDCDocServerName}/Site_Training54
/etc/apache2/httpd.conf:715:    ProxyPassReverse /Site_Training54 https://${envRDCDocServerName}/Site_Training54
/etc/apache2/httpd.conf:717:    ProxyPass /crfimages https://${envRDCDocImageServerName}/crfimages
/etc/apache2/httpd.conf:718:    ProxyPassReverse /crfimages https://${envRDCDocImageServerName}/crfimages
/etc/apache2/httpd.conf:720:    ProxyPass /opa54 https://${envRDCDocImageServerName}/opa54
/etc/apache2/httpd.conf:721:    ProxyPassReverse /opa54 https://${envRDCDocImageServerName}/opa54
/etc/apache2/httpd.conf:723:    ProxyPass /news https://${envRDCDocImageServerName}/news
/etc/apache2/httpd.conf:724:    ProxyPassReverse /news https://${envRDCDocImageServerName}/news
/etc/apache2/httpd.conf:726:    ProxyPass /tmp/ https://${envRDCDocImageServerName}/tmp/
/etc/apache2/httpd.conf:727:    ProxyPassReverse /tmp/ https://${envRDCDocImageServerName}/tmp/
command terminated with exit code 2
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ $K -n "$NS" exec "$POD" -- sh -c '
> echo "CATALINA_HOME=$CATALINA_HOME"
> grep -RniE "Connector|proxyName|proxyPort|scheme=|secure=|RemoteIpValve" \
> "${CATALINA_HOME:-/opt/tomcat}/conf" 2>/dev/null
> '
Defaulted container "argo-app" out of: argo-app, argo-app-otel-init (init)
CATALINA_HOME=/usr/local/apache-tomcat-10.1.54
/usr/local/apache-tomcat-10.1.54/conf/server.xml:73:  <!-- A "Service" is a collection of one or more "Connectors" that share
/usr/local/apache-tomcat-10.1.54/conf/server.xml:80:    <!--The connectors can use a shared executor, you can define one or more named thread pools-->
/usr/local/apache-tomcat-10.1.54/conf/server.xml:87:    <!-- A "Connector" represents an endpoint by which requests are received
/usr/local/apache-tomcat-10.1.54/conf/server.xml:89:         Java HTTP Connector: /docs/config/http.html
/usr/local/apache-tomcat-10.1.54/conf/server.xml:90:         Java AJP  Connector: /docs/config/ajp.html
/usr/local/apache-tomcat-10.1.54/conf/server.xml:91:         APR (HTTP/AJP) Connector: /docs/apr.html
/usr/local/apache-tomcat-10.1.54/conf/server.xml:92:         Define a non-SSL/TLS HTTP/1.1 Connector on port 8080
/usr/local/apache-tomcat-10.1.54/conf/server.xml:95:    <!-- proxyName="oracleclinicalrdcsso.${medtronic.environment.deployment}.${medtronic.environment.defaultHostDomain}" -->
/usr/local/apache-tomcat-10.1.54/conf/server.xml:96:    <Connector port="8080" maxHttpHeaderSize="65536" protocol="HTTP/1.1"
/usr/local/apache-tomcat-10.1.54/conf/server.xml:98:               scheme="https" secure="true"
/usr/local/apache-tomcat-10.1.54/conf/server.xml:99:               proxyPort="443"
/usr/local/apache-tomcat-10.1.54/conf/server.xml:100:               proxyName="oracleclinicalrdcsso.${medtronic.environment.deployment}.${medtronic.environment.defaultHostDomain}"
/usr/local/apache-tomcat-10.1.54/conf/server.xml:104:    <!-- A "Connector" using the shared thread pool-->
/usr/local/apache-tomcat-10.1.54/conf/server.xml:106:    <Connector executor="tomcatThreadPool"
/usr/local/apache-tomcat-10.1.54/conf/server.xml:111:    <!-- Define a SSL/TLS HTTP/1.1 Connector on port 8443
/usr/local/apache-tomcat-10.1.54/conf/server.xml:112:         This connector uses the NIO implementation. The default
/usr/local/apache-tomcat-10.1.54/conf/server.xml:120:    <Connector port="8443" protocol="org.apache.coyote.http11.Http11NioProtocol"
/usr/local/apache-tomcat-10.1.54/conf/server.xml:126:    </Connector>
/usr/local/apache-tomcat-10.1.54/conf/server.xml:128:    <!-- Define a SSL/TLS HTTP/1.1 Connector on port 8443 with HTTP/2
/usr/local/apache-tomcat-10.1.54/conf/server.xml:129:         This connector uses the APR/native implementation which always uses
/usr/local/apache-tomcat-10.1.54/conf/server.xml:135:    <Connector port="8443" protocol="org.apache.coyote.http11.Http11AprProtocol"
/usr/local/apache-tomcat-10.1.54/conf/server.xml:144:    </Connector>
/usr/local/apache-tomcat-10.1.54/conf/server.xml:147:    <!-- Define an AJP 1.3 Connector on port 8009 -->
/usr/local/apache-tomcat-10.1.54/conf/server.xml:149:    <Connector port="8009" protocol="AJP/1.3" redirectPort="8443" />
/usr/local/apache-tomcat-10.1.54/conf/server.xml:202:           <Valve className="org.apache.catalina.valves.RemoteIpValve" hostHeader="x-forwarded-host" changeLocalName="true" changeLocalPort="true"  />
/usr/local/apache-tomcat-10.1.54/conf/web.xml:80:  <!--   sendfileSize        If the connector used supports sendfile, this  -->