import os

base_dir = "data/src/main/java/com/larpconnect/njall/data"

system_java = """package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * System entity.
 */
@Entity
@Table(name = "systems")
public final class System extends NjallEntity {

    @Column(name = "name", nullable = false)
    private String name;

    public System() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
"""

campaign_java = """package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Campaign entity.
 */
@Entity
@Table(name = "campaigns")
public final class Campaign extends NjallEntity {

    @Column(name = "system_id")
    private UUID systemId;

    public Campaign() {
    }

    public UUID getSystemId() {
        return systemId;
    }

    public void setSystemId(UUID systemId) {
        this.systemId = systemId;
    }
}
"""

game_java = """package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Game entity.
 */
@Entity
@Table(name = "games")
public final class Game extends NjallEntity {

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    public Game() {
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(UUID campaignId) {
        this.campaignId = campaignId;
    }
}
"""

character_java = """package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Character entity.
 */
@Entity
@Table(name = "characters")
public final class Character extends NjallEntity {

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "name_template", nullable = false)
    private String nameTemplate;

    public Character() {
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(UUID campaignId) {
        this.campaignId = campaignId;
    }

    public String getNameTemplate() {
        return nameTemplate;
    }

    public void setNameTemplate(String nameTemplate) {
        this.nameTemplate = nameTemplate;
    }
}
"""

character_instance_java = """package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Character instance entity.
 */
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

    public CharacterInstance() {
    }

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
"""

def write_file(filename, content):
    with open(os.path.join(base_dir, filename), "w") as f:
        f.write(content)

write_file("System.java", system_java)
write_file("Campaign.java", campaign_java)
write_file("Game.java", game_java)
write_file("Character.java", character_java)
write_file("CharacterInstance.java", character_instance_java)

print("Created System, Campaign, Game, Character, and CharacterInstance.")
