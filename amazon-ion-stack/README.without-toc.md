# Amazon Ion handler

<!-- toc -->

## What is this architecture?

This architecture shows how to take messages on specific IoT topics and convert them to/from Amazon Ion format. Messages sent to the `ion/input` topic in Amazon Ion format come out on the `json/output` topic in JSON format. Messages sent to the `json/input` topic in JSON format come out on the `ion/output` topic in Amazon Ion format.

## What is Amazon Ion?

[Amazon Ion is a richly-typed, self-describing, hierarchical data serialization format offering interchangeable binary and text representations.](http://amzn.github.io/ion-docs/)

## Where is this architecture applicable?

This architecture is applicable for customers that are looking for examples of how to convert messages from one format to another. This architecture shows how to work with JSON payloads as well as Ion text and binary payloads. With this architecture customers can start to build systems that convert and re-route messages to appropriate destinations easily.

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

Yes, there are three bash scripts to test converting from JSON to Ion, from Ion's binary format to JSON, and from Ion's text format to JSON. The scripts don't receive the response via MQTT though so you'll need to use your own MQTT client to subscribe to the appropriate topics and see the responses.

### To send a canned JSON message and have it converted to Ion format (send-json-test-message.sh)

Run `./send-json-test-message.sh`. The following message will be sent to the `json/input` topic:

```json
{
  "message": "Hello from a bash script"
}
```

The output will show up on the `ion/output` topic in binary format. In the AWS IoT console's MQTT it should look like this:

```
e00100eaee8e8183de8a87b8876d657373616765de9b8a8e9848656c6c6f2066726f6d2061206261736820736372697074
```

### To send a canned Amazon Ion message in text format and have it converted to JSON format (send-ion-text-test-message.sh)

Run `./send-ion-text-test-message.sh`. The following message will be sent to the `ion/input` topic:

```
$ion_1_0 {message:"Hello from a bash script 2"}
```

The output will show up on the `json/output` topic and should look like this:

```json
{
  "message": "Hello from a bash script 2"
}
```

### To send a canned Amazon Ion message in binary format and have it converted to JSON format (send-ion-binary-test-message.sh)

Run `./send-ion-binary-test-message.sh`. A binary message will be sent to the `ion/input` topic. In the AWS IoT console's MQTT it should look like this:

```
e00100eaee8e8183de8a87b8876d657373616765de9d8a8e9a48656c6c6f2066726f6d20612062617368207363726970742033
```

The output will show up on the `json/output` topic and should look like this:

```json
{
  "message": "Hello from a bash script 3"
}
```

## How does the system work?

The Python scripts to perform this conversion are relatively short. The Ion to JSON conversion takes 16 lines of code while the JSON to Ion conversion takes 12 lines of code.

When a JSON message is sent to the `json/input` topic the AWS IoT Rules Engine converts the payload to a Python dictionary and ships it to the `Json.py` function in AWS Lambda in the AWS Lambda event object. This function takes the entire input event and calls the `amazon.ion.simpleion.dumps` function with the mode set to binary. This function operates just like Python's `json.dumps` function and converts the inbound object to Ion format automatically. The result of `amazon.ion.simpleion.dumps` is then sent to the `ion/output` topic.

When an Ion message is sent to the `ion/input` topic the AWS IoT Rules Engine first converts the payload to base64. The base64 payload is then put into a Python dictionary in an entry named `data` and shipped to the `Ion.py` function in AWS Lambda in the AWS Lambda event object. This function base64 decodes the event's `data` entry, calls `amazon.ion.simpleion.loads` to convert the Ion payload to an object, converts the full Ion object to a plain dictionary, and then calls `json.dumps` on the dictionary. The result of `json.dumps` is then sent to the `json/output` topic.
