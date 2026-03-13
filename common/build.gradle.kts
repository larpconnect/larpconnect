plugins {
    id("larpconnect.library")
}

dependencies {
    api(project(":proto"))
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.remove("-Werror")
}
