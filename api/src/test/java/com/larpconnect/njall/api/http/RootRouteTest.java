package com.larpconnect.njall.api.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.http.javadsl.model.HttpRequest;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.server.Directives;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class RootRouteTest {

  private static ActorSystem<Void> system;
  private static final RouteProvider REJECTING_ROUTE_PROVIDER = Directives::reject;

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "root-route-test");
  }

  @AfterAll
  static void tearDown() throws Exception {
    system.terminate();
    system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  @Test
  @DisplayName("route returns 200 OK with blank entity for GET /")
  void route_getSlash_returnsOkWithBlankEntity() throws Exception {
    var rootRoute = new DefaultRootRoute(Set.of(REJECTING_ROUTE_PROVIDER));
    var handler = rootRoute.route().seal().function(system);

    var response =
        handler.apply(HttpRequest.GET("/")).toCompletableFuture().get(5, TimeUnit.SECONDS);
    var strictEntity =
        response
            .entity()
            .toStrict(Duration.ofSeconds(1).toMillis(), system)
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
    assertThat(strictEntity.getData().utf8String()).isEmpty();
  }

  @Test
  @DisplayName("route concatenates and delegates to registered RouteProvider")
  void route_delegatesToRouteProvider() throws Exception {
    RouteProvider customProvider =
        () ->
            Directives.path(
                "custom-subroute",
                () -> Directives.get(() -> Directives.complete(StatusCodes.ACCEPTED, "custom")));
    var rootRoute = new DefaultRootRoute(Set.of(customProvider));
    var handler = rootRoute.route().seal().function(system);

    var response =
        handler
            .apply(HttpRequest.GET("/custom-subroute"))
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);

    assertThat(response.status()).isEqualTo(StatusCodes.ACCEPTED);
  }

  @Test
  @DisplayName("route rejects POST request to /")
  void route_postSlash_isRejected() throws Exception {
    var rootRoute = new DefaultRootRoute(Set.of(REJECTING_ROUTE_PROVIDER));
    var handler = rootRoute.route().seal().function(system);

    var response =
        handler.apply(HttpRequest.POST("/")).toCompletableFuture().get(5, TimeUnit.SECONDS);

    assertThat(response.status()).isEqualTo(StatusCodes.METHOD_NOT_ALLOWED);
  }
}
