/*
 * data library module build script.
 * Enforces database layer concerns. Absolutely no Vert.x dependencies allowed.
 */

plugins {
    id("njall.java-library-conventions")
}

dependencies {
    implementation(platform(project(":parent")))

    // Grows out of :common
    implementation(project(":common"))

    // Database access dependencies
    implementation(libs.hibernate.core)
    implementation(libs.postgresql)
    implementation(libs.caffeine)
    implementation(libs.slf4j.api)

    // Test dependencies
    testImplementation(project(":test"))
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
}

