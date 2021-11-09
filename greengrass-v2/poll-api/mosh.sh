#!/usr/bin/env bash

IP=$(../../get-ip.sh)

mosh ubuntu@$IP $@
