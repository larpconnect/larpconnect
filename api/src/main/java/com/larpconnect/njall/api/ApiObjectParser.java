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

  /** Converts an ApiObject to a Vert.x JsonObject, lifting extensions to the top level. */
  JsonObject toJson(ApiObject obj);

  /** Converts a Vert.x JsonObject back to an ApiObject. */
  ApiObject fromJson(JsonObject json);
}
