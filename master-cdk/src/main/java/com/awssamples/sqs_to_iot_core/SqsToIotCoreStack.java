package com.awssamples.sqs_to_iot_core;

import com.awssamples.shared.CdkHelper;
import com.awssamples.shared.CloudWatchEventsPolicies;
import com.awssamples.shared.LambdaPolicies;
import com.awssamples.shared.Permissions;
import io.vavr.control.Try;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Fn;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.iot.CfnTopicRule;
import software.amazon.awscdk.services.iot.CfnTopicRuleProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.sqs.Queue;
import software.amazon.awscdk.services.sqs.QueueProps;

import java.io.File;
import java.util.*;

import static java.util.Collections.singletonList;

public class SqsToIotCoreStack extends software.amazon.awscdk.core.Stack {
    private static final String SQS_QUEUE_ARN_PARAMETER = "SQS_QUEUE_ARN";
    private static final String DYNAMO_DB_TABLE_ARN = "dynamoDbTableArn";
    private static final String SQS_QUEUE_ARN = "sqsQueueArn";
    private static final String UUID_KEY = "uuidKey";
    private static final String MESSAGE_ID_KEY = "messageIdKey";
    private static final String HANDLE_REQUEST = "handleRequest";
    private static final String MESSAGE_ID = "messageId";
    private static final String UUID = "uuid";
    private static final String HANDLER_PACKAGE = "com.awssamples.iot.dynamodb.api.handlers";
    private static final String SQS_EVENT_HANDLER = String.join(".", HANDLER_PACKAGE, "HandleSqsEvent");
    // GET
    private static final String IOT_GET_EVENT_HANDLER = String.join(".", HANDLER_PACKAGE, "HandleIotGetEvent");
    private static final String GET_TOPIC_KEY = "getTopic";
    private static final String GET_RESPONSE_TOPIC_PREFIX_KEY = "getResponseTopicPrefix";
    private static final String GET_TOPIC_TEMPLATE = String.join("/", "request", "get", UUID, MESSAGE_ID);
    private static final String DEFAULT_GET_RESPONSE_TOPIC_PREFIX = "response/get";
    // QUERY
    private static final String IOT_QUERY_EVENT_HANDLER = String.join(".", HANDLER_PACKAGE, "HandleIotQueryEvent");
    private static final String QUERY_TOPIC_KEY = "queryTopic";
    private static final String QUERY_RESPONSE_TOPIC_PREFIX_KEY = "queryResponseTopicPrefix";
    private static final String QUERY_TOPIC_TEMPLATE = String.join("/", "request", "query", UUID);
    private static final String DEFAULT_QUERY_RESPONSE_TOPIC_PREFIX = "response/query";
    // DELETE
    private static final String IOT_DELETE_EVENT_HANDLER = String.join(".", HANDLER_PACKAGE, "HandleIotDeleteEvent");
    private static final String DELETE_TOPIC_KEY = "deleteTopic";
    private static final String DELETE_RESPONSE_TOPIC_PREFIX_KEY = "deleteResponseTopicPrefix";
    private static final String DELETE_TOPIC_TEMPLATE = String.join("/", "request", "delete", UUID, MESSAGE_ID);
    private static final String DEFAULT_DELETE_RESPONSE_TOPIC_PREFIX = "response/delete";
    // NEXT
    private static final String IOT_NEXT_EVENT_HANDLER = String.join(".", HANDLER_PACKAGE, "HandleIotNextEvent");
    private static final String NEXT_TOPIC_KEY = "nextTopic";
    private static final String NEXT_RESPONSE_TOPIC_PREFIX_KEY = "nextResponseTopicPrefix";
    private static final String NEXT_TOPIC_TEMPLATE = String.join("/", "request", "next", UUID, MESSAGE_ID);
    private static final String DEFAULT_NEXT_RESPONSE_TOPIC_PREFIX = "response/next";
    private final String projectDirectory = "../dynamodb-api/java/";
    private final List<File> projectDirectoryFiles = singletonList(new File(projectDirectory));
    private final String buildOutputDirectory = "build/libs/";
    private final String outputJar = "java-1.0-SNAPSHOT-all.jar";
    private final Duration queueVisibilityTimeout = Duration.seconds(30);
    // Queue visibility timeout must be greater than Lambda timeout
    private final Duration lambdaFunctionTimeout = Duration.seconds((int) queueVisibilityTimeout.toSeconds() / 2);
    private final String iotServicePrincipal = Fn.join(".", Arrays.asList("iot", getUrlSuffix()));

