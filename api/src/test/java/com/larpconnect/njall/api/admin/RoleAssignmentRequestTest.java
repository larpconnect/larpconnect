package com.larpconnect.njall.api.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class RoleAssignmentRequestTest {

  private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

  @Test
  @DisplayName("overloaded constructors correctly wrap nullable roleId and roleName")
  void constructors_wrapOptionals() {
    var uuid = UUID.randomUUID();

    var fromUuid = new RoleAssignmentRequest(uuid);
    assertThat(fromUuid.roleId()).contains(uuid);
    assertThat(fromUuid.roleName()).isEmpty();

    var fromName = new RoleAssignmentRequest("admin_role");
    assertThat(fromName.roleId()).isEmpty();
    assertThat(fromName.roleName()).contains("admin_role");

    var fromBothNull = new RoleAssignmentRequest((UUID) null, (String) null);
    assertThat(fromBothNull.roleId()).isEmpty();
    assertThat(fromBothNull.roleName()).isEmpty();

    var fromBothPresent = new RoleAssignmentRequest(uuid, "admin_role");
    assertThat(fromBothPresent.roleId()).contains(uuid);
    assertThat(fromBothPresent.roleName()).contains("admin_role");
  }

  @Test
  @DisplayName("canonical constructor preserves optionals")
  void canonicalConstructor_preservesOptionals() {
    var uuid = UUID.randomUUID();
    var req = new RoleAssignmentRequest(Optional.of(uuid), Optional.of("admin"));
    assertThat(req.roleId()).contains(uuid);
    assertThat(req.roleName()).contains("admin");
  }

  @Test
  @DisplayName("Jackson deserializes JSON with roleName into Optional")
  void jackson_deserializesJson() throws Exception {
    var json = "{\"roleName\":\"security_admin\"}";
    var req = objectMapper.readValue(json, RoleAssignmentRequest.class);
    assertThat(req.roleId()).isEmpty();
    assertThat(req.roleName()).contains("security_admin");
  }
}
