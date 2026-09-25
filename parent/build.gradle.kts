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
    api(platform(libs.jackson.bom))
    api(platform(libs.slf4j.bom))
    api(platform(libs.hibernate.bom))
    api(platform(libs.guava.bom))
    api(platform(libs.mug.bom))
    api(platform(libs.pekko.bom))
    api(platform(libs.pekko.http.bom))
    api(platform(libs.opentelemetry.bom))
    api(platform(libs.opentelemetry.instrumentation.bom))
    
    constraints {
        // OpenTelemetry
        api(libs.opentelemetry.api)
        api(libs.opentelemetry.sdk)
        api(libs.opentelemetry.logback.mdc)

        // Core runtime and ecosystem libraries
        api(libs.guice)
        api(libs.typesafe.config)
        api(libs.caffeine)
        api(libs.checker.qual)
        api(libs.jspecify)
        api(libs.metrics.healthchecks)
        api(libs.picocli)

        // Persistence & Messaging
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
