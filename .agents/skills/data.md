# Skill: Data and I/O

## Domain Context

This skill handles the reading and writing of JSON, CSV, Protobuf, and other
such formats.

## Technical Constraints

- Data Interchange: Protobuf is the primary specification format. All data to be
  exchanged should have a representation in protobuf.
- JSON: Protobuf should generally be turned into JSON before using it to respond
  to a web request unless explicitly requested otherwise.
- Schemas: Schemas (in protobuf and/or OpenAPI) must be kept up to date to
  ensure proper documentation of data exchange formats.
- Do not use unicode escapes in JSON files. They are UTF-8 files and the encoded
  character is always preferable.

## Specific Guidance

- Use Guava IO when possible for data reading and writing.
- If the tool doesn't exist in guava, Apache Commons IO can be used.
- If JSON must be parsed independently from Protobuf, use Gson.
