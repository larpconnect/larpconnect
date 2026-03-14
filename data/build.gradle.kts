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
        rules.forEach { rule ->
            rule.limits.forEach { limit ->
                limit.minimum = 0.00.toBigDecimal()
            }
        }
        rule {
            limit {
                minimum = 0.00.toBigDecimal()
            }
        }
    }
}
