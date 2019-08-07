package io.vertx.fargate.tokenvalidators.interfaces;

import io.vavr.control.Option;

public interface TokenValidator {
    /**
     * Provides a client ID for a client using token based authentication.  An empty value means the token was not accepted.
     *
     * @param token
     * @return
     */
    Option<String> getClientIdForToken(String token);
}
