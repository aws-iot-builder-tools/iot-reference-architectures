# SQS to DynamoDB with IoT Core based API

<!-- toc -->

## What is this architecture?

This architecture shows how to take messages from an SQS queue, move them into DynamoDB automatically, and build an API on top of IoT Core that allows other systems to query the data in DynamoDB.

## Where is this architecture applicable?

This architecture is applicable for customers that have processes that already drop messages into a queue but those messages need to be accessed by multiple systems. SQS is an appropriate destination for high value messages that must be durably stored in systems that must provide guarantees that messages are processed. Customers with existing systems that process messages that meet this criteria often already put messages into SQS. With this architecture those messages can be made available to more applications easily.

## How do I launch it?

Install the AWS CDK with npm if you haven't already like this:

```
$ npm i -g aws-cdk
```

Then run `cdk deploy`.

If you have an existing SQS queue that you want to use instead of creating a new one simply pass the SQS queue's ARN as an environment variable to `cdk deploy` and the deployment will hook the reference architecture up to the existing queue. Assuming your inbound SQS queue's ARN is `arn:aws:sqs:us-east-1:5xxxxxxxxxx7:inbound-queue` and your outbound SQS queue's ARN is `arn:aws:sqs:us-east-1:5xxxxxxxxxx7:outbound-queue` simply run (in bash) `INBOUND_SQS_QUEUE_ARN=arn:aws:sqs:us-east-1:5xxxxxxxxxx7:inbound-queue OUTBOUND_SQS_QUEUE_ARN=arn:aws:sqs:us-east-1:5xxxxxxxxxx7:outbound-queue cdk deploy`.

If you'd like to see the CloudFormation template that will be launched when you do this you can first do `cdk synth` and review it. If you have an existing queue, as mentioned above, you can run (in bash) `INBOUND_SQS_QUEUE_ARN=arn:aws:sqs:us-east-1:5xxxxxxxxxx7:inbound-queue OUTBOUND_SQS_QUEUE_ARN=arn:aws:sqs:us-east-1:5xxxxxxxxxx7:outbound-queue cdk synth`.

## I ran into an issue with cdk deploy, what do I do?

Open a Github issue and provide as much context as possible. `cdk deploy` in this project requires a JDK to be installed since the CDK code was written in Java. If you don't have a JDK installed you'll need to install one before running the deployment command.

## What is "an API on top of IoT Core"?

IoT Core is an MQTT message broker. To build an API on top of IoT Core means that there is a dedicated topic hierarchy for requests and another dedicated topic hierarchy for responses. A system makes a request on a request topic, the rules engine triggers Lambda functions to service that request, and then a response indicating the status of that request is sent to the response topic.

## Why is an API on top of IoT Core desirable?

With IoT Core the system making the request and the system receiving the response can be the same system, or they can be separate systems. This allows components of a system to be split up into smaller pieces that are easier to build and debug. For example, a user might click a button on a mobile app to send a request to delete a message from the system. Once the request has been serviced a different system might be listening on the response topic to instantly update a dashboard with the latest state information.

One or more systems can be wired up to receive messages on the response topics. Initially the system may be configured so that just the originator of a request gets confirmation that an operation completed. Later there may be dashboards, audit logs, and monitoring systems that can tie into those same responses without interfering with each other.

## Why not provide direct DynamoDB access instead?

Direct DynamoDB access might be appropriate for certain systems but providing this additional API on top of IoT Core allows the system to consolidate logic that would otherwise be duplicated in multiple places. Also, as mentioned above, IoT Core allows adding additional systems to the response topics. This decoupling of request/response messaging gives the user greater flexibility in how systems are built.

## Is there a quick way to test it?

Yes, there are several bash scripts that can be used to add messages to the SQS queue and then query them via MQTT. The scripts don't receive the response via MQTT though so you'll need to use your own MQTT client to subscribe to the appropriate topics and see the responses.

### To put a message into the queue (send-sqs-message.sh)

Run `./send-sqs-message.sh THING_NAME` where `THING_NAME` is the name of the thing you want to simulate a message from. A message will be generated and dropped into the queue. Then the message should automatically move to DynamoDB.

### To send the query request (send-query-message.sh)

Run `./send-query-message.sh THING_NAME` where `THING_NAME` is the name of the simulated thing you want to query.

### To send the get request (send-get-message.sh)

Run `./send-get-message.sh THING_NAME MESSAGE_ID` where `THING_NAME` is the name of the simulated thing and `MESSAGE_ID` is the ID of the message to get.

### To send the next request (send-next-message.sh)

Run `./send-next-message.sh THING_NAME MESSAGE_ID` where `THING_NAME` is the name of the simulated thing and `MESSAGE_ID` is the ID of the last message you've requested.

### To send the delete request (send-delete-message.sh)

Run `./send-delete-message.sh THING_NAME MESSAGE_ID` where `THING_NAME` is the name of the simulated thing and `MESSAGE_ID` is the ID of the message you want to delete.

## How does the system work?

### Phase 1: Dropping messages into SQS

In phase 1 a user or system drops a message into a specific SQS queue. That message is expected to have two field present. The first field is a UUID field that identifies the source of the message (typically a device that the message originated from). The second field is a message ID field that is used to order the messages in DynamoDB. The message ID field is expected to be a value that increases over time like a Unix epoch timestamp.

### Phase 2: Moving a message from SQS to DynamoDB

