package com.example.knowledgecollector.capability.source;

import java.util.List;

/** 面向公开搜索服务的采集源发现能力。 */
public interface SourceDiscoveryProvider {
    String id();

    List<Candidate> discover(Request request);

    record Request(String topic, String language, int count, String sourceType, String qualityLevel) {
    }

    record Candidate(String name, String websiteUrl, String collectionUrl, String sourceType,
                     String language, int reliabilityScore, String reason) {
    }
}
