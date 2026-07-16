package com.example.knowledgecollector.infrastructure.crawl;

import com.example.knowledgecollector.application.article.ArticleGateway;
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
    public PageResult<ArticleView> findPage(String keyword, Long sourceId, int page, int size) {
        StringBuilder where = new StringBuilder(" where 1=1");
        Map<String, Object> params = new HashMap<>();
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (lower(a.title) like :keyword or lower(coalesce(a.author,'')) like :keyword)");
            params.put("keyword", "%" + keyword.trim().toLowerCase() + "%");
        }
        if (sourceId != null) {
            where.append(" and a.source_id=:sourceId");
            params.put("sourceId", sourceId);
        }
        long total = jdbc.sql("select count(*) from article a" + where).params(params).query(Long.class).single();
        params.put("size", size);
        params.put("offset", page * size);
        var content = jdbc.sql(SELECT + where + " order by a.publish_time desc,a.id desc limit :size offset :offset")
                .params(params).query(this::map).list();
        return new PageResult<>(content, page, size, total,
                (int) Math.ceil((double) total / size), "publishTime,desc");
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
                resultSet.getObject("first_collected_at", OffsetDateTime.class),
                resultSet.getObject("last_collected_at", OffsetDateTime.class)
        );
    }
}
