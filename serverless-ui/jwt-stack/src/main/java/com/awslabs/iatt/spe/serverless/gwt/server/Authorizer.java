package com.awslabs.iatt.spe.serverless.gwt.server;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.aws.samples.cdk.annotations.CdkAutoWire;
import com.aws.samples.cdk.constructs.autowired.iot.IotCustomAuthorizer;
import com.aws.samples.cdk.constructs.iam.permissions.HasIamPermissions;
import com.aws.samples.cdk.constructs.iam.permissions.IamPermission;
import com.aws.samples.cdk.constructs.iam.permissions.iot.IotActions;
import com.aws.samples.cdk.constructs.iam.permissions.iot.IotResources;
import com.aws.samples.cdk.constructs.iam.permissions.sts.actions.GetCallerIdentity;
import com.aws.samples.cdk.constructs.iot.authorizer.data.ImmutableTokenSigningConfiguration;
import com.aws.samples.cdk.constructs.iot.authorizer.data.ImmutableTokenSigningKey;
import com.aws.samples.cdk.constructs.iot.authorizer.data.TokenSigningConfiguration;
import com.aws.samples.cdk.constructs.iot.authorizer.data.TokenSigningKey;
import com.aws.samples.cdk.constructs.iot.authorizer.data.input.AuthorizationRequest;
import com.aws.samples.cdk.constructs.iot.authorizer.data.output.AuthorizationResponse;
import com.aws.samples.cdk.constructs.iot.authorizer.data.output.PolicyDocument;
import com.aws.samples.cdk.constructs.iot.authorizer.data.output.Statement;
import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

import static com.awslabs.iatt.spe.serverless.gwt.client.SharedWithServer.topicPrefix;
import static com.awslabs.iatt.spe.serverless.gwt.client.shared.JwtResponse.TOKEN_KEY_NAME;

@CdkAutoWire
public class Authorizer implements IotCustomAuthorizer, HasIamPermissions {
    public static final String KEY_1 = "key1";
    private static final Logger log = LoggerFactory.getLogger(Authorizer.class);
    private static final TlsHelper tlsHelper = new BasicTlsHelper();
    private static Option<KeyPair> fixedKeypairOption = Option.none();
    // Used to validate the time of the token only, not the signature
    private static final JWTVerifier jwtContentOnlyVerifier = JWT.require(Algorithm.none())
            // No leeway accepted
            .acceptLeeway(0)
            .build();

    public static Try<DecodedJWT> extractDataWithFullVerification(Algorithm algorithm, String token) {
        // Algorithm specified, not validated already. Validate and return the decoded value.
        return Try.of(() -> JWT.require(algorithm)
                .build()
                .verify(addTrailingDotIfNecessary(token)));
    }

    public static Try<DecodedJWT> extractDataWithOnlyIssuedTimeVerification(String token) {
        // No algorithm specified, probably validated already. Just return the decoded value.
        Try<DecodedJWT> decodedJWTTry = extractDataWithNoVerification(token);

        if (decodedJWTTry.isFailure()) {
            return decodedJWTTry;
        }

        DecodedJWT decodedJWT = decodedJWTTry.get();

        Date now = new Date();

        if (decodedJWT.getIssuedAt().after(now)) {
            String message = "Token was issued in the future, issued at: " + decodedJWT.getIssuedAt() + ", now: " + now;
            log.error(message);

            return Try.failure(new RuntimeException(message));
        }

        if (decodedJWT.getExpiresAt().before(now)) {
            String message = "Token expired, expiration: " + decodedJWT.getExpiresAt() + ", now: " + now;
            log.error(message);

            return Try.failure(new RuntimeException(message));
        }

        return Try.success(decodedJWT);
    }

    public static Try<DecodedJWT> extractDataWithNoVerification(String token) {
        // Just decode
        return Try.of(() -> JWT.decode(addTrailingDotIfNecessary(token)));
    }

    /**
     * JWT verification library requires a trailing dot
     *
     * @param token
     * @return
     */
    @NotNull
    private static String addTrailingDotIfNecessary(String token) {
        if (!token.endsWith(".")) {
            token = token + ".";
        }

        return token;
    }

    public static String extractHeaderAndPayload(String token) {
        // The trailing dot is required for proper verification
        return token.substring(0, token.lastIndexOf('.'));
    }

    public static String extractSignature(String token) {
        return Try.of(() -> token.split("\\.")[2]).getOrNull();
    }

    @NotNull
    private static Algorithm getTokenVerifier() {
        RSAPublicKey publicKey = getRSAPublicKey(Option.none());

        return Algorithm.RSA256(publicKey, null);
    }

    public static KeyPair getFixedKeypair() {
        return getFixedKeypair(getServletContext());
    }

    private static Option<ServletContext> getServletContext() {
        return Option.none();
    }

    public static KeyPair getFixedKeypair(Option<ServletContext> servletContextOption) {
        if (fixedKeypairOption.isEmpty()) {
            fixedKeypairOption = Option.of(tlsHelper.getFixedKeypair(servletContextOption));
        }

        return fixedKeypairOption.get();
    }

    public static RSAPublicKey getRSAPublicKey(Option<ServletContext> servletContextOption) {
        return (RSAPublicKey) getFixedKeypair(servletContextOption).getPublic();
    }

    public static RSAPrivateKey getRSAPrivateKey(Option<ServletContext> servletContextOption) {
        return (RSAPrivateKey) getFixedKeypair(servletContextOption).getPrivate();
    }

    @Override
    public AuthorizationResponse handleRequest(AuthorizationRequest authorizationRequest, Context context) {
        // Rethrow all exceptions. Inner handle function reduces try/catch nesting for readability.
        return Try.of(() -> innerHandleRequest(authorizationRequest, context)).get();
    }

