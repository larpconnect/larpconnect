package com.larpconnect.njall.api.studios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.domain.AddressType;
import com.larpconnect.njall.data.domain.GeoJsonPoint;
import com.larpconnect.njall.data.domain.StudioLookup;
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

final class LocationsRouteTest {

  private static final String CREATE_LOCATION_BODY = "{\"name\":\"Camp Whispering Pines\"}";
  private static final String PATCH_LOCATION_BODY = "{\"name\":\"Camp Whispering Pines - North\"}";
  private static final String CREATE_ADDRESS_BODY =
      """
      {
        "addressType": "PHYSICAL",
        "addressLine1": "123 Forest Rd",
        "locality": "Pineville",
        "administrativeArea": "WA",
        "postalCode": "98101",
        "countryCode": "US"
      }
      """;
  private static final String PATCH_ADDRESS_BODY = "{\"addressLine1\":\"125 Forest Rd\"}";

  private static ActorSystem<Void> system;
  private static ObjectMapper objectMapper;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final UUID locationId = UUID.randomUUID();
  private final UUID addressId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "locations-route-test");
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

  private StudioLookup sampleLookup() {
    return new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.empty());
  }

  private LocationResponse sampleLocationResponse() {
    return new LocationResponse(
        locationId, "Camp Whispering Pines", Optional.of("Main campsite"), now, now);
  }

  private AddressResponse sampleAddressResponse() {
    return new AddressResponse(
        addressId,
        locationId,
        AddressType.PHYSICAL,
        "123 Forest Rd",
        Optional.empty(),
        Optional.empty(),
        "Pineville",
        "WA",
        "98101",
        "US",
        Optional.of(new GeoJsonPoint(-122.33, 47.60)));
  }

  private ActorRef<LocationCommand> createLocationActor(LocationActorResponse response) {
    return system.systemActorOf(
        Behaviors.receiveMessage(
            msg -> {
              switch (msg) {
                case LocationCommand.CreateLocation c -> c.replyTo().tell(response);
                case LocationCommand.GetLocation g -> g.replyTo().tell(response);
                case LocationCommand.PatchLocation p -> p.replyTo().tell(response);
                case LocationCommand.DeleteLocation d -> d.replyTo().tell(response);
              }
              return Behaviors.same();
            }),
        "testLocActor" + UUID.randomUUID(),
        Props.empty());
  }

  private ActorRef<AddressCommand> createAddressActor(AddressActorResponse response) {
    return system.systemActorOf(
        Behaviors.receiveMessage(
            msg -> {
              switch (msg) {
                case AddressCommand.CreateAddress c -> c.replyTo().tell(response);
                case AddressCommand.GetAddress g -> g.replyTo().tell(response);
                case AddressCommand.ListAddresses l -> l.replyTo().tell(response);
                case AddressCommand.PatchAddress p -> p.replyTo().tell(response);
                case AddressCommand.DeleteAddress d -> d.replyTo().tell(response);
              }
              return Behaviors.same();
            }),
        "testAddrActor" + UUID.randomUUID(),
        Props.empty());
  }

  private Function<HttpRequest, CompletionStage<HttpResponse>> createHandler(
      StudioLookupCache cache,
      ActorRef<LocationCommand> locActor,
      ActorRef<AddressCommand> addrActor) {
    return new LocationsRoute(cache, locActor, addrActor, system, objectMapper)
        .route()
        .seal()
        .function(system);
  }

  @Test
  @DisplayName("POST /api/studios/{alias}/v1/locations returns 201 Created on success")
  void postLocation_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var locActor = createLocationActor(LocationActorResponse.success(sampleLocationResponse()));
    var addrActor = createAddressActor(AddressActorResponse.deleted());
    var handler = createHandler(cache, locActor, addrActor);

    var request =
        HttpRequest.POST("/api/studios/valiant/v1/locations")
            .withEntity(ContentTypes.APPLICATION_JSON, CREATE_LOCATION_BODY);
    assertThat(executeRequest(handler, request).status()).isEqualTo(StatusCodes.CREATED);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/locations/{id} returns 200 OK on success")
  void getLocation_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var locActor = createLocationActor(LocationActorResponse.success(sampleLocationResponse()));
    var addrActor = createAddressActor(AddressActorResponse.deleted());
    var handler = createHandler(cache, locActor, addrActor);

    var request = HttpRequest.GET("/api/studios/valiant/v1/locations/" + locationId);
    assertThat(executeRequest(handler, request).status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("PATCH /api/studios/{alias}/v1/locations/{id} returns 200 OK on success")
  void patchLocation_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var locActor = createLocationActor(LocationActorResponse.success(sampleLocationResponse()));
    var addrActor = createAddressActor(AddressActorResponse.deleted());
    var handler = createHandler(cache, locActor, addrActor);

    var request =
        HttpRequest.PATCH("/api/studios/valiant/v1/locations/" + locationId)
            .withEntity(ContentTypes.APPLICATION_JSON, PATCH_LOCATION_BODY);
    assertThat(executeRequest(handler, request).status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("DELETE /api/studios/{alias}/v1/locations/{id} returns 204 No Content on success")
  void deleteLocation_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var locActor = createLocationActor(LocationActorResponse.deleted());
    var addrActor = createAddressActor(AddressActorResponse.deleted());
    var handler = createHandler(cache, locActor, addrActor);

    var request = HttpRequest.DELETE("/api/studios/valiant/v1/locations/" + locationId);
    assertThat(executeRequest(handler, request).status()).isEqualTo(StatusCodes.NO_CONTENT);
  }

  @Test
  @DisplayName(
      "POST /api/studios/{alias}/v1/locations/{id}/addresses returns 201 Created on success")
  void postAddress_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var locActor = createLocationActor(LocationActorResponse.deleted());
    var addrActor = createAddressActor(AddressActorResponse.success(sampleAddressResponse()));
    var handler = createHandler(cache, locActor, addrActor);

    var request =
        HttpRequest.POST("/api/studios/valiant/v1/locations/" + locationId + "/addresses")
            .withEntity(ContentTypes.APPLICATION_JSON, CREATE_ADDRESS_BODY);
    assertThat(executeRequest(handler, request).status()).isEqualTo(StatusCodes.CREATED);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/locations/{id}/addresses returns 200 OK on success")
  void listAddresses_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var locActor = createLocationActor(LocationActorResponse.deleted());
    var addrActor =
        createAddressActor(
            AddressActorResponse.listSuccess(ImmutableList.of(sampleAddressResponse())));
    var handler = createHandler(cache, locActor, addrActor);

    var request = HttpRequest.GET("/api/studios/valiant/v1/locations/" + locationId + "/addresses");
    assertThat(executeRequest(handler, request).status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName(
      "GET /api/studios/{alias}/v1/locations/{id}/addresses/{id} returns 200 OK on success")
  void getAddress_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var locActor = createLocationActor(LocationActorResponse.deleted());
    var addrActor = createAddressActor(AddressActorResponse.success(sampleAddressResponse()));
    var handler = createHandler(cache, locActor, addrActor);

    var request =
        HttpRequest.GET(
            "/api/studios/valiant/v1/locations/" + locationId + "/addresses/" + addressId);
    assertThat(executeRequest(handler, request).status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName(
      "PATCH /api/studios/{alias}/v1/locations/{id}/addresses/{id} returns 200 OK on success")
  void patchAddress_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var locActor = createLocationActor(LocationActorResponse.deleted());
    var addrActor = createAddressActor(AddressActorResponse.success(sampleAddressResponse()));
    var handler = createHandler(cache, locActor, addrActor);

    var request =
        HttpRequest.PATCH(
                "/api/studios/valiant/v1/locations/" + locationId + "/addresses/" + addressId)
            .withEntity(ContentTypes.APPLICATION_JSON, PATCH_ADDRESS_BODY);
    assertThat(executeRequest(handler, request).status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName(
      "DELETE /api/studios/{alias}/v1/locations/{id}/addresses/{id} returns 204 No Content")
  void deleteAddress_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var locActor = createLocationActor(LocationActorResponse.deleted());
    var addrActor = createAddressActor(AddressActorResponse.deleted());
    var handler = createHandler(cache, locActor, addrActor);

    var request =
        HttpRequest.DELETE(
            "/api/studios/valiant/v1/locations/" + locationId + "/addresses/" + addressId);
    assertThat(executeRequest(handler, request).status()).isEqualTo(StatusCodes.NO_CONTENT);
  }

  @Test
  @DisplayName("GET with nonexistent studio returns 404 Not Found")
  void nonexistentStudio_returnsNotFound() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("unknown")).thenReturn(Optional.empty());
    var locActor = createLocationActor(LocationActorResponse.deleted());
    var addrActor = createAddressActor(AddressActorResponse.deleted());
    var handler = createHandler(cache, locActor, addrActor);

    var request = HttpRequest.GET("/api/studios/unknown/v1/locations/" + locationId);
    assertThat(executeRequest(handler, request).status()).isEqualTo(StatusCodes.NOT_FOUND);
  }
}
