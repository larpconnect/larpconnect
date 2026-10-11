package com.larpconnect.njall.data.dao.studios;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.larpconnect.njall.data.dao.common.DaoCommonModule;
import com.larpconnect.njall.data.dao.studios.individuals.IndividualsDaoModule;

/** Guice module configuring Studio, Location, Address, and Link DAO bindings and entities. */
public final class StudiosDaoModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(StudioLookupDAO.class).to(DefaultStudioLookupDAO.class).in(Scopes.SINGLETON);
    bind(StudioDAO.class).to(DefaultStudioDAO.class).in(Scopes.SINGLETON);
    bind(LinkDAO.class).to(DefaultLinkDAO.class).in(Scopes.SINGLETON);
    bind(LocationDAO.class).to(DefaultLocationDAO.class).in(Scopes.SINGLETON);
    bind(AddressDAO.class).to(DefaultAddressDAO.class).in(Scopes.SINGLETON);
    bind(HashtagDAO.class).to(DefaultHashtagDAO.class).in(Scopes.SINGLETON);
    bind(EventDAO.class).to(DefaultEventDAO.class).in(Scopes.SINGLETON);
    install(new IndividualsDaoModule());

    var adminEntities =
        Multibinder.newSetBinder(
            binder(), DaoCommonModule.ENTITY_CLASS_TYPE_LITERAL, NjallAdmin.class);
    adminEntities.addBinding().toInstance(StudioLookupEntity.class);
    adminEntities.addBinding().toInstance(StudioEntity.class);
    adminEntities.addBinding().toInstance(LinkEntity.class);
    adminEntities.addBinding().toInstance(LocationEntity.class);
    adminEntities.addBinding().toInstance(AddressEntity.class);
    adminEntities.addBinding().toInstance(HashtagEntity.class);
    adminEntities.addBinding().toInstance(HashtagEntityMapping.class);
    adminEntities.addBinding().toInstance(EventEntity.class);

    var userEntities =
        Multibinder.newSetBinder(
            binder(), DaoCommonModule.ENTITY_CLASS_TYPE_LITERAL, NjallUsers.class);
    userEntities.addBinding().toInstance(StudioEntity.class);
    userEntities.addBinding().toInstance(LinkEntity.class);
    userEntities.addBinding().toInstance(LocationEntity.class);
    userEntities.addBinding().toInstance(AddressEntity.class);
    userEntities.addBinding().toInstance(HashtagEntity.class);
    userEntities.addBinding().toInstance(HashtagEntityMapping.class);
    userEntities.addBinding().toInstance(EventEntity.class);
  }
}
