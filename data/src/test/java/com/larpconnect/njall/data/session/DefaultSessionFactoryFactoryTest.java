package com.larpconnect.njall.data.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.config.SessionConfig;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DefaultSessionFactoryFactoryTest {

  @Entity
  @Table(name = "dummy_entity")
  static class DummyEntity {
    @Id private Long id;

    public Long getId() {
      return id;
    }
  }

  static final class TestDriver implements Driver {
    private final Connection connection;

    TestDriver(Connection connection) {
      this.connection = connection;
    }

    @Override
    public Connection connect(String url, Properties info) {
      return acceptsURL(url) ? connection : null;
    }

    @Override
    public boolean acceptsURL(String url) {
      return url.startsWith("jdbc:mockpg:");
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
      return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
      return 1;
    }

    @Override
    public int getMinorVersion() {
      return 0;
    }

    @Override
    public boolean jdbcCompliant() {
      return true;
    }

    @Override
    public Logger getParentLogger() {
      return Logger.getGlobal();
    }
  }

  private static TestDriver registerMockDriver() throws Exception {
    var connection = mock(Connection.class);
    var meta = mock(DatabaseMetaData.class);
    when(connection.getMetaData()).thenReturn(meta);
    when(connection.createStatement()).thenAnswer(inv -> mock(Statement.class));
    when(meta.getDatabaseProductName()).thenReturn("PostgreSQL");
    when(meta.getDatabaseMajorVersion()).thenReturn(16);
    when(meta.getDatabaseMinorVersion()).thenReturn(0);
    when(meta.getDriverName()).thenReturn("Mock Driver");

    var testDriver = new TestDriver(connection);
    DriverManager.registerDriver(testDriver);
    return testDriver;
  }

  @Test
  @DisplayName("create builds SessionFactory with annotated classes and applies password when set")
  void create_validConfigAndClasses_buildsSessionFactoryWithPassword() throws Exception {
    var testDriver = registerMockDriver();
    try {
      var factory = new DefaultSessionFactoryFactory();
      var config = new SessionConfig("jdbc:mockpg://localhost/test", "user", "pass", 2, 10, 5);

      var sessionFactory = factory.create(config, List.of(DummyEntity.class));

      assertThat(sessionFactory).isNotNull();
      assertThat(sessionFactory.getProperties().get(AvailableSettings.JAKARTA_JDBC_PASSWORD))
          .isEqualTo("****");
      sessionFactory.close();
    } finally {
      DriverManager.deregisterDriver(testDriver);
    }
  }

  @Test
  @DisplayName("create omits password setting when password is empty string and trustAuth is true")
  void create_emptyPassword_omitsPasswordSetting() throws Exception {
    var testDriver = registerMockDriver();
    try {
      var factory = new DefaultSessionFactoryFactory();
      var config = new SessionConfig("jdbc:mockpg://localhost/test", "user", "", true, 2, 10, 5);

      var sessionFactory = factory.create(config, List.of(DummyEntity.class));

      assertThat(sessionFactory).isNotNull();
      assertThat(sessionFactory.getProperties().get(AvailableSettings.JAKARTA_JDBC_PASSWORD))
          .isNull();
      sessionFactory.close();
    } finally {
      DriverManager.deregisterDriver(testDriver);
    }
  }

  @Test
  @DisplayName("create omits password setting when password is null and trustAuth is true")
  void create_nullPassword_omitsPasswordSetting() throws Exception {
    var testDriver = registerMockDriver();
    try {
      var factory = new DefaultSessionFactoryFactory();
      var config =
          new SessionConfig("jdbc:mockpg://localhost/test", "user", (String) null, true, 2, 10, 5);

      var sessionFactory = factory.create(config, List.of(DummyEntity.class));

      assertThat(sessionFactory).isNotNull();
      assertThat(sessionFactory.getProperties().get(AvailableSettings.JAKARTA_JDBC_PASSWORD))
          .isNull();
      sessionFactory.close();
    } finally {
      DriverManager.deregisterDriver(testDriver);
    }
  }
}
