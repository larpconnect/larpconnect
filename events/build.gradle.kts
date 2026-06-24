/*
 * events library module build script.
 */

plugins {
    id("njall.java-library-conventions")
}

dependencies {
    api(platform(project(":parent")))

    // Grows out of :common
    api(project(":common"))

    // Expose Vert.x core as permitted
    api(libs.vertx.core)

    implementation(libs.slf4j.api)
}
