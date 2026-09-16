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
    
    constraints {
        // Core runtime and ecosystem libraries
        api(libs.guice)
        api(libs.typesafe.config)
        api(libs.caffeine)
        api(libs.jsr305)
        api(libs.checker.qual)
        api(libs.metrics.healthchecks)
        api(libs.picocli)

        // Asynchronous runtime & HTTP (Pekko)
        api(libs.pekko.actor.typed)
        api(libs.pekko.actor.testkit.typed)
        api(libs.pekko.stream)
        api(libs.pekko.stream.testkit)
        api(libs.pekko.http)
        api(libs.pekko.http.testkit)
        api(libs.pekko.http.jackson)

        // Persistence & Messaging        api(libs.postgresql)
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
