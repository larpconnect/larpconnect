plugins {
    id("larpconnect.library")
}

dependencies {
    api(project(":parent"))
    api(libs.junit.api)
    api(libs.junit.params)
    api(libs.assertj.core)
    api(libs.assertj.guava)
    api(libs.mockito.core)
    api(libs.mockito.junit.jupiter)
    api(libs.cucumber.java)
    api(libs.cucumber.junit.platform.engine)
}
