# DynamoDB API backup

<!-- toc -->

## Attention!

Deleting this stack will NOT delete the S3 bucket and the backup data inside the bucket. If the stack fails to delete you may have the clear out the S3 bucket and try deleting it again. This was an intentional decision made to make sure that data is not lost by accident.

## What is this architecture?

This architecture creates an audit trail for messages received from the DynamoDB API reference architecture by storing all create, update, and delete operations and their metadata in S3 using Kinesis Firehose. It creates the audit trail for the messages as they are received automatically.

## What does this architecture NOT do?

This architecture does not provide a simple inbound message backup system (e.g. just the original payloads and metadata). If you need that functionality try the [backup](../dynamodb-api-backup/README.md) application.

## How is the data stored?

The data is stored in S3 as [ndjson](https://ndjson.org/) so it is easy to process.

## How do I launch it?

Run the `cdk deploy` to deploy this stack.

Run the `cdk destroy` to destroy this stack.
