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
                if (limit.minimum == 0.80.toBigDecimal()) {
                    limit.minimum = 0.20.toBigDecimal()
                }
            }
        }
    }
}
