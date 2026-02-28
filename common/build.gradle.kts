plugins {
    id("larpconnect.library")
}

dependencies {
    api(project(":parent"))
    api(project(":proto"))
    api(libs.vertx.core)
    api(libs.slf4j.api)
    api(libs.guava)

    testImplementation(project(":test"))
    testImplementation(libs.vertx.junit5)
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
    testImplementation(libs.assertj.core)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.remove("-Werror")
}
