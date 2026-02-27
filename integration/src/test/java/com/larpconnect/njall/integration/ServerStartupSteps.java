package com.larpconnect.njall.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
import com.larpconnect.njall.init.VerticleService;
import com.larpconnect.njall.init.VerticleServices;
import com.larpconnect.njall.proto.Message;
import com.larpconnect.njall.server.MainVerticle;
import com.larpconnect.njall.server.ServerModule;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ServerStartupSteps {
  private final Logger logger = LoggerFactory.getLogger(ServerStartupSteps.class);

  private VerticleService lifecycle;
  private Vertx vertx;
  private final AtomicBoolean deploymentSuccess = new AtomicBoolean(false);
  private final ServerCaptor serverCaptor = new ServerCaptor();

  @Given("the server is configured")
  public void the_server_is_configured() {
    var overrideModule =
        Modules.override(new ServerModule())
            .with(
                new AbstractModule() {
                  @Override
                  protected void configure() {
                    bindConstant().annotatedWith(Names.named("web.port")).to(0);
                    bind(new TypeLiteral<Optional<Consumer<Integer>>>() {})
                        .toInstance(Optional.of(port -> serverCaptor.setPort(port)));
                  }
                });

    lifecycle =
        VerticleServices.create(
            ImmutableList.of(
                overrideModule,
                new AbstractModule() {
                  @Override
                  protected void configure() {
                    bind(ServerCaptor.class).toInstance(serverCaptor);
                    requestInjection(serverCaptor);
                  }
                }));
  }

  @When("I start the server")
  public void i_start_the_server() throws InterruptedException, TimeoutException {
    lifecycle.startAsync().awaitRunning(10, TimeUnit.SECONDS);
    vertx = serverCaptor.getVertx();
    lifecycle.deploy(MainVerticle.class);

    var start = System.currentTimeMillis();
    while (System.currentTimeMillis() - start < 5000) {
      if (vertx != null && !vertx.deploymentIDs().isEmpty()) {
        deploymentSuccess.set(true);
        break;
      }
      Thread.sleep(100);
    }
  }

  @Then("the server should be running")
  public void the_server_should_be_running() {
    assertThat(lifecycle.isRunning()).isTrue();
    assertThat(vertx).isNotNull();
  }

  @Then("the MainVerticle should be deployed")
  public void the_main_verticle_should_be_deployed() {
    assertThat(deploymentSuccess.get()).isTrue();
    assertThat(vertx.deploymentIDs()).isNotEmpty();
  }

  @Then("I should be able to send a Message on the event bus")
  public void i_should_be_able_to_send_a_message_on_the_event_bus() throws InterruptedException {
    var latch = new CountDownLatch(1);
    var success = new AtomicBoolean(false);

    var msg = Message.newBuilder().setMessageType("Ping").build();

    vertx
        .eventBus()
        .consumer(
            "test-address",
            (io.vertx.core.eventbus.Message<Message> m) -> {
              m.reply(m.body());
            });

    vertx
        .eventBus()
        .request("test-address", msg)
        .onSuccess(
            reply -> {
              if (reply.body() instanceof Message) {
                success.set(true);
              }
              latch.countDown();
            })
        .onFailure(
            err -> {
              logger.error("Message send failed", err);
              latch.countDown();
            });

    if (!latch.await(5, TimeUnit.SECONDS)) {
      logger.error("Timed out waiting for message reply");
    }
    assertThat(success.get()).isTrue();
  }

  @Then("I should receive a response from {string} containing:")
  public void i_should_receive_a_response_from_containing(String urlString, String expectedJson)
      throws InterruptedException {
    var latch = new CountDownLatch(1);
    var success = new AtomicBoolean(false);
    var uri = URI.create(urlString);

    var client = WebClient.create(vertx);
    // Use dynamic port
    int port = serverCaptor.getPort();
    client
        .get(port, uri.getHost(), uri.getPath())
        .send()
        .onSuccess(
            response -> {
              try {
                if (response.statusCode() == 200) {
                  var actual = response.bodyAsJsonObject();
                  var expected = new JsonObject(expectedJson);
                  // Verify expected fields exist in actual
                  for (String key : expected.fieldNames()) {
                    if (!actual.containsKey(key)
                        || !actual.getValue(key).equals(expected.getValue(key))) {
                      logger.error(
                          "Mismatch. Expected: {} but got: {}", expectedJson, actual.encode());
                      return;
                    }
                  }
                  success.set(true);
                } else {
                  logger.error("Unexpected status code: {}", response.statusCode());
                }
              } catch (Exception e) {
                logger.error("Failed to verify response", e);
              } finally {
                latch.countDown();
              }
            })
        .onFailure(
            err -> {
              logger.error("HTTP Request failed", err);
              latch.countDown();
            });

    if (!latch.await(5, TimeUnit.SECONDS)) {
      logger.error("Timed out waiting for HTTP response");
    }
    assertThat(success.get()).isTrue();
  }

  @After
  public void tearDown() throws TimeoutException {
    if (lifecycle != null && lifecycle.isRunning()) {
      lifecycle.stopAsync().awaitTerminated(10, TimeUnit.SECONDS);
    }
  }

  @Singleton
  static final class ServerCaptor {
    private final AtomicReference<Vertx> vertxRef = new AtomicReference<>();
    private final AtomicInteger portRef = new AtomicInteger();

    @Inject
    void init(Vertx vertx) {
      vertxRef.set(vertx);
    }

    void setPort(int port) {
      portRef.set(port);
    }

    Vertx getVertx() {
      return vertxRef.get();
    }

    int getPort() {
      return portRef.get();
    }
  }
}
