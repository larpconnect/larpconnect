plugins {
    id("larpconnect.application")
}

dependencies {
    implementation(project(":parent"))
}

application {
    mainClass.set("com.larpconnect.server.Main")
}
