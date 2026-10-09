package com.larpconnect.njall.api.studios.locations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.larpconnect.njall.api.studios.addresses.*;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.domain.StudioLookup;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.http.javadsl.model.ContentTypes;
import org.apache.pekko.http.javadsl.model.HttpRequest;
import org.apache.pekko.http.javadsl.model.HttpResponse;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.japi.function.Function;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class LocationsRouteBranchTest {

  private static ActorSystem<Void> system;
  private static ObjectMapper objectMapper;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final UUID locationId = UUID.randomUUID();
  private final UUID addressId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "locations-route-branch-test");
    objectMapper =
        JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
  }

  @AfterAll
  static void tearDown() throws Exception {
    system.terminate();
    system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  private static HttpResponse executeRequest(
      Function<HttpRequest, CompletionStage<HttpResponse>> handler, HttpRequest request)
      throws Exception {
    return handler.apply(request).toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  private Function<HttpRequest, CompletionStage<HttpResponse>> createHandler(
      StudioLookupCache cache,
      ActorRef<LocationCommand> locActor,
      ActorRef<AddressCommand> addrActor) {
    var addressesRoute =
        new AddressesRoute(addrActor, system, objectMapper, Duration.ofSeconds(20));
    return new LocationsRoute(cache, locActor, addressesRoute, system, objectMapper)
        .route()
        .seal()
        .function(system);
  }

  private StudioLookup activeLookup() {
    return new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.empty());
  }

  private StudioLookup deletedLookup() {
    return new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.of(now));
  }

  private ActorRef<LocationCommand> createLocationActor(LocationActorResponse response) {
    return system.systemActorOf(
        Behaviors.receive(
            (ctx, msg) -> {
              switch (msg) {
                case LocationCommand.CreateLocation cl -> cl.replyTo().tell(response);
                case LocationCommand.GetLocation gl -> gl.replyTo().tell(response);
                case LocationCommand.PatchLocation pl -> pl.replyTo().tell(response);
                case LocationCommand.DeleteLocation dl -> dl.replyTo().tell(response);
              }
              return Behaviors.same();
            }),
        "loc-branch-" + UUID.randomUUID(),
        Props.empty());
  }

  private ActorRef<AddressCommand> createAddressActor(AddressActorResponse response) {
    return system.systemActorOf(
        Behaviors.receive(
            (ctx, msg) -> {
              switch (msg) {
                case AddressCommand.CreateAddress ca -> ca.replyTo().tell(response);
                case AddressCommand.GetAddress ga -> ga.replyTo().tell(response);
                case AddressCommand.ListAddresses la -> la.replyTo().tell(response);
                case AddressCommand.PatchAddress pa -> pa.replyTo().tell(response);
                case AddressCommand.DeleteAddress da -> da.replyTo().tell(response);
              }
              return Behaviors.same();
            }),
        "addr-branch-" + UUID.randomUUID(),
        Props.empty());
  }

  @Test
  @DisplayName("handleLocations returns 404 when studio is soft-deleted")
  void handleLocations_deletedStudio_returns404() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(deletedLookup()));

    var locActor = createLocationActor(new LocationActorResponse.NotFound("Not found"));
    var addrActor = createAddressActor(new AddressActorResponse.NotFound("Not found"));
    var handler = createHandler(cache, locActor, addrActor);

    var request = HttpRequest.GET("/api/studios/valiant/v1/locations/" + locationId);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("handleLocationBranch returns 404 when location ID is not a valid UUID")
  void handleLocationBranch_invalidUuid_returns404() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));

    var locActor = createLocationActor(new LocationActorResponse.NotFound("Not found"));
    var addrActor = createAddressActor(new AddressActorResponse.NotFound("Not found"));
    var handler = createHandler(cache, locActor, addrActor);

    var request = HttpRequest.GET("/api/studios/valiant/v1/locations/not-a-uuid");
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("handleAddressItem returns 404 when address ID is not a valid UUID")
  void handleAddressItem_invalidUuid_returns404() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));

    var locActor = createLocationActor(new LocationActorResponse.NotFound("Not found"));
    var addrActor = createAddressActor(new AddressActorResponse.NotFound("Not found"));
    var handler = createHandler(cache, locActor, addrActor);

    var request =
        HttpRequest.GET(
            "/api/studios/valiant/v1/locations/" + locationId + "/addresses/not-a-uuid");
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("mapLocationResponse returns 500 when location actor returns Failure")
  void mapLocationResponse_failure_returns500() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));

    var locActor = createLocationActor(new LocationActorResponse.Failure("Location failure"));
    var addrActor = createAddressActor(new AddressActorResponse.NotFound("Not found"));
    var handler = createHandler(cache, locActor, addrActor);

    var request = HttpRequest.GET("/api/studios/valiant/v1/locations/" + locationId);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("mapAddressResponse returns 500 when address actor returns Failure")
  void mapAddressResponse_failure_returns500() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));

    var locActor = createLocationActor(new LocationActorResponse.NotFound("Not found"));
    var addrActor = createAddressActor(new AddressActorResponse.Failure("Address failure"));
    var handler = createHandler(cache, locActor, addrActor);

    var request =
        HttpRequest.GET(
            "/api/studios/valiant/v1/locations/" + locationId + "/addresses/" + addressId);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("mapLocationResponse returns 400 when location actor returns BadRequest")
  void mapLocationResponse_badRequest_returns400() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));

    var locActor = createLocationActor(new LocationActorResponse.BadRequest("Invalid field"));
    var addrActor = createAddressActor(new AddressActorResponse.NotFound("Not found"));
    var handler = createHandler(cache, locActor, addrActor);

    var request =
        HttpRequest.POST("/api/studios/valiant/v1/locations")
            .withEntity(ContentTypes.APPLICATION_JSON, "{\"name\":\"\"}");
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.BAD_REQUEST);
  }

  @Test
  @DisplayName("mapAddressResponse returns 400 when address actor returns BadRequest")
  void mapAddressResponse_badRequest_returns400() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));

    var locActor = createLocationActor(new LocationActorResponse.NotFound("Not found"));
    var addrActor = createAddressActor(new AddressActorResponse.BadRequest("Invalid field"));
    var handler = createHandler(cache, locActor, addrActor);

    var request =
        HttpRequest.POST("/api/studios/valiant/v1/locations/" + locationId + "/addresses")
            .withEntity(
                ContentTypes.APPLICATION_JSON,
                "{\"addressType\":\"PHYSICAL\",\"addressLine1\":\"\"}");
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.BAD_REQUEST);
  }
}
