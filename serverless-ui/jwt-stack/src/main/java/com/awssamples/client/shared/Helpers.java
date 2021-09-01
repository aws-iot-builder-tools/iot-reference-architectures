package com.awssamples.client.shared;

public class Helpers {
    public static String getTokenWithSignature(JwtCreationResponse jwtCreationResponse) {
        return String.join(".", jwtCreationResponse.token, jwtCreationResponse.signature);
    }
}
