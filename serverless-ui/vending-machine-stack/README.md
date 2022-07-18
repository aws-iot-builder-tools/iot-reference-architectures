# Raspberry Pi image vending machine

<!-- toc -->

- [What is this architecture?](#what-is-this-architecture)
- [Where is this architecture applicable?](#where-is-this-architecture-applicable)
- [How do I launch it?](#how-do-i-launch-it)
- [I ran into an issue with cdk deploy, what do I do?](#i-ran-into-an-issue-with-cdk-deploy-what-do-i-do)
- [How is the system organized?](#how-is-the-system-organized)
- [How do I create a disk image?](#how-do-i-create-a-disk-image)
- [How do I download a disk image?](#how-do-i-download-a-disk-image)
- [How do I see which systems are online?](#how-do-i-see-which-systems-are-online)
- [How do I connect to a system with SSM in my browser?](#how-do-i-connect-to-a-system-with-ssm-in-my-browser)

<!-- tocstop -->

## What is this architecture?

This architecture shows how to generate disk images for Raspberry Pi devices. The vending machine automates enabling SSH, setting up WiFi (optional), enabling SSM for remote access (optional).

## Where is this architecture applicable?

This architecture is ideal for environments where many system images need to be built for IoT devices. It can be extended to build images for other devices and can have GUI elements added to allow users to easily configure options for those devices.

## How do I launch it?

Install the AWS CDK with npm if you haven't already like this:

```
$ npm i -g aws-cdk
```

Then run `cdk deploy` and wait for output that looks like this:

```
 ✅  vending-machine-stack

Outputs:
vending-machine-stack.vendingmachinelambdarestapiEndpoint6888CE98 = https://pvbnnd0ue2.execute-api.us-east-1.amazonaws.com/prod/
vending-machine-stack.vendingmachinerestapiEndpointB20675B4 = https://k72br3ux2e.execute-api.us-east-1.amazonaws.com/prod/
vending-machine-stack.vendingmachinestackapi = https://k72br3ux2e.execute-api.us-east-1.amazonaws.com/prod/
```

Open the URL for `vending-machine-stack.vendingmachinestackapi` in a browser and you'll be presented with the UI for building images.

## I ran into an issue with cdk deploy, what do I do?

Open a Github issue and provide as much context as possible. `cdk deploy` in this project requires a JDK to be installed since the CDK code was written in Java. If you don't have a JDK installed you'll need to install one before running the deployment command.

## How is the system organized?

The system has a tabbed user interface that has four main tabs:

- About
- Raspberry Pi
- Builds
- Systems
- Terminals

## How do I create a disk image?

Go to the `Raspberry Pi` tab and set all the properties for the build. Click the wrench icon that hovers in the lower left corner of the screen to start the build. The build will show up in the `Builds` tab in a few seconds.

## How do I download a disk image?

Go to `Builds` tab and click on a system's name to expand the card underneath it (unless it is already expanded). If the build is complete there will be a `Download image` link. Clicking this link will generate a pre-signed S3 URL for the image and will start downloading it immediately.

## How do I see which systems are online?

Go to `Systems` tab and it will show the status for any SSM enabled builds. If a system has been flashed with an SSM enabled image and is online there will be a `Connect` link and a `Share` link.

The `Connect` link will open a terminal to the system in the `Terminals` tab. The `Share` link will copy a link to the clipboard that can be given to someone else to remotely connect into the system with their browser.

If no system is using this image or the system is offline it will say `System appears to be offline`.

## How do I connect to a system with SSM in my browser?

Go to the `Systems` tab and click `Connect` on the system you would like to connect to. Next go to the `Terminals` tab and a terminal window will show up for the system. This interface contains an in-browser SSM terminal client.
