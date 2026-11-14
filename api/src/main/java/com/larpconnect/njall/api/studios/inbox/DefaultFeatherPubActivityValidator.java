package com.larpconnect.njall.api.studios.inbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** Default implementation of {@link FeatherPubActivityValidator}. */
final class DefaultFeatherPubActivityValidator implements FeatherPubActivityValidator {

  private static final String FEATHERPUB_CONTEXT = "https://larpconnect.app/schemas/featherpub";
  private static final Duration MAX_FUTURE_SKEW = Duration.ofMinutes(5);
  private static final Duration MAX_PAST_AGE = Duration.ofDays(8);

  private static final Set<String> VERBS =
      ImmutableSet.of(
          "Create",
          "Update",
          "Delete",
          "Add",
          "Remove",
          "Move",
          "Offer",
          "Accept",
          "Reject",
          "Announce");

  private final Clock clock;

  @Inject
  DefaultFeatherPubActivityValidator(Clock clock) {
    this.clock = clock;
  }

  @Override
  public Optional<String> validate(JsonNode activity) {
    if (!activity.isObject()) {
      return Optional.of("Activity payload must be a JSON object");
    }

    return validateContext(activity.get("@context"))
        .or(() -> validateVerbAndFields(activity))
        .or(() -> validateAuthority(activity.get("id"), activity.get("actor")))
        .or(() -> validateTemporalBounds(activity.get("created")))
        .or(() -> validateUpdatedTimestamp(activity))
        .or(() -> validateRecipients(activity.get("to"), activity.get("cc")))
        .or(() -> validateOptionalMetadataArrays(activity));
  }

  private Optional<String> validateContext(@Nullable JsonNode contextNode) {
    if (contextNode == null || !contextNode.isArray()) {
      return Optional.of("@context must be an array containing '" + FEATHERPUB_CONTEXT + "'");
    }
    boolean foundSchema = false;
    for (JsonNode item : contextNode) {
      if (!item.isTextual() || item.asText().isBlank()) {
        return Optional.of("@context array elements must be non-blank strings");
      }
      if (FEATHERPUB_CONTEXT.equals(item.asText())) {
        foundSchema = true;
      }
    }
    return foundSchema
        ? Optional.empty()
        : Optional.of("@context array must contain '" + FEATHERPUB_CONTEXT + "'");
  }

  private Optional<String> validateVerbAndFields(JsonNode activity) {
    @Nullable JsonNode typeNode = activity.get("type");
    if (typeNode == null || !typeNode.isTextual() || typeNode.asText().isBlank()) {
      return Optional.of("Activity type is required");
    }
    String verb = typeNode.asText();
    if (!VERBS.contains(verb)) {
      return Optional.of("Unsupported activity verb: " + verb);
    }
    return validateVerbSpecificFields(verb, activity);
  }

  private Optional<String> validateVerbSpecificFields(String verb, JsonNode activity) {
    return switch (verb) {
      case "Create", "Update" -> validateCreateOrUpdate(activity);
      case "Delete", "Offer", "Accept", "Reject", "Announce" -> validateObjectPresent(activity);
      case "Add", "Remove" -> validateAddOrRemove(activity);
      case "Move" -> validateMove(activity);
      default -> Optional.of("Unsupported activity verb: " + verb);
    };
  }

  private Optional<String> validateCreateOrUpdate(JsonNode activity) {
    @Nullable JsonNode objectNode = activity.get("object");
    if (objectNode == null || !objectNode.isObject()) {
      return Optional.of("Activity 'object' must be a JSON object for Create/Update");
    }
    return Optional.empty();
  }

  private Optional<String> validateObjectPresent(JsonNode activity) {
    @Nullable JsonNode objectNode = activity.get("object");
    if (objectNode == null
        || (!objectNode.isObject() && (!objectNode.isTextual() || objectNode.asText().isBlank()))) {
      return Optional.of("Activity 'object' must be a valid URI or JSON object");
    }
    return Optional.empty();
  }

