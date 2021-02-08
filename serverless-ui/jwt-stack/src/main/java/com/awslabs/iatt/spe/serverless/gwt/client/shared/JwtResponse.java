package com.awslabs.iatt.spe.serverless.gwt.client.shared;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.dominokit.domino.api.shared.extension.EventContext;

@SuppressWarnings("serial")
public class JwtResponse extends NoToString implements IsSerializable, EventContext {
    public static final String TOKEN_KEY_NAME = "token";

    public String token;

    public String signature;

    public String decodedJwt;

    public String iccid;

    public String endpoint;

    public boolean signatureAutoValidated;
}
