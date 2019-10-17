#!/usr/bin/env python

import boto3
        
client = boto3.client('iot-data')
response = client.publish(topic = 'ion/input', qos = 0, payload = 'e00100eaee8e8183de8a87b8876d657373616765de9d8a8e9a48656c6c6f2066726f6d206120507974686f6e20736372697074'.decode('hex'))
