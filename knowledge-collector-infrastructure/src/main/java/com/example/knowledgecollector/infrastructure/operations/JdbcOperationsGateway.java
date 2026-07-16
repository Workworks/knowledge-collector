package com.example.knowledgecollector.infrastructure.operations;

import com.example.knowledgecollector.application.article.ArticleView;
import com.example.knowledgecollector.application.crawl.CrawlTaskView;
import com.example.knowledgecollector.application.operations.BackupView;
import com.example.knowledgecollector.application.operations.DashboardView;
import com.example.knowledgecollector.application.operations.OperationsGateway;
import com.example.knowledgecollector.application.operations.ScheduleView;
import com.example.knowledgecollector.infrastructure.storage.StorageProperties;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Repository
public class JdbcOperationsGateway implements OperationsGateway {
    private static final String TASK_SELECT =
            "select t.*,s.source_name from crawl_task t join crawl_source s on s.id=t.source_id";
    private static final String ARTICLE_SELECT =
            "select a.*,s.source_name from article a join crawl_source s on s.id=a.source_id";
    private final JdbcClient jdbc;
    private final StorageProperties storage;

    public JdbcOperationsGateway(JdbcClient jdbc, StorageProperties storage) {
        this.jdbc = jdbc;
        this.storage = storage;
    }

    @Override
    public DashboardView dashboard() {
        return new DashboardView(count("article"), scalar("select count(*) from article where reading_status='UNREAD'"),
                scalar("select count(*) from article where favorite=true"),
                scalar("select count(*) from article where review_status='PENDING_REVIEW'"),
                count("crawl_source"), scalar("select count(*) from crawl_source where enabled=true"),
                count("crawl_task"), scalar("select count(*) from crawl_task where status='FAILED'"),
                jdbc.sql(TASK_SELECT + " order by t.id desc limit 6").query(this::task).list(),
                jdbc.sql(ARTICLE_SELECT + " order by a.id desc limit 6").query(this::article).list(),
                jdbc.sql("""
                        select t.id,t.topic_name,count(r.article_id) article_count
                        from topic t left join article_topic_rel r on r.topic_id=t.id
                        group by t.id,t.topic_name order by article_count desc,t.sort_order,t.id limit 8
                        """).query((rs, n) -> new DashboardView.TopicCount(
                        rs.getLong("id"), rs.getString("topic_name"), rs.getLong("article_count"))).list(),
                jdbc.sql("""
                        select s.id,s.source_name,s.consecutive_failures,
                        sum(case when t.status='SUCCESS' then 1 else 0 end) success_count,
                        sum(case when t.status='FAILED' then 1 else 0 end) failed_count
                        from crawl_source s left join crawl_task t on t.source_id=s.id
                        group by s.id,s.source_name,s.consecutive_failures
                        order by s.consecutive_failures desc,s.id limit 8
                        """).query((rs, n) -> {
                    long success = rs.getLong("success_count");
                    long failed = rs.getLong("failed_count");
                    int consecutive = rs.getInt("consecutive_failures");
                    String health = consecutive >= 3 ? "UNHEALTHY" : consecutive > 0 ? "DEGRADED" : "HEALTHY";
                    return new DashboardView.SourceHealth(rs.getLong("id"), rs.getString("source_name"),
                            success, failed, consecutive, health);
                }).list());
    }

    @Override
    public List<ScheduleView> schedules() {
        return jdbc.sql("""
                select s.id source_id,s.source_name,coalesce(c.enabled,false) enabled,
                coalesce(c.interval_minutes,60) interval_minutes,c.next_run_at,c.last_run_at
                from crawl_source s left join collection_schedule c on c.source_id=s.id
                order by s.source_name,s.id
                """).query(this::schedule).list();
    }

    @Override
    public ScheduleView saveSchedule(long sourceId, boolean enabled, int intervalMinutes) {
        OffsetDateTime now = OffsetDateTime.now();
        int updated = jdbc.sql("""
                update collection_schedule set enabled=:enabled,interval_minutes=:interval,
                next_run_at=:nextRun,updated_at=:now where source_id=:sourceId
                """).param("enabled", enabled).param("interval", intervalMinutes)
                .param("nextRun", enabled ? now.plusMinutes(intervalMinutes) : null)
                .param("now", now).param("sourceId", sourceId).update();
        if (updated == 0) {
            jdbc.sql("""
                    insert into collection_schedule(source_id,enabled,interval_minutes,next_run_at,created_at,updated_at)
                    values(:sourceId,:enabled,:interval,:nextRun,:now,:now)
                    """).param("sourceId", sourceId).param("enabled", enabled)
                    .param("interval", intervalMinutes)
                    .param("nextRun", enabled ? now.plusMinutes(intervalMinutes) : null)
                    .param("now", now).update();
        }
        return schedules().stream().filter(item -> item.sourceId() == sourceId).findFirst().orElseThrow();
    }

    @Override
    public List<ScheduleView> dueSchedules(OffsetDateTime now) {
        return jdbc.sql("""
                select c.source_id,s.source_name,c.enabled,c.interval_minutes,c.next_run_at,c.last_run_at
                from collection_schedule c join crawl_source s on s.id=c.source_id
                where c.enabled=true and s.enabled=true and c.next_run_at<=:now
                order by c.next_run_at
                """).param("now", now).query(this::schedule).list();
    }

