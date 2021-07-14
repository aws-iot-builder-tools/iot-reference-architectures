# Partner attribution for IoT Core

This section of the repository is for partners, running IoT platforms on AWS, that interact with IoT Core.

Partners using the HTTPS/REST APIs to publish data must conform to the "APN/1" format.

Partners using MQTT to publish data must use the "APN/2" prefix instead of "APN/1". There are no restrictions on the "APN/2" format but it is best to match the "APN/1" format if possible for consistency. See [Attribution in systems using MQTT](#Attribution-in-systems-using-MQTT) for more information. That is the only section of this document that covers MQTT. AWS SDKs making REST calls have no notion of the MQTT CONNECT packet. Therefore the values are specified as individual headers in the HTTPS request.

The code samples here demonstrate how to use the AWS SDKs to publish data to IoT Core and have the data attributed to a partner's platform and (optionally) to uniquely identify devices.

<!-- toc -->

- [Partner attribution terminology](#partner-attribution-terminology)
- [Best practices for partner attribution](#best-practices-for-partner-attribution)
  * [Requirements](#requirements)
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
  * [AWS SDK for Golang](#aws-sdk-for-golang)
- [Attribution in systems using MQTT](#attribution-in-systems-using-mqtt)

<!-- tocstop -->

## Partner attribution terminology

There are two values that a partner can include in requests to IoT Core that are used for attribution. Those values are referred to as `SDK` and `Platform`. These fields are identical to the partner attribution values with the same name in Amazon FreeRTOS.

NOTE: The mechanism Amazon FreeRTOS uses for attribution is via MQTT. It is different from the mechanism the AWS SDKs support attribution. See [Attribution in systems using MQTT](#Attribution-in-systems-using-MQTT) for the differences.

## Best practices for partner attribution

### Requirements

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

### AWS SDK for Golang

The [AWS SDK for Go sample](./golang) is for partners using the [AWS SDK for Go](https://github.com/aws/aws-sdk-go). Custom request headers are added before the request is sent in the main function of the sample.

## Attribution in systems using MQTT

Attribution in systems using MQTT is provided as a query string parameter at the end of the username field in the [MQTT CONNECT packet](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718028).

From the above example where the platform value is `APN/1 PartnerSoft,ManagedIoT,v1.2.1` it would need to be changed to `APN/2 PartnerSoft,ManagedIoT,v1.2.1`. Then it would be added as a query string parameter at the end of the username field in the MQTT CONNECT packet.

If the username field is blank the username field would change to `?Platform=APN/2 PartnerSoft,ManagedIoT,v1.2.1`.

If the username field contains some data but no query string parameters then this value would be appended to the existing username field `?Platform=APN/2 PartnerSoft,ManagedIoT,v1.2.1`.

If the username field contains some data and also one or more string parameters then this value would be appended to the existing username field `&Platform=APN/2 PartnerSoft,ManagedIoT,v1.2.1`.

