#!/usr/bin/env python3

import boto3
import botoWithAttribution

botoWithAttribution.init('APN/1 PyPartnerSoft,ManagedIoT,v1.2.1')

iot = boto3.client('iot')
data_ats_endpoint = iot.describe_endpoint(endpointType='iot:Data-ATS')
iot_data = boto3.client('iot-data', endpoint_url='https://' + data_ats_endpoint['endpointAddress'])
iot_data.publish(topic='topic_from_python', payload='payload_from_python')
