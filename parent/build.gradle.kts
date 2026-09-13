/*
 * parent platform module build script.
 * Defines shared version constraints and compiler arguments.
 */

plugins {
    id("njall.java-platform-conventions")
}

dependencies {
    constraints {
        // Core runtime and ecosystem libraries
        api(libs.guava)
        api(libs.guice)
        api(libs.mug)
        api(libs.typesafe.config)
        api(libs.caffeine)
        api(libs.slf4j.api)
        api(libs.jsr305)
        api(libs.metrics.healthchecks)

        // Asynchronous runtime & HTTP (Pekko)
        api(libs.pekko.actor.typed)
        api(libs.pekko.actor.testkit.typed)
        api(libs.pekko.stream)
        api(libs.pekko.stream.testkit)
        api(libs.pekko.http)
        api(libs.pekko.http.testkit)

        // Persistence & Messaging
        api(libs.hibernate.core)
        api(libs.postgresql)
        api(libs.flyway.core)
        api(libs.flyway.database.postgresql)
        api(libs.rabbitmq.amqp)

        // Logging runtime
        api(libs.logback.classic)

        // Testing libraries
        api(libs.assertj.core)
        api(libs.cucumber.java)
        api(libs.cucumber.junit.platform.engine)
        api(libs.junit.platform.suite)
        api(libs.archunit.junit5)
    }
}
