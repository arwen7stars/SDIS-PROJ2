# SDIS-Project2

GENERATE KEYSTORE
$ keytool -genkey -alias signFiles -keystore mykeystore

FILE INFO
CN=SDIS 2P, OU=FEUP, O=FEUP, L=Porto, ST=Porto, C=pt

CLIENT
System.setProperty("javax.net.ssl.trustStore","mykeystore");
System.setProperty("javax.net.ssl.trustStorePassword”,”1234567890”);

SERVER
System.setProperty("javax.net.ssl.keyStore","mykeystore");
System.setProperty("javax.net.ssl.keyStorePassword","1234567890");
