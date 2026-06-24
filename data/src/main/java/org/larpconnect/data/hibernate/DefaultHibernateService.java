package org.larpconnect.data.hibernate;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.larpconnect.data.entity.Actor;
import org.larpconnect.data.entity.Campaign;
import org.larpconnect.data.entity.CharacterInstance;
import org.larpconnect.data.entity.Collection;
import org.larpconnect.data.entity.Game;
import org.larpconnect.data.entity.Individual;
import org.larpconnect.data.entity.LarpCharacter;
import org.larpconnect.data.entity.LarpSystem;
import org.larpconnect.data.entity.Studio;
import org.larpconnect.data.entity.TenantEntity;
import org.larpconnect.data.entity.User;
import org.larpconnect.data.schema.StudioRoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Default implementation of HibernateService managing the SessionFactory lifecycle. */
@Singleton
class DefaultHibernateService extends AbstractIdleService implements HibernateService {
  private final Logger logger = LoggerFactory.getLogger(DefaultHibernateService.class);
  private final StudioRoutingService routingService;
  private SessionFactory sessionFactory;

  private static final List<Class<?>> ENTITY_CLASSES =
      List.of(
          Studio.class,
          TenantEntity.class,
          Campaign.class,
          LarpSystem.class,
          Game.class,
          LarpCharacter.class,
          CharacterInstance.class,
          Individual.class,
          User.class,
          Actor.class,
          Collection.class);

  @Inject
  DefaultHibernateService(StudioRoutingService routingService) {
    this.routingService = routingService;
  }

  @Override
  public SessionFactory getSessionFactory() {
    return sessionFactory;
  }

  @Override
  @SuppressWarnings("removal")
  protected void startUp() throws Exception {
    logger.info("Starting DefaultHibernateService...");

    String url = System.getProperty("db.url", "jdbc:postgresql://localhost:5432/larpconnect");
    String user = System.getProperty("db.user", "postgres");
    String pass = System.getProperty("db.password", "postgres");

    Properties settings = new Properties();
    settings.put(AvailableSettings.SHOW_SQL, "false");
    settings.put(AvailableSettings.FORMAT_SQL, "false");
    settings.put(AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "thread");
    settings.put(AvailableSettings.DIALECT, "org.hibernate.dialect.PostgreSQLDialect");
    settings.put("hibernate.multiTenancy", "SCHEMA");

    Map<String, Object> props = new HashMap<>();
    props.put("hibernate.connection.url", url);
    props.put("hibernate.connection.username", user);
    props.put("hibernate.connection.password", pass);
    ConnectionProvider connectionProvider = createConnectionProvider(props);

    MultiTenantConnectionProvider<String> multiTenantProvider =
        new DefaultMultiTenantConnectionProvider(connectionProvider, routingService);
    CurrentTenantIdentifierResolver<String> tenantResolver =
        new CurrentTenantIdentifierResolverImpl();

    settings.put("hibernate.multi_tenant_connection_provider", multiTenantProvider);
    settings.put("hibernate.tenant_identifier_resolver", tenantResolver);

    StandardServiceRegistry registry =
        new StandardServiceRegistryBuilder().applySettings(settings).build();

    MetadataSources sources = new MetadataSources(registry);
    ENTITY_CLASSES.forEach(sources::addAnnotatedClass);

    Metadata metadata = sources.getMetadataBuilder().build();
    this.sessionFactory = metadata.getSessionFactoryBuilder().build();

    logger.info("Hibernate SessionFactory initialized successfully.");
  }

  @SuppressWarnings("removal")
  ConnectionProvider createConnectionProvider(Map<String, Object> props) {
    DriverManagerConnectionProviderImpl provider = new DriverManagerConnectionProviderImpl();
    provider.configure(props);
    return provider;
  }

  @Override
  protected void shutDown() throws Exception {
    logger.info("Stopping DefaultHibernateService...");
    if (sessionFactory != null) {
      sessionFactory.close();
    }
    logger.info("Hibernate SessionFactory closed.");
  }
}
