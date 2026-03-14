import re

with open('gradle/libs.versions.toml', 'r') as f:
    content = f.read()

versions = """hibernate-orm = "7.0.2.Final"
"""

libraries = """
hibernate-core = { module = "org.hibernate.orm:hibernate-core", version.ref = "hibernate-orm" }
"""

# add versions before [libraries]
content = re.sub(r'(\[libraries\])', versions + r'\n\1', content)

# add libraries at end of [libraries] block (before [plugins])
content = re.sub(r'(\[plugins\])', libraries + r'\n\1', content)

with open('gradle/libs.versions.toml', 'w') as f:
    f.write(content)
