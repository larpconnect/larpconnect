/*
 * Convention plugins build-logic build configuration.
 */

plugins {
    `kotlin-dsl`
}

repositories {
    google()
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    // Reference the plugin classes as dependencies of the build-logic compilation classpath
    implementation(libs.spotless.plugin)
    implementation(libs.spotbugs.plugin)
    implementation(libs.errorprone.plugin.dep)
}
