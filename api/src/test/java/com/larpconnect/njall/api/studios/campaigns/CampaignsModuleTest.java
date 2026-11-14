package com.larpconnect.njall.api.studios.campaigns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.larpconnect.njall.api.http.RouteProvider;
import com.larpconnect.njall.api.studios.common.StudioSubRoute;
import com.larpconnect.njall.common.annotation.Blocking;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.dao.studios.campaigns.CampaignDAO;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import com.larpconnect.njall.data.domain.studios.Campaign;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.http.javadsl.model.ContentTypes;
import org.apache.pekko.http.javadsl.model.HttpRequest;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class CampaignsModuleTest {

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();
  private final UUID systemId = UUID.randomUUID();
  private final UUID campaignId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @Test
  @DisplayName("CampaignsModule binds singleton CampaignsRoute into StudioSubRoute multibinder")
  void configure_bindsRouteFactoryAndActorRef() throws Exception {
    ActorSystem<Void> testSystem =
        ActorSystem.create(Behaviors.empty(), "campaigns-module-binding-test");
    try {
      var injector =
          createInjector(mock(CampaignDAO.class), mock(StudioLookupCache.class), testSystem);
      var routeFirst = injector.getInstance(CampaignsRoute.class);
      var routeSecond = injector.getInstance(CampaignsRoute.class);
      var factory = injector.getInstance(CampaignActorFactory.class);
      var actorKey = Key.get(new TypeLiteral<ActorRef<CampaignCommand>>() {});
      var actorFirst = injector.getInstance(actorKey);
      var actorSecond = injector.getInstance(actorKey);
      var subRoutes =
          injector.getInstance(
              Key.get(new TypeLiteral<Set<RouteProvider>>() {}, StudioSubRoute.class));

      assertThat(routeFirst).isSameAs(routeSecond);
      assertThat(factory).isInstanceOf(DefaultCampaignActorFactory.class);
      assertThat(actorFirst).isSameAs(actorSecond);
      assertThat(actorFirst.path().name()).isEqualTo("campaignActor");
      assertThat(subRoutes).hasAtLeastOneElementOfType(CampaignsRoute.class);
    } finally {
      terminateSystem(testSystem);
    }
  }

  @Test
  @DisplayName("CampaignsModule wires CampaignsRoute and CampaignActor end-to-end")
  void routeProvider_executesPostAndListAgainstWiredActor() throws Exception {
    ActorSystem<Void> testSystem =
        ActorSystem.create(Behaviors.empty(), "campaigns-module-route-test");
    try {
      var campaignDao = mock(CampaignDAO.class);
      var domain = sampleCampaign();
      when(campaignDao.create(
              tenantId,
              actorId,
              "Seattle By Night",
              Optional.of("Chronicle"),
              Optional.of(systemId)))
          .thenReturn(domain);
      when(campaignDao.listAll(tenantId, Optional.of(systemId)))
          .thenReturn(ImmutableList.of(domain));

      var injector = createInjector(campaignDao, mockActiveCache(), testSystem);
      var handler = injector.getInstance(CampaignsRoute.class).route().seal().function(testSystem);

      var postResponse =
          handler.apply(samplePostRequest()).toCompletableFuture().get(5, TimeUnit.SECONDS);
      assertThat(postResponse.status()).isEqualTo(StatusCodes.CREATED);

      var listRequest = HttpRequest.GET("/api/studios/valiant/v1/campaigns?systemId=" + systemId);
      var listResponse = handler.apply(listRequest).toCompletableFuture().get(5, TimeUnit.SECONDS);
      assertThat(listResponse.status()).isEqualTo(StatusCodes.OK);
    } finally {
      terminateSystem(testSystem);
    }
  }

  private StudioLookupCache mockActiveCache() {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant"))
        .thenReturn(
            Optional.of(
                new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.empty())));
    return cache;
  }

  private Campaign sampleCampaign() {
    return new Campaign(
        campaignId,
        actorId,
        Optional.of(systemId),
        "Seattle By Night",
        Optional.of("Chronicle"),
        now,
        now,
        Optional.empty());
  }

  private HttpRequest samplePostRequest() {
    return HttpRequest.POST("/api/studios/valiant/v1/campaigns")
        .withEntity(
            ContentTypes.APPLICATION_JSON,
            "{\"generator\":\""
                + actorId
                + "\",\"title\":\"Seattle By Night\","
                + "\"summary\":\"Chronicle\",\"system\":\""
                + systemId
                + "\"}");
  }

  private void terminateSystem(ActorSystem<Void> testSystem) throws Exception {
    testSystem.terminate();
    testSystem.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  private Injector createInjector(
      CampaignDAO campaignDao, StudioLookupCache cache, ActorSystem<Void> actorSystem) {
    return Guice.createInjector(
        new CampaignsModule(), createTestModule(campaignDao, cache, actorSystem));
  }

  private Module createTestModule(
      CampaignDAO campaignDao, StudioLookupCache cache, ActorSystem<Void> actorSystem) {
    var mapper =
        JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    return new AbstractModule() {
      @Override
      protected void configure() {
        bind(CampaignDAO.class).toInstance(campaignDao);
        bind(StudioLookupCache.class).toInstance(cache);
        bind(ObjectMapper.class).toInstance(mapper);
        bind(Key.get(new TypeLiteral<ActorSystem<Void>>() {})).toInstance(actorSystem);
        bind(Props.class).annotatedWith(Blocking.class).toInstance(Props.empty());
      }
    };
  }
}
