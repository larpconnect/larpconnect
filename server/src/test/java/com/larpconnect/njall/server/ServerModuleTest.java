package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.json.JsonObject;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

final class ServerModuleTest {
  @Test
  void provideLarpconnectConfig_usesPortEnvVar_whenPresentAndValid() {
    Function<String, String> envProvider = name -> "PORT".equals(name) ? "9999" : null;
    var module = new ServerBindingModule(envProvider);
    var config = new JsonObject().put("larpconnect", new JsonObject().put("web.port", 1234));

    var larpConfig = module.provideLarpconnectConfig(config);

    assertThat(larpConfig.getWebPort()).isEqualTo(9999);
    assertThat(larpConfig.getOpenapiSpec()).isEqualTo("openapi.yaml");
  }

  @Test
  void provideLarpconnectConfig_fallsBackToConfig_whenPortEnvVarInvalid() {
    Function<String, String> envProvider = name -> "PORT".equals(name) ? "invalid" : null;
    var module = new ServerBindingModule(envProvider);
    var config = new JsonObject().put("larpconnect", new JsonObject().put("web.port", 1234));

    var larpConfig = module.provideLarpconnectConfig(config);

    assertThat(larpConfig.getWebPort()).isEqualTo(1234);
  }

  @Test
  void provideLarpconnectConfig_fallsBackToConfig_whenPortEnvVarNull() {
    Function<String, String> envProvider = name -> null;
    var module = new ServerBindingModule(envProvider);
    var config = new JsonObject().put("larpconnect", new JsonObject().put("web.port", 1234));

    var larpConfig = module.provideLarpconnectConfig(config);

    assertThat(larpConfig.getWebPort()).isEqualTo(1234);
  }

  @Test
  void provideLarpconnectConfig_usesLarpconnectConfig_whenPresent() {
    var module = new ServerBindingModule();
    var config =
        new JsonObject()
            .put(
                "larpconnect",
                new JsonObject().put("web.port", 1234).put("openapi.spec", "spec.yaml"));

    var larpConfig = module.provideLarpconnectConfig(config);

    assertThat(larpConfig.getWebPort()).isEqualTo(1234);
    assertThat(larpConfig.getOpenapiSpec()).isEqualTo("spec.yaml");
  }

  @Test
  void provideLarpconnectConfig_usesRootConfig_whenLarpconnectMissing() {
    var module = new ServerBindingModule();
    var config = new JsonObject().put("web.port", 5678).put("openapi.spec", "root-spec.yaml");

    var larpConfig = module.provideLarpconnectConfig(config);

    assertThat(larpConfig.getWebPort()).isEqualTo(5678);
    assertThat(larpConfig.getOpenapiSpec()).isEqualTo("root-spec.yaml");
  }
}
