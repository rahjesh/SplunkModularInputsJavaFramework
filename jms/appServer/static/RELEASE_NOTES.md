1.7.2
-----
* updated docs

1.7.1
-----
* added trial key functionality

1.7
-----
* docs updated

1.6.1
-----
* minor manager xml ui tweak for 7.1

1.6
-----
* Added an activation key requirement , visit http://www.baboonbones.com/#activation  to obtain a non-expiring key
* Docs updated
* Splunk 7.1 compatible

1.5.1
-----
* Added a new message handler that just dumps the message body :   
com.splunk.modinput.jms.custom.handler.BodyOnlyMessageHandler

1.5
---
* Minor HEC tweaks

1.4
---
* Added support to optional output to Splunk via a HEC (HTTP Event Collector) endpoint

1.3.9
-----
* Added more verbose INFO level logging

1.3.8
-----
* Enabled TLS1.2 support by default.
* Made the  core Modular Input Framework compatible with latest Splunk Java SDK
* Please use a Java Runtime version 7+
* If you need to use SSLv3 , you can turn this on in bin/mq.py  
SECURE_TRANSPORT = "tls"  
#SECURE_TRANSPORT = "ssl"  

1.3.7
-----
* Changed the point in the code where client ID is set for durable topic subscriptions

1.3.6
-----
* Added a LocalConnectionFactory for ActiveMQ

1.3.5
-----
* Added the ability to declare custom JVM System Properties in your stanzas
