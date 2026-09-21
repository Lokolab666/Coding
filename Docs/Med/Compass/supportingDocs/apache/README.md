# Details for setting up Apache on local dev environment

* Modify the httpd.conf file with admin rights to include the following:

  * Section 1: Global Environment

    * Load modules: include the following modules, meaning uncomment or remove the # at the line start if they are commented out:
    ```
    LoadModule headers_module modules/mod_headers.so
    LoadModule proxy_module modules/mod_proxy.so
    LoadModule proxy_http_module modules/mod_proxy_http.so
    ```

  * Section 2: 'Main' server configuration
    * Update ServerName and port:
      ```
      ServerName localhost:80
      ```

    * Aliases

      Add any aliases (in the Aliases section of the file) needed for your application web-static. Example:
      ```
      Alias /ist-static "C:/<your project path>/InterviewMatrix/web-static"
      ```

  * Section 3: Virtual Hosts
    * Proxies

      At the end of this section, add proxy needed for your application.
      In this example, /ist is the application context root:
      ```
      <IfModule mod_proxy.c>
          ProxyPass /ist http://localhost:8080/ist
          ProxyPassReverse /ist http://localhost:8080/ist
        </IfModule>
      ```
    * Request Headers
      At the end of the file, add any request headers needed for your application to simulate logged-in user.
      Examples:
      ```
      RequestHeader add mdtdirectorykeyint "<user directory key>"
      RequestHeader add acrole "<user role>"
      ```

* Restart the Apache service in Services from an elevated command prompt. If the service does not start it means there is an error in your file.


* Additional configuration, if using Weblogic Server:
    * Copy mod_wl_20.so from your weblogic install to your apache install:
      ```
      from <weblogic dir>/server/plugin/win/32
      to <apache dir>/modules
      ```
    * Add this to your Apache config load modules section:
      ```
      LoadModule weblogic_module modules/mod_wl_20.so
      ```
    * Add and use this instead of proxies in previous section
      ```
      <IfModule mod_weblogic.c>
        WebLogicHost localhost
        WebLogicPort 7001
        WLIOTimeoutSecs 1800
        <Location /ist>
          SetHandler weblogic-handler
        </Location>
      </IfModule>
      ```
      * If clustered, replace WeblogicHost and WeblogicPort lines with the following:
      ```
      WeblogicCluster localhost:7001,localhost:7501
      ```
