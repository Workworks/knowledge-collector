package com.example.knowledgecollector.application.advanced;

import java.util.List;
import java.util.Map;

public interface AdvancedCapabilityGateway {
    long saveExecution(String stage, String operation, String status, String requestJson,
                       String resultJson, String error, Long retryOfId, long durationMillis);
    Map<String, Object> getExecution(long id);
    List<Map<String, Object>> listExecutions(String stage, int limit);
    List<Map<String, Object>> searchableDocuments();
    long createImportedArticle(String title, String url, String content, String metadataJson, boolean aiContent);
    Map<String, Object> metrics();
    void saveSetting(String key, String value);
    Map<String, String> settings(String prefix);
}
