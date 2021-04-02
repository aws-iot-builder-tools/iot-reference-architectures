#!/usr/bin/env bash

DESIRED_TOPIC=$1

if [ -z "$DESIRED_TOPIC" ]; then
  echo "You must specify the desired topic as the first and only parameter to this script"
  exit 1
fi

DESIRED_TOPIC="$(echo $DESIRED_TOPIC | sed 's/\//%2F/g')"

#openssl genrsa -out cross-account.key 4096
openssl ecparam -out cross-account.key -name prime256v1 -genkey
openssl req -new -key cross-account.key -out cross-account.csr -subj "/CN=$DESIRED_TOPIC"
