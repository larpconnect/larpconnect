plugins {
    id("larpconnect.library")
}

dependencies {
    api(project(":proto"))
    implementation(libs.protobuf.java.util)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.remove("-Werror")
}
