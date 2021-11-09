# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0

import json
import os
import sys
import tempfile

ggutils_path = os.getenv("GGUTILS_PATH")

if ggutils_path is None:
    raise Exception("GGUTILS_PATH environment variable not set and this script is dependent on GGUtils")

sys.path.insert(0, ggutils_path)

import GGUtils

STREAM_NAME = "StreamName"
BUCKET_NAME = "BucketName"
KEY_PREFIX = "KeyPrefix"
BATCH_SIZE = "BatchSize"
STATUS_NEXT_SEQUENCE_NUMBER_PREFIX = "Status"
STATUS_NEXT_SEQUENCE_NUMBER = f"{STATUS_NEXT_SEQUENCE_NUMBER_PREFIX}{GGUtils.NEXT_SEQUENCE_NUMBER}"


def export_api(config):
    config = GGUtils.check_any_required_value(STREAM_NAME, config, "stream name")
    config = GGUtils.check_required_value(BUCKET_NAME, config, "bucket name")
    config = GGUtils.check_required_value(KEY_PREFIX, config, "key prefix")
    config = GGUtils.check_value_with_default(GGUtils.NEXT_SEQUENCE_NUMBER, config, "next sequence number", 0,
                                              lambda x: int(x))
    config = GGUtils.check_value_with_default(STATUS_NEXT_SEQUENCE_NUMBER, config, "status next sequence number", 0,
                                              lambda x: int(x))
    config = GGUtils.check_value_with_default(BATCH_SIZE, config, "batch size", 100, lambda x: int(x))

    source_stream_name = config[GGUtils.find_key_name(STREAM_NAME, config, "stream name")]
    next_sequence_number = config[GGUtils.NEXT_SEQUENCE_NUMBER]
    bucket = config[BUCKET_NAME]
    key_prefix = config[KEY_PREFIX]
    batch_size = config[BATCH_SIZE]

    # Create a stream to hold the S3 export requests and a stream to hold the S3 export statuses
    export_stream_name, _ = GGUtils.create_export_stream(source_stream_name)

    # Export whatever data is in the stream
    GGUtils.loop_over_stream(source_stream_name, next_sequence_number,
                             min_message_count=batch_size,
                             max_message_count=batch_size,
                             processing_function=lambda message_list: export_message_list(message_list=message_list,
                                                                                          stream_name=export_stream_name,
                                                                                          bucket=bucket,
                                                                                          key_prefix=key_prefix))


def export_message_list(message_list, stream_name, bucket, key_prefix):
    min_sequence_number, max_sequence_number = GGUtils.get_min_and_max_sequence_numbers(message_list)

    with tempfile.NamedTemporaryFile(mode="w", delete=False) as temp:
        file_name = temp.name
        json.dump(list(GGUtils.get_dict_from_message(message) for message in message_list), temp, indent=4)

    key = f"{key_prefix}{min_sequence_number:09d}-{max_sequence_number:09d}"
    GGUtils.create_export_task(stream_name, file_name, bucket, key)


# Run the export process as a task
GGUtils.TaskHandler(export_api)
