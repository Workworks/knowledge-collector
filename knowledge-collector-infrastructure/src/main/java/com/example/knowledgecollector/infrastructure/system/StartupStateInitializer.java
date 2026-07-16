package com.example.knowledgecollector.infrastructure.system;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class StartupStateInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public StartupStateInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String currentValue = jdbcTemplate.queryForObject(
                "select setting_value from system_setting where setting_key = ?",
                String.class,
                "system.startup-count"
        );
        long nextCount = currentValue == null ? 1 : Long.parseLong(currentValue) + 1;
        OffsetDateTime now = OffsetDateTime.now();

        mergeSetting("system.startup-count", Long.toString(nextCount), "LONG",
                "Number of successfully initialized application contexts", now);
        mergeSetting("system.last-started-at", now.toString(), "OFFSET_DATE_TIME",
                "Most recent successful application initialization time", now);
    }

    private void mergeSetting(
            String key,
            String value,
            String valueType,
            String description,
            OffsetDateTime updatedAt
    ) {
        jdbcTemplate.update("""
                merge into system_setting
                    (setting_key, setting_value, value_type, description, updated_at)
                key (setting_key)
                values (?, ?, ?, ?, ?)
                """, key, value, valueType, description, updatedAt);
    }
}
