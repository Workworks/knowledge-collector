package com.example.knowledgecollector.infrastructure.crawl;

import com.example.knowledgecollector.application.common.PageResult;
import com.example.knowledgecollector.application.crawl.CrawlTaskGateway;
import com.example.knowledgecollector.application.crawl.CrawlTaskView;
import com.example.knowledgecollector.capability.source.ContentSourceProvider;
import com.example.knowledgecollector.application.exception.ResourceNotFoundException;
import com.example.knowledgecollector.application.exception.ConflictException;
import com.example.knowledgecollector.domain.crawler.UrlNormalizer;
import com.example.knowledgecollector.domain.source.CrawlSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcCrawlTaskGateway implements CrawlTaskGateway {
    private static final String BASE =
            "select t.*,s.source_name from crawl_task t join crawl_source s on s.id=t.source_id";

    private final JdbcClient jdbc;

    public JdbcCrawlTaskGateway(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public CrawlTaskView create(CrawlSource source) {
        String taskNo = "TASK-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        try {
            jdbc.sql("""
                    insert into crawl_task(task_no,trigger_type,source_id,active_source_id,status,created_at)
                    values(:taskNo,'MANUAL_SOURCE',:sourceId,:sourceId,'CREATED',:createdAt)
                    """)
                    .param("taskNo", taskNo)
                    .param("sourceId", source.id())
                    .param("createdAt", OffsetDateTime.now())
                    .update();
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("CRAWL-TASK-ACTIVE", "同一采集源已有运行中任务");
        }
        Long id = jdbc.sql("select id from crawl_task where task_no=:taskNo")
                .param("taskNo", taskNo)
                .query(Long.class)
                .single();
        return get(id);
    }

    @Override
    public void running(long id) {
        jdbc.sql("update crawl_task set status='RUNNING',started_at=:now where id=:id")
                .param("now", OffsetDateTime.now())
                .param("id", id)
                .update();
    }

    @Override
    @Transactional
    public SaveResult saveEntry(long taskId, CrawlSource source, ContentSourceProvider.ContentItem entry) {
        var normalized = UrlNormalizer.normalize(entry.url(), source.feedUrl());
        var existing = jdbc.sql("select id from article where url_hash=:hash")
                .param("hash", normalized.hash())
                .query(Long.class)
                .optional();
        Long articleId;
        boolean created = existing.isEmpty();
        OffsetDateTime now = OffsetDateTime.now();
        if (created) {
            jdbc.sql("""
                    insert into article(source_id,title,author,summary,original_url,normalized_url,url_hash,language,
                    publish_time,publish_time_inferred,content_html,content_text,word_count,reading_minutes,
                    first_collected_at,last_collected_at,created_at,updated_at)
                    values(:sourceId,:title,:author,:summary,:originalUrl,:normalizedUrl,:urlHash,:language,
                    :publishTime,:publishTimeInferred,:contentHtml,:contentText,:wordCount,:readingMinutes,
                    :now,:now,:now,:now)
                    """)
                    .param("sourceId", source.id())
                    .param("title", entry.title())
                    .param("author", entry.author())
                    .param("summary", entry.summary())
                    .param("originalUrl", entry.url())
                    .param("normalizedUrl", normalized.value())
                    .param("urlHash", normalized.hash())
                    .param("language", source.language())
                    .param("publishTime", entry.publishedAt() == null ? now : entry.publishedAt())
                    .param("publishTimeInferred", entry.publishedAt() == null)
                    .param("contentHtml", entry.contentHtml())
                    .param("contentText", entry.contentText())
                    .param("wordCount", wordCount(entry.contentText()))
                    .param("readingMinutes", readingMinutes(entry.contentText()))
                    .param("now", now)
                    .update();
            articleId = jdbc.sql("select id from article where url_hash=:hash")
                    .param("hash", normalized.hash())
                    .query(Long.class)
                    .single();
        } else {
            articleId = existing.get();
            jdbc.sql("update article set last_collected_at=:now,updated_at=:now where id=:id")
                    .param("now", now)
                    .param("id", articleId)
                    .update();
        }
        jdbc.sql("""
                insert into crawl_task_item(task_id,original_url,normalized_url,url_hash,status,article_id,started_at,finished_at,created_at)
                values(:taskId,:originalUrl,:normalizedUrl,:urlHash,:status,:articleId,:now,:now,:now)
                """)
                .param("taskId", taskId)
                .param("originalUrl", entry.url())
                .param("normalizedUrl", normalized.value())
                .param("urlHash", normalized.hash())
                .param("status", created ? "CREATED" : "DUPLICATE")
                .param("articleId", articleId)
                .param("now", now)
                .update();
        return new SaveResult(created, articleId);
    }

    @Override
    public void success(long id, int discovered, int created, int duplicate, long durationMillis) {
        complete(id, "SUCCESS", discovered, created, duplicate, null, null, durationMillis);
    }

    @Override
    public void failure(long id, String code, String message, long durationMillis) {
        complete(id, "FAILED", 0, 0, 0, code, message, durationMillis);
    }

    private void complete(long id, String status, int discovered, int created, int duplicate,
                          String errorCode, String errorMessage, long durationMillis) {
        jdbc.sql("""
                update crawl_task set status=:status,active_source_id=null,discovered_count=:discovered,
                created_count=:created,duplicate_count=:duplicate,error_code=:errorCode,
                error_message=:errorMessage,finished_at=:finishedAt,duration_millis=:durationMillis
                where id=:id
                """)
                .param("status", status)
                .param("discovered", discovered)
                .param("created", created)
                .param("duplicate", duplicate)
                .param("errorCode", errorCode)
                .param("errorMessage", errorMessage)
                .param("finishedAt", OffsetDateTime.now())
                .param("durationMillis", durationMillis)
                .param("id", id)
                .update();
    }

    @Override
    public CrawlTaskView get(long id) {
        return jdbc.sql(BASE + " where t.id=:id")
                .param("id", id)
                .query(this::map)
                .optional()
                .orElseThrow(() -> new ResourceNotFoundException("采集任务不存在：" + id));
    }

    @Override
    public PageResult<CrawlTaskView> findPage(int page, int size) {
        long total = jdbc.sql("select count(*) from crawl_task").query(Long.class).single();
        var items = jdbc.sql(BASE + " order by t.id desc limit :size offset :offset")
                .param("size", size)
                .param("offset", page * size)
                .query(this::map)
                .list();
        return new PageResult<>(items, page, size, total, (int) Math.ceil((double) total / size), "id,desc");
    }

    @Override
    public List<?> findItems(long id) {
        get(id);
        return jdbc.sql("select * from crawl_task_item where task_id=:id order by id")
                .param("id", id)
                .query()
                .listOfRows();
    }

    private CrawlTaskView map(java.sql.ResultSet resultSet, int rowNumber) throws java.sql.SQLException {
        return new CrawlTaskView(
                resultSet.getLong("id"),
                resultSet.getString("task_no"),
                resultSet.getLong("source_id"),
                resultSet.getString("source_name"),
                resultSet.getString("status"),
                resultSet.getInt("discovered_count"),
                resultSet.getInt("created_count"),
                resultSet.getInt("duplicate_count"),
                resultSet.getInt("failed_count"),
                resultSet.getString("error_code"),
                resultSet.getString("error_message"),
                resultSet.getObject("started_at", OffsetDateTime.class),
                resultSet.getObject("finished_at", OffsetDateTime.class),
                (Long) resultSet.getObject("duration_millis"),
                resultSet.getObject("created_at", OffsetDateTime.class)
        );
    }

    private int wordCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    private int readingMinutes(String text) {
        int words = wordCount(text);
        return words == 0 ? 0 : Math.max(1, (int) Math.ceil(words / 250.0));
    }
}
