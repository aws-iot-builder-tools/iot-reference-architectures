#!/usr/bin/env bash

aws iot-data publish --topic cbor/input --cli-binary-format raw-in-base64-out --payload fileb://test-cbor-payload.bin
