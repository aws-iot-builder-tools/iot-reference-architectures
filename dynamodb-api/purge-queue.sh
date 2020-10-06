#!/usr/bin/env bash

QUEUE_URL=$(aws sqs list-queues --query "QueueUrls[?contains(@, 'sqs-to-iot-core-stack') == \`true\`]" --output text)
aws sqs purge-queue --queue-url "$QUEUE_URL"
