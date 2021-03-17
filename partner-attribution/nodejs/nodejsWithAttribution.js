const AWS = require('aws-sdk');
const Promise = require('promise');

// Specify your AWS IoT endpoint in the environment, otherwise it will be discovered automatically
let endpointAddress = process.env.endpointAddress;

// Actually use the .aws/config if it is available
process.env.AWS_SDK_LOAD_CONFIG = true;

module.exports = {
    publish: async function(platform, topic, payload, qos, callback) {
        // Discover the endpoint address if necessary
        if (typeof endpointAddress === "undefined") {
            // Wait for the endpoint value to be returned
            endpointAddress = await new AWS.Iot().describeEndpoint().promise().then(data => data['endpointAddress']);
        }

        const params = {
            topic: topic,
            payload: Buffer.from(payload),
            qos: qos
        };

        // Create the request to IotData
        request = new AWS.IotData({endpoint: endpointAddress}).publish(params);

        // Add the attribution information to the headers
        headers = request.httpRequest.headers;
        headers['x-amzn-platform'] = platform;

        // Send the request with the user specified callback
        request.send(callback);
    }
};
