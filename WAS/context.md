You already have enough evidence to stop treating this as “just a missing DB cert” problem.

The incident has at least two distinct states:

First state:
`Received fatal alert: handshake_failure`

Second state, after patching `cacerts`:
`No X509TrustManager implementation available`

Those are not the same failure. The second one is worse, and it strongly suggests the JVM in the pod is not able to initialize trust management from the truststore it is loading. That usually means one of these:

1. The app is not reading the truststore you think it is reading.
2. The mounted `cacerts` is valid as a file, but Java is interpreting it with the wrong store type.
3. A JVM security/provider issue was introduced with the new base image / Java 17 runtime.
4. The truststore file in the pod is corrupted, truncated, empty at runtime, or inaccessible.
5. Some JVM property is overriding truststore path/type/password.
6. The new image changed crypto defaults, disabled legacy algorithms/ciphers, or changed how Oracle JDBC/JSSE negotiates TLS.

The biggest blind spot in the current investigation is this:

You validated `secret-cacerts` and `live-cacerts` from your shell environment, but the failing app is running Java 17 inside the pod. Your shell output shows Corretto 11. That is not the same runtime, not the same filesystem, and not the same loaded truststore. So right now part of the investigation is happening in the wrong place.

What I think is most likely

The original problem was probably this:
the on-prem Oracle server presents a chain signed by `MDT Issuing CA 2-7`, and that CA is missing from the app truststore mounted into the pod.

But after your manual patch, the runtime moved into a different error:
`No X509TrustManager implementation available`

That typically points to truststore loading/configuration, not merely certificate absence.

So you may now have two problems:

* the Medtronic issuing CA is missing from the application truststore
* the patched/mounted truststore is not being consumed correctly by Java 17 in the new image

Questions you need answered now

Do not skip these, because they determine whether this is a cert-content issue or a JVM-loading issue.

1. In the failing pod, what is the exact Java version and vendor?
2. In the failing pod, what file actually exists at `/opt/java/openjdk/lib/security/cacerts`?
3. Is that file mounted from the secret, and does its checksum match the secret content?
4. Is Java using default truststore loading, or is something setting:

   * `javax.net.ssl.trustStore`
   * `javax.net.ssl.trustStoreType`
   * `javax.net.ssl.trustStorePassword`
   * `oracle.net.ssl_server_dn_match`
   * `oracle.net.authentication_services`
5. Is `javax.net.ssl.trustStoreType` set to `PKCS12` while the mounted file is actually `JKS`?
6. Did the base image move from Java 11 to Java 17 as part of the 2026.03.01 image?
7. Did the JDBC driver change at the same time, or is it still `ojdbc17-23.6.0.24.10` in both images?
8. Does the Oracle listener on `2484` require a specific TLS version or cipher suite that Java 17 no longer offers by default?
9. Is the server certificate chain complete from the listener side, or is the DB listener only sending the leaf certificate and relying on clients to already have the issuing CA?
10. Is there any custom `java.security` or FIPS-related setting in the new base image?

What I would challenge in the current reasoning

The statement “OpenSSL handshake works, therefore Java trust is the only issue” is too weak.

Why:

* OpenSSL success with `verify error:num=20/21` only proves the server speaks TLS and presents a certificate.
* It does not prove Java 17 will accept the chain.
* It does not prove Oracle JDBC and JSSE agree on protocol/cipher support.
* It does not prove the pod is loading the intended truststore.

Also, your leader’s advice about the injected `cacerts` is directionally right, but incomplete. If that mounted truststore is wrong type, corrupt, or overridden by JVM flags, importing the missing CA will still not fix the second error.

What to do next

Since you already disabled health checks with `sleep 36000`, do the investigation inside the exact app pod.

Pick one running debug pod:

```bash
./kubectl --kubeconfig ~/.kube/argo-dev.yaml -n argo-worldwiderevenue-dev get pods -o wide
./kubectl --kubeconfig ~/.kube/argo-dev.yaml -n argo-worldwiderevenue-dev exec -it <POD_NAME> -c argo-app -- sh
```

