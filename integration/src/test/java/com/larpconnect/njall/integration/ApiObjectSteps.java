package com.larpconnect.njall.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import com.larpconnect.njall.api.ApiObjects;
import com.larpconnect.njall.proto.ApiObject;
import com.larpconnect.njall.proto.Document;
import com.larpconnect.njall.proto.Event;
import com.larpconnect.njall.proto.Extension;
import com.larpconnect.njall.proto.Link;
import com.larpconnect.njall.proto.OrderedCollectionPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.vertx.core.json.JsonObject;

public class ApiObjectSteps {

  private ApiObject originalObj;
  private JsonObject serializedJson;
  private ApiObject deserializedObj;

  @Given("an ApiObject with basic properties {string}, {string}, and {string}")
  public void anApiObjectWithBasicProperties(String id, String name, String type) {
    originalObj =
        ApiObject.newBuilder()
            .setId("https://example.com/obj/" + id)
            .setName(name)
            .addType(type)
            .addContext("https://www.w3.org/ns/activitystreams")
            .build();
  }

  @Given("an ApiObject with a Document extension")
  public void anApiObjectWithADocumentExtension() {
    originalObj =
        ApiObject.newBuilder()
            .setId("https://example.com/obj/doc")
            .putExtensions(
                "document",
                Extension.newBuilder()
                    .setDocument(
                        Document.newBuilder()
                            .setMediaType("text/plain")
                            .setContent(ByteString.copyFromUtf8("hello cucumber"))
                            .build())
                    .build())
            .build();
  }

  @Given("an ApiObject with an Event extension")
  public void anApiObjectWithAnEventExtension() {
    originalObj =
        ApiObject.newBuilder()
            .setId("https://example.com/obj/event")
            .putExtensions(
                "event",
                Extension.newBuilder()
                    .setEvent(
                        Event.newBuilder()
                            .setStartTime(Timestamp.newBuilder().setSeconds(1000).build())
                            .setEndTime(Timestamp.newBuilder().setSeconds(2000).build())
                            .build())
                    .build())
            .build();
  }

  @Given("an ApiObject with a Link extension")
  public void anApiObjectWithALinkExtension() {
    originalObj =
        ApiObject.newBuilder()
            .setId("https://example.com/obj/link")
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
            .build();
  }

  @Given("an ApiObject with an OrderedCollectionPage extension")
  public void anApiObjectWithAnOrderedCollectionPageExtension() {
    originalObj =
        ApiObject.newBuilder()
            .setId("https://example.com/obj/page")
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
            .build();
  }

  @When("it is serialized to JSON")
  public void itIsSerializedToJson() {
    serializedJson = ApiObjects.toJson(originalObj);
    assertThat(serializedJson).isNotNull();
  }

  @When("it is deserialized back to an ApiObject")
  public void itIsDeserializedBackToAnApiObject() {
    deserializedObj = ApiObjects.fromJson(serializedJson);
    assertThat(deserializedObj).isNotNull();
  }

  @Then("the resulting ApiObject matches the original properties")
  public void theResultingApiObjectMatchesTheOriginalProperties() {
    assertThat(deserializedObj).isEqualTo(originalObj);
  }

  @Then("the Document extension is preserved")
  public void theDocumentExtensionIsPreserved() {
    assertThat(deserializedObj.getExtensionsMap()).containsKey("document");
    assertThat(deserializedObj.getExtensionsMap().get("document"))
        .isEqualTo(originalObj.getExtensionsMap().get("document"));
  }

  @Then("the Event extension is preserved")
  public void theEventExtensionIsPreserved() {
    assertThat(deserializedObj.getExtensionsMap()).containsKey("event");
    assertThat(deserializedObj.getExtensionsMap().get("event"))
        .isEqualTo(originalObj.getExtensionsMap().get("event"));
  }

  @Then("the Link extension is preserved")
  public void theLinkExtensionIsPreserved() {
    assertThat(deserializedObj.getExtensionsMap()).containsKey("link");
    assertThat(deserializedObj.getExtensionsMap().get("link"))
        .isEqualTo(originalObj.getExtensionsMap().get("link"));
  }

  @Then("the OrderedCollectionPage extension is preserved")
  public void theOrderedCollectionPageExtensionIsPreserved() {
    assertThat(deserializedObj.getExtensionsMap()).containsKey("ordered_collection_page");
    assertThat(deserializedObj.getExtensionsMap().get("ordered_collection_page"))
        .isEqualTo(originalObj.getExtensionsMap().get("ordered_collection_page"));
  }
}
