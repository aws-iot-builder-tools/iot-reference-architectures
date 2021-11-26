#!/usr/bin/env python3

import struct
import sys
from os import listdir
from os.path import isfile, join, isdir

# You can specify a list of names that are used to filter the stream names. The filter is case-insensitive.
filter_list = []

if len(sys.argv) > 1:
    filter_list = sys.argv[1:]

stream_manager_directory = "greengrass/v2/work/aws.greengrass.StreamManager"

if not isdir(stream_manager_directory):
    print("Stream Manager directory not found: " + stream_manager_directory)
    sys.exit(1)

streams = [f for f in listdir(stream_manager_directory) if isdir(join(stream_manager_directory, f))]


def filter_matches(filters, stream_name):
    for current_filter in filters:
        # If any filter term matches, return true
        if stream_name.lower().find(current_filter.lower()) != -1:
            return True

    # Nothing matched
    return False


for stream in streams:
    # If any filters are specified, only print streams that match
    if len(filter_list) != 0 and not filter_matches(filter_list, stream):
        continue

    # Found a stream directory
    print("Processing stream: " + stream)
    # Get the relative path from the directory name
    stream_directory = join(stream_manager_directory, stream)
    # Get all of the files in this stream's directory that end in .log
    stream_files = [f for f in listdir(stream_directory) if isfile(join(stream_directory, f)) and f.endswith(".log")]

    # Loop through a sorted list of files
    for stream_file in sorted(stream_files):
        print("Processing file: " + stream_file)
        # Get the relative path from the file name
        stream_file_path = join(stream_directory, stream_file)

        # Open the file in binary mode
        with open(stream_file_path, "rb") as file:
            # Loop until the end of the file
            while True:
                # The first 28 bytes are part of the header that we don't need
                throwaway = file.read(28)

                if not throwaway:
                    # If we hit EOF then we're done
                    break

                # Read the next 4 bytes which is the length of the current payload
                # Struct unpack to convert the 4 bytes to a number
                #   NOTE: Exclamation point to read the value in network/big-endian byte order
                payload_length = struct.unpack('!i', file.read(4))[0]

                # Read the data and decode it as a UTF-8 string so we can print it cleanly
                data = file.read(payload_length).decode('utf-8')
                print(data)
