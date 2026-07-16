package com.example.knowledgecollector;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FoundationIntegrationTest {

    private static final Path TEST_DATA = createTestDataDirectory();

    @DynamicPropertySource
    static void configureTestStorage(DynamicPropertyRegistry registry) {
        registry.add("knowledge-collector.storage.root", () -> TEST_DATA.toString());
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:foundation-test;DB_CLOSE_DELAY=-1");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Test
    void startsWebApplicationAndReportsDatabaseStatus() {
        JsonNode response = restTemplate.getForObject(
                "http://127.0.0.1:" + port + "/api/v1/system/status",
                JsonNode.class
        );

        assertThat(response.path("success").asBoolean()).isTrue();
        assertThat(response.path("data").path("applicationName").asText())
                .isEqualTo("knowledge-collector");
        assertThat(response.path("data").path("databaseProduct").asText()).containsIgnoringCase("H2");
        assertThat(response.path("data").path("flywayMigrationCount").asInt()).isEqualTo(4);
        assertThat(response.path("data").path("startupCount").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(response.path("correlationId").asText()).isNotBlank();
    }

    @Test
    void initializesAllLocalStorageDirectories() {
        List<String> directories = List.of(
                "database", "article-content", "snapshots", "exports", "logs"
        );

        assertThat(directories)
                .allSatisfy(directory -> assertThat(TEST_DATA.resolve(directory)).isDirectory());
    }

    @Test
    void rendersFoundationHomePage() {
        String html = restTemplate.getForObject(
                "http://127.0.0.1:" + port + "/",
                String.class
        );

        assertThat(html)
                .contains("资料收集与阅读管理系统")
                .contains("/api/v1/system/status")
                .contains("/test-console");
    }

    @Test
    void rendersWebTestConsoleAndServesItsScript() {
        String html = restTemplate.getForObject(
                "http://127.0.0.1:" + port + "/test-console",
                String.class
        );
        String script = restTemplate.getForObject(
                "http://127.0.0.1:" + port + "/js/test-console.js",
                String.class
        );

        assertThat(html)
                .contains("Web 接口测试台")
                .contains("http/knowledge-collector.http")
                .contains("/js/test-console.js");
        assertThat(script)
                .contains("isSafeApplicationPath")
                .contains("X-Correlation-Id");
    }

    @Test
    void exposesOpenApiDocumentation() {
        JsonNode document = restTemplate.getForObject(
                "http://127.0.0.1:" + port + "/v3/api-docs", JsonNode.class);
        assertThat(document.path("openapi").asText()).startsWith("3.");
        assertThat(document.path("paths").has("/api/v1/topics")).isTrue();
        assertThat(document.path("paths").has("/api/v1/sources")).isTrue();
    }

    private static Path createTestDataDirectory() {
        try {
            return Files.createTempDirectory("knowledge-collector-stage3-");
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
