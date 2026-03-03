with open('settings.gradle.kts', 'r') as f:
    content = f.read()

import re
content = re.sub(r'    repositoriesMode.set\(RepositoriesMode.FAIL_ON_PROJECT_REPOS\)\n', '', content)

content = content.replace('dependencyResolutionManagement {\n', 'dependencyResolutionManagement {\n    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)\n')

with open('settings.gradle.kts', 'w') as f:
    f.write(content)
