Context:
We are working in an incident in the cluster of AWS with some namespaces about javax.net.ssl.SSLHandshakeException: Received fatal alert: handshake_failure when the base image was update from 2026.02.02 to 2026.03.01They are not using ORACLE RDS Database, they are using on-prem oracle database server. They are able to connect from sql developer and in our localhost. only problem in argo environment that too due to recent tomcat_httpd base image upgrade from 02.01.2026 to 03.01.2026. with 02.01.2026
Error logs:

/environment $ ./kubectl --kubeconfig ~/.kube/argo-dev.yaml -n argo-worldwiderevenue-dev logs worldwiderevenue-dev-7fc9f8d5fb-tzmsw -c argo-app --tail=200
Picked up JAVA_TOOL_OPTIONS: -javaagent:/otel/opentelemetry.jar
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
[otel.javaagent 2026-03-12 11:32:07:329 -0500] [main] INFO io.opentelemetry.javaagent.tooling.VersionLogger - opentelemetry-javaagent - version: 1.26.0-aws
CONTRAST network access SUCCESSFUL!
Picked up JAVA_TOOL_OPTIONS: -javaagent:/otel/opentelemetry.jar
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
[otel.javaagent 2026-03-12 11:32:10:188 -0500] [main] INFO io.opentelemetry.javaagent.tooling.VersionLogger - opentelemetry-javaagent - version: 1.26.0-aws
DATABASE JDBC CONNECTION SUCCESSFUL jdbc:oracle:thin:@orcl-dihi2.corp.medtronic.com:1521/dihi2.test.corp.medtronic.com
Picked up JAVA_TOOL_OPTIONS: -javaagent:/otel/opentelemetry.jar
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
[otel.javaagent 2026-03-12 11:32:13:881 -0500] [main] INFO io.opentelemetry.javaagent.tooling.VersionLogger - opentelemetry-javaagent - version: 1.26.0-aws
Mar 12, 2026 11:32:15 AM oracle.jdbc.driver.PhysicalConnection connect
INFO: entering args (oracle.jdbc.internal.AbstractConnectionBuilder$1@19a5b637)
Mar 12, 2026 11:32:15 AM oracle.net.ns.NSProtocol connect
INFO: traceId=35CEC305. 
Mar 12, 2026 11:32:15 AM oracle.net.ns.NSProtocol establishConnection
INFO: Session Attributes: 
sdu=8192, tdu=2097152
nt: host=mspldb515.corp.medtronic.com, port=2484, socketOptions={0=YES, 1=NO, 17=0, 18=false, 2=20000, 20=true, 38=TLS, 23=40, 24=50, 40=false, 25=0}
    socket=null
client profile={oracle.net.encryption_types_client=(), oracle.net.crypto_seed=, oracle.net.authentication_services=(), oracle.net.setFIPSMode=false, oracle.net.kerberos5_mutual_authentication=false, oracle.net.encryption_client=ACCEPTED, oracle.net.crypto_checksum_client=ACCEPTED, oracle.net.crypto_checksum_types_client=()}
onBreakReset=false, dataEOF=false, negotiatedOptions=0x0, connected=false
TTIINIT enabled=false, TTC cookie enabled=false

Mar 12, 2026 11:32:15 AM oracle.net.ns.NSProtocol configureSessionAttsAno
INFO: traceId=35CEC305, anoEnabled=true. 
Mar 12, 2026 11:32:15 AM oracle.net.ns.NSProtocolNIO handleConnectPacketResponse
INFO: Got Resend, SessionTraceId = 35CEC305
Mar 12, 2026 11:32:15 AM oracle.net.ns.NSProtocolNIO handleIOException
INFO: Connection establishment failed due to IOException, will be trying with next available connect option.
Mar 12, 2026 11:32:15 AM oracle.net.ns.NSProtocolNIO handleConnectPacketResponse
INFO: Got Refused, SessionTraceId = 35CEC305
Mar 12, 2026 11:32:15 AM oracle.net.ns.NSProtocolNIO establishConnectionAfterRefusePacket
INFO: Outbound interrupt timer cancelled null
Mar 12, 2026 11:32:15 AM oracle.jdbc.driver.PhysicalConnection connect
INFO: throwing
java.sql.SQLRecoverableException: ORA-17967: SSL Handshake failure.: Received fatal alert: handshake_failure (CONNECTION_ID=4epCFgpDSvSuHcPiay6/1A==)
https://docs.oracle.com/error-help/db/ora-17967/
        at oracle.jdbc.driver.T4CConnection.handleLogonNetException(T4CConnection.java:1631)
        at oracle.jdbc.driver.T4CConnection.logon(T4CConnection.java:1151)
        at oracle.jdbc.driver.PhysicalConnection.connect(PhysicalConnection.java:1189)
        at oracle.jdbc.driver.T4CDriverExtension.getConnection(T4CDriverExtension.java:106)
        at oracle.jdbc.driver.OracleDriver.connect(OracleDriver.java:895)
        at oracle.jdbc.driver.OracleDriver.connect(OracleDriver.java:702)
        at java.sql/java.sql.DriverManager.getConnection(Unknown Source)
        at java.sql/java.sql.DriverManager.getConnection(Unknown Source)
        at DBCheck.main(DBCheck.java:50)
