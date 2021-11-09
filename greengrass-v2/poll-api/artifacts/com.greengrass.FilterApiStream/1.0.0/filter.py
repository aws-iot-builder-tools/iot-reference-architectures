# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0

import json
import os
import sys

ggutils_path = os.getenv("GGUTILS_PATH")

if ggutils_path is None:
    raise Exception("GGUTILS_PATH environment variable not set and this script is dependent on GGUtils")

sys.path.insert(0, ggutils_path)

import GGUtils

FILTERED_STREAM_NAME = "FilteredStreamName"
STREAM_NAME = "StreamName"
DEVICE_LIST = "DeviceList"


def filter_api(config):
    config = GGUtils.check_any_required_value(STREAM_NAME, config, "stream name")
    config = GGUtils.check_any_required_value(FILTERED_STREAM_NAME, config, "filtered stream name")
    config = GGUtils.check_value_with_default(GGUtils.NEXT_SEQUENCE_NUMBER, config, "next sequence number", 0,
                                              lambda x: int(x))
    config = GGUtils.check_value_with_default(DEVICE_LIST, config, "device list", [])

    create_output_streams(config)

    stream_name = config[GGUtils.find_key_name(STREAM_NAME, config, "stream name")]
    next_sequence_number = config[GGUtils.NEXT_SEQUENCE_NUMBER]
    filtered_stream_name = config[FILTERED_STREAM_NAME]
    device_list = config[DEVICE_LIST]

    GGUtils.loop_over_stream(stream_name, next_sequence_number,
                             processing_function=lambda message_list: filter_message_list(device_list=device_list,
                                                                                          message_list=message_list,
                                                                                          filtered_stream_name=filtered_stream_name))


def filter_message_list(device_list, message_list, filtered_stream_name):
    for message in message_list:
        payload_dict = GGUtils.get_dict_from_message(message)
        filtered_device_data = {}
        device_data = payload_dict['device_data']['points']

        for device in device_list:
            if device not in device_data:
                continue

            filtered_device_data[device] = device_data[device]

        payload_dict['device_data']['points'] = filtered_device_data
        GGUtils.append_message_to_stream(filtered_stream_name, json.dumps(payload_dict).encode())


def create_output_streams(config):
    GGUtils.create_stream(config[FILTERED_STREAM_NAME])


# Run the data processing as a task
GGUtils.TaskHandler(filter_api)
