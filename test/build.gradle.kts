plugins {
    id("larpconnect.library")
}

dependencies {
    api(libs.assertj.core)
    api(libs.assertj.guava)
    api(libs.cucumber.java)
    api(libs.cucumber.junit.platform.engine)
    api(libs.junit.api)
    api(libs.junit.params)
    api(libs.junit.platform.suite)
    api(libs.mockito.core)
    api(libs.mockito.junit.jupiter)
    api(libs.vertx.junit5)
    api(libs.vertx.web.client)

    runtimeOnly(libs.junit.engine)
    runtimeOnly(libs.junit.platform.launcher)
}
