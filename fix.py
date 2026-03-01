import re

with open("build.gradle.kts", "r") as f:
    content = f.read()

content = content.replace('    format("markdown") {\n        target("**/*.md")\n        targetExclude("**/build/**")\n        prettier().config(mapOf("parser" to "markdown", "proseWrap" to "preserve"))\n    }', '    flexmark {\n        target("**/*.md")\n        targetExclude("**/build/**")\n        flexmark().emulationProfile("GITHUB_DOC")\n    }')

content = content.replace('if (name.contains("Markdown") && name.contains("Check")) {', 'if ((name.contains("Markdown") || name.contains("Flexmark")) && name.contains("Check")) {')
content = content.replace('tasks.matching { it.name.contains("Markdown") && it.name.contains("Check") }', 'tasks.matching { (it.name.contains("Markdown") || it.name.contains("Flexmark")) && it.name.contains("Check") }')

with open("build.gradle.kts", "w") as f:
    f.write(content)
