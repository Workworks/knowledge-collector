package com.example.knowledgecollector.capability.clustering;

import java.time.OffsetDateTime;
import java.util.List;

public interface ContentClusteringProvider {
    ClusterResult cluster(List<ArticleFeature> articles);

    record ArticleFeature(String id, String title, List<String> keywords, OffsetDateTime publishTime) {
    }

    record ClusterResult(List<List<String>> clusters) {
    }
}
