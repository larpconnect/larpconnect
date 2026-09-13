package com.larpconnect.njall.server.http;

import java.util.concurrent.CompletionStage;
import org.apache.pekko.Done;
import org.apache.pekko.http.javadsl.ServerBinding;

/** Service interface managing the lifecycle of the Pekko HTTP server. */
public interface HttpServerService {

  /**
   * Starts the HTTP server and binds to the configured host and port.
   *
   * @return A {@link CompletionStage} completing with the {@link ServerBinding}.
   */
  CompletionStage<ServerBinding> start();

  /**
   * Stops the HTTP server and unbinds from the network socket.
   *
   * @return A {@link CompletionStage} completing with {@link Done}.
   */
  CompletionStage<Done> stop();

  /**
   * Returns the actual bound port number, or 0 if not yet bound.
   *
   * @return The local bound port number.
   */
  int getBoundPort();
}
