package com.example.knowledgecollector;

import com.example.knowledgecollector.capability.source.ContentSourceProvider;
import com.example.knowledgecollector.capability.web.WebContentProvider;
import com.example.knowledgecollector.provider.source.HtmlArticleContentExtractor;
import com.example.knowledgecollector.provider.source.ManualUrlSourceProvider;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ManualUrlSourceProviderTest {
    @Test
    void fetchesOneArticleFromManualUrl() {
        String html = """
                <html><head><title>手工采集测试</title><meta name="author" content="测试作者"></head>
                <body><article><h1>手工采集测试</h1><p>这是用于验证 MANUAL_URL Provider 的正文。</p>
                <p>正文包含足够的内容，以便通过文章正文抽取器的候选元素长度判断。</p>
                <p>采集完成后应返回一个资料条目，而不是提示未找到能力 Provider。</p></article></body></html>
                """;
        WebContentProvider web = request -> new WebContentProvider.WebResponse(
                request.url(), 200, "text/html; charset=UTF-8",
                html.getBytes(StandardCharsets.UTF_8), Map.of());
        ManualUrlSourceProvider provider = new ManualUrlSourceProvider(web, new HtmlArticleContentExtractor());

        var result = provider.fetch(new ContentSourceProvider.FetchRequest(
                "MANUAL_URL", "https://example.test/article", "https://example.test/article",
                "zh-CN", "UTF-8", "test", 5, true, false, Map.of()));

        assertThat(provider.supports("MANUAL_URL")).isTrue();
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).title()).isEqualTo("手工采集测试");
        assertThat(result.items().get(0).contentText()).contains("能力 Provider");
    }
}
