plugins {
    id("larpconnect.testing")
}

dependencies {
    testImplementation(project(":api"))
    testImplementation(project(":common"))
    testImplementation(project(":data"))
    testImplementation(project(":init"))
    testImplementation(project(":proto"))
    testImplementation(project(":server"))
    testImplementation(libs.archunit.junit5)
    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.assertj.core)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.hibernate.reactive.core)
}
