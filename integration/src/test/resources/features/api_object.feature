Feature: ApiObject JSON Serialization and Deserialization

  Scenario: Serializing and deserializing a generic ApiObject
    Given an ApiObject constructed from the following JSON:
      """
      {
        "id": "https://example.com/obj/1",
        "name": "A test document",
        "type": [
          "Document"
        ]
      }
      """
    When it is deserialized back to an ApiObject
    Then the resulting ApiObject matches the original JSON properties

  Scenario: Serializing and deserializing a Document ApiObject
    Given an ApiObject constructed from the following JSON:
      """
      {
        "id": "https://example.com/doc/1",
        "name": "A text file",
        "type": [
          "Document"
        ],
        "media_type": "text/plain",
        "content": "aGVsbG8gd29ybGQ="
      }
      """
    When it is deserialized back to an ApiObject
    Then the resulting ApiObject contains a "document" extension
    And the Document extension has mediaType "text/plain" and content "hello world"

  Scenario: Serializing and deserializing an Event ApiObject
    Given an ApiObject constructed from the following JSON:
      """
      {
        "id": "https://example.com/event/1",
        "type": [
          "Event"
        ],
        "start_time": "2023-01-01T00:00:00Z",
        "end_time": "2023-01-01T01:00:00Z"
      }
      """
    When it is deserialized back to an ApiObject
    Then the resulting ApiObject contains an "event" extension
    And the Event extension has startTime "2023-01-01T00:00:00Z"
