#!/usr/bin/env bash
#Update this to the region where the API will be deployed
AWS_REGION="us-east-1"
#Bucket name where artifacts will be deployed
BUCKET_NAME="iot-thing-registration-api-$(date +%Y%m%d%M%s)"
#Update this to the desired name of the attribute for each Thing
ATTRIBUTE_NAME="cloud_managed_id"
#Update this to include the ZTP partner cloud service ID
CLOUD_MANAGED_SERVICE_ACCOUNT_ID="CHANGE ME"

# Make the bucket
aws s3 mb s3://$BUCKET_NAME --region $AWS_REGION
# Upload the root CA
aws s3 cp root_ca.pem s3://$BUCKET_NAME 

# Upload the required artifacts for deployment and package the cloudformation template.
aws cloudformation package --template ./CloudFormation-Customer-Account.yaml --s3-bucket $BUCKET_NAME --output-template-file packaged-template.yaml

# Replace parameters in packaged-template.yaml
sed -i'' -e "s/<MY_BUCKET>/$BUCKET_NAME/g" packaged-template.yaml
sed -i'' -e "s/<ATTRIBUTE_NAME>/$ATTRIBUTE_NAME/g" packaged-template.yaml

# Deploy stack
aws cloudformation deploy --template-file ./packaged-template.yaml --stack-name RegisterThingAPIStack --parameter-overrides "PartnerAccountParameter=$CLOUD_MANAGED_SERVICE_ACCOUNT_ID" --capabilities CAPABILITY_NAMED_IAM --region $AWS_REGION

#Display outputs
aws cloudformation describe-stacks --stack-name RegisterThingAPIStack --region $AWS_REGION --query "Stacks[0].Outputs"
