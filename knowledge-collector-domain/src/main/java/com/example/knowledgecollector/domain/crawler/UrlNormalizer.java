package com.example.knowledgecollector.domain.crawler;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;

public final class UrlNormalizer {
    private static final Set<String> TRACKING = Set.of("fbclid", "gclid", "mc_cid", "mc_eid");
    private UrlNormalizer() {}

    public static NormalizedUrl normalize(String value, String baseUrl) {
        try {
            URI base = baseUrl == null ? null : URI.create(baseUrl);
            URI uri = base == null ? URI.create(value) : base.resolve(value);
            String scheme = uri.getScheme().toLowerCase();
            String host = uri.getHost().toLowerCase();
            int port = ("http".equals(scheme) && uri.getPort() == 80)
                    || ("https".equals(scheme) && uri.getPort() == 443) ? -1 : uri.getPort();
            String path = uri.normalize().getPath().replaceAll("/{2,}", "/");
            if (path.isBlank()) path = "/";
            String query = normalizeQuery(uri.getRawQuery());
            URI normalized = new URI(scheme, uri.getUserInfo(), host, port, path, query, null);
            String text = normalized.toASCIIString();
            return new NormalizedUrl(text, sha256(text));
        } catch (RuntimeException | URISyntaxException exception) {
            throw new IllegalArgumentException("无法规范化 URL", exception);
        }
    }

    private static String normalizeQuery(String query) {
        if (query == null || query.isBlank()) return null;
        return Arrays.stream(query.split("&"))
                .filter(part -> {
                    String key = part.split("=", 2)[0].toLowerCase();
                    return !key.startsWith("utm_") && !TRACKING.contains(key);
                }).sorted(Comparator.naturalOrder()).collect(Collectors.joining("&"));
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public record NormalizedUrl(String value, String hash) {}
}
