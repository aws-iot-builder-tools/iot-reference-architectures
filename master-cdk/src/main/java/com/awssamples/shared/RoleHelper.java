package com.awssamples.shared;

import com.awssamples.iam.policies.CloudWatchEventsPolicies;
import com.awssamples.iam.policies.LambdaPolicies;
import software.amazon.awscdk.core.Fn;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.iam.*;

import java.util.*;

import static java.util.Collections.singletonList;

public class RoleHelper {
    public static Role buildPublishToTopicIotEventRole(Stack stack, String rolePrefix, String topic, List<PolicyStatement> additionalPolicyStatements) {
        PolicyStatementProps iotPolicyStatementProps = PolicyStatementProps.builder()
                .effect(Effect.ALLOW)
                .resources(singletonList(Fn.join("", Arrays.asList("arn:aws:iot:", stack.getRegion(), ":", stack.getAccount(), ":topic/", topic))))
                .actions(singletonList("iot:Publish"))
                .build();
        PolicyStatement iotPolicyStatement = new PolicyStatement(iotPolicyStatementProps);

        List<PolicyStatement> basePolicyStatements = Arrays.asList(CloudWatchEventsPolicies.getMinimalCloudWatchEventsLoggingPolicy(), iotPolicyStatement);

        List<PolicyStatement> allPolicyStatements = new ArrayList<>();
        allPolicyStatements.addAll(basePolicyStatements);
        allPolicyStatements.addAll(additionalPolicyStatements);

        PolicyDocumentProps policyDocumentProps = PolicyDocumentProps.builder()
                .statements(allPolicyStatements)
                .build();
        PolicyDocument policyDocument = new PolicyDocument(policyDocumentProps);

        Map<String, PolicyDocument> policyDocuments = new HashMap<>();
        policyDocuments.put("root", policyDocument);

        RoleProps roleProps = RoleProps.builder()
                .assumedBy(LambdaPolicies.LAMBDA_SERVICE_PRINCIPAL)
                .inlinePolicies(policyDocuments)
                .build();

        return new Role(stack, rolePrefix + "Role", roleProps);
    }

    public static Role buildPublishToTopicPrefixIotEventRole(Stack stack, String rolePrefix, String topicPrefix, List<PolicyStatement> additionalPolicyStatements) {
        return buildPublishToTopicIotEventRole(stack, rolePrefix, topicPrefix + "/*", additionalPolicyStatements);
    }
}
