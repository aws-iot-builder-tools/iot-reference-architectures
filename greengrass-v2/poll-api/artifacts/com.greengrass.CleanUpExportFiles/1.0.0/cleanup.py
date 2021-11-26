# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0

import os
import sys

from stream_manager import (
    Util, StatusMessage, Status
)

ggutils_path = os.getenv("GGUTILS_PATH")

if ggutils_path is None:
    raise Exception("GGUTILS_PATH environment variable not set and this script is dependent on GGUtils")

sys.path.insert(0, ggutils_path)

import GGUtils

STREAM_NAME = "StreamName"


def clean_up_export_files(config):
    config = GGUtils.check_any_required_value(STREAM_NAME, config, "stream name")
    config = GGUtils.check_value_with_default(GGUtils.NEXT_SEQUENCE_NUMBER, config, "next sequence number", 0,
                                              lambda x: int(x))

    stream_name = config[GGUtils.find_key_name(STREAM_NAME, config, "stream name")]
    next_sequence_number = config[GGUtils.NEXT_SEQUENCE_NUMBER]
    export_status_stream_name = GGUtils.get_export_status_stream_name(stream_name)

    # Clean up whatever files have been successfully exported to S3 and log if there are any errors
    GGUtils.loop_over_stream(export_status_stream_name,
                             next_sequence_number,
                             processing_function=lambda message_list: inner_clean_up(message_list=message_list))


def inner_clean_up(message_list):
    # Process the list of status messages
    for message in message_list:
        # Deserialize the status message first
        status_message = Util.deserialize_json_bytes_to_obj(message.payload, StatusMessage)

        file_url = status_message.status_context.s3_export_task_definition.input_url
        filename = file_url.replace('file://', '')

        if status_message.status == Status.Success:
            GGUtils.logger.info(f"Successfully uploaded {filename} to S3.")
            try:
                os.remove(filename)
            except OSError:
                GGUtils.logger.error(f"Failed to delete {filename}.")
        elif status_message.status == Status.Failure or status_message.status == Status.Canceled:
            GGUtils.logger.error(f"Unable to upload {filename} to S3. Message: {status_message.message}")
            GGUtils.logger.error(f"{filename} will NOT be deleted.")
        else:
            GGUtils.logger.info(f"Ignoring unknown status {filename}. Status: {status_message.status}")


# Run the clean up process as a task
GGUtils.TaskHandler(clean_up_export_files)
