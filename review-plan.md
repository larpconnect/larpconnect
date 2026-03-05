1. **Refactor `ApiObjectConverter`**:
   - Change from static methods to an instantiable object, injected via Guice interface (e.g. `ApiObjectParser`).
   - The user commented: "This is very manual. You could accomplish the same thing with the built-in JSON parsing out of protobuf, similar to what you do below for the individual extensions." This implies I should perhaps construct a standard JSON representation that protobuf can parse directly (via JsonFormat.parser()) rather than manually mapping fields like id/name/deleted. Or vice versa, for toJson. Wait, if I use the built-in JsonFormat for the whole ApiObject, it will expect extensions to be a map of strings to extensions. The issue is we want extensions lifted to the top level in the generated JSON.
   - For deserialization, the comment states: "So we need to read the type field and use that information to then build the relevant extensions in our derived object, rather than presuming that the extensions will be laid out for us." So `type: ["Document"]` means we should take the entire root JSON object, parse it as a `Document` extension, and add it to the extensions map under `document`.
   - Also: `ignoreUnknownFields` should be set to true, and PRINTER/PARSER should be injected.
   - "Aside: if it contains deleted then, while it may have types, it should have no extensions and we don't need to parse them."
2. **Refactor the feature file**:
   - Update `integration/src/test/resources/features/api_object.feature` to define an entire JSON object in the Given clause using a multiline string (DocString).

Let's refine the logic for parsing and generating:

**Generating JSON**:
To convert `ApiObject` to `JsonObject`:
1. Use `JsonFormat.printer().print(apiObject)` to get the base JSON. (This will have id, name, deleted, etc. and a map `extensions` if present).
2. Take this JSON, remove the `extensions` field.
3. For each extension in the `extensions` map, merge its fields directly into the root JSON object.

**Parsing JSON**:
To convert `JsonObject` to `ApiObject`:
1. If `deleted` is present, just parse the root JSON using `JsonFormat.parser()` directly into `ApiObject.Builder` (with `ignoreUnknownFields`), and skip extensions.
2. If not deleted, parse the root JSON into `ApiObject.Builder`.
3. Check the `type` array. For each supported type (e.g., "Document", "Event", "Link", "OrderedCollectionPage"), use the *entire root JSON* and parse it into the respective Extension message. Add these to the `extensions` map.
Wait, `JsonFormat.parser()` requires the JSON to map exactly to the proto fields. So if the root JSON has `media_type` and we parse it into `ApiObject.Builder` (which has `ignoreUnknownFields` true), it will ignore `media_type`. Then we take the same root JSON string, and parse it into `Document.Builder`, and it will pick up `media_type` and ignore `id`, `name`, etc. This is brilliant and perfectly leverages Protobuf's built-in parsing!

Let's check the fields of `ApiObject`:
- `context`, `id`, `type`, `name`, `deleted`, `extensions`
If we parse the root JSON into `ApiObject.Builder`, it will populate all base fields.
We can then remove `extensions` if the JSON accidentally provided it (or just rely on the map being empty if they didn't).

Let's verify this plan with code changes.
