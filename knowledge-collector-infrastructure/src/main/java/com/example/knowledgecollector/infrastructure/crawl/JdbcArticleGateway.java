package com.example.knowledgecollector.infrastructure.crawl;

import com.example.knowledgecollector.application.article.ArticleGateway;
import com.example.knowledgecollector.application.article.ArticleSearchCriteria;
import com.example.knowledgecollector.application.article.ArticleView;
import com.example.knowledgecollector.application.common.PageResult;
import com.example.knowledgecollector.application.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Repository
public class JdbcArticleGateway implements ArticleGateway {
    private static final String SELECT = "select a.*,s.source_name from article a join crawl_source s on s.id=a.source_id";
    private final JdbcClient jdbc;

    public JdbcArticleGateway(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public PageResult<ArticleView> findPage(ArticleSearchCriteria criteria) {
        StringBuilder where = new StringBuilder(" where 1=1");
        Map<String, Object> params = new HashMap<>();
        if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
            where.append("""
                     and (lower(a.title) like :keyword or lower(coalesce(a.author,'')) like :keyword
                     or lower(coalesce(cast(a.summary as varchar),'')) like :keyword
                     or lower(coalesce(cast(a.content_text as varchar),'')) like :keyword
                     or lower(s.source_name) like :keyword
                     or exists(select 1 from article_tag_rel atr join tag t on t.id=atr.tag_id
                               where atr.article_id=a.id and lower(t.tag_name) like :keyword))
                    """);
            params.put("keyword", "%" + criteria.keyword().trim().toLowerCase() + "%");
        }
        if (criteria.sourceId() != null) {
            where.append(" and a.source_id=:sourceId");
            params.put("sourceId", criteria.sourceId());
        }
        if (criteria.reviewStatus() != null && !criteria.reviewStatus().isBlank()) {
            where.append(" and a.review_status=:reviewStatus");
            params.put("reviewStatus", criteria.reviewStatus());
        }
        if (criteria.minQuality() != null) {
            where.append(" and a.quality_score>=:minQuality");
            params.put("minQuality", criteria.minQuality());
        }
        if (criteria.topicId() != null) {
            where.append(" and exists(select 1 from article_topic_rel atr where atr.article_id=a.id and atr.topic_id=:topicId)");
            params.put("topicId", criteria.topicId());
        }
        if (criteria.readingStatus() != null && !criteria.readingStatus().isBlank()) {
            where.append(" and a.reading_status=:readingStatus");
            params.put("readingStatus", criteria.readingStatus());
        }
        if (criteria.favorite() != null) {
            where.append(" and a.favorite=:favorite");
            params.put("favorite", criteria.favorite());
        }
        if (criteria.archived() != null) {
            where.append(" and a.archived=:archived");
            params.put("archived", criteria.archived());
        }
        if (criteria.tagId() != null) {
            where.append(" and exists(select 1 from article_tag_rel atr where atr.article_id=a.id and atr.tag_id=:tagId)");
            params.put("tagId", criteria.tagId());
        }
        long total = jdbc.sql("select count(*) from article a join crawl_source s on s.id=a.source_id" + where)
                .params(params).query(Long.class).single();
        params.put("size", criteria.size());
        params.put("offset", criteria.page() * criteria.size());
        String orderBy = switch (criteria.sort() == null ? "" : criteria.sort()) {
            case "quality,desc" -> "a.quality_score desc,a.publish_time desc,a.id desc";
            case "quality,asc" -> "a.quality_score,a.publish_time desc,a.id desc";
            case "collected,desc" -> "a.first_collected_at desc,a.id desc";
            default -> "a.publish_time desc,a.id desc";
        };
        var content = jdbc.sql(SELECT + where + " order by " + orderBy + " limit :size offset :offset")
                .params(params).query(this::map).list();
        return new PageResult<>(content, criteria.page(), criteria.size(), total,
                (int) Math.ceil((double) total / criteria.size()),
                criteria.sort() == null ? "publishTime,desc" : criteria.sort());
    }

    @Override
    public ArticleView get(long id) {
        return jdbc.sql(SELECT + " where a.id=:id").param("id", id).query(this::map).optional()
                .orElseThrow(() -> new ResourceNotFoundException("文章不存在：" + id));
    }

    private ArticleView map(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ArticleView(
                resultSet.getLong("id"), resultSet.getLong("source_id"), resultSet.getString("source_name"),
                resultSet.getString("title"), resultSet.getString("author"), resultSet.getString("summary"),
                resultSet.getString("original_url"), resultSet.getString("normalized_url"),
                resultSet.getString("language"), resultSet.getObject("publish_time", OffsetDateTime.class),
                resultSet.getBoolean("publish_time_inferred"),
                resultSet.getString("content_html"), resultSet.getString("content_text"),
                resultSet.getInt("word_count"), resultSet.getInt("reading_minutes"),
                resultSet.getInt("quality_score"), resultSet.getString("review_status"),
                resultSet.getString("source_level"),
                resultSet.getString("content_origin"),
                resultSet.getString("reading_status"), resultSet.getBoolean("favorite"),
                resultSet.getBoolean("archived"),
                resultSet.getObject("first_collected_at", OffsetDateTime.class),
                resultSet.getObject("last_collected_at", OffsetDateTime.class)
        );
    }
}
