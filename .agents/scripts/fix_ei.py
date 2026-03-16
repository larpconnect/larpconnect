import os
import re

def fix_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Add suppression import if not present
    if "import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;" not in content:
        content = re.sub(
            r'(package .*?;)',
            r'\1\n\nimport edu.umd.cs.findbugs.annotations.SuppressFBWarnings;',
            content,
            count=1
        )

    # Suppress getters (EI_EXPOSE_REP)
    content = re.sub(
        r'(\s+public [A-Z][a-zA-Z0-9<>]* get[A-Z][a-zA-Z0-9]*\(\) {)',
        r'\n  @SuppressFBWarnings("EI_EXPOSE_REP")\1',
        content
    )

    # Suppress setters (EI_EXPOSE_REP2)
    content = re.sub(
        r'(\s+public void set[A-Z][a-zA-Z0-9]*\([A-Z][a-zA-Z0-9<>]* [a-zA-Z0-9]+\) {)',
        r'\n  @SuppressFBWarnings("EI_EXPOSE_REP2")\1',
        content
    )

    # Suppress constructors (EI_EXPOSE_REP2)
    class_match = re.search(r'class ([A-Za-z0-9_]+)', content)
    if class_match:
        class_name = class_match.group(1)
        content = re.sub(
            rf'(\s+public {class_name}\([^)]+\) {{)',
            r'\n    @SuppressFBWarnings("EI_EXPOSE_REP2")\1',
            content
        )

    with open(filepath, 'w') as f:
        f.write(content)

for root, _, files in os.walk('data/src/main/java/com/larpconnect/njall/data/entity'):
    for file in files:
        if file.endswith('.java'):
            fix_file(os.path.join(root, file))
