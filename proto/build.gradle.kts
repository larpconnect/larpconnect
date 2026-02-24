plugins {
    id("larpconnect.library")
    alias(libs.plugins.protobuf)
}

dependencies {
    implementation(project(":parent"))
    api(libs.protobuf.java)
    api(libs.spotbugs.annotations)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
}

// Configure Checkstyle to ignore generated files
tasks.withType<Checkstyle>().configureEach {
    exclude("**/MessageProto.java")
}

spotbugs {
    // Protobuf generated code often violates Spotbugs rules.
    ignoreFailures.set(true)
}

// Configure Jacoco to ignore generated files
tasks.withType<JacocoReport>().configureEach {
    classDirectories.setFrom(
        classDirectories.files.map {
            fileTree(it).matching {
                exclude("**/MessageProto.java")
            }
        },
    )
}
