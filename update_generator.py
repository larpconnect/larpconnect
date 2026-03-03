import re

file_path = "common/src/main/java/com/larpconnect/njall/common/id/UuidV7Generator.java"

with open(file_path, "r") as f:
    content = f.read()

search_pattern = r"""
    State updatedState =
        state\.accumulateAndGet\(
            new State\(currentTimeMs, 0\),
            \(current, update\) -> \{
              long newTimeMs = update\.timeMs\(\);
              long newCounter;

              if \(newTimeMs > current\.timeMs\(\)\) \{
                newCounter = randomProvider\.get\(\)\.nextLong\(MIN_COUNTER, MAX_COUNTER\);
              \} else \{
                newTimeMs = current\.timeMs\(\);
                newCounter = \(current\.counter\(\) \+ COUNTER_INCREMENT\) & COUNTER_MASK;
              \}
              return new State\(newTimeMs, newCounter\);
            \}\);
"""

replace_pattern = r"""
    // Generate unconditionally outside the atomic update to avoid side effects during retries.
    long nextRandomCounter = randomProvider.get().nextLong(MIN_COUNTER, MAX_COUNTER);

    State updatedState =
        state.accumulateAndGet(
            new State(currentTimeMs, nextRandomCounter),
            (current, update) -> {
              long newTimeMs = update.timeMs();
              long newCounter;

              if (newTimeMs > current.timeMs()) {
                newCounter = update.counter();
              } else {
                newTimeMs = current.timeMs();
                newCounter = (current.counter() + COUNTER_INCREMENT) & COUNTER_MASK;
              }
              return new State(newTimeMs, newCounter);
            });
"""

import sys
if search_pattern.strip() in content:
    print("Found exact string")
else:
    print("Could not find exact string")
