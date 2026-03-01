plugins {
    id("larpconnect.library")
}

dependencies {
    api(project(":proto"))

    testImplementation(libs.vertx.junit5)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.remove("-Werror")
}
