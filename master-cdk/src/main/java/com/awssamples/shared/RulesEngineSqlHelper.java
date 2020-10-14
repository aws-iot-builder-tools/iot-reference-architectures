package com.awssamples.shared;

import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.iot.CfnTopicRule;
import software.amazon.awscdk.services.iot.CfnTopicRuleProps;
import software.amazon.awscdk.services.lambda.Function;

import static java.util.Collections.singletonList;

public class RulesEngineSqlHelper {
    public static final String AWS_IOT_SQL_VERSION = "2016-03-23";

    public static CfnTopicRule buildIotEventRule(Stack stack, String topicRuleName, Function lambda, String sql) {
        CfnTopicRule.ActionProperty actionProperty = CfnTopicRule.ActionProperty.builder()
                .lambda(CfnTopicRule.LambdaActionProperty.builder()
                        .functionArn(lambda.getFunctionArn())
                        .build())
                .build();

        CfnTopicRule.TopicRulePayloadProperty topicRulePayloadProperty = CfnTopicRule.TopicRulePayloadProperty.builder()
                .actions(singletonList(actionProperty))
                .ruleDisabled(false)
                .sql(sql)
                .awsIotSqlVersion(AWS_IOT_SQL_VERSION)
                .build();

        CfnTopicRuleProps cfnTopicRuleProps = CfnTopicRuleProps.builder()
                .topicRulePayload(topicRulePayloadProperty)
                .build();

        return new CfnTopicRule(stack, topicRuleName, cfnTopicRuleProps);
    }

    public static CfnTopicRule buildIotEventRule(Stack stack, String topicRuleName, CfnTopicRule.RepublishActionProperty republishActionProperty, String sql) {
        CfnTopicRule.ActionProperty actionProperty = CfnTopicRule.ActionProperty.builder()
                .republish(republishActionProperty)
                .build();

        CfnTopicRule.TopicRulePayloadProperty topicRulePayloadProperty = CfnTopicRule.TopicRulePayloadProperty.builder()
                .actions(singletonList(actionProperty))
                .ruleDisabled(false)
                .sql(sql)
                .awsIotSqlVersion(AWS_IOT_SQL_VERSION)
                .build();

        CfnTopicRuleProps cfnTopicRuleProps = CfnTopicRuleProps.builder()
                .topicRulePayload(topicRulePayloadProperty)
                .build();

        return new CfnTopicRule(stack, topicRuleName, cfnTopicRuleProps);
    }

    public static CfnTopicRule buildIotEventRule(Stack stack, String topicRuleName, Function lambda, String selectClause, String topicFilter) {
        String quotedTopicFilter = "'" + topicFilter + "'";
        String sql = String.join(" ", selectClause, "from", quotedTopicFilter);

        return buildIotEventRule(stack, topicRuleName, lambda, sql);
    }

    public static CfnTopicRule buildIotEventRule(Stack stack, String topicRuleName, String sql, Function lambda) {
        CfnTopicRule.ActionProperty actionProperty = CfnTopicRule.ActionProperty.builder()
                .lambda(CfnTopicRule.LambdaActionProperty.builder()
                        .functionArn(lambda.getFunctionArn())
                        .build())
                .build();

        CfnTopicRule.TopicRulePayloadProperty topicRulePayloadProperty = CfnTopicRule.TopicRulePayloadProperty.builder()
                .actions(singletonList(actionProperty))
                .ruleDisabled(false)
                .sql(sql)
                .awsIotSqlVersion(AWS_IOT_SQL_VERSION)
                .build();

        CfnTopicRuleProps cfnTopicRuleProps = CfnTopicRuleProps.builder()
                .topicRulePayload(topicRulePayloadProperty)
                .build();

        return new CfnTopicRule(stack, topicRuleName, cfnTopicRuleProps);
    }

    public static CfnTopicRule buildSelectAllIotEventRule(Stack stack, String topicRuleNamePrefix, Function lambda, String topic) {
        return RulesEngineSqlHelper.buildIotEventRule(stack, topicRuleNamePrefix + "TopicRule", lambda, "select * from '" + topic + "'");
    }

    public static CfnTopicRule buildSelectAllBinaryIotEventRule(Stack stack, String topicRuleNamePrefix, Function lambda, String topic) {
        return RulesEngineSqlHelper.buildIotEventRule(stack, topicRuleNamePrefix + "TopicRule", lambda, "select encode(*, 'base64') as data from '" + topic + "'");
    }
}