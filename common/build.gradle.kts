/*
 * common library module build script.
 */

plugins {
    id("njall.java-library-conventions")
}

dependencies {
    // Import platform constraints from parent
    api(platform(project(":parent")))

    // Expose permitted core libraries
    api(libs.guava)
    api(libs.guice)
    api(libs.mug)

    // Internal implementations
    implementation(libs.slf4j.api)
}
