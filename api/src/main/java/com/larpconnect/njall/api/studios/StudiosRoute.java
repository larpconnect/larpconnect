package com.larpconnect.njall.api.studios;

import static org.apache.pekko.actor.typed.javadsl.AskPattern.ask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.larpconnect.njall.api.http.RouteProvider;
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

/** HTTP route handling user-space tenanted studio retrieval. */
public final class StudiosRoute extends AllDirectives implements RouteProvider {

  private final Logger logger = LoggerFactory.getLogger(StudiosRoute.class);

  private final ActorRef<StudioCommand> studioActor;
  private final ActorSystem<Void> system;
  private final ObjectMapper objectMapper;
  private final Duration askTimeout;

  @Inject
  StudiosRoute(
      ActorRef<StudioCommand> studioActor, ActorSystem<Void> system, ObjectMapper objectMapper) {
    this(studioActor, system, objectMapper, Duration.ofSeconds(20));
  }

  StudiosRoute(
      ActorRef<StudioCommand> studioActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper,
      Duration askTimeout) {
    this.studioActor = studioActor;
    this.system = system;
    this.objectMapper = objectMapper;
    this.askTimeout = askTimeout;
  }

  @Override
  public Route route() {
    return pathPrefix(
        PathMatchers.separateOnSlashes("api/studios"),
        () ->
            pathPrefix(
                PathMatchers.segment(),
                studioIdParam ->
                    path(
                        PathMatchers.separateOnSlashes("v1/studio"),
                        () -> get(() -> handleGetStudio(studioIdParam)))));
  }

  private Route handleGetStudio(String studioIdParam) {
    return onComplete(() -> askGetStudio(studioIdParam), this::mapResponse);
  }

  private CompletionStage<StudioActorResponse> askGetStudio(String studioIdParam) {
    return ask(
        studioActor,
        replyTo -> new StudioCommand.GetStudio(studioIdParam, replyTo),
        askTimeout,
        system.scheduler());
  }

  private Route mapResponse(Try<StudioActorResponse> responseTry) {
    if (responseTry.isFailure()) {
      return handleActorFailure(responseTry.failed().get());
    }
    return switch (responseTry.get()) {
      case StudioActorResponse.Success s ->
          completeOK(s.studio(), Jackson.marshaller(objectMapper));
      case StudioActorResponse.NotFound nf ->
          complete(
              StatusCodes.NOT_FOUND,
              new StudioErrorResponse(404, nf.message()),
              Jackson.marshaller(objectMapper));
      case StudioActorResponse.Failure f ->
          complete(
              StatusCodes.INTERNAL_SERVER_ERROR,
              new StudioErrorResponse(500, f.message()),
              Jackson.marshaller(objectMapper));
    };
  }

  private Route handleActorFailure(Throwable error) {
    logger.error("Studio actor request failed", error);
    return complete(
        StatusCodes.INTERNAL_SERVER_ERROR,
        new StudioErrorResponse(500, "Internal server error"),
        Jackson.marshaller(objectMapper));
  }
}
