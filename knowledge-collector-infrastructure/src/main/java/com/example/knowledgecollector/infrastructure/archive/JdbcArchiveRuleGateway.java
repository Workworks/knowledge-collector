package com.example.knowledgecollector.infrastructure.archive;

import com.example.knowledgecollector.application.archive.ArchiveRuleGateway;
import com.example.knowledgecollector.application.archive.ArchiveRuleView;
import com.example.knowledgecollector.application.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class JdbcArchiveRuleGateway implements ArchiveRuleGateway {
    private static final String SELECT = "select * from archive_rule";
    private final JdbcClient jdbc;

    public JdbcArchiveRuleGateway(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override
    public List<ArchiveRuleView> findAll() {
        return jdbc.sql(SELECT + " order by sort_order,id").query(this::map).list();
    }

    @Override
    public ArchiveRuleView save(Long id, String name, String keyword, Long topicId,
                                Long sourceId, Integer minQuality, int sortOrder, boolean enabled) {
        OffsetDateTime now = OffsetDateTime.now();
        if (id == null) {
            jdbc.sql("""
                    insert into archive_rule(rule_name,keyword,topic_id,source_id,min_quality,sort_order,enabled,created_at,updated_at)
                    values(:name,:keyword,:topicId,:sourceId,:minQuality,:sortOrder,:enabled,:now,:now)
                    """).param("name", name).param("keyword", keyword).param("topicId", topicId)
                    .param("sourceId", sourceId).param("minQuality", minQuality).param("sortOrder", sortOrder)
                    .param("enabled", enabled).param("now", now).update();
            id = jdbc.sql("select max(id) from archive_rule").query(Long.class).single();
        } else {
            int changed = jdbc.sql("""
                    update archive_rule set rule_name=:name,keyword=:keyword,topic_id=:topicId,
                    source_id=:sourceId,min_quality=:minQuality,sort_order=:sortOrder,enabled=:enabled,updated_at=:now
                    where id=:id
                    """).param("name", name).param("keyword", keyword).param("topicId", topicId)
                    .param("sourceId", sourceId).param("minQuality", minQuality).param("sortOrder", sortOrder)
                    .param("enabled", enabled).param("now", now).param("id", id).update();
            if (changed == 0) throw new ResourceNotFoundException("归档整理规则不存在：" + id);
        }
        long ruleId = id;
        return jdbc.sql(SELECT + " where id=:id").param("id", ruleId).query(this::map).single();
    }

    @Override
    public void delete(long id) {
        if (jdbc.sql("delete from archive_rule where id=:id").param("id", id).update() == 0) {
            throw new ResourceNotFoundException("归档整理规则不存在：" + id);
        }
    }

    private ArchiveRuleView map(ResultSet rs, int row) throws SQLException {
        return new ArchiveRuleView(rs.getLong("id"), rs.getString("rule_name"), rs.getString("keyword"),
                (Long) rs.getObject("topic_id"), (Long) rs.getObject("source_id"),
                (Integer) rs.getObject("min_quality"), rs.getInt("sort_order"), rs.getBoolean("enabled"),
                rs.getObject("created_at", OffsetDateTime.class), rs.getObject("updated_at", OffsetDateTime.class));
    }
}
