/*
 * api library module build script.
 * Deals with routing and web services.
 */

plugins {
    id("njall.java-library-conventions")
}

dependencies {
    implementation(platform(project(":parent")))

    // Grows out of :events
    implementation(project(":events"))

    // Routing and web APIs
    implementation(libs.vertx.web)
}
