import org.gradle.accessors.dm.LibrariesForLibs
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `application`
    id("larpconnect.java-common")
    id("larpconnect.testing")
    id("com.gradleup.shadow")
}

val libs = the<LibrariesForLibs>()

dependencies {
    runtimeOnly(libs.logback.classic)
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    archiveBaseName.set("larpconnect")
    mergeServiceFiles()
}
