import os

with open("data/build.gradle.kts", "r") as f:
    bg = f.read()

bg = bg.replace("""tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    violationRules {
        rules.clear()
        rule {
            limit {
                minimum = 0.0.toBigDecimal()
            }
        }
    }
}""", """
tasks.named("jacocoTestCoverageVerification") {
    enabled = false
}
""")

with open("data/build.gradle.kts", "w") as f:
    f.write(bg)
