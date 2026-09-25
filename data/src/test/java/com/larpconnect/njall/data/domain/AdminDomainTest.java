package com.larpconnect.njall.data.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class AdminDomainTest {

  private final UUID roleId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @Test
  @DisplayName("AdminRole constructs and validates fields")
  void adminRole_constructsSuccessfully() {
    var role = new AdminRole(roleId, "auditor");

    assertThat(role.id()).isEqualTo(roleId);
    assertThat(role.roleName()).isEqualTo("auditor");
    assertThat(role).isInstanceOf(DatabaseObject.class);
  }

  @Test
  @DisplayName("AdminUser constructs and validates fields")
  void adminUser_constructsSuccessfully() {
    var role = new AdminRole(roleId, "security_admin");
    var user = new AdminUser(userId, "admin_user", AdminUserStatus.ACTIVE, now, now, List.of(role));

    assertThat(user.id()).isEqualTo(userId);
    assertThat(user.username()).isEqualTo("admin_user");
    assertThat(user.status()).isEqualTo(AdminUserStatus.ACTIVE);
    assertThat(user.createdAt()).isEqualTo(now);
    assertThat(user.updatedAt()).isEqualTo(now);
    assertThat(user.roles()).containsExactly(role);
    assertThat(user).isInstanceOf(DatabaseObject.class);
  }

  @Test
  @DisplayName("StudioLookup constructs and checks isDeleted")
  void studioLookup_constructsSuccessfully() {
    var activeStudio = new StudioLookup(tenantId, studioId, "valhalla", now, now, (Instant) null);
    assertThat(activeStudio.id()).isEqualTo(studioId);
    assertThat(activeStudio.tenantId()).isEqualTo(tenantId);
    assertThat(activeStudio.studioId()).isEqualTo(studioId);
    assertThat(activeStudio.alias()).isEqualTo("valhalla");
    assertThat(activeStudio.deletedAt()).isEmpty();
    assertThat(activeStudio.isDeleted()).isFalse();
    assertThat(activeStudio).isInstanceOf(DatabaseObject.class);

    var deletedStudio = new StudioLookup(tenantId, studioId, "valhalla", now, now, now);
    assertThat(deletedStudio.deletedAt()).contains(now);
    assertThat(deletedStudio.isDeleted()).isTrue();

    var explicitOptStudio =
        new StudioLookup(tenantId, studioId, "valhalla", now, now, java.util.Optional.empty());
    assertThat(explicitOptStudio.deletedAt()).isEmpty();
    assertThat(explicitOptStudio.isDeleted()).isFalse();
  }
}