Caused by: oracle.net.ns.NetException: ORA-17967: SSL Handshake failure.: Received fatal alert: handshake_failure (CONNECTION_ID=4epCFgpDSvSuHcPiay6/1A==)
https://docs.oracle.com/error-help/db/ora-17967/
        at oracle.net.nt.SSLSocketChannel.handshakeFailure(SSLSocketChannel.java:830)
        at oracle.net.nt.SSLSocketChannel.unwrap(SSLSocketChannel.java:810)
        at oracle.net.nt.SSLSocketChannel.unwrapHandshakeMessage(SSLSocketChannel.java:735)
        at oracle.net.nt.SSLSocketChannel.doSSLHandshake(SSLSocketChannel.java:534)
        at oracle.net.nt.SSLSocketChannel.write(SSLSocketChannel.java:198)
        at oracle.net.ns.NIOPacket.writeToSocketChannel(NIOPacket.java:374)
        at oracle.net.ns.NIOConnectPacket.writeToSocketChannel(NIOConnectPacket.java:290)
        at oracle.net.ns.NSProtocolNIO.negotiateConnection(NSProtocolNIO.java:199)
        at oracle.net.ns.NSProtocol.connect(NSProtocol.java:353)
        at oracle.jdbc.driver.T4CConnection.connectNetworkSessionProtocol(T4CConnection.java:3462)
        at oracle.jdbc.driver.T4CConnection.logon(T4CConnection.java:1030)
        ... 7 more
Caused by: javax.net.ssl.SSLHandshakeException: Received fatal alert: handshake_failure
        at java.base/sun.security.ssl.Alert.createSSLException(Unknown Source)
        at java.base/sun.security.ssl.Alert.createSSLException(Unknown Source)
        at java.base/sun.security.ssl.TransportContext.fatal(Unknown Source)
        at java.base/sun.security.ssl.Alert$AlertConsumer.consume(Unknown Source)
        at java.base/sun.security.ssl.TransportContext.dispatch(Unknown Source)
        at java.base/sun.security.ssl.SSLTransport.decode(Unknown Source)
        at java.base/sun.security.ssl.SSLEngineImpl.decode(Unknown Source)
        at java.base/sun.security.ssl.SSLEngineImpl.readRecord(Unknown Source)
        at java.base/sun.security.ssl.SSLEngineImpl.unwrap(Unknown Source)
        at java.base/sun.security.ssl.SSLEngineImpl.unwrap(Unknown Source)
        at java.base/javax.net.ssl.SSLEngine.unwrap(Unknown Source)
        at oracle.net.nt.SSLSocketChannel.unwrap(SSLSocketChannel.java:787)
        ... 16 more

Mar 12, 2026 11:32:15 AM oracle.jdbc.diagnostics.Diagnostic dumpDiagnoseFirstFailure
INFO: properties={LOCALE=en_US, DriverVersion=23.6.0.24.10, java.library.path: =/opt/java/openjdk/lib/server:/opt/java/openjdk/lib:/opt/java/openjdk/../lib:/usr/java/packages/lib:/usr/lib64:/lib64:/lib:/usr/lib, java.class.path: =/usr/local/lib/DBCheck.jar:/usr/local/tomcat/lib/ojdbc17-23.6.0.24.10.jar, java.version: =17.0.18}. 
java.sql.SQLRecoverableException: ORA-17967: SSL Handshake failure.: Received fatal alert: handshake_failure (CONNECTION_ID=4epCFgpDSvSuHcPiay6/1A==)
https://docs.oracle.com/error-help/db/ora-17967/
        at oracle.jdbc.driver.T4CConnection.handleLogonNetException(T4CConnection.java:1631)
        at oracle.jdbc.driver.T4CConnection.logon(T4CConnection.java:1151)
        at oracle.jdbc.driver.PhysicalConnection.connect(PhysicalConnection.java:1189)
        at oracle.jdbc.driver.T4CDriverExtension.getConnection(T4CDriverExtension.java:106)
        at oracle.jdbc.driver.OracleDriver.connect(OracleDriver.java:895)
        at oracle.jdbc.driver.OracleDriver.connect(OracleDriver.java:702)
        at java.sql/java.sql.DriverManager.getConnection(Unknown Source)
        at java.sql/java.sql.DriverManager.getConnection(Unknown Source)
        at DBCheck.main(DBCheck.java:50)