    public SqsToIotCoreStack(final Construct parent, final String name) {
        super(parent, name);

        // Build all of the necessary JARs
        projectDirectoryFiles.forEach(this::buildJar);

        Optional<Map<String, String>> optionalArguments = CdkHelper.getArguments();

        // Get the queue ARN from the arguments or build the message queue and extract the ARN
        String sqsQueueArn = optionalArguments
                .flatMap(this::getQueueArn)
                .orElseGet(() -> buildMessageQueue().getQueueArn());

        Table messageTable = buildMessageTable();

        // Resources to move messages from SQS to DynamoDB
        Role moveFromSqsToDynamoDbRole = buildMoveFromSqsToDynamoDbRole(sqsQueueArn, messageTable);
        Function moveFromSqsToDynamoDb = buildMoveFromSqsToDynamoDbLambda(sqsQueueArn, messageTable, moveFromSqsToDynamoDbRole);
        buildEventSourceMapping(sqsQueueArn, moveFromSqsToDynamoDb);

        // Default environment for all functions just contains the table name
        Map<String, String> defaultEnvironment = getDefaultEnvironment(messageTable);

        PolicyStatement getItemPolicyStatementForMessageTable = getGetItemPolicyStatementForTable(messageTable);
        PolicyStatement queryPolicyStatementForMessageTable = getQueryPolicyStatementForTable(messageTable);
        PolicyStatement deleteItemPolicyStatementForMessageTable = getDeleteItemPolicyStatementForTable(messageTable);
        PolicyStatement nextItemPolicyStatementForMessageTable = getNextItemPolicyStatementForTable(messageTable);

        // Resources to get messages from DynamoDB and publish to IoT Core
        Role getMessageFromDynamoDbRole = buildIotEventRole("GetMessageFromDynamoDbRole", DEFAULT_GET_RESPONSE_TOPIC_PREFIX, singletonList(getItemPolicyStatementForMessageTable));
        Map<String, String> getMessageEnvironment = getGetMessageLambdaEnvironment();
        Function getMessageFromDynamoDb = buildIotEventLambda("GetMessageFromDynamoDbLambda", getMessageFromDynamoDbRole, defaultEnvironment, getMessageEnvironment, IOT_GET_EVENT_HANDLER);
        CfnTopicRule getRule = buildIotEventRule("GetMessageFromDynamoDbTopicRule", getMessageFromDynamoDb, "request/get/#");
        allowIotTopicRuleToInvokeLambdaFunction(getRule, getMessageFromDynamoDb, "LambdaInvocationPermissionsForIotGetMessage");

        // Resources to query for available messages from DynamoDB and publish to IoT Core
        Role queryMessageFromDynamoDbRole = buildIotEventRole("QueryMessageFromDynamoDbRole", DEFAULT_QUERY_RESPONSE_TOPIC_PREFIX, Arrays.asList(getItemPolicyStatementForMessageTable, queryPolicyStatementForMessageTable));
        Map<String, String> queryMessageEnvironment = getQueryMessageLambdaEnvironment();
        Function queryMessageFromDynamoDb = buildIotEventLambda("QueryMessageFromDynamoDbLambda", queryMessageFromDynamoDbRole, defaultEnvironment, queryMessageEnvironment, IOT_QUERY_EVENT_HANDLER);
        CfnTopicRule queryRule = buildIotEventRule("QueryMessageFromDynamoDbTopicRule", queryMessageFromDynamoDb, "request/query/+");
        allowIotTopicRuleToInvokeLambdaFunction(queryRule, queryMessageFromDynamoDb, "LambdaInvocationPermissionsForIotQueryMessage");

        // Resources to delete messages from DynamoDB
        Role deleteMessageFromDynamoDbRole = buildIotEventRole("DeleteMessageFromDynamoDbRole", DEFAULT_DELETE_RESPONSE_TOPIC_PREFIX, singletonList(deleteItemPolicyStatementForMessageTable));
        Map<String, String> deleteMessageEnvironment = getDeleteMessageLambdaEnvironment();
        Function deleteMessageFromDynamoDb = buildIotEventLambda("DeleteMessageFromDynamoDbLambda", deleteMessageFromDynamoDbRole, defaultEnvironment, deleteMessageEnvironment, IOT_DELETE_EVENT_HANDLER);
        CfnTopicRule deleteRule = buildIotEventRule("DeleteMessageFromDynamoDbTopicRule", deleteMessageFromDynamoDb, "request/delete/#");
        allowIotTopicRuleToInvokeLambdaFunction(deleteRule, deleteMessageFromDynamoDb, "LambdaInvocationPermissionsForIotDeleteMessage");

        // Resources to get the next message ID from DynamoDB
        Role nextMessageFromDynamoDbRole = buildIotEventRole("NextMessageFromDynamoDbRole", DEFAULT_NEXT_RESPONSE_TOPIC_PREFIX, singletonList(nextItemPolicyStatementForMessageTable));
        Map<String, String> nextMessageEnvironment = getNextMessageLambdaEnvironment();
        Function nextMessageFromDynamoDb = buildIotEventLambda("NextMessageFromDynamoDbLambda", nextMessageFromDynamoDbRole, defaultEnvironment, nextMessageEnvironment, IOT_NEXT_EVENT_HANDLER);
        CfnTopicRule nextRule = buildIotEventRule("NextMessageFromDynamoDbTopicRule", nextMessageFromDynamoDb, "request/next/#");
        allowIotTopicRuleToInvokeLambdaFunction(nextRule, nextMessageFromDynamoDb, "LambdaInvocationPermissionsForIotNextMessage");
    }

