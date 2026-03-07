plugins {
    id("larpconnect.library")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":proto"))
    implementation(libs.protobuf.java)
    implementation(libs.vertx.config)
}
