/*
 * test library module build script.
 */

plugins {
    id("njall.java-library-conventions")
}

dependencies {
    api(platform(project(":parent")))

    // Expose testing and core libraries
    api(libs.junit.jupiter)
    api(libs.mockito.core)
    api(libs.cucumber.java)
    api(libs.cucumber.junit.platform.engine)
    api(libs.guice)
    api(libs.assertj.core)

    implementation(libs.slf4j.api)
}
