import os

base_dir = "data/src/main/java/com/larpconnect/njall/data"

def fix_file(filename, replacements):
    path = os.path.join(base_dir, filename)
    with open(path, "r") as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(path, "w") as f:
        f.write(content)

# Fix Spotbugs: EI / EI2 in StudioRole
fix_file("StudioRole.java", [
    ("public StudioRoleId getId() {\n        return id;\n    }",
     "public StudioRoleId getId() {\n        return new StudioRoleId(id.getStudioId(), id.getRoleId(), id.getIndividualId());\n    }"),
    ("public void setId(StudioRoleId id) {\n        this.id = id;\n    }",
     "public void setId(StudioRoleId id) {\n        this.id = new StudioRoleId(id.getStudioId(), id.getRoleId(), id.getIndividualId());\n    }")
])

# Fix Spotbugs: EI / EI2 in ActorEndpoint
fix_file("ActorEndpoint.java", [
    ("public ActorEndpointId getId() {\n        return id;\n    }",
     "public ActorEndpointId getId() {\n        return new ActorEndpointId(id.getActorId(), id.getName());\n    }"),
    ("public void setId(ActorEndpointId id) {\n        this.id = id;\n    }",
     "public void setId(ActorEndpointId id) {\n        this.id = new ActorEndpointId(id.getActorId(), id.getName());\n    }")
])

# Fix MissingCtor in DataModule and DataBindingModule
fix_file("DataModule.java", [
    ("public final class DataModule extends AbstractModule {\n",
     "public final class DataModule extends AbstractModule {\n\n    /**\n     * Constructor.\n     */\n    public DataModule() {\n    }\n")
])
fix_file("DataBindingModule.java", [
    ("final class DataBindingModule extends AbstractModule {\n",
     "final class DataBindingModule extends AbstractModule {\n\n    /**\n     * Constructor.\n     */\n    public DataBindingModule() {\n    }\n")
])

# Fix MagicNumber in NjallEntity
fix_file("NjallEntity.java", [
    ("length = 2048", "length = EXTERNAL_REFERENCE_LENGTH"),
    ("public abstract class NjallEntity {", "public abstract class NjallEntity {\n\n    private static final int EXTERNAL_REFERENCE_LENGTH = 2048;\n"),
    ("OffsetDateTime.now()", "OffsetDateTime.now(java.time.ZoneOffset.UTC)")
])

# Fix OffsetDateTime in CollectionItem
fix_file("CollectionItem.java", [
    ("OffsetDateTime.now()", "OffsetDateTime.now(java.time.ZoneOffset.UTC)")
])

# Fix missing serialVersionUID in StudioRoleId and ActorEndpointId
fix_file("StudioRoleId.java", [
    ("public final class StudioRoleId implements Serializable {", "public final class StudioRoleId implements Serializable {\n    private static final long serialVersionUID = 1L;\n")
])
fix_file("ActorEndpointId.java", [
    ("public final class ActorEndpointId implements Serializable {", "public final class ActorEndpointId implements Serializable {\n    private static final long serialVersionUID = 1L;\n")
])

print("Applied fixes")
