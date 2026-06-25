package org.larpconnect.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.google.inject.Injector;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.larpconnect.data.context.TenantContext;
import org.larpconnect.data.dao.ActorDao;
import org.larpconnect.data.dao.CampaignDao;
import org.larpconnect.data.dao.CharacterInstanceDao;
import org.larpconnect.data.dao.CollectionDao;
import org.larpconnect.data.dao.GameDao;
import org.larpconnect.data.dao.IndividualDao;
import org.larpconnect.data.dao.LarpCharacterDao;
import org.larpconnect.data.dao.LarpSystemDao;
import org.larpconnect.data.dao.StudioDao;
import org.larpconnect.data.dao.UserDao;
import org.larpconnect.data.entity.Actor;
import org.larpconnect.data.entity.Campaign;
import org.larpconnect.data.entity.CharacterInstance;
import org.larpconnect.data.entity.Collection;
import org.larpconnect.data.entity.Game;
import org.larpconnect.data.entity.Individual;
import org.larpconnect.data.entity.LarpCharacter;
import org.larpconnect.data.entity.LarpSystem;
import org.larpconnect.data.entity.Studio;
import org.larpconnect.data.entity.User;
import org.larpconnect.data.hibernate.HibernateService;
import org.testcontainers.containers.PostgreSQLContainer;

/** Integration tests using Testcontainers to verify the database schema and DAO capabilities. */
@org.junit.jupiter.api.condition.EnabledIf("isDockerAvailable")
public final class DataIntegrationTest {

  private static PostgreSQLContainer<?> postgres;

  private static Injector injector;
  private static HibernateService hibernateService;
  private static SessionFactory sessionFactory;

  private StudioDao studioDao;
  private LarpSystemDao systemDao;
  private CampaignDao campaignDao;
  private GameDao gameDao;
  private LarpCharacterDao characterDao;
  private CharacterInstanceDao characterInstanceDao;
  private IndividualDao individualDao;
  private UserDao userDao;
  private ActorDao actorDao;
  private CollectionDao collectionDao;

  static boolean isDockerAvailable() {
    try {
      return org.testcontainers.DockerClientFactory.instance().isDockerAvailable();
    } catch (Throwable t) {
      return false;
    }
  }

