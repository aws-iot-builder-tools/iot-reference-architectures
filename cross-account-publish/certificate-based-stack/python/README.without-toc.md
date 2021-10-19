# AWS IoT cross-account publish using automated cross-account certificate signing

NOTE: This cross-account publish code will only work without modifications if you use the certificates and keys generated from [the cross-account-publish reference architecture in the parent directory of this script](..).

<!-- toc -->

## Troubleshooting

It is common to run this script and run into the following issues:

- The script is not executable. Run `chmod +x publish.py` on Linux/MacOS.
- The certificate has not been downloaded from AWS IoT and stored locally. This can be done by running the shell command in the CloudFormation stack output called `CertificatePEMCommand`.
- The certificate has not been activated. Open the URL in the CloudFormation stack output called `CertificateURL` or run the shell command in the CloudFormation stack output called `CertificateActivateCommand`.

## How to set up this script

Run `pip install -r requirements.txt` to get the necessary dependencies

## How to run this script

There are three required parameters:

- `--key` - the name of the file that contains the private key
- `--cert` - the name of the file that contains the public signed certificate
- `--message` - the message to send to the pre-defined topic

Usually the keys and certificates are generated in the parent directory so a typical invocation might look like this:

```sh
./publish.py --key ../cross-account.key --cert ../certificate-hash.pem --message 'Non-JSON test message'
```

Or this for JSON:

```sh
./publish.py --key ../cross-account.key --cert ../certificate-hash.pem --message '{ "message": "This is my message" }'
```
