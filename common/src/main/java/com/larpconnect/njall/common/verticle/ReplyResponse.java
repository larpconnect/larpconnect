package com.larpconnect.njall.common.verticle;

import com.larpconnect.njall.proto.Message;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP"},
    justification = "Protobuf messages are immutable")
public record ReplyResponse(Message reply) implements MessageResponse {}
