package com.larpconnect.njall.data.dao;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.data.dao.admin.AdminDaoModule;
import com.larpconnect.njall.data.dao.common.DaoCommonModule;
import com.larpconnect.njall.data.dao.servers.ServersDaoModule;
import com.larpconnect.njall.data.dao.studios.StudiosDaoModule;

/** Guice module configuring the DAO layer by installing direct subpackage DAO modules. */
public final class DaoModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new DaoCommonModule());
    install(new StudiosDaoModule());
    install(new AdminDaoModule());
    install(new ServersDaoModule());
  }
}