    private Optional<String> getQueueArn(Map<String, String> map) {
        return Optional.ofNullable(map.get(SQS_QUEUE_ARN_PARAMETER));
    }

    private CfnPermission allowIotTopicRuleToInvokeLambdaFunction(CfnTopicRule topicRule, Function function, String permissionName) {
        CfnPermissionProps cfnPermissionProps = CfnPermissionProps.builder()
                .sourceArn(topicRule.getAttrArn())
                .action(Permissions.LAMBDA_INVOKE_FUNCTION)
                .principal(iotServicePrincipal)
                .sourceAccount(getAccount())
                .functionName(function.getFunctionArn())
                .build();

        return new CfnPermission(this, permissionName, cfnPermissionProps);
    }

    private CfnTopicRule buildIotEventRule(String topicRuleName, Function lambda, String topic) {
        CfnTopicRule.ActionProperty actionProperty = CfnTopicRule.ActionProperty.builder()
                .lambda(CfnTopicRule.LambdaActionProperty.builder()
                        .functionArn(lambda.getFunctionArn())
                        .build())
                .build();
        CfnTopicRule.TopicRulePayloadProperty topicRulePayloadProperty = CfnTopicRule.TopicRulePayloadProperty.builder()
                .actions(singletonList(actionProperty))
                .ruleDisabled(false)
                .sql("select *, topic() as topic from '" + topic + "'")
                .build();
        CfnTopicRuleProps cfnTopicRuleProps = CfnTopicRuleProps.builder()
                .topicRulePayload(topicRulePayloadProperty)
                .build();
        CfnTopicRule cfnTopicRule = new CfnTopicRule(this, topicRuleName, cfnTopicRuleProps);

        return cfnTopicRule;
    }

    private Queue buildMessageQueue() {
        QueueProps queueProps = QueueProps.builder()
                .visibilityTimeout(queueVisibilityTimeout)
                .build();
        return new Queue(this, "SqsQueue", queueProps);
    }

    private EventSourceMapping buildEventSourceMapping(String sqsQueueArn, Function lambda) {
        EventSourceMappingProps eventSourceMappingProps = EventSourceMappingProps.builder()
                .eventSourceArn(sqsQueueArn)
                .batchSize(1)
                .target(lambda)
                .enabled(true)
                .build();

        return new EventSourceMapping(this, "MoveFromSqsToDynamoDbEventSourceMapping", eventSourceMappingProps);
    }

