#!/usr/bin/env python3

import os
import sys
from os import listdir
from os.path import join, isdir
import subprocess

stream_manager_directory = "greengrass/v2/work/aws.greengrass.StreamManager"

if not isdir(stream_manager_directory):
    print("Stream Manager directory not found: " + stream_manager_directory)
    sys.exit(1)

streams = [f for f in listdir(stream_manager_directory) if isdir(join(stream_manager_directory, f))]

for stream in streams:
    process = subprocess.Popen(["sh", "-c", f"./read-stream.py {stream} | wc -l"], stdout=subprocess.PIPE)
    result = process.communicate()[0].decode("utf-8").strip()

    print(f"{stream} - {result}")
