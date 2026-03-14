import os
import re

base_dir = "data/src/main/java/com/larpconnect/njall/data"

def fix_file(filename, replacements):
    path = os.path.join(base_dir, filename)
    with open(path, "r") as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(path, "w") as f:
        f.write(content)

# Address remaining Spotbugs in Main classes (if any, mostly we just need to ensure Spotbugs passes)
# The earlier spotbugs report had:
# M V EI2: com.larpconnect.njall.data.StudioRole.setId(StudioRoleId)
# M V EI: com.larpconnect.njall.data.StudioRole.getId()
# M V EI2: com.larpconnect.njall.data.ActorEndpoint.setId(ActorEndpointId)
# M V EI: com.larpconnect.njall.data.ActorEndpoint.getId()

# We already patched StudioRole and ActorEndpoint above. Let's make sure it stuck or if we need to adjust.
with open(os.path.join(base_dir, "StudioRole.java"), "r") as f:
    sr_content = f.read()
    if "return new StudioRoleId" not in sr_content:
        sr_content = sr_content.replace("return id;", "return new StudioRoleId(id.getStudioId(), id.getRoleId(), id.getIndividualId());")
        sr_content = sr_content.replace("this.id = id;", "this.id = new StudioRoleId(id.getStudioId(), id.getRoleId(), id.getIndividualId());")
        with open(os.path.join(base_dir, "StudioRole.java"), "w") as fw: fw.write(sr_content)

with open(os.path.join(base_dir, "ActorEndpoint.java"), "r") as f:
    ae_content = f.read()
    if "return new ActorEndpointId" not in ae_content:
        ae_content = ae_content.replace("return id;", "return new ActorEndpointId(id.getActorId(), id.getName());")
        ae_content = ae_content.replace("this.id = id;", "this.id = new ActorEndpointId(id.getActorId(), id.getName());")
        with open(os.path.join(base_dir, "ActorEndpoint.java"), "w") as fw: fw.write(ae_content)

# Handle EI_EXPOSE_REP by not providing getters/setters for internal OffsetDateTimes if they are mutable, or return copies. OffsetDateTime is immutable in Java, so spotbugs should not complain about it usually, but we'll see. Wait, SpotBugs might still be failing. Let's look at the errors.

# Checkstyle in test: Tests must not be named test.*()
test_path = "data/src/test/java/com/larpconnect/njall/data/PostgresIntegrationTest.java"
with open(test_path, "r") as f:
    tc = f.read()
# Replace testEntitySave_whenValid_shouldPersist with saveEntity_whenValid_shouldPersist
tc = tc.replace("void testEntitySave_whenValid_shouldPersist", "void saveEntity_whenValid_shouldPersist")

# For the Testcontainers error: "client version 1.32 is too old. Minimum supported API version is 1.44"
# We can tell Testcontainers to use a newer API version using DOCKER_API_VERSION env var
# or bypass it using System properties. We'll add this to the test.
import sys
tc = tc.replace("import org.junit.jupiter.api.BeforeAll;", "import org.junit.jupiter.api.BeforeAll;\nimport org.junit.jupiter.api.BeforeEach;")
tc = tc.replace("class PostgresIntegrationTest {", "class PostgresIntegrationTest {\n    static {\n        System.setProperty(\"DOCKER_API_VERSION\", \"1.44\");\n    }\n")

with open(test_path, "w") as f:
    f.write(tc)

print("Applied fixes 2")
