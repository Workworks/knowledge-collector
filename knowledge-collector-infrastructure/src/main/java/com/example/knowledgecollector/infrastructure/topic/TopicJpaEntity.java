package com.example.knowledgecollector.infrastructure.topic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;

@Entity
@Table(name = "topic")
public class TopicJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "topic_code", nullable = false, unique = true, length = 64)
    String code;
    @Column(name = "topic_name", nullable = false, unique = true, length = 128)
    String name;
    @Column(length = 1000)
    String description;
    @Column(nullable = false, columnDefinition = "CLOB")
    String keywords;
    @Column(name = "excluded_keywords", nullable = false, columnDefinition = "CLOB")
    String excludedKeywords;
    @Column(nullable = false, length = 16)
    String color;
    @Column(length = 64)
    String icon;
    @Column(nullable = false, length = 16)
    String language;
    @Column(nullable = false)
    boolean enabled;
    @Column(name = "sort_order", nullable = false)
    int sortOrder;
    @Version
    @Column(name = "lock_version", nullable = false)
    long version;
    @Column(name = "created_at", nullable = false)
    OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    OffsetDateTime updatedAt;

    protected TopicJpaEntity() {
    }

    public Long getId() {
        return id;
    }
}
