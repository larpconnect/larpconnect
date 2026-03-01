plugins {
    id("larpconnect.library")
}

dependencies {
    implementation(project(":proto"))
    implementation(project(":common"))
    implementation(libs.vertx.config)
    implementation(libs.protobuf.java)

    compileOnly(libs.vertx.codegen)

    testImplementation(libs.vertx.junit5)
    testCompileOnly(libs.vertx.codegen)
}
