package com.larpconnect.njall.api.studios.inbox;

import static org.apache.pekko.actor.typed.javadsl.AskPattern.ask;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class InboxModuleTest {

  private static final int ROUTEE_POOL_SIZE = 16;

  private ActorSystem<Void> system;

  @BeforeEach
  void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "inbox-module-test");
  }

  @AfterEach
  void tearDown() throws Exception {
    system.terminate();
    system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  @Test
  @DisplayName(
      "provideInboxActor spawns a supervised router pool that restarts failed routees and serves"
          + " commands")
  void provideInboxActor_routeeFailureAndDelivery_restartsAndRoutesCommands() throws Exception {
    var mapper = new ObjectMapper();
    var initialReady = new CountDownLatch(ROUTEE_POOL_SIZE);
    var restarted = new CountDownLatch(1);
    var setupCount = new AtomicInteger();
    InboxActorFactory factory =
        () ->
            Behaviors.setup(
                _ -> {
                  if (setupCount.incrementAndGet() <= ROUTEE_POOL_SIZE) {
                    initialReady.countDown();
                  } else {
                    restarted.countDown();
                  }
                  return Behaviors.receiveMessage(
                      msg -> {
                        if (msg instanceof InboxCommand.DeliverActivity cmd) {
                          if ("boom".equals(cmd.actorIdentifier())) {
                            throw new IllegalStateException("boom");
                          }
                          cmd.replyTo().tell(new InboxActorResponse.Success());
                        }
                        return Behaviors.same();
                      });
                });

    var actorRef = new InboxModule().provideInboxActor(system, factory, Props.empty());
    assertThat(actorRef.path().name()).isEqualTo("inboxActor");
    assertThat(initialReady.await(5, TimeUnit.SECONDS)).isTrue();

    TestInbox<InboxActorResponse> ignoredReply = TestInbox.create();
    actorRef.tell(deliverCommand(mapper, "boom", ignoredReply.getRef()));
    assertThat(restarted.await(5, TimeUnit.SECONDS)).isTrue();

    for (int i = 0; i < ROUTEE_POOL_SIZE; i++) {
      var response =
          ask(
                  actorRef,
                  (ActorRef<InboxActorResponse> replyTo) ->
                      deliverCommand(mapper, "valkyrie", replyTo),
                  Duration.ofSeconds(5),
                  system.scheduler())
              .toCompletableFuture()
              .get(5, TimeUnit.SECONDS);
      assertThat(response).isInstanceOf(InboxActorResponse.Success.class);
    }
  }

  private static InboxCommand.DeliverActivity deliverCommand(
      ObjectMapper mapper, String actorIdentifier, ActorRef<InboxActorResponse> replyTo) {
    return new InboxCommand.DeliverActivity(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "valiant",
        actorIdentifier,
        mapper.createObjectNode(),
        Optional.empty(),
        replyTo,
        Optional.empty());
  }
}