  private Optional<String> validateAddOrRemove(JsonNode activity) {
    var objError = validateObjectPresent(activity);
    if (objError.isPresent()) {
      return objError;
    }
    @Nullable JsonNode targetNode = activity.get("target");
    if (targetNode == null || !targetNode.isTextual() || targetNode.asText().isBlank()) {
      return Optional.of("Activity 'target' URI is required for Add/Remove");
    }
    return Optional.empty();
  }

  private Optional<String> validateMove(JsonNode activity) {
    var objError = validateObjectPresent(activity);
    if (objError.isPresent()) {
      return objError;
    }
    @Nullable JsonNode originNode = activity.get("origin");
    if (originNode == null || !originNode.isTextual() || originNode.asText().isBlank()) {
      return Optional.of("Activity 'origin' URI is required for Move");
    }
    @Nullable JsonNode targetNode = activity.get("target");
    if (targetNode == null || !targetNode.isTextual() || targetNode.asText().isBlank()) {
      return Optional.of("Activity 'target' URI is required for Move");
    }
    return Optional.empty();
  }

  private Optional<String> validateAuthority(
      @Nullable JsonNode idNode, @Nullable JsonNode actorNode) {
    if (idNode == null || !idNode.isTextual() || idNode.asText().isBlank()) {
      return Optional.of("Activity 'id' URI is required");
    }
    if (actorNode == null || !actorNode.isTextual() || actorNode.asText().isBlank()) {
      return Optional.of("Activity 'actor' URI is required");
    }
    try {
      URI idUri = URI.create(idNode.asText().trim());
      URI actorUri = URI.create(actorNode.asText().trim());
      return validateUriHosts(idUri, actorUri);
    } catch (IllegalArgumentException ex) {
      return Optional.of("Activity 'id' or 'actor' is not a valid URI");
    }
  }

  private Optional<String> validateUriHosts(URI idUri, URI actorUri) {
    String idHost = extractValidatedHttpHost(idUri);
    String actorHost = extractValidatedHttpHost(actorUri);
    if (idHost == null || actorHost == null) {
      return Optional.of(
          "Activity 'id' and 'actor' must be hierarchical http(s) URIs with a valid host");
    }
    String lowerId = idHost.toLowerCase(Locale.ROOT);
    String lowerActor = actorHost.toLowerCase(Locale.ROOT);
    if (!lowerId.equals(lowerActor) && !lowerId.endsWith("." + lowerActor)) {
      return Optional.of("Activity 'id' host must match or be a subdomain of 'actor' host");
    }
    return Optional.empty();
  }

  private static @Nullable String extractValidatedHttpHost(URI uri) {
    String scheme = uri.getScheme();
    if (uri.isOpaque()
        || uri.getRawUserInfo() != null
        || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
      return null;
    }
    return uri.getHost();
  }

  private Optional<String> validateTemporalBounds(@Nullable JsonNode createdNode) {
    if (createdNode == null || !createdNode.isTextual() || createdNode.asText().isBlank()) {
      return Optional.of("Activity 'created' timestamp is required");
    }
    Instant created;
    try {
      created = Instant.parse(createdNode.asText());
    } catch (DateTimeParseException ex) {
      return Optional.of("Activity 'created' must be an ISO-8601 UTC timestamp");
    }
    Instant now = clock.instant();
    if (created.isAfter(now.plus(MAX_FUTURE_SKEW))) {
      return Optional.of("Activity 'created' timestamp exceeds maximum future skew of 5 minutes");
    }
    if (created.isBefore(now.minus(MAX_PAST_AGE))) {
      return Optional.of("Activity 'created' timestamp is older than 8 days");
    }
    return Optional.empty();
  }

