import os

base_dir = "data/src/test/java/com/larpconnect/njall/data"

test_java = """package com.larpconnect.njall.data;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jakarta.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration tests for PostgreSQL test container.
 */
@Testcontainers
@ExtendWith(VertxExtension.class)
class PostgresIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16")) // PSQL 16 as Testcontainers might not fully support 18 yet
            .withDatabaseName("njall")
            .withUsername("test")
            .withPassword("test");

    private static Mutiny.SessionFactory sessionFactory;

    @BeforeAll
    static void setUp() {
        POSTGRES.start();

        Map<String, String> properties = new HashMap<>();
        properties.put("jakarta.persistence.jdbc.url", POSTGRES.getJdbcUrl());
        properties.put("jakarta.persistence.jdbc.user", POSTGRES.getUsername());
        properties.put("jakarta.persistence.jdbc.password", POSTGRES.getPassword());

        // Vert.x pg client connection properties
        String vertxUrl = POSTGRES.getJdbcUrl().replace("jdbc:", "vertx-reactive:");
        properties.put("hibernate.connection.url", vertxUrl);
        properties.put("hibernate.connection.username", POSTGRES.getUsername());
        properties.put("hibernate.connection.password", POSTGRES.getPassword());

        properties.put("hibernate.hbm2ddl.auto", "create");

        sessionFactory = Persistence.createEntityManagerFactory("njall-pu", properties)
                .unwrap(Mutiny.SessionFactory.class);
    }

    @AfterAll
    static void tearDown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
        POSTGRES.stop();
    }

    @Test
    void testEntitySave_whenValid_shouldPersist(Vertx vertx, VertxTestContext testContext) {
        ServerMetadata metadata = new ServerMetadata();
        metadata.setEntityType("server_metadata");
        metadata.setName("Test Server");
        metadata.setAdmin("Admin");
        metadata.setSecurity("Sec");
        metadata.setSupport("Support");

        sessionFactory.withTransaction((session, tx) -> session.persist(metadata))
                .subscribe().with(
                        v -> testContext.verify(() -> {
                            assertThat(metadata.getId()).isNotNull();
                            testContext.completeNow();
                        }),
                        testContext::failNow
                );
    }
}
"""

def write_file(filename, content):
    with open(os.path.join(base_dir, filename), "w") as f:
        f.write(content)

write_file("PostgresIntegrationTest.java", test_java)

print("Created PostgresIntegrationTest.")
