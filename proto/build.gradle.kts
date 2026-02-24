plugins {
    id("larpconnect.library")
    id("com.google.protobuf") version "4.28.2"
}

dependencies {
    implementation(project(":parent"))
    api(libs.protobuf.java)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.28.2"
    }
}
