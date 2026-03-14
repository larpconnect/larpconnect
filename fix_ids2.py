import os
import glob
import re

base_dir = "data/src/main/java/com/larpconnect/njall/data"

for path in glob.glob(os.path.join(base_dir, "*Id.java")):
    with open(path, "r") as f:
        content = f.read()

    # The previous regex left an extra `}` or malformed the end
    content = content.replace("  }", "}")

    with open(path, "w") as f:
        f.write(content)
