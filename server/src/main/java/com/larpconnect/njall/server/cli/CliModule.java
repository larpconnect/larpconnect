package com.larpconnect.njall.server.cli;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * Guice module providing command-line parsing components.
 *
 * <p>Note: The standard application bootstrap is executed via {@link
 * com.larpconnect.njall.server.ServerApp} before Guice injector initialization. The {@link
 * CliRunner} provided here operates with default command dispatching for standalone CLI parsing,
 * inspection, or auxiliary tools.
 */
public final class CliModule extends AbstractModule {

  @Override
  protected void configure() {
    // Providers declared via @Provides methods
  }

  @Provides
  @Singleton
  CliRunner provideCliRunner() {
    return new CliRunner();
  }
}
