import re

with open("server/src/test/java/com/larpconnect/njall/server/WebServerVerticleTest.java", "r") as f:
    content = f.read()

new_content = re.sub(
    r'  @Test\n  void actualPort_returnsConfiguredPort\(Vertx vertx, VertxTestContext testContext\) {\n    assertThat\(verticle.actualPort\(\)\)\.isEqualTo\(port\);\n    var unstarted = new WebServerVerticle\(\);\n    assertThat\(unstarted\.actualPort\(\)\)\.isEqualTo\(8080\); // default\n    testContext\.completeNow\(\);\n  }',
    r'  @Test\n  void actualPort_returnsConfiguredPort(Vertx vertx, VertxTestContext testContext) {\n    testContext.verify(() -> {\n      assertThat(verticle.actualPort()).isEqualTo(port);\n      var unstarted = new WebServerVerticle();\n      assertThat(unstarted.actualPort()).isEqualTo(8080);\n      testContext.completeNow();\n    });\n  }',
    content
)

with open("server/src/test/java/com/larpconnect/njall/server/WebServerVerticleTest.java", "w") as f:
    f.write(new_content)
