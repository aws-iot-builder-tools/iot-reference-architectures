#!/usr/bin/env bash

# Move to the directory that the script is in so the relative paths will work from anywhere
cd "$(dirname "$0")" || exit

# 2>&1 redirects stderr to stdout so we can grep directly
./greengrass/v2/bin/greengrass-cli logs get \
  --log-dir greengrass/v2/logs \
  2>&1
