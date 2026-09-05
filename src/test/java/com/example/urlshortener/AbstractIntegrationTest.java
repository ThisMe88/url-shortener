package com.example.urlshortener;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for tests that need a real PostgreSQL instance.
 *
 * <p>A single {@code postgres:16-alpine} container is started once per JVM and shared across
 * every subclass. {@code disabledWithoutDocker = true} means the whole suite is skipped
 * (not failed) on machines without a Docker environment; CI and evaluator machines with
 * Docker run it in full.
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
}
