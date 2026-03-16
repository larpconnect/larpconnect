import os
from typing import List

entities = [
    "ActorEndpoint", "Collection", "CollectionItem", "ContactInfo",
    "Individual", "Studio", "Role", "StudioRole", "Location",
    "LarpSystem", "Campaign", "Game", "LarpCharacter", "CharacterInstance",
    "ServerMetadata"
]

dao_dir = "data/src/test/java/com/larpconnect/njall/data/dao"

for entity in entities:
    interface_name = f"{entity}Dao"
    impl_name = f"Default{interface_name}"

    id_type = "UUID"
    if entity == "ActorEndpoint":
        id_type = "ActorEndpoint.ActorEndpointId"
    elif entity == "StudioRole":
        id_type = "StudioRole.StudioRoleId"

    test_content = f"""package com.larpconnect.njall.data.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.entity.{entity};
import io.smallrye.mutiny.Uni;
import jakarta.inject.Provider;
import java.util.UUID;
import java.util.function.Function;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class {impl_name}Test {{

  @Mock private Provider<Mutiny.SessionFactory> sessionFactoryProvider;
  @Mock private Mutiny.SessionFactory sessionFactory;
  @Mock private Mutiny.Session session;

  private {impl_name} dao;

  @BeforeEach
  void setUp() {{
    when(sessionFactoryProvider.get()).thenReturn(sessionFactory);
    dao = new {impl_name}(sessionFactoryProvider);
  }}

  @Test
  void findById_shouldReturnEntity_whenFound() {{
    {id_type} id = null;
    {entity} expectedEntity = new {entity}() {{}};

    when(sessionFactory.withSession(any()))
        .thenAnswer(
            invocation -> {{
              Function<Mutiny.Session, Uni<{entity}>> function = invocation.getArgument(0);
              return function.apply(session);
            }});
    when(session.find({entity}.class, id)).thenReturn(Uni.createFrom().item(expectedEntity));

    {entity} actualEntity = dao.findById(id).await().indefinitely();

    assertThat(actualEntity).isEqualTo(expectedEntity);
    verify(session).find({entity}.class, id);
  }}

  @Test
  void persist_shouldReturnPersistedEntity() {{
    {entity} entityToPersist = new {entity}() {{}};

    when(sessionFactory.withSession(any()))
        .thenAnswer(
            invocation -> {{
              Function<Mutiny.Session, Uni<{entity}>> function = invocation.getArgument(0);
              return function.apply(session);
            }});
    when(session.persist(entityToPersist)).thenReturn(Uni.createFrom().voidItem());
    when(session.flush()).thenReturn(Uni.createFrom().voidItem());

    {entity} persistedEntity = dao.persist(entityToPersist).await().indefinitely();

    assertThat(persistedEntity).isEqualTo(entityToPersist);
    verify(session).persist(entityToPersist);
    verify(session).flush();
  }}
}}
"""
    with open(os.path.join(dao_dir, f"{impl_name}Test.java"), "w") as f:
        f.write(test_content)
