#!/usr/bin/env bash

# Guidance from: https://stackoverflow.com/a/15340661
#bash -c 'cat < /dev/null > /dev/tcp/127.0.0.1/8080; echo $?'
HOST=127.0.0.1
PORT=8080
(exec 3<>/dev/tcp/$HOST/$PORT) &>/dev/null
FINISHED=$?

if [ $FINISHED -ne 0 ]; then
  echo Waiting for Jetty to start...

  while [ $FINISHED -ne 0 ]; do
    (exec 3<>/dev/tcp/$HOST/$PORT) &>/dev/null
    FINISHED=$?
    sleep 1
  done
fi

./gradlew gwtSuperDev $@
