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
