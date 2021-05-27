#!/usr/bin/env bash

QUEUE_URL=$(aws sqs list-queues --query "QueueUrls[?contains(@, 'dynamodb-api-stack') == \`true\`]" --output text)

if [ -z "$QUEUE_URL" ]; then
  echo Could not find any SQS queues that were created with the DynamoDB API stack. This script will not purge custom queues.
  exit 1
fi

aws sqs purge-queue --queue-url "$QUEUE_URL"
