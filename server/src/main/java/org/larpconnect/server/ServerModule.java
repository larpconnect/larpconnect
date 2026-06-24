package org.larpconnect.server;

import com.google.inject.AbstractModule;
import org.larpconnect.api.ApiModule;
import org.larpconnect.base.BaseModule;
import org.larpconnect.common.CommonModule;
import org.larpconnect.data.DataModule;
import org.larpconnect.events.EventsModule;
import org.larpconnect.queue.QueueModule;

/** Orchestrates dependency injection config by installing all operational Guice modules. */
public final class ServerModule extends AbstractModule {
  @Override
  protected void configure() {
    install(new CommonModule());
    install(new EventsModule());
    install(new QueueModule());
    install(new DataModule());
    install(new BaseModule());
    install(new ApiModule());

    bind(ServerService.class).in(com.google.inject.Singleton.class);
  }
}
