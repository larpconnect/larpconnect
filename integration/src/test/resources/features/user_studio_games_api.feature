@AdminApi
Feature: User Space Studio Games REST API
  As a studio member or client
  I want to manage games for a studio via user space endpoints
  So that scheduled game sessions linked to systems and optional campaigns operate within the tenant boundary under Row-Level Security

  Scenario: Successfully create game with independent system, campaign, location, and schedule
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_game_full",
        "name": "Valiant Games Full Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_game_full/v1/actors" with body:
      """
      {
        "preferredUsername": "game_lead"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "game_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_game_full/v1/locations" with body:
      """
      {
        "generator": "{game_actor_id}",
        "name": "Grand Manor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "game_loc_id"
    When an API admin sends a POST request to "/api/studios/valiant_game_full/v1/systems" with body:
      """
      {
        "generator": "{game_actor_id}",
        "name": "Vampire: The Masquerade"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "game_sys_a_id"
    When an API admin sends a POST request to "/api/studios/valiant_game_full/v1/systems" with body:
      """
      {
        "generator": "{game_actor_id}",
        "name": "World of Darkness"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "game_sys_b_id"
    When an API admin sends a POST request to "/api/studios/valiant_game_full/v1/campaigns" with body:
      """
      {
        "generator": "{game_actor_id}",
        "title": "Mixed Ruleset Chronicle",
        "system": "{game_sys_b_id}"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "game_camp_id"
    When an API admin sends a POST request to "/api/studios/valiant_game_full/v1/games" with body:
      """
      {
        "generator": "/api/studios/valiant_game_full/v1/actors/{game_actor_id}",
        "title": "Elysium Crossover",
        "summary": "Opening night crossover game",
        "system": "/api/studios/valiant_game_full/v1/systems/{game_sys_a_id}",
        "campaign": "/api/studios/valiant_game_full/v1/campaigns/{game_camp_id}",
        "locationId": "{game_loc_id}",
        "startTime": "2026-11-14T18:00:00Z",
        "endTime": "2026-11-14T23:00:00Z"
      }
      """
    Then the HTTP response status code is 201
    And the HTTP response content-type contains "application/json"
    And the JSON response contains non-null field "id"
    And the JSON response contains field "title" with value "Elysium Crossover"
    And the JSON response contains field "summary" with value "Opening night crossover game"
    And the JSON response contains field "eventType" with value "GAME"
    And the JSON response contains field "generator" with value "/api/studios/valiant_game_full/v1/actors/{game_actor_id}"
    And the JSON response contains field "system" with value "/api/studios/valiant_game_full/v1/systems/{game_sys_a_id}"
    And the JSON response contains field "campaign" with value "/api/studios/valiant_game_full/v1/campaigns/{game_camp_id}"
    And the JSON response contains field "locationId" with value "{game_loc_id}"
    And the JSON response contains field "startTime" with value "2026-11-14T18:00:00Z"
    And the JSON response contains field "endTime" with value "2026-11-14T23:00:00Z"
    And the JSON response contains non-null field "createdOn"
    And the JSON response contains non-null field "updatedOn"
    And the JSON response does not contain field "tenantId"

  Scenario: Successfully create standalone one-shot game without campaign using bare UUIDs
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_game_oneshot",
        "name": "Valiant One-Shot Game Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_game_oneshot/v1/actors" with body:
      """
      {
        "preferredUsername": "oneshot_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "oneshot_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_game_oneshot/v1/systems" with body:
      """
      {
        "generator": "{oneshot_actor_id}",
        "name": "Monsterhearts 2"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "oneshot_sys_id"
    When an API admin sends a POST request to "/api/studios/valiant_game_oneshot/v1/games" with body:
      """
      {
        "generator": "{oneshot_actor_id}",
        "title": "Midnight One-Shot",
        "system": "{oneshot_sys_id}"
      }
      """
    Then the HTTP response status code is 201
    And the JSON response contains field "title" with value "Midnight One-Shot"
    And the JSON response contains field "eventType" with value "GAME"
    And the JSON response contains field "generator" with value "/api/studios/valiant_game_oneshot/v1/actors/{oneshot_actor_id}"
    And the JSON response contains field "system" with value "/api/studios/valiant_game_oneshot/v1/systems/{oneshot_sys_id}"
    And the JSON response field "campaign" is null
    And the JSON response field "summary" is null
    And the JSON response field "locationId" is null
    And the JSON response field "startTime" is null
    And the JSON response field "endTime" is null

  Scenario: Creating game with missing, nonexistent, cross-studio, non-System, or soft-deleted system returns 400
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "game_bad_sys",
        "name": "Game Bad System Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "game_other_studio",
        "name": "Game Other Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/game_bad_sys/v1/actors" with body:
      """
      {
        "preferredUsername": "gbs_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "gbs_actor_id"
    When an API admin sends a POST request to "/api/studios/game_other_studio/v1/actors" with body:
      """
      {
        "preferredUsername": "g_other_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "g_other_actor_id"
    When an API admin sends a POST request to "/api/studios/game_other_studio/v1/systems" with body:
      """
      {
        "generator": "{g_other_actor_id}",
        "name": "Foreign Ruleset"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "g_other_sys_id"
    When an API admin sends a POST request to "/api/studios/game_bad_sys/v1/locations" with body:
      """
      {
        "generator": "{gbs_actor_id}",
        "name": "Hall Not A System"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "gbs_loc_id"
    When an API admin sends a POST request to "/api/studios/game_bad_sys/v1/systems" with body:
      """
      {
        "generator": "{gbs_actor_id}",
        "name": "Retired Game Ruleset"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "gbs_del_sys_id"
    When an API admin sends a DELETE request to "/api/studios/game_bad_sys/v1/systems/{gbs_del_sys_id}"
    Then the HTTP response status code is 204
    When an API admin sends a POST request to "/api/studios/game_bad_sys/v1/games" with body:
      """
      {
        "generator": "{gbs_actor_id}",
        "title": "Systemless Game"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message
    When an API admin sends a POST request to "/api/studios/game_bad_sys/v1/games" with body:
      """
      {
        "generator": "{gbs_actor_id}",
        "title": "Missing System Game",
        "system": "01925b6a-93be-7000-8000-000000000999"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message
    When an API admin sends a POST request to "/api/studios/game_bad_sys/v1/games" with body:
      """
      {
        "generator": "{gbs_actor_id}",
        "title": "Cross Studio System Game",
        "system": "{g_other_sys_id}"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message
    When an API admin sends a POST request to "/api/studios/game_bad_sys/v1/games" with body:
      """
      {
        "generator": "{gbs_actor_id}",
        "title": "Location As System Game",
        "system": "{gbs_loc_id}"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message
    When an API admin sends a POST request to "/api/studios/game_bad_sys/v1/games" with body:
      """
      {
        "generator": "{gbs_actor_id}",
        "title": "Deleted System Game",
        "system": "{gbs_del_sys_id}"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Creating game with invalid campaign, blank title, or invalid time order returns 400
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "game_bad_fields",
        "name": "Game Bad Fields Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/game_bad_fields/v1/actors" with body:
      """
      {
        "preferredUsername": "gbf_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "gbf_actor_id"
    When an API admin sends a POST request to "/api/studios/game_bad_fields/v1/systems" with body:
      """
      {
        "generator": "{gbf_actor_id}",
        "name": "Active Core System"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "gbf_sys_id"
    When an API admin sends a POST request to "/api/studios/game_bad_fields/v1/locations" with body:
      """
      {
        "generator": "{gbf_actor_id}",
        "name": "Tavern Not A Campaign"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "gbf_loc_id"
    When an API admin sends a POST request to "/api/studios/game_bad_fields/v1/campaigns" with body:
      """
      {
        "generator": "{gbf_actor_id}",
        "title": "Retired Chronicle"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "gbf_del_camp_id"
    When an API admin sends a DELETE request to "/api/studios/game_bad_fields/v1/campaigns/{gbf_del_camp_id}"
    Then the HTTP response status code is 204
    When an API admin sends a POST request to "/api/studios/game_bad_fields/v1/games" with body:
      """
      {
        "generator": "{gbf_actor_id}",
        "title": "Non Campaign Entity Game",
        "system": "{gbf_sys_id}",
        "campaign": "{gbf_loc_id}"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message
    When an API admin sends a POST request to "/api/studios/game_bad_fields/v1/games" with body:
      """
      {
        "generator": "{gbf_actor_id}",
        "title": "Soft Deleted Campaign Game",
        "system": "{gbf_sys_id}",
        "campaign": "{gbf_del_camp_id}"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message
    When an API admin sends a POST request to "/api/studios/game_bad_fields/v1/games" with body:
      """
      {
        "generator": "{gbf_actor_id}",
        "title": "   ",
        "system": "{gbf_sys_id}"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message
    When an API admin sends a POST request to "/api/studios/game_bad_fields/v1/games" with body:
      """
      {
        "generator": "{gbf_actor_id}",
        "title": "Broken Schedule Game",
        "system": "{gbf_sys_id}",
        "startTime": "2026-10-27T12:00:00Z",
        "endTime": "2026-10-25T18:00:00Z"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message
    When an API admin sends a POST request to "/api/studios/game_bad_fields/v1/games" with body:
      """
      {
        "generator": "{gbf_actor_id}",
        "title": "Missing Location Game",
        "system": "{gbf_sys_id}",
        "locationId": "01925b6a-93be-7000-8000-000000000999"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Creating game for nonexistent studio returns 404
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/studios/unknown/v1/games" with body:
      """
      {
        "generator": "018f2345-6789-7abc-def0-000000000000",
        "title": "Ghost Game",
        "system": "018f2345-6789-7abc-def0-000000000001"
      }
      """
    Then the HTTP response status code is 404

  Scenario: Successfully list active games with systemId and campaignId filters excluding non-game events
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_game_filter",
        "name": "Valiant Filter Games Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_game_filter/v1/actors" with body:
      """
      {
        "preferredUsername": "gf_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "gf_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_game_filter/v1/systems" with body:
      """
      {
        "generator": "{gf_actor_id}",
        "name": "Ruleset Alpha"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "gf_sys_a_id"
    When an API admin sends a POST request to "/api/studios/valiant_game_filter/v1/systems" with body:
      """
      {
        "generator": "{gf_actor_id}",
        "name": "Ruleset Beta"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "gf_sys_b_id"
    When an API admin sends a POST request to "/api/studios/valiant_game_filter/v1/campaigns" with body:
      """
      {
        "generator": "{gf_actor_id}",
        "title": "Arc One",
        "system": "{gf_sys_a_id}"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "gf_camp_1_id"
    When an API admin sends a POST request to "/api/studios/valiant_game_filter/v1/campaigns" with body:
      """
      {
        "generator": "{gf_actor_id}",
        "title": "Arc Two",
        "system": "{gf_sys_b_id}"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "gf_camp_2_id"
    When an API admin sends a POST request to "/api/studios/valiant_game_filter/v1/events" with body:
      """
      {
        "generator": "{gf_actor_id}",
        "title": "Non-Game Workshop",
        "eventType": "WORKSHOP",
        "startTime": "2026-11-01T10:00:00Z"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_game_filter/v1/games" with body:
      """
      {
        "generator": "{gf_actor_id}",
        "title": "Game A1",
        "system": "{gf_sys_a_id}",
        "campaign": "{gf_camp_1_id}",
        "startTime": "2026-11-02T10:00:00Z"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_game_filter/v1/games" with body:
      """
      {
        "generator": "{gf_actor_id}",
        "title": "Game A2",
        "system": "{gf_sys_a_id}",
        "campaign": "{gf_camp_2_id}",
        "startTime": "2026-11-03T10:00:00Z"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_game_filter/v1/games" with body:
      """
      {
        "generator": "{gf_actor_id}",
        "title": "Game B1",
        "system": "{gf_sys_b_id}",
        "campaign": "{gf_camp_1_id}",
        "startTime": "2026-11-04T10:00:00Z"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_game_filter/v1/games" with body:
      """
      {
        "generator": "{gf_actor_id}",
        "title": "Game Deleted",
        "system": "{gf_sys_a_id}",
        "campaign": "{gf_camp_1_id}"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "gf_del_game_id"
    When an API admin sends a DELETE request to "/api/studios/valiant_game_filter/v1/games/{gf_del_game_id}"
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_game_filter/v1/games"
    Then the HTTP response status code is 200
    And the HTTP response content-type contains "application/json"
    And the JSON response array has length 3
    And the JSON response array contains an item with "title" equal to "Game A1"
    And the JSON response array contains an item with "title" equal to "Game A2"
    And the JSON response array contains an item with "title" equal to "Game B1"
    And the JSON response array does not contain an item with "title" equal to "Non-Game Workshop"
    And the JSON response array does not contain an item with "title" equal to "Game Deleted"
    When an API admin sends a GET request to "/api/studios/valiant_game_filter/v1/games?systemId={gf_sys_a_id}"
    Then the HTTP response status code is 200
    And the JSON response array has length 2
    And the JSON response array contains an item with "title" equal to "Game A1"
    And the JSON response array contains an item with "title" equal to "Game A2"
    When an API admin sends a GET request to "/api/studios/valiant_game_filter/v1/games?campaignId=/api/studios/valiant_game_filter/v1/campaigns/{gf_camp_1_id}"
    Then the HTTP response status code is 200
    And the JSON response array has length 2
    And the JSON response array contains an item with "title" equal to "Game A1"
    And the JSON response array contains an item with "title" equal to "Game B1"
    When an API admin sends a GET request to "/api/studios/valiant_game_filter/v1/games?systemId={gf_sys_a_id}&campaignId={gf_camp_1_id}"
    Then the HTTP response status code is 200
    And the JSON response array has length 1
    And the JSON response path "0.title" has value "Game A1"

  Scenario: Listing games with malformed or cross-studio filters returns 400
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "game_filter_bad_a",
        "name": "Game Filter Bad A"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "game_filter_bad_b",
        "name": "Game Filter Bad B"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/game_filter_bad_b/v1/actors" with body:
      """
      {
        "preferredUsername": "gfb_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "gfb_actor_id"
    When an API admin sends a POST request to "/api/studios/game_filter_bad_b/v1/campaigns" with body:
      """
      {
        "generator": "{gfb_actor_id}",
        "title": "Foreign Chronicle"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "gfb_camp_id"
    When an API admin sends a GET request to "/api/studios/game_filter_bad_a/v1/games?systemId=not-a-uuid"
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message
    When an API admin sends a GET request to "/api/studios/game_filter_bad_a/v1/games?campaignId=/api/studios/game_filter_bad_b/v1/campaigns/{gfb_camp_id}"
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Soft-deleting campaign masks campaign as null while soft-deleting system retains historical game system URI
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "game_mask_refs",
        "name": "Game Mask Soft Deleted Refs Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/game_mask_refs/v1/actors" with body:
      """
      {
        "preferredUsername": "gmr_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "gmr_actor_id"
    When an API admin sends a POST request to "/api/studios/game_mask_refs/v1/systems" with body:
      """
      {
        "generator": "{gmr_actor_id}",
        "name": "Ephemeral Ruleset"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "gmr_sys_id"
    When an API admin sends a POST request to "/api/studios/game_mask_refs/v1/campaigns" with body:
      """
      {
        "generator": "{gmr_actor_id}",
        "title": "Ephemeral Chronicle"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "gmr_camp_id"
    When an API admin sends a POST request to "/api/studios/game_mask_refs/v1/games" with body:
      """
      {
        "generator": "{gmr_actor_id}",
        "title": "Resilient Session",
        "system": "{gmr_sys_id}",
        "campaign": "{gmr_camp_id}"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "gmr_game_id"
    When an API admin sends a DELETE request to "/api/studios/game_mask_refs/v1/campaigns/{gmr_camp_id}"
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/game_mask_refs/v1/games/{gmr_game_id}"
    Then the HTTP response status code is 200
    And the JSON response contains field "system" with value "/api/studios/game_mask_refs/v1/systems/{gmr_sys_id}"
    And the JSON response field "campaign" is null
    When an API admin sends a DELETE request to "/api/studios/game_mask_refs/v1/systems/{gmr_sys_id}"
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/game_mask_refs/v1/games/{gmr_game_id}"
    Then the HTTP response status code is 200
    And the JSON response contains field "title" with value "Resilient Session"
    And the JSON response contains field "system" with value "/api/studios/game_mask_refs/v1/systems/{gmr_sys_id}"
    And the JSON response field "campaign" is null
    When an API admin sends a GET request to "/api/studios/game_mask_refs/v1/games"
    Then the HTTP response status code is 200
    And the JSON response array has length 1
    And the JSON response path "0.id" has value "{gmr_game_id}"
    And the JSON response path "0.system" has value "/api/studios/game_mask_refs/v1/systems/{gmr_sys_id}"
    And the JSON response path "0.campaign" has value "null"
    When an API admin sends a GET request to "/api/studios/game_mask_refs/v1/games?campaignId={gmr_camp_id}"
    Then the HTTP response status code is 200
    And the JSON response array has length 0

  Scenario: Retrieving, updating, or deleting a non-game event UUID and cross-tenant or deleted games returns 404
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "game_ct_alpha",
        "name": "Game CT Alpha"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "game_ct_beta",
        "name": "Game CT Beta"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/game_ct_alpha/v1/actors" with body:
      """
      {
        "preferredUsername": "game_ct_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "game_ct_actor_id"
    When an API admin sends a POST request to "/api/studios/game_ct_alpha/v1/events" with body:
      """
      {
        "generator": "{game_ct_actor_id}",
        "title": "Community Town Hall",
        "eventType": "MEETUP"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "ct_non_game_event_id"
    When an API admin sends a GET request to "/api/studios/game_ct_alpha/v1/games/{ct_non_game_event_id}"
    Then the HTTP response status code is 404
    When an API admin sends a PATCH request to "/api/studios/game_ct_alpha/v1/games/{ct_non_game_event_id}?update_mask=title" with body:
      """
      {
        "title": "Should Not Patch Non-Game"
      }
      """
    Then the HTTP response status code is 404
    When an API admin sends a DELETE request to "/api/studios/game_ct_alpha/v1/games/{ct_non_game_event_id}"
    Then the HTTP response status code is 404
    When an API admin sends a POST request to "/api/studios/game_ct_alpha/v1/systems" with body:
      """
      {
        "generator": "{game_ct_actor_id}",
        "name": "CT Game System"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "ct_game_sys_id"
    When an API admin sends a POST request to "/api/studios/game_ct_alpha/v1/games" with body:
      """
      {
        "generator": "{game_ct_actor_id}",
        "title": "Alpha Secret Session",
        "system": "{ct_game_sys_id}"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "ct_game_id"
    When an API admin sends a GET request to "/api/studios/game_ct_alpha/v1/games/{ct_game_id}"
    Then the HTTP response status code is 200
    And the JSON response contains field "id" with value "{ct_game_id}"
    And the JSON response contains field "title" with value "Alpha Secret Session"
    And the JSON response contains field "eventType" with value "GAME"
    And the JSON response does not contain field "tenantId"
    When an API admin sends a GET request to "/api/studios/game_ct_beta/v1/games/{ct_game_id}"
    Then the HTTP response status code is 404
    When an API admin sends a DELETE request to "/api/studios/game_ct_alpha/v1/games/{ct_game_id}"
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/game_ct_alpha/v1/games/{ct_game_id}"
    Then the HTTP response status code is 404

  Scenario: Successfully update game fields, reassign system, and clear campaign adhering to AIP-134
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_game_patch",
        "name": "Valiant Patch Game Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_game_patch/v1/actors" with body:
      """
      {
        "preferredUsername": "pg_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "pg_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_game_patch/v1/systems" with body:
      """
      {
        "generator": "{pg_actor_id}",
        "name": "Game Ruleset One"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "pg_sys_1_id"
    When an API admin sends a POST request to "/api/studios/valiant_game_patch/v1/systems" with body:
      """
      {
        "generator": "{pg_actor_id}",
        "name": "Game Ruleset Two"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "pg_sys_2_id"
    When an API admin sends a POST request to "/api/studios/valiant_game_patch/v1/campaigns" with body:
      """
      {
        "generator": "{pg_actor_id}",
        "title": "Season Chronicle",
        "system": "{pg_sys_1_id}"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "pg_camp_id"
    When an API admin sends a POST request to "/api/studios/valiant_game_patch/v1/games" with body:
      """
      {
        "generator": "{pg_actor_id}",
        "title": "Night One",
        "summary": "Original session summary",
        "system": "{pg_sys_1_id}",
        "campaign": "{pg_camp_id}",
        "startTime": "2026-11-20T18:00:00Z",
        "endTime": "2026-11-20T22:00:00Z"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "pg_game_id"
    When an API admin sends a PATCH request to "/api/studios/valiant_game_patch/v1/games/{pg_game_id}?update_mask=title,system,start_time,end_time" with body:
      """
      {
        "title": "Night Two",
        "summary": "Unmasked summary should remain ignored",
        "system": "{pg_sys_2_id}",
        "startTime": "2026-11-21T19:00:00Z",
        "endTime": "2026-11-21T23:00:00Z"
      }
      """
    Then the HTTP response status code is 200
    And the JSON response contains field "title" with value "Night Two"
    And the JSON response contains field "summary" with value "Original session summary"
    And the JSON response contains field "eventType" with value "GAME"
    And the JSON response contains field "system" with value "/api/studios/valiant_game_patch/v1/systems/{pg_sys_2_id}"
    And the JSON response contains field "campaign" with value "/api/studios/valiant_game_patch/v1/campaigns/{pg_camp_id}"
    And the JSON response contains field "startTime" with value "2026-11-21T19:00:00Z"
    And the JSON response contains field "endTime" with value "2026-11-21T23:00:00Z"
    When an API admin sends a PATCH request to "/api/studios/valiant_game_patch/v1/games/{pg_game_id}?update_mask=campaign" with body:
      """
      {
        "campaign": null
      }
      """
    Then the HTTP response status code is 200
    And the JSON response contains field "title" with value "Night Two"
    And the JSON response contains field "system" with value "/api/studios/valiant_game_patch/v1/systems/{pg_sys_2_id}"
    And the JSON response field "campaign" is null

  Scenario: Patching game to clear required system, assign soft-deleted system, or set invalid time order returns 400
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_game_bad_patch",
        "name": "Valiant Bad Patch Game Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_game_bad_patch/v1/actors" with body:
      """
      {
        "preferredUsername": "bgp_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "bgp_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_game_bad_patch/v1/systems" with body:
      """
      {
        "generator": "{bgp_actor_id}",
        "name": "Non-Nullable System"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "bgp_sys_id"
    When an API admin sends a POST request to "/api/studios/valiant_game_bad_patch/v1/systems" with body:
      """
      {
        "generator": "{bgp_actor_id}",
        "name": "Deleted Target System"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "bgp_del_sys_id"
    When an API admin sends a DELETE request to "/api/studios/valiant_game_bad_patch/v1/systems/{bgp_del_sys_id}"
    Then the HTTP response status code is 204
    When an API admin sends a POST request to "/api/studios/valiant_game_bad_patch/v1/games" with body:
      """
      {
        "generator": "{bgp_actor_id}",
        "title": "Anchored Session",
        "system": "{bgp_sys_id}"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "bgp_game_id"
    When an API admin sends a PATCH request to "/api/studios/valiant_game_bad_patch/v1/games/{bgp_game_id}?update_mask=system" with body:
      """
      {
        "system": null
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message
    When an API admin sends a PATCH request to "/api/studios/valiant_game_bad_patch/v1/games/{bgp_game_id}?update_mask=system" with body:
      """
      {
        "system": "{bgp_del_sys_id}"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message
    When an API admin sends a PATCH request to "/api/studios/valiant_game_bad_patch/v1/games/{bgp_game_id}?update_mask=start_time,end_time" with body:
      """
      {
        "startTime": "2026-11-22T20:00:00Z",
        "endTime": "2026-11-22T18:00:00Z"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Successfully soft delete game and confirm database deleted_on
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_game_soft_del",
        "name": "Valiant Soft Delete Game Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_game_soft_del/v1/actors" with body:
      """
      {
        "preferredUsername": "soft_del_game_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "soft_del_game_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_game_soft_del/v1/systems" with body:
      """
      {
        "generator": "{soft_del_game_actor_id}",
        "name": "Delete Test System"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "soft_del_game_sys_id"
    When an API admin sends a POST request to "/api/studios/valiant_game_soft_del/v1/games" with body:
      """
      {
        "generator": "{soft_del_game_actor_id}",
        "title": "Game To Soft Delete",
        "system": "{soft_del_game_sys_id}"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "soft_del_game_id"
    When an API admin sends a DELETE request to "/api/studios/valiant_game_soft_del/v1/games/{soft_del_game_id}"
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_game_soft_del/v1/games/{soft_del_game_id}"
    Then the HTTP response status code is 404
    And the entity for remembered game "soft_del_game_id" has deleted_on populated in database
    When an API admin sends a DELETE request to "/api/studios/valiant_game_soft_del/v1/games/{soft_del_game_id}"
    Then the HTTP response status code is 404
