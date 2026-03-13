plugins {
    id("larpconnect.application")
}

dependencies {
    implementation(project(":api"))
    implementation(project(":common"))
    implementation(project(":init"))
    implementation(libs.vertx.config)
    implementation(libs.vertx.healthcheck)
    implementation(libs.vertx.openapi)
    implementation(libs.vertx.web)
    implementation(libs.vertx.web.openapi.router)
}

application {
    mainClass.set("com.larpconnect.njall.server.Main")
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    excludeFilter.set(file("config/spotbugs/exclude.xml"))
}
