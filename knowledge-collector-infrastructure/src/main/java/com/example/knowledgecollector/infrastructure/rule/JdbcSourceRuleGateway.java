package com.example.knowledgecollector.infrastructure.rule;

import com.example.knowledgecollector.application.exception.ResourceNotFoundException;
import com.example.knowledgecollector.application.rule.SourceRuleCommand;
import com.example.knowledgecollector.application.rule.SourceRuleGateway;
import com.example.knowledgecollector.application.rule.SourceRuleView;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcSourceRuleGateway implements SourceRuleGateway {
    private final JdbcClient jdbc;

    public JdbcSourceRuleGateway(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public SourceRuleView create(long sourceId, SourceRuleCommand command) {
        int version = jdbc.sql("select coalesce(max(rule_version),0)+1 from source_rule where source_id=:sourceId")
                .param("sourceId", sourceId).query(Integer.class).single();
        if (command.enabled()) {
            jdbc.sql("update source_rule set enabled=false where source_id=:sourceId")
                    .param("sourceId", sourceId).update();
        }
        OffsetDateTime now = OffsetDateTime.now();
        jdbc.sql("""
                insert into source_rule(source_id,rule_version,list_selector,link_selector,title_selector,
                content_selector,author_selector,publish_time_selector,date_pattern,remove_selectors,enabled,created_at)
                values(:sourceId,:version,:listSelector,:linkSelector,:titleSelector,:contentSelector,
                :authorSelector,:publishTimeSelector,:datePattern,:removeSelectors,:enabled,:createdAt)
                """)
                .param("sourceId", sourceId).param("version", version)
                .param("listSelector", command.listSelector()).param("linkSelector", command.linkSelector())
                .param("titleSelector", command.titleSelector()).param("contentSelector", command.contentSelector())
                .param("authorSelector", command.authorSelector()).param("publishTimeSelector", command.publishTimeSelector())
                .param("datePattern", command.datePattern()).param("removeSelectors", nullToEmpty(command.removeSelectors()))
                .param("enabled", command.enabled()).param("createdAt", now).update();
        return jdbc.sql("select * from source_rule where source_id=:sourceId and rule_version=:version")
                .param("sourceId", sourceId).param("version", version).query(this::map).single();
    }

    @Override
    public Optional<SourceRuleView> findActive(long sourceId) {
        return jdbc.sql("select * from source_rule where source_id=:sourceId and enabled=true order by rule_version desc")
                .param("sourceId", sourceId).query(this::map).optional();
    }

    @Override
    public List<SourceRuleView> findVersions(long sourceId) {
        return jdbc.sql("select * from source_rule where source_id=:sourceId order by rule_version desc")
                .param("sourceId", sourceId).query(this::map).list();
    }

    @Override
    @Transactional
    public SourceRuleView activate(long sourceId, long ruleId) {
        int exists = jdbc.sql("select count(*) from source_rule where id=:id and source_id=:sourceId")
                .param("id", ruleId).param("sourceId", sourceId).query(Integer.class).single();
        if (exists == 0) {
            throw new ResourceNotFoundException("采集规则不存在：" + ruleId);
        }
        jdbc.sql("update source_rule set enabled=false where source_id=:sourceId")
                .param("sourceId", sourceId).update();
        jdbc.sql("update source_rule set enabled=true where id=:id").param("id", ruleId).update();
        return jdbc.sql("select * from source_rule where id=:id").param("id", ruleId).query(this::map).single();
    }

    private SourceRuleView map(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SourceRuleView(
                resultSet.getLong("id"), resultSet.getLong("source_id"), resultSet.getInt("rule_version"),
                resultSet.getString("list_selector"), resultSet.getString("link_selector"),
                resultSet.getString("title_selector"), resultSet.getString("content_selector"),
                resultSet.getString("author_selector"), resultSet.getString("publish_time_selector"),
                resultSet.getString("date_pattern"), resultSet.getString("remove_selectors"),
                resultSet.getBoolean("enabled"), resultSet.getObject("created_at", OffsetDateTime.class)
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
