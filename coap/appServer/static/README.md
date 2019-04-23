## Splunk COAP (Constrained Application Protocol) Modular Input v1.1.1

## Overview

This is a Splunk Modular Input Add-On for indexing messages from a COAP Server.

## What is COAP ?

http://en.wikipedia.org/wiki/Constrained_Application_Protocol

## COAP and MQTT

http://eclipse.org/community/eclipse_newsletter/2014/february/article2.php

## Implementation

This Modular Input utilizes the Californium Java client library version 1.0 , https://eclipse.org/californium/

## Dependencies

* Splunk 5.0+
* Java Runtime 1.7+
* Supported on Windows, Linux, MacOS, Solaris, FreeBSD, HP-UX, AIX

## Setup

* Optionally set your JAVA_HOME environment variable to the root directory of your JRE installation.If you don't set this , the input will look for a default installed java executable on the path.
* Untar the release to your $SPLUNK_HOME/etc/apps directory
* Restart Splunk
* If you are using a Splunk UI Browse to `Settings -> Data Inputs -> COAP` to add a new Input stanza via the UI
* If you are not using a Splunk UI (ie: you are running on a Universal Forwarder) , you need to add a stanza to inputs.conf directly as per the specification in `README/inputs.conf.spec`. The `inputs.conf` file should be placed in a `local` directory under an App or User context.


## Activation Key

You require an activation key to use this App. Visit http://www.baboonbones.com/#activation  to obtain a non-expiring key


## Logging

Any log entries/errors will get written to $SPLUNK_HOME/var/log/splunk/splunkd.log

These are also searchable in Splunk : `index=_internal error coap.py`

## JVM Heap Size

The default heap maximum is 64MB.
If you require a larger heap, then you can alter this in $SPLUNK_HOME/etc/apps/coap_ta/bin/coap.py on line 95

## JVM System Properties

You can declare custom JVM System Properties when setting up new input stanzas.
Note : these JVM System Properties will apply to the entire JVM context and all stanzas you have setup

## Customized Message Handling

The way in which the Modular Input processes the received COAP messages is enitrely pluggable with custom implementations should you wish.

To do this you code an implementation of the com.splunk.modinput.coap.AbstractMessageHandler class and jar it up.

Ensure that the necessary jars are in the $SPLUNK_HOME/etc/apps/coap_ta/bin/lib directory.

If you don't need a custom handler then the default handler com.splunk.modinput.coap.DefaultMessageHandler will be used.

Code examples are on GitHub : https://github.com/damiendallimore/SplunkModularInputsJavaFramework/tree/master/coap/src/com/splunk/modinput/coap

## Troubleshooting

* JAVA_HOME environment variable is set or "java" is on the PATH for the user's environment you are running Splunk as
* You are using Splunk 5+
* You are using a 1.7+ Java Runtime
* You are running on a supported operating system
* Look for any errors in $SPLUNK_HOME/var/log/splunk/splunkd.log
* Run this command as the same user that you are running Splunk as and observe console output : "$SPLUNK_HOME/bin/splunk cmd python ../etc/apps/coap_ta/bin/coap.py --scheme" 

## Contact

This project was initiated by Damien Dallimore , damien@baboonbones.com

