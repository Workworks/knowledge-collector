package com.example.knowledgecollector.provider.web;

import com.example.knowledgecollector.capability.security.UrlSecurityValidator;
import com.example.knowledgecollector.capability.web.WebContentProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;

@Component
public class JdkWebContentProvider implements WebContentProvider {
    private final UrlSecurityValidator validator;
    private final boolean networkEnabled;

    public JdkWebContentProvider(UrlSecurityValidator validator,
            @Value("${knowledge-collector.network.enabled:true}") boolean networkEnabled) {
        this.validator = validator;
        this.networkEnabled = networkEnabled;
    }

    @Override
    public WebResponse get(WebRequest request) {
        if (!networkEnabled) {
            throw new IllegalStateException("NETWORK-DISABLED: 当前环境已禁用外部网络请求");
        }
        URI current = validated(request.url());
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(request.timeoutSeconds()))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        for (int redirect = 0; redirect <= 5; redirect++) {
            try {
                HttpRequest httpRequest = HttpRequest.newBuilder(current)
                        .timeout(Duration.ofSeconds(request.timeoutSeconds()))
                        .header("User-Agent", request.userAgent())
                        .header("Accept", "application/rss+xml,application/atom+xml,application/xml,text/xml,text/html")
                        .header("Accept-Language", request.language()).GET().build();
                HttpResponse<InputStream> response =
                        client.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() >= 300 && response.statusCode() < 400) {
                    String location = response.headers().firstValue("location")
                            .orElseThrow(() -> new IllegalStateException("重定向缺少 Location"));
                    current = validated(current.resolve(location).toString());
                    continue;
                }
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("远端 HTTP " + response.statusCode());
                }
                int maxBytes = Math.max(1, request.maxBytes());
                try (InputStream input = response.body();
                     ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int read;
                    int total = 0;
                    while ((read = input.read(buffer)) != -1) {
                        total += read;
                        if (total > maxBytes) {
                            throw new IllegalStateException("响应超过 " + maxBytes + " 字节");
                        }
                        output.write(buffer, 0, read);
                    }
                    var headers = new LinkedHashMap<String, String>();
                    response.headers().map().forEach((key, values) ->
                            headers.put(key, String.join(",", values)));
                    return new WebResponse(current.toString(), response.statusCode(),
                            response.headers().firstValue("content-type").orElse(""),
                            output.toByteArray(), headers);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("请求被中断", exception);
            } catch (Exception exception) {
                throw exception instanceof IllegalStateException state ? state
                        : new IllegalStateException(exception.getMessage(), exception);
            }
        }
        throw new IllegalStateException("重定向次数超过 5");
    }

    private URI validated(String url) {
        var result = validator.validate(url);
        if (!result.valid()) {
            throw new IllegalArgumentException(result.errorCode() + ": " + result.message());
        }
        return result.normalizedUri();
    }
}
