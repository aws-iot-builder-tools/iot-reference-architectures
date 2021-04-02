# AWS IoT automated cross-account certificate signing

<!-- toc -->

## What is this architecture?

This architecture shows how to obtain cross-account access to publish to a specific AWS IoT topic

## Where is this architecture applicable?

- Systems where data needs to be sent to a customer's or partner's AWS account directly

## Terminology

For this architecture there are two parties. One party is the producer. The other party is the consumer.

The producer generates and sends payloads to AWS IoT in the consumer's account.

## What do both parties need to do together?

Both parties need to agree on which topic the producer will publish their data to in AWS IoT. This should be a topic
that is reserved for that producer alone so that they aren't inadvertently pushing payloads that get received by
unrelated applications.

## Limitations

Currently the system only supports one destination topic. That destination topic may include wildcards but the wildcards
must be in IAM wildcard format. This means that they do not support MQTT's `+` and `#` wildcards. They only support
IAM's more generic `*` wildcard.

## Step 1: Producer generates private key and CSR

Once a destination topic has been agreed upon the producer must run the `create-csr.sh` script. This script takes one
parameter which is the destination topic. For example, if the destination topic is `producer1/data` the command the
producer would run would be:

```
$ ./create-csr.sh producer1/data
```

This command will generate a private key file and a CSR. These files are named `cross-account.key` and
`cross-account.csr` respectively. The producer then must send just the `cross-account.csr` file to the consumer. This
file only contains a public key so it can be sent via e-mail.

## Step 2: Consumer signs CSR, activates certificate, and sends the signed certificate to the producer

After receiving the `cross-account.csr` file the consumer needs to install the AWS CDK, if they haven't already, like
this:

```
$ npm i -g aws-cdk
```

Run `cdk deploy` with the `CSR_FILE` environment variable like this:

```
$ CSR_FILE=cross-account.csr cdk deploy
```

Wait for output that looks like this:

```
 ✅  certificate-based-stack

Outputs:
certificate-based-stack.CertificateARNOutput = arn:aws:iot:us-east-1:111111111111:cert/d9be716d9cdea6de263ec690fc18b095ca4fc6c84aa7ae1eb95b678bb782e400
certificate-based-stack.CertificateIDOutput = d9be716d9cdea6de263ec690fc18b095ca4fc6c84aa7ae1eb95b678bb782e400
certificate-based-stack.CertificatePEMCommand = aws iot describe-certificate --certificate-id d9be716d9cdea6de263ec690fc18b095ca4fc6c84aa7ae1eb95b678bb782e400 --query certificateDescription.certificatePem --output text > d9be716d9cdea6de263ec690fc18b095ca4fc6c84aa7ae1eb95b678bb782e400.pem
certificate-based-stack.CertificatePEMFile = d9be716d9cdea6de263ec690fc18b095ca4fc6c84aa7ae1eb95b678bb782e400.pem
certificate-based-stack.CertificateURLOutput = https://console.aws.amazon.com/iot/home?region=us-east-1#/certificate/d9be716d9cdea6de263ec690fc18b095ca4fc6c84aa7ae1eb95b678bb782e400
```

Open the URL from the `CertificateURLOutput` value. This goes directly to the certificate in the AWS IoT console. If
everything looks as expected click "Actions" -> "Activate".

Run the `CertificatePEMCommand` value as a shell command. Then take the `.pem` file that it creates and send it to the
producer.

## Step 3: Producer tests the integration

The producer can test the integration by running the `publish.py` script in the `python` directory. This script requires
`--cert` and `--key` command-line options that specify the paths of the signed certificate and private key files. The
producer's exact command-line will be different but the running the script with the values from above the command would
look like this:

```
$ ./publish.py --cert ../d9be716d9cdea6de263ec690fc18b095ca4fc6c84aa7ae1eb95b678bb782e400.pem  --key ../cross-account.key
```

The consumer must be monitoring the destination topic while the producer is running this command. They should expect to
see a "Hello, world!" message show up on the destination topic. The producer should see no output if everything worked
as expected.

## Step 4: Producer adjusts the script to publish their data

`publish.py` is a baseline cross-account publishing implementation that can be modified to publish any kind of payload.
In most implementations all of the changes will be made in the `cross_account_publish` function and the code that builds
the payload after it.

## Step 5: Decommissioning

If the cross-account integration needs to be removed at any time it is a two step process. First, the certificate must
be deactivated. If it isn't deactivated the stack deletion will fail.

Second, the stack must be deleted. This can be done via the CloudFormation console or via the command-line like this:

```
$ DESTROY=1 cdk destroy
```

`destroy.sh` has been provided as a convenience script to do this.

The temporary environment variable `DESTROY=1` is required. This prevents CDK from looking for a CSR file since it is
only required to synthesize and deploy the stack.

## I ran into an issue with cdk deploy, what do I do?

Open a Github issue and provide as much context as possible. `cdk deploy` in this project requires a JDK to be installed
since the CDK code was written in Java. If you don't have a JDK installed you'll need to install one before running the
deployment command.
