import net.ltgt.gradle.errorprone.errorprone

plugins {
    checkstyle
    id("com.github.spotbugs")
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
}

val rootConfigDir = rootProject.layout.projectDirectory.dir("config")

checkstyle {
    toolVersion = getVersion("checkstyle")
    configFile = rootConfigDir.file("checkstyle/checkstyle.xml").asFile
    isIgnoreFailures = false
    maxWarnings = 0
}

spotbugs {
    toolVersion.set(getVersion("spotbugs"))
    excludeFilter.set(rootConfigDir.file("spotbugs/exclude.xml").asFile)
    ignoreFailures.set(false)
}

spotless {
    java {
        target("src/*/java/**/*.java")
        googleJavaFormat()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}

dependencies {
    add("errorprone", getLibrary("errorprone-annotations"))
    add("compileOnly", getLibrary("jsr305"))
    add("compileOnly", getLibrary("spotbugs-annotations"))
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone.disableWarningsInGeneratedCode.set(true)
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Werror", "-Xlint:all", "-Xlint:-processing", "-parameters"))
}
