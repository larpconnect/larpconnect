plugins {
    id("larpconnect.library")
}

dependencies {
    api(libs.junit.api)
    api(libs.junit.params)
    api(libs.junit.platform.suite)
    api(libs.assertj.core)
    api(libs.assertj.guava)
    api(libs.mockito.core)
    api(libs.mockito.junit.jupiter)
    api(libs.cucumber.java)
    api(libs.cucumber.junit.platform.engine)
    runtimeOnly(libs.junit.engine)
    runtimeOnly(libs.junit.platform.launcher)
}
