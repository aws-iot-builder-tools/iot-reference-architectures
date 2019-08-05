#!/usr/bin/env bash

./gradlew build
aws cloudformation package --template-file template.cf.yaml --output-template-file a.yaml --s3-bucket timmatt-aws
aws cloudformation deploy --template-file /Users/timmatt/Dropbox/github/iot-reference-architectures/dynamodb-api/java/a.yaml --capabilities CAPABILITY_IAM --stack-name teststack
