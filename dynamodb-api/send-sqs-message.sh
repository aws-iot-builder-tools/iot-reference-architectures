#!/usr/bin/env bash

if [ -z "$QUEUE_URL" ];
then
  QUEUE_URL=$(aws sqs list-queues --query "QueueUrls[?contains(@, 'sqs-to-iot-core-stack') == \`true\`]" --output text)

  if [ -z "$QUEUE_URL" ];
  then
    echo QUEUE_URL not specified in the environment and could not be found in the CloudFormation stack. The queue must be created by the CloudFormation stack or its URL must be passed in as an environment variable.
    exit 1
  fi
fi

MESSAGE=$(cat sqs-example.json)
EPOCH_TIME=$(date +%s)
MESSAGE=${MESSAGE//EPOCH_TIME/$EPOCH_TIME}
echo $MESSAGE
aws sqs send-message --queue-url "$QUEUE_URL" --message-body "$MESSAGE"
