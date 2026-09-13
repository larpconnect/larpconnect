/*
 * test library module build script.
 */

plugins {
    id("njall.java-library-conventions")
}

dependencies {
    api(platform(project(":parent")))

    // Test platform BOMs
    api(platform(libs.junit.bom))
    api(platform(libs.mockito.bom))
    api(platform(libs.testcontainers.bom))

    // Core test frameworks and assertion libraries
    api(libs.junit.jupiter)
    api(libs.mockito.core)
    api(libs.mockito.junit.jupiter)
    api(libs.assertj.core)
    api(libs.guice)

    // Test logging runtime
    api(libs.logback.classic)
}
