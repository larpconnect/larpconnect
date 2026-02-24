plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:${libs.versions.spotbugsPlugin.get()}")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:${libs.versions.spotless.get()}")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:${libs.versions.errorprone.plugin.get()}")

    // Workaround to expose version catalog accessors to precompiled script plugins
    compileOnly(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
