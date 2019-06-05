## IoT Reference Architectures

The repo is a place to store architecture diagrams and the code for reference architectures that we refer to in IoT presentations.

## Terminology

- Partner - an entity that builds solutions on AWS that can be integrated into a customer's own AWS account
- Customer - an entity with an AWS account that can utilize partner solutions

## Reference architecture list

- CloudWatch Events
  - [Python](cloudwatch-events/python) - Sets up a CloudWatch Events rule that triggers a Python function when CreateThing is called. The Python function cross-account publishes the event information from a customer's account to a partner's account to give the partner visibility to new devices that a customer creates in their account.
- Cross-account publish
  - [C#](cross-account-publish/c-sharp) - A C# application that can cross-account publish from a partner's account to a customer's account to give the partner the ability to easily share information with their customers. This C# application is for running on EC2, not on AWS Lambda, and includes the necessary role that must be attached to the EC2 instance.
  - [Python](cross-account-publish/python) - A Python Lambda function that can cross-account publish from a partner's account to a customer's account using the AWS IoT Rules Engine
- MQTT over WebSockets
  - Java
    - [Library](mqtt-over-websockets/java) - A Java library, derived from the AWS IoT Device SDK for Java, that creates vanilla Paho connections over WebSockets to AWS IoT Core
    - [Example with Jitpack](mqtt-over-websockets-jitpack/java) - An example project that shows how to use the MQTT over WebSocketsJava library with Jitpack and includes tests to validate that the library is working as expected. This example demonstrates how a customer can use the library as a Gradle dependency without duplicating the code.

## License Summary

This sample code is made available under a modified MIT license. See the LICENSE file.
