package com.awssamples.shared;

import com.awssamples.iam.policies.CloudWatchEventsPolicies;
import com.awssamples.iam.policies.IotPolicies;
import com.awssamples.iam.policies.LambdaPolicies;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.iam.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.awssamples.shared.IotHelper.getPublishToTopicPolicyStatement;
import static java.util.Collections.singletonList;

public class RoleHelper {
    public static Role buildPublishToTopicRole(Stack stack, String rolePrefix, String topic, List<PolicyStatement> additionalPolicyStatements, IPrincipal iPrincipal) {
        PolicyStatement iotPolicyStatement = getPublishToTopicPolicyStatement(stack, topic);

        return buildRoleAssumedByPrincipal(stack, rolePrefix + "Role", combinePolicyStatements(additionalPolicyStatements, iotPolicyStatement), iPrincipal);
    }

    public static Role buildRoleAssumedByLambda(Construct construct, String roleName, List<PolicyStatement> policyStatementList) {
        return buildRoleAssumedByPrincipal(construct, roleName, policyStatementList, LambdaPolicies.LAMBDA_SERVICE_PRINCIPAL);
    }

    public static Role buildRoleAssumedByIot(Construct construct, String roleName, List<PolicyStatement> policyStatementList) {
        return buildRoleAssumedByPrincipal(construct, roleName, policyStatementList, IotPolicies.IOT_SERVICE_PRINCIPAL);
    }

    public static Role buildRoleAssumedByPrincipal(Construct construct, String roleName, List<PolicyStatement> policyStatementList, IPrincipal iPrincipal) {
        List<PolicyStatement> basePolicyStatements = singletonList(CloudWatchEventsPolicies.getMinimalCloudWatchEventsLoggingPolicy());

        List<PolicyStatement> allPolicyStatements = new ArrayList<>();
        allPolicyStatements.addAll(basePolicyStatements);
        allPolicyStatements.addAll(policyStatementList);

        PolicyDocumentProps policyDocumentProps = PolicyDocumentProps.builder()
                .statements(allPolicyStatements)
                .build();
        PolicyDocument policyDocument = new PolicyDocument(policyDocumentProps);

        Map<String, PolicyDocument> policyDocuments = new HashMap<>();
        policyDocuments.put("root", policyDocument);

        RoleProps roleProps = RoleProps.builder()
                .assumedBy(iPrincipal)
                .inlinePolicies(policyDocuments)
                .build();

        return new Role(construct, roleName, roleProps);
    }

    @NotNull
    public static List<PolicyStatement> combinePolicyStatements(List<PolicyStatement> policyStatementList, PolicyStatement policyStatement) {
        List<PolicyStatement> allStatements = new ArrayList<>();
        allStatements.addAll(policyStatementList);
        allStatements.add(policyStatement);
        return allStatements;
    }

    public static Role buildPublishToTopicPrefixIotEventRole(Stack stack, String rolePrefix, String topicPrefix, List<PolicyStatement> additionalPolicyStatements, IPrincipal iPrincipal) {
        return buildPublishToTopicRole(stack, rolePrefix, topicPrefix + "/*", additionalPolicyStatements, iPrincipal);
    }
}
