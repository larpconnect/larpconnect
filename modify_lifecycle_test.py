with open("init/src/test/java/com/larpconnect/njall/init/VerticleLifecycleTest.java", "r") as f:
    content = f.read()

import re

# Add imports for assertCode and Provider
content = content.replace(
    "import static org.assertj.core.api.Assertions.assertThatThrownBy;",
    "import static org.assertj.core.api.Assertions.assertThatCode;\nimport static org.assertj.core.api.Assertions.assertThatThrownBy;"
)
content = content.replace("import io.vertx.core.Vertx;", "import io.vertx.core.Vertx;\nimport jakarta.inject.Provider;")

# Change the constructor calls to use Provider mock
def mock_provider(mock_var):
    return f"() -> {mock_var}"

content = content.replace("new VerticleLifecycle(mockVertx, mockSetupService, mockInjector)", "new VerticleLifecycle(() -> mockVertx, () -> mockSetupService, mockInjector)")

# Update the names
content = content.replace("shutDown_closeFails_logsError", "shutDown_closeFails_doesNotThrowException")
content = content.replace("shutDown_interrupted_logsWarning", "shutDown_interrupted_doesNotThrowException")

# Add assertions for shutDown
content = re.sub(
    r'lifecycle\.shutDown\(\);\s*\}',
    '''assertThatCode(lifecycle::shutDown).doesNotThrowAnyException();
  }''',
    content
)

# And for the interrupted one where it's inside a try block
content = re.sub(
    r'lifecycle\.shutDown\(\);\s*assertThat\(Thread\.currentThread\(\)\.isInterrupted\(\)\)\.isTrue\(\);',
    '''assertThatCode(lifecycle::shutDown).doesNotThrowAnyException();
      assertThat(Thread.currentThread().isInterrupted()).isTrue();''',
    content
)

with open("init/src/test/java/com/larpconnect/njall/init/VerticleLifecycleTest.java", "w") as f:
    f.write(content)
