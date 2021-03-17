#!/usr/bin/env node

const publisher = require('./nodejsWithAttribution.js');

optionalCallback = undefined;
publisher.publish("APN/1 NodePartnerSoft,ManagedIoT,v1.2.1", "topic_from_node", "payload_from_node", 0, optionalCallback);
