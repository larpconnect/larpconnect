package com.larpconnect.njall.data.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class V5LocationsAndAddressesMigrationTest {

  @Test
  @DisplayName("V5 migration script exists and defines locations CTI, addresses, indices, and RLS")
  void v5MigrationScript_validContent() throws IOException {
    var resource =
        getClass()
            .getClassLoader()
            .getResourceAsStream("db/migration/V5__locations_and_addresses.sql");
    assertThat(resource).as("V5 migration resource must exist").isNotNull();

    String sql;
    try (var in = resource) {
      sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    assertThat(sql)
        .contains("CREATE TABLE IF NOT EXISTS njall_users.locations")
        .contains("PRIMARY KEY (tenant_id, id)")
        .contains("CONSTRAINT unq_locations_id UNIQUE (id)")
        .contains("CONSTRAINT fk_locations_entities FOREIGN KEY (tenant_id, id)")
        .contains("REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE")
        .contains("CREATE TABLE IF NOT EXISTS njall_users.addresses")
        .contains("PRIMARY KEY (tenant_id, id)")
        .contains("CONSTRAINT unq_addresses_id UNIQUE (id)")
        .contains("CONSTRAINT fk_addresses_locations FOREIGN KEY (tenant_id, location_id)")
        .contains("REFERENCES njall_users.locations (tenant_id, id) ON DELETE CASCADE")
        .contains("idx_addresses_location")
        .contains("idx_addresses_search")
        .contains("idx_addresses_geom")
        .contains("ALTER TABLE njall_users.locations ENABLE ROW LEVEL SECURITY")
        .contains("ALTER TABLE njall_users.addresses ENABLE ROW LEVEL SECURITY")
        .contains("CREATE POLICY rls_locations ON njall_users.locations")
        .contains("CREATE POLICY rls_addresses ON njall_users.addresses")
        .contains("GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.locations TO njall_users")
        .contains("GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.addresses TO njall_users");
  }
}
