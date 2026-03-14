package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** Character instance entity. */
@Entity
@Table(name = "character_instances")
public final class CharacterInstance extends NjallEntity {

  @Column(name = "character_id", nullable = false)
  private UUID characterId;

  @Column(name = "player_id", nullable = false)
  private UUID playerId;

  @Column(name = "game_id")
  private UUID gameId;

  @Column(name = "individual_name", nullable = false)
  private String individualName;

  public CharacterInstance() {}

  public UUID getCharacterId() {
    return characterId;
  }

  public void setCharacterId(UUID characterId) {
    this.characterId = characterId;
  }

  public UUID getPlayerId() {
    return playerId;
  }

  public void setPlayerId(UUID playerId) {
    this.playerId = playerId;
  }

  public UUID getGameId() {
    return gameId;
  }

  public void setGameId(UUID gameId) {
    this.gameId = gameId;
  }

  public String getIndividualName() {
    return individualName;
  }

  public void setIndividualName(String individualName) {
    this.individualName = individualName;
  }
}
