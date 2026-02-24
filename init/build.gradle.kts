plugins {
    id("larpconnect.library")
}

dependencies {
    implementation(project(":parent"))
    compileOnly("io.vertx:vertx-codegen:5.0.8")
}
