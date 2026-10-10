package com.larpconnect.njall.data.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class V8EventsMigrationTest {

  @Test
  @DisplayName("V8 migration script exists and defines events table, constraints, indices, and RLS")
  void v8MigrationScript_validContent() throws IOException {
    var resource = getClass().getClassLoader().getResourceAsStream("db/migration/V8__events.sql");
    assertThat(resource).as("V8 migration resource must exist").isNotNull();

    String sql;
    try (var in = resource) {
      sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    assertThat(sql)
        .contains("CREATE TABLE IF NOT EXISTS njall_users.events")
        .contains("tenant_id UUID NOT NULL")
        .contains("id UUID NOT NULL")
        .contains("location_id UUID NULL")
        .contains("title VARCHAR NOT NULL")
        .contains("start_time TIMESTAMPTZ NULL")
        .contains("end_time TIMESTAMPTZ NULL")
        .contains("PRIMARY KEY (tenant_id, id)")
        .contains("CONSTRAINT unq_event_id UNIQUE (id)")
        .contains("CONSTRAINT fk_events_entities FOREIGN KEY (tenant_id, id)")
        .contains("REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE")
        .contains("CONSTRAINT fk_events_locations FOREIGN KEY (tenant_id, location_id)")
        .contains("REFERENCES njall_users.locations (tenant_id, id) ON DELETE SET NULL")
        .contains("CONSTRAINT chk_events_time_order CHECK")
        .contains("end_time IS NULL OR start_time IS NULL OR end_time >= start_time")
        .contains("idx_events_location")
        .contains("idx_events_time")
        .contains("ALTER TABLE njall_users.events ENABLE ROW LEVEL SECURITY")
        .contains("CREATE POLICY rls_events ON njall_users.events")
        .contains("CREATE POLICY rls_events_admin ON njall_users.events")
        .contains("GRANT SELECT, INSERT, UPDATE, DELETE ON njall_users.events TO njall_users")
        .contains("GRANT ALL ON njall_users.events TO njall_admin");
  }
}