  private Optional<String> validateUpdatedTimestamp(JsonNode activity) {
    var actCheck = checkCreatedUpdated(activity.get("created"), activity.get("updated"));
    if (actCheck.isPresent()) {
      return actCheck;
    }
    @Nullable JsonNode obj = activity.get("object");
    if (obj != null && obj.isObject()) {
      return checkCreatedUpdated(obj.get("created"), obj.get("updated"));
    }
    return Optional.empty();
  }

  private Optional<String> checkCreatedUpdated(
      @Nullable JsonNode createdNode, @Nullable JsonNode updatedNode) {
    Optional<Instant> createdOpt = Optional.empty();
    if (createdNode != null && !createdNode.isNull()) {
      createdOpt = parseIsoTimestamp(createdNode);
      if (createdOpt.isEmpty()) {
        return Optional.of("Malformed 'created' timestamp: must be an ISO-8601 UTC timestamp");
      }
    }
    if (updatedNode == null || updatedNode.isNull()) {
      return Optional.empty();
    }
    var updatedOpt = parseIsoTimestamp(updatedNode);
    if (updatedOpt.isEmpty()) {
      return Optional.of("Malformed 'updated' timestamp: must be an ISO-8601 UTC timestamp");
    }
    if (createdOpt.isPresent() && updatedOpt.orElseThrow().isBefore(createdOpt.orElseThrow())) {
      return Optional.of("Temporal coherence violation: updated timestamp cannot precede created");
    }
    return Optional.empty();
  }

  private Optional<Instant> parseIsoTimestamp(JsonNode node) {
    if (!node.isTextual() || node.asText().isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(Instant.parse(node.asText()));
    } catch (DateTimeParseException ex) {
      return Optional.empty();
    }
  }

  private Optional<String> validateRecipients(
      @Nullable JsonNode toNode, @Nullable JsonNode ccNode) {
    if (toNode == null && ccNode == null) {
      return Optional.of("Activity must include 'to' or 'cc' recipients array");
    }
    return validateStringArrayField(toNode, "Activity 'to'")
        .or(() -> validateStringArrayField(ccNode, "Activity 'cc'"))
        .or(() -> checkNonEmptyRecipients(toNode, ccNode));
  }

  private Optional<String> validateOptionalMetadataArrays(JsonNode activity) {
    return validateStringArrayField(activity.get("audience"), "Activity 'audience'")
        .or(() -> validateStringArrayField(activity.get("mixins"), "Activity 'mixins'"))
        .or(() -> validateEmbeddedObjectArrays(activity.get("object")));
  }

  private Optional<String> validateEmbeddedObjectArrays(@Nullable JsonNode obj) {
    if (obj == null || !obj.isObject()) {
      return Optional.empty();
    }
    return validateStringArrayField(obj.get("to"), "Embedded object 'to'")
        .or(() -> validateStringArrayField(obj.get("cc"), "Embedded object 'cc'"))
        .or(() -> validateStringArrayField(obj.get("audience"), "Embedded object 'audience'"))
        .or(() -> validateStringArrayField(obj.get("mixins"), "Embedded object 'mixins'"));
  }

  private Optional<String> validateStringArrayField(@Nullable JsonNode node, String fieldLabel) {
    if (node == null) {
      return Optional.empty();
    }
    if (!node.isArray()) {
      return Optional.of(fieldLabel + " field must be an array");
    }
    for (JsonNode item : node) {
      if (!item.isTextual() || item.asText().isBlank()) {
        return Optional.of(fieldLabel + " array elements must be non-blank strings");
      }
    }
    return Optional.empty();
  }

  private Optional<String> checkNonEmptyRecipients(
      @Nullable JsonNode toNode, @Nullable JsonNode ccNode) {
    boolean hasTo = toNode != null && !toNode.isEmpty();
    boolean hasCc = ccNode != null && !ccNode.isEmpty();
    if (!hasTo && !hasCc) {
      return Optional.of("Activity must specify at least one recipient in 'to' or 'cc'");
    }
    return Optional.empty();
  }
}
