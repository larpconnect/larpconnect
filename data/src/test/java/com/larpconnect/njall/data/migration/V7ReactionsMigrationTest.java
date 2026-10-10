package com.larpconnect.njall.data.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class V7ReactionsMigrationTest {

  @Test
  @DisplayName(
      "V7 migration script exists and defines reactions, materialized view, indices, and RLS")
  void v7MigrationScript_validContent() throws IOException {
    var resource =
        getClass().getClassLoader().getResourceAsStream("db/migration/V7__reactions.sql");
    assertThat(resource).as("V7 migration resource must exist").isNotNull();

    String sql;
    try (var in = resource) {
      sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    assertThat(sql)
        .contains("CREATE TABLE IF NOT EXISTS njall_users.reactions")
        .contains("PRIMARY KEY (tenant_id, id)")
        .contains("CONSTRAINT unq_reaction_id UNIQUE (id)")
        .contains("CONSTRAINT fk_reactions_entities FOREIGN KEY (tenant_id, target_id)")
        .contains("REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE")
        .contains("CONSTRAINT fk_reactions_links FOREIGN KEY (tenant_id, link_id)")
        .contains("REFERENCES njall_users.links (tenant_id, id) ON DELETE SET NULL")
        .contains("idx_reactions_target_count")
        .contains("ALTER TABLE njall_users.reactions ENABLE ROW LEVEL SECURITY")
        .contains("CREATE POLICY rls_reactions ON njall_users.reactions")
        .contains("CREATE POLICY rls_reactions_admin ON njall_users.reactions")
        .contains("GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.reactions TO njall_users")
        .contains("GRANT ALL ON njall_users.reactions TO njall_admin")
        .contains("CREATE MATERIALIZED VIEW njall_users.reaction_counts")
        .contains("unq_reaction_counts_bucket")
        .contains("NULLS NOT DISTINCT")
        .contains("idx_reaction_counts_target")
        .contains("idx_reaction_counts_popular")
        .contains("GRANT SELECT ON njall_users.reaction_counts TO njall_users")
        .contains("GRANT ALL ON njall_users.reaction_counts TO njall_admin");
  }
}
