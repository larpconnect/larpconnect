pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "larpconnect"

include("parent", "bom", "test", "server", "api", "proto", "integration")
