The comment points out: "For a media_type of text/plain we should interpret content as just straight bytes and not as encoded bytes."

Looking at the feature file snippet:
```json
        "media_type": "text/plain",
        "content": "aGVsbG8gd29ybGQ="
```

The user is saying that if the media type is `text/plain`, the string in `content` shouldn't be base64-encoded (`aGVsbG8gd29ybGQ=`) but instead just the plain text `hello world` (which is what that decodes to). However, `content` is a `bytes` field in Protobuf:
```protobuf
message Document {
  bytes content = 1;
  string media_type = 2;
}
```

Protobuf's standard JSON mapping specifies that `bytes` fields are ALWAYS mapped to base64-encoded strings in JSON. If we want it to interpret `content` as plain string bytes in JSON when `media_type` is `text/plain`, we will have to customize our `ApiObjectParser` to handle this explicitly because `JsonFormat.parser()` will fail if given a non-base64 string for a `bytes` field.

Wait, if we customize this, we need to:
1. When generating JSON (`toJson`): If the extension is `Document`, and `media_type` starts with `text/`, we could decode the base64 string that `JsonFormat.printer()` produces into plain text. Or we can intercept the conversion.
   Wait, the comment says: "For a media_type of text/plain we should interpret content as just straight bytes and not as encoded bytes." This means that the JSON `content` field should be a standard string like `"content": "hello world"`.

Let's look at `DefaultApiObjectParser.java`:
```java
            case "Document" -> {
              // we can intercept the Document generation and parsing here
            }
```

For JSON mapping to/from ActivityPub or similar, `content` in a Document is typically HTML or text, not base64. So instead of strictly relying on `JsonFormat`, we might need to special-case `content` for `Document` in our parser.

Let's modify `DefaultApiObjectParser.java` to handle this custom parsing/printing for Document.
