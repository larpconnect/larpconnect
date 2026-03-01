import com.google.protobuf.gradle.GenerateProtoTask

plugins {
    id("larpconnect.library")
    alias(libs.plugins.protobuf)
}

dependencies {
    api(libs.protobuf.java)
    implementation(libs.google.common.protos)
}

val goBinDir = layout.buildDirectory.dir("go-bin")
val openApiPluginPath = goBinDir.map { it.file("protoc-gen-openapi").asFile.absolutePath }

val isUpdateOpenApi = gradle.startParameter.taskNames.any { it.contains("updateOpenApi") }

val installProtocGenOpenApi by tasks.registering(Exec::class) {
    outputs.file(openApiPluginPath)
    commandLine("go", "install", "github.com/google/gnostic/cmd/protoc-gen-openapi@latest")
    environment("GOBIN", goBinDir.get().asFile.absolutePath)
    onlyIf { isUpdateOpenApi }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    if (isUpdateOpenApi) {
        plugins {
            create("openapi") {
                path = openApiPluginPath.get()
            }
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            if (isUpdateOpenApi) {
                task.dependsOn(installProtocGenOpenApi)
                task.plugins {
                    create("openapi") { }
                }
            }
        }
    }
}

val updateOpenApi by tasks.registering(Copy::class) {
    dependsOn("generateProto")
    group = "build"
    description = "Copies generated OpenAPI specification to resources"

    val generatedDir = layout.buildDirectory.dir("generated/sources/proto/main/openapi")
    from(generatedDir) {
        include("openapi.yaml")
    }
    into(layout.projectDirectory.dir("src/main/resources"))
}

// Suppress failures in generated code
tasks.withType<Checkstyle>().configureEach {
    exclude("com/larpconnect/njall/proto/**")
}
tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    excludeFilter.set(file("config/spotbugs/exclude-generated.xml"))
}
