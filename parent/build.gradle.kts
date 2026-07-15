/*
 * parent platform module build script.
 * Defines shared version constraints and compiler arguments.
 */

plugins {
    id("njall.java-platform-conventions")
}

javaPlatform {
    allowDependencies()
}

dependencies {
    api(platform(libs.junit.bom))
    api(platform(libs.mockito.bom))
    api(platform(libs.testcontainers.bom))
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
        api(libs.jsr305)
    }
}

// Compiler arguments are managed by the common convention plugin.
