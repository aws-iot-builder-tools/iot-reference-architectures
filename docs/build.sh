#!/usr/bin/env bash

# Requires markdown-toc -- npm install -g markdown-toc
cp aws-iot-basics-without-toc.md aws-iot-basics.md
markdown-toc -i aws-iot-basics.md
