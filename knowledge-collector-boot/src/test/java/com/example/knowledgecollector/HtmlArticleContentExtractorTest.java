package com.example.knowledgecollector;

import com.example.knowledgecollector.provider.source.HtmlArticleContentExtractor;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlArticleContentExtractorTest {
    private final HtmlArticleContentExtractor extractor = new HtmlArticleContentExtractor();

    @Test
    void extractsMainArticleAndRemovesUnsafeOrNoisyContent() {
        String html = """
                <html><body><nav>navigation links</nav><main><article>
                <h1>Article title</h1><div class="entry-content">
                <p>A sufficiently long first paragraph describing memory consolidation and active recall.</p>
                <p>A second paragraph provides supporting evidence and makes the content extractor reliable.</p>
                <div class="advertisement">buy now</div><script>alert('unsafe')</script>
                </div></article></main></body></html>
                """;

        var result = extractor.extract(html.getBytes(StandardCharsets.UTF_8),
                "text/html; charset=UTF-8", "GBK", "https://example.test/article");

        assertThat(result.present()).isTrue();
        assertThat(result.text()).contains("memory consolidation", "supporting evidence")
                .doesNotContain("buy now", "unsafe");
        assertThat(result.html()).doesNotContain("script", "advertisement");
    }
}
