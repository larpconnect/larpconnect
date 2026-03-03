#!/bin/bash
# Remove repositories from java-common
sed -i '/repositories {/,/}/d' buildSrc/src/main/kotlin/larpconnect.java-common.gradle.kts

# Enforce in settings.gradle.kts
sed -i 's/dependencyResolutionManagement {/dependencyResolutionManagement {\n    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)/' settings.gradle.kts
