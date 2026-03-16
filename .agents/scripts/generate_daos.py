import os
from typing import List

entities = [
    "ActorEndpoint", "Collection", "CollectionItem", "ContactInfo",
    "Individual", "Studio", "Role", "StudioRole", "Location",
    "LarpSystem", "Campaign", "Game", "LarpCharacter", "CharacterInstance",
    "ServerMetadata"
]

dao_dir = "data/src/main/java/com/larpconnect/njall/data/dao"

for entity in entities:
    interface_name = f"{entity}Dao"
    impl_name = f"Default{interface_name}"

    id_type = "UUID"
    if entity == "ActorEndpoint":
        id_type = "ActorEndpoint.ActorEndpointId"
    elif entity == "StudioRole":
        id_type = "StudioRole.StudioRoleId"

    # Write Interface
    interface_content = f"""package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.{entity};
import io.smallrye.mutiny.Uni;
import java.util.UUID;

@DefaultImplementation({impl_name}.class)
public interface {interface_name} {{
  Uni<{entity}> findById({id_type} id);
  Uni<{entity}> persist({entity} entity);
}}
"""
    with open(os.path.join(dao_dir, f"{interface_name}.java"), "w") as f:
        f.write(interface_content)

    # Write Implementation
    impl_content = f"""package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.{entity};
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class {impl_name} implements {interface_name} {{
  private final Provider<Mutiny.SessionFactory> sessionFactoryProvider;

  @Inject
  {impl_name}(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {{
    this.sessionFactoryProvider = sessionFactoryProvider;
  }}

  @Override
  public Uni<{entity}> findById({id_type} id) {{
    return sessionFactoryProvider.get().withSession(session -> session.find({entity}.class, id));
  }}

  @Override
  public Uni<{entity}> persist({entity} entity) {{
    return sessionFactoryProvider
        .get()
        .withSession(
            session -> session.persist(entity).chain(session::flush).replaceWith(entity));
  }}
}}
"""
    with open(os.path.join(dao_dir, f"{impl_name}.java"), "w") as f:
        f.write(impl_content)

# Update DaoModule
module_content = f"""package com.larpconnect.njall.data.dao;

import com.google.inject.AbstractModule;

public final class DaoModule extends AbstractModule {{
  public DaoModule() {{}}

  @Override
  protected void configure() {{
    bind(EntityDao.class).to(DefaultEntityDao.class);
    bind(ActorDao.class).to(DefaultActorDao.class);
    bind(UserDao.class).to(DefaultUserDao.class);
{"".join(f'    bind({e}Dao.class).to(Default{e}Dao.class);\n' for e in entities)}  }}
}}
"""
with open(os.path.join(dao_dir, "DaoModule.java"), "w") as f:
    f.write(module_content)
