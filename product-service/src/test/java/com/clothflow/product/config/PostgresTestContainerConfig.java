package com.clothflow.product.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;

@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestContainerConfig {

    @Bean(destroyMethod = "stop")
    @SuppressWarnings("resource")
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:16")
                .withDatabaseName("clothflow_test")
                .withUsername("test")
                .withPassword("test");
    }

    @Bean
    @Primary
    DataSource testDataSource(PostgreSQLContainer postgres) {

        DriverManagerDataSource dataSource =
                new DriverManagerDataSource();

        dataSource.setDriverClassName(
                postgres.getDriverClassName()
        );

        dataSource.setUrl(
                postgres.getJdbcUrl()
        );

        dataSource.setUsername(
                postgres.getUsername()
        );

        dataSource.setPassword(
                postgres.getPassword()
        );

        return dataSource;
    }
}
