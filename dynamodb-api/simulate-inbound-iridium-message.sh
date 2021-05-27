#!/usr/bin/env bash

UUID=$1

if [ -z "$UUID" ]; then
  echo You must specify a UUID
  exit 1
fi

if [ -z "$QUEUE_URL" ];
then
  QUEUE_URL=$(aws sqs list-queues --query "QueueUrls[?contains(@, 'ICCMO.fifo')]" --output text | grep ICCMO)

  if [ -z "$QUEUE_URL" ];
  then
    echo QUEUE_URL not specified in the environment and could not be found automatically. The queue must be called ICCMO.fifo and must be in the current region or URL must be passed in as an environment variable.
    exit 1
  fi
fi

MESSAGE=$(cat sqs-iridium-example.json)
EPOCH_TIME=$(date +%s)
MESSAGE=${MESSAGE//EPOCH_TIME/$EPOCH_TIME}
MESSAGE=${MESSAGE//UUID/$UUID}
aws sqs send-message --queue-url "$QUEUE_URL" --message-body "$MESSAGE" --message-group-id $(uuidgen) --message-deduplication-id $(uuidgen)
