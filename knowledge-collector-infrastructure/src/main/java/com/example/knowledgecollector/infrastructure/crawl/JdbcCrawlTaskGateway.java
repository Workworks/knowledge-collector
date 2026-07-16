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
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcCrawlTaskGateway implements CrawlTaskGateway {
    private static final String BASE =
            "select t.*,s.source_name from crawl_task t join crawl_source s on s.id=t.source_id";

    private final JdbcClient jdbc;
    private final Duration staleTimeout;

    public JdbcCrawlTaskGateway(JdbcClient jdbc,
            @Value("${knowledge-collector.tasks.stale-timeout:PT10M}") Duration staleTimeout) {
        this.jdbc = jdbc;
        this.staleTimeout = staleTimeout;
    }

    @Override
    @Transactional
    public CrawlTaskView create(CrawlSource source) {
        return create(source, "MANUAL_SOURCE", null);
    }

    @Override
    @Transactional
    public CrawlTaskView create(CrawlSource source, String triggerType, Long retryOfTaskId) {
        expireStale(OffsetDateTime.now().minus(staleTimeout));
        String taskNo = "TASK-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        try {
            jdbc.sql("""
                    insert into crawl_task(task_no,retry_of_task_id,trigger_type,source_id,active_source_id,status,created_at)
                    values(:taskNo,:retryOfTaskId,:triggerType,:sourceId,:sourceId,'CREATED',:createdAt)
                    """)
                    .param("taskNo", taskNo)
                    .param("retryOfTaskId", retryOfTaskId)
                    .param("triggerType", triggerType)
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
        jdbc.sql("""
                update crawl_task set status='RUNNING',started_at=:now,heartbeat_at=:now
                where id=:id and status='CREATED'
                """)
                .param("now", OffsetDateTime.now())
                .param("id", id)
                .update();
    }

    @Override
    public void heartbeat(long id) {
        int updated = jdbc.sql("""
                update crawl_task set heartbeat_at=:now
                where id=:id and status='RUNNING' and active_source_id is not null
                """).param("now", OffsetDateTime.now()).param("id", id).update();
        if (updated != 1) {
            throw new IllegalStateException("TASK-NOT-RUNNING: 任务已结束或租约已被回收");
        }
    }

    @Override
    @Transactional
    public int expireStale(OffsetDateTime cutoff) {
        List<StaleTask> staleTasks = jdbc.sql("""
                select id,source_id from crawl_task
                where active_source_id is not null and status in ('CREATED','RUNNING')
                and coalesce(heartbeat_at,started_at,created_at)<:cutoff
                """).param("cutoff", cutoff).query((rs, row) ->
                new StaleTask(rs.getLong("id"), rs.getLong("source_id"))).list();
        if (staleTasks.isEmpty()) {
            return 0;
        }
        OffsetDateTime now = OffsetDateTime.now();
        int expired = 0;
        for (StaleTask task : staleTasks) {
            int updated = jdbc.sql("""
                    update crawl_task set status='FAILED',active_source_id=null,
                    error_code='TASK-TIMEOUT',
                    error_message='任务超过最大无心跳时间，系统已自动结束并释放采集源',
                    finished_at=:now,
                    duration_millis=datediff('MILLISECOND',coalesce(started_at,created_at),:now)
                    where id=:id and active_source_id is not null
                    and status in ('CREATED','RUNNING')
                    and coalesce(heartbeat_at,started_at,created_at)<:cutoff
                    """).param("now", now).param("id", task.id()).param("cutoff", cutoff).update();
            if (updated == 1) {
                expired++;
                jdbc.sql("""
                        update crawl_source set last_failure_at=:now,
                        consecutive_failures=consecutive_failures+1,updated_at=:now where id=:sourceId
                        """).param("now", now).param("sourceId", task.sourceId()).update();
            }
        }
        return expired;
    }

    @Override
    @Transactional
    public SaveResult saveEntry(long taskId, CrawlSource source, ContentSourceProvider.ContentItem entry) {
        heartbeat(taskId);
        var normalized = UrlNormalizer.normalize(entry.url(), source.feedUrl());
        var existing = jdbc.sql("""
                        select id,case when content_text is not null
                        and char_length(trim(content_text))>0 then true else false end as has_content
                        from article where url_hash=:hash
                        """)
                .param("hash", normalized.hash())
                .query((rs, row) -> new ExistingArticle(
                        rs.getLong("id"), rs.getBoolean("has_content")))
                .optional();
        Long articleId;
        boolean created = existing.isEmpty();
        boolean contentUpdated = false;
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
            ExistingArticle current = existing.get();
            articleId = current.id();
            contentUpdated = !current.hasContent()
                    && entry.contentText() != null && !entry.contentText().isBlank();
            jdbc.sql("""
                    update article set
                    title=case when title is null or trim(title)='' then :title else title end,
                    author=case when author is null or trim(author)='' then :author else author end,
                    summary=case when summary is null or trim(cast(summary as varchar))='' then :summary else summary end,
                    content_html=case
                        when content_html is null or trim(cast(content_html as varchar))='' then :contentHtml
                        else content_html end,
                    content_text=case
                        when content_text is null or trim(cast(content_text as varchar))='' then :contentText
                        else content_text end,
                    word_count=case
                        when content_text is null or trim(cast(content_text as varchar))='' then :wordCount
                        else word_count end,
                    reading_minutes=case
                        when content_text is null or trim(cast(content_text as varchar))='' then :readingMinutes
                        else reading_minutes end,
                    last_collected_at=:now,updated_at=:now where id=:id
                    """)
                    .param("title", entry.title())
                    .param("author", entry.author())
                    .param("summary", entry.summary())
                    .param("contentHtml", entry.contentHtml())
                    .param("contentText", entry.contentText())
                    .param("wordCount", wordCount(entry.contentText()))
                    .param("readingMinutes", readingMinutes(entry.contentText()))
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
        return new SaveResult(created, contentUpdated, articleId);
    }

    @Override
    public void success(long id, int discovered, int created, int duplicate, long durationMillis) {
        complete(id, "SUCCESS", discovered, created, duplicate, null, null, durationMillis);
    }

    @Override
    public void failure(long id, String code, String message, long durationMillis) {
        complete(id, "FAILED", 0, 0, 0, code, message, durationMillis);
    }

    @Override
    public boolean requestCancel(long id) {
        return jdbc.sql("""
                update crawl_task set cancel_requested=true,status='CANCELED',
                active_source_id=null,finished_at=:now
                where id=:id and status='CREATED'
                """).param("now", OffsetDateTime.now()).param("id", id).update() == 1;
    }

    private void complete(long id, String status, int discovered, int created, int duplicate,
                          String errorCode, String errorMessage, long durationMillis) {
        OffsetDateTime finishedAt = OffsetDateTime.now();
        int updated = jdbc.sql("""
                update crawl_task set status=:status,active_source_id=null,discovered_count=:discovered,
                created_count=:created,duplicate_count=:duplicate,error_code=:errorCode,
                error_message=:errorMessage,finished_at=:finishedAt,duration_millis=:durationMillis
                where id=:id and status='RUNNING' and active_source_id is not null
                """)
                .param("status", status)
                .param("discovered", discovered)
                .param("created", created)
                .param("duplicate", duplicate)
                .param("errorCode", errorCode)
                .param("errorMessage", errorMessage)
                .param("finishedAt", finishedAt)
                .param("durationMillis", durationMillis)
                .param("id", id)
                .update();
        if (updated != 1) {
            return;
        }
        if ("SUCCESS".equals(status)) {
            jdbc.sql("""
                    update crawl_source set last_success_at=:now,consecutive_failures=0,updated_at=:now
                    where id=(select source_id from crawl_task where id=:id)
                    """).param("now", finishedAt).param("id", id).update();
        } else if ("FAILED".equals(status)) {
            jdbc.sql("""
                    update crawl_source set last_failure_at=:now,
                    consecutive_failures=consecutive_failures+1,updated_at=:now
                    where id=(select source_id from crawl_task where id=:id)
                    """).param("now", finishedAt).param("id", id).update();
        }
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
                (Long) resultSet.getObject("retry_of_task_id"),
                resultSet.getString("trigger_type"),
                resultSet.getLong("source_id"),
                resultSet.getString("source_name"),
                resultSet.getString("status"),
                resultSet.getBoolean("cancel_requested"),
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

    private record StaleTask(long id, long sourceId) {
    }

    private record ExistingArticle(long id, boolean hasContent) {
    }
}
