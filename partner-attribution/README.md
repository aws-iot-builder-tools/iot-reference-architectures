# Partner attribution for IoT Core

This section of the repository is for partners, running IoT platforms on AWS, that interact with IoT Core using the HTTPS/REST APIs to publish data. The code samples here demonstrate how to use the AWS SDKs to publish data to IoT Core and have the data attributed to a partner's platform and (optionally) to uniquely identify devices.

<!-- toc -->

- [Partner attribution terminology](#partner-attribution-terminology)
- [Best practices for partner attribution](#best-practices-for-partner-attribution)
  * [General best practices](#general-best-practices)
  * [Platform value](#platform-value)
    + [Example platform values](#example-platform-values)
      - [Good](#good)
      - [Better](#better)
      - [Best](#best)
- [Samples](#samples)
  * [AWS SDK for Java v1](#aws-sdk-for-java-v1)
  * [AWS SDK for Java v2](#aws-sdk-for-java-v2)
  * [AWS SDK for JavaScript in Node.js](#aws-sdk-for-javascript-in-nodejs)
  * [AWS SDK for Python (Boto 3)](#aws-sdk-for-python-boto-3)

<!-- tocstop -->

## Partner attribution terminology

There are two values that a partner can include in requests to IoT Core that are used for attribution. Those values are referred to as `SDK` and `Platform`. These fields are identical to the partner attribution values with the same name in Amazon FreeRTOS.

The mechanism Amazon FreeRTOS uses for attribution is different from the mechanism the AWS SDKs support attribution. Amazon FreeRTOS supplies these two fields to IoT Core as a query string parameter at the end of the username field in the [MQTT CONNECT packet](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718028). AWS SDKs making REST calls have no notion of the MQTT CONNECT packet. Therefore the values are specified as individual headers in the HTTPS request.

## Best practices for partner attribution

### General best practices

Do:
- Allow your customers to turn this feature off
- Allow your customers to add data to these fields in the form of additional comma separated values

Do not:
- Include PII in the attribution fields

### Platform value

Platform values should identify the name of the partner company along with the name of the partner software. If the software has a version number that could be useful that value should be included as well.

#### Example platform values

NOTE: The only characters allowed in the partner name and partner software fields are `A-Z`, `a-z`, and `0-9`
NOTE: The only characters allowed in the partner software version field `A-Z`, `a-z`, `0-9`, and `.`

##### Good

- Partner name: `PartnerSoft`
- Partner software: N/A
- Partner software version: N/A

Platform value: `APN/1 PartnerSoft`

##### Better

- Partner name: `PartnerSoft`
- Partner software: `Managed IoT` -> `ManagedIoT` (no spaces allowed)
- Partner software version: N/A

Platform value: `APN/1 PartnerSoft,Managed IoT`

##### Best

- Partner name: `PartnerSoft`
- Partner software: `Managed IoT` -> `ManagedIoT` (no spaces allowed)
- Partner software version: `v1.2.1`

Platform value: `APN/1 PartnerSoft,ManagedIoT,v1.2.1`

## Samples 

All of the code samples can be launched by running the `./run-sample.sh` script in the sample's directory. The NodeJS and Python samples will install the necessary dependencies each time this script is run. The Java samples will build a fat JAR will all of the necessary dependencies each time the script is run.

### AWS SDK for Java v1

The [AWS SDK for Java v1 sample](./java-v1-sdk) is for partners using the [AWS SDK for Java v1](https://github.com/aws/aws-sdk-java). It is a Gradle project that has one class with a single static main method that publishes a message to IoT Core. Custom request headers are added before the request is sent and wire logging is enabled to validate that the request includes the values as expected.

### AWS SDK for Java v2

The [AWS SDK for Java v2 sample](./java-v2-sdk) is for partners using the [AWS SDK for Java v2](https://github.com/aws/aws-sdk-java-v2). It is a Gradle project that has one class with a single static main method that publishes a message to IoT Core. Custom request headers are added before the request is sent and wire logging is enabled to validate that the request includes the values as expected.

### AWS SDK for JavaScript in Node.js

The [AWS SDK for JavaScript in Node.js sample](./nodejs) is for partners using the [AWS SDK for JavaScript in Node.js](https://github.com/aws/aws-sdk-js). Custom request headers are added before the request is sent. The publish function is exported into a module so it is easier to use.

### AWS SDK for Python (Boto 3)

The [AWS SDK for Python sample](./python) is for partners using the [AWS SDK for Python (Boto 3)](https://github.com/boto/boto3). Custom request headers are added before the request is sent. There is an initialization function that needs to be called with the desired SDK and Platform values. After the initialization function is called Boto 3 calls can be made as normal and they will have the headers added automatically.