Then run these commands inside the pod.

1. Confirm the real runtime

```bash
java -version
which java
readlink -f "$(which java)"
```

2. Dump JVM security/trust settings

```bash
env | sort | grep -E 'JAVA|JDK|JRE|CATALINA|TRUST|SSL|TLS|ORACLE'
java -XshowSettings:properties -version 2>&1 | grep -E 'java.home|java.version|javax.net.ssl|jdk.tls|security'
```

3. Inspect the mounted truststore path

```bash
ls -l /opt/java/openjdk/lib/security/cacerts
file /opt/java/openjdk/lib/security/cacerts
sha256sum /opt/java/openjdk/lib/security/cacerts
du -h /opt/java/openjdk/lib/security/cacerts
```

4. Check whether Java can actually read that truststore

```bash
keytool -list -keystore /opt/java/openjdk/lib/security/cacerts -storepass changeit | head -n 20
keytool -list -keystore /opt/java/openjdk/lib/security/cacerts -storepass changeit | grep -i "MDT Issuing CA" || echo "CA not found"
```

5. Explicitly test both common store types
   This is critical.

```bash
keytool -list -storetype JKS -keystore /opt/java/openjdk/lib/security/cacerts -storepass changeit | head
keytool -list -storetype PKCS12 -keystore /opt/java/openjdk/lib/security/cacerts -storepass changeit | head
```

If one works and the other fails, and the JVM is configured with the wrong type, you found the problem.

6. Check whether truststore settings are being forced somewhere

```bash
grep -R "trustStore" /usr/local/tomcat /opt/java /etc 2>/dev/null
grep -R "javax.net.ssl" /usr/local/tomcat /opt/java /etc 2>/dev/null
grep -R "jdk.tls" /opt/java /etc /usr/local/tomcat 2>/dev/null
```

7. Verify the cert chain from inside the pod
   Do not rely only on CloudShell.

```bash
openssl s_client -connect mspldb515.corp.medtronic.com:2484 -servername mspldb515.corp.medtronic.com -tls1_2 -showcerts </dev/null
```

If possible, save the presented certs:

```bash
openssl s_client -connect mspldb515.corp.medtronic.com:2484 -servername mspldb515.corp.medtronic.com -tls1_2 -showcerts </dev/null 2>/dev/null | awk '/BEGIN CERTIFICATE/,/END CERTIFICATE/{print}' > /tmp/db-chain.pem
```

Then inspect:

```bash
awk 'split_after==1{n++;split_after=0} /-----END CERTIFICATE-----/{split_after=1} {print > ("/tmp/cert-" n ".pem")}' < /tmp/db-chain.pem
for f in /tmp/cert-*.pem; do echo "==== $f ===="; openssl x509 -in "$f" -noout -subject -issuer -dates -fingerprint -sha256; done
```

8. Compare pod truststore with secret content checksum
   Outside pod:

```bash
./kubectl --kubeconfig ~/.kube/argo-dev.yaml -n argo-worldwiderevenue-dev get secret secrets-files -o jsonpath='{.data.cacerts}' | base64 -d > /tmp/live-cacerts
sha256sum /tmp/live-cacerts
```

Inside pod:

```bash
sha256sum /opt/java/openjdk/lib/security/cacerts
```

If hashes differ, the secret is not what the pod is actually using.

9. Run a forced Java SSL debug test inside the pod
   This is one of the most useful commands.

```bash
java \
  -Djavax.net.debug=ssl,handshake,trustmanager \
  -Djavax.net.ssl.trustStore=/opt/java/openjdk/lib/security/cacerts \
  -Djavax.net.ssl.trustStorePassword=changeit \
  -Djavax.net.ssl.trustStoreType=JKS \
  -cp /usr/local/lib/DBCheck.jar:/usr/local/tomcat/lib/ojdbc17-23.6.0.24.10.jar \
  DBCheck
```

Then repeat with `PKCS12`:

