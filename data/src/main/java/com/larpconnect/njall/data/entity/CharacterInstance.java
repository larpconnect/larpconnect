package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Represents an instance of a character played by an individual in a game. */
@jakarta.persistence.Entity
@Table(name = "character_instances")
public final class CharacterInstance extends Entity {

  @ManyToOne
  @JoinColumn(name = "character_id", nullable = false)
  private Character character;

  @ManyToOne
  @JoinColumn(name = "player_id", nullable = false)
  private Individual player;

  @ManyToOne
  @JoinColumn(name = "game_id")
  private Game game;

  @Column(name = "individual_name", nullable = false)
  private String individualName;

  CharacterInstance() {}

  public Character getCharacter() {
    return character;
  }

  public void setCharacter(Character character) {
    this.character = character;
  }

  public Individual getPlayer() {
    return player;
  }

  public void setPlayer(Individual player) {
    this.player = player;
  }

  public Game getGame() {
    return game;
  }

  public void setGame(Game game) {
    this.game = game;
  }

  public String getIndividualName() {
    return individualName;
  }

  public void setIndividualName(String individualName) {
    this.individualName = individualName;
  }
}
