/*
 * api library module build script.
 */

plugins {
    id("njall.java-library-conventions")
}

dependencies {
    api(platform(project(":parent")))

    api(project(":common"))
    api(project(":data"))
    api(libs.pekko.http)
    api(libs.pekko.http.jackson)
    api(libs.opentelemetry.api)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.datatype.guava)
    implementation(libs.pekko.actor.typed)
    implementation(libs.pekko.stream)

    testImplementation(project(":test"))
    testImplementation(libs.opentelemetry.sdk)
}