Caused by: oracle.net.ns.NetException: ORA-17967: SSL Handshake failure.: Received fatal alert: handshake_failure (CONNECTION_ID=4epCFgpDSvSuHcPiay6/1A==)
https://docs.oracle.com/error-help/db/ora-17967/
        at oracle.net.nt.SSLSocketChannel.handshakeFailure(SSLSocketChannel.java:830)
        at oracle.net.nt.SSLSocketChannel.unwrap(SSLSocketChannel.java:810)
        at oracle.net.nt.SSLSocketChannel.unwrapHandshakeMessage(SSLSocketChannel.java:735)
        at oracle.net.nt.SSLSocketChannel.doSSLHandshake(SSLSocketChannel.java:534)
        at oracle.net.nt.SSLSocketChannel.write(SSLSocketChannel.java:198)
        at oracle.net.ns.NIOPacket.writeToSocketChannel(NIOPacket.java:374)
        at oracle.net.ns.NIOConnectPacket.writeToSocketChannel(NIOConnectPacket.java:290)
        at oracle.net.ns.NSProtocolNIO.negotiateConnection(NSProtocolNIO.java:199)
        at oracle.net.ns.NSProtocol.connect(NSProtocol.java:353)
        at oracle.jdbc.driver.T4CConnection.connectNetworkSessionProtocol(T4CConnection.java:3462)
        at oracle.jdbc.driver.T4CConnection.logon(T4CConnection.java:1030)
        ... 7 more
Caused by: javax.net.ssl.SSLHandshakeException: Received fatal alert: handshake_failure
        at java.base/sun.security.ssl.Alert.createSSLException(Unknown Source)
        at java.base/sun.security.ssl.Alert.createSSLException(Unknown Source)
        at java.base/sun.security.ssl.TransportContext.fatal(Unknown Source)
        at java.base/sun.security.ssl.Alert$AlertConsumer.consume(Unknown Source)
        at java.base/sun.security.ssl.TransportContext.dispatch(Unknown Source)
        at java.base/sun.security.ssl.SSLTransport.decode(Unknown Source)
        at java.base/sun.security.ssl.SSLEngineImpl.decode(Unknown Source)
        at java.base/sun.security.ssl.SSLEngineImpl.readRecord(Unknown Source)
        at java.base/sun.security.ssl.SSLEngineImpl.unwrap(Unknown Source)
        at java.base/sun.security.ssl.SSLEngineImpl.unwrap(Unknown Source)
        at java.base/javax.net.ssl.SSLEngine.unwrap(Unknown Source)
        at oracle.net.nt.SSLSocketChannel.unwrap(SSLSocketChannel.java:787)
        ... 16 more
DATABASE JDBC FAILURE [jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=tcps)(HOST=mspldb515.corp.medtronic.com)(PORT=2484))(CONNECT_DATA=(SERVICE_NAME=web12s.stage.corp.medtronic.com)))] : 1
Error during networkTest.sh ... exiting.





What I already did
sharing an update about the SSL validation tests against mspldb515.corp.medtronic.com:2484, I ran the following command to validate the certificate presented by the database listener:
Plain Text
openssl s_client -connect mspldb515.corp.medtronic.com:2484 \
-servername mspldb515.corp.medtronic.com -tls1_2 -showcerts </dev/null
 and the connection works (handshake completed), but OpenSSL reports a chain validation issue:
 
Plain Text
verify error:num=20:unable to get local issuer certificate
verify error:num=21:unable to verify the first certificate
Verification error: unable to verify the first certificate
The server is presenting its certificate correctly, and it’s valid (exp: July 2027), but OpenSSL cannot complete chain verification because the issuer CA is not trusted locally:
 
Plain Text
issuer=O=Medtronic, CN=MDT Issuing CA 2-7
To confirm this, I checked if the CA exists in the JVM truststore:
 
