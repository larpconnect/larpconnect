package com.larpconnect.njall.data.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.larpconnect.njall.data.config.MigrationConfig;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

final class DefaultDataSourceFactoryTest {

  @Test
  @DisplayName("create returns configured PGSimpleDataSource with password")
  void create_validConfig_returnsDataSource() {
    var config =
        new MigrationConfig(
            "jdbc:postgresql://localhost:5432/app",
            "njall",
            "pass",
            List.of("njall"),
            "njall",
            Map.of());

    var factory = new DefaultDataSourceFactory();
    var ds = factory.create(config);

    assertThat(ds).isInstanceOf(PGSimpleDataSource.class);
    var pgDs = (PGSimpleDataSource) ds;
    assertThat(pgDs.getUrl()).startsWith("jdbc:postgresql://localhost:5432/app");
    assertThat(pgDs.getUser()).isEqualTo("njall");
    assertThat(pgDs.getPassword()).isEqualTo("pass");
  }

  @Test
  @DisplayName(
      "create returns configured PGSimpleDataSource without password when trustAuth is true")
  void create_trustAuthWithoutPassword_omitsPassword() {
    var config =
        new MigrationConfig(
            "jdbc:postgresql://localhost:5432/app",
            "njall",
            null,
            true,
            List.of("njall"),
            "njall",
            Map.of());

    var factory = new DefaultDataSourceFactory();
    var ds = factory.create(config);

    assertThat(ds).isInstanceOf(PGSimpleDataSource.class);
    var pgDs = (PGSimpleDataSource) ds;
    assertThat(pgDs.getUrl()).startsWith("jdbc:postgresql://localhost:5432/app");
    assertThat(pgDs.getUser()).isEqualTo("njall");
    assertThat(pgDs.getPassword()).isNull();
  }
}
