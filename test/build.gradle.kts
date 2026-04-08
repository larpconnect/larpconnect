plugins {
    id("larpconnect.library")
}

dependencies {
    api(libs.assertj.core)
    api(libs.assertj.guava)
    api(libs.cucumber.java)
    api(libs.junit.api)
    api(libs.junit.params)
    api(libs.junit.platform.suite)
    api(libs.mockito.core)
    api(libs.mockito.junit.jupiter)
    api(libs.testcontainers.junit.jupiter)
    api(libs.testcontainers.postgresql)
    api(libs.vertx.junit5)
    api(libs.vertx.web.client)

    constraints {
        api(libs.commons.compress) {
            because("Vulnerability in 1.24.0 pulled by testcontainers")
        }
    }
}
