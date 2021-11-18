#!/usr/bin/env python

import boto3
import botoWithAttribution

botoWithAttribution.init('my_sdk_python', 'my_platform_python,00:11:22:33:44:55,serial_number')

iot_data = boto3.client('iot-data')
iot_data.publish(topic='topic_from_python', payload='payload_from_python')
