package com.larpconnect.njall.integration;

import com.larpconnect.njall.data.cache.StudioLookupCacheService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Helper methods for database operations, provisioning, and timestamp substitution. */
final class E2EFeatherpubDataHelper {

  private E2EFeatherpubDataHelper() {}

  public static void executeSql(String url, String pwd, String sql, Object... params)
      throws SQLException {
    try (var conn = DriverManager.getConnection(url, "njall", pwd);
        var stmt = conn.prepareStatement(sql)) {
      for (int i = 0; i < params.length; i++) {
        stmt.setObject(i + 1, params[i]);
      }
      stmt.executeUpdate();
    }
  }

  public static void truncateTables(
      String jdbcUrl, String password, @Nullable StudioLookupCacheService cacheService)
      throws SQLException {
    var sql =
        "TRUNCATE TABLE njall_admin.studios_lookup, njall_users.studios, "
            + "njall_users.entities, njall_users.actors CASCADE";
    executeSql(jdbcUrl, password, sql);
    try {
      executeSql(jdbcUrl, password, "TRUNCATE TABLE njall_users.inbox_activities CASCADE");
    } catch (SQLException expected) {
      // Table may not exist yet in test migrations
    }
    if (cacheService != null) {
      cacheService.refresh();
    }
  }

  public static boolean queryCount(String url, String pwd, String sql, Object... params)
      throws SQLException {
    try (var conn = DriverManager.getConnection(url, "njall", pwd);
        var s = conn.prepareStatement(sql)) {
      for (int i = 0; i < params.length; i++) {
        s.setObject(i + 1, params[i]);
      }
      try (var rs = s.executeQuery()) {
        return rs.next() && rs.getInt(1) > 0;
      }
    } catch (SQLException e) {
      if (e.getMessage() != null && e.getMessage().contains("does not exist")) {
        return false;
      }
      throw e;
    }
  }

  public static boolean deduplicationRecordExists(
      String url, String pwd, String actId, String targetId) throws SQLException {
    var sql =
        "SELECT COUNT(*) FROM njall_users.inbox_activities WHERE activity_id = ? AND"
            + " target_actor_id = ?::uuid";
    return queryCount(url, pwd, sql, actId, targetId);
  }

  public static int countDeduplicationRecords(String url, String pwd, String actId)
      throws SQLException {
    var sql = "SELECT COUNT(*) FROM njall_users.inbox_activities WHERE activity_id = ?";
    try (var conn = DriverManager.getConnection(url, "njall", pwd);
        var s = conn.prepareStatement(sql)) {
      s.setString(1, actId);
      try (var rs = s.executeQuery()) {
        return rs.next() ? rs.getInt(1) : 0;
      }
    }
  }

  public static void provisionBulkActors(
      String url, String pwd, UUID tenantId, UUID generatorId, String prefix, int count)
      throws SQLException {
    var entitySql =
        "INSERT INTO njall_users.entities (id, tenant_id, entity_type, summary, generator) "
            + "VALUES (?, ?, 'Actor', ?, ?)";
    var actorSql =
        "INSERT INTO njall_users.actors (tenant_id, id, preferred_username) VALUES (?, ?, ?)";
    try (var conn = DriverManager.getConnection(url, "njall", pwd);
        var entityStmt = conn.prepareStatement(entitySql);
        var actorStmt = conn.prepareStatement(actorSql)) {
      conn.setAutoCommit(false);
      for (int i = 0; i < count; i++) {
        var actorId = UUID.randomUUID();
        var username = prefix + i;
        entityStmt.setObject(1, actorId);
        entityStmt.setObject(2, tenantId);
        entityStmt.setString(3, "Bulk " + username);
        entityStmt.setObject(4, generatorId);
        entityStmt.addBatch();
        actorStmt.setObject(1, tenantId);
        actorStmt.setObject(2, actorId);
        actorStmt.setString(3, username);
        actorStmt.addBatch();
      }
      entityStmt.executeBatch();
      actorStmt.executeBatch();
      conn.commit();
    }
  }

  public static void expireDeduplicationRecord(String url, String pwd, String actId)
      throws SQLException {
    var sql =
        "UPDATE njall_users.inbox_activities SET expires_on = NOW() - INTERVAL '1 minute' WHERE"
            + " activity_id = ?";
    try {
      executeSql(url, pwd, sql, actId);
    } catch (SQLException e) {
      if (e.getMessage() == null || !e.getMessage().contains("does not exist")) {
        throw e;
      }
    }
  }

  public static boolean isDeduplicationVisibleToTenant(
      String url, String pwd, String actId, UUID tenantId) throws SQLException {
    var sql =
        "SELECT COUNT(*) FROM njall_users.inbox_activities WHERE activity_id = ? AND tenant_id"
            + " = ?";
    return queryCount(url, pwd, sql, actId, tenantId);
  }

  public static UUID[] provisionStudio(
      String url,
      String pwd,
      String alias,
      String name,
      @Nullable StudioLookupCacheService cacheService)
      throws SQLException {
    try (var conn = DriverManager.getConnection(url, "njall", pwd);
        var s =
            conn.prepareStatement(
                "SELECT tenant_id, studio_id FROM njall_admin.studios_lookup WHERE alias = ?")) {
      s.setString(1, alias);
      try (var rs = s.executeQuery()) {
        if (rs.next()) {
          return new UUID[] {(UUID) rs.getObject(1), (UUID) rs.getObject(2)};
        }
      }
    }
    var tId = UUID.randomUUID();
    var sId = UUID.randomUUID();
    executeSql(url, pwd, "INSERT INTO njall_users.studios (id, name) VALUES (?, ?)", tId, name);
    var sql = "INSERT INTO njall_admin.studios_lookup VALUES (?, ?, ?, NOW(), NOW(), NULL)";
    executeSql(url, pwd, sql, tId, sId, alias);
    if (cacheService != null) {
      cacheService.refresh();
    }
    return new UUID[] {tId, sId};
  }