    @Override
    public void scheduleCompleted(long sourceId, OffsetDateTime completedAt, int intervalMinutes) {
        jdbc.sql("""
                update collection_schedule set last_run_at=:completed,
                next_run_at=:nextRun,updated_at=:completed where source_id=:sourceId
                """).param("completed", completedAt)
                .param("nextRun", completedAt.plusMinutes(intervalMinutes))
                .param("sourceId", sourceId).update();
    }

    @Override
    public BackupView createBackup() {
        try {
            Path root = storage.root().toAbsolutePath().normalize();
            Path backupDir = root.resolve("backups");
            Files.createDirectories(backupDir);
            String stamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));
            String name = "knowledge-collector-" + stamp + ".zip";
            Path target = backupDir.resolve(name);
            Path databaseBackup = Files.createTempFile("knowledge-collector-db-", ".zip");
            String escaped = databaseBackup.toString().replace("\\", "/").replace("'", "''");
            jdbc.sql("BACKUP TO '" + escaped + "'").update();
            try (OutputStream output = Files.newOutputStream(target);
                 ZipOutputStream zip = new ZipOutputStream(output)) {
                addFile(zip, databaseBackup, "database/h2-backup.zip");
                addDirectory(zip, root.resolve("article-content"), root, backupDir);
                addDirectory(zip, root.resolve("snapshots"), root, backupDir);
                addDirectory(zip, root.resolve("exports"), root, backupDir);
                zip.putNextEntry(new ZipEntry("manifest.txt"));
                zip.write(("createdAt=" + OffsetDateTime.now() + "\nformat=stage-9\n").getBytes());
                zip.closeEntry();
            } finally {
                Files.deleteIfExists(databaseBackup);
            }
            OffsetDateTime createdAt = OffsetDateTime.now();
            long size = Files.size(target);
            jdbc.sql("""
                    insert into backup_record(backup_name,relative_path,size_bytes,status,created_at)
                    values(:name,:path,:size,'SUCCESS',:createdAt)
                    """).param("name", name).param("path", root.relativize(target).toString())
                    .param("size", size).param("createdAt", createdAt).update();
            return backups().stream().filter(item -> item.name().equals(name)).findFirst().orElseThrow();
        } catch (IOException exception) {
            throw new IllegalStateException("创建备份失败", exception);
        }
    }

    @Override
    public List<BackupView> backups() {
        return jdbc.sql("select * from backup_record order by id desc").query((rs, n) ->
                new BackupView(rs.getLong("id"), rs.getString("backup_name"),
                        rs.getString("relative_path"), rs.getLong("size_bytes"),
                        rs.getString("status"), rs.getObject("created_at", OffsetDateTime.class))).list();
    }

    private long count(String table) {
        return scalar("select count(*) from " + table);
    }

    private long scalar(String sql) {
        return jdbc.sql(sql).query(Long.class).single();
    }

    private ScheduleView schedule(ResultSet rs, int row) throws SQLException {
        return new ScheduleView(rs.getLong("source_id"), rs.getString("source_name"),
                rs.getBoolean("enabled"), rs.getInt("interval_minutes"),
                rs.getObject("next_run_at", OffsetDateTime.class),
                rs.getObject("last_run_at", OffsetDateTime.class));
    }

    private CrawlTaskView task(ResultSet rs, int row) throws SQLException {
        return new CrawlTaskView(rs.getLong("id"), rs.getString("task_no"),
                (Long) rs.getObject("retry_of_task_id"), rs.getString("trigger_type"),
                rs.getLong("source_id"), rs.getString("source_name"), rs.getString("status"),
                rs.getBoolean("cancel_requested"), rs.getInt("discovered_count"),
                rs.getInt("created_count"), rs.getInt("duplicate_count"), rs.getInt("failed_count"),
                rs.getString("error_code"), rs.getString("error_message"),
                rs.getObject("started_at", OffsetDateTime.class),
                rs.getObject("finished_at", OffsetDateTime.class),
                (Long) rs.getObject("duration_millis"),
                rs.getObject("created_at", OffsetDateTime.class));
    }

    private ArticleView article(ResultSet rs, int row) throws SQLException {
        return new ArticleView(rs.getLong("id"), rs.getLong("source_id"), rs.getString("source_name"),
                rs.getString("title"), rs.getString("author"), rs.getString("summary"),
                rs.getString("original_url"), rs.getString("normalized_url"), rs.getString("language"),
                rs.getObject("publish_time", OffsetDateTime.class), rs.getBoolean("publish_time_inferred"),
                rs.getString("content_html"), rs.getString("content_text"), rs.getInt("word_count"),
                rs.getInt("reading_minutes"), rs.getInt("quality_score"), rs.getString("review_status"),
                rs.getString("source_level"), rs.getString("reading_status"), rs.getBoolean("favorite"),
                rs.getBoolean("archived"), rs.getObject("first_collected_at", OffsetDateTime.class),
                rs.getObject("last_collected_at", OffsetDateTime.class));
    }

    private void addDirectory(ZipOutputStream zip, Path directory, Path root, Path excluded) throws IOException {
        if (!Files.isDirectory(directory)) return;
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                if (!path.startsWith(excluded)) addFile(zip, path, root.relativize(path).toString().replace("\\", "/"));
            }
        }
    }

    private void addFile(ZipOutputStream zip, Path file, String entryName) throws IOException {
        zip.putNextEntry(new ZipEntry(entryName));
        try (InputStream input = Files.newInputStream(file)) {
            input.transferTo(zip);
        }
        zip.closeEntry();
    }
}
