package com.larpconnect.njall.data.dao.admin;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import com.larpconnect.njall.data.annotation.NjallUsers;

/** Guice module configuring AdminUser, AdminRole, and StudioRole DAO bindings and entities. */
public final class AdminDaoModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(AdminRoleDAO.class).to(DefaultAdminRoleDAO.class).in(Scopes.SINGLETON);
    bind(AdminUserDAO.class).to(DefaultAdminUserDAO.class).in(Scopes.SINGLETON);
    bind(StudioRoleDAO.class).to(DefaultStudioRoleDAO.class).in(Scopes.SINGLETON);

    var adminEntities =
        Multibinder.newSetBinder(binder(), new TypeLiteral<Class<?>>() {}, NjallAdmin.class);
    adminEntities.addBinding().toInstance(AdminRoleEntity.class);
    adminEntities.addBinding().toInstance(AdminUserEntity.class);
    adminEntities.addBinding().toInstance(DefaultStudioRoleEntity.class);

    var userEntities =
        Multibinder.newSetBinder(binder(), new TypeLiteral<Class<?>>() {}, NjallUsers.class);
    userEntities.addBinding().toInstance(DefaultStudioRoleEntity.class);
  }
}
