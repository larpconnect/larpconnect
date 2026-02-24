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
}
