package com.awssamples.cross_account_publish;

import com.aws.samples.cdk.constructs.iam.permissions.iot.IotActions;
import com.aws.samples.cdk.constructs.iam.permissions.iot.IotResources;
import com.aws.samples.cdk.constructs.iot.authorizer.data.output.PolicyDocument;
import com.aws.samples.cdk.constructs.iot.authorizer.data.output.Statement;
import com.aws.samples.cdk.helpers.CdkHelper;
import com.aws.samples.cdk.helpers.CloudFormationHelper;
import com.awslabs.iot.helpers.interfaces.V2IotHelper;
import com.awssamples.MasterApp;
import com.awssamples.stacktypes.JavaGradleStack;
import io.vavr.Tuple;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.iot.*;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;

import javax.inject.Inject;
import java.io.File;
import java.security.KeyPair;

import static com.awslabs.general.helpers.implementations.IoHelper.writeFile;

public class CertificateBasedStack extends software.amazon.awscdk.core.Stack implements JavaGradleStack {
    public static final String PARTNER_POLICY = "PartnerPolicy";
    private static final Logger log = LoggerFactory.getLogger(CertificateBasedStack.class);
    private static final String FULL_KEY_PREFIX = "fixed";
    private static final String DESIRED_TOPIC_VARIABLE = "DESIRED_TOPIC";
    private static final String CSR_FILE_VARIABLE = "CSR_FILE";
    private static final String DESTROY_VARIABLE = "DESTROY";
    private String projectDirectory;
    private String outputArtifactName;
    @Inject
    V2IotHelper v2IotHelper;

    private final FunctionProps.Builder functionPropsBuilder = FunctionProps.builder()
            .runtime(Runtime.JAVA_11)
            .memorySize(1024)
            .timeout(Duration.minutes(1));

