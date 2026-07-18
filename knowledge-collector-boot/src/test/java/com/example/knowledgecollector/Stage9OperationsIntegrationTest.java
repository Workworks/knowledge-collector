package com.example.knowledgecollector;

import com.example.knowledgecollector.application.crawl.CrawlTaskGateway;
import com.example.knowledgecollector.application.crawl.CrawlTaskView;
import com.example.knowledgecollector.application.source.CrawlSourceService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "local"})
class Stage9OperationsIntegrationTest {
    private static final Path TEST_DATA = createTestDataDirectory();

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("knowledge-collector.storage.root", () -> TEST_DATA.toString());
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:file:" + TEST_DATA.resolve("database/stage9-test")
                        .toString().replace("\\", "/") + ";AUTO_SERVER=FALSE");
    }

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private CrawlTaskGateway tasks;
    @Autowired
    private CrawlSourceService sources;
    @Autowired
    private JdbcClient jdbc;
    @LocalServerPort
    private int port;

    @Test
    void managesSchedulesTasksDashboardAndBackups() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        JsonNode source = exchange(HttpMethod.POST, "/api/v1/sources", Map.ofEntries(
                Map.entry("code", "OPS_" + suffix), Map.entry("name", "Stage 9 Operations " + suffix),
                Map.entry("type", "JSON_API"), Map.entry("homeUrl", "https://fixture.local"),
                Map.entry("feedUrl", "https://fixture.local/" + suffix), Map.entry("language", "zh-CN"),
                Map.entry("charset", "UTF-8"), Map.entry("userAgent", "Stage9Test/1.0"),
                Map.entry("timeoutSeconds", 5), Map.entry("maxRetries", 0),
                Map.entry("requestIntervalMillis", 0), Map.entry("obeyRobots", true),
                Map.entry("fetchFullContent", true), Map.entry("summaryOnly", false),
                Map.entry("saveSnapshot", false), Map.entry("enabled", true),
                Map.entry("notes", "operations integration"), Map.entry("topicIds", new long[0])
        )).getBody();
        long sourceId = source.path("data").path("id").asLong();

        JsonNode schedule = exchange(HttpMethod.PUT, "/api/v1/operations/schedules/" + sourceId,
                Map.of("enabled", true, "intervalMinutes", 30)).getBody();
        assertThat(schedule.path("data").path("enabled").asBoolean()).isTrue();
        assertThat(schedule.path("data").path("intervalMinutes").asInt()).isEqualTo(30);

        jdbc.sql("update collection_schedule set next_run_at=:now where source_id=:sourceId")
                .param("now", OffsetDateTime.now().minusMinutes(1))
                .param("sourceId", sourceId).update();
        exchange(HttpMethod.POST, "/api/v1/operations/schedules/run-due", null);
        JsonNode scheduledTasks = exchange(HttpMethod.GET, "/api/v1/tasks?page=0&size=20", null).getBody();
        assertThat(scheduledTasks.path("data").path("content").get(0).path("triggerType").asText())
                .isEqualTo("SCHEDULED");

        CrawlTaskView stale = tasks.create(sources.get(sourceId));
        tasks.running(stale.id());
        jdbc.sql("update crawl_task set heartbeat_at=:expired where id=:id")
                .param("expired", OffsetDateTime.now().minusMinutes(20))
                .param("id", stale.id()).update();
        JsonNode recovered = exchange(HttpMethod.POST,
                "/api/v1/operations/tasks/recover-stale", null).getBody();
        assertThat(recovered.path("data").asInt()).isEqualTo(1);
        JsonNode expiredTask = exchange(HttpMethod.GET, "/api/v1/tasks/" + stale.id(), null).getBody();
        assertThat(expiredTask.path("data").path("status").asText()).isEqualTo("FAILED");
        assertThat(expiredTask.path("data").path("errorCode").asText()).isEqualTo("TASK-TIMEOUT");
        JsonNode afterRecovery = exchange(HttpMethod.POST,
                "/api/v1/sources/" + sourceId + "/crawl", null).getBody();
        assertThat(afterRecovery.path("data").path("status").asText()).isEqualTo("SUCCESS");

        CrawlTaskView pending = tasks.create(sources.get(sourceId));
        JsonNode canceled = exchange(HttpMethod.POST, "/api/v1/tasks/" + pending.id() + "/cancel", null).getBody();
        assertThat(canceled.path("data").path("status").asText()).isEqualTo("CANCELED");
        assertThat(canceled.path("data").path("cancelRequested").asBoolean()).isTrue();

        CrawlTaskView failed = tasks.create(sources.get(sourceId));
        tasks.running(failed.id());
        tasks.failure(failed.id(), "TEST-FAILURE", "retry integration fixture", 1);
        JsonNode retried = exchange(HttpMethod.POST, "/api/v1/tasks/" + failed.id() + "/retry", null).getBody();
        assertThat(retried.path("data").path("triggerType").asText()).isEqualTo("RETRY");
        assertThat(retried.path("data").path("retryOfTaskId").asLong()).isEqualTo(failed.id());
        assertThat(retried.path("data").path("status").asText()).isEqualTo("SUCCESS");

        JsonNode dashboard = exchange(HttpMethod.GET, "/api/v1/dashboard", null).getBody();
        assertThat(dashboard.path("data").path("sourceCount").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(dashboard.path("data").path("taskCount").asInt()).isGreaterThanOrEqualTo(2);
        assertThat(dashboard.path("data").path("recentTasks")).isNotEmpty();

        JsonNode backup = exchange(HttpMethod.POST, "/api/v1/operations/backups", null).getBody();
        String relativePath = backup.path("data").path("relativePath").asText();
        assertThat(backup.path("data").path("status").asText()).isEqualTo("SUCCESS");
        assertThat(backup.path("data").path("sizeBytes").asLong()).isGreaterThan(0);
        Path backupFile = TEST_DATA.resolve(relativePath);
        assertThat(backupFile).isRegularFile();
        verifyDatabaseCanBeRestored(backupFile);

        assertThat(rest.getForEntity(url("/operations"), String.class).getBody())
                .contains("SCHEDULING & OPERATIONS", "/js/operations.js");
        assertThat(rest.getForEntity(url("/"), String.class).getBody())
                .contains("基础设施状态", "知识研究与写作链路", "运行正常");
    }

    private ResponseEntity<JsonNode> exchange(HttpMethod method, String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Correlation-Id", "stage9-operations-test");
        ResponseEntity<JsonNode> response = rest.exchange(url(path), method,
                new HttpEntity<>(body, headers), JsonNode.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response;
    }

    private String url(String path) {
        return "http://127.0.0.1:" + port + path;
    }

    private static Path createTestDataDirectory() {
        try {
            Path directory = Files.createTempDirectory("knowledge-collector-stage9-");
            Files.createDirectories(directory.resolve("database"));
            return directory;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private void verifyDatabaseCanBeRestored(Path backupFile) {
        try {
            Path restoreRoot = Files.createTempDirectory("knowledge-collector-restore-");
            Path h2Backup = restoreRoot.resolve("h2-backup.zip");
            try (ZipFile zip = new ZipFile(backupFile.toFile())) {
                Files.copy(zip.getInputStream(zip.getEntry("database/h2-backup.zip")), h2Backup);
            }
            Path databaseDirectory = restoreRoot.resolve("database");
            Files.createDirectories(databaseDirectory);
            Class<?> restore = Class.forName("org.h2.tools.Restore");
            restore.getMethod("execute", String.class, String.class, String.class)
                    .invoke(null, h2Backup.toString(), databaseDirectory.toString(), null);
            Path databaseFile;
            try (var files = Files.list(databaseDirectory)) {
                databaseFile = files.filter(path -> path.getFileName().toString().endsWith(".mv.db"))
                        .findFirst().orElseThrow();
            }
            String databasePath = databaseFile.toString()
                    .substring(0, databaseFile.toString().length() - ".mv.db".length())
                    .replace("\\", "/");
            try (var connection = DriverManager.getConnection("jdbc:h2:file:" + databasePath, "sa", "");
                 var statement = connection.createStatement();
                 var result = statement.executeQuery("select count(*) from article")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getLong(1)).isGreaterThanOrEqualTo(1);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("备份恢复验证失败", exception);
        }
    }
}
