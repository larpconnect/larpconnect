plugins {
    id("larpconnect.testing")
}

dependencies {
    testImplementation(project(":api"))
    testImplementation(project(":common"))
    testImplementation(project(":proto"))
    testImplementation(project(":server"))
    testImplementation(project(":init"))
    testImplementation(libs.vertx.web.client)
    testImplementation(libs.vertx.junit5)
    testCompileOnly(libs.vertx.codegen)
    testImplementation(libs.archunit.junit5)
}
