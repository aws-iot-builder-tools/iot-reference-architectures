#!/usr/bin/bash env

AWS_CLI_ERROR_MESSAGE_PREFIX="No"
AWS_CLI_ERROR_MESSAGE_SUFFIX="found via aws configure get, do you have the AWS CLI configured on this system?"

# Allow failures, we will catch them
set +e

if [ -f /sys/hypervisor/uuid ] && [ `head -c 3 /sys/hypervisor/uuid` == ec2 ]; then
  # Running on EC2
  command -v jq >/dev/null 2>&1 || {
    echo >&2 "jq is required to assume a role but it's not installed. Aborting."
    exit 1
  }

  command -v curl >/dev/null 2>&1 || {
    echo >&2 "jq is required to assume a role but it's not installed. Aborting."
    exit 1
  }

  CREDENTIAL_JSON=$(TOKEN=`curl -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 21600"` && curl -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/identity-credentials/ec2/security-credentials/ec2-instance/)
  if [ $? -eq 0 ]; then
    export AWS_ACCESS_KEY_ID=$(jq --raw-output .credentials.accessKeyId <(echo $CREDENTIALS))
    export AWS_SECRET_ACCESS_KEY=$(jq --raw-output .credentials.secretAccessKey <(echo $CREDENTIALS))
    export AWS_SESSION_TOKEN=$(jq --raw-output .credentials.sessionToken <(echo $CREDENTIALS))
  else
    echo >&2 "Requesting credentials from EC2 instance metadata failed, do you have a role attached to this instance?"
    exit 1
  fi
else
  # Not running on EC2
  if [ ! command -v aws ] &>/dev/null; then
    echo >&2 "AWS CLI must be installed and configured for deployments"
    exit 1
  fi

  # Is the AWS CLI configured?
  AWS_ACCESS_KEY_ID=$(aws configure get aws_access_key_id)

  if [ $? -ne 0 ]; then
    echo >&2 $AWS_CLI_ERROR_MESSAGE_PREFIX access key ID $AWS_CLI_ERROR_MESSAGE_SUFFIX
    exit 1
  fi

  AWS_SECRET_ACCESS_KEY=$(aws configure get aws_secret_access_key)

  if [ $? -ne 0 ]; then
    echo >&2 $AWS_CLI_ERROR_MESSAGE_PREFIX secret access key $AWS_CLI_ERROR_MESSAGE_SUFFIX
    exit 1
  fi

  REGION=$(aws configure get region)

  if [ $? -ne 0 ]; then
    echo >&2 $AWS_CLI_ERROR_MESSAGE_PREFIX region $AWS_CLI_ERROR_MESSAGE_SUFFIX
    exit 1
  fi
fi

export AWS_ACCESS_KEY_ID
export AWS_SECRET_ACCESS_KEY
export AWS_SESSION_TOKEN
export REGION
