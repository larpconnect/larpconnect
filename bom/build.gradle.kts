plugins {
    id("larpconnect.platform")
}

dependencies {
    constraints {
        api(project(":api"))
        api(project(":server"))
        api(project(":proto"))
        api(project(":parent"))
        api(project(":test"))
    }
}
