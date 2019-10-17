#!/usr/bin/env bash

aws iot-data publish --topic json/input --payload '{ "message": "Hello from a bash script" }'
