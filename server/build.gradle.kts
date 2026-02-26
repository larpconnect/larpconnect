plugins {
    id("larpconnect.application")
}

dependencies {
    implementation(project(":parent"))
    implementation(project(":common"))
    implementation(project(":init"))
    implementation(project(":proto"))
    implementation(libs.vertx.core)
    implementation(libs.vertx.grpc)
    implementation(libs.vertx.web)
    implementation(libs.guice)
    compileOnly(libs.vertx.codegen)
    testCompileOnly(libs.vertx.codegen)
    testImplementation(libs.vertx.junit5)
    testImplementation(libs.vertx.web.client)
    testImplementation(libs.grpc.netty.shaded)
    testImplementation(libs.grpc.stub)
}

application {
    mainClass.set("com.larpconnect.njall.server.Main")
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    excludeFilter.set(file("config/spotbugs/exclude.xml"))
}
