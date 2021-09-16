import os
import json
import base64

account = os.environ['ACCOUNT']
region = os.environ['REGION']

def handler(event, context):
    # https://docs.aws.amazon.com/iot/latest/developerguide/config-custom-auth.html#custom-auth-lambda
    print(f'event: json.dumps(event)')
    username = event['protocolData']['mqtt']['username']
    password = base64.b64decode(event['protocolData']['mqtt']['password'].encode('ascii')).decode('ascii')
    clientId = event['protocolData']['mqtt']['clientId']
    print(f'username: {username}')
    # print(f'password: {password}')
    print(f'clientId: {clientId}')
    policy = {
        "isAuthenticated": True,
        "principalId": f"{clientId}",
        "disconnectAfterInSeconds": 86400,
        "refreshAfterInSeconds": 300,
        "policyDocuments": [
            {
                "Version": "2012-10-17",
                "Statement": [
                    {
                        "Action": "iot:Connect",
                        "Effect": "Allow",
                        "Resource": f"arn:aws:iot:{region}:{account}:client/*"
                    },
                    {
                        "Action": "iot:Publish",
                        "Effect": "Allow",
                        "Resource": f"arn:aws:iot:{region}:{account}:topic/*"
                    },
                    {
                        "Action": "iot:Subscribe",
                        "Effect": "Allow",
                        "Resource": f"arn:aws:iot:{region}:{account}:topicfilter/*"
                    },
                    {
                        "Action": "iot:Receive",
                        "Effect": "Allow",
                        "Resource": f"arn:aws:iot:{region}:{account}:topic/*"
                    }
                ]
            }
        ]
    }
    print(f'policy: {policy}')
    return json.dumps(policy)