plugins {
    id("larpconnect.testing")
}

dependencies {
    testCompileOnly(libs.vertx.codegen)

    testImplementation(project(":api"))
    testImplementation(project(":common"))
    testImplementation(project(":init"))
    testImplementation(project(":proto"))
    testImplementation(project(":server"))
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.vertx.junit5)
    testImplementation(libs.vertx.web.client)
}
dependencies {
    implementation(project(":api"))
}
