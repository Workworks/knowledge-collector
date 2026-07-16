package com.example.knowledgecollector.infrastructure.web;

import com.example.knowledgecollector.application.exception.BusinessRuleException;
import com.example.knowledgecollector.domain.source.CrawlSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class SafeWebContentClient {
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private final UrlSecurityValidator validator;
    private final boolean networkEnabled;

    public SafeWebContentClient(UrlSecurityValidator validator,
                                @Value("${knowledge-collector.network.enabled:true}") boolean networkEnabled) {
        this.validator = validator;
        this.networkEnabled = networkEnabled;
    }

    public byte[] get(CrawlSource source) {
        if (!networkEnabled) {
            throw new BusinessRuleException("NETWORK-DISABLED", "当前环境已禁用外部网络请求");
        }
        URI current = validator.validate(source.feedUrl());
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(source.timeoutSeconds()))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        for (int redirect = 0; redirect <= 5; redirect++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(current)
                        .timeout(Duration.ofSeconds(source.timeoutSeconds()))
                        .header("User-Agent", source.userAgent())
                        .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
                        .header("Accept-Language", source.language()).GET().build();
                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() >= 300 && response.statusCode() < 400) {
                    String location = response.headers().firstValue("location")
                            .orElseThrow(() -> new IllegalStateException("重定向缺少 Location"));
                    current = validator.validate(current.resolve(location).toString());
                    continue;
                }
                if (response.statusCode() != 200) throw new IllegalStateException("远端 HTTP " + response.statusCode());
                String type = response.headers().firstValue("content-type").orElse("").toLowerCase();
                if (!(type.contains("xml") || type.contains("rss") || type.contains("atom")))
                    throw new IllegalStateException("响应 Content-Type 不是 Feed/XML");
                try (InputStream input = response.body(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192]; int read, total = 0;
                    while ((read = input.read(buffer)) != -1) {
                        total += read; if (total > MAX_BYTES) throw new IllegalStateException("响应超过 5MB");
                        output.write(buffer, 0, read);
                    }
                    return output.toByteArray();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt(); throw new IllegalStateException("请求被中断", exception);
            } catch (Exception exception) {
                throw new IllegalStateException(exception.getMessage(), exception);
            }
        }
        throw new IllegalStateException("重定向次数超过 5");
    }
}
