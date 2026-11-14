package com.larpconnect.njall.data.dao.studios.games;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.larpconnect.njall.data.dao.common.DaoCommonModule;

/** Guice module configuring Game DAO bindings and entity registration. */
public final class GamesDaoModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(GameDAO.class).to(DefaultGameDAO.class).in(Scopes.SINGLETON);

    var adminEntities =
        Multibinder.newSetBinder(
            binder(), DaoCommonModule.ENTITY_CLASS_TYPE_LITERAL, NjallAdmin.class);
    adminEntities.addBinding().toInstance(GameEntity.class);

    var userEntities =
        Multibinder.newSetBinder(
            binder(), DaoCommonModule.ENTITY_CLASS_TYPE_LITERAL, NjallUsers.class);
    userEntities.addBinding().toInstance(GameEntity.class);
  }
}
