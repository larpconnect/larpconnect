plugins {
    id("larpconnect.platform")
}

dependencies {
    constraints {
        api(project(":api"))
        api(project(":common"))
        api(project(":integration"))
        api(project(":parent"))
        api(project(":proto"))
        api(project(":server"))
        api(project(":test"))
    }
}
