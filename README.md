## IoT Reference Architectures

The repo is a place to store architecture diagrams and the code for reference architectures that we refer to in IoT presentations.

## Terminology

- Partner - an entity that builds solutions on AWS that can be integrated into a customer's own AWS account
- Customer - an entity with an AWS account that can utilize partner solutions

## CDK reference architecture list

These reference architectures use [CDK](https://aws.amazon.com/cdk/)

- DynamoDB API
  - Java
    - [SQS to DynamoDB to IoT Core](dynamodb-api/java) - An example project that shows how to take messages from SQS, move them DynamoDB, and then query the DynamoDB table with an IoT Core based API. This pattern is useful when multiple applications need access to messages from a device, the messages from a device come through a non-MQTT ingest mechanism and are stored in SQS, or when a device may send multiple messages that need to be processed in order.
- Binary payloads
  - Java
    - [CBOR Handler](cbor-handler) - An example project that shows how to convert between CBOR and JSON. This uses the rules engine base64 encoding support to work with binary payloads in AWS Lambda.
  - Python
    - [Amazon Ion Handler](amazon-ion-handler) - An example project that shows how to convert between Amazon Ion and JSON. This uses the rules engine base64 encoding support to work with binary payloads in AWS Lambda.
- JWT authentication for AWS IoT Core
  - Java
    - [Java custom authentication demo with JWT](serverless-ui/jwt-stack) - A stack that contains a serverless UI that shows how to use custom authentication with JWTs in AWS IoT Core
- Cross-account publish
  - [Certificate based stack](cross-account-publish/certificate-based-stack) - A stack that simplifies onboarding an AWS IoT data producer to an account using a certificate to allow cross-account publishing

## CloudFormation reference architecture list

These reference architectures use CloudFormation, not CDK

- CloudWatch Events
  - [Python](cloudwatch-events/python) - Sets up a CloudWatch Events rule that triggers a Python function when CreateThing is called. The Python function cross-account publishes the event information from a customer's account to a partner's account to give the partner visibility to new devices that a customer creates in their account.
- Cross-account publish
  - [C#](cross-account-publish/c-sharp) - A C# application that can cross-account publish from a partner's account to a customer's account to give the partner the ability to easily share information with their customers. This C# application is for running on EC2, not on AWS Lambda, and includes the necessary role that must be attached to the EC2 instance.
  - [Python](cross-account-publish/python) - A Python Lambda function that can cross-account publish from a partner's account to a customer's account using the AWS IoT Rules Engine

## General code samples

- MQTT over WebSockets
  - Java
    - [Example with Jitpack](mqtt-over-websockets-jitpack) - An example project that shows how to use the MQTT over WebSocketsJava library with Jitpack and includes tests to validate that the library is working as expected. This example demonstrates how a customer can use the library as a Gradle dependency without duplicating the code.
- Results iterator
  - Java
    - [Example with Jitpack](results-iterator-jitpack) - An example project that shows how to use the results iterator library with Jitpack and includes tests to validate that the library is working as expected. This example demonstrates how a customer can use the library as a Gradle dependency without duplicating the code.

## License Summary

This sample code is made available under a modified MIT license. See the LICENSE file.
