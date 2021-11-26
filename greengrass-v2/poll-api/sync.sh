#!/usr/bin/env bash

IP=$(../../get-ip.sh)

rsync -avzP --delete \
           --exclude 'greengrass' \
           --exclude 'GreengrassInstaller' \
           ~/github/iot-reference-architectures/greengrass-v2/poll-api/ ubuntu@$IP:/home/ubuntu/environment/poll-api/
