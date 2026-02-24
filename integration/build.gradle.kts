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
    testImplementation(getLibrary("archunit-junit5"))
}
