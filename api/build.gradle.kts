/*
 * api library module build script.
 */

plugins {
    id("njall.java-library-conventions")
}

dependencies {
    api(platform(project(":parent")))

    api(project(":common"))
    api(libs.pekko.http)
    implementation(libs.pekko.actor.typed)
    implementation(libs.pekko.stream)

    testImplementation(project(":test"))
}
