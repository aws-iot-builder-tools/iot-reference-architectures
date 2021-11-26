#!/usr/bin/env python3

import json
import time
from random import gauss

from flask import Flask

number_of_devices = 10
number_of_values_per_second = 2
last_request = None

app = Flask(__name__)


@app.route('/')
def index():
    return 'Server is running'


def get_time_ms():
    return int(time.time() * 1000)


def generate_one_device(device_number, number_of_values, time_between_values):
    temp_data = []
    now = get_time_ms()
    for i in range(number_of_values):
        value = gauss(5, 2)
        temp_data.append(
            [int(now - gauss(1000 * time_between_values, 500)), "datum", str(value), value, 0])

    return {f"device_{device_number}": temp_data}


@app.route('/data')
def data():
    global last_request

    now = get_time_ms()

    if last_request is None:
        last_request = get_time_ms() - 10000

    # Generate the desired number of values per second
    number_of_values = int((now - last_request) / 1000 * number_of_values_per_second)

    if number_of_values == 0:
        return json.dumps({})

    last_request = now

    temp_data = {}

    for i in range(number_of_devices):
        temp_data.update(generate_one_device(i, number_of_values, 1))

    return json.dumps({"device_data": {
                           "descriptions": [
                               "timestamp",
                               "name",
                               "text_value",
                               "numeric_value",
                               "source"
                           ],
                           "points": temp_data
                       }
                       })
