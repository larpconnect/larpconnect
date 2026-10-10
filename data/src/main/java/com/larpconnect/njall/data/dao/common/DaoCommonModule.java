package com.larpconnect.njall.data.dao.common;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import com.larpconnect.njall.data.annotation.NjallUsers;

/** Guice module registering common JPA entity classes. */
public final class DaoCommonModule extends AbstractModule {

  public static final TypeLiteral<Class<?>> ENTITY_CLASS_TYPE_LITERAL =
      new TypeLiteral<Class<?>>() {};

  @Override
  protected void configure() {
    var adminEntities =
        Multibinder.newSetBinder(binder(), ENTITY_CLASS_TYPE_LITERAL, NjallAdmin.class);
    adminEntities.addBinding().toInstance(EntityBaseEntity.class);

    var userEntities =
        Multibinder.newSetBinder(binder(), ENTITY_CLASS_TYPE_LITERAL, NjallUsers.class);
    userEntities.addBinding().toInstance(EntityBaseEntity.class);
  }
}
