/*
 * queue library module build script.
 */

plugins {
    id("njall.java-library-conventions")
}

dependencies {
    implementation(platform(project(":parent")))

    // Grows out of :events
    implementation(project(":events"))

    // AMQP Client dependency
    implementation(libs.rabbitmq.amqp)
}