Plain Text
keytool -list -cacerts -storepass changeit | grep -i "MDT Issuing CA 2-7" || echo "CA not found"
And as expected:
 
Plain Text
CA not found
So the handshake succeeds, but Java can’t validate the certificate chain, which explains the errors

I ran a few validation commands directly inside the TomcatHTTPD base image and confirmed the image already includes the major public CA root certificates used by AWS RDS, including:
 
DigiCert roots
GlobalSign roots
Entrust roots
Amazon RDS CA 2019
Amazon RDS us‑east‑1 regional CAs
 
Based on this, the “missing certificate” issue is not coming from the base image. The base image truststore already contains the AWS RDS CA chain, so the base image is not missing the RDS CA certificates

The Ash (my leader was)
yea, I think most of these apps aren't using RDS, they are using on-prem oracle DBs. If you want to test from the exact app containers, you can always turn their health checks off: https://code.medtronic.com/bcp_web/devops/fluxconfigs/argononprod/-/blob/main/argo-oracleclinicalrdcsso-testing/app.yaml?ref_type=heads#L36
 
Then that'll allow those pods to stay up and running regardless of any error so you could shell into it and run any commands you need to to check certs

And the next, are the logs after do the next commands:
kubectl --kubeconfig ~/.kube/argo-dev.yaml -n argo-worldwiderevenue-dev get secret secrets-files -o jsonpath='{.data.cacerts}' | base64 -d > secret-cacerts

 java -version
openjdk version "11.0.30" 2026-01-20 LTS
OpenJDK Runtime Environment Corretto-11.0.30.7.1 (build 11.0.30+7-LTS)
OpenJDK 64-Bit Server VM Corretto-11.0.30.7.1 (build 11.0.30+7-LTS, mixed mode)
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ which keytool
/usr/bin/keytool
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ keytool -help | head
Key and Certificate Management Tool

Commands:

 -certreq            Generates a certificate request
 -changealias        Changes an entry's alias
 -delete             Deletes an entry
 -exportcert         Exports certificate
 -genkeypair         Generates a key pair
 -genseckey          Generates a secret key
 -gencert            Generates certificate from a certificate request
 -importcert         Imports a certificate or a certificate chain
 -importpass         Imports a password
 -importkeystore     Imports one or all entries from another keystore
 -keypasswd          Changes the key password of an entry
 -list               Lists entries in a keystore
 -printcert          Prints the content of a certificate
 -printcertreq       Prints the content of a certificate request
 -printcrl           Prints the content of a CRL file
 -storepasswd        Changes the store password of a keystore

Use "keytool -?, -h, or --help" for this help message
Use "keytool -command_name --help" for usage of command_name.
Use the -conf <url> option to specify a pre-configured options file.
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ 
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ ./kubectl --kubeconfig ~/.kube/argo-dev.yaml -n argo-worldwiderevenue-dev get secret secrets-files \
>   -o jsonpath='{.metadata.labels}{"\n"}{.metadata.annotations}{"\n"}'
{"kustomize.toolkit.fluxcd.io/name":"argononprod","kustomize.toolkit.fluxcd.io/namespace":"default"}

AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ 
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ 
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ ./kubectl --kubeconfig ~/.kube/argo-dev.yaml -n argo-worldwiderevenue-dev patch secret secrets-files \
>   --type='json' \
>   -p="[{\"op\":\"add\",\"path\":\"/data/cacerts\",\"value\":\"$NEW_B64\"}]"
secret/secrets-files patched
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ 
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ ./kubectl --kubeconfig ~/.kube/argo-dev.yaml -n argo-worldwiderevenue-dev patch secret secrets-files \
>   --type='json' \
>   -p="[{\"op\":\"add\",\"path\":\"/data/cacerts\",\"value\":\"$NEW_B64\"}]"
secret/secrets-files patched (no change)
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ ./kubectl --kubeconfig ~/.kube/argo-dev.yaml -n argo-worldwiderevenue-dev rollout restart deploy/worldwiderevenue-dev
deployment.apps/worldwiderevenue-dev restarted
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ ./kubectl --kubeconfig ~/.kube/argo-dev.yaml -n argo-worldwiderevenue-dev rollout status deploy/worldwiderevenue-dev
Waiting for deployment "worldwiderevenue-dev" rollout to finish: 0 of 2 updated replicas are available...
Waiting for deployment "worldwiderevenue-dev" rollout to finish: 1 of 2 updated replicas are available...
Waiting for deployment "worldwiderevenue-dev" rollout to finish: 0 of 2 updated replicas are available...
Waiting for deployment "worldwiderevenue-dev" rollout to finish: 1 of 2 updated replicas are available...
Waiting for deployment "worldwiderevenue-dev" rollout to finish: 0 of 2 updated replicas are available...
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ ./kubectl --kubeconfig ~/.kube/argo-dev.yaml -n argo-worldwiderevenue-dev get pods -o wide                       
NAME                                    READY   STATUS             RESTARTS      AGE    IP               NODE                            NOMINATED NODE   READINESS GATES
worldwiderevenue-dev-64b6dc766b-rsnd9   0/1     Error              701           6d5h   100.64.7.111     ip-10-210-90-217.ec2.internal   <none>           <none>
worldwiderevenue-dev-77949c68dd-chclq   0/1     CrashLoopBackOff   2 (17s ago)   48s    100.64.55.123    ip-10-210-90-202.ec2.internal   <none>           <none>
worldwiderevenue-dev-77949c68dd-rrzjx   0/1     CrashLoopBackOff   2 (22s ago)   51s    100.64.128.158   ip-10-210-91-114.ec2.internal   <none>           <none>
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ ./kubectl --kubeconfig ~/.kube/argo-dev.yaml -n argo-worldwiderevenue-dev get pods -o wide
NAME                                    READY   STATUS   RESTARTS      AGE    IP               NODE                            NOMINATED NODE   READINESS GATES
worldwiderevenue-dev-64b6dc766b-rsnd9   0/1     Error    701           6d5h   100.64.7.111     ip-10-210-90-217.ec2.internal   <none>           <none>
worldwiderevenue-dev-77949c68dd-chclq   0/1     Error    4 (54s ago)   115s   100.64.55.123    ip-10-210-90-202.ec2.internal   <none>           <none>
worldwiderevenue-dev-77949c68dd-rrzjx   0/1     Error    4 (56s ago)   118s   100.64.128.158   ip-10-210-91-114.ec2.internal   <none>           <none>
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ keytool -list -keystore secret-cacerts -storepass changeit | grep -i "MDT Issuing CA" || echo "CA not found"
CA not found


And in Grafana, the logs are:
DATABASE JDBC FAILURE [jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=tcps)(HOST=mspldb515.corp.medtronic.com)(PORT=2484))(CONNECT_DATA=(SERVICE_NAME=web12s.stage.corp.medtronic.com)))] : 1
	... 25 more
	at java.base/sun.security.ssl.DummyX509TrustManager.checkServerTrusted(Unknown Source)
Caused by: java.security.cert.CertificateException: No X509TrustManager implementation available
	at oracle.net.ns.NIOConnectPacket.writeToSocketChannel(NIOConnectPacket.java:290)
	at oracle.jdbc.driver.T4CConnection.handleLogonIOException(T4CConnection.java:1669)
	at oracle.net.nt.SSLSocketChannel.doSSLHandshake(SSLSocketChannel.java:530)
	at oracle.net.nt.SSLSocketChannel.runTasks(SSLSocketChannel.java:955)
	at java.base/sun.security.ssl.SSLEngineImpl$DelegatedTask.run(Unknown Source)
	at java.base/java.security.AccessController.doPrivileged(Unknown Source)
	... 10 more
	at java.base/sun.security.ssl.SSLEngineImpl$DelegatedTask$DelegatedAction.run(Unknown Source)
	at java.base/sun.security.ssl.HandshakeContext.dispatch(Unknown Source)
	at java.base/sun.security.ssl.SSLHandshake.consume(Unknown Source)
	at java.base/sun.security.ssl.CertificateMessage$T12CertificateConsumer.consume(Unknown Source)
	at java.base/sun.security.ssl.CertificateMessage$T12CertificateConsumer.onCertificate(Unknown Source)
	at java.base/sun.security.ssl.CertificateMessage$T12CertificateConsumer.checkServerCerts(Unknown Source)
	at java.base/sun.security.ssl.TransportContext.fatal(Unknown Source)
	at java.base/sun.security.ssl.TransportContext.fatal(Unknown Source)
	at java.base/sun.security.ssl.TransportContext.fatal(Unknown Source)
	at java.base/sun.security.ssl.Alert.createSSLException(Unknown Source)
