#!/usr/bin/env python3

import boto3
import json
from datetime import datetime, timedelta
import math

import argparse

import requests
from OpenSSL import crypto

parser = argparse.ArgumentParser(
    description='Publish a message to IoT Core using the endpoint and topic information embedded in a certificate')
parser.add_argument('--cert', help='The file that contains the certificate', type=ascii)
parser.add_argument('--key',
                    help='The file that contains the private key. Optional if the certificate key\'s filename ends in \'.key\' and is otherwise the same as the certificate\'s filename (e.g. iot.pem and iot.key).',
                    type=ascii)

args = parser.parse_args()

if args.cert is None:
    raise Exception("The path to the certificate is a required argument")

if args.key is None:
    args.key = str(args.cert)
    extension_index = args.key.rindex(".")
    args.key = args.key[:extension_index] + ".key"

args.cert = args.cert.replace("'", "")
args.key = args.key.replace("'", "")

cert = crypto.load_certificate(crypto.FILETYPE_PEM, open(args.cert).read())

endpoint = cert.get_issuer().CN
topic = cert.get_subject().CN.replace("%2F", "/")

publish_url = 'https://' + endpoint + ':8443/topics/' + topic + '?qos=1'

def cross_account_publish(payload):
    headers = {'x-amzn-platform': 'APN/1 partner,solution'}
    
    publish = requests.request('POST', publish_url, data=json.dumps(payload), cert=[args.cert, args.key], headers=headers)

payload = {}
payload['message'] = "Hello, world!"
cross_account_publish(payload)
