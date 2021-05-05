#!/usr/bin/env python3

from shared import argparse
from shared import setup

parser = argparse.ArgumentParser(description='Publish a payload to a topic')
parser.add_argument('topic')
parser.add_argument('payload')
args = parser.parse_args()
topic = args.topic
payload = args.payload
print('Publishing payload to ' + topic)

mqtt_client = setup()
mqtt_client.connect()
mqtt_client.publish(topic, payload, 0)
mqtt_client.disconnect()
