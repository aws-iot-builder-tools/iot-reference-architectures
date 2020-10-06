#!/usr/bin/env bash

UUID=$1

if [ -z "$UUID" ]; then
  echo You must specify a UUID
  exit 1
fi

TOKEN=`uuidgen`
aws iot-data publish --topic request/devices/$UUID/$TOKEN --cli-binary-format raw-in-base64-out --payload '{}'
