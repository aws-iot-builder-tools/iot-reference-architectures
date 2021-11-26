#!/usr/bin/env bash

aws logs describe-log-groups --query 'logGroups[*].logGroupName' --output text | tr '\t' '\n' | parallel aws logs delete-log-group --log-group-name
