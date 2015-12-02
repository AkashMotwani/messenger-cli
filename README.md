# messenger-cli
A Java 8 based [CLI](https://nl.wikipedia.org/wiki/Command-line-interface) for sending SMS messages. 

It is based on the [messenger-engine](https://github.com/dfranssen/messenger-engine) project and has following features:

```
Usage:
------
java -jar messenger-cli.jar --init -h   : for more directions to initialize your phone number with a MMP server.
java -jar messenger-cli.jar --send -h   : for more directions to send messages.
java -jar messenger-cli.jar --status -h : for more directions to get the status for a specific message id.
```

[Download](https://github.com/dfranssen/messenger-cli/releases/latest) the latest `messenger-cli.jar` and get started.

###Initialize a registration with an MMP server
```
Usage:
------
java -jar messenger-cli.jar --init [-s MMP_SERVER_URI] [-u USER_PHONE_NR] [-e USER_EMAIL]

-s: Optional MMP server uri, default is 'https://mobistar.msgsend.com/mmp/cp3'.
    This can also be set via an environment variable named 'DTV_MESSENGER_URL'

-u: Optional phone number for which a registration will be done.
    This can also be set via an environment variable named 'DTV_MESSENGER_MSISDN'.
    If missing, it will be requested as input.

-e: Optional email address of the user in case of 'replytoinbox' (future release).
    If missing, it will be requested as input.
```

###Sending SMS messages
```
Usage:
------
java -jar messenger-cli.jar [--send] [-s MMP_SERVER_URI] [-u USER_PHONE_NR] [-p PASSWORD] -m MESSAGE -t TO_RECEPIENTS

-s: Optional MMP server uri, default is 'https://mobistar.msgsend.com/mmp/cp3'.
    This can also be set via an environment variable named 'DTV_MESSENGER_URL'

-u: Optional phone number for which a registration will be done.
    This can also be set via an environment variable named 'DTV_MESSENGER_MSISDN'.
    If missing, it will be requested as input.

-p: Optional password (received during initialisation).
    This can also be set via an environment variable named 'DTV_MESSENGER_PASSWORD'.
    If missing, it will be requested as input.

-m: The message to be sent, encapsulated between double quotes

-t: Phone number(s) to which the message will be sent.
    Comma-seperated for multiple recipients. E.g. +32495123456,+32495654321
```

###Requesting status reports
```
Usage:
------
java -jar messenger-cli.jar --status [-s MMP_SERVER_URI] [-u USER_PHONE_NR] [-p PASSWORD] -id MESSAGE_IDS

-s:  Optional MMP server uri, default is 'https://mobistar.msgsend.com/mmp/cp3'.
     This can also be set via an environment variable named 'DTV_MESSENGER_URL'

-u:  Optional phone number for which a registration will be done.
     This can also be set via an environment variable named 'DTV_MESSENGER_MSISDN'.
     If missing, it will be requested as input.

-p:  Optional password (received during initialisation).
     This can also be set via an environment variable named 'DTV_MESSENGER_PASSWORD'.
     If missing, it will be requested as input.

-id: The message id to get the status for (returned by the send operation)
     Comma-seperated for multiple message ids. E.g. 123456,123457
```