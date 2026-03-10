package com.larpconnect.njall.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import com.larpconnect.njall.proto.ApiObject;
import com.larpconnect.njall.proto.Document;
import com.larpconnect.njall.proto.Extension;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class DefaultApiObjectParserTest {

  private ApiObjectParser parser;

  @BeforeEach
  void setUp() {
    parser =
        new DefaultApiObjectParser(
            JsonFormat.printer().preservingProtoFieldNames().omittingInsignificantWhitespace(),
            JsonFormat.parser().ignoringUnknownFields());
  }

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
            .putExtensions("unknown", Extension.getDefaultInstance()) // empty extension test
            .build();

    JsonObject json = parser.toJson(obj);

    assertThat(json.getJsonArray("context"))
        .containsExactly("https://www.w3.org/ns/activitystreams", "https://example.com/other");
    assertThat(json.getString("id")).isEqualTo("https://example.com/obj/1");
    assertThat(json.getJsonArray("type")).containsExactly("Document", "Note");
    assertThat(json.getString("name")).isEqualTo("A test document");
    assertThat(json.getString("deleted")).isEqualTo("2023-01-01T00:00:00Z");

    assertThat(json.getString("media_type")).isEqualTo("text/plain");
    assertThat(json.getString("content")).isEqualTo("hello world");
    assertThat(json.containsKey("extensions")).isFalse();
  }

  @Test
  void toJson_handlesEmptyObject_successfully() {
    ApiObject obj = ApiObject.getDefaultInstance();
    JsonObject json = parser.toJson(obj);
    assertThat(json.isEmpty()).isTrue();
  }

  @Test
  void toJson_handlesDocumentWithBase64Content_successfully() {
    ApiObject obj =
        ApiObject.newBuilder()
            .addType("Document")
            .putExtensions(
                "document",
                Extension.newBuilder()
                    .setDocument(
                        Document.newBuilder()
                            .setMediaType("image/png")
                            .setContent(
                                ByteString.copyFromUtf8(
                                    "hello world")) // base64 is aGVsbG8gd29ybGQ=
                            .build())
                    .build())
            .build();

    JsonObject json = parser.toJson(obj);
    assertThat(json.getString("media_type")).isEqualTo("image/png");
    assertThat(json.getString("content")).isEqualTo("aGVsbG8gd29ybGQ=");
  }

  @Test
  void toJson_throwsIllegalStateException_whenPrinterFails() throws Exception {
    JsonFormat.Printer mockPrinter = mock(JsonFormat.Printer.class);
    when(mockPrinter.print(any(ApiObject.class)))
        .thenThrow(new InvalidProtocolBufferException("Mock exception"));

    ApiObjectParser testParser =
        new DefaultApiObjectParser(mockPrinter, JsonFormat.parser().ignoringUnknownFields());

    assertThatThrownBy(() -> testParser.toJson(ApiObject.getDefaultInstance()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Failed to convert ApiObject to JSON")
        .hasCauseInstanceOf(InvalidProtocolBufferException.class);
  }

  @Test
  void fromJson_handlesAllFieldsAndExtensions_successfully() {
    var json =
        new JsonObject()
            .put(
                "context",
                new JsonArray().add("https://www.w3.org/ns/activitystreams").add("other"))
            .put("id", "https://example.com/obj/1")
            .put(
                "type",
                new JsonArray()
                    .add("Document")
                    .add("Note")
                    .add("Event")
                    .add("Link")
                    .add("OrderedCollectionPage")
                    .add("UnknownType")) // Also testing default switch case branch
            .put("name", "A test document")
            .put("media_type", "text/plain")
            .put("content", "hello world")
            .put("start_time", "1970-01-01T00:16:40Z")
            .put("href", "https://example.com/href")
            .put("total_items", "42");

    ApiObject obj = parser.fromJson(json);

    assertThat(obj.getContextList())
        .containsExactly("https://www.w3.org/ns/activitystreams", "other");
    assertThat(obj.getId()).isEqualTo("https://example.com/obj/1");
    assertThat(obj.getTypeList())
        .containsExactly(
            "Document", "Note", "Event", "Link", "OrderedCollectionPage", "UnknownType");
    assertThat(obj.getName()).isEqualTo("A test document");

    assertThat(obj.hasDeleted()).isFalse();

    assertThat(obj.getExtensionsMap().get("document").getDocument().getMediaType())
        .isEqualTo("text/plain");
    assertThat(obj.getExtensionsMap().get("document").getDocument().getContent().toStringUtf8())
        .isEqualTo("hello world");
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
  void fromJson_handlesDocumentWithBase64Content_successfully() {
    var json =
        new JsonObject()
            .put("type", new JsonArray().add("Document"))
            .put("media_type", "image/png")
            .put("content", "aGVsbG8gd29ybGQ=");

    ApiObject obj = parser.fromJson(json);
    assertThat(obj.getExtensionsMap().get("document").getDocument().getContent().toStringUtf8())
        .isEqualTo("hello world");
  }

  @Test
  void fromJson_handlesDocumentWithCamelCaseMediaType() {
    var json =
        new JsonObject()
            .put("type", new JsonArray().add("Document"))
            .put("mediaType", "text/plain")
            .put("content", "hello camel case");

    ApiObject obj = parser.fromJson(json);
    assertThat(obj.getExtensionsMap().get("document").getDocument().getContent().toStringUtf8())
        .isEqualTo("hello camel case");
  }

  @Test
  void fromJson_handlesDocumentMissingContent() {
    var json =
        new JsonObject()
            .put("type", new JsonArray().add("Document"))
            .put("media_type", "text/plain");

    ApiObject obj = parser.fromJson(json);
    assertThat(obj.getExtensionsMap().get("document").getDocument().getMediaType())
        .isEqualTo("text/plain");
  }

  @Test
  void fromJson_handlesDocumentMissingMediaType() {
    var json =
        new JsonObject()
            .put("type", new JsonArray().add("Document"))
            .put("content", "aGVsbG8="); // requires base64 because mediaType is not text/plain

    ApiObject obj = parser.fromJson(json);
    assertThat(obj.getExtensionsMap().get("document").getDocument().getContent().toStringUtf8())
        .isEqualTo("hello");
  }

  @Test
  void fromJson_handlesEmptyObject_successfully() {
    ApiObject obj = parser.fromJson(new JsonObject());
    assertThat(obj.getId()).isEmpty();
  }

  @Test
  void fromJson_doesNotParseExtensions_whenDeleted() {
    var json =
        new JsonObject()
            .put("id", "https://example.com/obj/1")
            .put("type", new JsonArray().add("Document"))
            .put("deleted", "2023-01-01T00:00:00Z")
            .put("media_type", "text/plain");

    ApiObject obj = parser.fromJson(json);
    assertThat(obj.getExtensionsMap()).isEmpty();
  }

  @Test
  void fromJson_throwsIllegalArgumentException_whenParserFails() {
    var json =
        new JsonObject()
            .put("type", new JsonArray().add("Event"))
            .put("start_time", "not-a-timestamp");
    assertThatThrownBy(() -> parser.fromJson(json)).isInstanceOf(IllegalArgumentException.class);
  }
}
