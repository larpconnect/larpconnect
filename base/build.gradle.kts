/*
 * base library module build script.
 * Bridge between events (Vert.x) and data (Hibernate).
 */

plugins {
    id("njall.java-library-conventions")
}

dependencies {
    implementation(platform(project(":parent")))

    // Grows out of :events and depends on :data
    implementation(project(":events"))
    implementation(project(":data"))
}
