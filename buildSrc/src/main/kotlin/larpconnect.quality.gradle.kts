import com.github.spotbugs.snom.SpotBugsTask
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    checkstyle
    jacoco
    id("com.github.spotbugs")
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
}

val libs = the<LibrariesForLibs>()
val rootConfigDir = rootProject.layout.projectDirectory.dir("config")

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    configFile = rootConfigDir.file("checkstyle/checkstyle.xml").asFile
    isIgnoreFailures = false
    maxWarnings = 0
}

spotbugs {
    toolVersion.set(libs.versions.spotbugs.get())
    excludeFilter.set(rootConfigDir.file("spotbugs/exclude.xml").asFile)
    ignoreFailures.set(false)
}

spotless {
    java {
        target("src/*/java/**/*.java")
        googleJavaFormat(libs.versions.google.java.format.get()) // Update to a version compatible with JDK 25
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.withType<JacocoReport>().configureEach {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.withType<JacocoCoverageVerification>().configureEach {
    dependsOn(tasks.withType<Test>())
    violationRules {
        rule {
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}

tasks.withType<Checkstyle>().configureEach {
    shouldRunAfter("spotlessApply")
    shouldRunAfter("spotlessJava")
}

tasks.withType<SpotBugsTask>().configureEach {
    shouldRunAfter("spotlessApply")
    shouldRunAfter("spotlessJava")
}

tasks.named("check") {
    dependsOn(tasks.withType<JacocoCoverageVerification>())
}

dependencies {
    add("errorprone", libs.errorprone.core)
    add("compileOnly", libs.errorprone.annotations)
    add("compileOnly", libs.jsr305)
    add("compileOnly", libs.spotbugs.annotations)
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone.disableWarningsInGeneratedCode.set(true)
    options.compilerArgs.addAll(listOf("-Werror", "-Xlint:all", "-Xlint:-processing"))
}
