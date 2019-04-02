#!/usr/bin/env python

import json
import os

import boto3

sts_client = boto3.client('sts')
iot_client = boto3.client('iot')
role_arn = os.environ['ROLE_ARN']


def lambda_handler(event, context):
    if 'detail' not in event:
        # No detail, cannot continue
        return

    detail = event['detail']

    if 'eventName' not in detail:
        # No event name, cannot continue
        return

    eventName = detail['eventName']

    if eventName != 'CreateThing':
        # Not the event we expected, cannot continue
        return

    if 'responseElements' not in detail:
        # No response elements, cannot continue
        return

    responseElements = detail['responseElements']

    if 'thingName' not in responseElements:
        # No thing name, cannot continue
        return

    thingName = responseElements['thingName']

    if 'account' not in event:
        # No account, cannot continue
        return

    account = event['account']

    # Describe the thing
    data = iot_client.describe_thing(thingName=thingName)

    # Remove the metadata we don't need
    if 'ResponseMetadata' in data:
        del data['ResponseMetadata']

    # Assume the role with STS to publish to the partner account
    role_session_name = 'temp'
    sts_credentials = sts_client.assume_role(RoleArn=role_arn,
                                             RoleSessionName=role_session_name)

    # Create a client for the service the client requested (by name) using the STS credentials
    sts_credentials = sts_credentials['Credentials']
    partner_iot_data_client = boto3.client('iot-data',
                                           aws_access_key_id=sts_credentials['AccessKeyId'],
                                           aws_secret_access_key=sts_credentials['SecretAccessKey'],
                                           aws_session_token=sts_credentials['SessionToken'])

    # Send the data to the cross account hooks topic hierarchy in the partner account
    topic = 'cross_account_hooks/' + account + '/create_thing'

    # Publish the data to the partner account
    payload = json.dumps(data)
    partner_iot_data_client.publish(topic=topic, qos=1, payload=payload)
