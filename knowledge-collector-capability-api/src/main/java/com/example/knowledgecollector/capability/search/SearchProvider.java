package com.example.knowledgecollector.capability.search;

import java.util.List;
import java.util.Map;

public interface SearchProvider {
    SearchResult search(SearchRequest request);
    void index(SearchDocument document);
    void delete(String documentId);

    record SearchRequest(String query, int page, int size, Map<String, String> filters) {
    }

    record SearchResult(List<SearchHit> hits, long total) {
    }

    record SearchHit(String documentId, double score, Map<String, Object> fields) {
    }

    record SearchDocument(String documentId, String title, String content, Map<String, Object> fields) {
    }
}
