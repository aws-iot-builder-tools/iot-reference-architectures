#!/usr/bin/env bash

./gradlew build && java -Dcom.amazonaws.sdk.disableCbor=true -jar ./build/libs/aws-iot-client.jar $@
