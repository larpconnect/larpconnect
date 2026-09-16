package com.larpconnect.njall.data.dao;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.larpconnect.njall.data.annotation.NjallAdmin;

/** Guice module configuring DAO bindings and registering JPA entity classes. */
public final class DaoModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ServerDAO.class).to(DefaultServerDAO.class);

    var adminEntities =
        Multibinder.newSetBinder(binder(), new TypeLiteral<Class<?>>() {}, NjallAdmin.class);
    adminEntities.addBinding().toInstance(ServerEntity.class);
    adminEntities.addBinding().toInstance(ServerContactEntity.class);
  }
}
