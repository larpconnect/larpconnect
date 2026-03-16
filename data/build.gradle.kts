plugins {
    id("larpconnect.library")
}

dependencies {
    implementation(libs.hibernate.core)
    implementation(libs.hibernate.reactive.core)
    implementation(libs.vertx.pg.client)
    api(project(":common"))
}
