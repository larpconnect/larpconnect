package com.larpconnect.njall.api.verticle;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Any;
import com.larpconnect.njall.common.id.IdGenerator;
import com.larpconnect.njall.proto.MessageReply;
import com.larpconnect.njall.proto.MessageRequest;
import com.larpconnect.njall.proto.NodeinfoJrd;
import io.vertx.core.Promise;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public final class NodeinfoWellKnownVerticleTest {

  private NodeinfoWellKnownVerticle verticle;

  @BeforeEach
  void setUp() {
    IdGenerator mockIdGenerator = () -> UUID.randomUUID();
    verticle = new NodeinfoWellKnownVerticle(mockIdGenerator);
  }

  @Test
  void handleMessage_returnsJrdResponse() throws Exception {
    Promise<MessageReply> promise = Promise.promise();
    MessageRequest request = MessageRequest.getDefaultInstance();

    MessageResponse response = verticle.handleMessage(new byte[8], request, promise);

    assertThat(response).isInstanceOf(ReplyResponse.class);

    ReplyResponse replyResponse = (ReplyResponse) response;
    Any payload = (Any) replyResponse.payload();
    NodeinfoJrd jrd = payload.unpack(NodeinfoJrd.class);

    assertThat(jrd.getLinksCount()).isEqualTo(1);
    assertThat(jrd.getLinks(0).getRel())
        .isEqualTo("http://nodeinfo.diaspora.software/ns/schema/2.2");
    assertThat(jrd.getLinks(0).getHref()).isEqualTo("http://localhost:8080/admin/nodeinfo");
  }
}
