package io.vertx.fargate.providers;

import io.vertx.fargate.data.ScopeDownConfiguration;
import io.vertx.fargate.providers.interfaces.ScopeDownPolicyProvider;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasicScopeDownPolicyProvider implements ScopeDownPolicyProvider {
    @Inject
    public BasicScopeDownPolicyProvider() {
    }

    @Override
    public Map generateScopeDownPolicy(ScopeDownConfiguration scopeDownConfiguration) {
        Map<String, Object> map = new HashMap();
        map.put("Version", "2012-10-17");

        List<Map> statements = new ArrayList<>();

        scopeDownConfiguration
                .getConfiguration()
                .entrySet().stream()
                .forEach(entry -> {
                    Map iotStatementMap = new HashMap();
                    iotStatementMap.put("Action", entry.getKey());
                    iotStatementMap.put("Effect", "Allow");
                    iotStatementMap.put("Resource", entry.getValue());
                    statements.add(iotStatementMap);
                });

        map.put("Statement", statements);

        return map;
    }
}
