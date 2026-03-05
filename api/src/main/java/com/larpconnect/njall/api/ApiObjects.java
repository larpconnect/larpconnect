package com.larpconnect.njall.api;

import com.larpconnect.njall.proto.ApiObject;
import io.vertx.core.json.JsonObject;

/**
 * Public factory class for ApiObject interactions to satisfy ArchUnit module encapsulation rules.
 */
public final class ApiObjects {
  private ApiObjects() {}

  /** Converts an ApiObject to a Vert.x JsonObject. */
  public static JsonObject toJson(ApiObject obj) {
    return ApiObjectConverter.toJson(obj);
  }

  /** Converts a Vert.x JsonObject back to an ApiObject. */
  public static ApiObject fromJson(JsonObject json) {
    return ApiObjectConverter.fromJson(json);
  }
}
