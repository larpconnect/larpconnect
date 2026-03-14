import os
import glob
import re

base_dir = "data/src/main/java/com/larpconnect/njall/data"

for path in glob.glob(os.path.join(base_dir, "*Id.java")):
    with open(path, "r") as f:
        content = f.read()

    # Remove equals and hashCode methods
    content = re.sub(r'@Override\s+public boolean equals\(Object o\).*?}\s+}', '}', content, flags=re.DOTALL)
    content = re.sub(r'@Override\s+public int hashCode\(\).*?}', '', content, flags=re.DOTALL)

    with open(path, "w") as f:
        f.write(content)
