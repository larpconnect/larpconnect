package com.larpconnect.njall.data.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class V9IndividualsMigrationTest {

  @Test
  @DisplayName("V9 migration script exists and defines individuals table, constraints, and RLS")
  void v9MigrationScript_validContent() throws IOException {
    var resource =
        getClass().getClassLoader().getResourceAsStream("db/migration/V9__individuals.sql");
    assertThat(resource).as("V9 migration resource must exist").isNotNull();

    String sql;
    try (var in = resource) {
      sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    assertThat(sql)
        .contains("CREATE TABLE IF NOT EXISTS njall_users.individuals")
        .contains("tenant_id UUID NOT NULL")
        .contains("id UUID NOT NULL")
        .contains("name VARCHAR NOT NULL")
        .contains("PRIMARY KEY (tenant_id, id)")
        .contains("CONSTRAINT unq_individuals_id UNIQUE (id)")
        .contains("CONSTRAINT fk_individuals_entities FOREIGN KEY (tenant_id, id)")
        .contains("REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE")
        .contains("ALTER TABLE njall_users.individuals ENABLE ROW LEVEL SECURITY")
        .contains("CREATE POLICY rls_individuals ON njall_users.individuals")
        .contains("CREATE POLICY rls_individuals_admin ON njall_users.individuals")
        .contains("GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.individuals TO njall_users")
        .contains("GRANT ALL ON njall_users.individuals TO njall_admin");
  }
}
