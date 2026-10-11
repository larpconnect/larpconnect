package com.larpconnect.njall.data.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class V10CollectionsMigrationTest {

  @Test
  @DisplayName("V10 migration script exists and defines collections, items, constraints, and RLS")
  void v10MigrationScript_validContent() throws IOException {
    var resource =
        getClass().getClassLoader().getResourceAsStream("db/migration/V10__collections.sql");
    assertThat(resource).as("V10 migration resource must exist").isNotNull();

    String sql;
    try (var in = resource) {
      sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    assertThat(sql)
        .contains("CREATE TABLE IF NOT EXISTS njall_users.collections")
        .contains("tenant_id UUID NOT NULL")
        .contains("id UUID NOT NULL")
        .contains("owner_id UUID NOT NULL")
        .contains("name VARCHAR NOT NULL")
        .contains("PRIMARY KEY (tenant_id, id)")
        .contains("CONSTRAINT unq_collections_id UNIQUE (id)")
        .contains("CONSTRAINT fk_collections_entities FOREIGN KEY (tenant_id, id)")
        .contains("REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE")
        .contains("CONSTRAINT fk_collections_owner FOREIGN KEY (tenant_id, owner_id)")
        .contains("CONSTRAINT unq_collections_owner_name UNIQUE (tenant_id, owner_id, name)")
        .contains("CREATE INDEX IF NOT EXISTS idx_collections_owner")
        .contains("ALTER TABLE njall_users.collections ENABLE ROW LEVEL SECURITY")
        .contains("CREATE POLICY rls_collections ON njall_users.collections")
        .contains("CREATE POLICY rls_collections_admin ON njall_users.collections")
        .contains("CREATE TABLE IF NOT EXISTS njall_users.collection_items")
        .contains("id UUID PRIMARY KEY DEFAULT uuidv7()")
        .contains("ordinal INTEGER NOT NULL")
        .contains("added_on TIMESTAMPTZ NOT NULL DEFAULT NOW()")
        .contains("refers_to_tenant_id UUID NOT NULL")
        .contains("refers_to_id UUID NOT NULL")
        .contains(
            "CONSTRAINT fk_collection_items_collection FOREIGN KEY (tenant_id, collection_id)")
        .contains("CONSTRAINT fk_collection_items_entities FOREIGN KEY")
        .contains("(refers_to_tenant_id, refers_to_id)")
        .contains(
            "CONSTRAINT unq_collection_items_ref UNIQUE (tenant_id, collection_id, refers_to_id)")
        .contains("CREATE INDEX IF NOT EXISTS idx_collection_items_order")
        .contains("ALTER TABLE njall_users.collection_items ENABLE ROW LEVEL SECURITY")
        .contains("CREATE POLICY rls_collection_items ON njall_users.collection_items")
        .contains("CREATE POLICY rls_collection_items_admin ON njall_users.collection_items")
        .contains("GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.collections TO njall_users")
        .contains("GRANT ALL ON njall_users.collections TO njall_admin")
        .contains(
            "GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.collection_items TO njall_users")
        .contains("GRANT ALL ON njall_users.collection_items TO njall_admin");
  }
}
