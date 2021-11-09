#!/usr/bin/env bash

./logs.sh | grep ": stdout." | sed "s/^.*: stdout. //"
