package com.awssamples.iot.crossaccountpublish.certificatebased;

import com.amazonaws.services.lambda.runtime.Context;
import com.aws.samples.cdk.annotations.CdkAutoWire;
import com.aws.samples.cdk.constructs.autowired.cloudformation.CustomResourceFunction;
import com.aws.samples.cdk.constructs.autowired.cloudformation.CustomResourceRequest;
import com.aws.samples.cdk.constructs.autowired.cloudformation.CustomResourceResponse;
import com.aws.samples.cdk.constructs.iam.permissions.IamPermission;
import com.aws.samples.cdk.constructs.iam.permissions.iot.dataplane.actions.ImmutableDescribeEndpoint;
import com.awslabs.iot.data.IotEndpointType;
import com.awslabs.iot.helpers.interfaces.IotHelper;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.PublicKey;
import java.security.Security;
import java.security.cert.X509Certificate;

@CdkAutoWire
public class CertificateSigner extends CustomResourceFunction {
    private final Lazy<Injector> lazyDaggerInjector = Lazy.of(DaggerInjector::create);
    private final Lazy<IotHelper> lazyIotHelper = Lazy.of(() -> lazyDaggerInjector.get().iotHelper());

    static {
        // Add BouncyCastle as a security provider in just one place
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    protected CustomResourceResponse create(CustomResourceRequest customResourceRequest, Context context) {
        // Try to get the CSR and fail if it isn't present
        String csr = tryGetValue(customResourceRequest, "CSR", String.class).get();

        // Try to get the allowed topic and fail if it isn't present
        String allowedTopic = tryGetValue(customResourceRequest, "AllowedTopic", String.class).get();

        // Get the ATS endpoint for this account
        IotHelper iotHelper = lazyIotHelper.get();
        String endpoint = iotHelper.getEndpoint(IotEndpointType.DATA_ATS);

        // Extract the public key from this CSR. If this is an RSA key it will likely fail later on due to custom
        //   resources only being allowed to return a 4kB value.
        PublicKey publicKey = iotHelper.getPublicKeyFromCsrPem(csr);

        // Put the endpoint information in the certificate as the issuer's common name
        List<Tuple2<String, String>> issuerCommonName = List.of(Tuple.of("CN", endpoint));

        // Put the allowed topic in the certificate as the subject's common name
        List<Tuple2<String, String>> subjectCommonName = List.of(Tuple.of("CN", allowedTopic));

        // Generate the certificate with a CA we generate on the fly and throw away
        X509Certificate x509Certificate = iotHelper.generateX509Certificate(publicKey, issuerCommonName, subjectCommonName);

        // Convert the certificate to a PEM formatted string
        String certificatePem = iotHelper.toPem(x509Certificate);

        // Get the fingerprint of the certificate since this is what AWS IoT uses as the certificate ID
        String certificateFingerprint = iotHelper.getFingerprint(certificatePem);

        // Put the PEM and the fingerprint in the return data structure. These are fetched by other code by calling
        //   customResource.getAtt("pem") or customResource.getAtt("fingerprint").
        HashMap<String, String> data = HashMap.of(
                "pem", certificatePem,
                "fingerprint", certificateFingerprint);

        // Return success with our data. The certificate fingerprint is the physical resource ID since it's unique.
        return simpleCreateOrUpdateSuccess(customResourceRequest, context, certificateFingerprint, data.toJavaMap());
    }

    @Override
    protected CustomResourceResponse delete(CustomResourceRequest customResourceRequest, Context context) {
        // Nothing to do here since the certificate, policy, and policy attachment are managed by CloudFormation
        return simpleDeleteSuccess(customResourceRequest, context, null);
    }

    @Override
    protected CustomResourceResponse update(CustomResourceRequest customResourceRequest, Context context) {
        // Nothing to do here, an update doesn't make sense at the moment so this is a NOP
        return create(customResourceRequest, context);
    }

    @Override
    public List<IamPermission> getPermissions() {
        // This function only needs to know the endpoint information since it generates a certificate and returns it to
        //   CloudFormation. CloudFormation then registers the certificate.
        return List.of(
                // Need to call describe endpoint to embed the endpoint information into the certificate
                ImmutableDescribeEndpoint.builder().build());
    }
}
