package com.awssamples.shared;

import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.PolicyStatementProps;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.sqs.Queue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LambdaPolicies {
    public static final ServicePrincipal LAMBDA_SERVICE_PRINCIPAL = new ServicePrincipal("lambda");
    
    // NOTE: sqs:GetQueueAttributes is required by CloudFormation for any role that is attached to a function that is an Dynamo DB event source mapping target
    private static final List<String> MINIMAL_SQS_QUEUE_EVENT_SOURCE_MAPPING_TARGET_ACTIONS = Arrays.asList("sqs:ReceiveMessage", "sqs:DeleteMessage", "sqs:GetQueueUrl", "sqs:GetQueueAttributes");

    public static PolicyStatement getMinimalLambdaSqsQueueEventSourceMappingTargetPolicy(String queueArn) {
        PolicyStatementProps sqsPolicyStatementProps = PolicyStatementProps.builder()
                .effect(Effect.ALLOW)
                .resources(Collections.singletonList(queueArn))
                .actions(MINIMAL_SQS_QUEUE_EVENT_SOURCE_MAPPING_TARGET_ACTIONS)
                .build();

        return new PolicyStatement(sqsPolicyStatementProps);
    }
}
