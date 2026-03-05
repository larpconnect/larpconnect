plugins {
    id("larpconnect.library")
}

dependencies {
    compileOnly(libs.vertx.codegen)

    implementation(project(":common"))
    implementation(project(":proto"))
    implementation(libs.protobuf.java)
    implementation(libs.vertx.config)

    testCompileOnly(libs.vertx.codegen)

    testImplementation(libs.vertx.junit5)
}
