plugins {
    id("larpconnect.library")
}

dependencies {
    implementation(project(":common"))
    implementation(libs.hibernate.reactive.core)
    implementation(libs.vertx.pg.client)
    implementation(libs.guice)

    testImplementation(project(":test"))
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    violationRules {
        rule {
            element = "CLASS"
            excludes =
                listOf(
                    "com.larpconnect.njall.data.DataModule",
                    "com.larpconnect.njall.data.DataBindingModule",
                    "com.larpconnect.njall.data.Greeting",
                )
        }
    }
}
