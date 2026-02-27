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
    constraints {
        implementation(libs.commons.beanutils) {
            because("CVE-2025-48734")
        }
        implementation(libs.log4j.core) {
            because("CVE-2025-68161")
        }
        implementation(libs.commons.lang3) {
            because("CVE-2025-48924")
        }
    }
}
