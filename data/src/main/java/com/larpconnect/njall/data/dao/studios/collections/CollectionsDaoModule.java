package com.larpconnect.njall.data.dao.studios.collections;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.larpconnect.njall.data.dao.common.DaoCommonModule;

/** Guice module configuring Collection DAO bindings and entity registration. */
public final class CollectionsDaoModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(CollectionDAO.class).to(DefaultCollectionDAO.class).in(Scopes.SINGLETON);

    var adminEntities =
        Multibinder.newSetBinder(
            binder(), DaoCommonModule.ENTITY_CLASS_TYPE_LITERAL, NjallAdmin.class);
    adminEntities.addBinding().toInstance(CollectionEntity.class);
    adminEntities.addBinding().toInstance(CollectionItemEntity.class);

    var userEntities =
        Multibinder.newSetBinder(
            binder(), DaoCommonModule.ENTITY_CLASS_TYPE_LITERAL, NjallUsers.class);
    userEntities.addBinding().toInstance(CollectionEntity.class);
    userEntities.addBinding().toInstance(CollectionItemEntity.class);
  }
}
