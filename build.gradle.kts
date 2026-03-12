plugins {
    id("com.diffplug.spotless")
}

spotless {
    flexmark {
        target("**/*.md")
        targetExclude("**/build/**")
        flexmark().emulationProfile("GITHUB_DOC").extensions("YamlFrontMatter")
    }
}

