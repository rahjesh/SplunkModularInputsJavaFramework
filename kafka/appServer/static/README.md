## Splunk Kafka Messaging Modular Input v1.3.2

## Overview

This is a Splunk Modular Input Add-On for indexing messages from a Kafka broker or cluster of brokers that are managed by Zookeeper.
Kafka version 0.8.1.1 is used for the consumer and the testing of this Modular Input.

## What is Kafka ?

https://kafka.apache.org/

## Dependencies

* Splunk 5.0+
* Java Runtime 1.7+
* Supported on Windows, Linux, MacOS, Solaris, FreeBSD, HP-UX, AIX
* Kafka 0.8+

## Setup

* Optionally set your JAVA_HOME environment variable to the root directory of your JRE installation.If you don't set this , the input will look for a default installed java executable on the path.
* Untar the release to your $SPLUNK_HOME/etc/apps directory
* Restart Splunk
* If you are using a Splunk UI Browse to `Settings -> Data Inputs -> Kafka Messaging` to add a new Input stanza via the UI
* If you are not using a Splunk UI (ie: you are running on a Universal Forwarder) , you need to add a stanza to inputs.conf directly as per the specification in `README/inputs.conf.spec`. The `inputs.conf` file should be placed in a `local` directory under an App or User context.


## Activation Key

You require an activation key to use this App. Visit http://www.baboonbones.com/#activation  to obtain a non-expiring key


## Logging

Any log entries/errors will get written to $SPLUNK_HOME/var/log/splunk/splunkd.log

These are also searchable in Splunk : `index=_internal error kafka.py`

## JVM Heap Size

The default heap maximum is 64MB.
If you require a larger heap, then you can alter this in $SPLUNK_HOME/etc/apps/kafka_ta/bin/kafka.py on line 95

## JVM System Properties

You can declare custom JVM System Properties when setting up new input stanzas.
Note : these JVM System Properties will apply to the entire JVM context and all stanzas you have setup

## Customized Message Handling

The way in which the Modular Input processes the received Kafka messages is enitrely pluggable with custom implementations should you wish.

To do this you code an implementation of the com.splunk.modinput.kafka.AbstractMessageHandler class and jar it up.

Ensure that the necessary jars are in the $SPLUNK_HOME/etc/apps/kafka_ta/bin/lib directory.

If you don't need a custom handler then the default handler com.splunk.modinput.kafka.DefaultMessageHandler will be used.

This handler simply trys to convert the received byte array into a textual string for indexing in Splunk.

Code examples are on GitHub : https://github.com/damiendallimore/SplunkModularInputsJavaFramework/tree/master/kafka/src/com/splunk/modinput/kafka

## Troubleshooting

* JAVA_HOME environment variable is set or "java" is on the PATH for the user's environment you are running Splunk as
* You are using Splunk 5+
* You are using a 1.7+ Java Runtime
* You are targetting Kafka version 0.8+
* You are running on a supported operating system
* Look for any errors in $SPLUNK_HOME/var/log/splunk/splunkd.log
* Run this command as the same user that you are running Splunk as and observe console output : "$SPLUNK_HOME/bin/splunk cmd python ../etc/apps/kafka_ta/bin/kafka.py --scheme" 

## Contact

This project was initiated by Damien Dallimore , damien@baboonbones.com

