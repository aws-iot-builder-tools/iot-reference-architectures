#!/usr/bin/env python

import json
import os

import boto3

sts_client = boto3.client('sts')
role_arn = os.environ['ROLE_ARN']


def lambda_handler(event, context):
    if 'topic' not in event:
        # Can't act on the event without knowing which topic it is for
        return

    topic = event['topic']
    del event['topic']

    # Assume the role with STS to publish to the partner account
    role_session_name = 'temp'
    sts_credentials = sts_client.assume_role(RoleArn=role_arn,
                                             RoleSessionName=role_session_name)

    # Create a client for the service the client requested (by name) using the STS credentials
    sts_credentials = sts_credentials['Credentials']
    customer_iot_data_client = boto3.client('iot-data',
                                            aws_access_key_id=sts_credentials['AccessKeyId'],
                                            aws_secret_access_key=sts_credentials['SecretAccessKey'],
                                            aws_session_token=sts_credentials['SessionToken'])

    payload = json.dumps(event)
    customer_iot_data_client.publish(topic=topic, qos=1, payload=payload)
