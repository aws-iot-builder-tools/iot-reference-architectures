#!/usr/bin/env bash

# Move to the directory that the script is in so the relative paths will work from anywhere
cd "$(dirname "$0")" || exit

CURRENT_GROUP_ID=$(id -gn)
CURRENT_USER_ID=$(id -un)

#sudo -E java -Droot="greengrass/v2" -Dlog.store=FILE \
if [ -d greengrass/v2 ]; then
    echo "Greengrass core already installed"
else
  rm -rf GreengrassInstaller
  curl -s https://d2s8p88vqu9w66.cloudfront.net/releases/greengrass-nucleus-latest.zip > greengrass-nucleus-latest.zip
  unzip greengrass-nucleus-latest.zip -d GreengrassInstaller && rm greengrass-nucleus-latest.zip
  java -Droot="greengrass/v2" -Dlog.store=FILE \
    -jar ./GreengrassInstaller/lib/Greengrass.jar \
    --aws-region us-east-1 \
    --thing-name $(uuidgen) \
    --thing-group-name $(uuidgen) \
    --thing-policy-name GreengrassV2IoTThingPolicy \
    --tes-role-name GreengrassV2TokenExchangeRole \
    --tes-role-alias-name GreengrassCoreTokenExchangeRoleAlias \
    --component-default-user $CURRENT_GROUP_ID:$CURRENT_USER_ID \
    --provision true \
    --deploy-dev-tools true
fi

while true; do
  sleep 1
  java -Droot="greengrass/v2" -Dlog.store=FILE \
    -jar ./GreengrassInstaller/lib/Greengrass.jar \
    --aws-region us-east-1 \
    --component-default-user $CURRENT_GROUP_ID:$CURRENT_USER_ID
done

