Feature: FeatherPub Inbox Ingress API
  As a federated FeatherPub client or remote server
  I want to submit single and batch Activities to an actor's inbox
  So that activities are validated, pre-queue routed, deduplicated, and asynchronously queued over AMQP 1.0

  Background:
    Given an active studio exists with alias "mystic-realms"
    And an active actor exists in studio "mystic-realms" with preferred username "valkyrie" and remembered as "valkyrie_actor"

  # Single Activity Ingress
  Scenario: Accept valid Create activity delivered to actor inbox by UUID
    When a federated client sends a POST request to "/api/studios/mystic-realms/v1/actors/{valkyrie_actor}/inbox" with body:
      """
      {
        "@context": ["https://larpconnect.app/schemas/featherpub"],
        "id": "https://remote.example/activities/create-001",
        "type": "Create",
        "actor": "https://remote.example/actors/gm",
        "to": ["/api/studios/mystic-realms/v1/actors/valkyrie"],
        "cc": [],
        "audience": [],
        "created": "2026-11-14T11:00:00Z",
        "object": {
          "id": "https://remote.example/notes/101",
          "type": "Note",
          "content": "Hail and well met"
        }
      }
      """
    Then the HTTP response status code is 202
    And the HTTP response body is empty

  Scenario: Accept valid activity on custom inbox:message method by preferredUsername
    When a federated client sends a POST request to "/api/studios/mystic-realms/v1/actors/VALKYRIE/inbox:message" with body:
      """
      {
        "@context": ["https://larpconnect.app/schemas/featherpub"],
        "id": "https://remote.example/activities/announce-002",
        "type": "Announce",
        "actor": "https://remote.example/actors/gm",
        "to": [],
        "cc": ["/api/studios/mystic-realms/v1/actors/valkyrie"],
        "audience": [],
        "created": "2026-11-14T11:00:00Z",
        "object": "https://remote.example/notes/101"
      }
      """
    Then the HTTP response status code is 202
    And the HTTP response body is empty

  # Content-Digest Verification
  Scenario: Accept inbox request with valid matching Content-Digest header
    When a federated client sends a POST request to "/api/studios/mystic-realms/v1/actors/valkyrie/inbox" with header "Content-Digest" having valid "sha-256" digest and body:
      """
      {
        "@context": ["https://larpconnect.app/schemas/featherpub"],
        "id": "https://remote.example/activities/create-digest-ok",
        "type": "Create",
        "actor": "https://remote.example/actors/gm",
        "to": ["/api/studios/mystic-realms/v1/actors/valkyrie"],
        "cc": [],
        "audience": [],
        "created": "2026-11-14T11:00:00Z",
        "object": {
          "id": "https://remote.example/notes/102",
          "type": "Note",
          "content": "Digest verified payload"
        }
      }
      """
    Then the HTTP response status code is 202

  Scenario: Reject inbox request with mismatched Content-Digest header
    When a federated client sends a POST request to "/api/studios/mystic-realms/v1/actors/valkyrie/inbox" with header "Content-Digest" having mismatched digest "sha-256=:47DEQpj8HBSa-_TImW-5JCeuQeRkm5NMpJWZG3hSuFU=:" and body:
      """
      {
        "@context": ["https://larpconnect.app/schemas/featherpub"],
        "id": "https://remote.example/activities/create-bad-digest",
        "type": "Create",
        "actor": "https://remote.example/actors/gm",
        "to": ["/api/studios/mystic-realms/v1/actors/valkyrie"],
        "cc": [],
        "audience": [],
        "created": "2026-11-14T11:00:00Z",
        "object": {
          "id": "https://remote.example/notes/103",
          "type": "Note",
          "content": "Tampered payload"
        }
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Reject inbox request with unsupported Content-Digest algorithm
    When a federated client sends a POST request to "/api/studios/mystic-realms/v1/actors/valkyrie/inbox" with header "Content-Digest" value "md5=:Q2hlY2sgSW50ZWdyaXR5IQ==:" and body:
      """
      {
        "@context": ["https://larpconnect.app/schemas/featherpub"],
        "id": "https://remote.example/activities/create-md5",
        "type": "Create",
        "actor": "https://remote.example/actors/gm",
        "to": ["/api/studios/mystic-realms/v1/actors/valkyrie"],
        "cc": [],
        "audience": [],
        "created": "2026-11-14T11:00:00Z",
        "object": {
          "id": "https://remote.example/notes/104",
          "type": "Note",
          "content": "Unsupported algorithm"
        }
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  # Wire Contract Validation
  Scenario: Reject activity with scalar @context or authority host mismatch
    When a federated client sends a POST request to "/api/studios/mystic-realms/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": "https://larpconnect.app/schemas/featherpub",
        "id": "https://evil.example/activities/create-bad-auth",
        "type": "Create",
        "actor": "https://remote.example/actors/gm",
        "to": ["/api/studios/mystic-realms/v1/actors/valkyrie"],
        "cc": [],
        "audience": [],
        "created": "2026-11-14T11:00:00Z",
        "object": {
          "id": "https://remote.example/notes/105",
          "type": "Note",
          "content": "Invalid context and host"
        }
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Reject activity outside temporal bounds or missing verb-specific fields
    When a federated client sends a POST request to "/api/studios/mystic-realms/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://larpconnect.app/schemas/featherpub"],
        "id": "https://remote.example/activities/move-bad",
        "type": "Move",
        "actor": "https://remote.example/actors/gm",
        "to": ["/api/studios/mystic-realms/v1/actors/valkyrie"],
        "cc": [],
        "audience": [],
        "created": "2026-10-01T00:00:00Z",
        "object": "https://remote.example/items/sword"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  # Pre-Queue Generator-Hierarchy Recipient Routing
  Scenario: Route activity addressed to child and grandchild actors generated by receiving actor
    Given an active actor exists in studio "mystic-realms" with preferred username "npc_sheriff" generated by "valkyrie"
    And an active actor exists in studio "mystic-realms" with preferred username "npc_hound" generated by "npc_sheriff"
    When a federated client sends a POST request to "/api/studios/mystic-realms/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://larpconnect.app/schemas/featherpub"],
        "id": "https://remote.example/activities/create-lineage",
        "type": "Create",
        "actor": "https://remote.example/actors/gm",
        "to": ["/api/studios/mystic-realms/v1/actors/npc_hound"],
        "cc": ["/api/studios/mystic-realms/v1/actors/npc_sheriff"],
        "audience": [],
        "created": "2026-11-14T11:00:00Z",
        "object": {
          "id": "https://remote.example/notes/201",
          "type": "Note",
          "content": "Message for descendants"
        }
      }
      """
    Then the HTTP response status code is 202
    And the activity is queued to AMQP with target actors including "npc_hound" and "npc_sheriff"

  Scenario: Reject activity addressed only to unrelated actor outside generator subtree
    Given an active actor exists in studio "mystic-realms" with preferred username "independent_bard"
    When a federated client sends a POST request to "/api/studios/mystic-realms/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://larpconnect.app/schemas/featherpub"],
        "id": "https://remote.example/activities/create-unrelated",
        "type": "Create",
        "actor": "https://remote.example/actors/gm",
        "to": ["/api/studios/mystic-realms/v1/actors/independent_bard"],
        "cc": [],
        "audience": [],
        "created": "2026-11-14T11:00:00Z",
        "object": {
          "id": "https://remote.example/notes/202",
          "type": "Note",
          "content": "Misaddressed message"
        }
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  # Eight-Day Deduplication
  Scenario: Duplicate activity retry returns 202 Accepted without re-queuing
    Given an activity with id "https://remote.example/activities/dup-001" was already accepted for actor "valkyrie"
    When a federated client sends a POST request to "/api/studios/mystic-realms/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://larpconnect.app/schemas/featherpub"],
        "id": "https://remote.example/activities/dup-001",
        "type": "Create",
        "actor": "https://remote.example/actors/gm",
        "to": ["/api/studios/mystic-realms/v1/actors/valkyrie"],
        "cc": [],
        "audience": [],
        "created": "2026-11-14T11:00:00Z",
        "object": {
          "id": "https://remote.example/notes/301",
          "type": "Note",
          "content": "Retry message"
        }
      }
      """
    Then the HTTP response status code is 202
    And zero AMQP messages are published for duplicate activity "https://remote.example/activities/dup-001"

  # AIP-233 Batch Message Delivery
  Scenario: Partial success in batch inbox delivery queues valid items and reports failed indices
    When a federated client sends a POST request to "/api/studios/mystic-realms/v1/actors/valkyrie/inbox:batchMessage" with body:
      """
      {
        "requests": [
          {
            "@context": ["https://larpconnect.app/schemas/featherpub"],
            "id": "https://remote.example/activities/batch-001",
            "type": "Create",
            "actor": "https://remote.example/actors/gm",
            "to": ["/api/studios/mystic-realms/v1/actors/valkyrie"],
            "cc": [],
            "audience": [],
            "created": "2026-11-14T11:00:00Z",
            "object": {
              "id": "https://remote.example/notes/401",
              "type": "Note",
              "content": "Valid item at index 0"
            }
          },
          {
            "id": "https://remote.example/activities/batch-002",
            "type": "Create",
            "actor": "https://remote.example/actors/gm",
            "to": ["/api/studios/mystic-realms/v1/actors/valkyrie"],
            "cc": [],
            "audience": [],
            "created": "2026-11-14T11:00:00Z",
            "object": {
              "id": "https://remote.example/notes/402",
              "type": "Note",
              "content": "Invalid item at index 1 missing @context"
            }
          },
          {
            "@context": ["https://larpconnect.app/schemas/featherpub"],
            "id": "https://remote.example/activities/batch-003",
            "type": "Delete",
            "actor": "https://remote.example/actors/gm",
            "to": ["/api/studios/mystic-realms/v1/actors/valkyrie"],
            "cc": [],
            "audience": [],
            "created": "2026-11-14T11:00:00Z",
            "object": "https://remote.example/notes/403"
          }
        ]
      }
      """
    Then the HTTP response status code is 202
    And exactly 2 AMQP messages are published for the batch
    And the JSON response contains failed request index "1" with code 400
    And the JSON response does not contain failed request index "0"
    And the JSON response does not contain failed request index "2"

  Scenario: Total failure in batch inbox delivery returns 400 Bad Request with failedRequests
    When a federated client sends a POST request to "/api/studios/mystic-realms/v1/actors/valkyrie/inbox:batchMessage" with body:
      """
      {
        "requests": [
          {
            "id": "https://remote.example/activities/batch-fail-001",
            "type": "Create",
            "actor": "https://remote.example/actors/gm",
            "to": ["/api/studios/mystic-realms/v1/actors/valkyrie"],
            "created": "2026-11-14T11:00:00Z"
          }
        ]
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message
    And the JSON response contains failed request index "0" with code 400
    And zero AMQP messages are published for the batch

  Scenario: Empty requests array in batch delivery returns 400 Bad Request
    When a federated client sends a POST request to "/api/studios/mystic-realms/v1/actors/valkyrie/inbox:batchMessage" with body:
      """
      {
        "requests": []
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Accepting a new activity prunes expired deduplication rows for target actor
    Given an expired activity with id "https://remote.example/activities/expired-prune-001" exists for actor "valkyrie"
    When a federated client sends a POST request to "/api/studios/mystic-realms/v1/actors/valkyrie/inbox" with body:
      """
      {
        "@context": ["https://larpconnect.app/schemas/featherpub"],
        "id": "https://remote.example/activities/fresh-prune-002",
        "type": "Create",
        "actor": "https://remote.example/actors/gm",
        "to": ["/api/studios/mystic-realms/v1/actors/valkyrie"],
        "cc": [],
        "audience": [],
        "created": "{CURRENT_TIMESTAMP}",
        "object": {
          "id": "https://remote.example/notes/909",
          "type": "Note",
          "content": "Triggers opportunistic pruning"
        }
      }
      """
    Then the HTTP response status code is 202
    And the deduplication table no longer contains activity "https://remote.example/activities/expired-prune-001" for actor "valkyrie"
    And the deduplication table contains activity "https://remote.example/activities/fresh-prune-002" for actor "valkyrie"

  Scenario: Route activity delivered to non-root child actor inbox including embedded object recipients
    Given an active actor exists in studio "mystic-realms" with preferred username "npc_sheriff" generated by "valkyrie"
    And an active actor exists in studio "mystic-realms" with preferred username "npc_hound" generated by "npc_sheriff"
    When a federated client sends a POST request to "/api/studios/mystic-realms/v1/actors/npc_sheriff/inbox" with body:
      """
      {
        "@context": ["https://larpconnect.app/schemas/featherpub"],
        "id": "https://remote.example/activities/child-inbox-embedded-001",
        "type": "Create",
        "actor": "https://remote.example/actors/gm",
        "to": ["https://www.w3.org/ns/activitystreams#Public", "/api/studios/mystic-realms/v1/actors/npc_sheriff"],
        "cc": [],
        "audience": [],
        "created": "{CURRENT_TIMESTAMP}",
        "object": {
          "id": "https://remote.example/notes/910",
          "type": "Note",
          "cc": ["/api/studios/mystic-realms/v1/actors/npc_hound", "/api/studios/mystic-realms/v1/actors/valkyrie"],
          "content": "Delivered to child actor inbox with grandchild in embedded object.cc"
        }
      }
      """
    Then the HTTP response status code is 202
    And the activity is queued to AMQP with target actors including "npc_sheriff" and "npc_hound"
    And the last queued AMQP envelope excludes target actor "valkyrie"

  Scenario: Reject activity delivered to non-root child actor inbox when addressed only to parent actor
    Given an active actor exists in studio "mystic-realms" with preferred username "npc_sheriff" generated by "valkyrie"
    When a federated client sends a POST request to "/api/studios/mystic-realms/v1/actors/npc_sheriff/inbox" with body:
      """
      {
        "@context": ["https://larpconnect.app/schemas/featherpub"],
        "id": "https://remote.example/activities/child-inbox-parent-only-002",
        "type": "Create",
        "actor": "https://remote.example/actors/gm",
        "to": ["/api/studios/mystic-realms/v1/actors/valkyrie"],
        "cc": [],
        "audience": [],
        "created": "{CURRENT_TIMESTAMP}",
        "object": {
          "id": "https://remote.example/notes/911",
          "type": "Note",
          "content": "Addressed only to parent of receiving actor"
        }
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Multi-chunk PostgreSQL batch upsert and deduplication across 1005 target actors
    Given 1005 active descendant actors exist in studio "mystic-realms" generated by "valkyrie"
    When a federated client posts an activity to "/api/studios/mystic-realms/v1/actors/valkyrie/inbox" addressing all 1005 bulk actors with id "https://remote.example/activities/bulk-1005"
    Then the HTTP response status code is 202
    And the AMQP envelope has 1005 target actors and PostgreSQL stores 1005 deduplication rows for "https://remote.example/activities/bulk-1005"
    When the local deduplication cache is invalidated
    And a federated client posts an activity to "/api/studios/mystic-realms/v1/actors/valkyrie/inbox" addressing all 1005 bulk actors with id "https://remote.example/activities/bulk-1005"
    Then the HTTP response status code is 202
    And zero AMQP messages are published for duplicate activity "https://remote.example/activities/bulk-1005"
