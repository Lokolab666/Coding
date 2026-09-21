# Developer Environment Setup #

Reference the table below to determine which tools are needed.  Following the table are the steps for each tool.  In general, most tools can be found [here](https://case.artifacts.medtronic.com/artifactory/webapp/#/artifacts/browse/tree/General/bcp-frameworks-virtual/mdt-web-tools).
<BR>
<BR>


| Tool to install if needed | Grails 2.x & OpenID | Grails 2.x | Grails 1.x | non-Grails |
|---|:---:|:---:|:---:|:---:|
|JDK 1.6| |X|X|X|
|JDK 1.8|X|Y| |Y|
|Ant|X|X|X|X|
|Grails 1.3.5| | |X| |
|Grails 2.4.3|X|X| | |
|Apache|X|X|X|X|
|IntelliJ|X|X|X|X|
|WebLogic| | | |X|
|Toad/SQL Developer|X|X|X|X|


## Installation overview for each of the tools listed above.  Only install the tools needed for the type of project(s) you are working on:



1. **Grails:**
    * Grails 2.4.3 - download from https://case.artifacts.medtronic.com/artifactory/webapp/#/artifacts/browse/tree/General/bcp-frameworks-virtual/mdt-web-tools/grails-2.4.3.zip
    * Grails 1.3.5 - download from: https://case.artifacts.medtronic.com/artifactory/webapp/#/artifacts/browse/tree/General/bcp-frameworks-virtual/mdt-web-tools/grails-1.3.5.zip
    * Recommended install path, unzip the downloaded file to:
        * Grails 2.4.3 - ```C:\Users\<username>\Development\grails\2.4.3```
        * Grails 1.3.5 - ```C:\Users\<username>\Development\grails\1.3.5```
    * Set GRAILS_HOME system environment variable:
        * ```GRAILS_HOME=C:\Users\<username>\Development\grails\2.4.3```
1. **JDK:**
    * JDK 1.8 - download from:  https://case.artifacts.medtronic.com/artifactory/webapp/#/artifacts/browse/tree/General/bcp-frameworks-virtual/mdt-web-tools/jdk-8u73-windows-x64.exe
    * JDK 1.6 - download from:  https://case.artifacts.medtronic.com/artifactory/webapp/#/artifacts/browse/tree/General/bcp-frameworks-virtual/mdt-web-tools/jdk-6u211-windows-x64.exe
    * Recommended install path:
        * ```C:\Users\<username>\Development\jdk_xxx```
    * Set system environment variable:
        * ```JAVA_HOME=C:\Users\<username>\Development\jdk_xxx```
    * [Add Medtronic CAs to the Java Keystore](mdt-ca-to-keystore.md)
1. **IntelliJ IDE:**
    * Download and install IntelliJ IDEA version 2020.3.4 from: [https://www.jetbrains.com/shop/download/II/2020300](https://www.jetbrains.com/shop/download/II/2020300)
    * Email [Ash Montebello](mailto:ash.l.montebello@medtronic.com) for license. There are limited licenses available! You may choose to use other IDEs such as VSCode or Eclipse if you would like.
1. **Apache 2.0.63:**
    * Download from: https://case.artifacts.medtronic.com/artifactory/webapp/#/artifacts/browse/tree/General/bcp-frameworks-virtual/mdt-web-tools/apache_2.0.63-win32-x86-no_ssl.msi
    * Recommended install path: C:\Apache
    * [Steps to setup Apache on local dev environment](apache/README.md)
1. **Ant:**
    * Required version is Ant 1.6.5
    * Download from: https://case.artifacts.medtronic.com/artifactory/webapp/#/artifacts/browse/tree/General/bcp-frameworks-virtual/mdt-web-tools/apache-ant-1.6.5-bin.zip
    * Recommended install path, unzip the downloaded file to: ```C:\Users\<username>\Development\ant\1.6.5```
    * Set system environment variable:
        * ```ANT_HOME=C:\Users\<username>\Development\ant\1.6.5```
    * for non-grails project, download ivy-2.1.0-rc2.jar from https://case.artifacts.medtronic.com:443/artifactory/bcp-frameworks-virtual/mdt-web-tools/ivy-2.1.0-rc2.jar
      and install the Ivy jar in the Ant lib directory
1. **Git client:**
    * Install Git from https://git-scm.com/downloads for Intellij use (jhm)
    * Update GIT config: ```git config --global http.sslVerify false```
    * Better yet, with Git for Windows 2.14 or newer, you can choose the new SChannel mechanism during the installation of Git for Windows. You can also update an existing installation to use SChannel by running:
        ```git config --global http.sslBackend schannel```
1. Install **Toad** (from Software center of Medtronic) or **Oracle SQL developer** (open source)
1. **Install database client for Oracle:**
    * Install Toad from Medtronic "Software Center"
    * OR download and install open source Oracle SQL developer from: [https://www.oracle.com/tools/downloads/sqldev-downloads.html](https://www.oracle.com/tools/downloads/sqldev-downloads.html)
1. **Update Path system environment variable:**
   * Add %ANT_HOME%\bin
   * Add %JAVA_HOME%\bin
   * Add %GRAILS_HOME%\bin
1. **WebLogic:**
    * Install weblogic 10.3.6 (Java 1.6.0_29 is installed as the default from WebLogic - based upon project, ensure your default is 1.8 or 1.6.9_211 as indicated above)
    * Download from : https://case.artifacts.medtronic.com/artifactory/webapp/#/artifacts/browse/tree/General/bcp-frameworks-virtual/mdt-web-tools/wls1036_win32.exe
    * Follow the prompts and perform a default installation - check the "I wish to remain uninformed" checkbox at the bottom.
    * In the Quick Start window, click _Getting Started with WebLogic Server 10.3.6_ and create a new domain using the default settings.
1. **Contrast IntelliJ Plugin:**
    * Follow setup and configuration instructions found here: https://docs.contrastsecurity.com/tools-ide.html#intell
        * Recommended install path: ```C:\Users\<username>\Development\contrast-intellij-plugin```
        * Username in IntelliJ settings is your Medtronic email address
        * Note: To find your keys, go to the user menu > Your Account > Profile page in the Contrast Web UI
    * Add Contrast Java Agent
        * Download the agent per the instructions here: https://docs.contrastsecurity.com/en/java.html#java-overview
        * Recommended install path: ```C:\Users\<username>\Development```
        * Configure application servers in IntelliJ (Grails and/or WebLogic)
            * VM Options:
            ```
            -Dcontrast.protect.enable=false
            -javaagent:C:\Users\<username>\Development\contrast.jar
            -noverify
            ```
1. **OJDBC jar:**
    * If Weblogic server is installed in your environment it includes ojdbc6 jar file
    * If Weblogic server is NOT installed in your environment, or you are using JDK8, download jar file from:
        * JDK 1.6: https://case.artifacts.medtronic.com/artifactory/bcp-frameworks-virtual/com/oracle/ojdbc6/11.2.0.4/ojdbc6-11.2.0.4.jar
        * JDK 1.8: https://case.artifacts.medtronic.com/artifactory/bcp-frameworks-virtual/com/oracle/ojdbc/ojdbc8/19.3.0.0/ojdbc8-19.3.0.0.jar
        * Recommended install path: ```C:\Users\<username>\Development```
    * Configure IntelliJ
        * Add the jar file to your global libraries (default project structure)

<BR>
## NOTE for Grails 2.4.3 on JDK 1.8 update 73:

With these tools installed you may run the Grails app from WebLogic without issue. If you wish to use "grails run-app" to run a Grails 2.4.3 app locally with JDK 1.8 update 73, you will need to take these additional steps:

* You may see an exception when trying to run the app in this case:  ```caused by: java.lang.illegalargumentexception: can not copy a non-root field Incompatible JVM```
* Replace the ```springloaded-1.2.1-release.jar``` with ```springloaded-1.2.4.jar``` to fix this issue
* Navigate to your ```GRAILS_HOME/lib/org.springframework/springloaded/jars``` directory
* Download https://repo.spring.io/release/org/springframework/springloaded/1.2.4.RELEASE/springloaded-1.2.4.RELEASE.jar and rename the file to ```springloaded-1.2.4.jar``` in that location
