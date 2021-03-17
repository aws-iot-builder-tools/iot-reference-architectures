package com.awslabs.iatt.spe.serverless.gwt.server;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.aws.samples.cdk.constructs.iam.permissions.HasIamPermissions;
import com.aws.samples.cdk.constructs.iam.permissions.IamPermission;
import com.aws.samples.cdk.constructs.iam.permissions.SharedPermissions;
import com.aws.samples.cdk.constructs.iam.permissions.iot.IotActions;
import com.aws.samples.cdk.constructs.iam.permissions.iot.IotResources;
import com.aws.samples.lambda.servlet.LambdaWebServlet;
import com.awslabs.iatt.spe.serverless.gwt.client.mqtt.ClientConfig;
import com.awslabs.iatt.spe.serverless.gwt.client.shared.JwtResponse;
import com.awslabs.iatt.spe.serverless.gwt.client.shared.JwtService;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import io.vavr.Tuple;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeEndpointRequest;
import software.amazon.awssdk.services.iot.model.DescribeEndpointResponse;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.Credentials;

import javax.servlet.annotation.WebServlet;
import java.net.URI;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.UUID;

import static com.awslabs.iatt.spe.serverless.gwt.client.SharedWithServer.topicPrefix;
import static com.awslabs.iatt.spe.serverless.gwt.server.Authorizer.*;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings({"serial", "GwtServiceNotRegistered"})
@WebServlet(name = "JwtService", displayName = "BasicJwtService", urlPatterns = {"/app/jwt"}, loadOnStartup = 1)
@LambdaWebServlet
public class BasicJwtService extends RemoteServiceServlet implements JwtService, HasIamPermissions {
    public static final String APN = "APN";
    public static final String AUTHORIZERS = "AUTHORIZERS";
    public static final String DELIMITER = "/";
    private static final String ICCID_KEY = "iccid";
    private static final int EXPIRATION_IN_MS_MIN = 10000;
    private static final int EXPIRATION_IN_MS_MAX = 120000;
    private static final String endpoint = IotClient.create().describeEndpoint(r -> r.endpointType("iot:Data-ATS")).endpointAddress();
    private static final StsClient stsClient = StsClient.create();
    private Option<IotDataPlaneClient> iotDataPlaneClientOption = Option.none();

    @Override
    public JwtResponse getJwtResponse(String iccid, int expirationInMs) {
        if (expirationInMs < EXPIRATION_IN_MS_MIN) {
            throw new RuntimeException("Expiration time is below the minimum [" + EXPIRATION_IN_MS_MIN + "]");
        }

        if (expirationInMs > EXPIRATION_IN_MS_MAX) {
            throw new RuntimeException("Expiration time is above the maximum [" + EXPIRATION_IN_MS_MAX + "]");
        }

        Algorithm algorithm = getTokenSigner();
        JwtResponse jwtResponse = new JwtResponse();

        String token = Try.of(() -> JWT.create()
                .withIssuer(APN)
                .withClaim(ICCID_KEY, iccid)
                .withExpiresAt(new Date(System.currentTimeMillis() + expirationInMs))
                .withIssuedAt(new Date(System.currentTimeMillis()))
                .sign(algorithm)).get();

        jwtResponse.token = undoBase64UrlEncoding(extractHeaderAndPayload(token));
        jwtResponse.signature = undoBase64UrlEncoding(extractSignature(token));
        jwtResponse.decodedJwt = JsonHelper.toJson(extractDataToMap(token));
        jwtResponse.iccid = iccid;
        jwtResponse.endpoint = endpoint;
        jwtResponse.signatureAutoValidated = new Authorizer().getTokenSigningConfigurationOption().isDefined();

        return jwtResponse;
    }

    private String undoBase64UrlEncoding(String input) {
        return input.replace('-', '+').replace('_', '/');
    }

    @NotNull
    private Algorithm getTokenSigner() {
        RSAPublicKey publicKey = getRSAPublicKey(Option.of(getServletContext()));
        RSAPrivateKey privateKey = getRSAPrivateKey(Option.of(getServletContext()));

        return Algorithm.RSA256(publicKey, privateKey);
    }

