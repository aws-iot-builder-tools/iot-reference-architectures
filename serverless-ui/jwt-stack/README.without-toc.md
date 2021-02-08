# AWS IoT authentication demo using JWT (JSON Web Tokens)

<!-- toc -->

## What is this architecture?

This architecture shows how to generate JWTs and use them to authenticate to AWS IoT Core.

## Where is this architecture applicable?

- Brownfield deployments where the devices cannot support client side certificates
- Mobile network deployments where the mobile network operator is providing identity services for the edge devices

## How do I launch it?

Install the AWS CDK with npm if you haven't already like this:

```
$ npm i -g aws-cdk
```

Then run `cdk deploy` and wait for output that looks like this:

```
 ✅  jwt-stack

Outputs:
jwt-stack.jwtEndpointD9B8B2AD = https://j5u3g62n89.execute-api.us-east-1.amazonaws.com/prod/
jwt-stack.jwtstackapi = https://j5u3g62n89.execute-api.us-east-1.amazonaws.com/prod/
```

Open your unique URL in a browser and you'll see the interface for testing out the authorizer.

## I ran into an issue with cdk deploy, what do I do?

Open a Github issue and provide as much context as possible. `cdk deploy` in this project requires a JDK to be installed since the CDK code was written in Java. If you don't have a JDK installed you'll need to install one before running the deployment command.

## How is the system organized?

The system has a tabbed user interface that has four main tabs:

- Create and Validate
- Attribution
- Inspect
- Test

## How do I generate a JWT?

- Navigate to the "Create and Validate" tab
- (Optional) Change the device ID to generate a different ICCID value
- (Optional) Change the expiration time in seconds with the slider
- Click "Generate JWT"

After the JWT has been generated the JWT itself is displayed in the "JWT data" box and a "Validate JWT" button appears.
Clicking "Validate JWT" will validate that the JWT in the "JWT data" box is valid and has not expired. The result will
be displayed in the top left corner of the browser.

## How do I inspect a JWT?

- Create a JWT with the instructions above
- Navigate to the "Inspect" tab

In the "Decoded JWT data" box you should see something like this:

```json
{
  "payload" : "eyJpY2NpZCI6IjAzMjg2ODU0NjE1NzU3MTMyNDYiLCJpc3MiOiJBUE4iLCJleHAiOjE2MTI4MTQ0MzQsImlhdCI6MTYxMjgxNDMxNH0",
  "signature" : "Z2cpFMVPB7GatOLC6ENbaBrAkob9as2Mun8KZLDF3QloakQutD63TTwQ4KVmgpwHBizbzdP2sGWn6rTy2zeB8Jkm8Pgtjmb4aYrzCwXU5dCE_BZ7QLuIyaLfP6VqBFcm3uiFILMb2TvKWfL9gh_zUxh1am_S_QkmdD2rwp8QoJcy9kodzr2nzfqIWCRxBPTpbVvWfPKCJOo_gcKgosC-3vkUvxdLaER6E-Zkg2MdG7AG5n9vBidgjoVlA6ebimbKPQbNdhc9uQrJj-k0z9PvWKfXFiOOIlC0jTjgLp5tC2lvnhCq7YL7slZg886DyhQ0_xj0orYLvbh5oq-dicWRd11L7TNzUFEkx-5zfpf02PtIkx2_0O3wH8RAtlH7_PQfb2ebaEzXkealoeQGb_0ZD6vEaMFXIz02Ou267vlqOiZXzgF-yrEtg41dB8LcHeBkRyQCvqxcr7x-GXlD63tozwDYjmMRTjGxoxtafJiOQs3Nyq-8UXAqTQa0d5PRvqNUBgYZLqZB73Bh6ZUlFomp1V7C7nSeFmAW5RFUTcdsUB3X9mahPSifgbLdCPvLuZoo33ZHaMrILO8IMYBE3ChVzUetPL0VnojCyoLqk3ChSU-ItAxYmaIHGux9wgHUGE9U5W9Mi5Ice7hsnlXrkN-msax34RTbqKaGiSjU-JJrRso",
  "subject" : null,
  "claims" : {
    "iccid" : "0328685461575713246",
    "iss" : "APN",
    "exp" : null,
    "iat" : null
  },
  "header" : "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9",
  "expiresAt" : 1612814434000
}
```

## How do I test a JWT?

- Create a JWT with the instructions above
- Navigate to the "Test" tab
- Click the copy button next to the type of test you'd like to perform. The available tests are:
  - Mosquitto publish command - Uses the JWT to connect to AWS IoT Core with MQTT and publishes a single message
  - Curl publish command - Uses the JWT to connect to AWS IoT Core with HTTP and publishes a single message
  - Test invoke with MQTT context command - Validates that the authorizer properly handles the MQTT context request from IoT Core
  - Test invoke with HTTP context command - Validates that the authorizer properly handles the HTTP context request from IoT Core
  - Test invoke with signature verification command - Validates that AWS IoT Core properly verifies the signature of a JWT (this is not present if signing is disabled)

You can click the down arrow to open up each type of command and see what it will run. All of the commands assume you are
using bash. The Mosquitto and Curl commands require that Mosquitto and Curl are already installed. The rest of the commands
require the AWS CLI.

At the top of the test panel there is a "Messages" box. That box will show messages that are sent from Mosquitto and Curl.

Due to Lambda cold starts it may take multiple attempts to connect as the backend Java function times out. Once it has been warmed
up it should run in less than 100ms.

## How do I test partner attribution?

- Create a JWT with the instructions above
- Navigate to the "Attribution" tab
- Click the switch to toggle attribution on
- Populate the attribution fields
- Run a test with the instructions above
