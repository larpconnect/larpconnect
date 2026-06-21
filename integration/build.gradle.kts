/*
 * integration test harness module build script.
 */

plugins {
    id("njall.java-library-conventions")
}

dependencies {
    testImplementation(platform(project(":parent")))

    // Depends on shared test framework and the server application
    testImplementation(project(":test"))
    testImplementation(project(":server"))

    // Depends on other submodules for testing individual boundaries
    testImplementation(project(":common"))
    testImplementation(project(":events"))
    testImplementation(project(":queue"))
    testImplementation(project(":data"))
    testImplementation(project(":base"))
    testImplementation(project(":api"))
}
