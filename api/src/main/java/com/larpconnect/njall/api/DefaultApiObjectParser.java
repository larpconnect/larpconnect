package com.larpconnect.njall.api;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.larpconnect.njall.common.annotations.BuildWith;
import com.larpconnect.njall.proto.ApiObject;
import com.larpconnect.njall.proto.Document;
import com.larpconnect.njall.proto.Event;
import com.larpconnect.njall.proto.Extension;
import com.larpconnect.njall.proto.Link;
import com.larpconnect.njall.proto.OrderedCollectionPage;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;

@BuildWith(ApiModule.class)
final class DefaultApiObjectParser implements ApiObjectParser {

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
      var json = new JsonObject(printer.print(obj));
      var extensions = json.getJsonObject("extensions");
      json.remove("extensions");

      if (extensions != null) {
        extensions.fieldNames().stream()
            .map(extensions::getJsonObject)
            .filter(extMap -> !extMap.isEmpty())
            .forEach(
                extMap ->
                    json.mergeIn(extMap.getJsonObject(extMap.fieldNames().iterator().next())));
      }

      // Handle raw content override for Document
      if (obj.getExtensionsMap().containsKey("document")) {
        var doc = obj.getExtensionsMap().get("document").getDocument();
        if (doc.getMediaType().startsWith("text/")) {
          json.put("content", doc.getContent().toStringUtf8());
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
      var jsonString = json.encode();
      var builder = ApiObject.newBuilder();
      parser.merge(jsonString, builder);

      if (builder.hasDeleted()) {
        return builder.build();
      }

      var types = json.getJsonArray("type");
      if (types != null) {
        for (int i = 0; i < types.size(); i++) {
          var type = types.getString(i);
          switch (type) {
            case "Document" -> {
              var docBuilder = Document.newBuilder();
              var mediaTypeStr = json.getString("media_type", json.getString("mediaType"));
              boolean isRawText =
                  json.containsKey("content")
                      && mediaTypeStr != null
                      && mediaTypeStr.startsWith("text/");
              if (isRawText) {
                var rawContent = json.getString("content");
                var safeJson = json.copy();
                safeJson.remove("content");
                parser.merge(safeJson.encode(), docBuilder);
                docBuilder.setContent(ByteString.copyFromUtf8(rawContent));
              } else {
                parser.merge(jsonString, docBuilder);
              }
              builder.putExtensions(
                  "document", Extension.newBuilder().setDocument(docBuilder).build());
            }
            case "Event" -> {
              var evtBuilder = Event.newBuilder();
              parser.merge(jsonString, evtBuilder);
              builder.putExtensions("event", Extension.newBuilder().setEvent(evtBuilder).build());
            }
            case "Link" -> {
              var linkBuilder = Link.newBuilder();
              parser.merge(jsonString, linkBuilder);
              builder.putExtensions("link", Extension.newBuilder().setLink(linkBuilder).build());
            }
            case "OrderedCollectionPage" -> {
              var ocpBuilder = OrderedCollectionPage.newBuilder();
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
