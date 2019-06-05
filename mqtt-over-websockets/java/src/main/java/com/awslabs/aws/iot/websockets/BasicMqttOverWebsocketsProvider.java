package com.awslabs.aws.iot.websockets;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProviderChain;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.iot.IotClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

public class BasicMqttOverWebsocketsProvider implements MqttOverWebsocketsProvider {
    @Override
    public MqttClient getMqttClient(String clientId) throws MqttException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        // Use default values for region and endpoint address
        return getMqttClient(clientId, Optional.empty(), Optional.empty());
    }

    @Override
    public MqttClient getMqttClient(String clientId, Optional<Region> optionalRegion, Optional<String> optionalEndpointAddress) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException, MqttException {
        String mqttOverWebsocketsUri = getMqttOverWebsocketsUri(optionalRegion, optionalEndpointAddress);

        MemoryPersistence persistence = new MemoryPersistence();

        return new MqttClient(mqttOverWebsocketsUri, clientId, persistence);
    }

    @Override
    public void connect(MqttClient mqttClient) throws MqttException {
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        mqttClient.connect(connOpts);
    }

    private String getDateStamp(DateTime dateTime) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyyMMdd");
        return dateTimeFormatter.print(dateTime.withZone(DateTimeZone.UTC));
    }

    private String getAmzDate(DateTime dateTime) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss'Z'");
        return dateTimeFormatter.print(dateTime.withZone(DateTimeZone.UTC));
    }

    // Derived from: http://docs.aws.amazon.com/iot/latest/developerguide/iot-dg.pdf
    @Override
    public String getMqttOverWebsocketsUri(Optional<Region> optionalRegion, Optional<String> optionalEndpointAddress) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        long time = System.currentTimeMillis();
        DateTime dateTime = new DateTime(time);
        String dateStamp = getDateStamp(dateTime);
        String amzdate = getAmzDate(dateTime);
        String service = "iotdata";
        Region region = optionalRegion.orElseGet(this::getDefaultRegionString);
        String regionString = region.toString();
        String clientEndpoint = optionalEndpointAddress.orElseGet(this::getDefaultEndpointAddress);

        AwsCredentials awsCredentials = DefaultCredentialsProvider.create().resolveCredentials();
        String awsAccessKeyId = awsCredentials.accessKeyId();
        String awsSecretAccessKey = awsCredentials.secretAccessKey();
        Optional<String> optionalSessionToken = Optional.empty();

        if (awsCredentials instanceof AwsSessionCredentials) {
            optionalSessionToken = Optional.of(((AwsSessionCredentials) awsCredentials).sessionToken());
        }

        String algorithm = "AWS4-HMAC-SHA256";
        String method = "GET";
        String canonicalUri = "/mqtt";

        String credentialScope = dateStamp + "/" + regionString + "/" + service + "/" + "aws4_request";
        String canonicalQuerystring = "X-Amz-Algorithm=AWS4-HMAC-SHA256";
        canonicalQuerystring += "&X-Amz-Credential=" + URLEncoder.encode(awsAccessKeyId + "/" + credentialScope, "UTF-8");
        canonicalQuerystring += "&X-Amz-Date=" + amzdate;
        canonicalQuerystring += "&X-Amz-SignedHeaders=host";

        String canonicalHeaders = "host:" + clientEndpoint + ":443\n";
        String payloadHash = sha256("");
        String canonicalRequest = method + "\n" + canonicalUri + "\n" + canonicalQuerystring + "\n" + canonicalHeaders + "\nhost\n" + payloadHash;

        String stringToSign = algorithm + "\n" + amzdate + "\n" + credentialScope + "\n" + sha256(canonicalRequest);
        byte[] signingKey = getSignatureKey(awsSecretAccessKey, dateStamp, regionString, service);
        String signature = sign(signingKey, stringToSign);

        canonicalQuerystring += "&X-Amz-Signature=" + signature;
        String requestUrl = "wss://" + clientEndpoint + canonicalUri + "?" + canonicalQuerystring;

        if (optionalSessionToken.isPresent()) {
            requestUrl += "&X-Amz-Security-Token=" + URLEncoder.encode(optionalSessionToken.get(), "UTF-8");
        }

        return requestUrl;
    }

    private String getDefaultEndpointAddress() {
        IotClient iotClient = IotClient.create();
        return iotClient.describeEndpoint().endpointAddress();
    }

    private Region getDefaultRegionString() {
        AwsRegionProviderChain awsRegionProviderChain = new DefaultAwsRegionProviderChain();
        return awsRegionProviderChain.getRegion();
    }

    private byte[] HmacSHA256(String data, byte[] key) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        String algorithm = "HmacSHA256";
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key, algorithm));
        return mac.doFinal(data.getBytes("UTF8"));
    }

    private byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName) throws UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException {
        byte[] kSecret = ("AWS4" + key).getBytes("UTF8");
        byte[] kDate = HmacSHA256(dateStamp, kSecret);
        byte[] kRegion = HmacSHA256(regionName, kDate);
        byte[] kService = HmacSHA256(serviceName, kRegion);
        byte[] kSigning = HmacSHA256("aws4_request", kService);

        return kSigning;
    }

    private String sign(byte[] key, String message) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        byte[] hash = HmacSHA256(message, key);
        return bytesToHex(hash);
    }

    // From: https://gist.github.com/avilches/750151
    private String sha256(String data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(data.getBytes());

        return bytesToHex(md.digest());
    }

    // From: https://gist.github.com/avilches/750151
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte byt : bytes) result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }
}
