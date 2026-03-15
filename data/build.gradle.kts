plugins {
    id("larpconnect.library")
}

dependencies {
    implementation(libs.hibernate.core)
    implementation(libs.hibernate.reactive.core)
    implementation(libs.vertx.pg.client)
    api(libs.mutiny.core)
    api(libs.mutiny.vertx.core)
    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.assertj.core)
    api(project(":common"))
}
