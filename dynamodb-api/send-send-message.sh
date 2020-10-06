#!/usr/bin/env bash

UUID=$1
PAYLOAD=$2

if [ -z "$UUID" ]; then
  echo You must specify a UUID
  exit 1
fi

if [ -z "$PAYLOAD" ]; then
  echo You must specify a payload
  exit 1
fi

TOKEN=`uuidgen`
BASE64_PAYLOAD=$(echo $PAYLOAD | base64 -)
aws iot-data publish --topic request/send/$UUID --cli-binary-format raw-in-base64-out --payload '{"token":"'$TOKEN'","base64_payload":"'$BASE64_PAYLOAD'"}'
