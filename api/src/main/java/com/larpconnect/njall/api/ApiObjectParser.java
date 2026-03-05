package com.larpconnect.njall.api;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.proto.ApiObject;
import io.vertx.core.json.JsonObject;

/** Interface for converting between ApiObject protobuf messages and Vert.x JsonObjects. */
@DefaultImplementation(DefaultApiObjectParser.class)
public interface ApiObjectParser {

  /** Converts an ApiObject to a Vert.x JsonObject, lifting extensions to the top level. */
  JsonObject toJson(ApiObject obj);

  /** Converts a Vert.x JsonObject back to an ApiObject. */
  ApiObject fromJson(JsonObject json);
}
