plugins {
    id("larpconnect.library")
}

dependencies {
    implementation(project(":common"))
    implementation(libs.hibernate.reactive.core)
    implementation(libs.vertx.pg.client)
    implementation(libs.guice)

    testImplementation(project(":test"))
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    enabled = false
}
