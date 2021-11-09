#!/usr/bin/env bash

./logs.sh | grep ": stderr." | sed "s/^.*: stderr. //"
