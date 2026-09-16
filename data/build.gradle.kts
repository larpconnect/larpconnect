/*
 * data persistence library module build script.
 */

plugins {
    id("njall.java-library-conventions")
}

dependencies {
    api(platform(project(":parent")))

    api(project(":common"))
    api(libs.guice)
    api(libs.flyway.core)
    api(libs.flyway.database.postgresql)
    api(libs.hibernate.core)
    api(libs.postgresql)
    api(libs.slf4j.api)
    compileOnly(libs.checker.qual)
    testCompileOnly(libs.checker.qual)

    testImplementation(project(":test"))
}
