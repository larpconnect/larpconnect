plugins {
    id("larpconnect.application")
}

dependencies {
    implementation(project(":parent"))
    implementation(project(":init"))
    compileOnly("io.vertx:vertx-codegen:5.0.8")
}

application {
    mainClass.set("com.larpconnect.server.Main")
}