  public static void provisionActor(
      String url, String pwd, UUID tId, UUID aId, String u, @Nullable UUID gen, String studio)
      throws SQLException {
    var eSql =
        "INSERT INTO njall_users.entities (id, tenant_id, entity_type, summary, generator) "
            + "VALUES (?, ?, 'Actor', ?, ?)";
    executeSql(url, pwd, eSql, aId, tId, "Actor " + u, gen != null ? gen : aId);
    var aSql =
        "INSERT INTO njall_users.actors (tenant_id, id, preferred_username) VALUES (?, ?, ?)";
    executeSql(url, pwd, aSql, tId, aId, u);
    var epSql =
        "INSERT INTO njall_users.actor_endpoints (tenant_id, actor_id, name, endpoint)"
            + " VALUES (?, ?, 'inbox', ?)";
    executeSql(
        url, pwd, epSql, tId, aId, "/api/studios/" + studio + "/v1/actors/" + aId + "/inbox");
    var inId = UUID.randomUUID();
    var outId = UUID.randomUUID();
    var cSql =
        "INSERT INTO njall_users.entities (id, tenant_id, entity_type, summary, generator)"
            + " VALUES (?, ?, 'OrderedCollection', ?, ?)";
    executeSql(url, pwd, cSql, inId, tId, "Inbox " + u, aId);
    executeSql(url, pwd, cSql, outId, tId, "Outbox " + u, aId);
    var cInSql =
        "INSERT INTO njall_users.collections (tenant_id, id, owner_id, name)"
            + " VALUES (?, ?, ?, 'inbox')";
    executeSql(url, pwd, cInSql, tId, inId, aId);
    var cOutSql =
        "INSERT INTO njall_users.collections (tenant_id, id, owner_id, name)"
            + " VALUES (?, ?, ?, 'outbox')";
    executeSql(url, pwd, cOutSql, tId, outId, aId);
  }

  public static void provisionLineage(
      String url, String pwd, UUID tId, int d, String root, String leaf, String studio)
      throws SQLException {
    UUID pId = null;
    var q =
        "SELECT id FROM njall_users.actors WHERE tenant_id = ? AND lower(preferred_username) ="
            + " lower(?)";
    try (var conn = DriverManager.getConnection(url, "njall", pwd);
        var s = conn.prepareStatement(q)) {
      s.setObject(1, tId);
      s.setString(2, root);
      try (var rs = s.executeQuery()) {
        if (rs.next()) {
          pId = (UUID) rs.getObject(1);
        }
      }
    }
    if (pId == null) {
      pId = UUID.randomUUID();
      provisionActor(url, pwd, tId, pId, root, null, studio);
    }
    for (int i = 1; i <= d; i++) {
      var cId = UUID.randomUUID();
      provisionActor(url, pwd, tId, cId, (i == d) ? leaf : (root + "_step_" + i), pId, studio);
      pId = cId;
    }
  }

  public static void provisionCyclicRelationship(
      String url, String pwd, UUID tId, UUID a1, String u1, UUID a2, String u2, String studio)
      throws SQLException {
    provisionActor(url, pwd, tId, a1, u1, a1, studio);
    provisionActor(url, pwd, tId, a2, u2, a1, studio);
    executeSql(url, pwd, "UPDATE njall_users.entities SET generator = ? WHERE id = ?", a2, a1);
  }

  public static void softDeleteActor(String url, String pwd, UUID id) throws SQLException {
    executeSql(url, pwd, "UPDATE njall_users.entities SET deleted_on = NOW() WHERE id = ?", id);
  }

  public static void softDeleteStudio(
      String url, String pwd, String alias, @Nullable StudioLookupCacheService cacheService)
      throws SQLException {
    executeSql(
        url,
        pwd,
        "UPDATE njall_admin.studios_lookup SET deleted_at = NOW() WHERE alias = ?",
        alias);
    if (cacheService != null) {
      cacheService.refresh();
    }
  }

  public static String substituteTimestamps(String in) {
    var n = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    var d8 = n.minus(8, ChronoUnit.DAYS);
    return in.replace("{CURRENT_TIMESTAMP}", n.toString())
        .replace("{TIMESTAMP_PLUS_4_MINUTES}", n.plus(4, ChronoUnit.MINUTES).toString())
        .replace("{TIMESTAMP_PLUS_6_MINUTES}", n.plus(6, ChronoUnit.MINUTES).toString())
        .replace("{TIMESTAMP_MINUS_7_DAYS}", n.minus(7, ChronoUnit.DAYS).toString())
        .replace("{TIMESTAMP_MINUS_9_DAYS}", n.minus(9, ChronoUnit.DAYS).toString())
        .replace("{TIMESTAMP_MINUS_8_DAYS_MINUS_1_MIN}", d8.minus(1, ChronoUnit.MINUTES) + "")
        .replace("{TIMESTAMP_MINUS_8_DAYS_PLUS_1_MIN}", d8.plus(1, ChronoUnit.MINUTES) + "");
  }

  public static String computeContentDigestHeader(String alg, String p)
      throws NoSuchAlgorithmException {
    var norm = alg.toLowerCase(Locale.ROOT);
    var md = MessageDigest.getInstance(norm.equals("sha3-256") ? "SHA3-256" : "SHA-256");
    var h = Base64.getUrlEncoder().encodeToString(md.digest(p.getBytes(StandardCharsets.UTF_8)));
    return norm + "=:" + h + ":";
  }
}
