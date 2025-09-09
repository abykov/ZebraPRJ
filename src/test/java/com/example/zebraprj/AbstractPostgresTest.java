package com.example.zebraprj;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class AbstractPostgresTest {

    private static final Logger log = LoggerFactory.getLogger(AbstractPostgresTest.class);

    @Container
    @ServiceConnection
    protected static final PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>("postgres:15-alpine");

    static {
        log.info("Using PostgreSQL Docker image: {}", postgreSQLContainer.getDockerImageName());
    }
}
