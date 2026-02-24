plugins {
    id("larpconnect.library")
}

dependencies {
    implementation(project(":parent"))
    implementation(project(":proto"))
    api(libs.protobuf.java)
    compileOnly("io.vertx:vertx-codegen:5.0.8")
}