In phase 2 SQS sends an event to a Lambda function that moves the message from SQS to DynamoDB. The UUID field is placed in the column named `uuid`. The message ID is placed in the column named `messageId`. The full body of the message is placed in the column named `body`.

The name of the UUID field in the input message is specified in the `uuidKey` value in the environment of this Lambda function.
The name of the message ID field in the input message is specified in the `messageIdKey` value in the environment of this Lambda function.

Once DynamoDB has acknowledged that the record was stored successfully the Lambda function removes the message from SQS. In the event of any failures the process is retried.

The Lambda function in this phase always expects to receive exactly one record at a time. It is not built to receive batches of messages.

### Phase 3: Utilizing the IoT Core based API

In phase 3 the data resides in DynamoDB and is ready to be queried. There are four types of requests that the system can process.

The topic hierarchy to for the requests is:

`request/{OPERATION}/{UUID}/{MESSAGE_ID}`

`{OPERATION}` is the name of the operation. Either `get`, `query`, `delete`, or `next`.
`{UUID}` is the UUID from phase 1.
`{MESSAGE_ID}` is the message ID from phase 1. It is only used for `get`, `delete`, and `next`. This topic hierarchy level must be omitted when using `query`.

The payload of each request must contain a JSON map with the field `token` specified. The token field is used to determine where the response should be sent (see examples).

#### Query operation

The query operation gets information about what messages are available for a particular UUID.

Example request topic:

```
request/query/testThing
```

Example request payload:

```
{ "token": "498641EE-3CC6-4B8D-857F-E484F58E947A" }
```

Example response topic:

```
response/query/498641EE-3CC6-4B8D-857F-E484F58E947A
```

Example response payload:

```
{
    "newestMessageId": "1564669331-1564669332364-7d7efcff-bdcf-4d98-917e-d171d27cc0b4",
    "oldestMessageId": "1564669331-1564669332364-7d7efcff-bdcf-4d98-917e-d171d27cc0b4",
    "uuid": "testThing"
}
```

In this case `testThing` only has one message available since the oldest message ID and the newest message ID are the same. If more than one message was available these values would be different.

The `uuid` value is included to help disambiguate responses for any systems that are receiving the responses but did not issue the requests.

If no messages are available an error response is returned that looks like this:

```
{
    "error": "No messages available",
    "uuid": "testThing"
}
```

#### Get operation

The get operation retrieves the contents of a specific message ID for a particular UUID.

Example request topic:

```
request/get/testThing/1564669331-1564669332364-7d7efcff-bdcf-4d98-917e-d171d27cc0b4
```

Example request payload:

```
{ "token": "76144D49-C155-481D-B84C-E86F00D329DC" }
```

Example response topic:

```
response/get/76144D49-C155-481D-B84C-E86F00D329DC
```

Example response payload:

```
{
    "body": {
        "epochTime": "1564669331",
        "tempSensor1F": "72",
        "tempSensor2F": "70",
        "thingName": "testThing"
    },
    "messageId": "1564669331-1564669332364-7d7efcff-bdcf-4d98-917e-d171d27cc0b4",
    "uuid": "testThing"
}
```

In this case the message was found and the original contents are in the `body` field.

The `uuid` and `messageId` values are included to help disambiguate responses for any systems that are receiving the responses but did not issue the requests.

#### Next operation

The next operation retrieves the next message ID for a particular UUID relative to a specified message ID. This is useful if a system needs to iterate through messages. The system can start at a particular message ID and iterate over as many messages as it needs to.

Example request topic:

```
request/next/testThing/1565033964-1565033965273-d8e31549-fd90-46f6-9588-2a28a9173f3a
```

Example request payload:

```
{ "token": "7AB81EFB-3CF8-4D7B-9EE3-DE9E114464BD" }
```

Example response topic:

```
response/next/7AB81EFB-3CF8-4D7B-9EE3-DE9E114464BD
```

Example response payload:

```
{
    "nextMessageId": "1565034142-1565034143557-e93620fb-073c-479d-8234-1fc5bfa5155c",
    "specifiedMessageId": "1565033964-1565033965273-d8e31549-fd90-46f6-9588-2a28a9173f3a",
    "uuid": "testThing"
}
```

In this case a message after the specified message ID was found and the next message ID is in the `nextMessageId` field

The `uuid` value is included to help disambiguate responses for any systems that are receiving the responses but did not issue the requests.

If no newer messages are available an error response is returned that looks like this:

```
{
    "error": "No newer messages available",
    "specifiedMessageId": "1565033964-1565033965273-d8e31549-fd90-46f6-9588-2a28a9173f3a",
    "uuid": "testThing"
}
```

#### Delete operation

The delete operation deletes the message with the specified message ID for a particular UUID.

Example request topic:

```
request/delete/testThing/1565033964-1565033965273-d8e31549-fd90-46f6-9588-2a28a9173f3a
```

Example request payload:

```
{ "token": "BC3CDD49-8E0C-4D37-89A9-48E2D29E2522" }
```

Example response topic:

```
response/delete/BC3CDD49-8E0C-4D37-89A9-48E2D29E2522
```

Example response payload:

```
{
    "messageId": "1565033964-1565033965273-d8e31549-fd90-46f6-9588-2a28a9173f3a",
    "uuid": "testThing"
}
```

The `uuid` and `messageId` values are included to help disambiguate responses for any systems that are receiving the responses but did not issue the requests. This operation will never return an error.
