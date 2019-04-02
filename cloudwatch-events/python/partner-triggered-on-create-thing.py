#!/usr/bin/env python

import json

import boto3

iot_data_client = boto3.client('iot-data')


def lambda_handler(event, context):
    if 'accountId' not in event:
        # Can't act on the event without knowing which customer it came from
        return

    if 'thingName' not in event:
        # Can't act on the event without knowing which thing it is for
        return

    if 'attributes' not in event:
        # (Optional) Can't act on the event without validating that the attributes we need are there (e.g. serial number)
        return

    if 'operation' not in event:
        # Can't act on the event without knowing which operation it is
        return

    # Debug info
    accountId = event['accountId']
    operation = event['operation']
    debug_topic = 'debug/' + accountId + '/' + operation
    iot_data_client.publish(topic=debug_topic, qos=1, payload=json.dumps(event))

    # TODO implement partner specific functionality
