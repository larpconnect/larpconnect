package com.larpconnect.njall.api.verticle;

import com.google.protobuf.Any;
import com.larpconnect.njall.common.id.IdGenerator;
import com.larpconnect.njall.proto.MessageReply;
import com.larpconnect.njall.proto.MessageRequest;
import com.larpconnect.njall.proto.Nodeinfo22;
import com.larpconnect.njall.proto.NodeinfoInstance;
import com.larpconnect.njall.proto.NodeinfoServices;
import com.larpconnect.njall.proto.NodeinfoSoftware;
import com.larpconnect.njall.proto.NodeinfoUsage;
import com.larpconnect.njall.proto.NodeinfoUsageUsers;
import io.vertx.core.Promise;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
final class NodeinfoVerticle extends AbstractLcVerticle {
  private final Logger logger = LoggerFactory.getLogger(NodeinfoVerticle.class);

  @Inject
  NodeinfoVerticle(IdGenerator idGenerator) {
    super("http.admin.nodeinfo.request", idGenerator);
  }

  @Override
  protected MessageResponse handleMessage(
      byte[] spanId, MessageRequest request, Promise<MessageReply> responsePromise) {
    try {
      var nodeinfo =
          Nodeinfo22.newBuilder()
              .setVersion("2.2")
              .setInstance(
                  NodeinfoInstance.newBuilder()
                      .setName("LARPConnect")
                      .setDescription("A robust platform for Live Action Role-Playing.")
                      .build())
              .setSoftware(
                  NodeinfoSoftware.newBuilder()
                      .setName("larpconnect")
                      .setVersion("0.0.1")
                      .setRepository("https://github.com/larpconnect/larpconnect")
                      .setHomepage("https://github.com/larpconnect/larpconnect")
                      .build())
              .addProtocols("activitypub")
              .setServices(
                  NodeinfoServices.newBuilder()
                      .build()) // Inbound and outbound services (empty for now)
              .setOpenRegistrations(false)
              .setUsage(
                  NodeinfoUsage.newBuilder()
                      .setUsers(
                          NodeinfoUsageUsers.newBuilder()
                              .setTotal(0)
                              .setActiveHalfyear(0)
                              .setActiveMonth(0)
                              .setActiveWeek(0)
                              .build())
                      .setLocalPosts(0)
                      .setLocalComments(0)
                      .build())
              .build();

      responsePromise.complete(
          MessageReply.newBuilder()
              .setProto(
                  com.larpconnect.njall.proto.ProtoDef.newBuilder()
                      .setProtobufName("Nodeinfo22")
                      .setMessage(Any.pack(nodeinfo))
                      .build())
              .build());
      return BasicResponse.CONTINUE;
    } catch (RuntimeException e) {
      logger.error("Failed to generate nodeinfo 2.2 response", e);
      responsePromise.fail(e);
      return BasicResponse.CONTINUE;
    }
  }
}
