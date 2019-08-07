package io.vertx.fargate.tokenvalidators;

import io.vavr.control.Option;
import io.vertx.fargate.tokenvalidators.interfaces.TokenValidator;

/**
 * Return the token back to the client as its client ID
 */
public class HardcodedTokenValidator implements TokenValidator {
    @Override
    public Option<String> getClientIdForToken(String token) {
        return Option.of(token);
    }
}
