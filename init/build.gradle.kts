plugins {
    id("larpconnect.library")
}

dependencies {
    implementation(libs.vertx.config)
    implementation(project(":common"))
}
