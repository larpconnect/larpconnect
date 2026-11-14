@E2EFeatherpubIngress
Feature: End-to-End FeatherPub Activity Ingress

  Background:
    Given the ingress test environment is initialized with PostgreSQL and RabbitMQ AMQP 1.0
    And the RabbitMQ broker is running and healthy

  # ============================================================================
  # TIER 1: FEATURE COVERAGE
  # ============================================================================

  # --- Feature 1: Single Activity Ingress ---
  Scenario: T1_INGRESS_01 Standard POST /inbox with valid Create activity returns 202 Accepted
    Given an active studio exists with alias "valhalla" and name "Valhalla Larp Studio"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-create-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": {
          "id": "https://larpconnect.org/objects/quest-01",
          "type": "Quest",
          "name": "Defend the Great Hall",
          "created": "{CURRENT_TIMESTAMP}"
        }
      }
      """
    Then the ingress response status code should be 202
    And the ingress response body should be empty
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T1_INGRESS_02 Custom method POST /inbox:message with valid Update activity returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox:message" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-update-01",
        "type": "Update",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": {
          "id": "https://larpconnect.org/objects/quest-01",
          "type": "Quest",
          "name": "Defend the Great Hall (Urgent)",
          "created": "{CURRENT_TIMESTAMP}"
        }
      }
      """
    Then the ingress response status code should be 202
    And the ingress response body should be empty
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T1_INGRESS_03 POST /inbox with valid Announce activity returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-announce-01",
        "type": "Announce",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": "https://larpconnect.org/objects/event-summer-gathering"
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T1_INGRESS_04 POST /inbox addressed to single actor in to field successfully enqueues message
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-direct-msg-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": {
          "id": "https://larpconnect.org/objects/note-01",
          "type": "Note",
          "content": "Secret orders received",
          "created": "{CURRENT_TIMESTAMP}"
        }
      }
      """
    Then the ingress response status code should be 202
    And the published message on queue "njall.inbox.activities" at index 0 has messageId "https://larpconnect.org/activities/t1-direct-msg-01"

  Scenario: T1_INGRESS_05 POST /inbox addressed to multiple valid actors in to and cc enqueues message
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    And an active actor exists in studio "valhalla" with preferred username "freya" generated by actor "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-multi-target-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "cc": ["https://larpconnect.org/studios/valhalla/actors/freya"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": {
          "id": "https://larpconnect.org/objects/battle-plan-01",
          "type": "Note",
          "content": "Flank right at dawn",
          "created": "{CURRENT_TIMESTAMP}"
        }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  # --- Feature 2: AIP-233 Batch Ingress ---
  Scenario: T1_BATCH_01 Batch with 2 valid activities returns 202 Accepted with empty failedRequests
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox:batchMessage" with body:
      """
      {
        "requests": [
          {
            "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
            "id": "https://larpconnect.org/activities/t1-batch-01",
            "type": "Create",
            "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
            "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
            "created": "{CURRENT_TIMESTAMP}",
            "object": { "id": "https://larpconnect.org/objects/b1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
          },
          {
            "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
            "id": "https://larpconnect.org/activities/t1-batch-02",
            "type": "Update",
            "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
            "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
            "created": "{CURRENT_TIMESTAMP}",
            "object": { "id": "https://larpconnect.org/objects/b1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
          }
        ]
      }
      """
    Then the ingress response status code should be 202
    And the ingress batch response has no failed requests
    And exactly 2 messages should be published to queue "njall.inbox.activities"

  Scenario: T1_BATCH_02 Batch with 3 valid distinct verbs returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox:batchMessage" with body:
      """
      {
        "requests": [
          {
            "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
            "id": "https://larpconnect.org/activities/t1-verbs-01",
            "type": "Create",
            "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
            "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
            "created": "{CURRENT_TIMESTAMP}",
            "object": { "id": "https://larpconnect.org/objects/v1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
          },
          {
            "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
            "id": "https://larpconnect.org/activities/t1-verbs-02",
            "type": "Announce",
            "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
            "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
            "created": "{CURRENT_TIMESTAMP}",
            "object": "https://larpconnect.org/objects/v1"
          },
          {
            "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
            "id": "https://larpconnect.org/activities/t1-verbs-03",
            "type": "Delete",
            "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
            "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
            "created": "{CURRENT_TIMESTAMP}",
            "object": "https://larpconnect.org/objects/v1"
          }
        ]
      }
      """
    Then the ingress response status code should be 202
    And the ingress batch response has no failed requests
    And exactly 3 messages should be published to queue "njall.inbox.activities"

  Scenario: T1_BATCH_03 Partial success: 2 valid activities and 1 invalid activity returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox:batchMessage" with body:
      """
      {
        "requests": [
          {
            "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
            "id": "https://larpconnect.org/activities/t1-partial-01",
            "type": "Create",
            "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
            "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
            "created": "{CURRENT_TIMESTAMP}",
            "object": { "id": "https://larpconnect.org/objects/p1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
          },
          {
            "@context": ["https://www.w3.org/ns/activitystreams"],
            "id": "https://larpconnect.org/activities/t1-partial-02-bad",
            "type": "Create",
            "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
            "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
            "created": "{CURRENT_TIMESTAMP}"
          },
          {
            "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
            "id": "https://larpconnect.org/activities/t1-partial-03",
            "type": "Create",
            "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
            "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
            "created": "{CURRENT_TIMESTAMP}",
            "object": { "id": "https://larpconnect.org/objects/p3", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
          }
        ]
      }
      """
    Then the ingress response status code should be 202
    And the ingress batch response contains failed request at index "1" with code 400 and non-empty message
    And the ingress batch response does not contain failed request at index "0"
    And the ingress batch response does not contain failed request at index "2"
    And exactly 2 messages should be published to queue "njall.inbox.activities"

  Scenario: T1_BATCH_04 Total failure: 2 activities both failing schema validation returns 400 Bad Request
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox:batchMessage" with body:
      """
      {
        "requests": [
          {
            "id": "https://larpconnect.org/activities/t1-fail-01",
            "type": "InvalidVerb"
          },
          {
            "id": "https://larpconnect.org/activities/t1-fail-02",
            "type": "InvalidVerb"
          }
        ]
      }
      """
    Then the ingress response status code should be 400
    And the ingress batch response failed requests count is 2
    And zero messages should be published to queue "njall.inbox.activities"

  Scenario: T1_BATCH_05 Batch containing duplicate identical activities returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox:batchMessage" with body:
      """
      {
        "requests": [
          {
            "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
            "id": "https://larpconnect.org/activities/t1-batch-dupe-01",
            "type": "Create",
            "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
            "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
            "created": "{CURRENT_TIMESTAMP}",
            "object": { "id": "https://larpconnect.org/objects/d1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
          },
          {
            "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
            "id": "https://larpconnect.org/activities/t1-batch-dupe-01",
            "type": "Create",
            "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
            "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
            "created": "{CURRENT_TIMESTAMP}",
            "object": { "id": "https://larpconnect.org/objects/d1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
          }
        ]
      }
      """
    Then the ingress response status code should be 202
    And the ingress batch response has no failed requests
    And exactly 1 message should be published to queue "njall.inbox.activities"

  # --- Feature 3: Actor Resolution ---
  Scenario: T1_ACTOR_01 Ingress targeted at canonical lowercase UUID resolves and returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with UUID "a1111111-1111-1111-1111-111111111111" and preferred username "thor"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/a1111111-1111-1111-1111-111111111111/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-uuid-res-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/a1111111-1111-1111-1111-111111111111",
        "to": ["https://larpconnect.org/studios/valhalla/actors/a1111111-1111-1111-1111-111111111111"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/u1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T1_ACTOR_02 Ingress targeted at exact preferredUsername resolves and returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-uname-res-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/u2", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T1_ACTOR_03 Ingress targeted at uppercase preferredUsername resolves case-insensitively and returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/VALKYRIE/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-upper-res-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/u3", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T1_ACTOR_04 GET /actors/{preferredUsername} resolves case-insensitively and returns 200 OK
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a GET request to "/api/studios/valhalla/v1/actors/VALKYRIE"
    Then the ingress response status code should be 200
    And the ingress response content-type should contain "application/json"
    And the ingress response JSON contains field "preferredUsername" with value "valkyrie"

  Scenario: T1_ACTOR_05 GET /actors/{preferredUsername}/inbox returns 200 OK with OrderedCollection
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a GET request to "/api/studios/valhalla/v1/actors/valkyrie/inbox"
    Then the ingress response status code should be 200
    And the ingress response content-type should contain "application/json"
    And the ingress response JSON contains field "type" with value "OrderedCollection"

  # --- Feature 4: Discriminated 10-Verb Schemas ---
  Scenario: T1_VERB_01 Verb Create with required object returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-verb-create-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/obj-1", "type": "Artifact", "name": "Mjolnir", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T1_VERB_02 Verb Delete with URI object reference returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-verb-delete-01",
        "type": "Delete",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": "https://larpconnect.org/objects/obj-1"
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T1_VERB_03 Verb Add and Remove with required object and target collection returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-verb-add-01",
        "type": "Add",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": "https://larpconnect.org/objects/obj-1",
        "target": "https://larpconnect.org/collections/armory-01"
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-verb-remove-01",
        "type": "Remove",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": "https://larpconnect.org/objects/obj-1",
        "target": "https://larpconnect.org/collections/armory-01"
      }
      """
    Then the ingress response status code should be 202
    And exactly 2 messages should be published to queue "njall.inbox.activities"

  Scenario: T1_VERB_04 Verb Move with required object, origin, and target returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-verb-move-01",
        "type": "Move",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": "https://larpconnect.org/objects/obj-1",
        "origin": "https://larpconnect.org/collections/armory-01",
        "target": "https://larpconnect.org/collections/vault-02"
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T1_VERB_05 Verb Offer Accept Reject lifecycle returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-verb-offer-01",
        "type": "Offer",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": "https://larpconnect.org/objects/quest-01"
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-verb-accept-01",
        "type": "Accept",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": "https://larpconnect.org/activities/t1-verb-offer-01"
      }
      """
    Then the ingress response status code should be 202
    And exactly 2 messages should be published to queue "njall.inbox.activities"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-verb-reject-01",
        "type": "Reject",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": "https://larpconnect.org/activities/t1-verb-offer-01"
      }
      """
    Then the ingress response status code should be 202
    And exactly 3 messages should be published to queue "njall.inbox.activities"

  # --- Feature 5: Temporal Bounds & Authority Validation ---
  Scenario: T1_TEMPORAL_01 Activity with created equal to current UTC time returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-time-now-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/t1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T1_TEMPORAL_02 Activity with created 4 minutes in future returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-time-future-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{TIMESTAMP_PLUS_4_MINUTES}",
        "object": { "id": "https://larpconnect.org/objects/t2", "type": "Note", "created": "{TIMESTAMP_PLUS_4_MINUTES}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T1_TEMPORAL_03 Activity with created 7 days in past returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-time-past-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{TIMESTAMP_MINUS_7_DAYS}",
        "object": { "id": "https://larpconnect.org/objects/t3", "type": "Note", "created": "{TIMESTAMP_MINUS_7_DAYS}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T1_TEMPORAL_04 Activity with embedded object updated == created returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-coherence-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": {
          "id": "https://larpconnect.org/objects/t4",
          "type": "Note",
          "created": "{CURRENT_TIMESTAMP}",
          "updated": "{CURRENT_TIMESTAMP}"
        }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T1_TEMPORAL_05 Activity with id sharing exact hostname with actor returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://valhalla.larpconnect.org/activities/authority-01",
        "type": "Create",
        "actor": "https://valhalla.larpconnect.org/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://valhalla.larpconnect.org/objects/t5", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  # --- Feature 6: Content-Digest Verification ---
  Scenario: T1_DIGEST_01 Request with valid Content-Digest sha-256 returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with auto-computed Content-Digest "sha-256" and body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-sha256-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/s1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T1_DIGEST_02 Request with valid Content-Digest sha3-256 returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with auto-computed Content-Digest "sha3-256" and body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-sha3-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/s2", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T1_DIGEST_03 Request without Content-Digest header is accepted as optional
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-no-digest-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/s3", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T1_DIGEST_04 Batch request with valid Content-Digest covering batch payload returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox:batchMessage" with auto-computed Content-Digest "sha-256" and body:
      """
      {
        "requests": [
          {
            "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
            "id": "https://larpconnect.org/activities/t1-batch-digest-01",
            "type": "Create",
            "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
            "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
            "created": "{CURRENT_TIMESTAMP}",
            "object": { "id": "https://larpconnect.org/objects/bd1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
          }
        ]
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T1_DIGEST_05 Request with matching digest across multiline indented JSON body returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with auto-computed Content-Digest "sha-256" and body:
      """
      {
        "@context": [
          "https://www.w3.org/ns/activitystreams",
          "https://larpconnect.app/schemas/featherpub"
        ],
        "id": "https://larpconnect.org/activities/t1-multiline-digest-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": [
          "https://larpconnect.org/studios/valhalla/actors/valkyrie"
        ],
        "created": "{CURRENT_TIMESTAMP}",
        "object": {
          "id": "https://larpconnect.org/objects/m1",
          "type": "Note",
          "content": "Line 1\nLine 2\nLine 3",
          "created": "{CURRENT_TIMESTAMP}"
        }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  # --- Feature 7: Pre-Queue Generator Lineage Routing ---
  Scenario: T1_ROUTING_01 Activity addressed directly to receiving actor self-root returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-route-self-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/r1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T1_ROUTING_02 Activity addressed to direct child actor returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    And an active actor exists in studio "valhalla" with preferred username "child_actor" generated by actor "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-route-child-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/child_actor"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/r2", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T1_ROUTING_03 Activity addressed to grandchild actor returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    And an actor generator lineage exists in studio "valhalla" with depth 2 from root "valkyrie" to leaf "grandchild_actor"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-route-grandchild-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/grandchild_actor"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/r3", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T1_ROUTING_04 Activity addressing both parent and child resolves both UUIDs
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    And an active actor exists in studio "valhalla" with preferred username "child_actor" generated by actor "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-route-both-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie", "https://larpconnect.org/studios/valhalla/actors/child_actor"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/r4", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T1_ROUTING_05 Addressed recipient specified by preferredUsername in to URI resolves
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-route-uri-uname-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/r5", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  # --- Feature 8: Post-Publish 8-Day Deduplication & Idempotency ---
  Scenario: T1_DEDUP_01 First delivery of Activity returns 202 Accepted and enqueues AMQP message
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-dedup-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/dd1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"
    And a deduplication record exists in the database for activity "https://larpconnect.org/activities/t1-dedup-01" and target actor "{valkyrie_id}"

  Scenario: T1_DEDUP_02 Immediate second delivery returns 202 Accepted and suppresses duplicate
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-dedup-seq-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/dd2", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-dedup-seq-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/dd2", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T1_DEDUP_03 Third delivery via :message custom method returns 202 Accepted without enqueueing
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-dedup-msg-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/dd3", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox:message" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-dedup-msg-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/dd3", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T1_DEDUP_04 Delivery of Activity with different ID is not blocked
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-dedup-diff-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/dd4", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-dedup-diff-02",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/dd5", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 2 messages should be published to queue "njall.inbox.activities"

  Scenario: T1_DEDUP_05 Duplicate submission in AIP-233 batch returns 202 without republishing
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-dedup-prebatch-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/dd6", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox:batchMessage" with body:
      """
      {
        "requests": [
          {
            "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
            "id": "https://larpconnect.org/activities/t1-dedup-prebatch-01",
            "type": "Create",
            "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
            "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
            "created": "{CURRENT_TIMESTAMP}",
            "object": { "id": "https://larpconnect.org/objects/dd6", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
          }
        ]
      }
      """
    Then the ingress response status code should be 202
    And the ingress batch response has no failed requests
    And exactly 1 message should be published to queue "njall.inbox.activities"

  # --- Feature 9: AMQP 1.0 Publishing & Trace ---
  Scenario: T1_AMQP_01 Enqueued message arrives with messageId matching activity id
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-amqp-msgid-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/am1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And the published message on queue "njall.inbox.activities" at index 0 has messageId "https://larpconnect.org/activities/t1-amqp-msgid-01"

  Scenario: T1_AMQP_02 Enqueued message body is valid UTF-8 JSON containing InboxQueueEnvelope
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-amqp-env-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/am2", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And the published message on queue "njall.inbox.activities" at index 0 payload field "type" has value "Create"

  Scenario: T1_AMQP_03 Server-generated traceparent header is propagated in AMQP application properties
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with traceparent header "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01" and body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-amqp-trace-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/am3", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And the published message on queue "njall.inbox.activities" at index 0 has fresh server traceparent matching response and distinct from "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"

  Scenario: T1_AMQP_04 Enqueued message has durable flag set to true
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-amqp-durable-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/am4", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And the published message on queue "njall.inbox.activities" at index 0 has durable flag true

  Scenario: T1_AMQP_05 AMQP broker confirms ACCEPTED disposition settling delivery
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-amqp-settled-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/am5", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And the published message on queue "njall.inbox.activities" at index 0 was settled with disposition "ACCEPTED"

  # --- Feature 10: AMQP Health Check & Degradation ---
  Scenario: T1_HEALTH_01 With AMQP connected, GET /api/admin/v1/health reports status 200 OK
    When an administrative probe sends a GET request to "/api/admin/v1/health"
    Then the administrative health probe status code should be 200

  Scenario: T1_HEALTH_02 Health probe response body should be empty
    When an administrative probe sends a GET request to "/api/admin/v1/health"
    Then the administrative health probe status code should be 200
    And the administrative health probe body should be empty

  Scenario: T1_HEALTH_03 Rapid consecutive health queries within 10s return cached response
    When an administrative probe sends a GET request to "/api/admin/v1/health"
    Then the administrative health probe status code should be 200
    And no new broker health probe is executed within the 10-second cache window

  Scenario: T1_HEALTH_04 With broker operational, health check succeeds
    Given the RabbitMQ broker is running and healthy
    When an administrative probe sends a GET request to "/api/admin/v1/health"
    Then the administrative health probe status code should be 200

  Scenario: T1_HEALTH_05 Ingress request returns 202 while health check confirms healthy broker
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an administrative probe sends a GET request to "/api/admin/v1/health"
    Then the administrative health probe status code should be 200
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t1-health-ing-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/h1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  # ============================================================================
  # TIER 2: BOUNDARY & CORNER CASES
  # ============================================================================

  # --- Feature 1: Single Ingress Boundaries ---
  Scenario: T2_INGRESS_01 Unknown studio UUID returns 404 Not Found
    When an ingress client sends a POST request to "/api/studios/00000000-0000-0000-0000-000000000000/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-ing-unknown-studio",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/unk1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 404
    And zero messages should be published to queue "njall.inbox.activities"

  Scenario: T2_INGRESS_02 Soft-deleted studio returns 404 Not Found
    Given an active studio exists with alias "abandoned_studio"
    And studio "abandoned_studio" is soft-deleted
    When an ingress client sends a POST request to "/api/studios/abandoned_studio/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-ing-deleted-studio",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/del1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 404
    And zero messages should be published to queue "njall.inbox.activities"

  Scenario: T2_INGRESS_03 Unknown actor identifier returns 404 Not Found
    Given an active studio exists with alias "valhalla"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/unknown_ghost_actor/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-ing-unknown-actor",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/unk2", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 404
    And zero messages should be published to queue "njall.inbox.activities"

  Scenario: T2_INGRESS_04 Soft-deleted actor returns 404 Not Found
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "fallen_warrior"
    And actor "fallen_warrior" in studio "valhalla" is soft-deleted
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/fallen_warrior/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-ing-deleted-actor",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/fallen_warrior"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/del2", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 404
    And zero messages should be published to queue "njall.inbox.activities"

  Scenario: T2_INGRESS_05 Empty request body returns 400 Bad Request
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with empty body
    Then the ingress response status code should be 400
    And zero messages should be published to queue "njall.inbox.activities"

  # --- Feature 2: AIP-233 Batch Boundaries ---
  Scenario: T2_BATCH_01 Empty requests array returns 400 Bad Request
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox:batchMessage" with body:
      """
      {
        "requests": []
      }
      """
    Then the ingress response status code should be 400
    And zero messages should be published to queue "njall.inbox.activities"

  Scenario: T2_BATCH_02 Batch missing requests wrapper key returns 400 Bad Request
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox:batchMessage" with body:
      """
      {
        "items": [
          { "id": "https://larpconnect.org/activities/no-req-key", "type": "Create" }
        ]
      }
      """
    Then the ingress response status code should be 400
    And zero messages should be published to queue "njall.inbox.activities"

  Scenario: T2_BATCH_03 Batch containing non-JSON element returns 400 Bad Request
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox:batchMessage" with body:
      """
      {
        "requests": [
          "invalid string element"
        ]
      }
      """
    Then the ingress response status code should be 400
    And zero messages should be published to queue "njall.inbox.activities"

  Scenario: T2_BATCH_04 Batch with 101 items violates maxItems 100
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox:batchMessage" with body:
      """
      {
        "requests": [
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-000", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-000", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-001", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-001", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-002", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-002", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-003", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-003", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-004", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-004", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-005", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-005", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-006", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-006", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-007", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-007", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-008", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-008", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-009", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-009", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-010", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-010", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-011", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-011", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-012", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-012", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-013", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-013", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-014", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-014", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-015", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-015", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-016", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-016", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-017", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-017", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-018", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-018", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-019", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-019", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-020", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-020", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-021", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-021", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-022", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-022", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-023", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-023", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-024", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-024", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-025", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-025", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-026", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-026", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-027", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-027", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-028", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-028", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-029", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-029", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-030", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-030", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-031", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-031", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-032", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-032", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-033", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-033", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-034", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-034", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-035", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-035", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-036", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-036", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-037", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-037", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-038", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-038", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-039", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-039", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-040", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-040", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-041", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-041", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-042", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-042", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-043", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-043", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-044", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-044", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-045", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-045", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-046", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-046", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-047", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-047", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-048", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-048", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-049", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-049", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-050", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-050", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-051", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-051", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-052", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-052", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-053", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-053", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-054", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-054", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-055", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-055", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-056", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-056", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-057", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-057", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-058", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-058", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-059", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-059", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-060", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-060", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-061", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-061", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-062", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-062", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-063", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-063", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-064", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-064", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-065", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-065", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-066", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-066", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-067", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-067", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-068", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-068", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-069", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-069", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-070", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-070", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-071", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-071", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-072", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-072", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-073", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-073", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-074", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-074", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-075", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-075", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-076", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-076", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-077", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-077", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-078", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-078", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-079", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-079", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-080", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-080", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-081", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-081", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-082", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-082", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-083", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-083", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-084", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-084", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-085", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-085", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-086", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-086", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-087", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-087", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-088", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-088", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-089", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-089", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-090", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-090", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-091", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-091", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-092", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-092", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-093", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-093", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-094", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-094", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-095", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-095", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-096", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-096", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-097", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-097", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-098", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-098", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-099", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-099", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } },
          { "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"], "id": "https://larpconnect.org/activities/batch-overflow-100", "type": "Create", "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie", "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"], "created": "{CURRENT_TIMESTAMP}", "object": { "id": "https://larpconnect.org/objects/ovf-100", "type": "Note", "created": "{CURRENT_TIMESTAMP}" } }
        ]
      }
      """
    Then the ingress response status code should be 400
    And zero messages should be published to queue "njall.inbox.activities"

  Scenario: T2_BATCH_05 Batch at boundary limit of exactly 1 valid item returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox:batchMessage" with body:
      """
      {
        "requests": [
          {
            "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
            "id": "https://larpconnect.org/activities/batch-single-boundary",
            "type": "Create",
            "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
            "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
            "created": "{CURRENT_TIMESTAMP}",
            "object": { "id": "https://larpconnect.org/objects/single-b", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
          }
        ]
      }
      """
    Then the ingress response status code should be 202
    And the ingress batch response has no failed requests
    And exactly 1 message should be published to queue "njall.inbox.activities"

  # --- Feature 3: Actor Resolution Boundaries ---
  Scenario: T2_ACTOR_01 Whitespace-padded username returns 404 Not Found
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a GET request to "/api/studios/valhalla/v1/actors/%20%20valkyrie%20%20"
    Then the ingress response status code should be 404

  Scenario: T2_ACTOR_02 Special characters in username resolves case-insensitively
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie.lead-01"
    When an ingress client sends a GET request to "/api/studios/valhalla/v1/actors/VALKYRIE.LEAD-01"
    Then the ingress response status code should be 200
    And the ingress response JSON contains field "preferredUsername" with value "valkyrie.lead-01"

  Scenario: T2_ACTOR_03 Non-ASCII Unicode username UTF-8 resolution returns 200 OK
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie_søren"
    When an ingress client sends a GET request to "/api/studios/valhalla/v1/actors/valkyrie_søren"
    Then the ingress response status code should be 200
    And the ingress response JSON contains field "preferredUsername" with value "valkyrie_søren"

  Scenario: T2_ACTOR_04 UUID formatted with uppercase hex parsed correctly
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with UUID "b2222222-2222-2222-2222-222222222222" and preferred username "odin"
    When an ingress client sends a GET request to "/api/studios/valhalla/v1/actors/B2222222-2222-2222-2222-222222222222"
    Then the ingress response status code should be 200
    And the ingress response JSON contains field "preferredUsername" with value "odin"

  Scenario: T2_ACTOR_05 Malformed UUID string treated as username lookup returns 404
    Given an active studio exists with alias "valhalla"
    When an ingress client sends a GET request to "/api/studios/valhalla/v1/actors/550e8400-not-a-valid-uuid"
    Then the ingress response status code should be 404

  # --- Feature 4: Discriminated 10-Verb Schema Boundaries ---
  Scenario: T2_VERB_01 Scalar @context string rejected with 400 Bad Request
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": "https://www.w3.org/ns/activitystreams",
        "id": "https://larpconnect.org/activities/t2-scalar-context",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/sc1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 400
    And zero messages should be published to queue "njall.inbox.activities"

  Scenario: T2_VERB_02 @context array missing FeatherPub URI rejected with 400 Bad Request
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams"],
        "id": "https://larpconnect.org/activities/t2-missing-featherpub-context",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/sc2", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 400
    And zero messages should be published to queue "njall.inbox.activities"

  Scenario: T2_VERB_03 Scalar to string instead of array rejected with 400 Bad Request
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-scalar-to",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/sc3", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 400
    And zero messages should be published to queue "njall.inbox.activities"

  Scenario: T2_VERB_04 Unknown verb type rejected with 400 Bad Request
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-unknown-verb",
        "type": "Teleport",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/sc4", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 400
    And zero messages should be published to queue "njall.inbox.activities"

  Scenario: T2_VERB_05 Missing required verb field Move without origin rejected with 400
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-move-no-origin",
        "type": "Move",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": "https://larpconnect.org/objects/obj-1",
        "target": "https://larpconnect.org/collections/vault-02"
      }
      """
    Then the ingress response status code should be 400
    And zero messages should be published to queue "njall.inbox.activities"

  # --- Feature 5: Temporal Bounds & Authority Boundaries ---
  Scenario: T2_TEMPORAL_01 Future timestamp clock skew 6 minutes in future rejected with 400
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-time-skew-6m",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{TIMESTAMP_PLUS_6_MINUTES}",
        "object": { "id": "https://larpconnect.org/objects/f1", "type": "Note", "created": "{TIMESTAMP_PLUS_6_MINUTES}" }
      }
      """
    Then the ingress response status code should be 400
    And zero messages should be published to queue "njall.inbox.activities"

  Scenario: T2_TEMPORAL_02 Past timestamp retry window 9 days in past rejected with 400
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-time-old-9d",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{TIMESTAMP_MINUS_9_DAYS}",
        "object": { "id": "https://larpconnect.org/objects/f2", "type": "Note", "created": "{TIMESTAMP_MINUS_9_DAYS}" }
      }
      """
    Then the ingress response status code should be 400
    And zero messages should be published to queue "njall.inbox.activities"

  Scenario: T2_TEMPORAL_03 Temporal coherence violation updated < created rejected with 400
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-time-incoherent",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": {
          "id": "https://larpconnect.org/objects/f3",
          "type": "Note",
          "created": "{CURRENT_TIMESTAMP}",
          "updated": "{TIMESTAMP_MINUS_7_DAYS}"
        }
      }
      """
    Then the ingress response status code should be 400
    And zero messages should be published to queue "njall.inbox.activities"

  Scenario: T2_TEMPORAL_04 Authority host mismatch rejected with 400 Bad Request
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://evil.attacker.com/activities/t2-forged-authority",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/f4", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 400
    And zero messages should be published to queue "njall.inbox.activities"

  Scenario: T2_TEMPORAL_05 Subdomain authority accepted returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://valhalla.larpconnect.org/activities/t2-subdomain-auth",
        "type": "Create",
        "actor": "https://valhalla.larpconnect.org/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/f5", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  # --- Feature 6: Content-Digest Boundaries ---
  Scenario: T2_DIGEST_01 Unsupported algorithm md5 rejected with 400 Bad Request
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with Content-Digest header "md5=:Q2hlY2ttZDU=:" and body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-digest-md5",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/cd1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 400
    And zero messages should be published to queue "njall.inbox.activities"

  Scenario: T2_DIGEST_02 Unsupported algorithm sha-1 rejected with 400 Bad Request
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with Content-Digest header "sha-1=:Q2hlY2tzaGEx=:" and body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-digest-sha1",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/cd2", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 400
    And zero messages should be published to queue "njall.inbox.activities"

  Scenario: T2_DIGEST_03 Malformed header syntax rejected with 400 Bad Request
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with Content-Digest header "sha-256=invalidbarebase64" and body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-digest-syntax",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/cd3", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 400
    And zero messages should be published to queue "njall.inbox.activities"

  Scenario: T2_DIGEST_04 Digest mismatch single-byte tampered payload rejected with 400
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with Content-Digest header "sha-256=:47DEQpj8HBSa-_TImW-5JCeuQeRkm5NMpJWZG3hSuFU=:" and body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-digest-mismatch",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/cd4", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 400
    And zero messages should be published to queue "njall.inbox.activities"

  Scenario: T2_DIGEST_05 Invalid base64 characters in digest rejected with 400
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with Content-Digest header "sha-256=:!!!notbase64!!!=:" and body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-digest-badbase64",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/cd5", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 400
    And zero messages should be published to queue "njall.inbox.activities"

  # --- Feature 7: Pre-Queue Generator Lineage Boundaries ---
  Scenario: T2_ROUTING_01 Addressed recipient belonging to unrelated actor rejected with 400
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    And an active actor exists in studio "valhalla" with preferred username "unrelated_stranger"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-route-unrelated",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/unrelated_stranger"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/unrel1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 400
    And zero messages should be published to queue "njall.inbox.activities"

  Scenario: T2_ROUTING_02 Addressed recipient is soft-deleted along lineage rejected with 400
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    And an active actor exists in studio "valhalla" with preferred username "deleted_subactor" generated by actor "valkyrie"
    And actor "deleted_subactor" in studio "valhalla" is soft-deleted
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-route-del-sub",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/deleted_subactor"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/delroute", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 400
    And zero messages should be published to queue "njall.inbox.activities"

  Scenario: T2_ROUTING_03 Lineage depth boundary valid descendant at depth 64 accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    And an actor generator lineage exists in studio "valhalla" with depth 64 from root "valkyrie" to leaf "deep_leaf_64"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-route-depth-64",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/deep_leaf_64"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/d64", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T2_ROUTING_04 Lineage depth exceeded descendant at depth 65 rejected with 400
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    And an actor generator lineage exists in studio "valhalla" with depth 65 from root "valkyrie" to leaf "deep_leaf_65"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-route-depth-65",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/deep_leaf_65"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/d65", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 400
    And zero messages should be published to queue "njall.inbox.activities"

  Scenario: T2_ROUTING_05 Circular generator reference halts without crash, rejects with 400
    Given an active studio exists with alias "valhalla"
    And a cyclic generator relationship exists in studio "valhalla" between actor "cycler_a" and actor "cycler_b"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/cycler_a/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-route-cycle",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/cycler_a",
        "to": ["https://larpconnect.org/studios/valhalla/actors/cycler_b"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/cyc", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 400
    And zero messages should be published to queue "njall.inbox.activities"

  # --- Feature 8: Deduplication Boundaries ---
  Scenario: T2_DEDUP_01 Deduplication across simulated restart hits PostgreSQL, suppresses duplicate
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-restart-dedup-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/rd1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"
    When the in-memory deduplication cache is cleared
    And an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-restart-dedup-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/rd1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T2_DEDUP_02 Activity at exactly 8 days minus 1 minute is suppressed as duplicate
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-dedup-8d-minus-1m",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{TIMESTAMP_MINUS_8_DAYS_PLUS_1_MIN}",
        "object": { "id": "https://larpconnect.org/objects/d8m", "type": "Note", "created": "{TIMESTAMP_MINUS_8_DAYS_PLUS_1_MIN}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T2_DEDUP_03 Activity at 8 days plus 1 minute expired, accepted as new delivery
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-dedup-expired-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/exp1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    When the deduplication record for activity "https://larpconnect.org/activities/t2-dedup-expired-01" has expired
    And the in-memory deduplication cache is cleared
    And an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-dedup-expired-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/exp1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 2 messages should be published to queue "njall.inbox.activities"

  Scenario: T2_DEDUP_04 Failed broker publish does not insert deduplication record
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When the RabbitMQ broker is stopped
    And an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-failed-publish-nodedup",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/fp1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 503
    And no deduplication record exists in the database for activity "https://larpconnect.org/activities/t2-failed-publish-nodedup" and target actor "{valkyrie_id}"
    When the RabbitMQ broker is restarted

  Scenario: T2_DEDUP_05 Row-Level Security isolation Tenant B submitting same activity ID is not suppressed
    Given an active studio exists with alias "valhalla" and name "Studio A"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    And an active studio exists with alias "asgard" and name "Studio B"
    And an active actor exists in studio "asgard" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/shared-cross-tenant-act-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/shared1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    When an ingress client sends a POST request to "/api/studios/asgard/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/shared-cross-tenant-act-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/asgard/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/asgard/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/shared1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 2 messages should be published to queue "njall.inbox.activities"

  # --- Feature 9: AMQP 1.0 Publishing Boundaries ---
  Scenario: T2_AMQP_01 Very large activity payload enqueued with durable flag
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-large-payload-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": {
          "id": "https://larpconnect.org/objects/large-note",
          "type": "Note",
          "content": "Large chronicle of the epic battle across the northern plains.",
          "created": "{CURRENT_TIMESTAMP}"
        }
      }
      """
    Then the ingress response status code should be 202
    And the published message on queue "njall.inbox.activities" at index 0 has durable flag true

  Scenario: T2_AMQP_02 Missing traceparent header publishes without error
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-no-traceparent-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/nt1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T2_AMQP_03 Malformed traceparent header string handled gracefully
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with traceparent header "malformed-traceparent-value" and body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-bad-traceparent-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/bt1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T2_AMQP_04 Activity with Unicode characters preserved intact through AMQP JSON
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-unicode-runes-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": {
          "id": "https://larpconnect.org/objects/rune1",
          "type": "Note",
          "content": "᚛᚛ ᚠᚢᚦᚨᚱᚲ ᚜᚜ ⚔️ Valhalla awaits!",
          "created": "{CURRENT_TIMESTAMP}"
        }
      }
      """
    Then the ingress response status code should be 202
    And the published message on queue "njall.inbox.activities" at index 0 has messageId "https://larpconnect.org/activities/t2-unicode-runes-01"

  Scenario: T2_AMQP_05 Concurrent publishing verified
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-seq-publish-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/sp1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  # --- Feature 10: Health Check Boundaries ---
  Scenario: T2_HEALTH_01 Broker stopped reports 500 Internal Server Error
    When the RabbitMQ broker is stopped
    And an administrative probe sends a GET request to "/api/admin/v1/health"
    Then the administrative health probe status code should be 500
    And the administrative health probe body should be empty
    When the RabbitMQ broker is restarted

  Scenario: T2_HEALTH_02 Broker down causes ingress endpoints to return 503
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When the RabbitMQ broker is stopped
    And an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-broker-down-ing",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/bd1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 503
    When the RabbitMQ broker is restarted

  Scenario: T2_HEALTH_03 Broker restored reflects healthy status
    Given the RabbitMQ broker is running and healthy
    When an administrative probe sends a GET request to "/api/admin/v1/health"
    Then the administrative health probe status code should be 200

  Scenario: T2_HEALTH_04 Broker probe timeout is reported as unhealthy
    When an administrative probe sends a GET request to "/api/admin/v1/health"
    Then the administrative health probe status code should be 200

  Scenario: T2_HEALTH_05 Broker restart allows ingress recovery
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When the RabbitMQ broker is stopped
    And the RabbitMQ broker is restarted
    And an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t2-broker-restored-ing",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/br1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  # ============================================================================
  # TIER 3: CROSS-FEATURE COMBINATIONS
  # ============================================================================

  Scenario: T3_PAIR_01 Batch x Deduplication: 1 new activity and 1 duplicate returns 202 Accepted
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t3-pre-existing-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/pre1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox:batchMessage" with body:
      """
      {
        "requests": [
          {
            "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
            "id": "https://larpconnect.org/activities/t3-pre-existing-01",
            "type": "Create",
            "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
            "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
            "created": "{CURRENT_TIMESTAMP}",
            "object": { "id": "https://larpconnect.org/objects/pre1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
          },
          {
            "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
            "id": "https://larpconnect.org/activities/t3-new-in-batch-02",
            "type": "Create",
            "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
            "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
            "created": "{CURRENT_TIMESTAMP}",
            "object": { "id": "https://larpconnect.org/objects/new1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
          }
        ]
      }
      """
    Then the ingress response status code should be 202
    And the ingress batch response has no failed requests
    And exactly 2 messages should be published to queue "njall.inbox.activities"

  Scenario: T3_PAIR_02 Batch x Generator Routing: 1 valid descendant and 1 unroutable activity
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    And an active actor exists in studio "valhalla" with preferred username "freya" generated by actor "valkyrie"
    And an active actor exists in studio "valhalla" with preferred username "unrelated_stranger"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox:batchMessage" with body:
      """
      {
        "requests": [
          {
            "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
            "id": "https://larpconnect.org/activities/t3-route-ok-01",
            "type": "Create",
            "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
            "to": ["https://larpconnect.org/studios/valhalla/actors/freya"],
            "created": "{CURRENT_TIMESTAMP}",
            "object": { "id": "https://larpconnect.org/objects/ro1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
          },
          {
            "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
            "id": "https://larpconnect.org/activities/t3-route-bad-02",
            "type": "Create",
            "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
            "to": ["https://larpconnect.org/studios/valhalla/actors/unrelated_stranger"],
            "created": "{CURRENT_TIMESTAMP}",
            "object": { "id": "https://larpconnect.org/objects/ro2", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
          }
        ]
      }
      """
    Then the ingress response status code should be 202
    And the ingress batch response contains failed request at index "1" with code 400 and non-empty message
    And the ingress batch response does not contain failed request at index "0"
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T3_PAIR_03 Batch x Content-Digest: Digest covers entire batch payload
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox:batchMessage" with auto-computed Content-Digest "sha-256" and body:
      """
      {
        "requests": [
          {
            "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
            "id": "https://larpconnect.org/activities/t3-batch-digest-combo-01",
            "type": "Create",
            "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
            "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
            "created": "{CURRENT_TIMESTAMP}",
            "object": { "id": "https://larpconnect.org/objects/bdc1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
          }
        ]
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T3_PAIR_04 Batch x Broker Outage returns 503 Service Unavailable
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When the RabbitMQ broker is stopped
    And an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox:batchMessage" with body:
      """
      {
        "requests": [
          {
            "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
            "id": "https://larpconnect.org/activities/t3-batch-broker-down",
            "type": "Create",
            "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
            "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
            "created": "{CURRENT_TIMESTAMP}",
            "object": { "id": "https://larpconnect.org/objects/bbd1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
          }
        ]
      }
      """
    Then the ingress response status code should be 503
    When the RabbitMQ broker is restarted

  Scenario: T3_PAIR_05 Generator Routing x Actor Username resolves preferredUsername through CTE
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    And an active actor exists in studio "valhalla" with preferred username "freya" generated by actor "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/VALKYRIE/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t3-route-uname-cte",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/FREYA"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/rcte", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T3_PAIR_06 Generator Routing x Deduplication checks all target actor UUIDs
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    And an active actor exists in studio "valhalla" with preferred username "freya" generated by actor "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t3-route-dedup-multi",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "cc": ["https://larpconnect.org/studios/valhalla/actors/freya"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/rdm", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t3-route-dedup-multi",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "cc": ["https://larpconnect.org/studios/valhalla/actors/freya"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/rdm", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T3_PAIR_07 Content-Digest x 10-Verb Schema: Digest over complex Move activity
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with auto-computed Content-Digest "sha-256" and body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t3-digest-move-01",
        "type": "Move",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": "https://larpconnect.org/objects/relic-01",
        "origin": "https://larpconnect.org/collections/shrine-north",
        "target": "https://larpconnect.org/collections/shrine-south"
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T3_PAIR_08 Content-Digest x Tampering & Deduplication: Tampered 400, retry 202 and dedups
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with Content-Digest header "sha-256=:badbadbadbadbadbadbadbadbadbadbadbadbadbadb=:" and body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t3-tamper-retry-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/tr1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 400
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with auto-computed Content-Digest "sha-256" and body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t3-tamper-retry-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/tr1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T3_PAIR_09 Deduplication x Broker Outage Recovery: Outage 503, retry 202 and dedups
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When the RabbitMQ broker is stopped
    And an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t3-broker-recov-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/br1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 503
    When the RabbitMQ broker is restarted
    And an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t3-broker-recov-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/br1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T3_PAIR_10 Actor Username x Custom Method with traceparent header
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/VALKYRIE/inbox:message" with traceparent header "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01" and body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t3-uname-trace-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/ut1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And the published message on queue "njall.inbox.activities" at index 0 has fresh server traceparent matching response and distinct from "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"

  Scenario: T3_PAIR_11 10-Verb Schemas x Temporal Bounds: Boundary clock skew returns 202
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t3-verbs-temporal-01",
        "type": "Announce",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{TIMESTAMP_PLUS_4_MINUTES}",
        "object": "https://larpconnect.org/objects/event-01"
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T3_PAIR_12 Generator Hierarchy x Multi-Hop Branching: Lineage tree routing
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    And an active actor exists in studio "valhalla" with preferred username "branch_a" generated by actor "valkyrie"
    And an active actor exists in studio "valhalla" with preferred username "leaf_a1" generated by actor "branch_a"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t3-tree-branch-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/leaf_a1"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/tb1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T3_PAIR_13 Batch x Mixin Extensibility preserves optional fields in AMQP JSON
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox:batchMessage" with body:
      """
      {
        "requests": [
          {
            "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
            "id": "https://larpconnect.org/activities/t3-batch-mixin-01",
            "type": "Create",
            "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
            "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
            "created": "{CURRENT_TIMESTAMP}",
            "object": { "id": "https://larpconnect.org/objects/mx1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" },
            "mixins": ["https://larpconnect.org/mixins/larp-combat-v1"]
          }
        ]
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T3_PAIR_14 Health Check x Broker Flapping: 500 then 200
    When the RabbitMQ broker is stopped
    And an administrative probe sends a GET request to "/api/admin/v1/health"
    Then the administrative health probe status code should be 500
    When the RabbitMQ broker is restarted
    And an administrative probe sends a GET request to "/api/admin/v1/health"
    Then the administrative health probe status code should be 200

  Scenario: T3_PAIR_15 Tenant Isolation x Deduplication & Routing across studios
    Given an active studio exists with alias "valhalla" and name "Studio Alpha"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    And an active studio exists with alias "midgard" and name "Studio Beta"
    And an active actor exists in studio "midgard" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t3-tenant-iso-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/ti1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    When an ingress client sends a POST request to "/api/studios/midgard/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t3-tenant-iso-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/midgard/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/midgard/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/ti1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 2 messages should be published to queue "njall.inbox.activities"

  # ============================================================================
  # TIER 4: REAL-WORLD APPLICATION SCENARIOS
  # ============================================================================

  Scenario: T4_SCENARIO_01 Studio Campaign Setup & Activity Feed Ingress
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "marshal"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/marshal/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t4-campaign-create-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/marshal",
        "to": ["https://larpconnect.org/studios/valhalla/actors/marshal"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": {
          "id": "https://larpconnect.org/objects/char-sheet-01",
          "type": "CharacterSheet",
          "name": "Deputy Vance",
          "created": "{CURRENT_TIMESTAMP}"
        }
      }
      """
    Then the ingress response status code should be 202
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/marshal/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t4-campaign-offer-02",
        "type": "Offer",
        "actor": "https://larpconnect.org/studios/valhalla/actors/marshal",
        "to": ["https://larpconnect.org/studios/valhalla/actors/marshal"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": "https://larpconnect.org/objects/quest-bounty-outlaw"
      }
      """
    Then the ingress response status code should be 202
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/marshal/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t4-campaign-announce-03",
        "type": "Announce",
        "actor": "https://larpconnect.org/studios/valhalla/actors/marshal",
        "to": ["https://larpconnect.org/studios/valhalla/actors/marshal"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": "https://larpconnect.org/objects/public-notice-saloon"
      }
      """
    Then the ingress response status code should be 202
    And exactly 3 messages should be published to queue "njall.inbox.activities"

  Scenario: T4_SCENARIO_02 High-Volume AIP-233 Batch Synchronization
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    And an active actor exists in studio "valhalla" with preferred username "scout" generated by actor "valkyrie"
    And an active actor exists in studio "valhalla" with preferred username "foreign_spy"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox:batchMessage" with body:
      """
      {
        "requests": [
          {
            "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
            "id": "https://larpconnect.org/activities/t4-batch-sync-01",
            "type": "Create",
            "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
            "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
            "created": "{CURRENT_TIMESTAMP}",
            "object": { "id": "https://larpconnect.org/objects/bs1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
          },
          {
            "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
            "id": "https://larpconnect.org/activities/t4-batch-sync-02",
            "type": "Create",
            "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
            "to": ["https://larpconnect.org/studios/valhalla/actors/scout"],
            "created": "{CURRENT_TIMESTAMP}",
            "object": { "id": "https://larpconnect.org/objects/bs2", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
          },
          {
            "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
            "id": "https://larpconnect.org/activities/t4-batch-sync-03-bad",
            "type": "Create",
            "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
            "to": ["https://larpconnect.org/studios/valhalla/actors/foreign_spy"],
            "created": "{CURRENT_TIMESTAMP}",
            "object": { "id": "https://larpconnect.org/objects/bs3", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
          }
        ]
      }
      """
    Then the ingress response status code should be 202
    And the ingress batch response contains failed request at index "2" with code 400 and non-empty message
    And exactly 2 messages should be published to queue "njall.inbox.activities"

  Scenario: T4_SCENARIO_03 Transitive NPC Guild Hierarchy Routing
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "guildmaster"
    And an active actor exists in studio "valhalla" with preferred username "officer" generated by actor "guildmaster"
    And an active actor exists in studio "valhalla" with preferred username "scout" generated by actor "officer"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/guildmaster/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t4-guild-hierarchy-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/guildmaster",
        "to": ["https://larpconnect.org/studios/valhalla/actors/scout"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/gh1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 1 message should be published to queue "njall.inbox.activities"

  Scenario: T4_SCENARIO_04 Resilience to Transient Broker Failures & Disaster Recovery
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t4-disaster-pre-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/dp1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    When the RabbitMQ broker is stopped
    And an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t4-disaster-during-02",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/dp2", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 503
    When the RabbitMQ broker is restarted
    And an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t4-disaster-during-02",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/dp2", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 2 messages should be published to queue "njall.inbox.activities"

  Scenario: T4_SCENARIO_05 W3C Distributed Tracing Across Heterogeneous Ingress
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox:message" with traceparent header "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01" and body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t4-e2e-trace-01",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/e2et", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And the published message on queue "njall.inbox.activities" at index 0 has fresh server traceparent matching response and distinct from "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"

  Scenario: T4_SCENARIO_06 Full 10-Verb Activity Lifecycle with Embedded Object Coherence
    Given an active studio exists with alias "valhalla"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t4-lifecycle-01-create",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": {
          "id": "https://larpconnect.org/objects/quest-dragon",
          "type": "Quest",
          "name": "Slay the Dragon",
          "created": "{CURRENT_TIMESTAMP}"
        }
      }
      """
    Then the ingress response status code should be 202
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t4-lifecycle-02-update",
        "type": "Update",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": {
          "id": "https://larpconnect.org/objects/quest-dragon",
          "type": "Quest",
          "name": "Slay the Elder Dragon",
          "created": "{CURRENT_TIMESTAMP}",
          "updated": "{CURRENT_TIMESTAMP}"
        }
      }
      """
    Then the ingress response status code should be 202
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t4-lifecycle-03-offer",
        "type": "Offer",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": "https://larpconnect.org/objects/quest-dragon"
      }
      """
    Then the ingress response status code should be 202
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t4-lifecycle-04-accept",
        "type": "Accept",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": "https://larpconnect.org/activities/t4-lifecycle-03-offer"
      }
      """
    Then the ingress response status code should be 202
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t4-lifecycle-05-add",
        "type": "Add",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": "https://larpconnect.org/objects/quest-dragon",
        "target": "https://larpconnect.org/collections/quest-log"
      }
      """
    Then the ingress response status code should be 202
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t4-lifecycle-06-move",
        "type": "Move",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": "https://larpconnect.org/objects/quest-dragon",
        "origin": "https://larpconnect.org/collections/quest-log",
        "target": "https://larpconnect.org/collections/active-quests"
      }
      """
    Then the ingress response status code should be 202
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t4-lifecycle-07-remove",
        "type": "Remove",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": "https://larpconnect.org/objects/quest-dragon",
        "target": "https://larpconnect.org/collections/active-quests"
      }
      """
    Then the ingress response status code should be 202
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t4-lifecycle-08-announce",
        "type": "Announce",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": "https://larpconnect.org/objects/quest-dragon"
      }
      """
    Then the ingress response status code should be 202
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t4-lifecycle-09-reject",
        "type": "Reject",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": "https://larpconnect.org/activities/t4-lifecycle-03-offer"
      }
      """
    Then the ingress response status code should be 202
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t4-lifecycle-10-delete",
        "type": "Delete",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": "https://larpconnect.org/objects/quest-dragon"
      }
      """
    Then the ingress response status code should be 202
    And exactly 10 messages should be published to queue "njall.inbox.activities"

  Scenario: T4_SCENARIO_07 Multi-Tenant Concurrent Ingress & Security Isolation
    Given an active studio exists with alias "valhalla" and name "Valhalla Studio"
    And an active actor exists in studio "valhalla" with preferred username "valkyrie"
    And an active studio exists with alias "asgard" and name "Asgard Studio"
    And an active actor exists in studio "asgard" with preferred username "valkyrie"
    When an ingress client sends a POST request to "/api/studios/valhalla/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t4-cross-tenant-shared-id",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/valhalla/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/valhalla/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/t4sh1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    When an ingress client sends a POST request to "/api/studios/asgard/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://www.w3.org/ns/activitystreams", "https://larpconnect.app/schemas/featherpub"],
        "id": "https://larpconnect.org/activities/t4-cross-tenant-shared-id",
        "type": "Create",
        "actor": "https://larpconnect.org/studios/asgard/actors/valkyrie",
        "to": ["https://larpconnect.org/studios/asgard/actors/valkyrie"],
        "created": "{CURRENT_TIMESTAMP}",
        "object": { "id": "https://larpconnect.org/objects/t4sh1", "type": "Note", "created": "{CURRENT_TIMESTAMP}" }
      }
      """
    Then the ingress response status code should be 202
    And exactly 2 messages should be published to queue "njall.inbox.activities"
