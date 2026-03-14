import re

with open('data/build.gradle.kts', 'r') as f:
    content = f.read()

deps = """
    api(libs.hibernate.core)
    api(libs.vertx.junit5)
    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.engine)
"""

content = content.replace('testImplementation(libs.testcontainers.junit.jupiter)', 'testImplementation(libs.testcontainers.junit.jupiter)' + deps)

with open('data/build.gradle.kts', 'w') as f:
    f.write(content)
