plugins {
    id("larpconnect.library")
}

dependencies {
    api(project(":proto"))
    implementation(libs.vertx.core)
    implementation(libs.protobuf.java.util)
    compileOnly(libs.vertx.codegen)
}
