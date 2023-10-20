#!/bin/sh
keytool -importkeystore -deststorepass changeit -destkeypass changeit -destkeystore $JAVA_HOME/lib/security/cacerts -srckeystore /CERTS/ConfigServerSslKey.p12 -srcstoretype PKCS12 -srcstorepass $ConfigServerSslKey -noprompt
java -jar /usr/local/lib/app.jar