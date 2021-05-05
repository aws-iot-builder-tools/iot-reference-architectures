#!/usr/bin/env python3

from shared import argparse
from shared import setup
from shared import sleep
import datetime

parser = argparse.ArgumentParser(description='Subscribe to a topic or topics')
parser.add_argument('topics', nargs='+',
                    help='The topics to subscribe to. Make sure to put topics with # wildcards in single quotes so the shell does not ignore them.')
args = parser.parse_args()
topics = args.topics

if not isinstance(topics, list):
    topics = [topics]

print('Subscribing to ' + ', '.join(topics))


def print_payload(client, userdata, message):
    print(str(datetime.datetime.now()) + ' - ' + str(message.topic) + ' - ' + str(message.payload))


mqtt_client = setup()
mqtt_client.connect()

for topic in topics:
    mqtt_client.subscribe(topic, 0, print_payload)

while True:
    sleep(1)