  @BeforeAll
  public static void beforeAll() throws Exception {
    if (!isDockerAvailable()) {
      return;
    }
    postgres =
        new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("larpconnect")
            .withUsername("postgres")
            .withPassword("postgres");
    postgres.start();

    System.setProperty("db.url", postgres.getJdbcUrl());
    System.setProperty("db.user", postgres.getUsername());
    System.setProperty("db.password", postgres.getPassword());

    try (Connection conn =
        DriverManager.getConnection(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

      try (Statement stmt = conn.createStatement()) {
        stmt.execute("CREATE SCHEMA IF NOT EXISTS njall_core_admin");
      }
      executeSqlScript(conn, "njall_core_admin", "db/migration/admin/admin-schema.sql");

      try (Statement stmt = conn.createStatement()) {
        stmt.execute("CREATE SCHEMA IF NOT EXISTS njall_testtenant");
      }
      executeSqlScript(conn, "njall_testtenant", "db/migration/tenant/tenant-schema-template.sql");
    }

    injector = Guice.createInjector(new DataModule());
    hibernateService = injector.getInstance(HibernateService.class);
    hibernateService.startAsync().awaitRunning();
    sessionFactory = injector.getInstance(SessionFactory.class);
  }

  @AfterAll
  public static void afterAll() {
    if (hibernateService != null) {
      hibernateService.stopAsync().awaitTerminated();
    }
    if (postgres != null) {
      postgres.stop();
    }
  }

  private static void executeSqlScript(Connection conn, String schema, String path)
      throws Exception {
    if (schema == null
        || !schema.matches(
            "^(njall_core_admin|njall_core_default|njall_testtenant"
                + "|njall_[a-z0-9]{26}|njall_tenant_[a-z0-9]{26})$")) {
      throw new IllegalArgumentException("Invalid schema name: " + schema);
    }
    try (var is = DataIntegrationTest.class.getClassLoader().getResourceAsStream(path)) {
      if (is == null) {
        throw new IllegalArgumentException("Script not found: " + path);
      }
      String content = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
      conn.setSchema(schema);
      try (Statement stmt = conn.createStatement()) {
        stmt.execute(content);
      }
    }
  }

  @BeforeEach
  public void setUp() {
    studioDao = injector.getInstance(StudioDao.class);
    systemDao = injector.getInstance(LarpSystemDao.class);
    campaignDao = injector.getInstance(CampaignDao.class);
    gameDao = injector.getInstance(GameDao.class);
    characterDao = injector.getInstance(LarpCharacterDao.class);
    characterInstanceDao = injector.getInstance(CharacterInstanceDao.class);
    individualDao = injector.getInstance(IndividualDao.class);
    userDao = injector.getInstance(UserDao.class);
    actorDao = injector.getInstance(ActorDao.class);
    collectionDao = injector.getInstance(CollectionDao.class);

    TenantContext.setTenantSupplier(() -> "njall_core_admin");
  }

  @AfterEach
  public void tearDown() {
    TenantContext.setTenantSupplier(() -> "njall_core_default");
  }

  private void runInTransaction(String tenantSchema, Runnable runnable) {
    String previousTenant = TenantContext.getTenantSchema();
    TenantContext.setTenantSupplier(() -> tenantSchema);
    Session session = sessionFactory.openSession();
    org.hibernate.context.internal.ThreadLocalSessionContext.bind(session);
    session.beginTransaction();
    try {
      runnable.run();
      session.getTransaction().commit();
    } catch (Exception e) {
      if (session.getTransaction().isActive()) {
        session.getTransaction().rollback();
      }
      throw e;
    } finally {
      org.hibernate.context.internal.ThreadLocalSessionContext.unbind(sessionFactory);
      session.close();
      TenantContext.setTenantSupplier(() -> previousTenant);
    }
  }

  @Test
  public void studioDao_adminStudioLifecycle_savesFindsAndDeletesSuccessfully() {
    UUID studioId = UUID.randomUUID();
    Studio studio = new Studio(studioId, "Test Studio", "njall_testtenant");

    runInTransaction(
        "njall_core_admin",
        () -> {
          studioDao.save("njall_core_admin", studio);
        });

    AtomicReference<Optional<Studio>> foundRef = new AtomicReference<>();
    runInTransaction(
        "njall_core_admin",
        () -> {
          foundRef.set(studioDao.findById("njall_core_admin", studioId));
        });

    assertThat(foundRef.get()).isPresent();
    assertThat(foundRef.get().get().getName()).isEqualTo("Test Studio");

    runInTransaction(
        "njall_core_admin",
        () -> {
          Studio toDelete = studioDao.findById("njall_core_admin", studioId).orElseThrow();
          studioDao.delete("njall_core_admin", toDelete);
        });

    runInTransaction(
        "njall_core_admin",
        () -> {
          foundRef.set(studioDao.findById("njall_core_admin", studioId));
        });

    assertThat(foundRef.get()).isEmpty();
  }

  @Test
  public void tenantDaos_tenantEntitiesLifecycle_savesFindsAndSoftDeletesSuccessfully() {
    TenantTestContext context =
        new TenantTestContext(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID());

    saveTenantEntities(context);
    verifyTenantEntitiesSaved(context.systemId(), context.userId());
    deleteCampaignAndVerify(context.campaignId());
  }

  private void saveTenantEntities(TenantTestContext context) {
    runInTransaction(
        "njall_testtenant",
        () -> {
          LarpSystem system = new LarpSystem(context.systemId(), "Amtgard");
          systemDao.save("njall_testtenant", system);

          Campaign campaign = new Campaign(context.campaignId(), system);
          campaignDao.save("njall_testtenant", campaign);

          Game game = new Game(context.gameId(), campaign);
          gameDao.save("njall_testtenant", game);

          LarpCharacter character = new LarpCharacter(context.charId(), campaign, "Hero Template");
          characterDao.save("njall_testtenant", character);

          Individual player = new Individual(context.playerId(), "Alice");
          individualDao.save("njall_testtenant", player);

          User user = new User(context.userId(), "Bob", "bob123");
          userDao.save("njall_testtenant", user);

          CharacterInstance instance =
              new CharacterInstance(context.instId(), character, player, game, "Alice's Mage");
          characterInstanceDao.save("njall_testtenant", instance);

          Actor actor = new Actor(context.actorId(), campaign, "Campaign Actor", "preferred_alice");
          actorDao.save("njall_testtenant", actor);

          Collection collection = new Collection(context.collId(), campaign, "Campaign Items");
          collectionDao.save("njall_testtenant", collection);
        });
  }

  private void verifyTenantEntitiesSaved(UUID systemId, UUID userId) {
    AtomicReference<Optional<LarpSystem>> foundSysRef = new AtomicReference<>();
    AtomicReference<Optional<User>> foundUserRef = new AtomicReference<>();
    runInTransaction(
        "njall_testtenant",
        () -> {
          foundSysRef.set(systemDao.findById("njall_testtenant", systemId));
          foundUserRef.set(userDao.findById("njall_testtenant", userId));
        });

    assertThat(foundSysRef.get()).isPresent();
    assertThat(foundSysRef.get().get().getName()).isEqualTo("Amtgard");
    assertThat(foundUserRef.get()).isPresent();
    assertThat(foundUserRef.get().get().getUsername()).isEqualTo("bob123");
  }

  private void deleteCampaignAndVerify(UUID campaignId) {
    runInTransaction(
        "njall_testtenant",
        () -> {
          Campaign toDelete = campaignDao.findById("njall_testtenant", campaignId).orElseThrow();
          campaignDao.delete("njall_testtenant", toDelete);
        });

    runInTransaction(
        "njall_testtenant",
        () -> {
          Optional<Campaign> foundCamp = campaignDao.findById("njall_testtenant", campaignId);
          assertThat(foundCamp).isEmpty();
        });
  }
}

record TenantTestContext(
    UUID systemId,
    UUID campaignId,
    UUID gameId,
    UUID charId,
    UUID playerId,
    UUID userId,
    UUID instId,
    UUID actorId,
    UUID collId) {}
