#!/usr/bin/env bash

# Move to the directory that the script is in so the relative paths will work from anywhere
cd "$(dirname "$0")" || exit

CLI="./greengrass/v2/bin/greengrass-cli"

while [ ! -f "$CLI" ];
do
  echo Waiting for CLI to become available...
  sleep 5
done

./deploy.sh
