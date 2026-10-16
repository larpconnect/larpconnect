## 2026-10-13 17:44

Refactored LinkValidation to streamline Optional field checks using Optional.filter in place of nested isPresent and orElseThrow checks.

## 2026-10-13 18:55

Refactored DefaultStudioLookupCacheService.filterActive to use Optional.ofNullable(entry).filter(e -> e.deletedAt().isEmpty()) for cleaner functional composition and elimination of explicit null check branching.
