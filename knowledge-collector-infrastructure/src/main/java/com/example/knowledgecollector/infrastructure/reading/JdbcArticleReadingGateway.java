package com.example.knowledgecollector.infrastructure.reading;

import com.example.knowledgecollector.application.reading.ArticleReadingGateway;
import com.example.knowledgecollector.application.reading.ArticleReadingView;
import com.example.knowledgecollector.application.reading.TagView;
import com.example.knowledgecollector.application.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class JdbcArticleReadingGateway implements ArticleReadingGateway {
    private final JdbcClient jdbc;

    public JdbcArticleReadingGateway(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public ArticleReadingView get(long articleId) {
        var rows = jdbc.sql("""
                select a.id,a.reading_status,a.favorite,a.archived,n.note_content,n.updated_at note_updated_at
                from article a left join article_note n on n.article_id=a.id where a.id=:id
                """).param("id", articleId).query().listOfRows();
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("文章不存在：" + articleId);
        }
        var row = rows.get(0);
        return new ArticleReadingView(articleId, (String) row.get("READING_STATUS"),
                Boolean.TRUE.equals(row.get("FAVORITE")), Boolean.TRUE.equals(row.get("ARCHIVED")),
                (String) row.get("NOTE_CONTENT"), (OffsetDateTime) row.get("NOTE_UPDATED_AT"),
                articleTags(articleId));
    }

    @Override
    public ArticleReadingView updateState(long articleId, String readingStatus,
                                          Boolean favorite, Boolean archived) {
        jdbc.sql("""
                update article set reading_status=coalesce(:readingStatus,reading_status),
                favorite=coalesce(:favorite,favorite),archived=coalesce(:archived,archived),
                updated_at=:now where id=:id
                """).param("readingStatus", readingStatus).param("favorite", favorite)
                .param("archived", archived).param("now", OffsetDateTime.now())
                .param("id", articleId).update();
        return get(articleId);
    }

    @Override
    public ArticleReadingView saveNote(long articleId, String content) {
        OffsetDateTime now = OffsetDateTime.now();
        int updated = jdbc.sql("update article_note set note_content=:content,updated_at=:now where article_id=:id")
                .param("content", content).param("now", now).param("id", articleId).update();
        if (updated == 0) {
            jdbc.sql("""
                    insert into article_note(article_id,note_content,created_at,updated_at)
                    values(:id,:content,:now,:now)
                    """).param("id", articleId).param("content", content).param("now", now).update();
        }
        return get(articleId);
    }

    @Override
    @Transactional
    public ArticleReadingView replaceTags(long articleId, List<String> tagNames) {
        jdbc.sql("delete from article_tag_rel where article_id=:id").param("id", articleId).update();
        for (String name : tagNames) {
            var existing = jdbc.sql("select id from tag where lower(tag_name)=lower(:name)")
                    .param("name", name).query(Long.class).optional();
            long tagId;
            if (existing.isPresent()) {
                tagId = existing.get();
            } else {
                OffsetDateTime now = OffsetDateTime.now();
                jdbc.sql("""
                        insert into tag(tag_name,color,created_at,updated_at)
                        values(:name,'#64748B',:now,:now)
                        """).param("name", name).param("now", now).update();
                tagId = jdbc.sql("select id from tag where lower(tag_name)=lower(:name)")
                        .param("name", name).query(Long.class).single();
            }
            jdbc.sql("""
                    insert into article_tag_rel(article_id,tag_id,created_at)
                    values(:articleId,:tagId,:now)
                    """).param("articleId", articleId).param("tagId", tagId)
                    .param("now", OffsetDateTime.now()).update();
        }
        return get(articleId);
    }

    @Override
    public List<TagView> tags() {
        return jdbc.sql("""
                select t.id,t.tag_name,t.color from tag t
                where exists(select 1 from article_tag_rel r where r.tag_id=t.id)
                order by lower(t.tag_name),t.id
                """).query((rs, rowNum) ->
                new TagView(rs.getLong("id"), rs.getString("tag_name"), rs.getString("color"))).list();
    }

    private List<TagView> articleTags(long articleId) {
        return jdbc.sql("""
                select t.id,t.tag_name,t.color from tag t join article_tag_rel r on r.tag_id=t.id
                where r.article_id=:id order by lower(t.tag_name),t.id
                """).param("id", articleId).query((rs, rowNum) ->
                new TagView(rs.getLong("id"), rs.getString("tag_name"), rs.getString("color"))).list();
    }
}
