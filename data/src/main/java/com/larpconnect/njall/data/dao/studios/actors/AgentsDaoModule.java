package com.larpconnect.njall.data.dao.studios.actors;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.larpconnect.njall.data.dao.common.DaoCommonModule;

/** Guice module configuring Agent DAO bindings and entity registration. */
public final class AgentsDaoModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(AgentDAO.class).to(DefaultAgentDAO.class).in(Scopes.SINGLETON);

    var adminEntities =
        Multibinder.newSetBinder(
            binder(), DaoCommonModule.ENTITY_CLASS_TYPE_LITERAL, NjallAdmin.class);
    adminEntities.addBinding().toInstance(AgentEntity.class);
    adminEntities.addBinding().toInstance(AgentEndpointEntity.class);

    var userEntities =
        Multibinder.newSetBinder(
            binder(), DaoCommonModule.ENTITY_CLASS_TYPE_LITERAL, NjallUsers.class);
    userEntities.addBinding().toInstance(AgentEntity.class);
    userEntities.addBinding().toInstance(AgentEndpointEntity.class);
  }
}
