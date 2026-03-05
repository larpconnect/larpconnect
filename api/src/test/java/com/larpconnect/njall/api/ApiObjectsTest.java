package com.larpconnect.njall.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import com.larpconnect.njall.proto.ApiObject;
import com.larpconnect.njall.proto.Document;
import com.larpconnect.njall.proto.Event;
import com.larpconnect.njall.proto.Extension;
import com.larpconnect.njall.proto.Link;
import com.larpconnect.njall.proto.OrderedCollectionPage;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.time.Instant;
import org.junit.jupiter.api.Test;

public class ApiObjectsTest {

  @Test
  void toJson_handlesAllFieldsAndExtensions_successfully() {
    ApiObject obj =
        ApiObject.newBuilder()
            .addContext("https://www.w3.org/ns/activitystreams")
            .addContext("https://example.com/other")
            .setId("https://example.com/obj/1")
            .addType("Document")
            .addType("Note")
            .setName("A test document")
            .setDeleted(Timestamp.newBuilder().setSeconds(1672531200).setNanos(0).build())
            .putExtensions(
                "document",
                Extension.newBuilder()
                    .setDocument(
                        Document.newBuilder()
                            .setMediaType("text/plain")
                            .setContent(ByteString.copyFromUtf8("hello world"))
                            .build())
                    .build())
            .putExtensions(
                "event",
                Extension.newBuilder()
                    .setEvent(
                        Event.newBuilder()
                            .setStartTime(Timestamp.newBuilder().setSeconds(1000).build())
                            .setEndTime(Timestamp.newBuilder().setSeconds(2000).build())
                            .build())
                    .build())
            .putExtensions(
                "link",
                Extension.newBuilder()
                    .setLink(
                        Link.newBuilder()
                            .setHref("https://example.com/href")
                            .addRel("self")
                            .setMediaType("text/html")
                            .build())
                    .build())
            .putExtensions(
                "ordered_collection_page",
                Extension.newBuilder()
                    .setOrderedCollectionPage(
                        OrderedCollectionPage.newBuilder()
                            .setTotalItems(42)
                            .setFirst("first")
                            .setLast("last")
                            .setPartOf("partOf")
                            .setNext("next")
                            .setPrev("prev")
                            .addItems("item1")
                            .addItems("item2")
                            .build())
                    .build())
            .putExtensions("unknown", Extension.getDefaultInstance()) // Should be ignored
            .build();

    JsonObject json = ApiObjects.toJson(obj);

    assertThat(json.getJsonArray("@context"))
        .containsExactly("https://www.w3.org/ns/activitystreams", "https://example.com/other");
    assertThat(json.getString("id")).isEqualTo("https://example.com/obj/1");
    assertThat(json.getJsonArray("type")).containsExactly("Document", "Note");
    assertThat(json.getString("name")).isEqualTo("A test document");
    assertThat(json.getString("deleted")).isEqualTo("2023-01-01T00:00:00Z");

    JsonObject docExt = json.getJsonObject("document");
    assertThat(docExt.getString("media_type")).isEqualTo("text/plain");
    assertThat(docExt.getString("content")).isEqualTo("aGVsbG8gd29ybGQ=");

    JsonObject evtExt = json.getJsonObject("event");
    assertThat(evtExt.getString("start_time")).isEqualTo("1970-01-01T00:16:40Z");

    JsonObject linkExt = json.getJsonObject("link");
    assertThat(linkExt.getString("href")).isEqualTo("https://example.com/href");

    JsonObject ocpExt = json.getJsonObject("ordered_collection_page");
    assertThat(ocpExt.getString("total_items")).isEqualTo("42");

    assertThat(json.containsKey("unknown")).isFalse();
  }

  @Test
  void toJson_handlesEmptyObject_successfully() {
    ApiObject obj = ApiObject.getDefaultInstance();
    JsonObject json = ApiObjects.toJson(obj);
    assertThat(json.isEmpty()).isTrue();
  }

  @Test
  void fromJson_handlesAllFieldsAndExtensions_successfully() {
    JsonObject json =
        new JsonObject()
            .put(
                "@context",
                new JsonArray().add("https://www.w3.org/ns/activitystreams").add("other"))
            .put("id", "https://example.com/obj/1")
            .put("type", new JsonArray().add("Document").add("Note"))
            .put("name", "A test document")
            .put("deleted", "2023-01-01T00:00:00Z")
            .put(
                "document",
                new JsonObject().put("media_type", "text/plain").put("content", "aGVsbG8gd29ybGQ="))
            .put("event", new JsonObject().put("start_time", "1970-01-01T00:16:40Z"))
            .put("link", new JsonObject().put("href", "https://example.com/href"))
            .put("ordered_collection_page", new JsonObject().put("total_items", "42"));

    ApiObject obj = ApiObjects.fromJson(json);

    assertThat(obj.getContextList())
        .containsExactly("https://www.w3.org/ns/activitystreams", "other");
    assertThat(obj.getId()).isEqualTo("https://example.com/obj/1");
    assertThat(obj.getTypeList()).containsExactly("Document", "Note");
    assertThat(obj.getName()).isEqualTo("A test document");

    Instant expectedTime = Instant.parse("2023-01-01T00:00:00Z");
    assertThat(obj.getDeleted().getSeconds()).isEqualTo(expectedTime.getEpochSecond());

    assertThat(obj.getExtensionsMap().get("document").getDocument().getMediaType())
        .isEqualTo("text/plain");
    assertThat(obj.getExtensionsMap().get("event").getEvent().getStartTime().getSeconds())
        .isEqualTo(1000);
    assertThat(obj.getExtensionsMap().get("link").getLink().getHref())
        .isEqualTo("https://example.com/href");
    assertThat(
            obj.getExtensionsMap()
                .get("ordered_collection_page")
                .getOrderedCollectionPage()
                .getTotalItems())
        .isEqualTo(42);
  }

  @Test
  void fromJson_handlesEmptyObject_successfully() {
    ApiObject obj = ApiObjects.fromJson(new JsonObject());
    assertThat(obj.getId()).isEmpty();
  }

  @Test
  void fromJson_throwsIllegalArgumentException_whenExtensionIsInvalid() {
    JsonObject json =
        new JsonObject()
            .put("document", new JsonObject().put("unknown_field_to_crash_parser", 123));
    assertThatThrownBy(() -> ApiObjects.fromJson(json))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
