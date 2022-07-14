#!/usr/bin/env bash

set -e

REPO_AND_TAG=$1

if [ -z "$REPO_AND_TAG" ]; then
  echo "Repository and tag must be specified as a single parameter for this script (e.g. 'myfunction:latest')"
  exit 1
fi

# Make sure we are in the correct directory if we are called from elsewhere or the Docker build will fail
cd "$(dirname "$0")"

docker build --progress plain -t "$REPO_AND_TAG" .
