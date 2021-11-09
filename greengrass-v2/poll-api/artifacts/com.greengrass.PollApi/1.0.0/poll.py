# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0

import os
import sys

import requests

ggutils_path = os.getenv("GGUTILS_PATH")

if ggutils_path is None:
    raise Exception("GGUTILS_PATH environment variable not set and this script is dependent on GGUtils")

sys.path.insert(0, ggutils_path)

import GGUtils

STREAM_NAME = "StreamName"
ENDPOINT = "Endpoint"


def poll_api(config):
    config = GGUtils.check_required_value(ENDPOINT, config, "API endpoint URL")
    config = GGUtils.check_required_value(STREAM_NAME, config, "stream name")

    endpoint = config[ENDPOINT]
    stream_name = config[STREAM_NAME]

    # Fetch data from an endpoint
    data = requests.get(endpoint)

    # Create the stream
    GGUtils.create_stream(stream_name)
    GGUtils.append_message_to_stream(stream_name, data.content)


# Run the API poller as a task
GGUtils.TaskHandler(poll_api)
