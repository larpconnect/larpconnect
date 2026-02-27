package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

final class ServerModuleTest {

  @Test
  void provideWebPort_usesLarpconnectConfig_whenPresent() {
    var module = new ServerModule();
    var config = new JsonObject().put("larpconnect", new JsonObject().put("web.port", 1234));

    var port = module.provideWebPort(config);

    assertThat(port).isEqualTo(1234);
  }

  @Test
  void provideWebPort_usesRootConfig_whenLarpconnectMissing() {
    var module = new ServerModule();
    var config = new JsonObject().put("web.port", 5678);

    var port = module.provideWebPort(config);

    assertThat(port).isEqualTo(5678);
  }

  @Test
  void provideOpenApiSpec_usesLarpconnectConfig_whenPresent() {
    var module = new ServerModule();
    var config =
        new JsonObject().put("larpconnect", new JsonObject().put("openapi.spec", "spec.yaml"));

    var spec = module.provideOpenApiSpec(config);

    assertThat(spec).isEqualTo("spec.yaml");
  }

  @Test
  void provideOpenApiSpec_usesRootConfig_whenLarpconnectMissing() {
    var module = new ServerModule();
    var config = new JsonObject().put("openapi.spec", "root-spec.yaml");

    var spec = module.provideOpenApiSpec(config);

    assertThat(spec).isEqualTo("root-spec.yaml");
  }
}
