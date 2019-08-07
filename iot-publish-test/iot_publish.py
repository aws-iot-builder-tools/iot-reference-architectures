#!/usr/bin/env python

import boto3
import json
import os

sts_client = boto3.client('sts')


def lambda_handler(event, context):
    # Assume the role with STS to publish to the partner account
    role_session_name = 'TimMattisonHTTPSPublishSession'
    role_arn = os.environ['RoleToAssume']
    sts_credentials = sts_client.assume_role(RoleArn=role_arn,
                                             RoleSessionName=role_session_name)

    # Create a client for the service the client requested (by name) using the STS credentials
    sts_credentials = sts_credentials['Credentials']
    customer_iot_data_client = boto3.client('iot-data',
                                            aws_access_key_id=sts_credentials['AccessKeyId'],
                                            aws_secret_access_key=sts_credentials['SecretAccessKey'],
                                            aws_session_token=sts_credentials['SessionToken'])

    payload = json.dumps('hello from lambda')
    topic = 'hello there'
    customer_iot_data_client.publish(topic=topic, qos=1, payload=payload)
