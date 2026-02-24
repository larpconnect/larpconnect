plugins {
    id("larpconnect.library")
    alias(libs.plugins.protobuf)
}

dependencies {
    implementation(project(":parent"))
    api(libs.protobuf.java)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
}
