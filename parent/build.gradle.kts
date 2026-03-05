plugins {
    id("larpconnect.library")
}

dependencies {
    api(libs.errorprone.annotations)
    api(libs.guava)
    api(libs.guice)
    api(libs.jsr305)
    api(libs.mug)
    api(libs.mug.errorprone)
    api(libs.spotbugs.annotations)
    api(libs.vertx.core)
}
