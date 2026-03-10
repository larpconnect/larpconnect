plugins {
    id("larpconnect.library")
}

dependencies {
    api(project(":proto"))
    implementation(project(":common"))
    implementation(libs.protobuf.java.util)
}
