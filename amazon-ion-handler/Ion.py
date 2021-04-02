#!/usr/bin/env python

import amazon.ion.simpleion as ion
import json
import os
import boto3
import base64

iot = boto3.client('iot')
endpoint_address = iot.describe_endpoint(endpointType='iot:Data-ATS')['endpointAddress']
iot_data = boto3.client('iot-data', endpoint_url='https://' + endpoint_address)

output_topic = os.environ['OutputTopic']


def function_handler(event, context):
    decoded = base64.b64decode(event['data'])
    obj = ion.loads(decoded)
    return iot_data.publish(topic=output_topic, qos=0, payload=json.dumps(dict(obj.items())))
