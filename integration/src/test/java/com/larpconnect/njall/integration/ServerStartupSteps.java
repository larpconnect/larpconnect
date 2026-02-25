package com.larpconnect.njall.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.init.VerticleService;
import com.larpconnect.njall.init.VerticleServices;
import com.larpconnect.njall.proto.Message;
import com.larpconnect.njall.server.ServerModule;
import com.larpconnect.njall.server.ServerVerticle;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.vertx.core.Vertx;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerStartupSteps {
  private final Logger logger = LoggerFactory.getLogger(ServerStartupSteps.class);

  private VerticleService lifecycle;
  private Vertx vertx;
  private final AtomicBoolean deploymentSuccess = new AtomicBoolean(false);
  private final VertxCaptor vertxCaptor = new VertxCaptor();

  @Given("the server is configured")
  public void the_server_is_configured() {
    lifecycle =
        VerticleServices.create(
            Arrays.asList(
                new ServerModule(),
                new AbstractModule() {
                  @Override
                  protected void configure() {
                    bind(VertxCaptor.class).toInstance(vertxCaptor);
                    requestInjection(vertxCaptor);
                  }
                }));
  }

  @When("I start the server")
  public void i_start_the_server() throws InterruptedException, TimeoutException {
    lifecycle.startAsync().awaitRunning(10, TimeUnit.SECONDS);
    vertx = vertxCaptor.getVertx();
    lifecycle.deploy(ServerVerticle.class);

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

  @After
  public void tearDown() throws TimeoutException {
    if (lifecycle != null && lifecycle.isRunning()) {
      lifecycle.stopAsync().awaitTerminated(10, TimeUnit.SECONDS);
    }
  }

  @Singleton
  static class VertxCaptor {
    private final AtomicReference<Vertx> ref = new AtomicReference<>();

    @Inject
    void setVertx(Vertx vertx) {
      ref.set(vertx);
    }

    Vertx getVertx() {
      return ref.get();
    }
  }
}
