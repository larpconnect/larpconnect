import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("larpconnect.java-common")
}

val libs = the<LibrariesForLibs>()

dependencies {
    if (project.name != "test") {
        testImplementation(project(":test"))
    }

    testRuntimeOnly(libs.cucumber.junit.platform.engine)
    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.logback.classic)
}

tasks.withType<Test>().configureEach {
    // --illegal-final-field-mutation=deny is part of JDK 26 and is here to support forward migration.
    jvmArgs("-XX:+IgnoreUnrecognizedVMOptions", "--illegal-final-field-mutation=deny")
    systemProperty("testcontainers.ryuk.disabled", "true")
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
