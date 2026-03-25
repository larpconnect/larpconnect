package com.larpconnect.njall.data.dao;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class TenantContextTest {

  private Vertx vertx;

  @BeforeEach
  void setUp() {
    vertx = Vertx.vertx();
  }

  @AfterEach
  void tearDown() {
    vertx.close().toCompletionStage().toCompletableFuture().join();
  }

  @Test
  void getCurrentTenant_returnsSetTenant() throws Exception {
    var future =
        vertx
            .executeBlocking(
                () -> {
                  TenantContext.setCurrentTenant("testserver");
                  return TenantContext.getCurrentTenant();
                })
            .toCompletionStage()
            .toCompletableFuture();

    assertThat(future.get()).isEqualTo("njall_server_testserver");
  }

  @Test
  void getCurrentTenant_noTenantSet_returnsNull() throws Exception {
    var future =
        vertx
            .executeBlocking(() -> TenantContext.getCurrentTenant())
            .toCompletionStage()
            .toCompletableFuture();

    assertThat(future.get()).isNull();
  }

  @Test
  void clear_removesTenant() throws Exception {
    var future =
        vertx
            .executeBlocking(
                () -> {
                  TenantContext.setCurrentTenant("testserver");
                  TenantContext.clear();
                  return TenantContext.getCurrentTenant();
                })
            .toCompletionStage()
            .toCompletableFuture();

    assertThat(future.get()).isNull();
  }

  @Test
  void formatTenantId_empty_returnsNjallBase() {
    assertThat(TenantContext.formatTenantId("")).isEqualTo("njall_base");
  }

  @Test
  void formatTenantId_unprefixed_addsPrefix() {
    assertThat(TenantContext.formatTenantId("testserver")).isEqualTo("njall_server_testserver");
  }

  @Test
  void formatTenantId_alreadyPrefixed_returnsAsIs() {
    assertThat(TenantContext.formatTenantId("njall_server_testserver"))
        .isEqualTo("njall_server_testserver");
  }

  @Test
  void formatTenantId_null_returnsNjallBase() {
    assertThat(TenantContext.formatTenantId(null)).isEqualTo("njall_base");
  }

  @Test
  void formatTenantId_baseTenants_returnsAsIs() {
    assertThat(TenantContext.formatTenantId("njall_base")).isEqualTo("njall_base");
    assertThat(TenantContext.formatTenantId("njall_admin")).isEqualTo("njall_admin");
    assertThat(TenantContext.formatTenantId("njall_analytics")).isEqualTo("njall_analytics");
  }
}
