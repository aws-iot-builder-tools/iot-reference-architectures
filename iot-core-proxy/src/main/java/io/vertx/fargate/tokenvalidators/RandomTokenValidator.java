package io.vertx.fargate.tokenvalidators;

import io.vavr.control.Option;
import io.vertx.fargate.tokenvalidators.interfaces.TokenValidator;

import javax.inject.Inject;
import java.util.UUID;

/**
 * Just return a random value no matter what token is specified
 */
public class RandomTokenValidator implements TokenValidator {
    @Inject
    public RandomTokenValidator() {
    }

    @Override
    public Option<String> getClientIdForToken(String token) {
        return Option.of(UUID.randomUUID().toString());
    }
}
