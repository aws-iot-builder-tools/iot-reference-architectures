import json


def handler(event, context):
    print(f'event: {event}')
    print(f'context: {context}')
    policy = {
        "Version": "2012-10-17",
        "Statement": [
            {
                "Action": "iot:*",
                "Effect": "Allow",
                "Resource": ["*"]
            }
        ]
    }
    print(f'policy: {policy}')
    return policy