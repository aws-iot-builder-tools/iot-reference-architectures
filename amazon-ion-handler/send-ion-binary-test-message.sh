#!/usr/bin/env bash

aws iot-data publish --topic ion/input --cli-binary-format raw-in-base64-out --payload fileb://test-ion-payload.bin
