package com.larpconnect.njall.data.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.larpconnect.njall.data.config.MigrationConfig;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DefaultFlywayFactoryTest {

  @Test
  @DisplayName("create returns configured Flyway instance")
  void create_validParameters_returnsConfiguredFlyway() {
    var config =
        MigrationConfig.of(
            "jdbc:postgresql://localhost:5432/app",
            "njall",
            "pass",
            List.of("njall", "njall_admin"),
            "njall",
            Map.of("server_name", "test-server"));
    var dataSource = mock(DataSource.class);

    var factory = new DefaultFlywayFactory();
    var flyway = factory.create(config, dataSource);

    assertThat(flyway).isNotNull();
    assertThat(flyway.getConfiguration().getSchemas()).containsExactly("njall", "njall_admin");
    assertThat(flyway.getConfiguration().getDefaultSchema()).isEqualTo("njall");
    assertThat(flyway.getConfiguration().getPlaceholders())
        .containsEntry("server_name", "test-server");
  }
}
