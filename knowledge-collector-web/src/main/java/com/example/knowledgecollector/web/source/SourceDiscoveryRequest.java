package com.example.knowledgecollector.web.source;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SourceDiscoveryRequest(
        @NotBlank @Size(max = 128) String topic,
        @NotBlank @Size(max = 16) String language,
        @Min(1) @Max(50) int count,
        @Size(max = 16) String quality) {
}
