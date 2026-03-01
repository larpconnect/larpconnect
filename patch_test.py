with open('server/src/test/java/com/larpconnect/njall/server/WebServerVerticleTest.java', 'r') as f:
    lines = f.readlines()

test_method = """
  @Test
  void healthcheckEndpoint_succeeds(VertxTestContext testContext) {
    webClient
        .get("/healthz")
        .send()
        .onComplete(
            testContext.succeeding(
                response ->
                    testContext.verify(
                        () -> {
                          assertThat(response.statusCode()).isEqualTo(200);
                          testContext.completeNow();
                        })));
  }
"""

for i in range(len(lines) - 1, -1, -1):
    if lines[i].strip() == '}':
        lines.insert(i, test_method)
        break

with open('server/src/test/java/com/larpconnect/njall/server/WebServerVerticleTest.java', 'w') as f:
    f.writelines(lines)
