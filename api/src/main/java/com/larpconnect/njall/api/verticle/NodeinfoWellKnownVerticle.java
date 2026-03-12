package com.larpconnect.njall.api.verticle;

import com.google.protobuf.Any;
import com.larpconnect.njall.common.id.IdGenerator;
import com.larpconnect.njall.proto.MessageReply;
import com.larpconnect.njall.proto.MessageRequest;
import com.larpconnect.njall.proto.NodeinfoJrd;
import com.larpconnect.njall.proto.NodeinfoJrdLink;
import io.vertx.core.Promise;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class NodeinfoWellKnownVerticle extends AbstractLcVerticle {
  private static final Logger logger = LoggerFactory.getLogger(NodeinfoWellKnownVerticle.class);

  @Inject
  NodeinfoWellKnownVerticle(IdGenerator idGenerator) {
    super("http.well-known.nodeinfo.request", idGenerator);
  }

  @Override
  protected MessageResponse handleMessage(
      byte[] spanId, MessageRequest request, Promise<MessageReply> responsePromise) {
    try {
      var nodeinfoJrd =
          NodeinfoJrd.newBuilder()
              .addLinks(
                  NodeinfoJrdLink.newBuilder()
                      .setRel("http://nodeinfo.diaspora.software/ns/schema/2.2")
                      .setHref("http://localhost:8080/admin/nodeinfo")
                      .build())
              .build();

      return new ReplyResponse(Any.pack(nodeinfoJrd));
    } catch (RuntimeException e) {
      logger.error("Failed to generate nodeinfo jrd response", e);
      responsePromise.fail(e);
      return BasicResponse.CONTINUE;
    }
  }
}
