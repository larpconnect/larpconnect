plugins {
    id("larpconnect.testing")
}

dependencies {
    testImplementation(project(":api"))
    testImplementation(project(":common"))
    testImplementation(project(":parent"))
    testImplementation(project(":proto"))
    testImplementation(project(":server"))
    testImplementation(project(":test"))
    testImplementation(project(":init"))
    testImplementation(libs.vertx.core)
    testImplementation(libs.vertx.grpc)
    testImplementation(libs.vertx.junit5)
    testCompileOnly(libs.vertx.codegen)
    testImplementation(libs.archunit.junit5)
}
