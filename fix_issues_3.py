import os

test_path = "data/src/test/java/com/larpconnect/njall/data/PostgresIntegrationTest.java"

with open(test_path, "r") as f:
    tc = f.read()

# Since our package has `com.larpconnect.njall.data.System`, `System.setProperty` fails
tc = tc.replace("System.setProperty(", "java.lang.System.setProperty(")

with open(test_path, "w") as f:
    f.write(tc)

print("Applied fixes 3")
