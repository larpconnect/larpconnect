plugins {
    id("larpconnect.library")
}

dependencies {
    implementation(project(":common"))
    implementation(libs.vertx.config)
}
