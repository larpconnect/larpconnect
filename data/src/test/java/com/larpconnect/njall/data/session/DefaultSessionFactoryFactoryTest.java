package com.larpconnect.njall.data.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.larpconnect.njall.data.config.SessionConfig;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
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

  static final class TestDriver implements java.sql.Driver {
    private final java.sql.Connection connection;

    TestDriver(java.sql.Connection connection) {
      this.connection = connection;
    }

    @Override
    public java.sql.Connection connect(String url, java.util.Properties info) {
      return acceptsURL(url) ? connection : null;
    }

    @Override
    public boolean acceptsURL(String url) {
      return url.startsWith("jdbc:mockpg:");
    }

    @Override
    public java.sql.DriverPropertyInfo[] getPropertyInfo(String url, java.util.Properties info) {
      return new java.sql.DriverPropertyInfo[0];
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
    public java.util.logging.Logger getParentLogger() {
      return java.util.logging.Logger.getGlobal();
    }
  }

  private static TestDriver registerMockDriver() throws Exception {
    var connection = org.mockito.Mockito.mock(java.sql.Connection.class);
    var meta = org.mockito.Mockito.mock(java.sql.DatabaseMetaData.class);
    org.mockito.Mockito.when(connection.getMetaData()).thenReturn(meta);
    org.mockito.Mockito.when(connection.createStatement())
        .thenAnswer(inv -> org.mockito.Mockito.mock(java.sql.Statement.class));
    org.mockito.Mockito.when(meta.getDatabaseProductName()).thenReturn("PostgreSQL");
    org.mockito.Mockito.when(meta.getDatabaseMajorVersion()).thenReturn(16);
    org.mockito.Mockito.when(meta.getDatabaseMinorVersion()).thenReturn(0);
    org.mockito.Mockito.when(meta.getDriverName()).thenReturn("Mock Driver");

    var testDriver = new TestDriver(connection);
    java.sql.DriverManager.registerDriver(testDriver);
    return testDriver;
  }

  @Test
  @DisplayName("create builds SessionFactory with annotated classes and applies password when set")
  void create_validConfigAndClasses_buildsSessionFactoryWithPassword() throws Exception {
    var testDriver = registerMockDriver();
    try {
      var factory = new DefaultSessionFactoryFactory();
      var config = SessionConfig.of("jdbc:mockpg://localhost/test", "user", "pass", 2, 10, 5);

      var sessionFactory = factory.create(config, List.of(DummyEntity.class));

      assertThat(sessionFactory).isNotNull();
      assertThat(sessionFactory.getProperties().get(AvailableSettings.JAKARTA_JDBC_PASSWORD))
          .isEqualTo("****");
      sessionFactory.close();
    } finally {
      java.sql.DriverManager.deregisterDriver(testDriver);
    }
  }

  @Test
  @DisplayName("create omits password setting when password is empty string and trustAuth is true")
  void create_emptyPassword_omitsPasswordSetting() throws Exception {
    var testDriver = registerMockDriver();
    try {
      var factory = new DefaultSessionFactoryFactory();
      var config = SessionConfig.of("jdbc:mockpg://localhost/test", "user", "", true, 2, 10, 5);

      var sessionFactory = factory.create(config, List.of(DummyEntity.class));

      assertThat(sessionFactory).isNotNull();
      assertThat(sessionFactory.getProperties().get(AvailableSettings.JAKARTA_JDBC_PASSWORD))
          .isNull();
      sessionFactory.close();
    } finally {
      java.sql.DriverManager.deregisterDriver(testDriver);
    }
  }

  @Test
  @DisplayName("create omits password setting when password is null and trustAuth is true")
  void create_nullPassword_omitsPasswordSetting() throws Exception {
    var testDriver = registerMockDriver();
    try {
      var factory = new DefaultSessionFactoryFactory();
      var config = SessionConfig.of("jdbc:mockpg://localhost/test", "user", null, true, 2, 10, 5);

      var sessionFactory = factory.create(config, List.of(DummyEntity.class));

      assertThat(sessionFactory).isNotNull();
      assertThat(sessionFactory.getProperties().get(AvailableSettings.JAKARTA_JDBC_PASSWORD))
          .isNull();
      sessionFactory.close();
    } finally {
      java.sql.DriverManager.deregisterDriver(testDriver);
    }
  }

  @Test
  @DisplayName("create throws NullPointerException when arguments are null")
  void create_nullArguments_throwsNullPointerException() {
    var factory = new DefaultSessionFactoryFactory();
    var config =
        SessionConfig.of("jdbc:postgresql://localhost:5432/test", "user", "pass", 2, 10, 5);

    assertThatThrownBy(() -> factory.create(null, List.of()))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("config cannot be null");

    assertThatThrownBy(() -> factory.create(config, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("annotatedClasses cannot be null");
  }
}
