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
    }
}

// Share compiler arguments with subprojects via extra properties
extra["compilerArgs"] = listOf(
    "-Xlint:all",
    "-Werror",
    "-Xlint:-processing",
    "-Xlint:-classfile",
    "-parameters"
)
