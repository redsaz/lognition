# Lognition

Efficiently import, store, view, and export JMeter test results.


## Building and Running

To build Lognition, you need at least Java 11. Lognition comes with Maven Wrapper v3.8.1, but if you want to use your own Maven make sure it is at least that version. Once you clone the repo:

    cd lognition
    ./mvnw clean install
    cd lognition-quarkus
    ../mvnw quarkus:dev

Once Lognition has started up, you can open http://localhost:8080 and start using Lognition.

### Configuration

Lognition uses [Quarkus](https://quarkus.io/), which allows [several different sources](https://quarkus.io/guides/config-reference#configuration-sources) for configuration. We'll focus on the following sources, in decending priority:

* [System Properties](https://quarkus.io/guides/config-reference#system-properties) are command-line options starting with `-D`.
  * Lognition jar example: `java -Dexample.item=Hello -jar lognition.jar`
  * Quarkus dev mode: `../mvnw quarkus:dev -Dexample.item=Hello`
* [Environment Variables](https://quarkus.io/guides/config-reference#environment-variables) must be upper-case alphanumeric and underscores using [conversion rules](https://github.com/eclipse/microprofile-config/blob/master/spec/src/main/asciidoc/configsources.asciidoc#default-configsources). They can be set in your `.bashrc` file, in a script that runs the app, or listed just before the command, shown below:
  * Lognition jar example: `EXAMPLE_ITEM1=Hello EXAMPLE_ITEM2=World java -jar lognition.jar`
  * Quarkus dev mode: `EXAMPLE_ITEM1=Hello EXAMPLE_ITEM2=World ../mvnw quarkust:dev`
* [.env File](https://quarkus.io/guides/config-reference#env-file) in the working directory. The contents of the file follow the same naming conventions as environment variables, above.

There are [quite a few config options](https://quarkus.io/guides/all-config) for Lognition that are provided through Quarkus, in addition to some Lognition specific options. The more common are listed below.

#### SSL

If using a pair of cert+key _*PEM files*_ for [SSL](https://quarkus.io/guides/http-reference#ssl):

As system properties: `java -Dquarkus.http.ssl.certificate.file=/path/to/certificate -Dquarkus.http.ssl.certificate.key-file=/path/to/key -jar lognition.jar`

Or in .env file:

    quarkus.http.ssl.certificate.file=/path/to/certificate
    quarkus.http.ssl.certificate.key-file=/path/to/key

Otherwise, if using _*PKCS12*_ (other keystore formats are also supported):

As system properties: `java -Dquarkus.http.ssl.certificate.key-store-file=/path/to/store.p12 -Dquarkus.http.ssl.certificate.key-store-password=example -jar lognition.jar`

Or in .env file:

    quarkus.http.ssl.certificate.key-store-file=/path/to/store.p12
    quarkus.http.ssl.certificate.key-store-password=example

Either way, once configured and Lognition is started, visit https://localhost:8443 to make sure it works. (If you use https with port 8080, you might see an error in Firefox like `Error code: SSL_ERROR_RX_RECORD_TOO_LONG`
