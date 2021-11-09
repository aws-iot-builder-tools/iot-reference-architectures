#!/usr/bin/env bash

# Move to the directory that the script is in so the relative paths will work from anywhere
cd "$(dirname "$0")" || exit

rm -rf greengrass
