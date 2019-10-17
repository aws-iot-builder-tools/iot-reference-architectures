#!/usr/bin/env python

import boto3
        
client = boto3.client('iot-data')
response = client.publish(topic = 'cbor/input', qos = 0, payload = 'a1676d657373616765781a48656c6c6f2066726f6d206120507974686f6e20736372697074'.decode('hex'))
