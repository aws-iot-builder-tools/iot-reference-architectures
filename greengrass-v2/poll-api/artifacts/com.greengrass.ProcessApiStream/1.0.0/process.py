# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0

import os
import sys

ggutils_path = os.getenv("GGUTILS_PATH")

if ggutils_path is None:
    raise Exception("GGUTILS_PATH environment variable not set and this script is dependent on GGUtils")

sys.path.insert(0, ggutils_path)

import GGUtils

ABNORMAL_VALUE_STREAM_NAME = "AbnormalValueStreamName"
HIGH_VALUE_STREAM_NAME = "HighValueStreamName"
LOW_VALUE_STREAM_NAME = "LowValueStreamName"
CHECK_FIELD_MAX_VALUE = "CheckFieldMaxValue"
CHECK_FIELD_MIN_VALUE = "CheckFieldMinValue"
CHECK_FIELD_NUMBER = "CheckFieldNumber"
FILTERED_STREAM_NAME = "FilteredStreamName"


def process_api(config):
    config = GGUtils.check_any_required_value(FILTERED_STREAM_NAME, config, "filtered stream name")
    config = GGUtils.check_required_value(CHECK_FIELD_NUMBER, config, "check field number")
    config = GGUtils.check_required_value(CHECK_FIELD_MIN_VALUE, config, "check field minimum value", lambda x: int(x))
    config = GGUtils.check_required_value(CHECK_FIELD_MAX_VALUE, config, "check field maximum value", lambda x: int(x))
    config = GGUtils.check_required_value(LOW_VALUE_STREAM_NAME, config, "low value stream name")
    config = GGUtils.check_required_value(HIGH_VALUE_STREAM_NAME, config, "high value stream name")
    config = GGUtils.check_required_value(ABNORMAL_VALUE_STREAM_NAME, config, "abnormal value stream name")
    config = GGUtils.check_value_with_default(GGUtils.NEXT_SEQUENCE_NUMBER, config, "next sequence number", 0,
                                              lambda x: int(x))

    create_output_streams(config)

    filtered_stream_name = config[GGUtils.find_key_name(FILTERED_STREAM_NAME, config, "filtered stream name")]
    next_sequence_number = config[GGUtils.NEXT_SEQUENCE_NUMBER]
    abnormal_value_stream_name = config[ABNORMAL_VALUE_STREAM_NAME]
    high_value_stream_name = config[HIGH_VALUE_STREAM_NAME]
    low_value_stream_name = config[LOW_VALUE_STREAM_NAME]
    check_field_max_value = config[CHECK_FIELD_MAX_VALUE]
    check_field_min_value = config[CHECK_FIELD_MIN_VALUE]
    check_field_number = config[CHECK_FIELD_NUMBER]

    def processing_function(message_list):
        process_message_list(check_field_number, check_field_min_value, check_field_max_value, low_value_stream_name,
                             high_value_stream_name, abnormal_value_stream_name, message_list)

    # Process the list of messages we received
    GGUtils.loop_over_stream(filtered_stream_name, next_sequence_number, processing_function=processing_function)


def process_message_list(check_field_number, check_field_min_value, check_field_max_value, low_value_stream_name,
                         high_value_stream_name, abnormal_value_stream_name, message_list):
    for message in message_list:
        payload_dict = GGUtils.get_dict_from_message(message)

        found_high, found_low = check_payload_for_outliers(payload_dict, check_field_number, check_field_min_value,
                                                           check_field_max_value)

        if found_high:
            # Log high values
            GGUtils.append_message_to_stream(high_value_stream_name, message.payload)

        if found_low:
            # Log low values
            GGUtils.append_message_to_stream(low_value_stream_name, message.payload)

        if found_high or found_low:
            # Log high and low values in a combined stream
            GGUtils.append_message_to_stream(abnormal_value_stream_name, message.payload)
            # Publish them to IoT Core as well
            GGUtils.publish_results_to_cloud_qos_1("anomalies", payload_dict)


def check_payload_for_outliers(payload_dict, check_field_number, check_field_min_value, check_field_max_value):
    found_high = False
    found_low = False

    for key, value in payload_dict['device_data']['points'].items():
        if not type(value) is list:
            # Ignore this value since it isn't a list
            continue

        for item in value:
            value_to_check = item[check_field_number]

            if value_to_check < check_field_min_value:
                found_low = True

            if value_to_check > check_field_max_value:
                found_high = True

            if found_high and found_low:
                # If we found both a high and low value then we can stop looking
                break

    return found_high, found_low


def create_output_streams(config):
    GGUtils.create_stream(config[LOW_VALUE_STREAM_NAME])
    GGUtils.create_stream(config[HIGH_VALUE_STREAM_NAME])
    GGUtils.create_stream(config[ABNORMAL_VALUE_STREAM_NAME])


# Run the data processing as a task
GGUtils.TaskHandler(process_api)
