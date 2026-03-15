plugins {
    id("larpconnect.library")
}

dependencies {
    implementation(project(":parent"))
    implementation(libs.hibernate.core)
    implementation(libs.hibernate.reactive.core)
    implementation(libs.vertx.pg.client)

    testImplementation(project(":test"))
}
