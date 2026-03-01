import re

with open('server/src/main/java/com/larpconnect/njall/server/WebServerVerticle.java', 'r') as f:
    content = f.read()

imports = """import io.vertx.ext.web.openapi.router.RouterBuilder;
import io.vertx.ext.web.healthchecks.HealthCheckHandler;
import io.vertx.openapi.contract.OpenAPIContract;"""

content = content.replace("import io.vertx.ext.web.openapi.router.RouterBuilder;\nimport io.vertx.openapi.contract.OpenAPIContract;", imports)

router_setup = """              var router = builder.createRouter();
              var hc = HealthCheckHandler.create(vertx);
              router.get("/healthz").handler(hc);
              router.get("/.well-known/webfinger").handler(this::handleWebfinger);"""

content = content.replace('              var router = builder.createRouter();\n              router.get("/.well-known/webfinger").handler(this::handleWebfinger);', router_setup)

with open('server/src/main/java/com/larpconnect/njall/server/WebServerVerticle.java', 'w') as f:
    f.write(content)
