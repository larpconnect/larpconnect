import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    `java-library`
    id("larpconnect.quality")
}

val libs = the<LibrariesForLibs>()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-parameters", "-Werror", "-Xlint:all", "-Xlint:-processing"))
}

dependencies {
    api(libs.slf4j.api)
    if (project.name != "parent") {
        api(project(":parent"))
    }

    compileOnly(libs.vertx.codegen)

    constraints {
        implementation(libs.commons.beanutils) {
            because("CVE-2025-48734")
        }
        implementation(libs.commons.lang3) {
            because("CVE-2025-48924")
        }
        implementation(libs.log4j.core) {
            because("CVE-2025-68161")
        }
        implementation(libs.jackson.core) {
            because("GHSA-72hv-8253-57qq")
        }
        implementation(libs.jackson.databind) {
            because("GHSA-72hv-8253-57qq")
        }
        implementation(libs.jackson.annotations) {
            because("GHSA-72hv-8253-57qq")
        }
    }

    testCompileOnly(libs.vertx.codegen)
}
