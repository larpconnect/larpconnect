package com.larpconnect.njall.api;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import com.larpconnect.njall.proto.ApiObject;
import com.larpconnect.njall.proto.Document;
import com.larpconnect.njall.proto.Event;
import com.larpconnect.njall.proto.Extension;
import com.larpconnect.njall.proto.Link;
import com.larpconnect.njall.proto.OrderedCollectionPage;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.time.Instant;

/** Converts between ApiObject protobuf messages and Vert.x JsonObjects. */
final class ApiObjectConverter {

  private static final JsonFormat.Printer PRINTER =
      JsonFormat.printer().preservingProtoFieldNames().omittingInsignificantWhitespace();
  private static final JsonFormat.Parser PARSER =
      JsonFormat.parser(); // Removed ignoreUnknownFields to fix the test throwing Exception

  private ApiObjectConverter() {}

  /** Converts an ApiObject to a Vert.x JsonObject, lifting extensions to the top level. */
  public static JsonObject toJson(ApiObject obj) {
    JsonObject json = new JsonObject();
    addContext(obj, json);
    addBasicFields(obj, json);
    addExtensions(obj, json);
    return json;
  }

  private static void addContext(ApiObject obj, JsonObject json) {
    if (obj.getContextCount() > 0) {
      JsonArray contextArray = new JsonArray();
      obj.getContextList().forEach(contextArray::add);
      json.put("@context", contextArray);
    }
  }

  private static void addBasicFields(ApiObject obj, JsonObject json) {
    if (!obj.getId().isEmpty()) {
      json.put("id", obj.getId());
    }
    if (obj.getTypeCount() > 0) {
      JsonArray typeArray = new JsonArray();
      obj.getTypeList().forEach(typeArray::add);
      json.put("type", typeArray);
    }
    if (!obj.getName().isEmpty()) {
      json.put("name", obj.getName());
    }
    if (obj.hasDeleted()) {
      Timestamp ts = obj.getDeleted();
      json.put("deleted", Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos()).toString());
    }
  }

  private static void addExtensions(ApiObject obj, JsonObject json) {
    for (var entry : obj.getExtensionsMap().entrySet()) {
      String key = entry.getKey();
      Extension ext = entry.getValue();
      try {
        String extJsonStr =
            switch (ext.getExtensionCase()) {
              case DOCUMENT -> PRINTER.print(ext.getDocument());
              case EVENT -> PRINTER.print(ext.getEvent());
              case LINK -> PRINTER.print(ext.getLink());
              case ORDERED_COLLECTION_PAGE -> PRINTER.print(ext.getOrderedCollectionPage());
              default -> "";
            };
        if (!extJsonStr.isEmpty()) {
          json.put(key, new JsonObject(extJsonStr));
        }
      } catch (InvalidProtocolBufferException e) {
        throw new IllegalStateException("Failed to convert extension to JSON", e);
      }
    }
  }

  /** Converts a Vert.x JsonObject back to an ApiObject. */
  public static ApiObject fromJson(JsonObject json) {
    ApiObject.Builder builder = ApiObject.newBuilder();
    parseContext(json, builder);
    parseBasicFields(json, builder);
    parseExtensions(json, builder);
    return builder.build();
  }

  private static void parseContext(JsonObject json, ApiObject.Builder builder) {
    if (json.containsKey("@context")) {
      json.getJsonArray("@context").forEach(c -> builder.addContext((String) c));
    }
  }

  private static void parseBasicFields(JsonObject json, ApiObject.Builder builder) {
    if (json.containsKey("id")) {
      builder.setId(json.getString("id"));
    }
    if (json.containsKey("type")) {
      json.getJsonArray("type").forEach(t -> builder.addType((String) t));
    }
    if (json.containsKey("name")) {
      builder.setName(json.getString("name"));
    }
    if (json.containsKey("deleted")) {
      Instant instant = Instant.parse(json.getString("deleted"));
      builder.setDeleted(
          Timestamp.newBuilder()
              .setSeconds(instant.getEpochSecond())
              .setNanos(instant.getNano())
              .build());
    }
  }

  private static void parseExtensions(JsonObject json, ApiObject.Builder builder) {
    try {
      if (json.containsKey("document")) {
        Document.Builder docBuilder = Document.newBuilder();
        PARSER.merge(json.getJsonObject("document").encode(), docBuilder);
        builder.putExtensions("document", Extension.newBuilder().setDocument(docBuilder).build());
      }
      if (json.containsKey("event")) {
        Event.Builder evtBuilder = Event.newBuilder();
        PARSER.merge(json.getJsonObject("event").encode(), evtBuilder);
        builder.putExtensions("event", Extension.newBuilder().setEvent(evtBuilder).build());
      }
      if (json.containsKey("link")) {
        Link.Builder linkBuilder = Link.newBuilder();
        PARSER.merge(json.getJsonObject("link").encode(), linkBuilder);
        builder.putExtensions("link", Extension.newBuilder().setLink(linkBuilder).build());
      }
      if (json.containsKey("ordered_collection_page")) {
        OrderedCollectionPage.Builder ocpBuilder = OrderedCollectionPage.newBuilder();
        PARSER.merge(json.getJsonObject("ordered_collection_page").encode(), ocpBuilder);
        builder.putExtensions(
            "ordered_collection_page",
            Extension.newBuilder().setOrderedCollectionPage(ocpBuilder).build());
      }
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException("Failed to parse extension from JSON", e);
    }
  }
}