    private AuthorizationResponse innerHandleRequest(AuthorizationRequest authorizationRequest, Context context) {
        LambdaLogger log = context.getLogger();

        String token;
        Try<DecodedJWT> decodedJWTTry;

        Option<String> unverifiedTokenOption = getUnverifiedToken(authorizationRequest);
        Option<String> verifiedTokenOption = getVerifiedToken(authorizationRequest);

        if (verifiedTokenOption.isDefined()) {
            // Verified by IoT Core already
            token = verifiedTokenOption.get();

            decodedJWTTry = extractDataWithOnlyIssuedTimeVerification(token);
        } else if (unverifiedTokenOption.isDefined()) {
            // Unverified token is present, verify it manually
            token = unverifiedTokenOption.get();

            decodedJWTTry = extractDataWithFullVerification(getTokenVerifier(), token);
        } else {
            // JWT wasn't found
            throw new RuntimeException("Couldn't find a verified or unverified token");
        }

        if (decodedJWTTry.isFailure()) {
            log.log("JWT decoding/validation failure cause: " + decodedJWTTry.getCause().getMessage());

            // Decoded JWT probably expired
            AuthorizationResponse authorizationResponse = new AuthorizationResponse();

            authorizationResponse.isAuthenticated = false;

            return authorizationResponse;
        }

        DecodedJWT decodedJWT = decodedJWTTry.get();

        Claim iccidClaim = decodedJWT.getClaim("iccid");

        if (iccidClaim.isNull()) {
            // No ICCID found
            log.log("No ICCID found in claims");
            return null;
        }

        String iccid = iccidClaim.asString();
        String clientId = iccid;

        String allowedTopic = String.join("/", topicPrefix, clientId);

        List<Statement> statement = List.of(
                Statement.allowIamAction(IotActions.publish(IotResources.topic(allowedTopic))),
                Statement.allowIamAction(IotActions.connect(IotResources.clientId(clientId))),
                Statement.allowIamAction(IotActions.subscribe(IotResources.topicFilter(allowedTopic))),
                Statement.allowIamAction(IotActions.receive(IotResources.topic(allowedTopic))));

        PolicyDocument policyDocument = new PolicyDocument();
        policyDocument.Version = "2012-10-17";
        policyDocument.Statement = statement.asJava();

        List<PolicyDocument> policyDocuments = List.of(policyDocument);

        AuthorizationResponse authorizationResponse = new AuthorizationResponse();
        authorizationResponse.isAuthenticated = true;
        authorizationResponse.principalId = clientId;
        authorizationResponse.disconnectAfterInSeconds = 86400;
        authorizationResponse.refreshAfterInSecs = 300;
        authorizationResponse.policyDocuments = policyDocuments.asJava();

        return authorizationResponse;
    }

    @NotNull
    private Option<String> getUnverifiedToken(AuthorizationRequest authorizationRequest) {
        Option<String> returnValue = getUnverifiedTokenFromMqtt(authorizationRequest);

        if (returnValue.isDefined()) {
            // Using the token from the MQTT context
            return returnValue;
        }

        // No MQTT context, check if there is an HTTP context
        return getUnverifiedTokenFromHttp(authorizationRequest);
    }

    private Option<String> getUnverifiedTokenFromMqtt(AuthorizationRequest authorizationRequest) {
        return Option.of(authorizationRequest)
                // Get the protocol data, if available
                .flatMap(value1 -> Option.of(value1).map(value2 -> value2.protocolData))
                // Get the MQTT data, if available
                .flatMap(value1 -> Option.of(value1).map(value2 -> value2.mqtt))
                // Get the MQTT username, if available
                .flatMap(value1 -> Option.of(value1).map(value2 -> value2.username))
                // If we get a username make sure we trim everything after the question mark
                .map(username -> username.replaceAll("\\?.*$", ""));
    }

    private Option<String> getUnverifiedTokenFromHttp(AuthorizationRequest authorizationRequest) {
        return Option.of(authorizationRequest)
                // Get the protocol data, if available
                .flatMap(a -> Option.of(a).map(b -> b.protocolData))
                // Get the HTTP data, if available
                .flatMap(a -> Option.of(a).map(b -> b.http))
                // Get the HTTP query string username, if available
                .flatMap(a -> Option.of(a).map(b -> b.queryString))
                // If we get a username make sure we trim everything before the "?token=" string
                .map(username -> username.replaceAll("\\?token=", ""))
                // If we get a username make sure we trim everything after the ampersand
                .map(username -> username.replaceAll("\\&.*$", ""));
    }

    @NotNull
    private Option<String> getVerifiedToken(AuthorizationRequest authorizationRequest) {
        return Option.of(authorizationRequest)
                // Signature must be verified
                .filter(value -> value.signatureVerified)
                // Get the token
                .map(value -> value.token);
    }

    @Override
    public List<IamPermission> getPermissions() {
        return List.of(new GetCallerIdentity());
    }

    @Override
    public Option<TokenSigningConfiguration> getTokenSigningConfigurationOption() {
        StringWriter stringWriter = new StringWriter();

        tlsHelper.writeKey(getFixedKeypair().getPublic(), stringWriter);

        TokenSigningKey tokenSigningKey = ImmutableTokenSigningKey.builder()
                .name(KEY_1)
                .rawKey(Either.right(stringWriter.toString()))
                .build();

        TokenSigningConfiguration tokenSigningConfiguration = ImmutableTokenSigningConfiguration.builder()
                .tokenSigningKeys(List.of(tokenSigningKey))
                .tokenKeyName(TOKEN_KEY_NAME)
                .build();

        return Option.of(tokenSigningConfiguration);
    }
}
