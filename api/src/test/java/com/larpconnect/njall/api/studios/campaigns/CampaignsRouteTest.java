package com.larpconnect.njall.api.studios.campaigns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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

final class CampaignsRouteTest {

  private static final String CREATE_BODY =
      "{\"generator\":\"019273c5-9271-7000-8000-000000000001\","
          + "\"title\":\"Seattle By Night\",\"summary\":\"Chronicle\"}";
  private static final String PATCH_BODY = "{\"title\":\"Season Two\",\"system\":null}";

  private static ActorSystem<Void> system;
  private static ObjectMapper objectMapper;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final UUID campaignId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "campaigns-route-test");
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

  private static String readBody(HttpResponse response) throws Exception {
    return response
        .entity()
        .toStrict(1000, system)
        .toCompletableFuture()
        .get(5, TimeUnit.SECONDS)
        .getData()
        .utf8String();
  }

  private StudioLookup activeLookup() {
    return new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.empty());
  }

  private CampaignResponse sampleCampaignResponse() {
    return new CampaignResponse(
        campaignId,
        "/api/studios/valiant/v1/actors/019273c5-9271-7000-8000-000000000001",
        "Seattle By Night",
        Optional.of("Chronicle"),
        Optional.of("/api/studios/valiant/v1/systems/019273c5-9271-7000-8000-000000000002"),
        now,
        now);
  }

  private ActorRef<CampaignCommand> createCampaignActor(
      CampaignActorResponse response, AtomicReference<CampaignCommand> captured) {
    return system.systemActorOf(
        Behaviors.receiveMessage(
            msg -> {
              captured.set(msg);
              switch (msg) {
                case CampaignCommand.CreateCampaign c -> c.replyTo().tell(response);
                case CampaignCommand.GetCampaign g -> g.replyTo().tell(response);
                case CampaignCommand.ListCampaigns l -> l.replyTo().tell(response);
                case CampaignCommand.PatchCampaign p -> p.replyTo().tell(response);
                case CampaignCommand.DeleteCampaign d -> d.replyTo().tell(response);
              }
              return Behaviors.same();
            }),
        "testCampaignActor" + UUID.randomUUID(),
        Props.empty());
  }

  private Function<HttpRequest, CompletionStage<HttpResponse>> createHandler(
      StudioLookupCache cache, ActorRef<CampaignCommand> actor) {
    return new CampaignsRoute(cache, actor, system, objectMapper).route().seal().function(system);
  }

  @Test
  @DisplayName("POST /api/studios/{studio}/v1/campaigns creates campaign and forwards lookup")
  void createCampaign_success_returns201() throws Exception {
    var cache = mock(StudioLookupCache.class);
    var lookup = activeLookup();
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(lookup));
    var captured = new AtomicReference<CampaignCommand>();
    var actor =
        createCampaignActor(CampaignActorResponse.success(sampleCampaignResponse()), captured);

    var response =
        executeRequest(
            createHandler(cache, actor),
            HttpRequest.POST("/api/studios/valiant/v1/campaigns")
                .withEntity(ContentTypes.APPLICATION_JSON, CREATE_BODY));

    assertThat(response.status()).isEqualTo(StatusCodes.CREATED);
    var body = objectMapper.readTree(readBody(response));
    assertThat(body.get("id").asText()).isEqualTo(campaignId.toString());
    assertThat(body.get("title").asText()).isEqualTo("Seattle By Night");
    var cmd = (CampaignCommand.CreateCampaign) captured.get();
    assertThat(cmd.lookup()).contains(lookup);
  }

  @Test
  @DisplayName("GET /api/studios/{studio}/v1/campaigns forwards optional systemId and lookup")
  void listCampaigns_withAndWithoutSystemId_returns200() throws Exception {
    var cache = mock(StudioLookupCache.class);
    var lookup = activeLookup();
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(lookup));
    var captured = new AtomicReference<CampaignCommand>();
    var actor =
        createCampaignActor(
            CampaignActorResponse.items(ImmutableList.of(sampleCampaignResponse())), captured);
    var handler = createHandler(cache, actor);

    var noFilter = executeRequest(handler, HttpRequest.GET("/api/studios/valiant/v1/campaigns"));
    assertThat(noFilter.status()).isEqualTo(StatusCodes.OK);
    assertThat(((CampaignCommand.ListCampaigns) captured.get()).systemId()).isEmpty();

    var withFilter =
        executeRequest(handler, HttpRequest.GET("/api/studios/valiant/v1/campaigns?systemId=abc"));
    assertThat(withFilter.status()).isEqualTo(StatusCodes.OK);
    var filteredCmd = (CampaignCommand.ListCampaigns) captured.get();
    assertThat(filteredCmd.systemId()).contains("abc");
    assertThat(filteredCmd.lookup()).contains(lookup);
  }

  @Test
  @DisplayName("GET /api/studios/{studio}/v1/campaigns/{id} returns 200 and 404")
  void getCampaign_foundAndMissing_returns200And404() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));

    var okActor =
        createCampaignActor(
            CampaignActorResponse.success(sampleCampaignResponse()), new AtomicReference<>());
    var okResponse =
        executeRequest(
            createHandler(cache, okActor),
            HttpRequest.GET("/api/studios/valiant/v1/campaigns/" + campaignId));
    assertThat(okResponse.status()).isEqualTo(StatusCodes.OK);

    var missingActor =
        createCampaignActor(
            CampaignActorResponse.notFound("Campaign not found"), new AtomicReference<>());
    var missingResponse =
        executeRequest(
            createHandler(cache, missingActor),
            HttpRequest.GET("/api/studios/valiant/v1/campaigns/" + campaignId));
    assertThat(missingResponse.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("PATCH /api/studios/{studio}/v1/campaigns/{id} forwards update_mask and lookup")
  void patchCampaign_success_returns200() throws Exception {
    var cache = mock(StudioLookupCache.class);
    var lookup = activeLookup();
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(lookup));
    var captured = new AtomicReference<CampaignCommand>();
    var actor =
        createCampaignActor(CampaignActorResponse.success(sampleCampaignResponse()), captured);

    var response =
        executeRequest(
            createHandler(cache, actor),
            HttpRequest.PATCH(
                    "/api/studios/valiant/v1/campaigns/" + campaignId + "?update_mask=title,system")
                .withEntity(ContentTypes.APPLICATION_JSON, PATCH_BODY));

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
    var cmd = (CampaignCommand.PatchCampaign) captured.get();
    assertThat(cmd.updateMask()).contains("title,system");
    assertThat(cmd.lookup()).contains(lookup);
  }

  @Test
  @DisplayName("DELETE /api/studios/{studio}/v1/campaigns/{id} returns 204")
  void deleteCampaign_success_returns204() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));
    var actor = createCampaignActor(CampaignActorResponse.deleted(), new AtomicReference<>());

    var response =
        executeRequest(
            createHandler(cache, actor),
            HttpRequest.DELETE("/api/studios/valiant/v1/campaigns/" + campaignId));
    assertThat(response.status()).isEqualTo(StatusCodes.NO_CONTENT);
  }

  @Test
  @DisplayName("Returns 404 when studio is missing, soft-deleted, or campaignId is invalid")
  void missingStudioDeletedStudioOrInvalidId_returns404() throws Exception {
    var cache = mock(StudioLookupCache.class);
    var deletedLookup = new StudioLookup(tenantId, studioId, "deleted", now, now, Optional.of(now));
    when(cache.findByIdOrAlias("unknown")).thenReturn(Optional.empty());
    when(cache.findByIdOrAlias("deleted")).thenReturn(Optional.of(deletedLookup));
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));

    var actor =
        createCampaignActor(CampaignActorResponse.notFound("unused"), new AtomicReference<>());
    var handler = createHandler(cache, actor);

    assertThat(
            executeRequest(handler, HttpRequest.GET("/api/studios/unknown/v1/campaigns")).status())
        .isEqualTo(StatusCodes.NOT_FOUND);
    assertThat(
            executeRequest(handler, HttpRequest.GET("/api/studios/deleted/v1/campaigns")).status())
        .isEqualTo(StatusCodes.NOT_FOUND);
    assertThat(
            executeRequest(handler, HttpRequest.GET("/api/studios/valiant/v1/campaigns/not-a-uuid"))
                .status())
        .isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("Maps actor 400, 500, and ask failures to HTTP status codes")
  void actorFailureAndTimeout_returns400And500() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));

    var badActor =
        createCampaignActor(
            CampaignActorResponse.badRequest("Invalid system"), new AtomicReference<>());
    assertThat(
            executeRequest(
                    createHandler(cache, badActor),
                    HttpRequest.POST("/api/studios/valiant/v1/campaigns")
                        .withEntity(ContentTypes.APPLICATION_JSON, CREATE_BODY))
                .status())
        .isEqualTo(StatusCodes.BAD_REQUEST);

    var errorActor =
        createCampaignActor(CampaignActorResponse.failure("DB failure"), new AtomicReference<>());
    assertThat(
            executeRequest(
                    createHandler(cache, errorActor),
                    HttpRequest.GET("/api/studios/valiant/v1/campaigns/" + campaignId))
                .status())
        .isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);

    ActorRef<CampaignCommand> silentActor =
        system.systemActorOf(
            Behaviors.receiveMessage(msg -> Behaviors.same()),
            "silentCampaignActor" + UUID.randomUUID(),
            Props.empty());
    var timeoutHandler =
        new CampaignsRoute(cache, silentActor, system, objectMapper, Duration.ofMillis(50))
            .route()
            .seal()
            .function(system);
    assertThat(
            executeRequest(
                    timeoutHandler,
                    HttpRequest.GET("/api/studios/valiant/v1/campaigns/" + campaignId))
                .status())
        .isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("POST rejects malformed, empty, non-object, and unknown-field JSON with 400 JSON")
  void jsonRejections_malformedEmptyAndNonObject_returnsAip193BadRequest() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));
    var actor =
        createCampaignActor(
            CampaignActorResponse.success(sampleCampaignResponse()), new AtomicReference<>());
    var handler = createHandler(cache, actor);

    var malformed =
        executeRequest(
            handler,
            HttpRequest.POST("/api/studios/valiant/v1/campaigns")
                .withEntity(ContentTypes.APPLICATION_JSON, "{not-json"));
    assertThat(malformed.status()).isEqualTo(StatusCodes.BAD_REQUEST);
    var malformedJson = objectMapper.readTree(readBody(malformed));
    assertThat(malformedJson.get("code").asInt()).isEqualTo(400);
    assertThat(malformedJson.get("message").asText()).isNotBlank();

    var emptyBody = executeRequest(handler, HttpRequest.POST("/api/studios/valiant/v1/campaigns"));
    assertThat(emptyBody.status()).isEqualTo(StatusCodes.BAD_REQUEST);
    var emptyJson = objectMapper.readTree(readBody(emptyBody));
    assertThat(emptyJson.get("code").asInt()).isEqualTo(400);
    assertThat(emptyJson.get("message").asText()).isNotBlank();

    var nonObject =
        executeRequest(
            handler,
            HttpRequest.POST("/api/studios/valiant/v1/campaigns")
                .withEntity(ContentTypes.APPLICATION_JSON, "[1,2]"));
    assertThat(nonObject.status()).isEqualTo(StatusCodes.BAD_REQUEST);
    assertThat(readBody(nonObject)).contains("\"code\":400", "Request body must be a JSON object");

    var unknownField =
        executeRequest(
            handler,
            HttpRequest.POST("/api/studios/valiant/v1/campaigns")
                .withEntity(ContentTypes.APPLICATION_JSON, "{\"unknownField\":\"value\"}"));
    assertThat(unknownField.status()).isEqualTo(StatusCodes.BAD_REQUEST);
    assertThat(readBody(unknownField)).contains("\"code\":400", "Invalid request payload");
  }

  @Test
  @DisplayName("Rejects unmasked null title and non-textual optional fields with 400")
  void jsonValidation_nullTitleAndNonTextualFields_returns400() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));
    var actor =
        createCampaignActor(
            CampaignActorResponse.success(sampleCampaignResponse()), new AtomicReference<>());
    var handler = createHandler(cache, actor);

    var postNullTitle =
        executeRequest(
            handler,
            HttpRequest.POST("/api/studios/valiant/v1/campaigns")
                .withEntity(
                    ContentTypes.APPLICATION_JSON,
                    "{\"generator\":\"" + UUID.randomUUID() + "\",\"title\":null}"));
    assertThat(postNullTitle.status()).isEqualTo(StatusCodes.BAD_REQUEST);
    assertThat(readBody(postNullTitle)).contains("\"code\":400", "Campaign title must be a string");

    var patchNullTitle =
        executeRequest(
            handler,
            HttpRequest.PATCH("/api/studios/valiant/v1/campaigns/" + campaignId)
                .withEntity(ContentTypes.APPLICATION_JSON, "{\"title\":null}"));
    assertThat(patchNullTitle.status()).isEqualTo(StatusCodes.BAD_REQUEST);
    assertThat(readBody(patchNullTitle))
        .contains("\"code\":400", "Campaign title must be a string");

    var badSummary =
        executeRequest(
            handler,
            HttpRequest.POST("/api/studios/valiant/v1/campaigns")
                .withEntity(
                    ContentTypes.APPLICATION_JSON,
                    "{\"generator\":\""
                        + UUID.randomUUID()
                        + "\",\"title\":\"Valid\",\"summary\":123}"));
    assertThat(badSummary.status()).isEqualTo(StatusCodes.BAD_REQUEST);
    assertThat(readBody(badSummary))
        .contains("\"code\":400", "Campaign summary must be a string or null");
  }

  @Test
  @DisplayName("PATCH with update_mask permits unmasked null title while rejecting non-textual")
  void patchJsonValidation_updateMaskAllowsNullRequiredAndRejectsNonTextual() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));
    var captured = new AtomicReference<CampaignCommand>();
    var actor =
        createCampaignActor(CampaignActorResponse.success(sampleCampaignResponse()), captured);
    var handler = createHandler(cache, actor);
    var maskedUrl = "/api/studios/valiant/v1/campaigns/" + campaignId + "?update_mask=summary";

    var partialNullSummaryPatch =
        executeRequest(
            handler,
            HttpRequest.PATCH(maskedUrl)
                .withEntity(ContentTypes.APPLICATION_JSON, "{\"title\":null,\"summary\":null}"));
    assertThat(partialNullSummaryPatch.status()).isEqualTo(StatusCodes.OK);
    assertThat(((CampaignCommand.PatchCampaign) captured.get()).updateMask()).contains("summary");

    var nonTextualTitleUnderMask =
        executeRequest(
            handler,
            HttpRequest.PATCH(maskedUrl)
                .withEntity(ContentTypes.APPLICATION_JSON, "{\"title\":123,\"summary\":null}"));
    assertThat(nonTextualTitleUnderMask.status()).isEqualTo(StatusCodes.BAD_REQUEST);
  }
}
