package com.larpconnect.njall.api;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.proto.ApiObject;
import io.vertx.core.json.JsonObject;

/**
 * Bridges the gap between internal type-safe Protobuf representations and the external schema-less
 * JSON API format.
 *
 * <p>The system uses Protobuf for strict internal contracts and efficient binary storage, but must
 * communicate with clients using a flattened JSON structure where polymorphic extensions (like
 * Document, Event, or Link) appear at the root level. This parser handles that impedance mismatch
 * by projecting structured Protobuf extensions into a flat JSON map during serialization, and
 * dynamically reconstructing the type-safe extensions from incoming JSON payloads based on type
 * metadata.
 */
@DefaultImplementation(DefaultApiObjectParser.class)
public interface ApiObjectParser {

  /**
   * Converts a structured {@link ApiObject} into a flattened {@link JsonObject}.
   *
   * <p>In the internal Protobuf definition, domain-specific fields are nested inside the {@code
   * extensions} map to maintain type safety. This method flattens those nested structures into the
   * root JSON level to match the external API specification expected by clients. For example, a
   * Document's media type and content will appear as top-level JSON fields.
   *
   * @param obj the internal type-safe representation
   * @return the flattened JSON representation suitable for HTTP responses
   * @throws IllegalStateException if the underlying Protobuf conversion fails
   */
  JsonObject toJson(ApiObject obj);

  /**
   * Reconstructs an {@link ApiObject} from a flattened incoming {@link JsonObject}.
   *
   * <p>This process reverses {@link #toJson(ApiObject)} by examining the {@code type} array field
   * in the JSON payload (e.g., "Document", "Event") and routing the flat fields into the
   * appropriate strongly-typed Protobuf extension message. It also handles special cases, such as
   * embedding raw text content into the {@code Document} extension.
   *
   * @param json the flattened JSON payload from an HTTP request
   * @return the reconstructed type-safe internal representation
   * @throws IllegalArgumentException if the JSON structure is malformed or conversion fails
   */
  ApiObject fromJson(JsonObject json);
}
