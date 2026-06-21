/*
 * LarpConnect root settings configuration.
 */

pluginManagement {
    // Include 'build-logic' to define convention plugins.
    includeBuild("build-logic")
}

plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "larpconnect"

// Subprojects topological layout
include(
    "parent",
    "bom",
    "common",
    "events",
    "test",
    "queue",
    "data",
    "base",
    "api",
    "server",
    "integration"
)
