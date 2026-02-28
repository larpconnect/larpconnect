package com.larpconnect.njall.server;

import com.google.inject.AbstractModule;
import java.util.function.Function;

public final class ServerModule extends AbstractModule {
  private final Function<String, String> getenv;

  public ServerModule() {
    this(System::getenv);
  }

  ServerModule(Function<String, String> getenv) {
    this.getenv = getenv;
  }

  @Override
  protected void configure() {
    install(new com.larpconnect.njall.common.CommonModule());
    install(new ServerBindingModule(getenv));
  }
}
