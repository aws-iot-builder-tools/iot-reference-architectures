package io.vertx.fargate.providers.interfaces;

import java.util.List;
import java.util.Map;

public interface ScopeDownConfigurationProvider {
    Map<String, List<String>> generateScopeDownConfiguration(List<String> allowedClientIds, List<String> allowedPublishTopics, List<String> allowedReceiveTopics, List<String> allowedSubscribeTopicFilters);
}
