package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

class ServerModuleTest {

  @Test
  void provideWebPort_usesLarpconnectConfig_whenPresent() {
    var config =
        new JsonObject()
            .put("larpconnect", new JsonObject().put("web.port", 9090))
            .put("web.port", 8080);

    Injector injector =
        Guice.createInjector(
            new ServerModule(), binder -> binder.bind(JsonObject.class).toInstance(config));

    Integer port = injector.getInstance(Key.get(Integer.class, Names.named("web.port")));
    assertThat(port).isEqualTo(9090);
  }

  @Test
  void provideWebPort_usesRootConfig_whenLarpconnectMissing() {
    var config = new JsonObject().put("web.port", 7070);

    Injector injector =
        Guice.createInjector(
            new ServerModule(), binder -> binder.bind(JsonObject.class).toInstance(config));

    Integer port = injector.getInstance(Key.get(Integer.class, Names.named("web.port")));
    assertThat(port).isEqualTo(7070);
  }

  @Test
  void provideWebPort_usesRootConfig_whenLarpconnectPresentButEmpty() {
    var config = new JsonObject().put("larpconnect", new JsonObject()).put("web.port", 7070);

    Injector injector =
        Guice.createInjector(
            new ServerModule(), binder -> binder.bind(JsonObject.class).toInstance(config));

    Integer port = injector.getInstance(Key.get(Integer.class, Names.named("web.port")));
    assertThat(port).isEqualTo(7070);
  }

  @Test
  void provideOpenApiSpec_usesLarpconnectConfig_whenPresent() {
    var config =
        new JsonObject()
            .put("larpconnect", new JsonObject().put("openapi.spec", "custom.yaml"))
            .put("openapi.spec", "default.yaml");

    Injector injector =
        Guice.createInjector(
            new ServerModule(), binder -> binder.bind(JsonObject.class).toInstance(config));

    String spec = injector.getInstance(Key.get(String.class, Names.named("openapi.spec")));
    assertThat(spec).isEqualTo("custom.yaml");
  }

  @Test
  void provideOpenApiSpec_usesRootConfig_whenLarpconnectMissing() {
    var config = new JsonObject().put("openapi.spec", "root.yaml");

    Injector injector =
        Guice.createInjector(
            new ServerModule(), binder -> binder.bind(JsonObject.class).toInstance(config));

    String spec = injector.getInstance(Key.get(String.class, Names.named("openapi.spec")));
    assertThat(spec).isEqualTo("root.yaml");
  }

  @Test
  void provideOpenApiSpec_usesRootConfig_whenLarpconnectPresentButEmpty() {
    var config =
        new JsonObject().put("larpconnect", new JsonObject()).put("openapi.spec", "root.yaml");

    Injector injector =
        Guice.createInjector(
            new ServerModule(), binder -> binder.bind(JsonObject.class).toInstance(config));

    String spec = injector.getInstance(Key.get(String.class, Names.named("openapi.spec")));
    assertThat(spec).isEqualTo("root.yaml");
  }
}
