# Simple pub/sub tools for the command line

This directory contains two tools `pub.py` and `sub.py` to let customers easily publish and subscribe to topics on the
command line for development and testing.

## How do I get started?

### Publishing messages

```
./pub.py topic payload
```

### Subscribing to topics

```
./sub.py topic1 topic2 ...
```

## Why is this necessary?

Here are some of the questions you might have if you're asking if this is necessary

### Can't we just use a normal MQTT client?

Yes, but if you do you'll need to set up certificates or recreate the temporary credential magic done here.

### Can't we just use the AWS IoT test console?

Yes, but if you prefer to do things on the command line (grepping, logging to files, etc) then you have to use a local
client.

### Can't we just publish with aws iot-data publish ... ?

Yes, but there are some gotchas. If you try to publish the payload `payload` on the topic `topic` like this:

```
aws iot-data publish --topic topic --payload payload
```

You'll receive an error that says:

```
Invalid base64: "payload"
```

This is because the CLI expects base64 payloads. This error is helpful for longer payloads and payloads that aren't
valid base64 but shorter payloads like `test` produce unexpected results. Running this command:

```
aws iot-data publish --topic topic --payload test
```

Will actually publish a three byte binary payload of `0xb5`, `0xeb`, `-` but not report an error.
