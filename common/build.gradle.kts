/*
 * common library module build script.
 */

plugins {
    id("njall.java-library-conventions")
}

dependencies {
    api(platform(project(":parent")))

    api(libs.typesafe.config)
    api(libs.guice)

    testImplementation(project(":test"))
}
