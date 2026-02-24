plugins {
    id("larpconnect.java-common")
}

dependencies {
    if (project.name != "test") {
        testImplementation(project(":test"))
    }
    testImplementation(getLibrary("junit-api"))
    testImplementation(getLibrary("junit-params"))
    testImplementation(getLibrary("assertj-core"))
    testImplementation(getLibrary("assertj-guava"))
    testImplementation(getLibrary("mockito-core"))
    testImplementation(getLibrary("mockito-junit-jupiter"))
    testImplementation(getLibrary("cucumber-java"))
    testImplementation(getLibrary("cucumber-junit-platform-engine"))

    testRuntimeOnly(getLibrary("junit-engine"))
    testRuntimeOnly(getLibrary("junit-platform-launcher"))
    testRuntimeOnly(getLibrary("logback-classic"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
