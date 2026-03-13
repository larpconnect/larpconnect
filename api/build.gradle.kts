plugins {
    id("larpconnect.library")
}

dependencies {
    api(project(":proto"))
    implementation(project(":common"))
}
