package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.json.JsonObject;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

final class ServerModuleTest {
  @Test
  void provideLarpConnectConfig_usesPortEnvVar_whenPresentAndValid() {
    Function<String, String> envProvider = name -> "PORT".equals(name) ? "9999" : null;
    var module = new ServerBindingModule(envProvider);
    var config = new JsonObject().put("larpconnect", new JsonObject().put("webPort", 1234));

    var larpConfig = module.provideLarpConnectConfig(config);

    assertThat(larpConfig.getWebPort()).isEqualTo(9999);
    assertThat(larpConfig.getOpenapiSpec()).isEqualTo("openapi.yaml");
  }

  @Test
  void provideLarpConnectConfig_fallsBackToConfig_whenPortEnvVarInvalid() {
    Function<String, String> envProvider = name -> "PORT".equals(name) ? "invalid" : null;
    var module = new ServerBindingModule(envProvider);
    var config = new JsonObject().put("larpconnect", new JsonObject().put("webPort", 1234));

    var larpConfig = module.provideLarpConnectConfig(config);

    assertThat(larpConfig.getWebPort()).isEqualTo(1234);
  }

  @Test
  void provideLarpConnectConfig_fallsBackToConfig_whenPortEnvVarNull() {
    Function<String, String> envProvider = name -> null;
    var module = new ServerBindingModule(envProvider);
    var config = new JsonObject().put("larpconnect", new JsonObject().put("webPort", 1234));

    var larpConfig = module.provideLarpConnectConfig(config);

    assertThat(larpConfig.getWebPort()).isEqualTo(1234);
  }

  @Test
  void provideLarpConnectConfig_usesLarpConnectConfig_whenPresent() {
    var module = new ServerBindingModule();
    var config =
        new JsonObject()
            .put(
                "larpconnect",
                new JsonObject().put("webPort", 1234).put("openapiSpec", "spec.yaml"));

    var larpConfig = module.provideLarpConnectConfig(config);

    assertThat(larpConfig.getWebPort()).isEqualTo(1234);
    assertThat(larpConfig.getOpenapiSpec()).isEqualTo("spec.yaml");
  }

  @Test
  void provideLarpConnectConfig_usesRootConfig_whenLarpconnectMissing() {
    var module = new ServerBindingModule();
    var config = new JsonObject().put("webPort", 5678).put("openapiSpec", "root-spec.yaml");

    var larpConfig = module.provideLarpConnectConfig(config);

    assertThat(larpConfig.getWebPort()).isEqualTo(5678);
    assertThat(larpConfig.getOpenapiSpec()).isEqualTo("root-spec.yaml");
  }
}
