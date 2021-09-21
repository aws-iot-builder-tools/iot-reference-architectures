import json


def handler(event, context):
    policy = {
        "isAuthenticated": True,
        "principalId": "*",
        "disconnectAfterInSeconds": 86400,
        "refreshAfterInSeconds": 300,
        "policyDocuments": [
            {
                "Version": "2012-10-17",
                "Statement": [
                    {
                        "Action": "iot:*",
                        "Effect": "Allow",
                        "Resource": "*"
                    }
                ]
            }
        ]
    }
    print(f'policy: {policy}')
    return json.dumps(policy)