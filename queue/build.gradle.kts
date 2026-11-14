/*
 * queue library module build script.
 */

plugins {
    id("njall.java-library-conventions")
}

dependencies {
    api(platform(project(":parent")))

    api(project(":common"))
    api(libs.guice)
    api(libs.caffeine)
    api(libs.metrics.healthchecks)
    api(libs.rabbitmq.amqp)
    api(libs.slf4j.api)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.datatype.jdk8)
    implementation(libs.jackson.datatype.guava)
    compileOnly(libs.checker.qual)
    testCompileOnly(libs.checker.qual)

    testImplementation(project(":test"))
}
