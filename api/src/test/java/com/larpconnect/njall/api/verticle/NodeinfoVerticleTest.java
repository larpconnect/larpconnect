package com.larpconnect.njall.api.verticle;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Any;
import com.larpconnect.njall.common.id.IdGenerator;
import com.larpconnect.njall.proto.MessageReply;
import com.larpconnect.njall.proto.MessageRequest;
import com.larpconnect.njall.proto.Nodeinfo22;
import io.vertx.core.Promise;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public final class NodeinfoVerticleTest {

  private NodeinfoVerticle verticle;

  @BeforeEach
  void setUp() {
    IdGenerator mockIdGenerator = () -> UUID.randomUUID();
    verticle = new NodeinfoVerticle(mockIdGenerator);
  }

  @Test
  void handleMessage_returnsNodeinfoResponse() throws Exception {
    Promise<MessageReply> promise = Promise.promise();
    MessageRequest request = MessageRequest.getDefaultInstance();

    MessageResponse response = verticle.handleMessage(new byte[8], request, promise);

    assertThat(response).isInstanceOf(ReplyResponse.class);

    ReplyResponse replyResponse = (ReplyResponse) response;
    Any payload = (Any) replyResponse.payload();
    Nodeinfo22 nodeinfo = payload.unpack(Nodeinfo22.class);

    assertThat(nodeinfo.getVersion()).isEqualTo("2.2");
    assertThat(nodeinfo.getSoftware().getName()).isEqualTo("larpconnect");
    assertThat(nodeinfo.getProtocolsList()).containsExactly("activitypub");
    assertThat(nodeinfo.getOpenRegistrations()).isFalse();
    assertThat(nodeinfo.getUsage().getUsers().getTotal()).isEqualTo(0);
  }
}
