/*
 * integration test harness module build script.
 */

plugins {
    id("njall.java-library-conventions")
}

dependencies {
    api(platform(project(":parent")))

    testImplementation(project(":common"))
    testImplementation(project(":api"))
    testImplementation(project(":server"))
    testImplementation(project(":test"))
    testImplementation(libs.pekko.actor.typed)
    testImplementation(libs.cucumber.java)
    testImplementation(libs.cucumber.junit.platform.engine)
    testImplementation(libs.junit.platform.suite)
}
