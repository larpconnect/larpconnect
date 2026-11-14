package com.larpconnect.njall.api.studios;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.util.Modules;
import com.larpconnect.njall.api.http.RouteProvider;
import com.larpconnect.njall.api.studios.common.StudioSubRoute;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.http.javadsl.model.HttpRequest;
import org.apache.pekko.http.javadsl.model.HttpResponse;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.server.Directives;
import org.apache.pekko.http.javadsl.server.Route;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class StudioSubRoutesTest {

  private static ActorSystem<Void> system;

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "studio-sub-routes-test");
  }

  @AfterAll
  static void tearDown() throws Exception {
    system.terminate();
    system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  private static HttpResponse execute(StudioSubRoutes subRoutes, String path) throws Exception {
    var handler = subRoutes.route().seal().function(system);
    return handler.apply(HttpRequest.GET(path)).toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  @Test
  void route_emptyProviders_rejectsRequestsWithNotFound() throws Exception {
    var subRoutes = new StudioSubRoutes(Set.of());

    assertThat(execute(subRoutes, "/probe").status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  void route_singleProvider_servesRoute() throws Exception {
    var subRoutes = new StudioSubRoutes(Set.of(new ZebraRouteProvider()));

    assertThat(execute(subRoutes, "/probe").status()).isEqualTo(StatusCodes.ACCEPTED);
    assertThat(execute(subRoutes, "/zebra").status()).isEqualTo(StatusCodes.NO_CONTENT);
  }

  @Test
  void route_multipleProviders_sortsDeterministicallyByClassName() throws Exception {
    var alpha = new AlphaRouteProvider();
    var zebra = new ZebraRouteProvider();

    var zebraFirst = new LinkedHashSet<RouteProvider>();
    zebraFirst.add(zebra);
    zebraFirst.add(alpha);
    var fromZebraFirst = new StudioSubRoutes(zebraFirst);

    var alphaFirst = new LinkedHashSet<RouteProvider>();
    alphaFirst.add(alpha);
    alphaFirst.add(zebra);
    var fromAlphaFirst = new StudioSubRoutes(alphaFirst);

    assertThat(execute(fromZebraFirst, "/probe").status()).isEqualTo(StatusCodes.OK);
    assertThat(execute(fromAlphaFirst, "/probe").status()).isEqualTo(StatusCodes.OK);
    assertThat(execute(fromZebraFirst, "/alpha").status()).isEqualTo(StatusCodes.CREATED);
    assertThat(execute(fromZebraFirst, "/zebra").status()).isEqualTo(StatusCodes.NO_CONTENT);
  }

  @Test
  void injector_strictGuiceMode_injectsMultiboundStudioSubRoutes() throws Exception {
    var injector =
        Guice.createInjector(
            Modules.disableCircularProxiesModule(),
            Modules.requireAtInjectOnConstructorsModule(),
            Modules.requireExplicitBindingsModule(),
            Modules.requireExactBindingAnnotationsModule(),
            new TestSubRoutesModule());

    var subRoutes = injector.getInstance(StudioSubRoutes.class);

    assertThat(execute(subRoutes, "/probe").status()).isEqualTo(StatusCodes.OK);
    assertThat(execute(subRoutes, "/alpha").status()).isEqualTo(StatusCodes.CREATED);
    assertThat(execute(subRoutes, "/zebra").status()).isEqualTo(StatusCodes.NO_CONTENT);
  }

  @Test
  void studioSubRoute_annotation_hasExpectedGuiceMetadata() {
    assertThat(StudioSubRoute.class.isAnnotationPresent(BindingAnnotation.class)).isTrue();
    assertThat(StudioSubRoute.class.getAnnotation(Retention.class).value())
        .isEqualTo(RetentionPolicy.RUNTIME);
    assertThat(StudioSubRoute.class.getAnnotation(Target.class).value())
        .containsExactlyInAnyOrder(ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD);
  }

  static final class AlphaRouteProvider implements RouteProvider {

    @Inject
    AlphaRouteProvider() {}

    @Override
    public Route route() {
      return Directives.concat(
          Directives.path("probe", () -> Directives.complete(StatusCodes.OK)),
          Directives.path("alpha", () -> Directives.complete(StatusCodes.CREATED)));
    }
  }

  static final class ZebraRouteProvider implements RouteProvider {

    @Inject
    ZebraRouteProvider() {}

    @Override
    public Route route() {
      return Directives.concat(
          Directives.path("probe", () -> Directives.complete(StatusCodes.ACCEPTED)),
          Directives.path("zebra", () -> Directives.complete(StatusCodes.NO_CONTENT)));
    }
  }

  private static final class TestSubRoutesModule extends AbstractModule {

    @Override
    protected void configure() {
      bind(AlphaRouteProvider.class).in(Scopes.SINGLETON);
      bind(ZebraRouteProvider.class).in(Scopes.SINGLETON);
      bind(StudioSubRoutes.class).in(Scopes.SINGLETON);
      var multibinder =
          Multibinder.newSetBinder(binder(), RouteProvider.class, StudioSubRoute.class);
      multibinder.addBinding().to(ZebraRouteProvider.class);
      multibinder.addBinding().to(AlphaRouteProvider.class);
    }
  }
}