    private Map<String, String> getDefaultEnvironment(Table table) {
        Map<String, String> environment = new HashMap<>();
        environment.put(DYNAMO_DB_TABLE_ARN, table.getTableArn());

        return environment;
    }

    private Map<String, String> getGetMessageLambdaEnvironment() {
        Map<String, String> environment = new HashMap<>();
        environment.put(GET_TOPIC_KEY, GET_TOPIC_TEMPLATE);
        environment.put(GET_RESPONSE_TOPIC_PREFIX_KEY, DEFAULT_GET_RESPONSE_TOPIC_PREFIX);

        return environment;
    }

    private Map<String, String> getQueryMessageLambdaEnvironment() {
        Map<String, String> environment = new HashMap<>();
        environment.put(QUERY_TOPIC_KEY, QUERY_TOPIC_TEMPLATE);
        environment.put(QUERY_RESPONSE_TOPIC_PREFIX_KEY, DEFAULT_QUERY_RESPONSE_TOPIC_PREFIX);

        return environment;
    }

    private Map<String, String> getDeleteMessageLambdaEnvironment() {
        Map<String, String> environment = new HashMap<>();
        environment.put(DELETE_TOPIC_KEY, DELETE_TOPIC_TEMPLATE);
        environment.put(DELETE_RESPONSE_TOPIC_PREFIX_KEY, DEFAULT_DELETE_RESPONSE_TOPIC_PREFIX);

        return environment;
    }

    private Map<String, String> getNextMessageLambdaEnvironment() {
        Map<String, String> environment = new HashMap<>();
        environment.put(NEXT_TOPIC_KEY, NEXT_TOPIC_TEMPLATE);
        environment.put(NEXT_RESPONSE_TOPIC_PREFIX_KEY, DEFAULT_NEXT_RESPONSE_TOPIC_PREFIX);

        return environment;
    }

    private Function buildIotEventLambda(String functionName, Role role, Map<String, String> defaultEnvironment, Map<String, String> additionalEnvironment, String handlerClassName) {
        Map<String, String> environment = new HashMap<>(defaultEnvironment);
        environment.putAll(additionalEnvironment);

        FunctionProps functionProps = FunctionProps.builder()
                .code(Code.fromAsset(String.join("", projectDirectory, buildOutputDirectory, outputJar)))
                .handler(String.join("::", handlerClassName, HANDLE_REQUEST))
                .memorySize(1024)
                .timeout(lambdaFunctionTimeout)
                .environment(environment)
                .runtime(Runtime.JAVA_8)
                .role(role)
                .build();

        return new Function(this, functionName, functionProps);
    }

    private Function buildMoveFromSqsToDynamoDbLambda(String queueArn, Table table, Role role) {
        Map<String, String> environment = new HashMap<>();
        environment.put(SQS_QUEUE_ARN, queueArn);
        environment.put(DYNAMO_DB_TABLE_ARN, table.getTableArn());
        environment.put(UUID_KEY, "thingName");
        environment.put(MESSAGE_ID_KEY, "epochTime");

        FunctionProps functionProps = FunctionProps.builder()
                .code(Code.fromAsset(String.join("", projectDirectory, buildOutputDirectory, outputJar)))
                .handler(String.join("::", SQS_EVENT_HANDLER, HANDLE_REQUEST))
                .memorySize(1024)
                .timeout(lambdaFunctionTimeout)
                .environment(environment)
                .runtime(Runtime.JAVA_8)
                .role(role)
                .build();

        return new Function(this, "MoveFromSqsToDynamoDbLambda", functionProps);
    }

    private void buildJar(File projectDirectoryFile) {
        ProjectConnection projectConnection = GradleConnector.newConnector()
                .forProjectDirectory(projectDirectoryFile)
                .connect();

        Try.withResources(() -> projectConnection)
                .of(this::runBuild)
                .get();
    }

