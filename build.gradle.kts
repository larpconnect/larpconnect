plugins {
    id("com.diffplug.spotless")
}

spotless {
    format("markdown") {
        target("**/*.md")
        targetExclude("**/build/**")
        prettier().config(mapOf("parser" to "markdown", "proseWrap" to "preserve"))
    }
}

tasks.withType<com.diffplug.gradle.spotless.SpotlessTask>().configureEach {
    if (name.contains("Markdown") && name.contains("Check")) {
        enabled = false
    }
}

tasks.named("spotlessCheck") {
    dependsOn(tasks.matching { it.name.contains("Markdown") && it.name.contains("Check") }.map { it.apply { enabled = false } })
}
