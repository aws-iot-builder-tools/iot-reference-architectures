#!/usr/bin/env bash

aws iot-data publish --topic json/input --cli-binary-format raw-in-base64-out --payload '{ "message": "Hello from a bash script" }'
