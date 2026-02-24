package com.larpconnect.njall.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.larpconnect.njall.init.VerticleService;
import com.larpconnect.njall.init.VerticleServices;
import com.larpconnect.njall.proto.Message;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.vertx.core.Vertx;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerStartupSteps {
  private static final Logger logger = LoggerFactory.getLogger(ServerStartupSteps.class);

  private VerticleService lifecycle;
  private Vertx vertx;
  private final AtomicBoolean deploymentSuccess = new AtomicBoolean(false);

  @Given("the server is configured")
  public void the_server_is_configured() {
    lifecycle = VerticleServices.create(Collections.emptyList());
  }

  @When("I start the server")
  public void i_start_the_server() throws InterruptedException, TimeoutException {
    lifecycle.startAsync().awaitRunning(10, TimeUnit.SECONDS);
    vertx = lifecycle.getVertx();

    CountDownLatch latch = new CountDownLatch(1);
    // Use string name since class is package-private
    vertx
        .deployVerticle("guice:com.larpconnect.njall.server.MainVerticle")
        .onSuccess(
            id -> {
              deploymentSuccess.set(true);
              latch.countDown();
            })
        .onFailure(
            err -> {
              logger.error("Deployment failed", err);
              latch.countDown();
            });

    if (!latch.await(5, TimeUnit.SECONDS)) {
      logger.error("Timed out waiting for verticle deployment");
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
    CountDownLatch latch = new CountDownLatch(1);
    AtomicBoolean success = new AtomicBoolean(false);

    Message msg = Message.newBuilder().setMessageType("Ping").build();

    // Register a consumer to reply
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
}
