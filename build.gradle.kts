plugins {
    id("com.diffplug.spotless")
}

spotless {
    flexmark {
        target("**/*.md")
        targetExclude("**/build/**")
        flexmark().emulationProfile("GITHUB_DOC")
    }
}

tasks.withType<com.diffplug.gradle.spotless.SpotlessTask>().configureEach {
    if ((name.contains("Markdown") || name.contains("Flexmark")) && name.contains("Check")) {
        enabled = false
    }
}

tasks.named("spotlessCheck") {
    dependsOn(tasks.matching { (it.name.contains("Markdown") || it.name.contains("Flexmark")) && it.name.contains("Check") }.map { it.apply { enabled = false } })
}
