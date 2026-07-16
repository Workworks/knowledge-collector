package com.example.knowledgecollector.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LocalStorageInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageInitializer.class);
    private static final List<String> REQUIRED_DIRECTORIES = List.of(
            "database", "article-content", "snapshots", "exports", "logs"
    );

    private final StorageProperties properties;

    public LocalStorageInitializer(StorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        Path root = properties.root().toAbsolutePath().normalize();
        Files.createDirectories(root);

        for (String directory : REQUIRED_DIRECTORIES) {
            Path target = root.resolve(directory).normalize();
            if (!target.startsWith(root)) {
                throw new IOException("Storage directory escapes configured root: " + directory);
            }
            Files.createDirectories(target);
        }

        log.info("Local storage initialized at {}", root);
    }
}
