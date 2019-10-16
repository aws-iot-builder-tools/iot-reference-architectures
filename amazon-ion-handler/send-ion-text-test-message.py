#!/usr/bin/env python

import boto3
        
client = boto3.client('iot-data')
response = client.publish(topic = 'ion/input', qos = 0, payload = '$ion_1_0 {message:"Hello from a Python script"}')
