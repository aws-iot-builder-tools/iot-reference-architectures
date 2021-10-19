#!/usr/bin/env node

const {X509Certificate} = require('crypto');
const {ArgumentParser} = require('argparse');
const FS = require('fs');
const https = require('https');

const parser = new ArgumentParser();

function getCommonName(input) {
    if (input.indexOf("CN=") === -1) {
        // No common name found
        return undefined;
    }

    return input.replace(/^.*CN=/, "").replace(/,.*$/, "")
}

parser.add_argument('-k', '--key', {help: 'The location of the private key file'});
parser.add_argument('-c', '--cert', {help: 'The location of the public certificate file'});
parser.add_argument('-m', '--message', {help: 'The message to publish'});

const arguments = parser.parse_args();

const keyFilename = arguments['key'];
const publicCertificateFilename = arguments['cert'];
const message = arguments['message'];

let argumentsSpecified = true;

if (typeof keyFilename === 'undefined') {
    argumentsSpecified = false;
    console.log('The private key filename was not specified')
}

if (typeof publicCertificateFilename === 'undefined') {
    argumentsSpecified = false;
    console.log('The public certificate filename was not specified')
}

if (typeof message === 'undefined') {
    argumentsSpecified = false;
    console.log('The message was not specified')
}

if (!argumentsSpecified) {
    process.exit(1)
}

let publicCertificateData = null;

try {
    publicCertificateData = FS.readFileSync(publicCertificateFilename, 'utf8');
} catch (e) {
    console.log('Failed to read public certificate file [', publicCertificateFilename, ']: ', e.stack);
    process.exit(1)
}

const publicCertificate = new X509Certificate(publicCertificateData);

const endpoint = getCommonName(publicCertificate.issuer)
const topic = getCommonName(publicCertificate.subject).replaceAll("%2F", "/")

if (typeof endpoint === 'undefined') {
    console.log('No endpoint information was found in the certificate\'s issuer common name field');
    process.exit(1)
}

if (typeof topic === 'undefined') {
    console.log('No topic information was found in the certificate\'s subject common name field');
    process.exit(1)
}

const options = {
    protocol: 'https:',
    hostname: endpoint,
    port: 8443,
    path: '/topics/' + topic + '?qos=1',
    method: 'POST',
    // headers: {
    //     'Content-Type': 'application/json',
    //     'Content-Length': data.length
    // },
    key: FS.readFileSync(keyFilename),
    cert: FS.readFileSync(publicCertificateFilename)//publicCertificateData
};

const req = https.request(options, (res) => {
    let data = '';

    res.on('data', (chunk) => {
        data += chunk;
    });

    res.on('end', () => {
        console.log(JSON.parse(data));
    });

}).on("error", (err) => {
    console.log("Error: ", err.message);
});

req.write(message);
req.end();
