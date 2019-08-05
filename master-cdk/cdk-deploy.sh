#!/usr/bin/env bash

#cdk deploy --context CustomerAccountParameter=541589084637,PartnerAccountParameter=541589084637
cdk --context PartnerAccountParameter=541589084637 deploy
