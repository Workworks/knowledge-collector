package com.example.knowledgecollector.infrastructure.system;

import com.example.knowledgecollector.application.system.SystemStatus;
import com.example.knowledgecollector.application.system.SystemStatusQuery;
import com.example.knowledgecollector.infrastructure.storage.StorageProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.OffsetDateTime;

@Service
public class JdbcSystemStatusQuery implements SystemStatusQuery {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final StorageProperties storageProperties;
    private final BuildProperties buildProperties;
    private final String applicationName;

    public JdbcSystemStatusQuery(
            JdbcTemplate jdbcTemplate,
            DataSource dataSource,
            StorageProperties storageProperties,
            ObjectProvider<BuildProperties> buildProperties,
            @Value("${spring.application.name:knowledge-collector}") String applicationName
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.storageProperties = storageProperties;
        this.buildProperties = buildProperties.getIfAvailable();
        this.applicationName = applicationName;
    }

    @Override
    public SystemStatus getStatus() {
        DatabaseDetails database = databaseDetails();
        Integer migrationCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from "flyway_schema_history"
                where "success" = true and "version" is not null
                """,
                Integer.class
        );
        Long startupCount = readLongSetting("system.startup-count");
        OffsetDateTime lastStartedAt = OffsetDateTime.parse(readSetting("system.last-started-at"));

        return new SystemStatus(
                applicationName,
                buildProperties == null ? "1.0.0" : buildProperties.getVersion(),
                database.product(),
                database.version(),
                migrationCount == null ? 0 : migrationCount,
                startupCount == null ? 0 : startupCount,
                lastStartedAt,
                storageProperties.root().toAbsolutePath().normalize().toString()
        );
    }

    private Long readLongSetting(String key) {
        return Long.parseLong(readSetting(key));
    }

    private String readSetting(String key) {
        return jdbcTemplate.queryForObject(
                "select setting_value from system_setting where setting_key = ?",
                String.class,
                key
        );
    }

    private DatabaseDetails databaseDetails() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            return new DatabaseDetails(
                    metaData.getDatabaseProductName(),
                    metaData.getDatabaseProductVersion()
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to read database metadata", exception);
        }
    }

    private record DatabaseDetails(String product, String version) {
    }
}
