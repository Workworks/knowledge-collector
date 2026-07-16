package com.example.knowledgecollector.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "knowledge-collector.storage")
public record StorageProperties(Path root) {

    public StorageProperties {
        if (root == null) {
            root = Path.of("./data");
        }
    }
}
