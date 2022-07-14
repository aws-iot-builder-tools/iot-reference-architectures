import com.amazonaws.lambda.thirdparty.org.json.JSONObject;
import com.awslabs.general.helpers.implementations.JacksonHelper;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.control.Try;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.services.sts.model.GetFederationTokenRequest;
import software.amazon.awssdk.services.sts.model.GetFederationTokenResponse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.logging.Logger;

public class FederationTest {
    public static final Logger log = Logger.getLogger(FederationTest.class.getName());

    @Test
    @Ignore
    public void doit() {
        StsClient stsClient = StsClient.create();

        String managedInstanceId = "mi-08c0e312d9a03d8e4";
        String region = DefaultAwsRegionProviderChain.builder().build().getRegion().id();
        String accountId = stsClient.getCallerIdentity().account();
        String resourceArn = String.join("/", "arn:aws:ssm:" + region + ":" + accountId + ":managed-instance", managedInstanceId);
        HashMap<String, Serializable> policyMap = HashMap.of("Version", "2012-10-17",
                "Statement",
                List.of(
                        HashMap.of("Effect", "Allow",
                                "Resource", List.of(resourceArn),
                                "Action", List.of("ssm:StartSession"))));

        String policy = JacksonHelper.tryToJsonString(policyMap).get();

        GetFederationTokenRequest getFederationTokenRequest = GetFederationTokenRequest.builder()
                .durationSeconds(1800)
                .name("temp-user")
                .policy(policy)
                .build();

        GetFederationTokenResponse federationTokenResult = stsClient.getFederationToken(getFederationTokenRequest);

        Credentials federatedCredentials = federationTokenResult.credentials();

        // The issuer parameter specifies your internal sign-in
        // page, for example https://mysignin.internal.mycompany.com/.
        // The console parameter specifies the URL to the destination console of the
        // AWS Management Console. This example goes to Amazon SNS.
        // The signin parameter is the URL to send the request to.

        String issuerURL = "https://mysignin.internal.mycompany.com/";
        //        String consoleURL = "https://console.aws.amazon.com/sns";
        String consoleURL = "https://" + region + ".console.aws.amazon.com/systems-manager/session-manager/" + managedInstanceId + "?region=" + region + "#";
        String signInURL = "https://signin.aws.amazon.com/federation";

        // Create the sign-in token using temporary credentials,
        // including the access key ID,  secret access key, and security token.
        String sessionJson = String.format(
                "{\"%1$s\":\"%2$s\",\"%3$s\":\"%4$s\",\"%5$s\":\"%6$s\"}",
                "sessionId", federatedCredentials.accessKeyId(),
                "sessionKey", federatedCredentials.secretAccessKey(),
                "sessionToken", federatedCredentials.sessionToken());

        // Construct the sign-in request with the request sign-in token action, a
        // 12-hour console session duration, and the JSON document with temporary
        // credentials as parameters.

        String getSigninTokenURL = signInURL +
                "?Action=getSigninToken" +
                "&DurationSeconds=43200" +
                "&SessionType=json&Session=" +
                Try.of(() -> URLEncoder.encode(sessionJson, "UTF-8")).get();

        URL url = Try.of(() -> new URL(getSigninTokenURL)).get();

        // Send the request to the AWS federation endpoint to get the sign-in token
        URLConnection conn = Try.of(url::openConnection).get();

        BufferedReader bufferReader = new BufferedReader(new
                InputStreamReader(Try.of(conn::getInputStream).get()));
        String returnContent = Try.of(bufferReader::readLine).get();

        String signinToken = new JSONObject(returnContent).getString("SigninToken");

        String signinTokenParameter = "&SigninToken=" + Try.of(() -> URLEncoder.encode(signinToken, "UTF-8")).get();

        // The issuer parameter is optional, but recommended. Use it to direct users
        // to your sign-in page when their session expires.

        String issuerParameter = "&Issuer=" + Try.of(() -> URLEncoder.encode(issuerURL, "UTF-8")).get();

        // Finally, present the completed URL for the AWS console session to the user

        String destinationParameter = "&Destination=" + Try.of(() -> URLEncoder.encode(consoleURL, "UTF-8")).get();
        String loginURL = signInURL + "?Action=login" +
                signinTokenParameter + issuerParameter + destinationParameter;

        log.info(loginURL);
    }
}
