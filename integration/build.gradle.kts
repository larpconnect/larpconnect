plugins {
    id("larpconnect.testing")
}

dependencies {
    testImplementation(libs.commons.compress)
    testImplementation(libs.archunit.junit5)
    testImplementation(project(":api"))
    testImplementation(project(":common"))
    testImplementation(project(":init"))
    testImplementation(project(":data"))
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)

    testImplementation(project(":proto"))
    testImplementation(project(":server"))
}

tasks.withType<Test> {
    systemProperty("testcontainers.ryuk.disabled", "true")
}
