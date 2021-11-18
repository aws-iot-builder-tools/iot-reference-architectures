#!/usr/bin/env node

const publisher = require('./nodejsWithAttribution.js');

optionalCallback = undefined;
publisher.publish("my_sdk_node", "my_platform_node,00:11:22:33:44:55,serial_number", "topic_from_node", "payload_from_node", 0, optionalCallback);
