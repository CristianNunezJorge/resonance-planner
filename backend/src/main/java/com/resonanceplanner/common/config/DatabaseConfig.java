package com.resonanceplanner.common.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the connection pool from the validated AppProperties (D83).
 * Spring must validate AppProperties before it can create this bean, so a missing
 * DATABASE_* variable is reported by name instead of as a generic driver error.
 * Because this bean replaces the auto-configured one, spring.datasource.* settings are not used.
 */
@Configuration(proxyBeanMethods = false)
public class DatabaseConfig {

    @Bean
    public DataSource dataSource(AppProperties appProperties) {
        AppProperties.Database database = appProperties.database();
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(database.url())
                .username(database.username())
                .password(database.password())
                .build();
    }
}
