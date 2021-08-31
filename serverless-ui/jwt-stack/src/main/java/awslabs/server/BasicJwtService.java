package awslabs.server;

import awslabs.client.mqtt.ClientConfig;
import awslabs.client.shared.JwtCreationResponse;
import awslabs.client.shared.JwtService;
import awslabs.client.shared.JwtValidationResponse;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.aws.samples.cdk.constructs.iam.permissions.HasIamPermissions;
import com.aws.samples.cdk.constructs.iam.permissions.IamPermission;
import com.aws.samples.cdk.constructs.iam.permissions.SharedPermissions;
import com.aws.samples.cdk.constructs.iam.permissions.iot.IotActions;
import com.aws.samples.cdk.constructs.iam.permissions.iot.IotResources;
import com.aws.samples.lambda.servlet.LambdaWebServlet;
import com.awslabs.cloudformation.data.ImmutableStackName;
import com.awslabs.cloudformation.data.StackName;
import com.awslabs.cloudformation.interfaces.CloudFormationHelper;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import io.vavr.Lazy;
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

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.UUID;

import static awslabs.client.SharedWithServer.topicPrefix;

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
    private static final String endpoint = IotClient.create().describeEndpoint(r -> r.endpointType("iot:Data-ATS")).endpointAddress();
    private static final StsClient stsClient = StsClient.create();
    private static final Lazy<Injector> lazyInjector = Lazy.of(DaggerInjector::create);
    public static final Lazy<String> lazyAuthorizerName = Lazy.of(() ->
            Option.of(System.getenv(AUTHORIZERS))
                    .getOrElseThrow(() -> new RuntimeException(AUTHORIZERS + " environment variable not present, cannot continue")));
    private final StackName stackName = ImmutableStackName.builder().stackName("jwt-stack").build();

    @Inject
    public CloudFormationHelper cloudFormationHelper;

    @Override
    public JwtCreationResponse getJwtResponse(String iccid, int expirationInMs) {
        if (expirationInMs < EXPIRATION_IN_MS_MIN) {
            throw new RuntimeException("Expiration time is below the minimum [" + EXPIRATION_IN_MS_MIN + "]");
        }

        if (expirationInMs > EXPIRATION_IN_MS_MAX) {
            throw new RuntimeException("Expiration time is above the maximum [" + EXPIRATION_IN_MS_MAX + "]");
        }

        Algorithm algorithm = getTokenSigner();
        JwtCreationResponse jwtCreationResponse = new JwtCreationResponse();

        String token = Try.of(() -> JWT.create()
                .withIssuer(APN)
                .withClaim(ICCID_KEY, iccid)
                .withExpiresAt(new Date(System.currentTimeMillis() + expirationInMs))
                .withIssuedAt(new Date(System.currentTimeMillis()))
                .sign(algorithm)).get();

        jwtCreationResponse.token = undoBase64UrlEncoding(Authorizer.extractHeaderAndPayload(token));
        jwtCreationResponse.signature = undoBase64UrlEncoding(Authorizer.extractSignature(token));
        jwtCreationResponse.decodedJwt = JsonHelper.toJson(extractDataToMap(token));
        jwtCreationResponse.iccid = iccid;
        jwtCreationResponse.endpoint = endpoint;
        jwtCreationResponse.signatureAutoValidated = new Authorizer().getTokenSigningConfigurationOption().isDefined();

        return jwtCreationResponse;
    }

    private String undoBase64UrlEncoding(String input) {
        return input.replace('-', '+').replace('_', '/');
    }

    private String redoBase64UrlEncoding(String input) {
        return input.replace('+', '-').replace('/', '_');
    }

    @NotNull
    private Algorithm getTokenSigner() {
        RSAPublicKey publicKey = Authorizer.getRSAPublicKey(Option.of(getServletContext()));
        RSAPrivateKey privateKey = Authorizer.getRSAPrivateKey(Option.of(getServletContext()));

        return Algorithm.RSA256(publicKey, privateKey);
    }

    private Map extractDataToMap(String token) {
        DecodedJWT decodedJWT = Authorizer.extractDataWithNoVerification(token).get();

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

    private String fixTokenFromClient(String token) {
        String headerAndPayload = Authorizer.extractHeaderAndPayload(token);
        String signature = redoBase64UrlEncoding(Authorizer.extractSignature(token));
        return String.join(".", headerAndPayload, signature);
    }

    @Override
    public JwtValidationResponse isTokenValid(String token) {
        // Always do full verification here, even if we expect IoT Core will do the signature verification for us
        token = fixTokenFromClient(token);

        return Authorizer.extractDataWithFullVerification(getTokenSigner(), token)
                .map(value -> new JwtValidationResponse().valid(true))
                .onFailure(throwable -> System.err.println("Token is not valid, cause: " + throwable.getMessage()))
                .getOrElseGet(throwable -> new JwtValidationResponse().valid(false).errorMessage(throwable.getMessage()));
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

            ClientConfig clientConfig = new ClientConfig();
            clientConfig.accessKeyId = credentials.accessKeyId();
            clientConfig.secretAccessKey = credentials.secretAccessKey();
            clientConfig.sessionToken = credentials.sessionToken();
            clientConfig.endpointAddress = endpoint;
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
        String authorizerName;

        if (!SharedPermissions.isRunningInLambda()) {
            // Running locally
            lazyInjector.get().inject(this);
            authorizerName = cloudFormationHelper.getStackResource(stackName, "AWS::IoT::Authorizer", Option.of("authorizer"))
                    .getOrElseThrow(() -> new RuntimeException("Could not find jwt-stackAuthorizer stack resource"));
        } else {
            authorizerName = lazyAuthorizerName.get();
        }

        if (authorizerName.contains(",")) {
            throw new RuntimeException("This architecture only expects one authorizer, cannot continue");
        }

        return authorizerName;
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
