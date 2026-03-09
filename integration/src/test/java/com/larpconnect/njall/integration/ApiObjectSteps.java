package com.larpconnect.njall.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.larpconnect.njall.api.ApiObjectParser;
import com.larpconnect.njall.proto.ApiObject;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.vertx.core.json.JsonObject;

public final class ApiObjectSteps {

  private JsonObject inputJson;
  private ApiObject deserializedObj;

  private final ApiObjectParser parser;

  public ApiObjectSteps() {
    Injector injector =
        Guice.createInjector(
            new com.larpconnect.njall.api.ApiModule(),
            new com.larpconnect.njall.common.CommonModule());
    this.parser = injector.getInstance(ApiObjectParser.class);
  }

  @Given("an ApiObject constructed from the following JSON:")
  public void anApiObjectConstructedFromTheFollowingJson(String jsonString) {
    inputJson = new JsonObject(jsonString);
  }

  @When("it is deserialized back to an ApiObject")
  public void itIsDeserializedBackToAnApiObject() {
    deserializedObj = parser.fromJson(inputJson);
    assertThat(deserializedObj).isNotNull();
  }

  @Then("the resulting ApiObject matches the original JSON properties")
  public void theResultingApiObjectMatchesTheOriginalJsonProperties() {
    assertThat(deserializedObj.getId()).isEqualTo(inputJson.getString("id"));
    assertThat(deserializedObj.getName()).isEqualTo(inputJson.getString("name"));
    assertThat(deserializedObj.getTypeList().get(0)).isEqualTo("Document");
  }

  @Then("the resulting ApiObject contains a {string} extension")
  public void theResultingApiObjectContainsAExtension(String extensionKey) {
    assertThat(deserializedObj.getExtensionsMap()).containsKey(extensionKey);
  }

  @Then("the resulting ApiObject contains an {string} extension")
  public void theResultingApiObjectContainsAnExtension(String extensionKey) {
    assertThat(deserializedObj.getExtensionsMap()).containsKey(extensionKey);
  }

  @Then("the Document extension has mediaType {string} and content {string}")
  public void theDocumentExtensionHasMediaTypeAndContent(String mediaType, String content) {
    var doc = deserializedObj.getExtensionsMap().get("document").getDocument();
    assertThat(doc.getMediaType()).isEqualTo(mediaType);
    assertThat(doc.getContent().toStringUtf8()).isEqualTo(content);
  }

  @Then("the Event extension has startTime {string}")
  public void theEventExtensionHasStartTime(String startTime) {
    var event = deserializedObj.getExtensionsMap().get("event").getEvent();
    // In Protobuf JSON, Timestamp is parsed to seconds and nanos. We can test via Instant.
    assertThat(event.getStartTime().getSeconds())
        .isEqualTo(java.time.Instant.parse(startTime).getEpochSecond());
  }
}
