package com.example.knowledgecollector.infrastructure.source;

import com.example.knowledgecollector.domain.source.SourceType;
import com.example.knowledgecollector.infrastructure.topic.TopicJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "crawl_source")
public class CrawlSourceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "source_code", nullable = false, unique = true, length = 64)
    String code;
    @Column(name = "source_name", nullable = false, length = 128)
    String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    SourceType type;
    @Column(name = "home_url", length = 2048)
    String homeUrl;
    @Column(name = "feed_url", length = 2048)
    String feedUrl;
    @Column(nullable = false, length = 16)
    String language;
    @Column(nullable = false, length = 32)
    String charset;
    @Column(name = "user_agent", nullable = false, length = 256)
    String userAgent;
    @Column(name = "timeout_seconds", nullable = false)
    int timeoutSeconds;
    @Column(name = "max_retries", nullable = false)
    int maxRetries;
    @Column(name = "request_interval_millis", nullable = false)
    long requestIntervalMillis;
    @Column(name = "obey_robots", nullable = false)
    boolean obeyRobots;
    @Column(name = "fetch_full_content", nullable = false)
    boolean fetchFullContent;
    @Column(name = "summary_only", nullable = false)
    boolean summaryOnly;
    @Column(name = "save_snapshot", nullable = false)
    boolean saveSnapshot;
    @Column(nullable = false)
    boolean enabled;
    @Column(name = "last_success_at")
    OffsetDateTime lastSuccessAt;
    @Column(name = "last_failure_at")
    OffsetDateTime lastFailureAt;
    @Column(name = "consecutive_failures", nullable = false)
    int consecutiveFailures;
    @Column(length = 2000)
    String notes;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "topic_source_rel",
            joinColumns = @JoinColumn(name = "source_id"),
            inverseJoinColumns = @JoinColumn(name = "topic_id"))
    Set<TopicJpaEntity> topics = new LinkedHashSet<>();
    @Version
    @Column(name = "lock_version", nullable = false)
    long version;
    @Column(name = "created_at", nullable = false)
    OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    OffsetDateTime updatedAt;

    protected CrawlSourceJpaEntity() {
    }
}
