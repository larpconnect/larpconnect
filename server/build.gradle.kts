/*
 * server application module build script.
 */

plugins {
    id("njall.java-application-conventions")
}

application {
    mainClass.set("com.larpconnect.njall.server.ServerApp")
}

dependencies {
    implementation(platform(project(":parent")))

    implementation(project(":common"))
    implementation(project(":data"))
    implementation(project(":api"))
    implementation(libs.picocli)
    implementation(libs.pekko.http)
    implementation(libs.pekko.actor.typed)
    implementation(libs.pekko.stream)
    implementation(libs.logback.classic)
    implementation(libs.opentelemetry.logback.mdc)

    testImplementation(project(":test"))
    testImplementation(libs.opentelemetry.sdk)
}
