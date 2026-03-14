plugins {
    id("larpconnect.library")
}

dependencies {
    api(project(":common"))
    api(project(":proto"))

    api(libs.hibernate.reactive.core)
    api(libs.vertx.pg.client)

    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    api(libs.hibernate.core)
    api(libs.vertx.junit5)
    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.engine)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.remove("-Werror")
}

tasks.withType<Test>().configureEach {
    environment("DOCKER_API_VERSION", "1.44")
}

tasks.named("jacocoTestCoverageVerification") {
    enabled = false
}
