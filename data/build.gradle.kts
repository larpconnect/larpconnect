plugins {
    id("larpconnect.library")
}

dependencies {
    api(project(":common"))

    implementation(libs.hibernate.core)
    implementation(libs.hibernate.reactive.core)
    implementation(libs.vertx.pg.client)
}
