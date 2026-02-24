plugins {
    id("larpconnect.application")
}

dependencies {
    implementation(project(":parent"))
    implementation(project(":init"))
    implementation(libs.vertx.core)
    implementation(libs.guice)
    compileOnly(libs.vertx.codegen)
}

application {
    mainClass.set("com.larpconnect.njall.server.Main")
}
