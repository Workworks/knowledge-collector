package com.example.knowledgecollector.infrastructure.storage;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class KnowledgeCollectorHealthIndicator implements HealthIndicator {

    private final StorageProperties properties;

    public KnowledgeCollectorHealthIndicator(StorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public Health health() {
        Path root = properties.root().toAbsolutePath().normalize();
        Path articles = root.resolve("article-content");
        boolean dataWritable = Files.isDirectory(root) && Files.isWritable(root);
        boolean articlesWritable = Files.isDirectory(articles) && Files.isWritable(articles);
        Health.Builder builder = dataWritable && articlesWritable ? Health.up() : Health.down();
        return builder
                .withDetail("dataDirectoryWritable", dataWritable)
                .withDetail("articleDirectoryWritable", articlesWritable)
                .withDetail("scheduler", "NOT_IMPLEMENTED_STAGE_9")
                .withDetail("lastCrawlTask", "NOT_AVAILABLE_STAGE_5")
                .build();
    }
}
