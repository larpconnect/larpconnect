plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:${libs.versions.spotbugs.get()}")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:${libs.versions.spotless.get()}")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:4.1.0")
}
