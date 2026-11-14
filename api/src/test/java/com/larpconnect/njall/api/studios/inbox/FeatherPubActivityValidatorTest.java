package com.larpconnect.njall.api.studios.inbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

final class FeatherPubActivityValidatorTest {

  private static final String SCHEMA_URL = "https://larpconnect.app/schemas/featherpub";
  private static final Instant FIXED_NOW = Instant.parse("2026-11-14T12:00:00Z");

  private ObjectMapper mapper;
  private FeatherPubActivityValidator validator;

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper();
    Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    validator = new DefaultFeatherPubActivityValidator(fixedClock);
  }

  private ObjectNode createBaseActivity(String type) {
    ObjectNode node = mapper.createObjectNode();
    ArrayNode ctx = node.putArray("@context");
    ctx.add(SCHEMA_URL);
    node.put("id", "https://larpconnect.app/api/studios/valiant/v1/activities/1");
    node.put("type", type);
    node.put("actor", "https://larpconnect.app/api/studios/valiant/v1/actors/master");
    node.put("created", FIXED_NOW.toString());
    ArrayNode to = node.putArray("to");
    to.add("https://larpconnect.app/api/studios/valiant/v1/actors/apprentice");
    return node;
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Create", "Update", "Delete", "Add", "Remove",
        "Move", "Offer", "Accept", "Reject", "Announce"
      })
  @DisplayName("All 10 FeatherPub verbs validate successfully with valid fields")
  void validate_allTenVerbs_succeeds(String verb) {
    ObjectNode activity = createBaseActivity(verb);
    ObjectNode objectNode = mapper.createObjectNode().put("name", "Artifact");
    activity.set("object", objectNode);

    if ("Add".equals(verb) || "Remove".equals(verb)) {
      activity.put(
          "target",
          "https://larpconnect.app/api/studios/valiant/v1/actors/apprentice/collections/inv");
    } else if ("Move".equals(verb)) {
      activity.put(
          "origin",
          "https://larpconnect.app/api/studios/valiant/v1/actors/apprentice/collections/inv1");
      activity.put(
          "target",
          "https://larpconnect.app/api/studios/valiant/v1/actors/apprentice/collections/inv2");
    }

    assertThat(validator.validate(activity)).isEmpty();
  }

  @Test
  @DisplayName("Non-object payload is rejected")
  void validate_nonObjectPayload_rejected() {
    var textNode = mapper.getNodeFactory().textNode("not an object");
    assertThat(validator.validate(textNode))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("must be a JSON object"));
  }

  @Test
  @DisplayName("Missing or scalar @context is rejected")
  void validate_invalidContext_rejected() {
    ObjectNode activity = createBaseActivity("Create");
    activity.set("object", mapper.createObjectNode());

    activity.remove("@context");
    assertThat(validator.validate(activity))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("@context must be an array"));

    activity.put("@context", SCHEMA_URL);
    assertThat(validator.validate(activity))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("@context must be an array"));

    ArrayNode wrongArray = mapper.createArrayNode().add("https://www.w3.org/ns/activitystreams");
    activity.set("@context", wrongArray);
    assertThat(validator.validate(activity))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("must contain"));
  }

  @Test
  @DisplayName("Unsupported verb is rejected")
  void validate_unsupportedVerb_rejected() {
    ObjectNode activity = createBaseActivity("Explode");
    activity.set("object", mapper.createObjectNode());

    assertThat(validator.validate(activity))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("Unsupported activity verb: Explode"));
  }

  @Test
  @DisplayName("Move verb requires origin and target fields")
  void validate_moveMissingOriginOrTarget_rejected() {
    ObjectNode activity = createBaseActivity("Move");
    activity.set("object", mapper.createObjectNode());

    assertThat(validator.validate(activity))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("origin"));

    activity.put("origin", "https://larpconnect.app/col1");
    assertThat(validator.validate(activity))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("target"));
  }

  @Test
  @DisplayName("Add and Remove require target URI")
  void validate_addMissingTarget_rejected() {
    ObjectNode activity = createBaseActivity("Add");
    activity.set("object", mapper.createObjectNode());

    assertThat(validator.validate(activity))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("target"));
  }

  @Test
  @DisplayName("Authority host mismatch between id and actor is rejected")
  void validate_authorityMismatch_rejected() {
    ObjectNode activity = createBaseActivity("Create");
    activity.set("object", mapper.createObjectNode());
    activity.put("id", "https://foreign-domain.com/act/1");

    assertThat(validator.validate(activity))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("must match or be a subdomain"));
  }

  @Test
  @DisplayName("Subdomain authority match between id and actor is accepted")
  void validate_authoritySubdomain_accepted() {
    ObjectNode activity = createBaseActivity("Create");
    activity.set("object", mapper.createObjectNode());
    activity.put("id", "https://sub.larpconnect.app/act/1");

    assertThat(validator.validate(activity)).isEmpty();
  }

  @Test
  @DisplayName("Created timestamp exceeds 5m future skew or 8d past age is rejected")
  void validate_temporalBounds_rejected() {
    ObjectNode activity = createBaseActivity("Create");
    activity.set("object", mapper.createObjectNode());

    activity.put("created", FIXED_NOW.plus(Duration.ofMinutes(6)).toString());
    assertThat(validator.validate(activity))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("5 minutes"));

    activity.put("created", FIXED_NOW.minus(Duration.ofDays(9)).toString());
    assertThat(validator.validate(activity))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("8 days"));

    activity.put("created", "invalid-date-string");
    assertThat(validator.validate(activity))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("ISO-8601"));
  }

  @Test
  @DisplayName("Missing to and cc recipients is rejected")
  void validate_recipientsPresence_rejected() {
    ObjectNode activity = createBaseActivity("Create");
    activity.set("object", mapper.createObjectNode());
    activity.remove("to");

    assertThat(validator.validate(activity))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("include 'to' or 'cc'"));

    activity.put("to", "not-an-array");
    assertThat(validator.validate(activity))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("must be an array"));

    activity.set("to", mapper.createArrayNode());
    assertThat(validator.validate(activity))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("at least one recipient"));
  }

  @Test
  @DisplayName("Non-textual or blank element in @context array is rejected")
  void validate_contextArrayNonTextual_rejected() {
    ObjectNode activity = createBaseActivity("Create");
    activity.set("object", mapper.createObjectNode());
    ArrayNode ctx = mapper.createArrayNode();
    ctx.add(12345);
    activity.set("@context", ctx);

    assertThat(validator.validate(activity))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("non-blank strings"));

    ArrayNode mixedCtx = mapper.createArrayNode().add(SCHEMA_URL).add(12345);
    activity.set("@context", mixedCtx);
    assertThat(validator.validate(activity))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("non-blank strings"));

    ArrayNode blankCtx = mapper.createArrayNode().add(SCHEMA_URL).add("   ");
    activity.set("@context", blankCtx);
    assertThat(validator.validate(activity))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("non-blank strings"));
  }

  @Test
  @DisplayName("Missing, blank, or non-textual type field is rejected")
  void validate_typeFieldVariations_rejected() {
    ObjectNode activity = createBaseActivity("Create");
    activity.set("object", mapper.createObjectNode());

    activity.remove("type");
    assertThat(validator.validate(activity)).isPresent();

    activity.put("type", 123);
    assertThat(validator.validate(activity)).isPresent();

    activity.put("type", "   ");
    assertThat(validator.validate(activity)).isPresent();
  }

  @Test
  @DisplayName("Create or Update requires object to be a JSON object, not a URI string or array")
  void validate_createOrUpdateRequiresObject_rejected() {
    ObjectNode activity = createBaseActivity("Create");
    activity.put("object", "https://larpconnect.app/obj/1");
    assertThat(validator.validate(activity))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("must be a JSON object"));

    activity.remove("object");
    assertThat(validator.validate(activity)).isPresent();
  }

  @Test
  @DisplayName("Delete/Offer accepts textual URI for object, but rejects invalid/blank values")
  void validate_objectPresentTextualOrInvalid() {
    ObjectNode deleteAct = createBaseActivity("Delete");
    deleteAct.put("object", "https://larpconnect.app/obj/1");
    assertThat(validator.validate(deleteAct)).isEmpty();
    deleteAct.put("object", "   ");
    assertThat(validator.validate(deleteAct)).isPresent();
    deleteAct.put("object", 42);
    assertThat(validator.validate(deleteAct)).isPresent();
    deleteAct.remove("object");
    assertThat(validator.validate(deleteAct)).isPresent();
  }

  @Test
  @DisplayName("Add/Remove validates target field variations")
  void validate_addOrRemoveTargetVariations() {
    ObjectNode addAct = createBaseActivity("Add");
    addAct.put("object", "https://larpconnect.app/obj/1");
    addAct.remove("target");
    assertThat(validator.validate(addAct)).isPresent();
    addAct.put("target", 123);
    assertThat(validator.validate(addAct)).isPresent();
    addAct.put("target", "   ");
    assertThat(validator.validate(addAct)).isPresent();
  }

  @Test
  @DisplayName("Move validates origin and target field variations")
  void validate_moveFieldVariations() {
    ObjectNode moveAct = createBaseActivity("Move");
    moveAct.put("object", "https://larpconnect.app/obj/1");
    moveAct.put("origin", "https://larpconnect.app/col1");
    moveAct.put("target", "https://larpconnect.app/col2");
    moveAct.put("origin", 123);
    assertThat(validator.validate(moveAct)).isPresent();
    moveAct.put("origin", "   ");
    assertThat(validator.validate(moveAct)).isPresent();
    moveAct.put("origin", "https://larpconnect.app/col1").put("target", 123);
    assertThat(validator.validate(moveAct)).isPresent();
    moveAct.put("target", "   ");
    assertThat(validator.validate(moveAct)).isPresent();
  }

  @Test
  @DisplayName("Authority validates id/actor missing, non-textual, blank, or invalid URI")
  void validate_authorityVariations() {
    ObjectNode activity = createBaseActivity("Create");
    activity.set("object", mapper.createObjectNode());
    activity.remove("id");
    assertThat(validator.validate(activity)).isPresent();
    activity.put("id", 123);
    assertThat(validator.validate(activity)).isPresent();
    activity.put("id", "   ");
    assertThat(validator.validate(activity)).isPresent();
    activity.put("id", "https://larpconnect.app/act/1");
    activity.remove("actor");
    assertThat(validator.validate(activity)).isPresent();
    activity.put("actor", 456);
    assertThat(validator.validate(activity)).isPresent();
    activity.put("actor", "   ");
    assertThat(validator.validate(activity)).isPresent();
    activity.put("id", "http://invalid uri with spaces");
    activity.put("actor", "https://larpconnect.app/actor");
    assertThat(validator.validate(activity))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("not a valid URI"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/relative/path/1",
        "urn:isbn:0451450523",
        "http:opaque/path",
        "ftp://larpconnect.app/act/1",
        "https://bad_host/act/1",
        "https://user@larpconnect.app/act/1",
        "http://user:pass@larpconnect.app/act/1"
      })
  @DisplayName(
      "Relative, opaque, non-http, userinfo, and null-host URIs are rejected in authority check")
  void validate_authorityNonHttpOrMissingHost_rejected(String invalidUri) {
    ObjectNode badId = createBaseActivity("Create");
    badId.set("object", mapper.createObjectNode());
    badId.put("id", invalidUri);
    assertThat(validator.validate(badId))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("hierarchical http(s) URIs"));

    ObjectNode badActor = createBaseActivity("Create");
    badActor.set("object", mapper.createObjectNode());
    badActor.put("actor", invalidUri);
    assertThat(validator.validate(badActor))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("hierarchical http(s) URIs"));

    ObjectNode httpValid = createBaseActivity("Create");
    httpValid.set("object", mapper.createObjectNode());
    httpValid.put("id", "http://larpconnect.app/act/1");
    httpValid.put("actor", "http://larpconnect.app/actors/master");
    assertThat(validator.validate(httpValid)).isEmpty();
  }

  @Test
  @DisplayName("Created timestamp validates missing, non-textual, blank variations")
  void validate_createdFieldVariations() {
    ObjectNode activity = createBaseActivity("Create");
    activity.set("object", mapper.createObjectNode());
    activity.remove("created");
    assertThat(validator.validate(activity)).isPresent();
    activity.put("created", 123456);
    assertThat(validator.validate(activity)).isPresent();
    activity.put("created", "   ");
    assertThat(validator.validate(activity)).isPresent();
  }

  @Test
  @DisplayName("Recipients validation with cc only, both to and cc, and non-array cc")
  void validate_recipientCombinations() {
    ObjectNode activity = createBaseActivity("Create");
    activity.set("object", mapper.createObjectNode());
    activity.remove("to");

    ArrayNode cc = activity.putArray("cc");
    cc.add("https://larpconnect.app/api/studios/valiant/v1/actors/cc_target");
    assertThat(validator.validate(activity)).isEmpty();

    ArrayNode to = activity.putArray("to");
    to.add("https://larpconnect.app/api/studios/valiant/v1/actors/to_target");
    assertThat(validator.validate(activity)).isEmpty();

    activity.put("cc", "scalar-cc");
    assertThat(validator.validate(activity))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("cc' field must be an array"));

    activity.putArray("to"); // empty to
    activity.putArray("cc"); // empty cc
    assertThat(validator.validate(activity))
        .isPresent()
        .hasValueSatisfying(msg -> assertThat(msg).contains("at least one recipient"));
  }

  @Test
  @DisplayName("Activity updated timestamp validates format and ordering vs created")
  void validate_activityUpdatedFieldVariations() {
    ObjectNode activity = createBaseActivity("Create");
    activity.set("object", mapper.createObjectNode());

    activity.put("updated", FIXED_NOW.plusSeconds(30).toString());
    assertThat(validator.validate(activity)).isEmpty();
    activity.putNull("updated");
    assertThat(validator.validate(activity)).isEmpty();

    activity.put("updated", 123);
    assertThat(validator.validate(activity).orElseThrow()).contains("Malformed 'updated'");
    activity.put("updated", "   ");
    assertThat(validator.validate(activity).orElseThrow()).contains("Malformed 'updated'");
    activity.put("updated", "not-a-timestamp");
    assertThat(validator.validate(activity).orElseThrow()).contains("ISO-8601");
    activity.put("updated", FIXED_NOW.minusSeconds(10).toString());
    assertThat(validator.validate(activity).orElseThrow())
        .contains("updated timestamp cannot precede");
  }

  @Test
  @DisplayName("Embedded object created and updated timestamps validate format and ordering")
  void validate_embeddedObjectTimestamps() {
    ObjectNode activity = createBaseActivity("Create");
    ObjectNode obj = mapper.createObjectNode();
    obj.put("created", FIXED_NOW.minusSeconds(60).toString());
    obj.put("updated", FIXED_NOW.toString());
    activity.set("object", obj);
    assertThat(validator.validate(activity)).isEmpty();

    obj.put("updated", FIXED_NOW.minusSeconds(120).toString());
    assertThat(validator.validate(activity).orElseThrow())
        .contains("updated timestamp cannot precede");
    obj.put("created", "bad-created");
    obj.remove("updated");
    assertThat(validator.validate(activity).orElseThrow())
        .contains("Malformed 'created' timestamp");
    obj.putNull("created");
    obj.put("updated", FIXED_NOW.toString());
    assertThat(validator.validate(activity)).isEmpty();
    obj.remove("created");
    obj.put("updated", "bad-updated");
    assertThat(validator.validate(activity).orElseThrow())
        .contains("Malformed 'updated' timestamp");
  }

  @Test
  @DisplayName("Valid union-free array fields on activity and embedded object succeed")
  void validate_unionFreeArrayFields_validArraysSucceed() {
    ObjectNode activity = createBaseActivity("Create");
    activity.putObject("object").putArray("mixins").add("https://larpconnect.app/mixins/item");
    activity.putArray("audience").add("https://larpconnect.app/studios/valiant/followers");
    activity.putArray("mixins").add("https://larpconnect.app/mixins/quest");
    assertThat(validator.validate(activity)).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
    "false, audience, scalar, audience' field",
    "false, audience, number, non-blank strings",
    "false, mixins, scalar, mixins' field",
    "false, to, blank, Activity 'to' array",
    "true, mixins, scalar, Embedded object 'mixins'"
  })
  @DisplayName("Union-free array fields reject scalar, non-string, and blank elements")
  void validate_unionFreeArrayFieldInvalidValue_rejected(
      boolean embeddedObject, String field, String kind, String expectedMessage) {
    ObjectNode activity = createBaseActivity("Create");
    ObjectNode target =
        embeddedObject
            ? activity.putObject("object")
            : (ObjectNode) activity.set("object", mapper.createObjectNode());
    ObjectNode holder = embeddedObject ? target : activity;
    switch (kind) {
      case "scalar" -> holder.put(field, "scalar-value");
      case "number" -> holder.putArray(field).add(99);
      default -> holder.putArray(field).add("   ");
    }
    assertThat(validator.validate(activity).orElseThrow()).contains(expectedMessage);
  }
}
