package com.awssamples.client.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

@SuppressWarnings("serial")
public class JwtCreationResponse extends NoToString implements IsSerializable {
    public static final String TOKEN_KEY_NAME = "token";

    public String token;

    public String signature;

    public String decodedJwt;

    public String iccid;

    public String endpoint;

    public boolean signatureAutoValidated;
}
