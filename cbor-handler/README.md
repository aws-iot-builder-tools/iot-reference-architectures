# CBOR handler

<!-- toc -->

- [What is this architecture?](#what-is-this-architecture)
- [What is CBOR?](#what-is-cbor)
- [Where is this architecture applicable?](#where-is-this-architecture-applicable)
- [How do I launch it?](#how-do-i-launch-it)
- [I ran into an issue with cdk deploy, what do I do?](#i-ran-into-an-issue-with-cdk-deploy-what-do-i-do)
- [Is there a quick way to test it?](#is-there-a-quick-way-to-test-it)
  * [To send a canned JSON message and have it converted to CBOR format (send-json-test-message.sh)](#to-send-a-canned-json-message-and-have-it-converted-to-cbor-format-send-json-test-messagesh)
  * [To send a canned CBOR message and have it converted to JSON format (send-cbor-test-message.sh)](#to-send-a-canned-cbor-message-and-have-it-converted-to-json-format-send-cbor-test-messagesh)
- [How does the system work?](#how-does-the-system-work)

<!-- tocstop -->

## What is this architecture?

This architecture shows how to take messages on specific IoT topics and convert them to/from CBOR format. Messages sent to the `cbor/input` topic in CBOR format come out on the `json/output` topic in JSON format. Messages sent to the `json/input` topic in JSON format come out on the `cbor/output` topic in CBOR format.

## What is CBOR?

[The Concise Binary Object Representation (CBOR) is a data format whose design goals include the possibility of extremely small code size, fairly small message size, and extensibility without the need for version negotiation.](https://cbor.io/)

## Where is this architecture applicable?

This architecture is applicable for customers that are looking for examples of how to convert messages from one format to another. This architecture shows how to work with JSON payloads as well as CBOR binary payloads. With this architecture customers can start to build systems that convert and re-route messages to appropriate destinations easily.

## How do I launch it?

Install the AWS CDK with npm if you haven't already like this:

```
$ npm i -g aws-cdk
```

Then run `cdk deploy`.

If you'd like to see the CloudFormation template that will be launched when you do this you can first do `cdk synth` and review it.

## I ran into an issue with cdk deploy, what do I do?

Open a Github issue and provide as much context as possible. `cdk deploy` in this project requires a JDK to be installed since the CDK code was written in Java. If you don't have a JDK installed you'll need to install one before running the deployment command.

## Is there a quick way to test it?

Yes, there are two bash scripts to test converting from JSON to CBOR and from CBOR to JSON. The scripts don't receive the response via MQTT though so you'll need to use your own MQTT client to subscribe to the appropriate topics and see the responses.

### To send a canned JSON message and have it converted to CBOR format (send-json-test-message.sh)

Run `./send-json-test-message.sh`. The following message will be sent to the `json/input` topic:

```json
{
  "message": "Hello from a bash script"
}
```

The output will show up on the `cbor/output` topic in binary format. In the AWS IoT console's MQTT it should look like this:

```
a1676d657373616765781848656c6c6f2066726f6d2061206261736820736372697074
```

### To send a canned CBOR message and have it converted to JSON format (send-cbor-test-message.sh)

Run `./send-cbor-test-message.sh`. A binary message will be sent to the `cbor/input` topic. In the AWS IoT console's MQTT it should look like this:

```
a1676d657373616765781a48656c6c6f2066726f6d20612062617368207363726970742032
```

The output will show up on the `json/output` topic and should look like this:

```json
{
  "message": "Hello from a bash script 2"
}
```

## How does the system work?

Two Java Lambda functions are set up to convert from JSON to CBOR and CBOR to JSON.

When a JSON message is sent to the `json/input` topic the AWS IoT Rules Engine sends the payload to the `com.awssamples.iot.cbor.handler.handlers.HandleJsonEvent::handleRequest` method in AWS Lambda. This function takes the entire input as a String and calls `CBORObject.FromJSONStrong` to automatically convert the payload from JSON to CBOR. The result is then sent to the `json/output` topic.

When a CBOR message is sent to the `cbor/input` topic the AWS IoT Rules Engine converts the payload to base64. The base64 payload is then put into a Java Map in an entry named `data` and shipped to the `com.awssamples.iot.cbor.handler.handlers.HandleCborEvent::handleRequest` function in AWS Lambda. This function base64 decodes the `data` entry, calls `CBORObject.Read` to convert the CBOR payload to a CBOR object, and then calls the CBOR object's `ToJSONString` method to get the JSON payload. The result is then sent to the `json/output` topic.