Caused by: java.io.IOException: IO Error No X509TrustManager implementation available, connect lapse 86 ms., Authentication lapse 0 ms.
	at java.base/sun.security.ssl.SSLEngineImpl$DelegatedTask$DelegatedAction.run(Unknown Source)
	... 14 more
Caused by: javax.net.ssl.SSLHandshakeException: No X509TrustManager implementation available
	at oracle.net.ns.NIOPacket.writeToSocketChannel(NIOPacket.java:374)
	at oracle.net.nt.SSLSocketChannel.write(SSLSocketChannel.java:198)
	at oracle.net.nt.SSLSocketChannel.doSSLHandshake(SSLSocketChannel.java:538)
	at oracle.net.nt.SSLSocketChannel.wrapHandshakeMessage(SSLSocketChannel.java:687)
	at oracle.net.nt.SSLSocketChannel.wrap(SSLSocketChannel.java:893)
Caused by: java.io.IOException: IO Error No X509TrustManager implementation available
	... 7 more
	at oracle.jdbc.driver.T4CConnection.logon(T4CConnection.java:1030)
	at oracle.jdbc.driver.T4CConnection.connectNetworkSessionProtocol(T4CConnection.java:3462)
	at oracle.net.ns.NSProtocol.connect(NSProtocol.java:353)
	at oracle.net.ns.NSProtocolNIO.negotiateConnection(NSProtocolNIO.java:264)
Caused by: java.io.IOException: IO Error No X509TrustManager implementation available, connect lapse 86 ms.
	... 8 more
	at oracle.net.ns.NSProtocolNIO.negotiateConnection(NSProtocolNIO.java:199)
	at oracle.net.nt.SSLSocketChannel.runTasks(SSLSocketChannel.java:955)
	at DBCheck.main(DBCheck.java:50)
	at java.sql/java.sql.DriverManager.getConnection(Unknown Source)
	at java.sql/java.sql.DriverManager.getConnection(Unknown Source)
	at oracle.jdbc.driver.OracleDriver.connect(OracleDriver.java:702)
	at oracle.jdbc.driver.OracleDriver.connect(OracleDriver.java:895)
	at oracle.jdbc.driver.T4CDriverExtension.getConnection(T4CDriverExtension.java:106)
	at oracle.jdbc.driver.PhysicalConnection.connect(PhysicalConnection.java:1189)
	at oracle.jdbc.driver.T4CConnection.logon(T4CConnection.java:1154)

	at oracle.jdbc.driver.T4CConnection.handleLogonIOException(T4CConnection.java:1674)
https://docs.oracle.com/error-help/db/ora-17002/
java.sql.SQLRecoverableException: ORA-17002: I/O error: IO Error No X509TrustManager implementation available, connect lapse 86 ms., Authentication lapse 0 ms.
INFO: properties={LOCALE=en_US, DriverVersion=23.6.0.24.10, java.library.path: =/opt/java/openjdk/lib/server:/opt/java/openjdk/lib:/opt/java/openjdk/../lib:/usr/java/packages/lib:/usr/lib64:/lib64:/lib:/usr/lib, java.class.path: =/usr/local/lib/DBCheck.jar:/usr/local/tomcat/lib/ojdbc17-23.6.0.24.10.jar, java.version: =17.0.18}. 
Mar 12, 2026 11:48:03 AM oracle.jdbc.diagnostics.Diagnostic dumpDiagnoseFirstFailure

	... 25 more
	at java.base/sun.security.ssl.DummyX509TrustManager.checkServerTrusted(Unknown Source)
Caused by: java.security.cert.CertificateException: No X509TrustManager implementation available
	... 14 more
	at oracle.net.nt.SSLSocketChannel.doSSLHandshake(SSLSocketChannel.java:530)
Mar 12, 2026 11:48:03 AM oracle.jdbc.driver.PhysicalConnection connect
	at java.base/sun.security.ssl.SSLEngineImpl$DelegatedTask.run(Unknown Source)
	at java.base/java.security.AccessController.doPrivileged(Unknown Source)
	at java.base/sun.security.ssl.SSLEngineImpl$DelegatedTask$DelegatedAction.run(Unknown Source)
	at java.base/sun.security.ssl.SSLEngineImpl$DelegatedTask$DelegatedAction.run(Unknown Source)
	at java.base/sun.security.ssl.HandshakeContext.dispatch(Unknown Source)
	at java.base/sun.security.ssl.SSLHandshake.consume(Unknown Source)
	at java.base/sun.security.ssl.CertificateMessage$T12CertificateConsumer.consume(Unknown Source)
	at java.base/sun.security.ssl.CertificateMessage$T12CertificateConsumer.onCertificate(Unknown Source)
	at java.base/sun.security.ssl.CertificateMessage$T12CertificateConsumer.checkServerCerts(Unknown Source)
	at java.base/sun.security.ssl.TransportContext.fatal(Unknown Source)
	at java.base/sun.security.ssl.TransportContext.fatal(Unknown Source)
	at java.base/sun.security.ssl.TransportContext.fatal(Unknown Source)
	at java.base/sun.security.ssl.Alert.createSSLException(Unknown Source)
