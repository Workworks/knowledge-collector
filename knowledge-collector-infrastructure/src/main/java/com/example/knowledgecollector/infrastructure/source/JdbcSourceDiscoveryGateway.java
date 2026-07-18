package com.example.knowledgecollector.infrastructure.source;

import com.example.knowledgecollector.application.exception.ResourceNotFoundException;
import com.example.knowledgecollector.application.source.DiscoveryCandidateView;
import com.example.knowledgecollector.application.source.SourceDiscoveryGateway;
import com.example.knowledgecollector.capability.source.SourceDiscoveryProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class JdbcSourceDiscoveryGateway implements SourceDiscoveryGateway {
    private final JdbcClient jdbc;
    public JdbcSourceDiscoveryGateway(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override @Transactional
    public List<DiscoveryCandidateView> replace(String providerId, String topic, List<SourceDiscoveryProvider.Candidate> candidates) {
        jdbc.sql("update source_discovery_candidate set ignored=true,updated_at=:now where provider_id=:provider and imported_source_id is null")
                .param("now", OffsetDateTime.now()).param("provider", providerId).update();
        for (var c : candidates) {
            jdbc.sql("""
                    insert into source_discovery_candidate(provider_id,topic,name,website_url,collection_url,source_type,
                    language,reliability_score,recommendation_reason,created_at,updated_at)
                    values(:provider,:topic,:name,:website,:collection,:type,:language,:score,:reason,:now,:now)
                    """).param("provider", providerId).param("topic", topic).param("name", c.name())
                    .param("website", c.websiteUrl()).param("collection", c.collectionUrl()).param("type", c.sourceType())
                    .param("language", c.language()).param("score", c.reliabilityScore()).param("reason", c.reason())
                    .param("now", OffsetDateTime.now()).update();
        }
        return list();
    }
    @Override public List<DiscoveryCandidateView> list() {
        return jdbc.sql("select * from source_discovery_candidate where ignored=false order by id desc").query(this::map).list();
    }
    @Override public DiscoveryCandidateView get(long id) {
        return jdbc.sql("select * from source_discovery_candidate where id=:id").param("id", id).query(this::map)
                .optional().orElseThrow(() -> new ResourceNotFoundException("候选采集源不存在：" + id));
    }
    @Override public DiscoveryCandidateView validation(long id, boolean valid, String message) {
        jdbc.sql("update source_discovery_candidate set validation_status=:status,validation_message=:message,validated_at=:now,updated_at=:now where id=:id")
                .param("status", valid ? "VERIFIED" : "FAILED").param("message", message)
                .param("now", OffsetDateTime.now()).param("id", id).update(); return get(id);
    }
    @Override public DiscoveryCandidateView imported(long id, long sourceId) {
        jdbc.sql("update source_discovery_candidate set validation_status='IMPORTED',imported_source_id=:source,updated_at=:now where id=:id")
                .param("source", sourceId).param("now", OffsetDateTime.now()).param("id", id).update(); return get(id);
    }
    @Override public DiscoveryCandidateView ignored(long id) {
        get(id); jdbc.sql("update source_discovery_candidate set ignored=true,updated_at=:now where id=:id")
                .param("now", OffsetDateTime.now()).param("id", id).update(); return get(id);
    }
    private DiscoveryCandidateView map(ResultSet rs, int row) throws SQLException {
        long sourceId=rs.getLong("imported_source_id"); boolean sourceIdNull=rs.wasNull();
        return new DiscoveryCandidateView(rs.getLong("id"),rs.getString("provider_id"),rs.getString("topic"),
                rs.getString("name"),rs.getString("website_url"),rs.getString("collection_url"),rs.getString("source_type"),
                rs.getString("language"),rs.getInt("reliability_score"),rs.getString("validation_status"),
                rs.getString("recommendation_reason"),rs.getString("validation_message"),sourceIdNull?null:sourceId,
                rs.getBoolean("ignored"),rs.getObject("created_at",OffsetDateTime.class),rs.getObject("validated_at",OffsetDateTime.class));
    }
}
