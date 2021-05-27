#!/usr/bin/env python

import json
import os
import sys

import boto3
from dynamodb_json import json_util as ddb_json

kinesis_client = boto3.client('firehose')
delivery_stream_name = os.getenv('delivery_stream_name')


def function_handler(event, context):
    global delivery_stream_name

    if (len(event["Records"]) != 1):
        raise ValueError("Only one record per invocation is allowed")

    event = event["Records"][0]

    if (event["eventName"] != 'INSERT'):
        print("Ignoring this record since backup only handles INSERTs")
        sys.exit(0)

    event = event["dynamodb"]["NewImage"]

    # The newline at the end makes the Firehose files ndjson (http://ndjson.org/)
    # This requires three steps:
    #   1. Dump the event as JSON, this will have the DynamoDB specific JSON with extra values that indicate each entry's data type
    #   2. Load the even from the JSON with DDB JSON, this removes the DynamoDB specific JSON and makes a regular Python dictionary
    #   3. Dump the dictionary from step 2 to get "normal" JSON
    json_event = json.dumps(ddb_json.loads(json.dumps(event))) + '\n'
    kinesis_client.put_record(DeliveryStreamName=delivery_stream_name,
                              Record={'Data': json_event})
    return None
