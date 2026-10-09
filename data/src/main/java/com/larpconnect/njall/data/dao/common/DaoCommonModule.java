package com.larpconnect.njall.data.dao.common;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import com.larpconnect.njall.data.annotation.NjallUsers;

/** Guice module registering common JPA entity classes. */
public final class DaoCommonModule extends AbstractModule {

  @Override
  protected void configure() {
    var adminEntities =
        Multibinder.newSetBinder(binder(), new TypeLiteral<Class<?>>() {}, NjallAdmin.class);
    adminEntities.addBinding().toInstance(EntityBaseEntity.class);

    var userEntities =
        Multibinder.newSetBinder(binder(), new TypeLiteral<Class<?>>() {}, NjallUsers.class);
    userEntities.addBinding().toInstance(EntityBaseEntity.class);
  }
}
