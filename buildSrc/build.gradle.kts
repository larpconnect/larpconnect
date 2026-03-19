plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    // Workaround to expose version catalog accessors to precompiled script plugins
    compileOnly(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    implementation(libs.errorprone.gradle.plugin)
    implementation(libs.shadow.gradle.plugin)
    implementation(libs.spotbugs.gradle.plugin)
    implementation(libs.spotless.gradle.plugin)
}
