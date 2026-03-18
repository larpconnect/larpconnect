plugins {
    id("larpconnect.testing")
}

dependencies {
    testImplementation(libs.archunit.junit5)
    testImplementation(project(":api"))
    testImplementation(project(":common"))
    testImplementation(project(":init"))
    testImplementation(project(":proto"))
    testImplementation(project(":server"))
}
