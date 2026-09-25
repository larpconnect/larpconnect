package com.larpconnect.njall.api.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.domain.AdminUserStatus;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class CreateUserRequestTest {

  @Test
  @DisplayName("overloaded constructor instantiates request with null and non-null roles")
  void constructor_withAndWithoutRoles() {
    var reqNullRoles = new CreateUserRequest("admin", AdminUserStatus.ACTIVE, (List<String>) null);
    assertThat(reqNullRoles.username()).isEqualTo("admin");
    assertThat(reqNullRoles.status()).isEqualTo(AdminUserStatus.ACTIVE);
    assertThat(reqNullRoles.roles()).isEmpty();

    var reqWithRoles =
        new CreateUserRequest("admin", AdminUserStatus.ACTIVE, List.of("superadmin"));
    assertThat(reqWithRoles.roles()).containsExactly("superadmin");
  }

  @Test
  @DisplayName("constructor copies non-null roles immutably and normalizes null status to UNKNOWN")
  void constructor_copiesRoles() {
    var req = new CreateUserRequest("admin", null, ImmutableList.of("admin_role"));
    assertThat(req.status()).isEqualTo(AdminUserStatus.UNKNOWN);
    assertThat(req.roles()).containsExactly("admin_role");
  }
}
