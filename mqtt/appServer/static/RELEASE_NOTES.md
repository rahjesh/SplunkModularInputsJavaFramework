1.2.1
-----
* added trial key functionality

1.2
-----
* docs updated

1.1
-----
* minor manager xml ui tweak for 7.1

1.0
-----
* Added an activation key requirement , visit http://www.baboonbones.com/#activation  to obtain a non-expiring key
* Docs updated
* Splunk 7.1 compatible

0.9.1
---
* Fixed subscription loop

0.9
---
* Minor HEC tweaks

0.8
---
* Added support to optional output to Splunk via a HEC (HTTP Event Collector) endpoint

0.7
---
* Added a message handler (BodyOnlyMessageHandler) that just outputs the mqtt body

0.6
----
* Enabled TLS1.2 support by default.
* Made the  core Modular Input Framework compatible with latest Splunk Java SDK
* Please use a Java Runtime version 7+
* If you need to use SSLv3 , you can turn this on in bin/mqtt.py  
SECURE_TRANSPORT = "tls"  
#SECURE_TRANSPORT = "ssl"  

0.5
-----
* Initial beta release
