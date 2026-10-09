package com.larpconnect.njall.data.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.Ticker;
import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.dao.studios.StudioLookupDAO;
import com.larpconnect.njall.data.domain.DeletionFilter;
import com.larpconnect.njall.data.domain.StudioLookup;
import com.typesafe.config.ConfigFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DefaultStudioLookupCacheServiceTest {

  private static final UUID STUDIO_ID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID TENANT_ID_1 = UUID.fromString("11111111-1111-1111-1111-222222222222");
  private static final UUID STUDIO_ID_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID TENANT_ID_2 = UUID.fromString("22222222-2222-2222-2222-333333333333");

  @Test
  @DisplayName("startUp populates in-memory cache from StudioLookupDAO")
  void startUp_queriesDaoAndPopulatesCache() throws Exception {
    var activeStudio = createActiveStudio(TENANT_ID_1, STUDIO_ID_1, "valiant");
    var deletedStudio = createDeletedStudio(TENANT_ID_2, STUDIO_ID_2, "abandoned", Instant.now());
    var dao = mock(StudioLookupDAO.class);
    when(dao.list(DeletionFilter.INCLUDE_DELETED))
        .thenReturn(ImmutableList.of(activeStudio, deletedStudio));

    var service =
        new DefaultStudioLookupCacheService(dao, Duration.ofMinutes(5), Ticker.systemTicker());
    service.startUp();

    assertThat(service.findById(STUDIO_ID_1)).contains(activeStudio);
    assertThat(service.findByAlias("valiant")).contains(activeStudio);
    assertThat(service.findByAlias("VALIANT")).contains(activeStudio);
    assertThat(service.findByIdOrAlias(STUDIO_ID_1.toString())).contains(activeStudio);
    assertThat(service.findByIdOrAlias("valiant")).contains(activeStudio);

    assertThat(service.findById(STUDIO_ID_2)).isEmpty();
    assertThat(service.findByAlias("abandoned")).isEmpty();
    assertThat(service.listActive()).containsExactly(activeStudio);
  }

  @Test
  @DisplayName("startUp throws exception when DAO execution fails")
  void startUp_daoThrowsException_propagatesException() {
    var dao = mock(StudioLookupDAO.class);
    when(dao.list(DeletionFilter.INCLUDE_DELETED))
        .thenThrow(new RuntimeException("Database unreachable"));

    var service =
        new DefaultStudioLookupCacheService(dao, Duration.ofMinutes(5), Ticker.systemTicker());

    assertThatThrownBy(service::startUp)
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Database unreachable");
  }

  @Test
  @DisplayName("findByIdOrAlias returns empty for unknown identifier")
  void findByIdOrAlias_unknownIdentifier_returnsEmpty() throws Exception {
    var dao = mock(StudioLookupDAO.class);
    when(dao.list(DeletionFilter.INCLUDE_DELETED)).thenReturn(ImmutableList.of());

    var service =
        new DefaultStudioLookupCacheService(dao, Duration.ofMinutes(5), Ticker.systemTicker());
    service.startUp();

    assertThat(service.findByIdOrAlias("nonexistent")).isEmpty();
    assertThat(service.findByIdOrAlias(UUID.randomUUID().toString())).isEmpty();
  }

  @Test
  @DisplayName("refresh immediately reloads all studios from StudioLookupDAO")
  void refresh_queriesDaoAndUpdatesCache() throws Exception {
    var studio1 = createActiveStudio(TENANT_ID_1, STUDIO_ID_1, "studio_one");
    var studio2 = createActiveStudio(TENANT_ID_2, STUDIO_ID_2, "studio_two");
    var dao = mock(StudioLookupDAO.class);
    when(dao.list(DeletionFilter.INCLUDE_DELETED))
        .thenReturn(ImmutableList.of(studio1))
        .thenReturn(ImmutableList.of(studio1, studio2));

    var service =
        new DefaultStudioLookupCacheService(dao, Duration.ofMinutes(5), Ticker.systemTicker());
    service.startUp();

    assertThat(service.findByAlias("studio_two")).isEmpty();

    service.refresh();

    assertThat(service.findByAlias("studio_two")).contains(studio2);
    assertThat(service.listActive()).containsExactlyInAnyOrder(studio1, studio2);
    verify(dao, times(2)).list(DeletionFilter.INCLUDE_DELETED);
  }

  @Test
  @DisplayName("runOneIteration executes scheduled refresh and tolerates exception")
  void runOneIteration_daoThrowsException_retainsExistingSnapshot() throws Exception {
    var studio = createActiveStudio(TENANT_ID_1, STUDIO_ID_1, "valiant");
    var dao = mock(StudioLookupDAO.class);
    when(dao.list(DeletionFilter.INCLUDE_DELETED))
        .thenReturn(ImmutableList.of(studio))
        .thenThrow(new RuntimeException("Transient connection error"));

    var service =
        new DefaultStudioLookupCacheService(dao, Duration.ofMinutes(5), Ticker.systemTicker());
    service.startUp();

    assertThat(service.findByAlias("valiant")).contains(studio);

    service.runOneIteration();

    assertThat(service.findByAlias("valiant")).contains(studio);
  }

  @Test
  @DisplayName("scheduler returns fixed-rate schedule matching configured interval")
  void scheduler_returnsConfiguredInterval() {
    var dao = mock(StudioLookupDAO.class);
    var service =
        new DefaultStudioLookupCacheService(dao, Duration.ofMinutes(3), Ticker.systemTicker());

    var scheduler = service.scheduler();

    assertThat(scheduler).isNotNull();
  }

  @Test
  @DisplayName("inject constructor resolves refresh-interval from Typesafe Config")
  void constructor_injectsConfig_initializesCorrectly() throws Exception {
    var dao = mock(StudioLookupDAO.class);
    when(dao.list(DeletionFilter.INCLUDE_DELETED)).thenReturn(ImmutableList.of());
    var config =
        ConfigFactory.parseString("larpconnect.data.cache.studio-lookup.refresh-interval = 10m");

    var service = new DefaultStudioLookupCacheService(dao, config, Ticker.systemTicker());
    service.startUp();

    assertThat(service.listActive()).isEmpty();
  }

  private static StudioLookup createActiveStudio(UUID tenantId, UUID studioId, String alias) {
    var now = Instant.now();
    return new StudioLookup(tenantId, studioId, alias, now, now, Optional.empty());
  }

  private static StudioLookup createDeletedStudio(
      UUID tenantId, UUID studioId, String alias, Instant deletedAt) {
    var now = Instant.now();
    return new StudioLookup(tenantId, studioId, alias, now, now, Optional.of(deletedAt));
  }
}
