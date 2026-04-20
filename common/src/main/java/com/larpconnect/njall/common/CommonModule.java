package com.larpconnect.njall.common;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.common.codec.CodecModule;
import com.larpconnect.njall.common.id.IdModule;
import com.larpconnect.njall.common.time.TimeModule;

/**
 * Guice module that provides common utilities and core infrastructure for the system, including
 * time services, ID generation, and EventBus codecs.
 */
public final class CommonModule extends AbstractModule {
  /**
   * Constructs a new {@link CommonModule}.
   *
   * <p>This constructor is intentionally public to allow this module to be installed across package
   * boundaries by other modules. It serves to encapsulate the internal package-private bindings.
   */
  public CommonModule() {}

  @Override
  protected void configure() {
    install(new CommonBindingModule());
    install(new TimeModule());
    install(new IdModule());
    install(new CodecModule());
  }
}
