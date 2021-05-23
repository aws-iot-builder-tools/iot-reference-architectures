#!/usr/bin/env python3

import datetime
import os
import sys
from shared import argparse
from shared import setup
from shared import sleep

try:
    MESSAGE_LIMIT = int(os.getenv('MESSAGE_LIMIT'))
except ValueError as ex:
    MESSAGE_LIMIT = None

MESSAGE_COUNT = 0
RUNNING = True

parser = argparse.ArgumentParser(description='Subscribe to a topic or topics')
parser.add_argument('topics', nargs='+',
                    help='The topics to subscribe to. Make sure to put topics with # wildcards in single quotes so the shell does not ignore them.')
args = parser.parse_args()
topics = args.topics

if not isinstance(topics, list):
    topics = [topics]

print('Subscribing to ' + ', '.join(topics))


def disconnect_and_exit():
    global RUNNING
    global mqtt_client

    RUNNING = False
    mqtt_client.disconnect()
    sys.exit(0)


def print_payload(client, userdata, message):
    global MESSAGE_COUNT
    global MESSAGE_LIMIT

    print(str(datetime.datetime.now()) + ' - ' + str(message.topic) + ' - ' + str(message.payload))

    MESSAGE_COUNT = MESSAGE_COUNT + 1
    if (MESSAGE_LIMIT is None): return
    if (MESSAGE_COUNT >= MESSAGE_LIMIT): disconnect_and_exit()


mqtt_client = setup()
mqtt_client.connect()

for topic in topics:
    mqtt_client.subscribe(topic, 0, print_payload)

while RUNNING:
    sleep(1)
