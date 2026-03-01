## 2024-05-24 - Default implementations need not be specified if the target is package private

**Learning:** For `IdGenerator` which has `@DefaultImplementation(UuidV7Generator.class)` maybe we can apply it. Wait, `UuidV7Generator` is package private. Check if `IdGenerator` has `@DefaultImplementation`? Let's verify.
