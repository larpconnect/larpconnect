package com.larpconnect.njall.data.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class V4EntitiesAndLinksMigrationTest {

  @Test
  @DisplayName("V4 migration script exists and defines CTI schema, constraints, indices, and RLS")
  void v4MigrationScript_validContent() throws IOException {
    var resource =
        getClass()
            .getClassLoader()
            .getResourceAsStream("db/migration/V4__entities_and_links_cti.sql");
    assertThat(resource).as("V4 migration resource must exist").isNotNull();

    String sql;
    try (var in = resource) {
      sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    assertThat(sql)
        .contains("CREATE TABLE IF NOT EXISTS njall_users.entities")
        .contains("PRIMARY KEY (tenant_id, id)")
        .contains("CONSTRAINT unq_entities_global_id UNIQUE (id)")
        .contains("idx_entities_active")
        .contains("idx_entities_type_lookup")
        .contains("CREATE TABLE IF NOT EXISTS njall_users.links")
        .contains("CONSTRAINT fk_links_entities FOREIGN KEY (tenant_id, id)")
        .contains("REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE")
        .contains("ALTER TABLE njall_users.entities ENABLE ROW LEVEL SECURITY")
        .contains("ALTER TABLE njall_users.links ENABLE ROW LEVEL SECURITY")
        .contains("CREATE POLICY rls_entities ON njall_users.entities")
        .contains("CREATE POLICY rls_links ON njall_users.links")
        .contains("GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.entities TO njall_users")
        .contains("GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.links TO njall_users");
  }
}
