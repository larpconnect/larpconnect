package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class CharacterInstanceTest {

  @Test
  void testGettersAndSetters() {
    CharacterInstance entity = new CharacterInstance();

    Character characterVal = Mockito.mock(Character.class);
    entity.setCharacter(characterVal);
    assertThat(entity.getCharacter()).isEqualTo(characterVal);
    Individual playerVal = Mockito.mock(Individual.class);
    entity.setPlayer(playerVal);
    assertThat(entity.getPlayer()).isEqualTo(playerVal);
    Game gameVal = Mockito.mock(Game.class);
    entity.setGame(gameVal);
    assertThat(entity.getGame()).isEqualTo(gameVal);
    String individualNameVal = "test";
    entity.setIndividualName(individualNameVal);
    assertThat(entity.getIndividualName()).isEqualTo(individualNameVal);
    UUID idVal = UUID.randomUUID();
    entity.setId(idVal);
    assertThat(entity.getId()).isEqualTo(idVal);
  }
}
