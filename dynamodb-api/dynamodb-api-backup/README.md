# DynamoDB API backup

<!-- toc -->

- [Attention!](#attention)
- [What is this architecture?](#what-is-this-architecture)
- [What does this architecture NOT do?](#what-does-this-architecture-not-do)
- [How is the data stored?](#how-is-the-data-stored)
- [How do I launch it?](#how-do-i-launch-it)

<!-- tocstop -->

## Attention!

Deleting this stack will NOT delete the S3 bucket and the audit data inside the bucket. If the stack fails to delete you may have the clear out the S3 bucket and try deleting it again. This was an intentional decision made to make sure that data is not lost by accident.

## What is this architecture?

This architecture backs up messages received from the DynamoDB API reference architecture by storing them in S3 using Kinesis Firehose. It backs up the messages as they are received automatically.

## What does this architecture NOT do?

This architecture does not provide an audit trail for messages (e.g. if a message is modified or deleted). If you need that functionality try the [audit](../dynamodb-api-audit/README.md) application.

## How is the data stored?

The data is stored in S3 as [ndjson](https://ndjson.org/) so it is easy to process.

## How do I launch it?

Run the `cdk deploy` to deploy this stack.

Run the `cdk destroy` to destroy this stack.
