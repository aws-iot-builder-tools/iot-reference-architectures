# AWS IoT Basics

Questions, suggestions, problems? Create an issue!

**NOTE: Commands listed were validated in Mac OS. They may work in Linux and CygWin. They will not work in Windows/PowerSHELL**

# AWS IoT

## Useful OpenSSL commands

### Read the contents of a CSR

```
openssl req -in thing.csr -noout -text
```

### Read the contents of a certificate

```
openssl x509 -in thing.crt -noout -text
```

### Get the fingerprint of a certificate

```
openssl x509 -in thing.crt -noout -text -sha256 -fingerprint | grep Fingerprint | sed -e 's/SHA256 Fingerprint=//' -e 's/://g' | tr [A-F] [a-f]
```

### Look up a certificate in AWS IoT

```
aws iot describe-certificate --certificate-id `openssl x509 -in temp.crt -noout -text -sha256 -fingerprint | grep Fingerprint | sed -e 's/SHA256 Fingerprint=//' -e 's/://g' | tr [A-F] [a-f]`
```

## Useful mosquitto commands

Follow the instructions in the [Configuring an external MQTT client](https://quip-amazon.com/1NhgAA8Kmi54#GUU9CAr0TYo) section to set up your credentials for the Mosquitto first.

### Subscribe to a topic

Subscribe to the `hello/world` topic:

```
mosquitto_sub -h `aws iot describe-endpoint --output text` --cafile root-ca.crt --cert mqtt-client.crt --key mqtt-client-private.key -p 8883 -t hello/world
```

### Publish a message to a topic

Publish a message to the `hello/world` topic:

```
mosquitto_pub -h `aws iot describe-endpoint --output text` --cafile root-ca.crt --cert mqtt-client.crt --key mqtt-client-private.key -p 8883 -t hello/world -m "Hi there!"
```

### Publish a message to a topic from a file (works for text or binary data)

Publish the contents of the file `input_file` to the `hello/world` topic:

```
mosquitto_pub -h `aws iot describe-endpoint --output text` --cafile root-ca.crt --cert mqtt-client.crt --key mqtt-client-private.key -p 8883 -t hello/world -f input_file
```

## Getting the root CA

You may need to get the AWS IoT root CA for your device(s). You can fetch it using cURL.

Command:

```
curl -o root-ca.crt https://www.symantec.com/content/en/us/enterprise/verisign/roots/VeriSign-Class%203-Public-Primary-Certification-Authority-G5.pem
```

## Setting up an IAM user for AWS IoT only

Assign the AWSIoTFullAccess policy to your user. It looks like this and allows all access to AWS IoT, in all regions, in your account:

```
{
    "Statement": [
        {
            "Action": [
                "iot:*"
            ],
            "Effect": "Allow",
            "Resource": "*"
        }
    ],
    "Version": "2012-10-17"
}
```

## Configuring an external MQTT client

It can be helpful to use an external MQTT client (e.g. MQTT.fx, mqtt-spy) to develop with AWS IoT. You can do this in three steps.

### Create a policy for your external MQTT client to make sure it can only connect, publish, subscribe, and receive messages

Command:

```
aws --output=text iot create-keys-and-certificate --set-as-active --certificate-pem-outfile mqtt-client.crt --private-key-outfile mqtt-client-private.key --public-key-outfile mqtt-client-public.key --query certificateArn > mqtt-client.cert.arn
```

Result, just the certificate ID stored in mqtt-client.cert.arn which looks like this:

```
arn:aws:iot:us-east-1:123456789012:cert/cf8ef62a65b58d6ef0703913c8780617d34bf623395de1d59c5ffb28b93b08d3
```

### Attach the policy to the certificate

```
aws iot attach-principal-policy --policy-name mqtt-client-policy --principal `cat mqtt-client.cert.arn`
```

## Configuring a thing

### Create a thing

Command:

```
aws iot create-thing --thing-name test-thing-1
```

Result:

```
{
    "thingArn": "arn:aws:iot:us-east-1:123456789012:thing/test-thing-1",
    "thingName": "test-thing-1"
}
```

### Create a permissive policy for that thing

Command:

```
aws iot create-policy --policy-name test-thing-1-policy --policy-document '{ "Version": "2012-10-17", "Statement": [{ "Effect": "Allow", "Action":["iot:*"], "Resource": ["*"] }] }'
```

Result:

```
{
    "policyArn": "arn:aws:iot:us-east-1:123456789012:policy/test-thing-1-policy",
    "policyDocument": "{ \"Version\": \"2012-10-17\", \"Statement\": [{ \"Effect\": \"Allow\", \"Action\":[\"iot:*\"], \"Resource\": [\"*\"] }] }",
    "policyName": "test-thing-1-policy",
    "policyVersionId": "1"
}
```

### Create keys and certificate for that thing

Command:

```
aws --output=text iot create-keys-and-certificate --set-as-active --certificate-pem-outfile thing.crt --private-key-outfile thing.key --query certificateArn > thing.cert.arn
```

Result, just the certificate ID stored in thing.cert.arn which looks like this:

```
arn:aws:iot:us-east-1:123456789012:cert/cf8ef62a65b58d6ef0703913c8780617d34bf623395de1d59c5ffb28b93b08d3
```

### Attach the policy to that certificate

Command:

```
aws iot attach-principal-policy --policy-name test-thing-1-policy --principal `cat thing.cert.arn`
```

Result: None, unless there is an error

Now that certificate can connect to AWS IoT via MQTT, publish and subscribe, and work with the shadow. You can test this with [MQTT.fx](http://mqttfx.jfx4ee.org/) - to make sure everything works.

### (Optional) Attach the certificate to the thing

Command:

```
aws iot attach-thing-principal --thing-name test-thing-1 --principal `cat thing.cert.arn`
```

This optional step is just for bookkeeping purposes. Future features may make use of this information.

## Generating a certificate and CSR with OpenSSL

### Create the thing's private key

```
openssl genrsa -out thing.key 4096
```

### Create a certificate signing request (CSR) with the private key

```
openssl req -new -key thing.key -out thing.csr -subj "/CN=thing"
```

### Signing your own certificate with the AWS IoT CA

```
aws iot create-certificate-from-csr --certificate-signing-request file://thing.csr --certificate-pem-outfile thing.crt
```

## Creating your own certificate authority

### Get the CA registration code for your account and store it in a file

Command:

```
aws iot get-registration-code --output text > registration-code.txt
```

### Create the root CA key

Command:

```
openssl genrsa -out root-ca.key 4096
```

### Create the root CA certificate

Command:

```
openssl req -x509 -new -nodes -key root-ca.key -sha256 -subj "/CN=root-ca-1" -days 365 -out root-ca.crt
```

### Create the root CA verification key

Command:

```
openssl genrsa -out root-ca-verification.key 4096
```

### Create a CSR for the root CA verification key with the registration code as the common name (CN) in the subject field

Command:

```
openssl req -new -subj "/CN=`cat registration-code.txt`" -key root-ca-verification.key -out root-ca-verification.csr
```

### Sign the root CA verification CSR with the root CA

Command:

```
openssl x509 -req -in root-ca-verification.csr -CA root-ca.crt -CAkey root-ca.key -CAcreateserial -out root-ca-verification.crt -days 365 -sha256
```

### Register the root CA by providing the root CA certificate with the root CA verification CSR, allow its certificates to be automatically registered (JITR), and mark it active

Command:

```
aws iot register-ca-certificate --ca-certificate "`cat root-ca.crt`" --verification-certificate "`cat root-ca-verification.crt`" --allow-auto-registration --set-as-active
```

NOTE: This command may fail for one of two different reasons.

**Reason #1: CA certificate is registered in another account in the same region**

The CA certificate must not be registered in another account in the same region. If it is the error reported will be:

```
An error occurred (InvalidRequestException) when calling the RegisterCACertificate operation: Cannot accept this CACertificate
```

**Reason #2: 10 certificate authorities are already registered in this account with the same subject name**

You may register up to 10 CAs with the same subject name. If you already have 10 and try to register another one with the same subject name the error reported will be:

```
An error occurred (LimitExceededException) when calling the RegisterCACertificate operation: Limit reached for number of CA Certificates with same subject
```

## Signing a thing's certificate signing request with your own certificate authority

Follow the instructions in the [Generating a certificate and CSR with OpenSSL](https://quip-amazon.com/1NhgAA8Kmi54#GUU9CAuC3Bb) section to generate your CSR

### Sign the CSR with the certificate authority

Command:

```
openssl x509 -req -in thing.csr -CA root-ca.crt -CAkey root-ca.key -CAcreateserial -out thing.crt -days 365 -sha256
```

## Registering a thing's signed certificate with AWS IoT

### Register the thing's signed certificate with AWS IoT and mark it as active

Command:

```
aws iot register-certificate --certificate-pem "`cat thing.crt`" --ca-certificate-pem "`cat root-ca.crt`" --set-as-active
```

NOTE: This command may fail for the following reason.

**Certificate is registered in another account in the same region**

The certificate must not be registered in another account in the same region. If it is the error reported will be:

```
An error occurred (InvalidRequestException) when calling the RegisterCertificate operation: This request is not valid
```

## CLI usage

Assumption: Thing is named `rpi3_001` for all commands in this section

### Describing a thing

Command:

```
aws iot describe-thing --thing-name rpi3_001
```

Example output:

```
{
    "attributes": {},
    "version": 1,
    "thingName": "rpi3_001",
    "defaultClientId": "rpi3_001"
}
```

### Adding an attribute to a thing

Add an attribute called `location` with a value of `NYC`. `"merge": true` indicates that attributes should be updated or created but not removed.

Command:

```
aws iot update-thing --thing-name rpi3_001 --attribute-payload "{ \"attributes\": { \"location\": \"NYC\" }, \"merge\": true }"
```

## iot-data

### Publishing a simple JSON message

**AWS CLI**

```
aws iot-data publish --topic TOPIC --payload '{"message":"hello"}'
```

**AWS Lambda with Python 2.7**

NOTE: Your Lambda function will need appropriate permissions to publish to AWS IoT or you will get a "Forbidden" error message

```
import boto3
import json

message = 'Hello, world!'
topic = 'topic'
client = boto3.client('iot-data')
payload = {}
payload['data'] = message
payload = json.dumps(payload)

def lambda_handler(event, context):
  response = client.publish(topic=topic, qos=0, payload=payload)
  return message
```

## TODO: Policies

### Basic policies

[Docs for thing attributes in policies](http://docs.aws.amazon.com/iot/latest/developerguide/thing-policy-variables.html)

### Policies using certificate attributes

[Docs for certificate attributes in policies](http://docs.aws.amazon.com/iot/latest/developerguide/cert-policy-variables.html)

## TODO: Rules engine

TODO: Coming soon!
TODO: Inline JSON (e.g. select blah as {'test': 'hello'} from input)

## TODO: Lifecycle events

TODO: Coming soon!

## TODO: JITR

TODO: Coming soon!
TODO: How to include the certificate chain with a public signed certificate

## TODO: AWS IoT Device Management and Thing Groups

TODO: Coming soon!

# Greengrass

## Discovery

### **Doing discovery manually with the AWS CLI**

NOTE: To do this you'll need a Greengrass Core that has been deployed, a Greengrass Device that has been associated with the Greengrass Core and has the proper policies set up, and the certificates for the Greengrass Device on your local machine.

### **Step 1: Convert the Greengrass Device's certificate and private key to PKCS12 format with OpenSSL**

This command will convert thing.crt and thing.key into thing.p12 with the password of "pass".

**MacOS**

```
openssl pkcs12 -export -in thing.crt -inkey thing.key -out thing.p12 -password pass:pass
```

**Linux**

```
openssl pkcs12 -export -in thing.crt -inkey thing.key -out thing.p12 -password pass:pass
openssl pkcs12 -in thing.p12 -out thing.pem -password pass:pass
```

### **Step 2: Hit the discovery API with cURL using the client side certificate**

NOTE: Replace THING_NAME with the name of your Greengrass Device

**MacOS**

```
curl --cert thing.p12:pass https://`aws iot describe-endpoint --output text`:8443/greengrass/discover/thing/THING_NAME
```

**Linux**

```
curl --cert thing.pem:pass https://`aws iot describe-endpoint --output text`:8443/greengrass/discover/thing/THING_NAME
```

The output will be a JSON document that is similar to this (NOTE: The output is not pretty printed like the object below):

```
{
    "GGGroups": [
        {
            "CAs": [
                "-----BEGIN CERTIFICATE-----\nMIIEFDCCAvygAwIBAgIUDCcE9E/Q3Y+xMPaZnubkFJ8sQwcwDQYJKoZIhvcNAQEL\nBQAwgagxCzAJBgNVBAYTAlVTMRgwFgYDVQQKDA9BbWF6b24uY29tIEluYy4xHDAa\nBgNVBAsME0FtYXpvbiBXZWIgU2VydmljZXMxEzARBgNVBAgMCldhc2hpbmd0b24x\nEDAOBgNVBAcMB1NlYXR0bGUxOjA4BgNVBAMMMTU0MTU4OTA4NDYzNzo0MDVkNTYx\nNy0zN2U2LTQ5NmUtYjMzOC01NjU4MzhkYjNjNjYwIBcNMTcwNjAyMTI1MzI2WhgP\nMjA5NzA2MDIxMjUzMjVaMIGoMQswCQYDVQQGEwJVUzEYMBYGA1UECgwPQW1hem9u\nLmNvbSBJbmMuMRwwGgYDVQQLDBNBbWF6b24gV2ViIFNlcnZpY2VzMRMwEQYDVQQI\nDApXYXNoaW5ndG9uMRAwDgYDVQQHDAdTZWF0dGxlMTowOAYDVQQDDDE1NDE1ODkw\nODQ2Mzc6NDA1ZDU2MTctMzdlNi00OTZlLWIzMzgtNTY1ODM4ZGIzYzY2MIIBIjAN\nBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAggIYZqU9/fUqAY6ISiJ640lKTe0A\nzNJxx+q6akmeusAtJxCDTZDM9gRse9tQdNK9VkhbeyBeY6yRto/thwbxUiD8o6jN\nwjoKWeX0wsvLXgYMFQ92CpUU9U1WjcGaRA+/ClcQFj/4ZTYlnuWFPLWQ3CT3QOI8\nNJfbsVT0lEeFdZcRLmdWILCFKKsl3cTktpshUD7d+agpHD05UXs5JBnlmZIP/keV\n0EC/UpWESNTluZFxWR4q+Zu0Scxz98ptJ4I7uxePCEOsl6dN6CKS9sv7OndNhkMt\nlkRvS3rBwK2HR8Pe6rJirhh0+SG4iAZ8x9IKotjvmNLDHa9nb4jAoEOhOQIDAQAB\nozIwMDAPBgNVHRMBAf8EBTADAQH/MB0GA1UdDgQWBBQ4lJVGmyyiQfj6FaIfU8NF\nuH7CEDANBgkqhkiG9w0BAQsFAAOCAQEAPt26QOys7Byz4BrgUEy9fngJqnuLOGfM\nWYvvF/J1An/haNRLfDB0whD/DM08xOTo+wPxoMbpWxu06AfDRRydYfWXTXfbp9cu\nXrNJawpSp0kGAPXJbF/7QkMlaXtYyIz6eG/r9SD5pda54DHEDAAc1oESxuqJe/r0\ny4yEyUczqUoPS8YBWKbikj6QoLg62fJm/6TxWRtIlXcKY3V5zavGUXKK9zZ3lWJ3\nDcfH8Y84WXxrUcQWizZslkKG7CwVR1ThA5WbZ4co+IMWrcYVoS6bxRgiuWU1D85X\nopeI5t+K7FOBeAdusL9PcTBP1WA6HUEdppvKDDSalDvy5efAhPDgew==\n-----END CERTIFICATE-----\n"
            ],
            "Cores": [
                {
                    "Connectivity": [
                        {
                            "HostAddress": "127.0.0.1",
                            "Id": "AUTOIP_127.0.0.1",
                            "Metadata": "",
                            "PortNumber": 8883
                        },
                        {
                            "HostAddress": "192.168.1.132",
                            "Id": "AUTOIP_192.168.1.132",
                            "Metadata": "",
                            "PortNumber": 8883
                        },
                        {
                            "HostAddress": "::1",
                            "Id": "AUTOIP_::1",
                            "Metadata": "",
                            "PortNumber": 8883
                        },
                        {
                            "HostAddress": "fe80::481c:dde6:5810:9396",
                            "Id": "AUTOIP_fe80::481c:dde6:5810:9396",
                            "Metadata": "",
                            "PortNumber": 8883
                        },
                        {
                            "HostAddress": "fe80::cc54:be99:39d7:1515",
                            "Id": "AUTOIP_fe80::cc54:be99:39d7:1515",
                            "Metadata": "",
                            "PortNumber": 8883
                        }
                    ],
                    "thingArn": "arn:aws:iot:us-west-2:xxxxxxxxxxxx:thing/CORE_NAME"
                }
            ],
            "GGGroupId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
        }
    ]
}
```

Every element described next is under the GGGroups key.

CAs is an array of certificate authority certificates you should use when connecting to the Greengrass Core from the Greengrass Device. There will be one or more CAs here. Your code will need to support the possibility that more than one is listed or you may run into issues in the future.

Cores is an array of objects. Each object is a core. Each core has a ConnectivityInfo structure and a thingArn value. The ConnectivityInfo structure contains all of the IP addresses that a Greengrass Device should try to connect to. The thingArn is the just the thing ARN of the core. Currently only one core per group is supported but in the future there could be more than one.

GGGroupId is a UUID that uniquely identifies this Greengrass group. This is used for control plane functions.

## Troubleshooting

### **Obtaining the Greengrass Core's local server certificate information**

If you run into SSL issues it may be helpful to get the TLS certificate info from the local MQTT server. To do this you can use this command:

```
echo | openssl s_client -showcerts -connect 127.0.0.1:8883 2>/dev/null | openssl x509 -inform pem -noout -text
```

# Debugging

## Enable CloudWatch Logs in AWS IoT

* Go to the AWS IoT console
* Click "Settings" in the upper right corner
* Click the "Update" button
* Click "Create a new role"
* Give the role a name like "cloudwatchiot" and click "Create"
* Set the "Log level" to "Debug (most verbose)"
* Click "Save"

A log group called AWSIotLogs will be created that will log information about connections, messages published, etc. It will not log failed connections.

## Issues with failed connections, subscribing to topics, publishing messages, or receiving messages

* Attempt to connect to the MQTT endpoint but do not publish any messages (including LWT), or subscribe to any topics
    * If the client cannot connect these are the likely issues:
        * Its certificate may be in a state other than ACTIVE. Make the certificate active again if possible, reconnect, and try again.
        * A valid policy may not be attached to its certificate. Attach a known good policy to the certificate, detach the bad policy, reconnect, and try again.
        * The policy may not include permissions to connect. Add connect permissions to the policy, reconnect, and try again.
        * The root CA that your client is using is not the AWS root CA. Even if you are bringing your own certificates the root CA that the client uses to validate the broker must be the AWS root CA. Do not use your own CA as the root CA on the client. Switch to the AWS root CA and try again.
    * If the client connects but disconnects while it is idle it is likely that another client has the same client ID. Client IDs must be unique in the region in which you are using AWS IoT.
* Attempt to publish a message
    * If the client cannot publish these are the likely issues:
        * A valid policy may not be attached to its certificate. Attach a known good policy to the certificate, detach the bad policy, reconnect, and try again.
        * The policy may not include permissions to publish to that topic. Add publish permissions for that topic to the policy, reconnect, and try again.
* Attempt to subscribe to a topic
    * If the client cannot subscribe these are the likely issues:
        * A valid policy may not be attached to its certificate. Attach a known good policy to the certificate, detach the bad policy, reconnect, and try again.
        * The policy may not include permissions to subscribe to that topic. Add subscribe permissions for that topic to the policy, reconnect, and try again.
    * If the client can subscribe to that topic but does not receive any messages or gets periodically disconnected it is likely that the policy does not include receive permissions for that topic. Add receive permissions for that topic to the policy, reconnect, and try again.

## Java specific but similar in other languages

* Connection lost (32109) - java.io.EOFException
    * No policy attached to certificate
    * Policy attached to certificate is too restrictive (deny all, etc)
* javax.net.ssl.SSLHandshakeException: Received fatal alert: certificate_unknown
    * Certificate has been deleted
    * Certificate doesn't exist
    * Certificate is present but marked as inactive

## Common rule errors

* If a republish rule doesn't work make sure the rule's role has the permissions necessary to republish. Check CloudWatch logs for this:
    * MESSAGE:Failed to republish to topic. Received Server error. The error code is 403
* If a rule cannot execute a Lambda function it may be because you need to give AWS IoT permission to execute that Lambda function. Delete the old role you've used for this function and let the console create a new role automatically. The error message looks like this:
    * MESSAGE:Failed to invoke lambda function. Received Server error from Lambda. The error code is 403

# Reserved MQTT topics

* `$aws/events/presence/#` - wildcard topic to see all presence events
* `$aws/things/+/shadow/#` - wildcard topic to see all shadow events

## Presence message examples

Connection:

```
{
    "clientId": "modular-client",
    "eventType": "connected",
    "principalIdentifier": "fba86c3e2500fcf5d5986d87a8f7cf86571b3253c15746d1579da46531ee1bf9",
    "sessionIdentifier": "021fa8d7-5cee-4e55-8236-2f496db97049",
    "timestamp": 1481325569114
}
```

Unexpected disconnection:

```
{
    "clientId": "modular-client",
    "clientInitiatedDisconnect": false,
    "eventType": "disconnected",
    "principalIdentifier": "fba86c3e2500fcf5d5986d87a8f7cf86571b3253c15746d1579da46531ee1bf9",
    "sessionIdentifier": "021fa8d7-5cee-4e55-8236-2f496db97049",
    "timestamp": 1481325689294
}
```

Graceful disconnection:

```
{
    "clientId": "modular-client",
    "clientInitiatedDisconnect": true,
    "eventType": "disconnected",
    "principalIdentifier": "fba86c3e2500fcf5d5986d87a8f7cf86571b3253c15746d1579da46531ee1bf9",
    "sessionIdentifier": "021fa8d7-5cee-4e55-8236-2f496db97049",
    "timestamp": 1481325689294
}
```
