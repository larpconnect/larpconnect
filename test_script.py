import re

with open("api/src/test/java/com/larpconnect/njall/api/verticle/NodeinfoWellKnownVerticleTest.java", "r") as f:
    content = f.read()

new_content = re.sub(
    r'    MessageResponse response = verticle.handleMessage\(new byte\[8\], request, promise\);\n\n    assertThat\(response\).isInstanceOf\(ReplyResponse.class\);\n\n    ReplyResponse replyResponse = \(ReplyResponse\) response;\n    Any payload = \(Any\) replyResponse.payload\(\);\n    NodeinfoJrd jrd = payload.unpack\(NodeinfoJrd.class\);',
    r'    verticle.handleMessage(new byte[8], request, promise);\n    MessageReply replyResponse = promise.future().toCompletionStage().toCompletableFuture().get();\n    Any payload = replyResponse.getProto().getMessage();\n    NodeinfoJrd jrd = payload.unpack(NodeinfoJrd.class);',
    content
)

with open("api/src/test/java/com/larpconnect/njall/api/verticle/NodeinfoWellKnownVerticleTest.java", "w") as f:
    f.write(new_content)

with open("api/src/test/java/com/larpconnect/njall/api/verticle/NodeinfoVerticleTest.java", "r") as f:
    content = f.read()

new_content = re.sub(
    r'    MessageResponse response = verticle.handleMessage\(new byte\[8\], request, promise\);\n\n    assertThat\(response\).isInstanceOf\(ReplyResponse.class\);\n\n    ReplyResponse replyResponse = \(ReplyResponse\) response;\n    Any payload = \(Any\) replyResponse.payload\(\);\n    Nodeinfo22 nodeinfo = payload.unpack\(Nodeinfo22.class\);',
    r'    verticle.handleMessage(new byte[8], request, promise);\n    MessageReply replyResponse = promise.future().toCompletionStage().toCompletableFuture().get();\n    Any payload = replyResponse.getProto().getMessage();\n    Nodeinfo22 nodeinfo = payload.unpack(Nodeinfo22.class);',
    content
)

with open("api/src/test/java/com/larpconnect/njall/api/verticle/NodeinfoVerticleTest.java", "w") as f:
    f.write(new_content)
