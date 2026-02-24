plugins {
    id("larpconnect.library")
}

dependencies {
    api(libs.vertx.core)
    api(libs.guava)
    api(libs.guice)
    api(libs.mug)
    api(libs.mug.errorprone)
    api(libs.jsr305)
    api(libs.errorprone.annotations)
    api(libs.spotbugs.annotations)
}
