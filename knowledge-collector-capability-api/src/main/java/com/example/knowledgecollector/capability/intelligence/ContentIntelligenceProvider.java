package com.example.knowledgecollector.capability.intelligence;

import java.util.Map;
import java.util.Set;

public interface ContentIntelligenceProvider {
    AnalysisResult analyze(AnalysisRequest request);

    enum Capability {
        SUMMARY, CLASSIFICATION, KEYWORD_EXTRACTION, ENTITY_EXTRACTION,
        TAG_GENERATION, QUALITY_SCORING, RELATED_ARTICLE_DISCOVERY
    }

    record AnalysisRequest(String title, String content, Set<Capability> capabilities) {
    }

    record AnalysisResult(Map<String, Object> values, String provider) {
    }
}
