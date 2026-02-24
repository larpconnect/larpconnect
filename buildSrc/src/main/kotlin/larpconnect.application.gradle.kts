import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    `application`
    id("larpconnect.java-common")
    id("larpconnect.testing")
}

val libs = the<LibrariesForLibs>()

dependencies {
    implementation(libs.logback.classic)
}
