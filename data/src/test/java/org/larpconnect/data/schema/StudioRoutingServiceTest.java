package org.larpconnect.data.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.Provider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link DefaultStudioRoutingService}. */
public final class StudioRoutingServiceTest {
  private SessionFactory sessionFactory;
  private Session session;
  private Transaction transaction;
  private NativeQuery<Object[]> nativeQuery;
  private Provider<SessionFactory> sessionFactoryProvider;
  private DefaultStudioRoutingService service;

  @BeforeEach
  @SuppressWarnings("unchecked")
  public void setUp() {
    sessionFactory = mock(SessionFactory.class);
    session = mock(Session.class);
    transaction = mock(Transaction.class);
    nativeQuery = mock(NativeQuery.class);
    org.hibernate.SessionBuilder sessionBuilder = mock(org.hibernate.SessionBuilder.class);

    when(sessionFactory.openSession()).thenReturn(session);
    when(sessionFactory.withOptions()).thenReturn(sessionBuilder);
    when(sessionBuilder.tenantIdentifier(org.mockito.ArgumentMatchers.any(Object.class)))
        .thenReturn(sessionBuilder);
    when(sessionBuilder.openSession()).thenReturn(session);

    when(session.beginTransaction()).thenReturn(transaction);
    when(session.getTransaction()).thenReturn(transaction);
    when(session.createNativeQuery(anyString(), org.mockito.ArgumentMatchers.any(Class.class)))
        .thenReturn(nativeQuery);

    sessionFactoryProvider = () -> sessionFactory;
  }

  @Test
  public void getSchemaName_adminName_returnsCoreAdminSchema() {
    service = new DefaultStudioRoutingService(sessionFactoryProvider);
    Optional<String> schema = service.getSchemaName("admin");
    assertThat(schema).hasValue("njall_core_admin");
  }

  @Test
  public void getSchemaName_nullValues_returnsEmpty() {
    service = new DefaultStudioRoutingService(sessionFactoryProvider);
    assertThat(service.getSchemaName((UUID) null)).isEmpty();
    assertThat(service.getSchemaName((String) null)).isEmpty();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void getSchemaName_lookupByUuidAndName_returnsMappedSchema() {
    UUID studioId = UUID.randomUUID();
    Object[] row = new Object[] {studioId, "mystudio", "njall_encodeduuid"};
    org.mockito.Mockito.doReturn(List.of(new Object[][] {row})).when(nativeQuery).getResultList();

    service = new DefaultStudioRoutingService(sessionFactoryProvider);

    Optional<String> schemaByUuid = service.getSchemaName(studioId);
    assertThat(schemaByUuid).hasValue("njall_encodeduuid");

    Optional<String> schemaByName = service.getSchemaName("mystudio");
    assertThat(schemaByName).hasValue("njall_encodeduuid");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void loadMappings_databaseThrowsException_returnsEmptyMappings() {
    when(sessionFactory.openSession()).thenThrow(new RuntimeException("DB Connection failed"));

    service = new DefaultStudioRoutingService(sessionFactoryProvider);
    Optional<String> schema = service.getSchemaName(UUID.randomUUID());
    assertThat(schema).isEmpty();
  }
}