Caused by: javax.net.ssl.SSLHandshakeException: No X509TrustManager implementation available
	... 10 more

$ keytool -list -keystore secret-cacerts -storepass changeit | head -n 20
Keystore type: JKS
Keystore provider: SUN

Your keystore contains 147 entries

c = at, o = e-commerce monitoring gmbh, cn = globaltrust 2020, Jan 18, 2023, trustedCertEntry, 
Certificate fingerprint (SHA-256): 9A:29:6A:51:82:D1:D4:51:A2:E3:7F:43:9B:74:DA:AF:A2:67:52:33:29:F9:0F:9A:0D:20:07:C3:34:E2:3C:9A
c = be, o = globalsign nv-sa, cn = globalsign root e46, Jan 18, 2023, trustedCertEntry, 
Certificate fingerprint (SHA-256): CB:B9:C4:4D:84:B8:04:3E:10:50:EA:31:A6:9F:51:49:55:D7:BF:D2:E2:C6:B4:93:01:01:9A:D6:1D:9F:50:58
c = be, o = globalsign nv-sa, cn = globalsign root r46, Jan 18, 2023, trustedCertEntry, 
Certificate fingerprint (SHA-256): 4F:A3:12:6D:8D:3A:11:D1:C4:85:5A:4F:80:7C:BA:D6:CF:91:9D:3A:5A:88:B0:3B:EA:2C:63:72:D9:3C:40:C9
c = be, o = globalsign nv-sa, ou = root ca, cn = globalsign root ca, Jan 18, 2023, trustedCertEntry, 
Certificate fingerprint (SHA-256): EB:D4:10:40:E4:BB:3E:C7:42:C9:E3:81:D3:1E:F2:A4:1A:48:B6:68:5C:96:E7:CE:F3:C1:DF:6C:D4:33:1C:99
c = bm, o = quovadis limited, cn = quovadis root ca 1 g3, Jan 18, 2023, trustedCertEntry, 
Certificate fingerprint (SHA-256): 8A:86:6F:D1:B2:76:B5:7E:57:8E:92:1C:65:82:8A:2B:ED:58:E9:F2:F2:88:05:41:34:B7:F1:F4:BF:C9:CC:74
c = bm, o = quovadis limited, cn = quovadis root ca 2, Jan 18, 2023, trustedCertEntry, 
Certificate fingerprint (SHA-256): 85:A0:DD:7D:D7:20:AD:B7:FF:05:F8:3D:54:2B:20:9D:C7:FF:45:28:F7:D6:77:B1:83:89:FE:A5:E5:C4:9E:86
c = bm, o = quovadis limited, cn = quovadis root ca 2 g3, Jan 18, 2023, trustedCertEntry, 
Certificate fingerprint (SHA-256): 8F:E4:FB:0A:F9:3A:4D:0D:67:DB:0B:EB:B2:3E:37:C7:1B:F3:25:DC:BC:DD:24:0E:A0:4D:AF:58:B4:7E:18:40
c = bm, o = quovadis limited, cn = quovadis root ca 3, Jan 18, 2023, trustedCertEntry, 
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ keytool -list -keystore secret-cacerts -storepass changeit | grep -i "MDT Issuing CA" || echo "CA not found"
CA not found
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ 
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ ./kubectl --kubeconfig ~/.kube/argo-dev.yaml -n argo-worldwiderevenue-dev \
>   get secret secrets-files -o jsonpath='{.data.cacerts}' | base64 -d > live-cacerts

AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ 
AWSReservedSSO_DefaultDeveloperRole_f2bbe1d53a7b5622:~/environment $ keytool -list -keystore live-cacerts -storepass changeit | head -n 20
Keystore type: JKS
Keystore provider: SUN

Your keystore contains 147 entries

