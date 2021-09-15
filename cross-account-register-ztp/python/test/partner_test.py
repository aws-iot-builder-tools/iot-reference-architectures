import boto3
from boto3.session import Session
from requests_aws4auth import AWS4Auth
import requests
import json

def assume_role(arn, session_name):

    client = boto3.client('sts')
    response = client.assume_role(RoleArn=arn, RoleSessionName=session_name)
     
    session = Session(aws_access_key_id=response['Credentials']['AccessKeyId'],
                      aws_secret_access_key=response['Credentials']['SecretAccessKey'],
                      aws_session_token=response['Credentials']['SessionToken'])
    return response
    

sts_response = assume_role('arn:aws:iam::123456789012:role/CloudManagedRoleForAPIAccess','testsession')

method = 'POST'
headers = {}
body = {
  "region": "us-east-1",
  "certificate": "-----BEGIN CERTIFICATE-----\nMIIB5TCCAYoCCQD0QhU41DbVtzAKBggqhkjOPQQDAjBzMQswCQYDVQQGEwJVUzEQ\nMA4GA1UECAwHVVNTdGF0ZTEQMA4GA1UEBwwHQW55dG93bjEXMBUGA1UECgwORHVt\nbXlDb3JwIEx0ZC4xEzARBgNVBAsMCklvVFNlY3Rpb24xEjAQBgNVBAMMCUR1bW15\nQ29ycDAeFw0yMTA5MTUwMjAyMDFaFw0yNDA2MTEwMjAyMDFaMIGAMQswCQYDVQQG\nEwJVUzETMBEGA1UECAwKV2FzaGluZ3RvbjEQMA4GA1UEBwwHU2VhdHRsZTEQMA4G\nA1UECgwHSW9UQ29ycDEXMBUGA1UECwwOSW9UQ29ycERldmljZXMxHzAdBgNVBAMM\nFklvVENvcnBJb1RQbGF0Zm9ybS5jb20wWTATBgcqhkjOPQIBBggqhkjOPQMBBwNC\nAAQjOn6W5yp/2WKj5avLAbR7SL7cnVsoEwZjep9gPX+jRccNmTgh/CnqtMtHgDN4\nWB+lgwdHsf5Oi5dKel0S3RjxMAoGCCqGSM49BAMCA0kAMEYCIQDWYHe5WtuhE7i1\n2DMIZMNSHrvzYh10HUPY1XvKMf+gTwIhALxeRADj8KEAN0+E133Eek0p3Yb7IO9i\n2vym8IH/Pu4C\n-----END CERTIFICATE-----",
  "cloud_managed_id": "123456789"
}

service = 'execute-api'
url = 'https://abcdefghij.execute-api.us-east-1.amazonaws.com/Prod/register'
region = 'us-east-1'

auth = AWS4Auth(sts_response['Credentials']['AccessKeyId'], sts_response['Credentials']['SecretAccessKey'], region, service, sts_response['Credentials']['SessionToken'])
response = requests.request(method, url, auth=auth, data=json.dumps(body), headers=headers)
print(response.text)