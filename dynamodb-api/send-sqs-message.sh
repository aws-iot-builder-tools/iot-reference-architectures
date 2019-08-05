#!/usr/bin/env bash

QUEUE_URL=$(aws sqs list-queues --query "QueueUrls[?contains(@, 'sqs-to-iot-core-stack') == \`true\`]" --output text)
MESSAGE=$(cat sqs-example.json)
EPOCH_TIME=$(date +%s)
MESSAGE=${MESSAGE//EPOCH_TIME/$EPOCH_TIME}
echo $MESSAGE
aws sqs send-message --queue-url "$QUEUE_URL" --message-body "$MESSAGE"
