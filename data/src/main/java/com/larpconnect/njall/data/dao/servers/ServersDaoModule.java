package com.larpconnect.njall.data.dao.servers;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.larpconnect.njall.data.annotation.NjallAdmin;

/** Guice module configuring Server DAO bindings and entities. */
public final class ServersDaoModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ServerDAO.class).to(DefaultServerDAO.class).in(Scopes.SINGLETON);

    var adminEntities =
        Multibinder.newSetBinder(binder(), new TypeLiteral<Class<?>>() {}, NjallAdmin.class);
    adminEntities.addBinding().toInstance(ServerEntity.class);
    adminEntities.addBinding().toInstance(ServerContactEntity.class);
  }
}
