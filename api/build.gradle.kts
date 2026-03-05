plugins {
    id("larpconnect.library")
}

dependencies {
    api(project(":proto"))
    implementation(project(":common"))
    implementation(libs.vertx.core)
    implementation(libs.protobuf.java.util)
    implementation(libs.guice)
    compileOnly(libs.vertx.codegen)
}
