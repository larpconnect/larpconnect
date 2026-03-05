package com.larpconnect.njall.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.util.JsonFormat;
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
    try {
      java.lang.reflect.Constructor<?> ctor =
          Class.forName("com.larpconnect.njall.api.DefaultApiObjectParser")
              .getDeclaredConstructor(JsonFormat.Printer.class, JsonFormat.Parser.class);
      ctor.setAccessible(true);
      this.parser =
          (ApiObjectParser)
              ctor.newInstance(
                  JsonFormat.printer()
                      .preservingProtoFieldNames()
                      .omittingInsignificantWhitespace(),
                  JsonFormat.parser().ignoringUnknownFields());
    } catch (Exception e) {
      throw new IllegalStateException("Failed to initialize parser", e);
    }
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
}
