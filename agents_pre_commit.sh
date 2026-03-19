# 4. Record Learnings
cat << 'LOG' >> .agents/pentiousinator/log.md
## 2026-03-19 - Test Configuration and Compilation Improvements

**Learning:** Found and fixed several issues, including bad imports, Mockito compilation errors due to mismatched UUIDs to Object conversions, generic matching syntax inside Mutiny block verification, and some invalid references. Removed redundant build entries in the gitignore file, and properly applied the Java spot check plugin with test names. Tests needed to be properly named avoiding `test...()` functions. The `Testcontainers` do not currently function natively inside of the sandbox space.

**Action:** Ensure tests are named accurately, use specific Matcher syntax for Mockito when verifying Uni methods, and ensure redundant folders inside `.gitignore` are trimmed if covered by top-level variables.
LOG

cat << 'LOG' >> .agents/nifftyinator/log.md
## 2026-03-19 - Code formatting and cleanups

**Learning:** There was a double DefaultImplementation annotation resulting in compile-time errors. Multiple `.java` test files needed to be run against spotlessApply for syntax.

**Action:** Check that annotations are only applied once to classes, especially in DAO patterns, and apply Spotless to enforce standard format.
LOG
