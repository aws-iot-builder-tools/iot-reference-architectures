import json
import boto3
import os
from cryptography import x509
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import ec
from datetime import datetime

client = boto3.client('iot')

## Get Root CA from S3 bucket
s3_bucket_containing_root_ca = os.environ.get('S3_BUCKET_NAME_ROOT_CA')
s3 = boto3.resource('s3')
s3.meta.client.download_file(s3_bucket_containing_root_ca, 'root_ca.pem', '/tmp/root_ca.pem')
root_ca_pem = ""

with open ('/tmp/root_ca.pem') as file:
    lines = file.readlines()
    for line in lines:
        root_ca_pem += line

## Get managed IoT Policy from environment variable
managed_iot_policy = os.environ.get('IOT_POLICY')

## Get attributes from environment variable
thing_attribute_name = os.environ.get('THING_ATTRIBUTE_NAME')

## Deletes all created AWS IoT resources in the Lambda by implementing All or Nothing
def cleanup_resources(certificateArnToDelete=None, certificateIdToDelete=None, thingNameToDelete=None):
    
    if (thingNameToDelete is not None):
        response = client.detach_thing_principal(
            thingName=thingNameToDelete,
            principal=certificateArnToDelete
        )
    if (certificateIdToDelete is not None):
        response = client.update_certificate(
            certificateId=certificateIdToDelete,
            newStatus='INACTIVE'
        )
    if (certificateArnToDelete is not None):
        response = client.delete_certificate(
            certificateId=certificateIdToDelete,
            forceDelete=True
        )
    if (thingNameToDelete is not None):
        response = client.delete_thing(
            thingName=thingNameToDelete
        )

## Verify signature of X509 certificate to be registered against the Root CA
def verify_signature(cert):
    root_ca = {}
    try:
        root_ca = x509.load_pem_x509_certificate(root_ca_pem.encode('utf-8'), default_backend())
        public_key = root_ca.public_key()
        public_key.verify(
            cert.signature,
            cert.tbs_certificate_bytes,
            ec.ECDSA(hashes.SHA256())
        )
    except Exception as e:
        print(e)
        print ('Signature could not be verified')
        return False
        
    return True

## Verify that the certificate date is valid
def verify_date(cert):
    today = datetime.today()
    return cert.not_valid_before < today < cert.not_valid_after

## Verify that certificate is valid.
def verify_certificate(certificatePEM) :
    cert = {}
    try:
        cert = x509.load_pem_x509_certificate(certificatePEM.encode('utf-8'), default_backend())
    except Exception as e:
        print (e)
        print ('Invalid certificate')
        return False
        
    if not (verify_signature(cert)):
        print ('Unable to verify signature')
        return False
    if not (verify_date(cert)):
        print ('Certificate dates invalid')
        return False
        
    return True
    
    
def lambda_handler(event, context):
    
    global client
    
    region = 'us-east-1' #default
    certificatePEM = ""
    attribute_1 = "undefined"

    body = json.loads(event['body'])
    
    if 'region' in body.keys():
        region=body['region']
    
    if 'certificate' in body.keys():
        certificatePEM = body['certificate']
        
    if thing_attribute_name in body.keys():
        attribute_1 = body[thing_attribute_name]

    client = boto3.client('iot', region_name=region)
    
    if not verify_certificate(certificatePEM):
        return {
            'statusCode': 400,
            'body': json.dumps('Failed to verify certificate.')
        }
        
    try:
        response = client.register_certificate_without_ca(
            certificatePem=certificatePEM,
            status='ACTIVE'
        )
    except Exception as e:
        return {
            'statusCode': 400,
            'body': json.dumps('Failed to register certificate: {}'.format(e))
        }
    certificateArn = response['certificateArn']
    certificateId = response['certificateId']
    
    try:
        response = client.create_thing(
            thingName = certificateId,
            attributePayload={
                'attributes': {
                    thing_attribute_name : attribute_1
                },
                'merge': True
            }
        )
    except Exception as e:
        print ('Failed to Create Thing')
        cleanup_resources(certificateArn, certificateId)
        return {
            'statusCode': 400,
            'body': json.dumps('Failed to create thing: {}'.format(e))
        }
    thingName = response['thingName']
    thingArn = response['thingArn']
    
    try:
        response = client.attach_principal_policy(
            policyName=managed_iot_policy,
            principal=certificateArn
        )
    except Exception as e:
        print ('Failed to attach policy to certificate')
        cleanup_resources(certificateArn, certificateId, thingName)
        return {
            'statusCode': 400,
            'body': json.dumps('Failed to attach policy to cert: {}'.format(e))
        }
        
    try:
        response = client.attach_thing_principal(
            thingName=thingName,
            principal=certificateArn
        )
    except Exception as e:
        print ('Failed to attach certificate to Thing')
        cleanup_resources(certificateArn, certificateId, thingName)
        return {
            'statusCode': 400,
            'body': json.dumps('Failed to attach cert to thing: {}'.format(e))
        }
    
    response = client.describe_endpoint(
        endpointType='iot:Data-ATS'
    )
    endpointAddress = {
        'endpointAddress': response['endpointAddress']
    }
    
    print ('Successfully created thing, registered certificate.')
    return {
        'statusCode': 200,
        'body': json.dumps(endpointAddress)
    }