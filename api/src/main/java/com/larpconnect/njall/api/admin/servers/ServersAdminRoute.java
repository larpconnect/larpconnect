package com.larpconnect.njall.api.admin.servers;

import static org.apache.pekko.actor.typed.javadsl.AskPattern.ask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.http.javadsl.marshallers.jackson.Jackson;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.server.AllDirectives;
import org.apache.pekko.http.javadsl.server.PathMatchers;
import org.apache.pekko.http.javadsl.server.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.util.Try;

/** HTTP route handling administrative server queries and telemetry. */
public final class ServersAdminRoute extends AllDirectives {

  private final Logger logger = LoggerFactory.getLogger(ServersAdminRoute.class);
  private static final Duration SERVER_ASK_TIMEOUT = Duration.ofSeconds(10);

  private final ActorRef<ServerAdminCommand> serverAdminActor;
  private final ActorSystem<Void> system;
  private final ObjectMapper objectMapper;

  @Inject
  ServersAdminRoute(
      ActorRef<ServerAdminCommand> serverAdminActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper) {
    this.serverAdminActor = serverAdminActor;
    this.system = system;
    this.objectMapper = objectMapper;
  }

  /**
   * Pure factory method constructing a {@link ServersAdminRoute} instance.
   *
   * @param serverAdminActor The server admin actor reference.
   * @param system The ActorSystem.
   * @param objectMapper The ObjectMapper for JSON marshalling.
   * @return A configured route instance.
   */
  public static ServersAdminRoute create(
      ActorRef<ServerAdminCommand> serverAdminActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper) {
    return new ServersAdminRoute(serverAdminActor, system, objectMapper);
  }

  /**
   * Generates the server administration HTTP route.
   *
   * @return The configured route.
   */
  public Route route() {
    return pathPrefix(
        PathMatchers.separateOnSlashes("api/admin/v1/servers"),
        () -> pathEndOrSingleSlash(() -> get(this::handleServers)));
  }

  private Route handleServers() {
    return onComplete(this::askListServers, this::mapServerResponseToRoute);
  }

  private CompletionStage<ServerAdminResponse> askListServers() {
    return ask(
        serverAdminActor,
        ServerAdminCommand.ListServers::new,
        SERVER_ASK_TIMEOUT,
        system.scheduler());
  }

  private Route mapServerResponseToRoute(Try<ServerAdminResponse> responseTry) {
    if (responseTry.isFailure()) {
      logger.error("Failed to query servers", responseTry.failed().get());
      return complete(StatusCodes.INTERNAL_SERVER_ERROR, "");
    }
    return switch (responseTry.get()) {
      case ServerAdminResponse.ServerList list ->
          completeOK(list.servers(), Jackson.marshaller(objectMapper));
      case ServerAdminResponse.Failure f -> {
        logger.error("Server query returned failure: {}", f.reason());
        yield complete(StatusCodes.INTERNAL_SERVER_ERROR, "");
      }
    };
  }
}
