import com.google.protobuf.gradle.GenerateProtoTask

plugins {
    id("larpconnect.library")
    alias(libs.plugins.protobuf)
}

dependencies {
    implementation(project(":parent"))
    api(libs.protobuf.java)
    implementation(libs.google.common.protos)
}

val goBinDir = layout.buildDirectory.dir("go-bin")
val openApiPluginPath = goBinDir.map { it.file("protoc-gen-openapi").asFile.absolutePath }

val installProtocGenOpenApi by tasks.registering(Exec::class) {
    outputs.file(openApiPluginPath)
    commandLine("go", "install", "github.com/google/gnostic/cmd/protoc-gen-openapi@latest")
    environment("GOBIN", goBinDir.get().asFile.absolutePath)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    plugins {
        create("openapi") {
            path = openApiPluginPath.get()
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.dependsOn(installProtocGenOpenApi)
            task.plugins {
                create("openapi") { }
            }
        }
    }
}

// Suppress failures in generated code
tasks.withType<Checkstyle>().configureEach {
    exclude("com/larpconnect/njall/proto/**")
}
tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    excludeFilter.set(file("config/spotbugs/exclude-generated.xml"))
}

sourceSets {
    main {
        resources {
            srcDir(
                tasks.generateProto.map {
                    layout.buildDirectory.dir("generated/sources/proto/main/openapi").get()
                },
            )
        }
    }
}
