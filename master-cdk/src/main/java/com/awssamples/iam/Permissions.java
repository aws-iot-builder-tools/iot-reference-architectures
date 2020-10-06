package com.awssamples.iam;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.PolicyStatementProps;

import static java.util.Collections.singletonList;

public class Permissions {
    public static final String ALL_RESOURCES = "*";
    public static final String PERMISSION_DELIMITER = ":";
    private static final String DYNAMODB_PERMISSION_PREFIX = "dynamodb";
    public static final String DYNAMODB_GET_ITEM_PERMISSION = String.join(PERMISSION_DELIMITER, DYNAMODB_PERMISSION_PREFIX, "GetItem");
    public static final String DYNAMODB_PUT_ITEM_PERMISSION = String.join(PERMISSION_DELIMITER, DYNAMODB_PERMISSION_PREFIX, "PutItem");
    public static final String DYNAMODB_QUERY_PERMISSION = String.join(PERMISSION_DELIMITER, DYNAMODB_PERMISSION_PREFIX, "Query");
    public static final String DYNAMODB_SCAN_PERMISSION = String.join(PERMISSION_DELIMITER, DYNAMODB_PERMISSION_PREFIX, "Scan");
    public static final String DYNAMODB_DELETE_ITEM_PERMISSION = String.join(PERMISSION_DELIMITER, DYNAMODB_PERMISSION_PREFIX, "DeleteItem");
    private static final String LAMBDA_PERMISSION_PREFIX = "lambda";
    public static final String LAMBDA_INVOKE_FUNCTION = String.join(PERMISSION_DELIMITER, LAMBDA_PERMISSION_PREFIX, "InvokeFunction");
    private static final String IOT_PERMISSION_PREFIX = "iot";
    public static final String IOT_PUBLISH_PERMISSION = String.join(PERMISSION_DELIMITER, IOT_PERMISSION_PREFIX, "Publish");
    public static final String IOT_CREATE_THING_PERMISSION = String.join(PERMISSION_DELIMITER, IOT_PERMISSION_PREFIX, "CreateThing");
    public static final String IOT_CREATE_THING_GROUP_PERMISSION = String.join(PERMISSION_DELIMITER, IOT_PERMISSION_PREFIX, "CreateThingGroup");
    public static final String IOT_UPDATE_THING_GROUPS_FOR_THING_PERMISSION = String.join(PERMISSION_DELIMITER, IOT_PERMISSION_PREFIX, "UpdateThingGroupsForThing");
    public static final String IOT_UPDATE_THING_SHADOW_PERMISSION = String.join(PERMISSION_DELIMITER, IOT_PERMISSION_PREFIX, "UpdateThingShadow");
    public static final String IOT_SEARCH_INDEX_PERMISSION = String.join(PERMISSION_DELIMITER, IOT_PERMISSION_PREFIX, "SearchIndex");
    private static final String CLOUDWATCH_LOGS_PERMISSION_PREFIX = "logs";
    public static final String CLOUDWATCH_LOGS_CREATE_LOG_GROUP = String.join(PERMISSION_DELIMITER, CLOUDWATCH_LOGS_PERMISSION_PREFIX, "CreateLogGroup");
    public static final String CLOUDWATCH_LOGS_CREATE_LOG_STREAM = String.join(PERMISSION_DELIMITER, CLOUDWATCH_LOGS_PERMISSION_PREFIX, "CreateLogStream");
    public static final String CLOUDWATCH_LOGS_PUT_LOG_EVENTS = String.join(PERMISSION_DELIMITER, CLOUDWATCH_LOGS_PERMISSION_PREFIX, "PutLogEvents");
    public static final String CLOUDWATCH_LOGS_DESCRIBE_LOG_STREAMS = String.join(PERMISSION_DELIMITER, CLOUDWATCH_LOGS_PERMISSION_PREFIX, "DescribeLogStreams");

    @NotNull
    public static PolicyStatement getAllowAllPolicyStatement(String action) {
        PolicyStatementProps iotPolicyStatementProps = PolicyStatementProps.builder()
                .effect(Effect.ALLOW)
                .resources(singletonList(ALL_RESOURCES))
                .actions(singletonList(action))
                .build();

        return new PolicyStatement(iotPolicyStatementProps);
    }
}
