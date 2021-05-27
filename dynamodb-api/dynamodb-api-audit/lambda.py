#!/usr/bin/env python

import boto3
import json
import os

kinesis_client = boto3.client('firehose')
delivery_stream_name = os.getenv('delivery_stream_name')


def function_handler(event, context):
    global delivery_stream_name

    # The newline at the end makes the Firehose files ndjson (http://ndjson.org/)
    json_event = json.dumps(event) + '\n'
    kinesis_client.put_record(DeliveryStreamName=delivery_stream_name,
                              Record={'Data': json_event})
    return None
