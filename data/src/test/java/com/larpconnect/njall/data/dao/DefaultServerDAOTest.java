package com.larpconnect.njall.data.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.domain.ContactType;
import com.larpconnect.njall.data.domain.RoleType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DefaultServerDAOTest {

  private SessionFactory sessionFactory;
  private Session session;
  private DefaultServerDAO dao;

  private final UUID serverId = UUID.randomUUID();
  private final UUID contactId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeEach
  void setUp() {
    sessionFactory = mock(SessionFactory.class);
    session = mock(Session.class);
    when(sessionFactory.openSession()).thenReturn(session);
    dao = new DefaultServerDAO(() -> sessionFactory);
  }

  @Test
  @DisplayName("findById returns empty when entity not found")
  void findById_whenNotFound_returnsEmpty() {
    when(session.find(ServerEntity.class, serverId)).thenReturn(null);

    var result = dao.findById(serverId);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("findById returns populated Server when found")
  @SuppressWarnings("unchecked")
  void findById_whenFound_returnsServerWithContacts() {
    var serverEntity = new ServerEntity(serverId, "alpha", "alpha.org", now);
    var contactEntity =
        new ServerContactEntity(contactId, RoleType.ADMIN, ContactType.EMAIL, "ops@alpha.org", 0);
    Query<ServerContactEntity> contactQuery = mock(Query.class);

    when(session.find(ServerEntity.class, serverId)).thenReturn(serverEntity);
    when(session.createQuery(anyString(), eq(ServerContactEntity.class))).thenReturn(contactQuery);
    when(contactQuery.list()).thenReturn(List.of(contactEntity));

    var result = dao.findById(serverId);

    assertThat(result).isPresent();
    var server = result.get();
    assertThat(server.id()).isEqualTo(serverId);
    assertThat(server.name()).isEqualTo("alpha");
    assertThat(server.primaryDomain()).isEqualTo("alpha.org");
    assertThat(server.contacts()).hasSize(1);
    var contact = server.contacts().getFirst();
    assertThat(contact.id()).isEqualTo(contactId);
    assertThat(contact.roleType()).isEqualTo(RoleType.ADMIN);
    assertThat(contact.contactType()).isEqualTo(ContactType.EMAIL);
    assertThat(contact.contact()).isEqualTo("ops@alpha.org");
  }

  @Test
  @DisplayName("list returns empty list when no servers exist")
  @SuppressWarnings("unchecked")
  void list_whenEmpty_returnsEmptyList() {
    Query<ServerEntity> serverQuery = mock(Query.class);
    when(session.createQuery("from ServerEntity", ServerEntity.class)).thenReturn(serverQuery);
    when(serverQuery.list()).thenReturn(List.of());

    var result = dao.list();

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("list returns populated server list")
  @SuppressWarnings("unchecked")
  void list_whenServersExist_returnsList() {
    var serverEntity = new ServerEntity(serverId, "alpha", "alpha.org", now);
    var contactEntity =
        new ServerContactEntity(contactId, RoleType.ADMIN, ContactType.EMAIL, "ops@alpha.org", 0);
    Query<ServerEntity> serverQuery = mock(Query.class);
    Query<ServerContactEntity> contactQuery = mock(Query.class);

    when(session.createQuery("from ServerEntity", ServerEntity.class)).thenReturn(serverQuery);
    when(serverQuery.list()).thenReturn(List.of(serverEntity));
    when(session.createQuery(anyString(), eq(ServerContactEntity.class))).thenReturn(contactQuery);
    when(contactQuery.list()).thenReturn(List.of(contactEntity));

    var result = dao.list();

    assertThat(result).hasSize(1);
    var server = result.getFirst();
    assertThat(server.name()).isEqualTo("alpha");
    assertThat(server.contacts()).hasSize(1);
  }
}