c = at, o = e-commerce monitoring gmbh, cn = globaltrust 2020, Jan 18, 2023, trustedCertEntry, 
Certificate fingerprint (SHA-256): 9A:29:6A:51:82:D1:D4:51:A2:E3:7F:43:9B:74:DA:AF:A2:67:52:33:29:F9:0F:9A:0D:20:07:C3:34:E2:3C:9A
c = be, o = globalsign nv-sa, cn = globalsign root e46, Jan 18, 2023, trustedCertEntry, 
Certificate fingerprint (SHA-256): CB:B9:C4:4D:84:B8:04:3E:10:50:EA:31:A6:9F:51:49:55:D7:BF:D2:E2:C6:B4:93:01:01:9A:D6:1D:9F:50:58
c = be, o = globalsign nv-sa, cn = globalsign root r46, Jan 18, 2023, trustedCertEntry, 
Certificate fingerprint (SHA-256): 4F:A3:12:6D:8D:3A:11:D1:C4:85:5A:4F:80:7C:BA:D6:CF:91:9D:3A:5A:88:B0:3B:EA:2C:63:72:D9:3C:40:C9
c = be, o = globalsign nv-sa, ou = root ca, cn = globalsign root ca, Jan 18, 2023, trustedCertEntry, 
Certificate fingerprint (SHA-256): EB:D4:10:40:E4:BB:3E:C7:42:C9:E3:81:D3:1E:F2:A4:1A:48:B6:68:5C:96:E7:CE:F3:C1:DF:6C:D4:33:1C:99
c = bm, o = quovadis limited, cn = quovadis root ca 1 g3, Jan 18, 2023, trustedCertEntry, 
Certificate fingerprint (SHA-256): 8A:86:6F:D1:B2:76:B5:7E:57:8E:92:1C:65:82:8A:2B:ED:58:E9:F2:F2:88:05:41:34:B7:F1:F4:BF:C9:CC:74
c = bm, o = quovadis limited, cn = quovadis root ca 2, Jan 18, 2023, trustedCertEntry, 
Certificate fingerprint (SHA-256): 85:A0:DD:7D:D7:20:AD:B7:FF:05:F8:3D:54:2B:20:9D:C7:FF:45:28:F7:D6:77:B1:83:89:FE:A5:E5:C4:9E:86
c = bm, o = quovadis limited, cn = quovadis root ca 2 g3, Jan 18, 2023, trustedCertEntry, 
Certificate fingerprint (SHA-256): 8F:E4:FB:0A:F9:3A:4D:0D:67:DB:0B:EB:B2:3E:37:C7:1B:F3:25:DC:BC:DD:24:0E:A0:4D:AF:58:B4:7E:18:40
c = bm, o = quovadis limited, cn = quovadis root ca 3, Jan 18, 2023, trustedCertEntry,

This is the YAML:
apiVersion: "helm.toolkit.fluxcd.io/v2beta1"
kind: "HelmRelease"
metadata:
  name: "worldwiderevenue-dev"
spec:
  driftDetection:
    mode: "enabled"
    ignore:
    - paths: ["/spec/replicas"]
      target:
        kind: "Deployment"
  chart:
    spec:
      chart: "./charts/argo-app/1.0.9"
      sourceRef:
        kind: "GitRepository"
        name: "webdev-helm-git"
        namespace: "webdev-helm"
  interval: "0h10m0s"
  values:
    replicaCount: 2
    containerPort: 8081
    environment:
      deployNumber: "3687597"
      deployment: "DEV"
    mdtDefault:
      hostDomainName: "argo-dev.eks.mdtcloud.io"
    mdtApplication:
      imageDirectory: "case.artifacts.medtronic.com/bcp_web-docker-releases-virtual/com/medtronic/web/app/bcp_web/finance"
      name: "WorldWideRevenue"
      rOrS: "Release"
      version: "03.09.01"
      readOnlyRootFilesystem: true
      contextRoot: "worldwiderevenue"
      webstaticRoot: "worldwiderevenue-static"
      healthCheckEnabled: false
      
      command: ["sh", "-c"]
      args: ["sleep 36000"]

      horizontalAutoscaler:
        enabled: false
      runAs:
        userId: 1000
      appType: "java"
      containerFlavor: "app-tomcat"
      healthCheckDelaySeconds: 200
      quarantine: false
      useDefaultSessionTable: false
      volumes: []
      injectContrastYaml: true
    service:
      create: false
    resources:
      limits:
        cpu: "3000m"
        memory: "4000Mi"
      requests:
        cpu: "100m"
        memory: "1500Mi"



So, if you need more context, give me the questions, the new commands for check this issue
