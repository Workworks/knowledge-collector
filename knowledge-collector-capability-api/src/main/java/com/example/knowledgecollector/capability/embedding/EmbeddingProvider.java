package com.example.knowledgecollector.capability.embedding;

import java.util.List;

public interface EmbeddingProvider {
    EmbeddingResult embed(EmbeddingRequest request);

    record EmbeddingRequest(List<String> inputs, String model) {
    }

    record EmbeddingResult(List<List<Double>> vectors, String model) {
    }
}
