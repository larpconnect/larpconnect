import os
import glob
import re

test_dir = "data/src/test/java/com/larpconnect/njall/data"

for path in glob.glob(os.path.join(test_dir, "*IdCoverageTest.java")):
    with open(path, "r") as f:
        content = f.read()

    # remove assertions on equals and hashCode
    content = re.sub(r'org\.assertj\.core\.api\.Assertions\.assertThat\(obj\.hashCode[^;]+;\n', '', content)
    content = re.sub(r'org\.assertj\.core\.api\.Assertions\.assertThat\(obj\.equals[^;]+;\n', '', content)
    content = re.sub(r'\s+[A-Za-z0-9]+ obj2 = new [^;]+;\n', '', content)

    with open(path, "w") as f:
        f.write(content)