    public CertificateBasedStack(final Construct parent, final String name) {
        super(parent, name);

        // Inject dependencies
        MasterApp.masterInjector.inject(this);

        Map<String, String> arguments = CdkHelper.getArguments();

        Option<String> destroyArgument = getDestroyArgument(arguments);

        if (destroyArgument.isDefined()) {
            return;
        }

        Option<String> csrFileArgument = getCsrFileArgument(arguments);
        Option<String> desiredTopicArgument = getDesiredTopicArgument(arguments);

        if (csrFileArgument.isEmpty() && desiredTopicArgument.isEmpty()) {
            throw new RuntimeException("In the environment variables either a CSR filename [" + CSR_FILE_VARIABLE + "] or a desired topic/topic hierarchy [" + DESIRED_TOPIC_VARIABLE + "] must be specified");
        }

        if (csrFileArgument.isDefined() && desiredTopicArgument.isDefined()) {
            throw new RuntimeException("In the environment variables either a CSR filename [" + CSR_FILE_VARIABLE + "] or a desired topic/topic hierarchy [" + DESIRED_TOPIC_VARIABLE + "] must be specified, but not both");
        }

        // Build all of the necessary JARs
        projectDirectory = "../cross-account-publish/" + name + "/";
        outputArtifactName = String.join("-", name, "all.jar");

        build();

        // Make sure we look for the CSR file argument in the right directory
        csrFileArgument = csrFileArgument.map(csrFile -> String.join("", projectDirectory, csrFile));

        // Use the existing CSR or the default one
        File csrFile = new File(csrFileArgument.getOrElse(() -> String.join("", projectDirectory, String.join("-", FULL_KEY_PREFIX, "public.csr"))));

        if (csrFileArgument.isEmpty()) {
            // We are creating the CSR

            // Must have a desired topic
            String desiredTopic = desiredTopicArgument.get();

            // Create a new private key
            File temporaryCaPrivateKeyFile = new File(String.join("", projectDirectory, String.join("-", FULL_KEY_PREFIX, "private.pem")));
            log.info("Creating temporary CA private key file [" + temporaryCaPrivateKeyFile.getAbsolutePath() + "]");
            KeyPair keyPair = v2IotHelper.getRandomRsaKeypair(4096);
            Try.run(() -> writeFile(temporaryCaPrivateKeyFile.getAbsolutePath(), v2IotHelper.toPem(keyPair))).get();

            // Create CSR
            PKCS10CertificationRequest pkcs10CertificationRequest = v2IotHelper.generateCertificateSigningRequest(keyPair, List.of(Tuple.of("CN", desiredTopic)));
            Try.run(() -> writeFile(csrFile.getAbsolutePath(), v2IotHelper.toPem(pkcs10CertificationRequest))).get();
        }

        // At this point we have the provided CSR or our generated CSR

        // Convert from PEM back to CSR and CSR back to PEM just to be sure that the data is valid in case it was edited or corrupted between runs
        Try<PKCS10CertificationRequest> csrTry = v2IotHelper.tryGetObjectFromPem(csrFile, PKCS10CertificationRequest.class);

        // Get the CSR string so we can add it to the Lambda backed custom resource's environment
        String csrString = csrTry.map(v2IotHelper::toPem)
                .getOrElseThrow(() -> new RuntimeException("Couldn't read CSR file or the CSR file is invalid [" + csrFile.getAbsolutePath() + "]"));

        // Extract the common name which is the topic/topic hierarchy they would like to publish on
        String allowedTopic = csrTry.toStream()
                // Get the X500Name (subject)
                .map(PKCS10CertificationRequest::getSubject)
                // Get all of the relative distinguished names (RDNs)
                .map(X500Name::getRDNs)
                // Turn all of the RDNs into a single stream
                .flatMap(Stream::of)
                // Get all of the attribute types and values
                .map(RDN::getTypesAndValues)
                // Turn all of the attribute types and values into a single stream
                .flatMap(Stream::of)
                // Find the common name (CN) attribute
                .filter(value -> value.getType().equals(BCStyle.CN))
                // Extract the CN value
                .map(AttributeTypeAndValue::getValue)
                // Convert the CN value to a string
                .map(Object::toString)
                // Undo the URL style encoding (convert %2F to forward slash)
                .map(value -> value.replace("%2F", "/"))
                // If no common name was found in the CSR then throw an exception
                .getOrElseThrow(() -> new RuntimeException("No common name value was found in the CSR"));

        // Allow this certificate to publish only on the provided topic
        List<Statement> statement = List.of(
                Statement.allowIamAction(IotActions.publish(IotResources.topic(allowedTopic))));

        PolicyDocument policyDocument = new PolicyDocument();
        policyDocument.Statement = statement.asJava();

        String policyName = Fn.join("-", List.of(this.getStackName(), PARTNER_POLICY).asJava());

        CfnPolicyProps cfnPolicyProps = CfnPolicyProps.builder()
                .policyDocument(policyDocument)
                .policyName(policyName)
                .build();

        CfnPolicy cfnPolicy = new CfnPolicy(this, PARTNER_POLICY, cfnPolicyProps);

        // Add the CSR to the custom resource request
        CustomResourceProps.Builder customResourcePropsBuilder = CustomResourceProps.builder()
                .properties(
                        HashMap.of("CSR", csrString,
                                "AllowedTopic", allowedTopic)
                                .toJavaMap());

        // Build a publish policy for the certificate
        List<CustomResource> customResourceList = CloudFormationHelper.getCustomResources(this, getOutputArtifactFile(), Option.of(customResourcePropsBuilder), Option.of(functionPropsBuilder));

        if (customResourceList.size() != 1) {
            throw new RuntimeException("This stack only expects that one custom resource is present but it found [" + customResourceList.size() + "]");
        }

        CustomResource customResource = customResourceList.get();
        String certificatePem = customResource.getAtt("pem").toString();
        String certificateFingerprint = customResource.getAtt("fingerprint").toString();

        CfnCertificateProps cfnCertificateProps = CfnCertificateProps.builder()
                .certificatePem(certificatePem)
                // Set the certificate as inactive so the customer can validate that everything is set up as they expected before granting access
                .status("INACTIVE")
                .certificateMode("SNI_ONLY")
                .build();

        CfnCertificate cfnCertificate = new CfnCertificate(this, "Certificate", cfnCertificateProps);

        String certificateArn = Fn.getAtt(cfnCertificate.getLogicalId(), "Arn").toString();

        // URL to link directly to the certificate
        new CfnOutput(this, "CertificateURLOutput", CfnOutputProps.builder()
                .exportName("CertificateURL")
                .value(Fn.join("", List.of("https://console.aws.amazon.com/iot/home?region=", Aws.REGION, "#/certificate/", certificateFingerprint).asJava()))
                .build());

        // The certificate ARN
        new CfnOutput(this, "CertificateARNOutput", CfnOutputProps.builder()
                .exportName("CertificateARN")
                .value(certificateArn)
                .build());

        new CfnOutput(this, "CertificateIDOutput", CfnOutputProps.builder()
                .exportName("CertificateID")
                .value(certificateFingerprint)
                .build());

        String certificateFileName = Fn.join(".", List.of(certificateFingerprint, "pem").asJava());

        new CfnOutput(this, "CertificatePEMFile", CfnOutputProps.builder()
                .exportName("CertificateFile")
                .value(certificateFileName)
                .build());

        // The command to get the certificate PEM into a file
        new CfnOutput(this, "CertificatePEMCommand", CfnOutputProps.builder()
                .exportName("CertificateCommand")
                .value(Fn.join("", List.of("aws iot describe-certificate --certificate-id ", certificateFingerprint, " --query certificateDescription.certificatePem --output text > ", certificateFingerprint, ".pem").asJava()))
                .build());

        CfnPolicyPrincipalAttachmentProps cfnPolicyPrincipalAttachmentProps = CfnPolicyPrincipalAttachmentProps.builder()
                .policyName(cfnPolicy.getPolicyName())
                .principal(certificateArn)
                .build();

        new CfnPolicyPrincipalAttachment(this, "PrincipalPolicyAttachment", cfnPolicyPrincipalAttachmentProps)
                // Depends on the policy, must delete the attachment first
                .addDependsOn(cfnPolicy);
    }

    private Option<String> getCsrFileArgument(Map<String, String> map) {
        return map.get(CSR_FILE_VARIABLE);
    }

    private Option<String> getDestroyArgument(Map<String, String> map) {
        return map.get(DESTROY_VARIABLE);
    }

    private Option<String> getDesiredTopicArgument(Map<String, String> map) {
        return map.get(DESIRED_TOPIC_VARIABLE);
    }

    @Override
    public String getProjectDirectory() {
        return projectDirectory;
    }

    @Override
    public String getOutputArtifactName() {
        return outputArtifactName;
    }
}