    private Void runBuild(ProjectConnection projectConnection) {
        // Build with gradle and send the output to stderr
        BuildLauncher build = projectConnection.newBuild();
        build.forTasks("build");
        build.setStandardOutput(System.err);
        build.run();

        return null;
    }

    private Role buildMoveFromSqsToDynamoDbRole(String queueArn, Table table) {
        PolicyStatement sqsPolicyStatement = LambdaPolicies.getMinimalLambdaSqsQueueEventSourceMappingTargetPolicy(queueArn);

        PolicyStatementProps dynamoDbPolicyStatementProps = PolicyStatementProps.builder()
                .effect(Effect.ALLOW)
                .resources(singletonList(table.getTableArn()))
                .actions(singletonList(Permissions.DYNAMODB_PUT_ITEM_PERMISSION))
                .build();
        PolicyStatement dynamoDbPolicyStatement = new PolicyStatement(dynamoDbPolicyStatementProps);

        PolicyDocumentProps policyDocumentProps = PolicyDocumentProps.builder()
                .statements(Arrays.asList(sqsPolicyStatement,
                        CloudWatchEventsPolicies.getMinimalCloudWatchEventsLoggingPolicy(),
                        dynamoDbPolicyStatement))
                .build();
        PolicyDocument policyDocument = new PolicyDocument(policyDocumentProps);

        Map<String, PolicyDocument> policyDocuments = new HashMap<>();
        policyDocuments.put("root", policyDocument);

        RoleProps moveFromSqsToDynamoDbRoleProps = RoleProps.builder()
                .assumedBy(LambdaPolicies.LAMBDA_SERVICE_PRINCIPAL)
                .inlinePolicies(policyDocuments)
                .build();

        return new Role(this, "MoveFromSqsToDynamoDbRole", moveFromSqsToDynamoDbRoleProps);
    }

    private Role buildIotEventRole(String roleName, String topicPrefix, List<PolicyStatement> additionalPolicyStatements) {
        PolicyStatementProps iotPolicyStatementProps = PolicyStatementProps.builder()
                .effect(Effect.ALLOW)
                .resources(singletonList(Fn.join("", Arrays.asList("arn:aws:iot:", getRegion(), ":", getAccount(), ":topic/", topicPrefix, "/*"))))
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

        RoleProps moveFromSqsToDynamoDbRoleProps = RoleProps.builder()
                .assumedBy(LambdaPolicies.LAMBDA_SERVICE_PRINCIPAL)
                .inlinePolicies(policyDocuments)
                .build();

        return new Role(this, roleName, moveFromSqsToDynamoDbRoleProps);
    }

    private PolicyStatement getGetItemPolicyStatementForTable(Table table) {
        return getPolicyStatementForTable(table, Permissions.DYNAMODB_GET_ITEM_PERMISSION);
    }

    private PolicyStatement getQueryPolicyStatementForTable(Table table) {
        return getPolicyStatementForTable(table, Permissions.DYNAMODB_QUERY_PERMISSION);
    }

    private PolicyStatement getDeleteItemPolicyStatementForTable(Table table) {
        return getPolicyStatementForTable(table, Permissions.DYNAMODB_DELETE_ITEM_PERMISSION);
    }

    private PolicyStatement getNextItemPolicyStatementForTable(Table table) {
        return getPolicyStatementForTable(table, Permissions.DYNAMODB_QUERY_PERMISSION);
    }

    private PolicyStatement getPolicyStatementForTable(Table table, String action) {
        PolicyStatementProps dynamoDbPolicyStatementProps = PolicyStatementProps.builder()
                .effect(Effect.ALLOW)
                .resources(singletonList(table.getTableArn()))
                .actions(singletonList(action))
                .build();
        return new PolicyStatement(dynamoDbPolicyStatementProps);
    }

    private Table buildMessageTable() {
        Attribute uuid = Attribute.builder()
                .type(AttributeType.STRING)
                .name(UUID)
                .build();

        Attribute messageId = Attribute.builder()
                .type(AttributeType.STRING)
                .name(MESSAGE_ID)
                .build();

        TableProps tableProps = TableProps.builder()
                .partitionKey(uuid)
                .sortKey(messageId)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        return new Table(this, "DynamoDbTable", tableProps);
    }
}
