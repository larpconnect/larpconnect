/*
 * Convention plugins build-logic build configuration.
 */

plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Reference the plugin classes as dependencies of the build-logic compilation classpath
    implementation(libs.spotless.plugin)
    implementation(libs.spotbugs.plugin)
    implementation(libs.errorprone.plugin.dep)
    implementation(libs.kotlin.plugin)
}
