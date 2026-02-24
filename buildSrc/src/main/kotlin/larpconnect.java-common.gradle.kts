plugins {
    `java-library`
    id("larpconnect.quality")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

dependencies {
    api(getLibrary("slf4j-api"))
    constraints {
        implementation(getLibrary("commons-beanutils")) {
            because("CVE-2025-48734")
        }
        implementation(getLibrary("log4j-core")) {
            because("CVE-2025-68161")
        }
        implementation(getLibrary("commons-lang3")) {
            because("CVE-2025-48924")
        }
    }
}
