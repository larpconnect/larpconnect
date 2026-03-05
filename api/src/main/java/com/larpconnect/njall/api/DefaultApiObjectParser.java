package com.larpconnect.njall.api;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.larpconnect.njall.proto.ApiObject;
import com.larpconnect.njall.proto.Document;
import com.larpconnect.njall.proto.Event;
import com.larpconnect.njall.proto.Extension;
import com.larpconnect.njall.proto.Link;
import com.larpconnect.njall.proto.OrderedCollectionPage;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;

public final class DefaultApiObjectParser implements ApiObjectParser {

  private final JsonFormat.Printer printer;
  private final JsonFormat.Parser parser;

  @Inject
  DefaultApiObjectParser(JsonFormat.Printer printer, JsonFormat.Parser parser) {
    this.printer = printer;
    this.parser = parser;
  }

  @Override
  public JsonObject toJson(ApiObject obj) {
    try {
      JsonObject json = new JsonObject(printer.print(obj));
      JsonObject extensions = json.getJsonObject("extensions");
      json.remove("extensions");

      if (extensions != null) {
        for (String key : extensions.fieldNames()) {
          JsonObject extMap = extensions.getJsonObject(key);
          if (extMap != null && !extMap.isEmpty()) {
            String innerKey = extMap.fieldNames().iterator().next();
            JsonObject extData = extMap.getJsonObject(innerKey);
            if (extData != null) {
              json.mergeIn(extData);
            }
          }
        }
      }
      return json;
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalStateException("Failed to convert ApiObject to JSON", e);
    }
  }

  @Override
  public ApiObject fromJson(JsonObject json) {
    try {
      String jsonString = json.encode();
      ApiObject.Builder builder = ApiObject.newBuilder();
      parser.merge(jsonString, builder);

      if (builder.hasDeleted()) {
        return builder.build();
      }

      JsonArray types = json.getJsonArray("type");
      if (types != null) {
        for (int i = 0; i < types.size(); i++) {
          String type = types.getString(i);
          switch (type) {
            case "Document" -> {
              Document.Builder docBuilder = Document.newBuilder();
              parser.merge(jsonString, docBuilder);
              builder.putExtensions(
                  "document", Extension.newBuilder().setDocument(docBuilder).build());
            }
            case "Event" -> {
              Event.Builder evtBuilder = Event.newBuilder();
              parser.merge(jsonString, evtBuilder);
              builder.putExtensions("event", Extension.newBuilder().setEvent(evtBuilder).build());
            }
            case "Link" -> {
              Link.Builder linkBuilder = Link.newBuilder();
              parser.merge(jsonString, linkBuilder);
              builder.putExtensions("link", Extension.newBuilder().setLink(linkBuilder).build());
            }
            case "OrderedCollectionPage" -> {
              OrderedCollectionPage.Builder ocpBuilder = OrderedCollectionPage.newBuilder();
              parser.merge(jsonString, ocpBuilder);
              builder.putExtensions(
                  "ordered_collection_page",
                  Extension.newBuilder().setOrderedCollectionPage(ocpBuilder).build());
            }
            default -> {}
          }
        }
      }

      return builder.build();
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException("Failed to parse ApiObject from JSON", e);
    }
  }
}
