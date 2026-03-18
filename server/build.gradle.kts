plugins {
    id("larpconnect.application")
}

dependencies {
    implementation(libs.vertx.config)
    implementation(libs.vertx.healthcheck)
    implementation(libs.vertx.openapi)
    implementation(libs.vertx.web)
    implementation(libs.vertx.web.openapi.router)
    implementation(project(":api"))
    implementation(project(":common"))
    implementation(project(":data"))
    implementation(project(":init"))
}

application {
    mainClass.set("com.larpconnect.njall.server.Main")
}
