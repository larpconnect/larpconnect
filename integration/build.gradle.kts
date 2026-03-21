plugins {
    id("larpconnect.testing")
}

dependencies {
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.commons.compress)
    testImplementation(libs.hibernate.core)
    testImplementation(libs.mutiny.core)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(project(":api"))
    testImplementation(project(":common"))
    testImplementation(project(":data"))
    testImplementation(project(":init"))
    testImplementation(project(":proto"))
    testImplementation(project(":server"))
}

tasks.withType<Test> {
    systemProperty("testcontainers.ryuk.disabled", "true")
}
