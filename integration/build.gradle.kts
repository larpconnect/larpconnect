plugins {
    id("larpconnect.testing")
}

dependencies {
    testImplementation(project(":api"))
    testImplementation(project(":common"))
    testImplementation(project(":init"))
    testImplementation(project(":proto"))
    testImplementation(project(":server"))
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.protobuf.java.util)
    testImplementation(libs.vertx.web.client)
}
