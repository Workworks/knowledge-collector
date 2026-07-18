package com.example.knowledgecollector.capability.management;

import java.util.List;

/** 可由管理页面动态配置、检查和调用的第三方能力。 */
public interface ManagedCapabilityProvider {
    String id();

    String serviceType();

    String displayName();

    String implementationName();

    List<String> businessUsages();

    RuntimeConfiguration currentConfiguration();

    void configure(RuntimeConfiguration configuration);

    ConnectionResult testConnection();

    record RuntimeConfiguration(boolean enabled, String endpoint, String model,
                                String authenticationType, String credential) {
    }

    record ConnectionResult(boolean available, String message, List<String> models) {
    }
}
