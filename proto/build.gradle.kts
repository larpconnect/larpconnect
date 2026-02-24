plugins {
    id("larpconnect.library")
}

dependencies {
    implementation(project(":parent"))
    api(libs.protobuf.java)
}
