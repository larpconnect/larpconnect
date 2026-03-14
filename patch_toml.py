import re

with open('gradle/libs.versions.toml', 'r') as f:
    content = f.read()

versions = """testcontainers = "1.21.3"
hibernate-reactive = "4.0.0.Beta1"
"""

libraries = """
hibernate-reactive-core = { module = "org.hibernate.reactive:hibernate-reactive-core", version.ref = "hibernate-reactive" }
vertx-pg-client = { module = "io.vertx:vertx-pg-client", version.ref = "vertx" }
testcontainers = { module = "org.testcontainers:testcontainers", version.ref = "testcontainers" }
testcontainers-postgresql = { module = "org.testcontainers:postgresql", version.ref = "testcontainers" }
testcontainers-junit-jupiter = { module = "org.testcontainers:junit-jupiter", version.ref = "testcontainers" }
"""

# add versions before [libraries]
content = re.sub(r'(\[libraries\])', versions + r'\n\1', content)

# add libraries at end of [libraries] block (before [plugins])
content = re.sub(r'(\[plugins\])', libraries + r'\n\1', content)

with open('gradle/libs.versions.toml', 'w') as f:
    f.write(content)
