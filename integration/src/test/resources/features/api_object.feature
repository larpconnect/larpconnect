Feature: ApiObject JSON Serialization and Deserialization

  Scenario: Serializing and deserializing a generic ApiObject
    Given an ApiObject with basic properties "id", "name", and "type"
    When it is serialized to JSON
    And it is deserialized back to an ApiObject
    Then the resulting ApiObject matches the original properties

  Scenario: Serializing an ApiObject with a Document extension
    Given an ApiObject with a Document extension
    When it is serialized to JSON
    And it is deserialized back to an ApiObject
    Then the Document extension is preserved

  Scenario: Serializing an ApiObject with an Event extension
    Given an ApiObject with an Event extension
    When it is serialized to JSON
    And it is deserialized back to an ApiObject
    Then the Event extension is preserved

  Scenario: Serializing an ApiObject with a Link extension
    Given an ApiObject with a Link extension
    When it is serialized to JSON
    And it is deserialized back to an ApiObject
    Then the Link extension is preserved

  Scenario: Serializing an ApiObject with an OrderedCollectionPage extension
    Given an ApiObject with an OrderedCollectionPage extension
    When it is serialized to JSON
    And it is deserialized back to an ApiObject
    Then the OrderedCollectionPage extension is preserved
