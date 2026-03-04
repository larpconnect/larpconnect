## 2025-01-01 - Be Careful With Cruft

**Learning:** Pure explanatory comments (`//`) are usually preferred by the maintainer and shouldn't be automatically assumed to be "cruft" just because they only explain what the code is doing without the context of "why". Indiscriminate deletion of comments will fail code review and block merges.

**Action:** Be very conservative about deleting comments. Focus primarily on the code logic tasks (like `new RuntimeException() -> new CustomException()`) and only delete comments that are extremely obviously auto-generated artifact cruft or directly contradict code.
