1.3.1
-----
* added trial key functionality

1.3
-----
* docs updated

1.2
-----
* minor manager xml ui tweak for 7.1

1.1
-----
* Added an activation key requirement , visit http://www.baboonbones.com/#activation  to obtain a non-expiring key
* Docs updated
* Splunk 7.1 compatible

1.0
---
* Minor HEC tweaks

0.9
---

* Adjusted pre fetch logic

0.8
---
* Added support to optional output to Splunk via a HEC (HTTP Event Collector) endpoint

0.7
---
* Enabled TLS1.2 support by default.
* Made the  core Modular Input Framework compatible with latest Splunk Java SDK
* Please use a Java Runtime version 7+
* If you need to use SSLv3 , you can turn this on in bin/mq.py  
SECURE_TRANSPORT = "tls"  
#SECURE_TRANSPORT = "ssl"  

0.6
----
* Added configurable basic qos parameter

0.5
-----
* Initial beta release


