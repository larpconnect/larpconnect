package org.larpconnect.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public final class EntityTest {

  @Test
  public void testStudio() {
    UUID id = UUID.randomUUID();
    Studio studio = new Studio(id, "Test Studio", "schema_1");
    assertThat(studio.getId()).isEqualTo(id);
    assertThat(studio.getName()).isEqualTo("Test Studio");
    assertThat(studio.getSchemaName()).isEqualTo("schema_1");
    assertThat(studio.getDeletedAt()).isNull();

    studio.setName("New Name");
    studio.setSchemaName("schema_2");
    Instant now = Instant.now();
    studio.setDeletedAt(now);

    assertThat(studio.getName()).isEqualTo("New Name");
    assertThat(studio.getSchemaName()).isEqualTo("schema_2");
    assertThat(studio.getDeletedAt()).isEqualTo(now);

    Studio studio2 = new Studio(id, "New Name", "schema_2");
    assertThat(studio).isEqualTo(studio2);
    assertThat(studio.hashCode()).isEqualTo(studio2.hashCode());
    assertThat(studio).isNotEqualTo(new Studio());

    // equals branches coverage in AbstractEntity
    Object sameStudio = studio;
    assertThat(studio.equals(sameStudio)).isTrue(); // this == o
    Object otherType = "some string";
    assertThat(studio.equals(otherType)).isFalse(); // !(o instanceof AbstractEntity)

    Studio empty = new Studio();
    empty.setId(id);
    assertThat(empty.getId()).isEqualTo(id);
  }

  @Test
  public void testLarpSystem() {
    UUID id = UUID.randomUUID();
    LarpSystem system = new LarpSystem(id, "System 1");
    assertThat(system.getId()).isEqualTo(id);
    assertThat(system.getName()).isEqualTo("System 1");
    assertThat(system.getCreatedOn()).isNull();
    assertThat(system.getUpdatedOn()).isNull();
    assertThat(system.getDeletedOn()).isNull();

    system.setName("System 2");
    Instant deleted = Instant.now();
    system.setDeletedOn(deleted);

    assertThat(system.getName()).isEqualTo("System 2");
    assertThat(system.getDeletedOn()).isEqualTo(deleted);

    LarpSystem system2 = new LarpSystem(id, "System 2");
    assertThat(system).isEqualTo(system2);
  }

  @Test
  public void testCampaign() {
    UUID id = UUID.randomUUID();
    LarpSystem system = new LarpSystem(UUID.randomUUID(), "System");
    Campaign campaign = new Campaign(id, system);
    assertThat(campaign.getId()).isEqualTo(id);
    assertThat(campaign.getSystem()).isEqualTo(system);

    LarpSystem system2 = new LarpSystem(UUID.randomUUID(), "System 2");
    campaign.setSystem(system2);
    assertThat(campaign.getSystem()).isEqualTo(system2);

    Campaign campaign2 = new Campaign(id, system2);
    assertThat(campaign).isEqualTo(campaign2);
    assertThat(campaign).isNotEqualTo(new Campaign());
  }

  @Test
  public void testGame() {
    UUID id = UUID.randomUUID();
    Campaign campaign = new Campaign(UUID.randomUUID(), null);
    Game game = new Game(id, campaign);
    assertThat(game.getId()).isEqualTo(id);
    assertThat(game.getCampaign()).isEqualTo(campaign);

    Campaign campaign2 = new Campaign(UUID.randomUUID(), null);
    game.setCampaign(campaign2);
    assertThat(game.getCampaign()).isEqualTo(campaign2);

    Game game2 = new Game(id, campaign2);
    assertThat(game).isEqualTo(game2);
    assertThat(game).isNotEqualTo(new Game());
  }

  @Test
  public void testLarpCharacter() {
    UUID id = UUID.randomUUID();
    Campaign campaign = new Campaign(UUID.randomUUID(), null);
    LarpCharacter character = new LarpCharacter(id, campaign, "Hero");
    assertThat(character.getId()).isEqualTo(id);
    assertThat(character.getCampaign()).isEqualTo(campaign);
    assertThat(character.getNameTemplate()).isEqualTo("Hero");

    Campaign campaign2 = new Campaign(UUID.randomUUID(), null);
    character.setCampaign(campaign2);
    character.setNameTemplate("Villain");
    assertThat(character.getCampaign()).isEqualTo(campaign2);
    assertThat(character.getNameTemplate()).isEqualTo("Villain");

    LarpCharacter character2 = new LarpCharacter(id, campaign2, "Villain");
    assertThat(character).isEqualTo(character2);
    assertThat(character).isNotEqualTo(new LarpCharacter());
  }

  @Test
  public void testIndividual() {
    UUID id = UUID.randomUUID();
    Individual ind = new Individual(id, "Alice");
    assertThat(ind.getId()).isEqualTo(id);
    assertThat(ind.getName()).isEqualTo("Alice");

    ind.setName("Bob");
    assertThat(ind.getName()).isEqualTo("Bob");

    Individual ind2 = new Individual(id, "Bob");
    assertThat(ind).isEqualTo(ind2);
    assertThat(ind).isNotEqualTo(new Individual());
  }

  @Test
  public void testUser() {
    UUID id = UUID.randomUUID();
    User user = new User(id, "name", "username");
    assertThat(user.getId()).isEqualTo(id);
    assertThat(user.getName()).isEqualTo("name");
    assertThat(user.getUsername()).isEqualTo("username");

    user.setName("Name");
    user.setUsername("user123");
    assertThat(user.getName()).isEqualTo("Name");
    assertThat(user.getUsername()).isEqualTo("user123");

    User user2 = new User(id, "Name", "user123");
    assertThat(user).isEqualTo(user2);
    assertThat(user).isNotEqualTo(new User());
  }

  @Test
  public void testCharacterInstance() {
    UUID id = UUID.randomUUID();
    LarpCharacter character = new LarpCharacter(UUID.randomUUID(), null, "Hero");
    Individual player = new Individual(UUID.randomUUID(), "Alice");
    Game game = new Game(UUID.randomUUID(), null);

    CharacterInstance instance = new CharacterInstance(id, character, player, game, "Alice Mage");
    assertThat(instance.getId()).isEqualTo(id);
    assertThat(instance.getCharacter()).isEqualTo(character);
    assertThat(instance.getPlayer()).isEqualTo(player);
    assertThat(instance.getGame()).isEqualTo(game);
    assertThat(instance.getIndividualName()).isEqualTo("Alice Mage");

    LarpCharacter character2 = new LarpCharacter(UUID.randomUUID(), null, "Hero 2");
    Individual player2 = new Individual(UUID.randomUUID(), "Bob");
    Game game2 = new Game(UUID.randomUUID(), null);

    instance.setCharacter(character2);
    instance.setPlayer(player2);
    instance.setGame(game2);
    instance.setIndividualName("Bob Mage");

    assertThat(instance.getCharacter()).isEqualTo(character2);
    assertThat(instance.getPlayer()).isEqualTo(player2);
    assertThat(instance.getGame()).isEqualTo(game2);
    assertThat(instance.getIndividualName()).isEqualTo("Bob Mage");

    CharacterInstance instance2 = new CharacterInstance(id, character2, player2, game2, "Bob Mage");
    assertThat(instance).isEqualTo(instance2);
    assertThat(instance).isNotEqualTo(new CharacterInstance());
  }

  @Test
  public void testActor() {
    UUID id = UUID.randomUUID();
    Campaign campaign = new Campaign(UUID.randomUUID(), null);
    Actor actor = new Actor(id, campaign, "Actor Name", "pref_user");
    assertThat(actor.getId()).isEqualTo(id);
    assertThat(actor.getOwner()).isEqualTo(campaign);
    assertThat(actor.getSummary()).isEqualTo("Actor Name");
    assertThat(actor.getPreferredUsername()).isEqualTo("pref_user");

    Campaign campaign2 = new Campaign(UUID.randomUUID(), null);
    actor.setOwner(campaign2);
    actor.setSummary("Actor 2");
    actor.setPreferredUsername("user2");

    assertThat(actor.getOwner()).isEqualTo(campaign2);
    assertThat(actor.getSummary()).isEqualTo("Actor 2");
    assertThat(actor.getPreferredUsername()).isEqualTo("user2");

    Actor actor2 = new Actor(id, campaign2, "Actor 2", "user2");
    assertThat(actor).isEqualTo(actor2);
    assertThat(actor).isNotEqualTo(new Actor());
  }

  @Test
  public void testCollection() {
    UUID id = UUID.randomUUID();
    Campaign campaign = new Campaign(UUID.randomUUID(), null);
    Collection collection = new Collection(id, campaign, "Items");
    assertThat(collection.getId()).isEqualTo(id);
    assertThat(collection.getOwner()).isEqualTo(campaign);
    assertThat(collection.getName()).isEqualTo("Items");

    Campaign campaign2 = new Campaign(UUID.randomUUID(), null);
    collection.setOwner(campaign2);
    collection.setName("Quests");

    assertThat(collection.getOwner()).isEqualTo(campaign2);
    assertThat(collection.getName()).isEqualTo("Quests");

    Collection collection2 = new Collection(id, campaign2, "Quests");
    assertThat(collection).isEqualTo(collection2);
    assertThat(collection).isNotEqualTo(new Collection());
  }
}