    private Map extractDataToMap(String token) {
        DecodedJWT decodedJWT = extractDataWithNoVerification(token).get();

        Map claimMap = HashMap.ofAll(decodedJWT.getClaims())
                // Convert the claim value to a string
                .map((key, value) -> Tuple.of(key, value.asString()));

        return HashMap.of(
                "subject", decodedJWT.getSubject(),
                "claims", claimMap,
                "payload", decodedJWT.getPayload(),
                "expiresAt", decodedJWT.getExpiresAt(),
                "header", decodedJWT.getHeader(),
                "signature", decodedJWT.getSignature());
    }

    @Override
    public boolean isTokenValid(String token) {
        // Always do full verification here, even if we expect IoT Core will do the signature verification for us
        Try<DecodedJWT> decodedJWTTry = extractDataWithFullVerification(getTokenSigner(), token);

        if (decodedJWTTry.isSuccess()) {
            return true;
        }

        log("Token is not valid, cause: " + decodedJWTTry.getCause().getMessage());

        return false;
    }

    @Override
    public ClientConfig getClientConfig() {
        try {
            Credentials credentials;

            if (SharedPermissions.isRunningInLambda()) {
                // Running in Lambda, get session token
                credentials = Credentials.builder()
                        .accessKeyId(System.getenv("AWS_ACCESS_KEY_ID"))
                        .secretAccessKey(System.getenv("AWS_SECRET_ACCESS_KEY"))
                        .sessionToken(System.getenv("AWS_SESSION_TOKEN"))
                        .build();
            } else {
                // Running locally, get session token
                credentials = stsClient.getSessionToken().credentials();
            }

            DescribeEndpointRequest describeEndpointRequest = DescribeEndpointRequest.builder()
                    .endpointType("iot:Data-ATS")
                    .build();
            DescribeEndpointResponse describeEndpointResponse = IotClient.create().describeEndpoint(describeEndpointRequest);

            ClientConfig clientConfig = new ClientConfig();
            clientConfig.accessKeyId = credentials.accessKeyId();
            clientConfig.secretAccessKey = credentials.secretAccessKey();
            clientConfig.sessionToken = credentials.sessionToken();
            clientConfig.endpointAddress = describeEndpointResponse.endpointAddress();
            clientConfig.region = DefaultAwsRegionProviderChain.builder().build().getRegion().toString();
            clientConfig.clientId = UUID.randomUUID().toString();

            return clientConfig;
        } catch (Exception e) {
            log("e: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getAuthorizerName() {
        Option<String> authorizerOption = Option.of(System.getenv(AUTHORIZERS));

        if (authorizerOption.isEmpty()) {
            throw new RuntimeException("No authorizer found");
        }

        String authorizer = authorizerOption.get();

        if (authorizer.contains(",")) {
            throw new RuntimeException("This architecture only expects one authorizer, cannot continue");
        }

        return authorizer;
    }

    private IotDataPlaneClient getClient() {
        if (iotDataPlaneClientOption.isEmpty()) {
            iotDataPlaneClientOption = Option.of(IotDataPlaneClient.builder()
                    .endpointOverride(URI.create("https://" + endpoint))
                    .build());
        }

        return iotDataPlaneClientOption.get();
    }

    @Override
    public List<IamPermission> getPermissions() {
        return List.of(
                IotActions.publish(IotResources.topic(String.join(DELIMITER, topicPrefix, SharedPermissions.ALL_RESOURCES))),
                IotActions.subscribe(IotResources.topicFilter(String.join(DELIMITER, topicPrefix, SharedPermissions.ALL_RESOURCES))),
                IotActions.receive(IotResources.topic(String.join(DELIMITER, topicPrefix, SharedPermissions.ALL_RESOURCES))),
                IotActions.connect(IotResources.clientId(SharedPermissions.ALL_RESOURCES)),
                IotActions.describeEndpoint);
    }
}
