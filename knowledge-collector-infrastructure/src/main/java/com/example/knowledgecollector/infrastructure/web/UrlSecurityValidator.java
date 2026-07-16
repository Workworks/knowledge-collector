package com.example.knowledgecollector.infrastructure.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;

@Component
public class UrlSecurityValidator {
    private final boolean allowLoopback;
    public UrlSecurityValidator(@Value("${knowledge-collector.network.allow-loopback:false}") boolean allowLoopback) {
        this.allowLoopback = allowLoopback;
    }

    public URI validate(String value) {
        try {
            URI uri = URI.create(value);
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))
                throw new IllegalArgumentException("只允许 HTTP/HTTPS");
            if (uri.getUserInfo() != null || uri.getHost() == null) throw new IllegalArgumentException("URL 主机无效");
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (!allowLoopback && (address.isAnyLocalAddress() || address.isLoopbackAddress()
                        || address.isLinkLocalAddress() || address.isSiteLocalAddress()))
                    throw new IllegalArgumentException("禁止访问本机或私有网络地址");
            }
            return uri;
        } catch (Exception exception) {
            throw new IllegalArgumentException("URL 安全校验失败：" + exception.getMessage(), exception);
        }
    }
}
