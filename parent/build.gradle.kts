/*
 * parent platform module build script.
 * Defines shared version constraints and compiler arguments.
 */

plugins {
    id("njall.java-platform-conventions")
}

dependencies {
    constraints {
        // Core libraries version constraints exported via java-platform
        api(libs.guava)
        api(libs.guice)
        api(libs.mug)
        api(libs.vertx.core)
        api(libs.vertx.web)
        api(libs.hibernate.core)
        api(libs.postgresql)
        api(libs.caffeine)
        api(libs.rabbitmq.amqp)
        api(libs.slf4j.api)
        api(libs.flyway.core)
        api(libs.flyway.database.postgresql)
        api(libs.testcontainers)
        api(libs.testcontainers.postgresql)
        api(libs.testcontainers.jdbc)
        api(libs.testcontainers.junit.jupiter)
        api(libs.jsr305)
    }
}

// Compiler arguments are managed by the common convention plugin.
