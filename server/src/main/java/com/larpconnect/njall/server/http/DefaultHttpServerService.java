package com.larpconnect.njall.server.http;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.larpconnect.njall.api.http.RootRoute;
import com.larpconnect.njall.common.config.ServerConfig;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.pekko.Done;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.http.javadsl.Http;
import org.apache.pekko.http.javadsl.ServerBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
final class DefaultHttpServerService implements HttpServerService {

  private static final Duration TERMINATION_TIMEOUT = Duration.ofSeconds(10);

  private final Logger logger = LoggerFactory.getLogger(DefaultHttpServerService.class);
  private final ActorSystem<Void> system;
  private final ServerConfig config;
  private final RootRoute rootRoute;
  private final AtomicReference<ServerBinding> bindingRef = new AtomicReference<>();

  @Inject
  DefaultHttpServerService(ActorSystem<Void> system, ServerConfig config, RootRoute rootRoute) {
    this.system = system;
    this.config = config;
    this.rootRoute = rootRoute;
  }

  @Override
  public CompletionStage<ServerBinding> start() {
    return Http.get(system)
        .newServerAt(config.host(), config.port())
        .bind(rootRoute.route())
        .thenApply(this::onBound);
  }

  private ServerBinding onBound(ServerBinding binding) {
    binding.addToCoordinatedShutdown(TERMINATION_TIMEOUT, system);
    bindingRef.set(binding);
    logger.info("Server online at http://{}:{}/", config.host(), binding.localAddress().getPort());
    return binding;
  }

  @Override
  public CompletionStage<Done> stop() {
    var binding = bindingRef.getAndSet(null);
    if (binding == null) {
      return CompletableFuture.completedFuture(Done.done());
    }
    return binding.unbind();
  }

  @Override
  public int getBoundPort() {
    var binding = bindingRef.get();
    return binding != null ? binding.localAddress().getPort() : 0;
  }
}
