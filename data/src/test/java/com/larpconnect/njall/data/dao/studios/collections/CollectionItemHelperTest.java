package com.larpconnect.njall.data.dao.studios.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.domain.studios.ExpandedEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class CollectionItemHelperTest {

  @Test
  @DisplayName("queryExpandedItems hydrates System, Campaign, and Game rows")
  void queryExpandedItems_hydratesSystemCampaignAndGame() {
    var session = mock(Session.class);
    var tenantId = UUID.randomUUID();
    var collectionId = UUID.randomUUID();
    var sysId = UUID.randomUUID();
    var campId = UUID.randomUUID();
    var gameId = UUID.randomUUID();
    var generatorId = UUID.randomUUID();

    NativeQuery<Object[]> query = mock();
    when(session.createNativeQuery(anyString(), eq(Object[].class))).thenReturn(query);
    when(query.setParameter("tenantId", tenantId)).thenReturn(query);
    when(query.setParameter("collectionId", collectionId)).thenReturn(query);
    when(query.getResultList())
        .thenReturn(
            List.of(
                new Object[] {sysId, generatorId, "System", null, "Vampire: The Masquerade"},
                new Object[] {
                  campId, generatorId, "Campaign", "Camarilla chronicle", "Seattle By Night"
                },
                new Object[] {gameId, generatorId, "Game", "Opening session", "Opening Elysium"}));

    var items = CollectionItemHelper.queryExpandedItems(session, tenantId, collectionId);

    assertThat(items)
        .containsExactly(
            new ExpandedEntity(
                sysId,
                generatorId,
                "System",
                Optional.of("Vampire: The Masquerade"),
                Optional.empty()),
            new ExpandedEntity(
                campId,
                generatorId,
                "Campaign",
                Optional.of("Seattle By Night"),
                Optional.of("Camarilla chronicle")),
            new ExpandedEntity(
                gameId,
                generatorId,
                "Game",
                Optional.of("Opening Elysium"),
                Optional.of("Opening session")));
  }

  @Test
  @DisplayName("queryExpandedItems returns empty list when collection has no active items")
  void queryExpandedItems_emptyResult_returnsEmptyList() {
    var session = mock(Session.class);
    var tenantId = UUID.randomUUID();
    var collectionId = UUID.randomUUID();

    NativeQuery<Object[]> query = mock();
    when(session.createNativeQuery(anyString(), eq(Object[].class))).thenReturn(query);
    when(query.setParameter("tenantId", tenantId)).thenReturn(query);
    when(query.setParameter("collectionId", collectionId)).thenReturn(query);
    when(query.getResultList()).thenReturn(List.of());

    var items = CollectionItemHelper.queryExpandedItems(session, tenantId, collectionId);

    assertThat(items).isEmpty();
  }
}
