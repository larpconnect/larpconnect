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
    jvmArgs("-XX:+IgnoreUnrecognizedVMOptions", "--illegal-final-field-mutation=deny")
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