```bash
java \
  -Djavax.net.debug=ssl,handshake,trustmanager \
  -Djavax.net.ssl.trustStore=/opt/java/openjdk/lib/security/cacerts \
  -Djavax.net.ssl.trustStorePassword=changeit \
  -Djavax.net.ssl.trustStoreType=PKCS12 \
  -cp /usr/local/lib/DBCheck.jar:/usr/local/tomcat/lib/ojdbc17-23.6.0.24.10.jar \
  DBCheck
```

That will tell you whether the store type mismatch is the issue.

10. Test TLS protocol compatibility explicitly
    Because this may also be a Java 17 crypto-default issue.

```bash
java \
  -Djavax.net.debug=ssl,handshake \
  -Djdk.tls.client.protocols=TLSv1.2 \
  -Doracle.net.ssl_version=1.2 \
  -Djavax.net.ssl.trustStore=/opt/java/openjdk/lib/security/cacerts \
  -Djavax.net.ssl.trustStorePassword=changeit \
  -Djavax.net.ssl.trustStoreType=JKS \
  -cp /usr/local/lib/DBCheck.jar:/usr/local/tomcat/lib/ojdbc17-23.6.0.24.10.jar \
  DBCheck
```

11. Check Java security providers
    Because `No X509TrustManager implementation available` can also happen if the security provider stack is broken.

```bash
jshell <<'EOF'
import java.security.*;
for (Provider p : Security.getProviders()) System.out.println(p.getName()+" "+p.getVersionStr());
EOF
```

Or if `jshell` is unavailable:

```bash
java -XshowSettings:properties -version 2>&1 | grep security
```

What results would mean

If this command fails:

```bash
keytool -list -keystore /opt/java/openjdk/lib/security/cacerts -storepass changeit
```

inside the pod, then the mounted truststore is bad or unreadable. Stop focusing on the Oracle server.

If `keytool` works with `-storetype JKS` but Java only works when you force `-Djavax.net.ssl.trustStoreType=JKS`, then the new image or some runtime property is defaulting to the wrong type.

If the truststore is readable, but `MDT Issuing CA 2-7` is absent, then the original failure is expected and you need to import the missing Medtronic CA/intermediate into the app `cacerts` source that manage-namespace uses.

If the CA is present and Java still fails only on the new image, then the likely next cause is TLS/cipher/protocol compatibility between Java 17 + Oracle JDBC and the on-prem Oracle TCPS listener.

Commands to import the missing CA, if needed

Only do this after you extract the proper CA cert from a trusted source, not from guesswork.

```bash
keytool -importcert \
  -alias mdt-issuing-ca-2-7 \
  -file MDT_Issuing_CA_2_7.pem \
  -keystore cacerts \
  -storepass changeit \
  -noprompt
```

Then verify:

```bash
keytool -list -keystore cacerts -storepass changeit | grep -i "mdt-issuing-ca-2-7"
```

A more direct hypothesis tree

Most likely:

* the injected truststore does not contain `MDT Issuing CA 2-7`
* and/or the new runtime is not loading the mounted truststore correctly

Less likely but still plausible:

* Java 17 rejects the Oracle listener negotiation unless forced to TLS 1.2
* cipher suite overlap changed in the new image
* the DB server is not sending the full chain and older behavior masked it

What I would tell the team right now

You can say this plainly:

The evidence no longer supports a single-cause explanation. The original `handshake_failure` is consistent with the Medtronic issuing CA missing from the namespace-injected truststore. However, after patching, the runtime changed to `No X509TrustManager implementation available`, which points to truststore loading/format/runtime configuration inside the Java 17 pod, not just certificate absence. The next validation must be executed from inside the exact app container, checking the actual mounted truststore at `/opt/java/openjdk/lib/security/cacerts`, its checksum, readable type, JVM truststore properties, and a forced SSL debug run with explicit `trustStoreType` and TLS 1.2.

Your strongest next move is not another generic cert patch. It is proving, inside the pod, exactly which truststore Java 17 is loading and whether it can build an `X509TrustManager` from it.

If you want, I can turn this into a clean incident update for your team channel or ServiceNow ticket.
