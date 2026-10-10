package com.resonanceplanner.common.config;

import static com.resonanceplanner.common.config.AppPropertiesTestSupport.VALID_PROPERTIES;
import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DatabaseConfigTest {

    // Same setup as the real application: our DataSource next to Spring Boot's auto-configuration
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
            .withUserConfiguration(AppPropertiesTestSupport.EnableAppProperties.class, DatabaseConfig.class)
            .withPropertyValues(VALID_PROPERTIES);

    @Test
    void buildsTheConnectionPoolFromTheValidatedProperties() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DataSource.class);
            HikariDataSource dataSource = context.getBean(HikariDataSource.class);
            assertThat(dataSource.getJdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5433/resonance_planner");
            assertThat(dataSource.getUsername()).isEqualTo("resonance");
        });
    }

    @Test
    void createsTheDataSourceOnlyAfterAppPropertiesIsValidated() {
        contextRunner.run(context -> assertThat(context.getBeanFactory().getDependenciesForBean("dataSource"))
                .anyMatch(beanName -> beanName.endsWith("AppProperties")));
    }

    @Test
    void namesTheMissingDatabaseVariableInsteadOfFailingOnTheDriver() {
        contextRunner.withPropertyValues("app.database.url=").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("DATABASE_URL");
        });
    }
}
