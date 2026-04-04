plugins {
    id("larpconnect.library")
}

dependencies {
    api(project(":common"))

    implementation(libs.hibernate.core)
    implementation(libs.hibernate.reactive.core)
    implementation(libs.mutiny.core)
    implementation(libs.mutiny.vertx.core)
    implementation(libs.mutiny.vertx.pg.client)
    implementation(libs.vertx.pg.client)

    testImplementation(libs.commons.compress)
}
