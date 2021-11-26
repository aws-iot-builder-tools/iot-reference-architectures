#!/usr/bin/env bash

IP=$(../../get-ip.sh)

ssh ubuntu@$IP -L 1441:localhost:1441 -L 1442:localhost:1442
