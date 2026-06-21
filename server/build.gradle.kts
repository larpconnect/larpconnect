/*
 * server application module build script.
 */

plugins {
    id("njall.java-application-conventions")
}

dependencies {
    implementation(platform(project(":parent")))

    // Depends on api routing
    implementation(project(":api"))

    // Compiles and installs all operational modules
    implementation(project(":common"))
    implementation(project(":events"))
    implementation(project(":queue"))
    implementation(project(":data"))
    implementation(project(":base"))

    // Logback runtime logging implementation
    implementation(libs.logback.classic)
}

application {
    mainClass.set("org.larpconnect.server.ServerApp")
}
