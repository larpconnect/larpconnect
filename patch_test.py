import re

with open('server/src/test/java/com/larpconnect/njall/server/WebServerVerticleTest.java', 'r') as f:
    content = f.read()

# Remove all healthcheck methods that have been added multiple times and clean up status codes
content = re.sub(r'  @Test\s+void healthcheckEndpoint_succeeds\(VertxTestContext testContext\) \{\s+webClient\s+\.get\("/healthz"\)\s+\.send\(\)\s+\.onComplete\(\s+testContext\.succeeding\(\s+response ->\s+testContext\.verify\(\s+\(\) -> \{\s+assertThat\(response\.statusCode\(\)\)\.isEqualTo\(204\);\s+testContext\.completeNow\(\);\s+\}\)\)\);\s+\}\n', '', content)
content = re.sub(r'  @Test\s+void healthcheckEndpoint_succeeds\(VertxTestContext testContext\) \{\s+webClient\s+\.get\("/healthz"\)\s+\.send\(\)\s+\.onComplete\(\s+testContext\.succeeding\(\s+response ->\s+testContext\.verify\(\s+\(\) -> \{\s+assertThat\(response\.statusCode\(\)\)\.isEqualTo\(200\);\s+testContext\.completeNow\(\);\s+\}\)\)\);\s+\}\n', '', content)
content = re.sub(r'assertThat\(response\.statusCode\(\)\)\.isEqualTo\(204\);(.*subject)', r'assertThat(response.statusCode()).isEqualTo(200);\1', content, flags=re.DOTALL)
content = re.sub(r'assertThat\(response\.statusCode\(\)\)\.isEqualTo\(204\);(.*test)', r'assertThat(response.statusCode()).isEqualTo(200);\1', content, flags=re.DOTALL)

with open('server/src/test/java/com/larpconnect/njall/server/WebServerVerticleTest.java', 'w') as f:
    f.write(content)
