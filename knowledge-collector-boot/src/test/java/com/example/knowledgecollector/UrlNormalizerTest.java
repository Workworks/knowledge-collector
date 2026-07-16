package com.example.knowledgecollector;

import com.example.knowledgecollector.domain.crawler.UrlNormalizer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UrlNormalizerTest {
    @Test
    void normalizesRelativeUrlTrackingParametersQueryOrderAndFragment() {
        var normalized = UrlNormalizer.normalize(
                "../posts//42?utm_source=newsletter&b=2&a=1#section",
                "HTTPS://Example.COM:443/feed/index.xml"
        );

        assertThat(normalized.value()).isEqualTo("https://example.com/posts/42?a=1&b=2");
        assertThat(normalized.hash()).hasSize(64);
    }
}
