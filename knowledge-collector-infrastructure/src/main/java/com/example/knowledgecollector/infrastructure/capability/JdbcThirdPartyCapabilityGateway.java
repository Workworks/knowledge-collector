package com.example.knowledgecollector.infrastructure.capability;

import com.example.knowledgecollector.application.capability.*;
import com.example.knowledgecollector.application.exception.ResourceNotFoundException;
import com.example.knowledgecollector.capability.management.ManagedCapabilityProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcThirdPartyCapabilityGateway implements ThirdPartyCapabilityGateway {
    private final JdbcClient jdbc;
    public JdbcThirdPartyCapabilityGateway(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override public List<ThirdPartyServiceView> listServices() {
        return jdbc.sql(serviceSql() + " order by s.service_type,s.id").query(this::service).list();
    }
    @Override public Optional<ThirdPartyServiceView> findService(String providerId) {
        return jdbc.sql(serviceSql() + " where s.provider_id=:providerId").param("providerId", providerId)
                .query(this::service).optional();
    }
    @Override public Optional<ManagedCapabilityProvider.RuntimeConfiguration> runtimeConfiguration(String providerId) {
        return jdbc.sql("select enabled,endpoint,model,authentication_type,credential from third_party_service where provider_id=:id")
                .param("id", providerId).query((rs, row) -> new ManagedCapabilityProvider.RuntimeConfiguration(
                        rs.getBoolean("enabled"), rs.getString("endpoint"), rs.getString("model"),
                        rs.getString("authentication_type"), rs.getString("credential"))).optional();
    }

    @Override @Transactional
    public ThirdPartyServiceView save(String providerId, ThirdPartyServiceCommand c) {
        ThirdPartyServiceView existing = findService(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("第三方服务不存在：" + providerId));
        if (c.defaultProvider()) {
            jdbc.sql("update third_party_service set default_provider=false where service_type=:type and provider_id<>:id")
                    .param("type", existing.serviceType()).param("id", providerId).update();
        }
        jdbc.sql("""
                update third_party_service set endpoint=:endpoint,model=:model,authentication_type=:auth,
                credential=case when :credential='' then credential else :credential end,enabled=:enabled,
                default_provider=:isDefault,fallback_provider=:fallback,user_configured=true,updated_at=:now where provider_id=:id
                """).param("endpoint", c.endpoint().trim()).param("model", blank(c.model()))
                .param("auth", c.authenticationType() == null ? "NONE" : c.authenticationType())
                .param("credential", c.credential() == null ? "" : c.credential())
                .param("enabled", c.enabled()).param("isDefault", c.defaultProvider())
                .param("fallback", c.fallbackProvider()).param("now", OffsetDateTime.now())
                .param("id", providerId).update();
        return findService(providerId).orElseThrow();
    }

    @Override public void updateHealth(String providerId, boolean available, String message) {
        OffsetDateTime now = OffsetDateTime.now();
        jdbc.sql("""
                update third_party_service set health_status=:status,last_checked_at=:now,
                last_success_at=case when :available=true then :now else last_success_at end,
                last_error=case when :available=true then null else :message end,updated_at=:now where provider_id=:id
                """).param("status", available ? "AVAILABLE" : "UNAVAILABLE").param("now", now)
                .param("available", available).param("message", trim(message, 4000)).param("id", providerId).update();
    }

    @Override public long beginCall(String providerId, String operation, String scene, String requestSummary, Long retryOfId) {
        long id = jdbc.sql("select next value for third_party_call_log_seq").query(Long.class).single();
        jdbc.sql("""
                insert into third_party_call_log(id,provider_id,operation,business_scene,status,request_summary,retry_of_id,created_at)
                values(:id,:provider,:operation,:scene,'RUNNING',:request,:retry,:now)
                """).param("provider", providerId).param("operation", operation).param("scene", scene)
                .param("id", id)
                .param("request", trim(requestSummary, 4000)).param("retry", retryOfId)
                .param("now", OffsetDateTime.now()).update();
        return id;
    }

    @Override public void finishCall(long id, boolean success, String resultSummary, String errorMessage, long durationMillis) {
        jdbc.sql("""
                update third_party_call_log set status=:status,result_summary=:result,error_message=:error,
                duration_millis=:duration,finished_at=:now where id=:id
                """).param("status", success ? "SUCCESS" : "FAILED").param("result", trim(resultSummary, 4000))
                .param("error", trim(errorMessage, 4000)).param("duration", Math.max(0, durationMillis))
                .param("now", OffsetDateTime.now()).param("id", id).update();
    }

    @Override public List<ThirdPartyCallLogView> listCalls(int limit) {
        return jdbc.sql("select * from third_party_call_log order by id desc limit :limit")
                .param("limit", limit).query(this::call).list();
    }
    @Override public ThirdPartyCallLogView getCall(long id) {
        return jdbc.sql("select * from third_party_call_log where id=:id").param("id", id)
                .query(this::call).optional().orElseThrow(() -> new ResourceNotFoundException("调用记录不存在：" + id));
    }

    private String serviceSql() { return """
            select s.*,
            (select count(*) from third_party_call_log l where l.provider_id=s.provider_id and l.created_at>=current_date) today_calls,
            coalesce((select avg(l.duration_millis) from third_party_call_log l where l.provider_id=s.provider_id and l.status<>'RUNNING'),0) average_duration
            from third_party_service s
            """; }
    private ThirdPartyServiceView service(ResultSet rs, int row) throws SQLException {
        return new ThirdPartyServiceView(rs.getLong("id"), rs.getString("provider_id"), rs.getString("service_name"),
                rs.getString("service_type"), rs.getString("implementation_name"), rs.getString("endpoint"),
                rs.getString("model"), rs.getString("authentication_type"), rs.getString("credential") != null,
                rs.getBoolean("enabled"), rs.getBoolean("default_provider"), rs.getBoolean("fallback_provider"), rs.getBoolean("user_configured"),
                rs.getString("health_status"), rs.getObject("last_checked_at", OffsetDateTime.class),
                rs.getObject("last_success_at", OffsetDateTime.class), rs.getString("last_error"),
                rs.getLong("today_calls"), rs.getLong("average_duration"), List.of());
    }
    private ThirdPartyCallLogView call(ResultSet rs, int row) throws SQLException {
        long retry = rs.getLong("retry_of_id");
        return new ThirdPartyCallLogView(rs.getLong("id"), rs.getString("provider_id"), rs.getString("operation"),
                rs.getString("business_scene"), rs.getString("status"), rs.getString("request_summary"),
                rs.getString("result_summary"), rs.getString("error_message"), rs.wasNull() ? null : retry,
                rs.getLong("duration_millis"), rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("finished_at", OffsetDateTime.class));
    }
    private String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String trim(String value, int max) { return value == null ? null : value.substring(0, Math.min(max, value.length())); }
}
