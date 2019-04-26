#!/usr/bin/env bash

set -x
set -e

find . -name "build.sh" -mindepth 2 -exec sh -c 'cd `dirname {}` ; ./build.sh' \;
