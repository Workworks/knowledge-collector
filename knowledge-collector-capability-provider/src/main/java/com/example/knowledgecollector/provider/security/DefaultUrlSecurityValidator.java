package com.example.knowledgecollector.provider.security;

import com.example.knowledgecollector.capability.security.UrlSecurityValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultUrlSecurityValidator implements UrlSecurityValidator {
    private final boolean allowLoopback;

    public DefaultUrlSecurityValidator(
            @Value("${knowledge-collector.network.allow-loopback:false}") boolean allowLoopback) {
        this.allowLoopback = allowLoopback;
    }

    @Override
    public UrlValidationResult validate(String url) {
        try {
            URI uri = URI.create(url).normalize();
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                return invalid("URL-SCHEME-BLOCKED", "只允许 HTTP/HTTPS");
            }
            if (uri.getHost() == null || uri.getUserInfo() != null) {
                return invalid("URL-HOST-INVALID", "URL 主机无效或包含用户信息");
            }
            List<String> addresses = new ArrayList<>();
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                addresses.add(address.getHostAddress());
                if (!allowLoopback && blocked(address)) {
                    return invalid("URL-PRIVATE-ADDRESS", "禁止访问本机、链路本地或私有网络地址");
                }
            }
            return new UrlValidationResult(true, uri, List.copyOf(addresses), null, null);
        } catch (Exception exception) {
            return invalid("URL-VALIDATION-FAILED", "URL 安全校验失败：" + exception.getMessage());
        }
    }

    private boolean blocked(InetAddress address) {
        return address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isLinkLocalAddress() || address.isSiteLocalAddress();
    }

    private UrlValidationResult invalid(String code, String message) {
        return new UrlValidationResult(false, null, List.of(), code, message);
    }
}
