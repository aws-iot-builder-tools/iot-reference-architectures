#!/usr/bin/env python3

try:
    from AWSIoTPythonSDK.MQTTLib import AWSIoTMQTTClient
    from os import path
    from time import sleep
    import boto3
    import requests
    import argparse
    import uuid
except ImportError as error:
    print(
        'Looks like some dependencies are missing. Try running "pip3 install -r requirements.txt" and then run this script again.')
    quit()


def setup():
    root_ca_file = "root-ca.pem"

    # Get the CA cert if we need it
    if not path.exists(root_ca_file):
        request = requests.get('https://www.amazontrust.com/repository/AmazonRootCA1.pem', allow_redirects=True)
        open(root_ca_file, 'wb').write(request.content)

    # Get the hostname for the customer's ATS endpoint in this region
    iot_client = boto3.client('iot')
    iot_data = iot_client.describe_endpoint(endpointType='iot:Data-ATS')
    host = iot_data['endpointAddress']

    # Get temporary credentials
    sts_client = boto3.client('sts')
    sts_data = sts_client.get_session_token()

    # Extract the temporary credential values
    credentials = sts_data['Credentials']
    access_key_id = credentials['AccessKeyId']
    secret_access_key = credentials['SecretAccessKey']
    session_token = credentials['SessionToken']

    # Create an MQTT client with a random client ID that uses WebSockets
    mqtt_client = AWSIoTMQTTClient(str(uuid.uuid4()), useWebsocket=True)
    # Use port 443 + ALPN on the customer's ATS endpoint
    mqtt_client.configureEndpoint(host, 443)
    # Explicitly enable the root CA we downloaded
    mqtt_client.configureCredentials(root_ca_file)

    # Configure the library to use the specified credentials instead of using another provider to fetch them
    AWSIoTMQTTClient.configureIAMCredentials(mqtt_client, access_key_id, secret_access_key, session_token)

    return mqtt_client
