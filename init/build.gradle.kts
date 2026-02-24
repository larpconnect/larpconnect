plugins {
    id("larpconnect.library")
}

dependencies {
    api(project(":parent"))
    implementation(project(":proto"))
    implementation(project(":common"))
    implementation(libs.vertx.core)
    implementation(libs.guice)
    implementation(libs.guava)
    implementation(libs.protobuf.java)

    compileOnly(libs.vertx.codegen)

    testImplementation(libs.vertx.junit5)
    testCompileOnly(libs.vertx.codegen)
}
