package com.larpconnect.njall.api.studios.inbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.larpconnect.njall.common.config.ServerConfig;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class LocalActorRecipientExtractorTest {

  private ObjectMapper mapper;
  private LocalActorRecipientExtractor extractor;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-11-14T12:00:00Z");

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper();
    extractor = new DefaultLocalActorRecipientExtractor(new ServerConfig("127.0.0.1", 0));
  }

  @Test
  @DisplayName(
      "Extracts recipients matching alias, UUID, or param from relative and local primary-domain"
          + " URIs")
  void extract_validRecipientsInToAndCc() {
    var lookup =
        Optional.of(new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.empty()));

    ObjectNode activity = mapper.createObjectNode();
    ArrayNode to = activity.putArray("to");
    to.add("/api/studios/valiant/v1/actors/galahad");
    to.add("https://larpconnect.org/api/studios/" + studioId + "/v1/actors/lancelot");
    to.add("http://LARPCONNECT.ORG/studios/valiant/actors/percival");
    to.add("https://larpconnect.org:443/api/studios/valiant/v1/actors/bedivere");
    to.add("http://larpconnect.org:80/api/studios/valiant/v1/actors/tristan");

    ArrayNode cc = activity.putArray("cc");
    cc.add("/api/studios/valiant/v1/actors/018f2345-6789-7abc-def0-123456789def");
    cc.add("/api/studios/other-studio/v1/actors/mordred");

    var result = extractor.extract(activity, "valiant", studioId, lookup);

    assertThat(result)
        .containsExactlyInAnyOrder(
            "galahad",
            "lancelot",
            "percival",
            "bedivere",
            "tristan",
            "018f2345-6789-7abc-def0-123456789def");
  }

  @Test
  @DisplayName("Extracts recipient from object when object is textual actor URI")
  void extract_recipientFromObject() {
    var lookup =
        Optional.of(new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.empty()));

    ObjectNode activity = mapper.createObjectNode();
    activity.put("object", "/api/studios/valiant/v1/actors/arthur");

    var result = extractor.extract(activity, "valiant", studioId, lookup);

    assertThat(result).containsExactly("arthur");
  }

  @Test
  @DisplayName("Extracts recipients from embedded JSON object to and cc arrays")
  void extract_recipientsFromEmbeddedObjectToAndCc() {
    var lookup =
        Optional.of(new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.empty()));

    ObjectNode activity = mapper.createObjectNode();
    activity.putArray("to").add("https://www.w3.org/ns/activitystreams#Public");
    ObjectNode embedded = activity.putObject("object");
    embedded.putArray("to").add("/api/studios/valiant/v1/actors/percival");
    embedded
        .putArray("cc")
        .add("https://larpconnect.org/api/studios/" + studioId + "/v1/actors/bors")
        .add("/api/studios/other-studio/v1/actors/outsider");

    var result = extractor.extract(activity, "valiant", studioId, lookup);

    assertThat(result).containsExactlyInAnyOrder("percival", "bors");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "https://foreign.example/api/studios/valiant/v1/actors/galahad",
        "https://larpconnect.app/api/studios/valiant/v1/actors/galahad",
        "//larpconnect.org/api/studios/valiant/v1/actors/galahad",
        "acct:galahad@larpconnect.org",
        "urn:uuid:018f2345-6789-7abc-def0-123456789def",
        "as:Public",
        "bare_username",
        "ftp://larpconnect.org/api/studios/valiant/v1/actors/galahad",
        "https://bad_host/api/studios/valiant/v1/actors/galahad",
        "https://larpconnect.org",
        "https://larpconnect.org/api/studios/valiant/v1/actors/galahad?page=1",
        "https://larpconnect.org/api/studios/valiant/v1/actors/galahad#main-key",
        "/api/studios/valiant/v1/actors/galahad/followers",
        "/api/studios/valiant/v1/actors/galahad/subcollection",
        "https://larpconnect.org/studios/valiant/actors/galahad/inbox",
        "https://larpconnect.org/api/studios/valiant/v1/actors/invalid actor with spaces",
        "https://attacker@larpconnect.org/api/studios/valiant/v1/actors/galahad",
        "https://larpconnect.org:8443/api/studios/valiant/v1/actors/galahad",
        "http://larpconnect.org:443/api/studios/valiant/v1/actors/galahad"
      })
  @DisplayName("Rejects foreign hosts, non-http schemes, fragments, queries, and sub-resources")
  void extract_rejectsForeignHostsInvalidSchemesAndSubResources(String candidateUri) {
    ObjectNode activity = mapper.createObjectNode();
    activity.putArray("to").add(candidateUri);

    var result = extractor.extract(activity, "valiant", studioId, Optional.empty());
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Supports host:port primaryDomain and fallback for unparseable primaryDomain")
  void extract_hostWithExplicitPortAndMalformedPrimaryDomain() {
    var portExtractor =
        new DefaultLocalActorRecipientExtractor(
            new ServerConfig("127.0.0.1", 0, "dev", "localhost:8080", "admin@localhost"));
    ObjectNode activity = mapper.createObjectNode();
    activity
        .putArray("to")
        .add("http://localhost:8080/api/studios/valiant/v1/actors/galahad")
        .add("http://localhost:9090/api/studios/valiant/v1/actors/lancelot")
        .add("http://localhost/api/studios/valiant/v1/actors/percival");
    assertThat(portExtractor.extract(activity, "valiant", studioId, Optional.empty()))
        .containsExactly("galahad");

    var fallbackExtractor =
        new DefaultLocalActorRecipientExtractor(
            new ServerConfig("127.0.0.1", 0, "dev", "bad domain", "admin@localhost"));
    ObjectNode relActivity = mapper.createObjectNode();
    relActivity.putArray("to").add("/api/studios/valiant/v1/actors/arthur");
    assertThat(fallbackExtractor.extract(relActivity, "valiant", studioId, Optional.empty()))
        .containsExactly("arthur");
  }

  @Test
  @DisplayName("Handles non-textual nodes, non-array nodes, malformed paths, and empty lookups")
  void extract_edgeCases_handledGracefully() {
    ObjectNode activity = mapper.createObjectNode();
    ArrayNode to = activity.putArray("to");
    to.addObject();
    to.add(42);
    to.add("");
    to.add("   ");
    to.add("/not/a/studios/prefix");
    to.add("/api/studios/valiant/v1/games/boardgame");
    to.add("/api/studios/valiant/v1/actors/");
    to.add("/api/studios/valiant/v1/actors/   ");
    to.add("/studios/valiant/actors/kay");

    activity.put("cc", "scalar-not-array");

    var result = extractor.extract(activity, "valiant", studioId, Optional.empty());
    assertThat(result).containsExactly("kay");
  }
}
