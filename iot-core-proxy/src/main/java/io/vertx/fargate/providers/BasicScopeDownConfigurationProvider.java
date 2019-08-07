package io.vertx.fargate.providers;

import io.vertx.fargate.mqtt.data.AccountId;
import io.vertx.fargate.providers.interfaces.ScopeDownConfigurationProvider;
import software.amazon.awssdk.regions.Region;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BasicScopeDownConfigurationProvider implements ScopeDownConfigurationProvider {
    private final String IOT_CONNECT = "iot:Connect";
    private final String IOT_PUBLISH = "iot:Publish";
    private final String ARN_AWS_IOT = "arn:aws:iot:";
    private final String IOT_RECEIVE = "iot:Receive";
    private final String IOT_SUBSCRIBE = "iot:Subscribe";
    private final String TOPIC = ":topic/";
    private final String TOPICFILTER = ":topicfilter/";
    @Inject
    AccountId accountId;
    @Inject
    Region region;

    @Inject
    public BasicScopeDownConfigurationProvider() {
    }

    @Override
    public Map<String, List<String>> generateScopeDownConfiguration(List<String> allowedClientIds, List<String> allowedPublishTopics, List<String> allowedReceiveTopics, List<String> allowedSubscribeTopicFilters) {
        Map<String, List<String>> scopeDownConfiguration = new HashMap<>();
        scopeDownConfiguration.put(IOT_CONNECT, allowedClientIds);

        scopeDownConfiguration.put(IOT_PUBLISH, allowedPublishTopics.stream()
                .map(publishTopic -> ARN_AWS_IOT + region.toString() + ":" + accountId.getAccountId() + TOPIC + publishTopic)
                .collect(Collectors.toList()));

        scopeDownConfiguration.put(IOT_RECEIVE, allowedReceiveTopics.stream()
                .map(receiveTopic -> ARN_AWS_IOT + region.toString() + ":" + accountId.getAccountId() + TOPIC + receiveTopic)
                .collect(Collectors.toList()));

        scopeDownConfiguration.put(IOT_SUBSCRIBE, allowedSubscribeTopicFilters.stream()
                .map(subscribeTopicFilter -> ARN_AWS_IOT + region.toString() + ":" + accountId.getAccountId() + TOPICFILTER + subscribeTopicFilter)
                .collect(Collectors.toList()));

        return scopeDownConfiguration;
    }
}
