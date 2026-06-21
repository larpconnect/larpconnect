import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    checkstyle
    jacoco
    id("com.diffplug.spotless")
    id("com.github.spotbugs")
    id("net.ltgt.errorprone")
}

repositories {
    mavenCentral()
}

// Java 25 LTS configuration
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

// Share compiler arguments from the `:parent` project if they exist
tasks.withType<JavaCompile>().configureEach {
    if (project.parent != null && project.parent!!.subprojects.any { it.name == "parent" }) {
        val parentProject = project.project(":parent")
        if (parentProject.state.executed) {
            val compilerArgs = parentProject.extra["compilerArgs"] as? List<*>
            if (compilerArgs != null) {
                options.compilerArgs.addAll(compilerArgs.filterIsInstance<String>())
            }
        }
    } else {
        // Fallback default compiler arguments if `:parent` is not evaluated/present yet
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror", "-Xlint:-processing", "-Xlint:-classfile"))
    }
}

// Spotless formatting configuration
configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    java {
        googleJavaFormat("1.27.0")
        target("src/**/*.java")
    }
}

// Checkstyle static analysis configuration
checkstyle {
    toolVersion = "10.21.2"
    configFile = file("${rootDir}/config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
}

// SpotBugs configuration
configure<com.github.spotbugs.snom.SpotBugsExtension> {
    toolVersion.set("4.9.7")
    ignoreFailures.set(false)
    effort.set(com.github.spotbugs.snom.Effort.MAX)
    reportLevel.set(com.github.spotbugs.snom.Confidence.DEFAULT)
    // Exclude generated code using the exclude filter
    excludeFilter.set(file("${rootDir}/config/spotbugs/exclude.xml"))
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    reports {
        create("xml") { required.set(true) }
        create("html") { required.set(true) }
    }
}

// ErrorProne configuration
dependencies {
    "errorprone"("com.google.errorprone:error_prone_core:2.36.0")
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone {
        isEnabled = true
        disableWarningsInGeneratedCode.set(true) // Exempt generated code
    }
}

// JaCoCo test coverage configuration
jacoco {
    toolVersion = "0.8.14"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                minimum = "0.85".toBigDecimal()
            }
        }
        rule {
            limit {
                counter = "BRANCH"
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}

// Enforce test coverage verification during build check
tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

// Define dependency constraints from Version Catalog
dependencies {
    constraints {
        // Enforce same versions across all subprojects
        implementation("org.apache.commons:commons-text:1.14.0")
    }
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter("6.1.0")
        }
    }
}

if (project.name != "test") {
    dependencies {
        "testImplementation"(project(":test"))
    }
}
