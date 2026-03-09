import com.google.protobuf.gradle.GenerateProtoTask

plugins {
    id("larpconnect.library")
    alias(libs.plugins.protobuf)
}

dependencies {
    api(libs.protobuf.java)
    implementation(libs.google.common.protos)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
}

// Suppress failures in generated code
tasks.withType<Checkstyle>().configureEach {
    exclude("com/larpconnect/njall/proto/**")
}
tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    excludeFilter.set(file("config/spotbugs/exclude-generated.xml"))
}
