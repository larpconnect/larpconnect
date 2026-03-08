package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.larpconnect.njall.common.id.IdGenerator;
import com.larpconnect.njall.common.verticle.ReplyResponse;
import com.larpconnect.njall.proto.Message;
import com.larpconnect.njall.proto.Parameter;
import io.vertx.core.json.JsonObject;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public final class WebfingerVerticleTest {

  @Test
  public void handleMessage_missingResource_returnsError() {
    WebfingerVerticle verticle =
        new WebfingerVerticle(
            new IdGenerator() {
              @Override
              public java.util.UUID generate() {
                return java.util.UUID.randomUUID();
              }
            });
    Message msg = Message.newBuilder().build();
    ReplyResponse response = (ReplyResponse) verticle.handleMessage(new byte[0], msg);

    assertThat(response.reply().hasMime()).isTrue();
    String content = response.reply().getMime().getContent().toString(StandardCharsets.UTF_8);
    assertThat(content).contains("missing resource parameter");
  }

  @Test
  public void handleMessage_withResource_returnsJrd() {
    WebfingerVerticle verticle =
        new WebfingerVerticle(
            new IdGenerator() {
              @Override
              public java.util.UUID generate() {
                return java.util.UUID.randomUUID();
              }
            });
    Message msg =
        Message.newBuilder()
            .addParameter(
                Parameter.newBuilder().setKey("resource").setValue("acct:test@localhost").build())
            .build();

    ReplyResponse response = (ReplyResponse) verticle.handleMessage(new byte[0], msg);

    assertThat(response.reply().hasMime()).isTrue();
    assertThat(response.reply().getMime().getSubtype()).isEqualTo("jrd+json");

    String content = response.reply().getMime().getContent().toString(StandardCharsets.UTF_8);
    JsonObject json = new JsonObject(content);
    assertThat(json.getString("subject")).isEqualTo("acct:test@localhost");
  }
}
