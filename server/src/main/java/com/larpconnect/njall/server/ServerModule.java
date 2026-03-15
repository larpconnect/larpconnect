package com.larpconnect.njall.server;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.api.ApiModule;
import com.larpconnect.njall.common.CommonModule;
import com.larpconnect.njall.data.DataModule;
import com.larpconnect.njall.init.InitModule;

/**
 * The main entry point Guice module that wires together the server infrastructure, integrating the
 * API, common utilities, and initialization components.
 */
public final class ServerModule extends AbstractModule {
  /**
   * Constructs a new {@link ServerModule}.
   *
   * <p>This constructor is intentionally public to serve as the root configuration for the Guice
   * dependency injection graph. It is installed directly by tests or the main application bootstrap
   * process. It installs the {@link ApiModule}, {@link CommonModule}, {@link DataModule}, {@link
   * InitModule}, and the internal {@link ServerBindingModule}.
   */
  public ServerModule() {}

  @Override
  protected void configure() {
    install(new ApiModule());
    install(new CommonModule());
    install(new DataModule());
    /*
     * VertxModule and ConfigModule require state initialized dynamically during
     * VerticleLifecycle startup, so they are injected directly by the lifecycle
     * instead of statically installed here.
     */
    install(new InitModule());
    install(new ServerBindingModule());
  }
}
