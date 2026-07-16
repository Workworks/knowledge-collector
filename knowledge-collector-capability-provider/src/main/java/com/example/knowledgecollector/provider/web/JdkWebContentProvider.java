package com.example.knowledgecollector.provider.web;

import com.example.knowledgecollector.capability.security.UrlSecurityValidator;
import com.example.knowledgecollector.capability.web.WebContentProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.net.ssl.SSLHandshakeException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

@Component
public class JdkWebContentProvider implements WebContentProvider {
    private final UrlSecurityValidator validator;
    private final boolean networkEnabled;
    private final TlsContextFactory tls;

    public JdkWebContentProvider(UrlSecurityValidator validator,
            @Value("${knowledge-collector.network.enabled:true}") boolean networkEnabled,
            TlsContextFactory tls) {
        this.validator = validator;
        this.networkEnabled = networkEnabled;
        this.tls = tls;
    }

    @Override
    public WebResponse get(WebRequest request) {
        if (!networkEnabled) {
            throw new IllegalStateException("NETWORK-DISABLED: 当前环境已禁用外部网络请求");
        }
        URI current = validated(request.url());
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(request.timeoutSeconds()))
                .sslContext(tls.sslContext())
                .followRedirects(HttpClient.Redirect.NEVER).build();
        for (int redirect = 0; redirect <= 5; redirect++) {
            try {
                HttpRequest httpRequest = HttpRequest.newBuilder(current)
                        .timeout(Duration.ofSeconds(request.timeoutSeconds()))
                        .header("User-Agent", request.userAgent())
                        .header("Accept", "application/rss+xml,application/atom+xml,application/xml,text/xml,text/html")
                        .header("Accept-Language", request.language()).GET().build();
                int maxBytes = Math.max(1, request.maxBytes());
                HttpResponse<byte[]> response =
                        client.send(httpRequest, info -> new LimitedByteArraySubscriber(maxBytes));
                if (response.statusCode() >= 300 && response.statusCode() < 400) {
                    String location = response.headers().firstValue("location")
                            .orElseThrow(() -> new IllegalStateException("重定向缺少 Location"));
                    current = validated(current.resolve(location).toString());
                    continue;
                }
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("远端 HTTP " + response.statusCode());
                }
                var headers = new LinkedHashMap<String, String>();
                response.headers().map().forEach((key, values) ->
                        headers.put(key, String.join(",", values)));
                return new WebResponse(current.toString(), response.statusCode(),
                        response.headers().firstValue("content-type").orElse(""),
                        response.body(), headers);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("请求被中断", exception);
            } catch (Exception exception) {
                if (causedBy(exception, HttpTimeoutException.class)) {
                    throw new IllegalStateException("HTTP-REQUEST-TIMEOUT: 请求在 "
                            + request.timeoutSeconds() + " 秒内未完成", exception);
                }
                if (causedBy(exception, SSLHandshakeException.class)
                        || containsMessage(exception, "PKIX path building failed")) {
                    throw new IllegalStateException("""
                            TLS-CERTIFICATE-UNTRUSTED: 目标站点证书链不受当前系统信任。\
                            请更新 JDK/系统根证书，或通过 KNOWLEDGE_COLLECTOR_ADDITIONAL_CA_FILE 配置 PEM CA 证书\
                            """.trim(), exception);
                }
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

    private boolean causedBy(Throwable throwable, Class<? extends Throwable> type) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsMessage(Throwable throwable, String text) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current.getMessage() != null && current.getMessage().contains(text)) {
                return true;
            }
        }
        return false;
    }

    private static final class LimitedByteArraySubscriber implements HttpResponse.BodySubscriber<byte[]> {
        private final int maxBytes;
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private final CompletableFuture<byte[]> body = new CompletableFuture<>();
        private Flow.Subscription subscription;
        private int total;

        private LimitedByteArraySubscriber(int maxBytes) {
            this.maxBytes = maxBytes;
        }

        @Override
        public CompletionStage<byte[]> getBody() {
            return body;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (this.subscription != null) {
                subscription.cancel();
                return;
            }
            this.subscription = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(List<ByteBuffer> buffers) {
            try {
                for (ByteBuffer buffer : buffers) {
                    int remaining = buffer.remaining();
                    total += remaining;
                    if (total > maxBytes) {
                        subscription.cancel();
                        body.completeExceptionally(
                                new IllegalStateException("响应超过 " + maxBytes + " 字节"));
                        return;
                    }
                    byte[] bytes = new byte[remaining];
                    buffer.get(bytes);
                    output.write(bytes);
                }
                subscription.request(1);
            } catch (Exception exception) {
                subscription.cancel();
                body.completeExceptionally(exception);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            body.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            body.complete(output.toByteArray());
        }
    }
}
